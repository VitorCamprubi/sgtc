package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.ReuniaoStatus;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.ReuniaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReuniaoService {
    private final ReuniaoRepository repo;
    private final PermissaoService perms;

    public ReuniaoService(ReuniaoRepository repo, PermissaoService perms) {
        this.repo = repo;
        this.perms = perms;
    }

    @Transactional
    public Reuniao agendar(Long grupoId, LocalDateTime dataHora,
                           String pauta, String observacoes, User atual) {
        if (dataHora == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data/hora eh obrigatoria");
        }
        if (pauta == null || pauta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pauta eh obrigatoria");
        }

        var g = perms.assertPodeAcessarGrupo(grupoId, atual);

        Reuniao r = new Reuniao();
        r.setGrupo(g);
        r.setDataHora(dataHora);
        r.setPauta(pauta.trim());
        r.setObservacoes(observacoes == null || observacoes.isBlank() ? null : observacoes.trim());
        r.setStatus(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);
        r.setRelatorio(null);
        r.setEncerradaEm(null);
        r.setCriadoPor(atual);
        return repo.save(r);
    }

    @Transactional
    public List<Reuniao> listar(Long grupoId, User atual) {
        perms.assertPodeAcessarGrupo(grupoId, atual);
        fecharReunioesAtrasadasDoGrupo(grupoId);

        var reunioes = repo.findByGrupoIdOrderByDataHoraDesc(grupoId);
        var paraSalvar = new ArrayList<Reuniao>();
        for (Reuniao reuniao : reunioes) {
            if (normalizarStatus(reuniao)) {
                paraSalvar.add(reuniao);
            }
        }
        if (!paraSalvar.isEmpty()) {
            repo.saveAll(paraSalvar);
        }
        return reunioes;
    }

    @Transactional
    public Reuniao remarcar(Long reuniaoId, LocalDateTime dataHora, String pauta, String observacoes, User atual) {
        if (dataHora == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data/hora eh obrigatoria");
        }
        if (pauta == null || pauta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pauta eh obrigatoria");
        }

        Reuniao r = carregar(reuniaoId);
        atualizarStatusNaoRealizadaSeAtrasada(r, LocalDateTime.now());
        perms.assertOrientadorOuCoorientadorDoGrupo(r.getGrupo().getId(), atual);
        assertAberta(r);

        r.setDataHora(dataHora);
        r.setPauta(pauta.trim());
        r.setObservacoes(observacoes == null || observacoes.isBlank() ? null : observacoes.trim());
        return repo.save(r);
    }

    @Transactional
    public Reuniao concluir(Long reuniaoId, String relatorio, User atual) {
        if (relatorio == null || relatorio.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Relatorio eh obrigatorio para concluir");
        }

        Reuniao r = carregar(reuniaoId);
        atualizarStatusNaoRealizadaSeAtrasada(r, LocalDateTime.now());
        perms.assertOrientadorOuCoorientadorDoGrupo(r.getGrupo().getId(), atual);
        assertAberta(r);

        r.setStatus(ReuniaoStatus.CONCLUIDA);
        r.setRelatorio(relatorio.trim());
        r.setEncerradaEm(LocalDateTime.now());
        return repo.save(r);
    }

    @Transactional
    public Reuniao cancelar(Long reuniaoId, User atual) {
        Reuniao r = carregar(reuniaoId);
        atualizarStatusNaoRealizadaSeAtrasada(r, LocalDateTime.now());
        perms.assertOrientadorOuCoorientadorDoGrupo(r.getGrupo().getId(), atual);
        assertAberta(r);

        r.setStatus(ReuniaoStatus.CANCELADA);
        r.setEncerradaEm(LocalDateTime.now());
        r.setRelatorio(null);
        return repo.save(r);
    }

    @Transactional
    public int fecharReunioesAtrasadasAutomaticamente() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limite = now.minusWeeks(1);
        List<Reuniao> atrasadas = repo.findAtrasadas(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO, limite);

        var paraSalvar = new ArrayList<Reuniao>();
        for (Reuniao reuniao : atrasadas) {
            if (atualizarStatusNaoRealizadaSeAtrasada(reuniao, now)) {
                paraSalvar.add(reuniao);
            }
        }
        if (!paraSalvar.isEmpty()) {
            repo.saveAll(paraSalvar);
        }
        return paraSalvar.size();
    }

    @Transactional
    public Reuniao atualizar(Long reuniaoId, LocalDateTime dataHora, String pauta, String observacoes, User atual) {
        return remarcar(reuniaoId, dataHora, pauta, observacoes, atual);
    }

    @Transactional
    public void excluir(Long reuniaoId, User atual) {
        cancelar(reuniaoId, atual);
    }

    private Reuniao carregar(Long reuniaoId) {
        Reuniao r = repo.findById(reuniaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reuniao nao encontrada"));
        normalizarStatus(r);
        return r;
    }

    private void assertAberta(Reuniao reuniao) {
        if (reuniao.getStatus() != ReuniaoStatus.AGUARDANDO_DATA_REUNIAO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reuniao ja encerrada");
        }
    }

    private boolean normalizarStatus(Reuniao reuniao) {
        if (reuniao.getStatus() == null) {
            reuniao.setStatus(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);
            return true;
        }
        return false;
    }

    private boolean atualizarStatusNaoRealizadaSeAtrasada(Reuniao reuniao, LocalDateTime agora) {
        normalizarStatus(reuniao);
        if (reuniao.getStatus() != ReuniaoStatus.AGUARDANDO_DATA_REUNIAO) {
            return false;
        }
        if (reuniao.getDataHora() == null || reuniao.getDataHora().isAfter(agora.minusWeeks(1))) {
            return false;
        }
        reuniao.setStatus(ReuniaoStatus.NAO_REALIZADA);
        reuniao.setEncerradaEm(agora);
        reuniao.setRelatorio(null);
        return true;
    }

    private void fecharReunioesAtrasadasDoGrupo(Long grupoId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limite = now.minusWeeks(1);
        List<Reuniao> atrasadas = repo.findAtrasadasDoGrupo(grupoId, ReuniaoStatus.AGUARDANDO_DATA_REUNIAO, limite);

        var paraSalvar = new ArrayList<Reuniao>();
        for (Reuniao reuniao : atrasadas) {
            if (atualizarStatusNaoRealizadaSeAtrasada(reuniao, now)) {
                paraSalvar.add(reuniao);
            }
        }
        if (!paraSalvar.isEmpty()) {
            repo.saveAll(paraSalvar);
        }
    }
}
