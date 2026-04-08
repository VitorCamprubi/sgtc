package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.Grupo;
import com.vitorcamprubi.sgtc.domain.GrupoAluno;
import com.vitorcamprubi.sgtc.domain.Materia;
import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.DocumentoVersaoRepository;
import com.vitorcamprubi.sgtc.repo.GrupoAlunoRepository;
import com.vitorcamprubi.sgtc.repo.GrupoRepository;
import com.vitorcamprubi.sgtc.repo.ReuniaoRepository;
import com.vitorcamprubi.sgtc.repo.UserRepository;
import com.vitorcamprubi.sgtc.web.dto.GrupoCreateRequest;
import com.vitorcamprubi.sgtc.web.dto.GrupoResumoDTO;
import com.vitorcamprubi.sgtc.web.dto.UserAdminDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class GrupoService {

    private final GrupoRepository grupos;
    private final UserRepository users;
    private final GrupoAlunoRepository grupoAlunos;
    private final DocumentoVersaoRepository documentos;
    private final ReuniaoRepository reunioes;
    private final DocumentoService documentoService;
    private final PermissaoService perms;

    public GrupoService(GrupoRepository grupos, UserRepository users, GrupoAlunoRepository grupoAlunos,
                        DocumentoVersaoRepository documentos, ReuniaoRepository reunioes,
                        DocumentoService documentoService, PermissaoService perms) {
        this.grupos = grupos;
        this.users = users;
        this.grupoAlunos = grupoAlunos;
        this.documentos = documentos;
        this.reunioes = reunioes;
        this.documentoService = documentoService;
        this.perms = perms;
    }

    @Transactional
    public GrupoResumoDTO criar(GrupoCreateRequest req) {
        Grupo g = new Grupo();
        g.setId(proximoIdDisponivel());
        preencherDadosGrupo(g, req);
        g = grupos.save(g);
        return toResumo(g);
    }

    @Transactional
    public GrupoResumoDTO atualizar(Long grupoId, GrupoCreateRequest req) {
        Grupo g = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));

        preencherDadosGrupo(g, req);
        return toResumo(grupos.save(g));
    }

    @Transactional
    public void adicionarMembros(Long grupoId, List<Long> alunosIds) {
        if (alunosIds == null || alunosIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos um aluno");
        }

        Grupo g = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));

        for (Long alunoId : new LinkedHashSet<>(alunosIds)) {
            User u = buscaUser(alunoId);
            if (u.getRole() != Role.ALUNO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario " + alunoId + " nao eh ALUNO");
            }
            if (!grupoAlunos.existsByGrupoIdAndAlunoId(grupoId, alunoId)) {
                GrupoAluno ga = new GrupoAluno();
                ga.setGrupo(g);
                ga.setAluno(u);
                grupoAlunos.save(ga);
            }
        }
    }

    @Transactional
    public void atualizarMembros(Long grupoId, List<Long> alunosIds) {
        if (alunosIds == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista de alunos obrigatoria");
        }

        grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));

        grupoAlunos.deleteByGrupoId(grupoId);

        Set<Long> idsDistintos = new LinkedHashSet<>(alunosIds);
        if (idsDistintos.isEmpty()) {
            return;
        }

        adicionarMembros(grupoId, idsDistintos.stream().toList());
    }

    public List<GrupoResumoDTO> listarDoUsuario(User atual) {
        List<Grupo> lista;
        if (atual.getRole() == Role.ADMIN) {
            lista = grupos.findAll();
        } else if (atual.getRole() == Role.PROFESSOR) {
            lista = grupos.findByOrientadorIdOrCoorientadorId(atual.getId(), atual.getId());
        } else {
            lista = grupos.findByAlunoId(atual.getId());
        }
        return lista.stream().map(this::toResumo).toList();
    }

    public GrupoResumoDTO obterResumo(Long grupoId, User atual) {
        Grupo grupo = perms.assertPodeAcessarGrupo(grupoId, atual);
        return toResumo(grupo);
    }

    public List<UserAdminDTO> listarMembros(Long grupoId, User atual) {
        perms.assertPodeAcessarGrupo(grupoId, atual);
        return grupoAlunos.findAlunosByGrupoId(grupoId).stream().map(UserAdminDTO::of).toList();
    }

    @Transactional
    public void removerMembro(Long grupoId, Long alunoId) {
        grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));

        User aluno = buscaUser(alunoId);
        if (aluno.getRole() != Role.ALUNO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario informado nao eh ALUNO");
        }

        int removidos = grupoAlunos.deleteByGrupoIdAndAlunoId(grupoId, alunoId);
        if (removidos == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro nao encontrado no grupo");
        }
    }

    @Transactional
    public void excluir(Long grupoId, User atual) {
        if (atual.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas ADMIN pode excluir grupo");
        }

        Grupo g = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));

        var docs = documentos.findByGrupoIdOrderByVersaoDesc(grupoId);
        for (var d : docs) {
            try {
                documentoService.delete(d.getId(), atual);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao remover documentos", e);
            }
        }

        reunioes.deleteByGrupoId(grupoId);
        grupoAlunos.deleteByGrupoId(grupoId);
        grupos.delete(g);
    }

    private User buscaUser(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado: " + id));
    }

    private User buscaProfessor(Long id, String campo) {
        User u = buscaUser(id);
        if (u.getRole() != Role.PROFESSOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario informado para " + campo + " nao eh professor");
        }
        return u;
    }

    private void preencherDadosGrupo(Grupo g, GrupoCreateRequest req) {
        if (req.getTitulo() == null || req.getTitulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Titulo eh obrigatorio");
        }
        if (req.getOrientadorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Orientador eh obrigatorio");
        }
        if (req.getMateria() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Materia eh obrigatoria");
        }
        if (!(req.getMateria() == Materia.TG || req.getMateria() == Materia.PTG)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Materia invalida. Use TG ou PTG");
        }

        g.setTitulo(req.getTitulo().trim());
        g.setMateria(req.getMateria());
        g.setOrientador(buscaProfessor(req.getOrientadorId(), "orientador"));
        g.setCoorientador(null);

        if (req.getCoorientadorId() != null) {
            g.setCoorientador(buscaProfessor(req.getCoorientadorId(), "coorientador"));
        }

        if (g.getCoorientador() != null && g.getOrientador().getId().equals(g.getCoorientador().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O mesmo professor nao pode ser orientador e coorientador no mesmo grupo");
        }
    }

    private GrupoResumoDTO toResumo(Grupo g) {
        long count = grupoAlunos.countByGrupoId(g.getId());
        return new GrupoResumoDTO(
                g.getId(),
                g.getTitulo(),
                g.getMateria(),
                g.getOrientador() != null ? g.getOrientador().getId() : null,
                g.getOrientador() != null ? g.getOrientador().getNome() : null,
                g.getCoorientador() != null ? g.getCoorientador().getId() : null,
                g.getCoorientador() != null ? g.getCoorientador().getNome() : null,
                count
        );
    }

    private long proximoIdDisponivel() {
        List<Long> ids = grupos.findAllIdsOrderByIdAsc();
        long esperado = 1L;
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            if (id > esperado) {
                break;
            }
            if (id.equals(esperado)) {
                esperado++;
            }
        }
        return esperado;
    }
}
