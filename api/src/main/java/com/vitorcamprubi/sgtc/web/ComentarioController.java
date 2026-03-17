package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.security.AuthService;
import com.vitorcamprubi.sgtc.service.ComentarioService;
import com.vitorcamprubi.sgtc.web.dto.ComentarioDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ComentarioController {
    private final ComentarioService service;
    private final AuthService auth;

    public ComentarioController(ComentarioService s, AuthService a) {
        this.service = s;
        this.auth = a;
    }

    public record NovoComentarioRequest(@NotBlank String texto) {
    }

    @PostMapping("/documentos/{docId}/comentarios")
    public ComentarioDTO comentar(@PathVariable Long docId, @RequestBody @Valid NovoComentarioRequest req) {
        return ComentarioDTO.of(service.comentar(docId, req.texto(), auth.getCurrentUser()));
    }

    @GetMapping("/documentos/{docId}/comentarios")
    public List<ComentarioDTO> listar(@PathVariable Long docId) {
        return service.listar(docId, auth.getCurrentUser()).stream().map(ComentarioDTO::of).toList();
    }

    @PutMapping("/comentarios/{comentarioId}")
    public ComentarioDTO atualizar(@PathVariable Long comentarioId, @RequestBody @Valid NovoComentarioRequest req) {
        return ComentarioDTO.of(service.atualizar(comentarioId, req.texto(), auth.getCurrentUser()));
    }

    @DeleteMapping("/comentarios/{comentarioId}")
    public void excluir(@PathVariable Long comentarioId) {
        service.excluir(comentarioId, auth.getCurrentUser());
    }
}
