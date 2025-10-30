package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.security.AuthService;
import com.vitorcamprubi.sgtc.service.ComentarioService;
import com.vitorcamprubi.sgtc.web.dto.ComentarioDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ComentarioController {
    private final ComentarioService service; private final AuthService auth;
    public ComentarioController(ComentarioService s, AuthService a){ this.service=s; this.auth=a; }

    record NovoComentarioRequest(String texto) {}

    @PostMapping("/documentos/{docId}/comentarios")
    public ComentarioDTO comentar(@PathVariable Long docId, @RequestBody NovoComentarioRequest req) {
        return ComentarioDTO.of(service.comentar(docId, req.texto(), auth.getCurrentUser()));
    }

    @GetMapping("/documentos/{docId}/comentarios")
    public List<ComentarioDTO> listar(@PathVariable Long docId) {
        return service.listar(docId, auth.getCurrentUser()).stream().map(ComentarioDTO::of).toList();
    }
}
