package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.service.UserAdminService;
import com.vitorcamprubi.sgtc.web.dto.UserAdminDTO;
import com.vitorcamprubi.sgtc.web.dto.UserAdminRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserAdminDTO> listar(@RequestParam(required = false) Role role) {
        return service.listar(role);
    }

    @PostMapping
    public UserAdminDTO criar(@RequestBody @Valid UserAdminRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public UserAdminDTO atualizar(@PathVariable Long id, @RequestBody @Valid UserAdminRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
