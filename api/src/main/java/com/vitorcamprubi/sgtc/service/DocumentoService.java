package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.DocumentoVersao;
import com.vitorcamprubi.sgtc.domain.Grupo;
import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class DocumentoService {
    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final DocumentoVersaoRepository docs;
    private final DocumentoComentarioRepository comentarios;
    private final PermissaoService perms;

    public DocumentoService(DocumentoVersaoRepository docs, DocumentoComentarioRepository comentarios, PermissaoService perms) {
        this.docs = docs;
        this.comentarios = comentarios;
        this.perms = perms;
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
        return docs.save(d);
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
}
