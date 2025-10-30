package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.*;
import com.vitorcamprubi.sgtc.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class ComentarioService {
    private final DocumentoVersaoRepository docs;
    private final DocumentoComentarioRepository repo;
    private final PermissaoService perms;

    public ComentarioService(DocumentoVersaoRepository docs, DocumentoComentarioRepository repo, PermissaoService perms) {
        this.docs = docs; this.repo = repo; this.perms = perms;
    }

    public DocumentoComentario comentar(Long docId, String texto, User atual) {
        DocumentoVersao d = docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
        // só orientador/coorientador (ou admin) pode COMENTAR
        perms.assertOrientadorOuCoorientadorDoGrupo(d.getGrupo().getId(), atual);

        DocumentoComentario c = new DocumentoComentario();
        c.setDocumento(d);
        c.setAutor(atual);
        c.setTexto(texto);
        return repo.save(c);
    }

    public List<DocumentoComentario> listar(Long docId, User atual) {
        DocumentoVersao d = docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
        // todos que podem acessar o grupo (alunos, orientadores, admin) PODEM LER
        perms.assertPodeAcessarGrupo(d.getGrupo().getId(), atual);
        return repo.findByDocumentoIdOrderByCreatedAtAsc(docId);
    }
}
