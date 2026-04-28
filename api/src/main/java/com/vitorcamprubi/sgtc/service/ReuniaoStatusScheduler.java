package com.vitorcamprubi.sgtc.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReuniaoStatusScheduler {
    private final ReuniaoService reuniaoService;

    public ReuniaoStatusScheduler(ReuniaoService reuniaoService) {
        this.reuniaoService = reuniaoService;
    }

    @Scheduled(fixedDelayString = "${app.reunioes.status-check-ms:3600000}")
    public void atualizarReunioesNaoRealizadas() {
        reuniaoService.fecharReunioesAtrasadasAutomaticamente();
    }
}
