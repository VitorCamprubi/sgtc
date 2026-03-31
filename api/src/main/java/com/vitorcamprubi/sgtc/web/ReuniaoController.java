package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.security.AuthService;
import com.vitorcamprubi.sgtc.service.ReuniaoService;
import com.vitorcamprubi.sgtc.web.dto.ReuniaoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReuniaoController {
    private final ReuniaoService service;
    private final AuthService auth;

    public ReuniaoController(ReuniaoService s, AuthService a) {
        this.service = s;
        this.auth = a;
    }

    public record AgendarReuniaoRequest(@NotNull LocalDateTime dataHora,
                                        @NotBlank String pauta,
                                        String observacoes) {
    }

    public record ConcluirReuniaoRequest(@NotBlank String relatorio) {
    }

    @PostMapping("/grupos/{grupoId}/reunioes")
    public ReuniaoDTO agendar(@PathVariable Long grupoId, @RequestBody @Valid AgendarReuniaoRequest req) {
        var r = service.agendar(grupoId, req.dataHora(), req.pauta(), req.observacoes(), auth.getCurrentUser());
        return ReuniaoDTO.of(r);
    }

    @PutMapping("/reunioes/{reuniaoId}")
    public ReuniaoDTO atualizar(@PathVariable Long reuniaoId, @RequestBody @Valid AgendarReuniaoRequest req) {
        var r = service.remarcar(reuniaoId, req.dataHora(), req.pauta(), req.observacoes(), auth.getCurrentUser());
        return ReuniaoDTO.of(r);
    }

    @PostMapping("/reunioes/{reuniaoId}/concluir")
    public ReuniaoDTO concluir(@PathVariable Long reuniaoId, @RequestBody @Valid ConcluirReuniaoRequest req) {
        var r = service.concluir(reuniaoId, req.relatorio(), auth.getCurrentUser());
        return ReuniaoDTO.of(r);
    }

    @PostMapping("/reunioes/{reuniaoId}/cancelar")
    public ReuniaoDTO cancelar(@PathVariable Long reuniaoId) {
        var r = service.cancelar(reuniaoId, auth.getCurrentUser());
        return ReuniaoDTO.of(r);
    }

    @DeleteMapping("/reunioes/{reuniaoId}")
    public void excluir(@PathVariable Long reuniaoId) {
        service.excluir(reuniaoId, auth.getCurrentUser());
    }

    @GetMapping("/grupos/{grupoId}/reunioes")
    public List<ReuniaoDTO> listar(@PathVariable Long grupoId) {
        return service.listar(grupoId, auth.getCurrentUser()).stream().map(ReuniaoDTO::of).toList();
    }
}
