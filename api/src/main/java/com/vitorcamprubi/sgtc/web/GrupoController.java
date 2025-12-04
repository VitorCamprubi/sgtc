package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.security.AuthService;
import com.vitorcamprubi.sgtc.service.GrupoService;
import com.vitorcamprubi.sgtc.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos")
public class GrupoController {

    private final GrupoService service;
    private final AuthService auth;

    public GrupoController(GrupoService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public GrupoResumoDTO criar(@RequestBody @Valid GrupoCreateRequest req) {
        return service.criar(req);
    }

    @GetMapping("/me")
    public List<GrupoResumoDTO> meusGrupos() {
        return service.listarDoUsuario(auth.getCurrentUser());
    }

    @PostMapping("/{id}/membros")
    @PreAuthorize("hasRole('ADMIN')")
    public void adicionarMembros(@PathVariable Long id, @RequestBody @Valid AddMembrosRequest req) {
        service.adicionarMembros(id, req.getAlunosIds());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(@PathVariable Long id) {
        service.excluir(id, auth.getCurrentUser());
    }
}
