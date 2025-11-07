package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.*;
import com.vitorcamprubi.sgtc.repo.DocumentoVersaoRepository;
import com.vitorcamprubi.sgtc.repo.DocumentoComentarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
public class DocumentoService {
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final DocumentoVersaoRepository docs;
    private final DocumentoComentarioRepository comentarios;
    private final PermissaoService perms;

    public DocumentoService(DocumentoVersaoRepository docs, DocumentoComentarioRepository comentarios, PermissaoService perms) {
        this.docs = docs; this.comentarios = comentarios; this.perms = perms;
    }

    @Transactional
    public DocumentoVersao upload(Long grupoId, String titulo, MultipartFile file, User atual) throws IOException {
        Grupo g = perms.assertPodeAcessarGrupo(grupoId, atual);
        int next = docs.countByGrupoId(grupoId) + 1;

        Path dir = Paths.get(uploadDir, String.valueOf(grupoId));
        Files.createDirectories(dir);
        String original = file.getOriginalFilename()==null ? "arquivo" : file.getOriginalFilename();
        String filename = "v" + next + "_" + original.replaceAll("\\s+", "_");
        Path dest = dir.resolve(filename);
        file.transferTo(dest);

        DocumentoVersao d = new DocumentoVersao();
        d.setGrupo(g); d.setTitulo(titulo); d.setVersao(next);
        d.setFilePath(dest.toString());
        d.setMimeType(Files.probeContentType(dest));
        d.setTamanho(Files.size(dest));
        d.setEnviadoPor(atual);
        return docs.save(d);
    }

    public List<DocumentoVersao> listar(Long grupoId, User atual) {
        perms.assertPodeAcessarGrupo(grupoId, atual);
        return docs.findByGrupoIdOrderByVersaoDesc(grupoId);
    }

    public ResponseEntity<Resource> download(Long docId, User atual) throws IOException {
        DocumentoVersao d = docs.findById(docId).orElseThrow();
        perms.assertPodeAcessarGrupo(d.getGrupo().getId(), atual);
        Path path = Paths.get(d.getFilePath());
        Resource res = new UrlResource(path.toUri());
        String ct = d.getMimeType()!=null ? d.getMimeType() : "application/octet-stream";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+path.getFileName()+"\"")
                .contentLength(Files.size(path))
                .contentType(MediaType.parseMediaType(ct))
                .body(res);
    }

    @Transactional
    public void delete(Long docId, User atual) throws IOException {
        DocumentoVersao d = docs.findById(docId).orElseThrow();
        Grupo g = d.getGrupo();
        boolean pode = atual.getRole() == Role.ADMIN
                || perms.isOrientadorOuCoorientador(g, atual)
                || (d.getEnviadoPor() != null && d.getEnviadoPor().getId().equals(atual.getId()));
        if (!pode) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Sem permissão");
        }
        // remove comentários vinculados
        comentarios.deleteByDocumentoId(docId);
        // apaga arquivo físico
        try { Files.deleteIfExists(Paths.get(d.getFilePath())); } catch (Exception ignored) {}
        // remove registro
        docs.delete(d);
    }
}
