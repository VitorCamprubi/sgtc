package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.DocumentoVersao;
import com.vitorcamprubi.sgtc.domain.Grupo;
import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.notification.EmailService;
import com.vitorcamprubi.sgtc.repo.DocumentoComentarioRepository;
import com.vitorcamprubi.sgtc.repo.DocumentoVersaoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DocumentoService {
    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");

    /**
     * Tipos MIME aceitos para upload de documentos.
     * Mantemos restrito a formatos academicos comuns para reduzir superficie de ataque.
     */
    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "application/pdf",
            "application/msword",                                                    // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.oasis.opendocument.text",                               // .odt
            "application/rtf",
            "text/plain"
    );

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(
            ".pdf", ".doc", ".docx", ".odt", ".rtf", ".txt"
    );

    /** Magic bytes do tipo de arquivo, comparados com os primeiros bytes do upload. */
    private static final byte[] MAGIC_PDF      = {0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] MAGIC_OFFICE_X = {0x50, 0x4B, 0x03, 0x04}; // PK.. (zip - docx/odt)
    private static final byte[] MAGIC_DOC_OLE  = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}; // OLE2 (.doc legado)
    private static final byte[] MAGIC_RTF      = {0x7B, 0x5C, 0x72, 0x74}; // {\rt

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final DocumentoVersaoRepository docs;
    private final DocumentoComentarioRepository comentarios;
    private final PermissaoService perms;
    private final EmailService emailService;

    public DocumentoService(DocumentoVersaoRepository docs, DocumentoComentarioRepository comentarios,
                            PermissaoService perms, EmailService emailService) {
        this.docs = docs;
        this.comentarios = comentarios;
        this.perms = perms;
        this.emailService = emailService;
    }

    @Transactional
    public DocumentoVersao upload(Long grupoId, String titulo, MultipartFile file, User atual) throws IOException {
        if (titulo == null || titulo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Titulo do documento eh obrigatorio");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo obrigatorio");
        }

        Grupo g = perms.assertPodeAcessarGrupo(grupoId, atual);
        perms.assertGrupoEmCurso(g);

        validarTipoArquivo(file);

        int next = docs.countByGrupoId(grupoId) + 1;

        Path dir = Paths.get(uploadDir, String.valueOf(grupoId)).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String original = file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename();
        String safeOriginal = Paths.get(original).getFileName().toString().replace(' ', '_');
        safeOriginal = UNSAFE_FILENAME_CHARS.matcher(safeOriginal).replaceAll("_");
        if (safeOriginal.isBlank()) {
            safeOriginal = "arquivo";
        }

        String filename = "v" + next + "_" + safeOriginal;
        Path dest = dir.resolve(filename).normalize();
        if (!dest.startsWith(dir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo invalido");
        }

        file.transferTo(dest);

        String mimeType = Files.probeContentType(dest);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        DocumentoVersao d = new DocumentoVersao();
        d.setGrupo(g);
        d.setTitulo(titulo.trim());
        d.setVersao(next);
        d.setFilePath(dest.toString());
        d.setMimeType(mimeType);
        d.setTamanho(Files.size(dest));
        d.setEnviadoPor(atual);
        DocumentoVersao salvo = docs.save(d);

        // Notifica orientador e coorientador quando o upload foi feito por aluno
        if (atual.getRole() == Role.ALUNO) {
            try {
                List<User> destinatarios = professoresDoGrupo(g);
                emailService.enviarUploadDocumentoParaProfessores(destinatarios, salvo, atual);
            } catch (RuntimeException ignored) {
                // best-effort
            }
        }
        return salvo;
    }

    private List<User> professoresDoGrupo(Grupo g) {
        Set<Long> ids = new LinkedHashSet<>();
        List<User> destinatarios = new ArrayList<>();
        if (g.getOrientador() != null && g.getOrientador().isEmailConfirmado()) {
            destinatarios.add(g.getOrientador());
            ids.add(g.getOrientador().getId());
        }
        if (g.getCoorientador() != null && g.getCoorientador().isEmailConfirmado()
                && !ids.contains(g.getCoorientador().getId())) {
            destinatarios.add(g.getCoorientador());
        }
        return destinatarios;
    }

    public List<DocumentoVersao> listar(Long grupoId, User atual) {
        perms.assertPodeAcessarGrupo(grupoId, atual);
        return docs.findByGrupoIdOrderByVersaoDesc(grupoId);
    }

    public ResponseEntity<Resource> download(Long docId, User atual) throws IOException {
        DocumentoVersao d = docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento nao encontrado"));

        perms.assertPodeAcessarGrupo(d.getGrupo().getId(), atual);

        Path path = Paths.get(d.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo nao encontrado");
        }

        Resource res = new UrlResource(path.toUri());
        if (!res.exists() || !res.isReadable()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo nao encontrado");
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(d.getMimeType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                .contentLength(Files.size(path))
                .contentType(mediaType)
                .body(res);
    }

    @Transactional
    public DocumentoVersao atualizarTitulo(Long docId, String titulo, User atual) {
        if (titulo == null || titulo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Titulo do documento eh obrigatorio");
        }

        DocumentoVersao d = docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento nao encontrado"));

        assertPodeAlterarOuExcluirDocumento(d, atual);
        d.setTitulo(titulo.trim());
        return docs.save(d);
    }

    @Transactional
    public void delete(Long docId, User atual) throws IOException {
        DocumentoVersao d = docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento nao encontrado"));

        assertPodeAlterarOuExcluirDocumento(d, atual);

        comentarios.deleteByDocumentoId(docId);

        try {
            Files.deleteIfExists(Paths.get(d.getFilePath()));
        } catch (Exception ignored) {
            // arquivo pode nao existir no disco, mas o registro precisa ser removido
        }

        docs.delete(d);
    }

    private void assertPodeAlterarOuExcluirDocumento(DocumentoVersao d, User atual) {
        Grupo g = d.getGrupo();
        perms.assertGrupoEmCurso(g);
        boolean pode = atual.getRole() == Role.ADMIN
                || perms.isOrientadorOuCoorientador(g, atual)
                || (d.getEnviadoPor() != null && d.getEnviadoPor().getId().equals(atual.getId()));
        if (!pode) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissao");
        }
    }

    /**
     * Valida o tipo do arquivo combinando 3 checagens:
     *   1) Extensao no nome original;
     *   2) Content-Type declarado pelo cliente;
     *   3) Magic bytes lidos do conteudo.
     * Apenas (3) e' confiavel contra um cliente malicioso, mas (1) e (2) eliminam
     * acidentes obvios e melhoram as mensagens de erro.
     */
    private void validarTipoArquivo(MultipartFile file) {
        String nomeOriginal = file.getOriginalFilename();
        String extensao = extrairExtensao(nomeOriginal).toLowerCase();
        if (!EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Extensao nao permitida. Permitidas: " + String.join(", ", EXTENSOES_PERMITIDAS));
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !MIME_PERMITIDOS.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de arquivo nao permitido (" + contentType + ")");
        }

        byte[] header = lerCabecalho(file, 8);
        if (!magicBytesValidos(header, extensao)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Conteudo do arquivo nao corresponde a extensao informada");
        }
    }

    private static String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal == null) {
            return "";
        }
        int idx = nomeOriginal.lastIndexOf('.');
        return idx >= 0 ? nomeOriginal.substring(idx) : "";
    }

    private static byte[] lerCabecalho(MultipartFile file, int tamanho) {
        byte[] buffer = new byte[tamanho];
        try (InputStream in = file.getInputStream()) {
            int lidos = in.read(buffer);
            if (lidos < tamanho) {
                byte[] aparado = new byte[Math.max(lidos, 0)];
                System.arraycopy(buffer, 0, aparado, 0, aparado.length);
                return aparado;
            }
            return buffer;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel ler o arquivo");
        }
    }

    private static boolean magicBytesValidos(byte[] header, String extensao) {
        return switch (extensao) {
            case ".pdf" -> startsWith(header, MAGIC_PDF);
            case ".docx", ".odt" -> startsWith(header, MAGIC_OFFICE_X);
            case ".doc" -> startsWith(header, MAGIC_DOC_OLE);
            case ".rtf" -> startsWith(header, MAGIC_RTF);
            // .txt nao tem magic byte; aceitamos qualquer conteudo (ja foi limitado por extensao + mime)
            case ".txt" -> true;
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data == null || prefix == null || data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
