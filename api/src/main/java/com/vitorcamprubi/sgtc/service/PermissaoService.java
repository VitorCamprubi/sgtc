package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.*;
import com.vitorcamprubi.sgtc.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PermissaoService {
    private final GrupoRepository grupos; private final GrupoAlunoRepository ga;
    public PermissaoService(GrupoRepository grupos, GrupoAlunoRepository ga){ this.grupos=grupos; this.ga=ga; }

    public Grupo assertPodeAcessarGrupo(Long grupoId, User atual) {
        Grupo g = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo não encontrado"));
        boolean ok = atual.getRole()==Role.ADMIN
                || ga.existsByGrupoIdAndAlunoId(grupoId, atual.getId())
                || (g.getOrientador()!=null && g.getOrientador().getId().equals(atual.getId()))
                || (g.getCoorientador()!=null && g.getCoorientador().getId().equals(atual.getId()));
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão");
        return g;
    }
    public boolean isOrientadorOuCoorientador(Grupo g, User u) {
        return (g.getOrientador()!=null && g.getOrientador().getId().equals(u.getId())) ||
                (g.getCoorientador()!=null && g.getCoorientador().getId().equals(u.getId()));
    }

    public void assertOrientadorOuCoorientadorDoGrupo(Long grupoId, User atual) {
        Grupo g = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo não encontrado"));
        if (!(atual.getRole()==Role.ADMIN || isOrientadorOuCoorientador(g, atual))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas orientador/coorientador");
        }
    }
}
