package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.GrupoRepository;
import com.vitorcamprubi.sgtc.repo.ReuniaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReuniaoService {
    private final ReuniaoRepository repo;
    private final GrupoRepository grupos;
    private final PermissaoService perms;

    public ReuniaoService(ReuniaoRepository repo, GrupoRepository grupos, PermissaoService perms) {
        this.repo = repo;
        this.grupos = grupos;
        this.perms = perms;
    }

    public Reuniao agendar(Long grupoId, LocalDateTime dataHora,
                           String pauta, String observacoes, User atual) {
        if (dataHora == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data/hora eh obrigatoria");
        }
        if (pauta == null || pauta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pauta eh obrigatoria");
        }

        var g = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));

        perms.assertOrientadorOuCoorientadorDoGrupo(grupoId, atual);

        Reuniao r = new Reuniao();
        r.setGrupo(g);
        r.setDataHora(dataHora);
        r.setPauta(pauta.trim());
        r.setObservacoes(observacoes == null || observacoes.isBlank() ? null : observacoes.trim());
        r.setCriadoPor(atual);
        return repo.save(r);
    }

    public List<Reuniao> listar(Long grupoId, User atual) {
        perms.assertPodeAcessarGrupo(grupoId, atual);
        return repo.findByGrupoIdOrderByDataHoraDesc(grupoId);
    }

    public Reuniao atualizar(Long reuniaoId, LocalDateTime dataHora, String pauta, String observacoes, User atual) {
        if (dataHora == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data/hora eh obrigatoria");
        }
        if (pauta == null || pauta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pauta eh obrigatoria");
        }

        Reuniao r = repo.findById(reuniaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reuniao nao encontrada"));

        perms.assertOrientadorOuCoorientadorDoGrupo(r.getGrupo().getId(), atual);
        r.setDataHora(dataHora);
        r.setPauta(pauta.trim());
        r.setObservacoes(observacoes == null || observacoes.isBlank() ? null : observacoes.trim());
        return repo.save(r);
    }

    public void excluir(Long reuniaoId, User atual) {
        Reuniao r = repo.findById(reuniaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reuniao nao encontrada"));

        perms.assertOrientadorOuCoorientadorDoGrupo(r.getGrupo().getId(), atual);
        repo.delete(r);
    }
}
