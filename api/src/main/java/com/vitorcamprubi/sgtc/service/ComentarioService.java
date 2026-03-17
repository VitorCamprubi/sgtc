package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.DocumentoComentario;
import com.vitorcamprubi.sgtc.domain.DocumentoVersao;
import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.DocumentoComentarioRepository;
import com.vitorcamprubi.sgtc.repo.DocumentoVersaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ComentarioService {
    private final DocumentoVersaoRepository docs;
    private final DocumentoComentarioRepository repo;
    private final PermissaoService perms;

    public ComentarioService(DocumentoVersaoRepository docs, DocumentoComentarioRepository repo, PermissaoService perms) {
        this.docs = docs;
        this.repo = repo;
        this.perms = perms;
    }

    public DocumentoComentario comentar(Long docId, String texto, User atual) {
        if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comentario nao pode ser vazio");
        }

        DocumentoVersao d = docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento nao encontrado"));

        perms.assertOrientadorOuCoorientadorDoGrupo(d.getGrupo().getId(), atual);

        DocumentoComentario c = new DocumentoComentario();
        c.setDocumento(d);
        c.setAutor(atual);
        c.setTexto(texto.trim());
        return repo.save(c);
    }

    public List<DocumentoComentario> listar(Long docId, User atual) {
        DocumentoVersao d = docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento nao encontrado"));

        perms.assertPodeAcessarGrupo(d.getGrupo().getId(), atual);
        return repo.findByDocumentoIdOrderByCreatedAtAsc(docId);
    }

    public DocumentoComentario atualizar(Long comentarioId, String texto, User atual) {
        if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comentario nao pode ser vazio");
        }

        DocumentoComentario c = repo.findById(comentarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario nao encontrado"));

        perms.assertPodeAcessarGrupo(c.getDocumento().getGrupo().getId(), atual);
        assertPodeAlterarOuExcluir(c, atual);

        c.setTexto(texto.trim());
        return repo.save(c);
    }

    public void excluir(Long comentarioId, User atual) {
        DocumentoComentario c = repo.findById(comentarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario nao encontrado"));

        perms.assertPodeAcessarGrupo(c.getDocumento().getGrupo().getId(), atual);
        assertPodeAlterarOuExcluir(c, atual);
        repo.delete(c);
    }

    private void assertPodeAlterarOuExcluir(DocumentoComentario comentario, User atual) {
        boolean pode = atual.getRole() == Role.ADMIN
                || (comentario.getAutor() != null && comentario.getAutor().getId().equals(atual.getId()));
        if (!pode) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissao");
        }
    }
}
