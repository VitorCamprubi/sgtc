package com.vitorcamprubi.sgtc.web;

import com.vitorcamprubi.sgtc.domain.ReuniaoDesempenhoGrupo;
import com.vitorcamprubi.sgtc.security.AuthService;
import com.vitorcamprubi.sgtc.service.ReuniaoPdfService;
import com.vitorcamprubi.sgtc.service.ReuniaoService;
import com.vitorcamprubi.sgtc.web.dto.ReuniaoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReuniaoController {
    private final ReuniaoService service;
    private final ReuniaoPdfService pdfService;
    private final AuthService auth;

    public ReuniaoController(ReuniaoService s, ReuniaoPdfService pdfService, AuthService a) {
        this.service = s;
        this.pdfService = pdfService;
        this.auth = a;
    }

    public record AgendarReuniaoRequest(@NotNull LocalDateTime dataHora,
                                        @NotBlank String pauta,
                                        String observacoes) {
    }

    public record ConcluirReuniaoRequest(
            @NotNull LocalDate dataAtividadesRealizadas,
            @NotBlank @Size(max = ReuniaoService.MAX_ATIVIDADES_REALIZADAS) String atividadesRealizadas,
            @NotNull ReuniaoDesempenhoGrupo desempenhoGrupo,
            @NotBlank String professorDisciplina
    ) {
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
        var dados = new ReuniaoService.ExecucaoReuniaoDados(
                req.dataAtividadesRealizadas(),
                req.atividadesRealizadas(),
                req.desempenhoGrupo(),
                req.professorDisciplina()
        );
        var r = service.concluir(reuniaoId, dados, auth.getCurrentUser());
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

    @GetMapping("/grupos/{grupoId}/reunioes/pdf")
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long grupoId) {
        byte[] pdf = pdfService.gerarPdfExecutadasDoGrupo(grupoId, auth.getCurrentUser());
        String nomeArquivo = "reunioes-grupo-" + grupoId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(pdf);
    }
}
