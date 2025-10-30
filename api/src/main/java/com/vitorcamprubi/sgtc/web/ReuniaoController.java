package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.security.AuthService;
import com.vitorcamprubi.sgtc.service.ReuniaoService;
import com.vitorcamprubi.sgtc.web.dto.ReuniaoDTO;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReuniaoController {
    private final ReuniaoService service; private final AuthService auth;
    public ReuniaoController(ReuniaoService s, AuthService a){ this.service=s; this.auth=a; }

    public record AgendarReuniaoRequest(LocalDateTime dataHora, String pauta, String observacoes) {}

    @PostMapping("/grupos/{grupoId}/reunioes")
    public ReuniaoDTO agendar(@PathVariable Long grupoId, @RequestBody AgendarReuniaoRequest req) {
        var r = service.agendar(grupoId, req.dataHora(), req.pauta(), req.observacoes(), auth.getCurrentUser());
        return ReuniaoDTO.of(r);
    }

    @GetMapping("/grupos/{grupoId}/reunioes")
    public List<ReuniaoDTO> listar(@PathVariable Long grupoId) {
        return service.listar(grupoId, auth.getCurrentUser()).stream().map(ReuniaoDTO::of).toList();
    }
}
