package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.*;
import com.vitorcamprubi.sgtc.repo.*;
import com.vitorcamprubi.sgtc.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GrupoService {

    private final GrupoRepository grupos;
    private final UserRepository users;
    private final GrupoAlunoRepository grupoAlunos;

    public GrupoService(GrupoRepository grupos, UserRepository users, GrupoAlunoRepository grupoAlunos) {
        this.grupos = grupos; this.users = users; this.grupoAlunos = grupoAlunos;
    }

    @Transactional
    public GrupoResumoDTO criar(GrupoCreateRequest req) {
        Grupo g = new Grupo();
        g.setTitulo(req.getTitulo());
        g.setOrientador(buscaUser(req.getOrientadorId()));
        if (req.getCoorientadorId() != null) {
            g.setCoorientador(buscaUser(req.getCoorientadorId()));
        }
        g = grupos.save(g);
        return toResumo(g);
    }

    @Transactional
    public void adicionarMembros(Long grupoId, List<Long> alunosIds) {
        Grupo g = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo não encontrado"));
        for (Long alunoId : alunosIds) {
            User u = buscaUser(alunoId);
            if (u.getRole() != Role.ALUNO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário "+alunoId+" não é ALUNO");
            }
            if (!grupoAlunos.existsByGrupoIdAndAlunoId(grupoId, alunoId)) {
                GrupoAluno ga = new GrupoAluno();
                ga.setGrupo(g); ga.setAluno(u);
                grupoAlunos.save(ga);
            }
        }
    }

    public List<GrupoResumoDTO> listarDoUsuario(User atual) {
        List<Grupo> lista;
        if (atual.getRole() == Role.ADMIN) {
            lista = grupos.findAll();
        } else if (atual.getRole() == Role.ORIENTADOR || atual.getRole() == Role.COORIENTADOR) {
            lista = grupos.findByOrientadorIdOrCoorientadorId(atual.getId(), atual.getId());
        } else { // ALUNO
            lista = grupos.findByAlunoId(atual.getId());
        }
        return lista.stream().map(this::toResumo).toList();
    }

    private User buscaUser(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: "+id));
    }

    private GrupoResumoDTO toResumo(Grupo g) {
        long count = grupoAlunos.countByGrupoId(g.getId());
        return new GrupoResumoDTO(
                g.getId(),
                g.getTitulo(),
                g.getOrientador()!=null?g.getOrientador().getNome():null,
                g.getCoorientador()!=null?g.getCoorientador().getNome():null,
                count
        );
    }
}
