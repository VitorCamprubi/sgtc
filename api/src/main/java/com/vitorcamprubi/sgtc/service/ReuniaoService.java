package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.*;
import com.vitorcamprubi.sgtc.repo.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReuniaoService {
    private final ReuniaoRepository repo;
    private final GrupoRepository grupos;
    private final PermissaoService perms;

    public ReuniaoService(ReuniaoRepository repo, GrupoRepository grupos, PermissaoService perms) {
        this.repo = repo; this.grupos = grupos; this.perms = perms;
    }

    public Reuniao agendar(Long grupoId, java.time.LocalDateTime dataHora,
                           String pauta, String observacoes, User atual) {
        var g = grupos.findById(grupoId).orElseThrow();
        perms.assertOrientadorOuCoorientadorDoGrupo(grupoId, atual); // só orient/coorient (ou admin)
        Reuniao r = new Reuniao();
        r.setGrupo(g); r.setDataHora(dataHora); r.setPauta(pauta); r.setObservacoes(observacoes); r.setCriadoPor(atual);
        return repo.save(r);
    }

    public List<Reuniao> listar(Long grupoId, User atual) {
        perms.assertPodeAcessarGrupo(grupoId, atual); // alunos, orientadores, admin veem
        return repo.findByGrupoIdOrderByDataHoraDesc(grupoId);
    }
}
