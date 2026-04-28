package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.Grupo;
import com.vitorcamprubi.sgtc.domain.GrupoAluno;
import com.vitorcamprubi.sgtc.domain.GrupoAlunoStatus;
import com.vitorcamprubi.sgtc.domain.GrupoStatus;
import com.vitorcamprubi.sgtc.domain.Materia;
import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
<<<<<<< HEAD
import com.vitorcamprubi.sgtc.notification.EmailService;
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@Service
public class GrupoService {

    private final GrupoRepository grupos;
    private final UserRepository users;
    private final GrupoAlunoRepository grupoAlunos;
    private final DocumentoVersaoRepository documentos;
    private final ReuniaoRepository reunioes;
    private final DocumentoService documentoService;
    private final PermissaoService perms;
<<<<<<< HEAD
    private final EmailService emailService;

    public GrupoService(GrupoRepository grupos, UserRepository users, GrupoAlunoRepository grupoAlunos,
                        DocumentoVersaoRepository documentos, ReuniaoRepository reunioes,
                        DocumentoService documentoService, PermissaoService perms,
                        EmailService emailService) {
=======

    public GrupoService(GrupoRepository grupos, UserRepository users, GrupoAlunoRepository grupoAlunos,
                        DocumentoVersaoRepository documentos, ReuniaoRepository reunioes,
                        DocumentoService documentoService, PermissaoService perms) {
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
        this.grupos = grupos;
        this.users = users;
        this.grupoAlunos = grupoAlunos;
        this.documentos = documentos;
        this.reunioes = reunioes;
        this.documentoService = documentoService;
        this.perms = perms;
<<<<<<< HEAD
        this.emailService = emailService;
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
    }

    @Transactional
    public GrupoResumoDTO criar(GrupoCreateRequest req) {
        Grupo g = new Grupo();
        g.setId(proximoIdDisponivel());
        preencherDadosGrupo(g, req);
        g.setStatus(GrupoStatus.EM_CURSO);
        g.setNotaFinal(null);
        g.setArquivadoEm(null);
        g = grupos.save(g);
        return toResumo(g);
    }

    @Transactional
    public GrupoResumoDTO atualizar(Long grupoId, GrupoCreateRequest req) {
        Grupo g = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));
        normalizarStatusGrupo(g);
        perms.assertGrupoEmCurso(g);

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
        normalizarStatusGrupo(g);
        perms.assertGrupoEmCurso(g);

        for (Long alunoId : new LinkedHashSet<>(alunosIds)) {
            User u = buscaUser(alunoId);
            if (u.getRole() != Role.ALUNO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario " + alunoId + " nao eh ALUNO");
            }
            assertAlunoElegivelParaGrupo(u, g);
            if (!grupoAlunos.existsByGrupoIdAndAlunoId(grupoId, alunoId)) {
                GrupoAluno ga = new GrupoAluno();
                ga.setGrupo(g);
                ga.setAluno(u);
                ga.setStatus(GrupoAlunoStatus.EM_CURSO);
                grupoAlunos.save(ga);
            }
        }
    }

    @Transactional
    public void atualizarMembros(Long grupoId, List<Long> alunosIds) {
        if (alunosIds == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista de alunos obrigatoria");
        }

        Grupo grupo = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));
        normalizarStatusGrupo(grupo);
        perms.assertGrupoEmCurso(grupo);

        grupoAlunos.deleteByGrupoId(grupoId);

        Set<Long> idsDistintos = new LinkedHashSet<>(alunosIds);
        if (idsDistintos.isEmpty()) {
            return;
        }

        adicionarMembros(grupoId, idsDistintos.stream().toList());
    }

    public List<GrupoResumoDTO> listarDoUsuario(User atual) {
        return listarPorStatusDoUsuario(atual, null, GrupoStatus.EM_CURSO);
    }

    public List<GrupoResumoDTO> listarArquivadosDoUsuario(User atual, String busca) {
        if (atual.getRole() == Role.ALUNO) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente professores e administradores podem acessar arquivos"
            );
        }
        return listarPorStatusDoUsuario(atual, busca, GrupoStatus.APROVADO, GrupoStatus.REPROVADO);
    }

    public List<GrupoResumoDTO> listarArquivadosAprovadosDoUsuario(User atual, String busca) {
        if (atual.getRole() == Role.ALUNO) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente professores e administradores podem acessar arquivos"
            );
        }
        return listarPorStatusDoUsuario(atual, busca, GrupoStatus.APROVADO);
    }

    public List<GrupoResumoDTO> listarArquivadosReprovadosDoUsuario(User atual, String busca) {
        if (atual.getRole() == Role.ALUNO) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente professores e administradores podem acessar arquivos"
            );
        }
        return listarPorStatusDoUsuario(atual, busca, GrupoStatus.REPROVADO);
    }

    public GrupoResumoDTO obterResumo(Long grupoId, User atual) {
        Grupo grupo = perms.assertPodeAcessarGrupo(grupoId, atual);
        if (normalizarStatusGrupo(grupo)) {
            grupo = grupos.save(grupo);
        }
        return toResumo(grupo);
    }

    public List<UserAdminDTO> listarMembros(Long grupoId, User atual) {
        perms.assertPodeAcessarGrupo(grupoId, atual);
        return grupoAlunos.findAlunosByGrupoId(grupoId).stream().map(UserAdminDTO::of).toList();
    }

    @Transactional
    public void removerMembro(Long grupoId, Long alunoId) {
        Grupo grupo = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));
        normalizarStatusGrupo(grupo);
        perms.assertGrupoEmCurso(grupo);

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
        normalizarStatusGrupo(g);
        if (perms.isGrupoArquivado(g)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Grupo arquivado nao pode ser excluido"
            );
        }

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

    @Transactional
    public GrupoResumoDTO definirNotaFinal(Long grupoId, Double notaInformada, User atual) {
        if (atual.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas ADMIN pode definir nota final");
        }

        Grupo grupo = grupos.findById(grupoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo nao encontrado"));
        normalizarStatusGrupo(grupo);
        perms.assertGrupoEmCurso(grupo);

        double nota = validarENormalizarNota(notaInformada);
        GrupoStatus novoStatus = nota >= 6.0d ? GrupoStatus.APROVADO : GrupoStatus.REPROVADO;

        grupo.setNotaFinal(nota);
        grupo.setStatus(novoStatus);
        grupo.setArquivadoEm(LocalDateTime.now());
        grupos.save(grupo);

        GrupoAlunoStatus statusMembro = mapearStatusMembro(novoStatus);
        List<GrupoAluno> membros = grupoAlunos.findByGrupoId(grupoId);
        for (GrupoAluno membro : membros) {
            membro.setStatus(statusMembro);
        }
        if (!membros.isEmpty()) {
            grupoAlunos.saveAll(membros);
        }

<<<<<<< HEAD
        // Notifica todos os alunos do grupo sobre a nota final
        try {
            List<User> alunosVerificados = grupoAlunos.findAlunosByGrupoId(grupoId).stream()
                    .filter(User::isEmailConfirmado)
                    .toList();
            emailService.enviarNotaFinalParaAlunos(alunosVerificados, grupo, nota,
                    novoStatus == GrupoStatus.APROVADO);
        } catch (RuntimeException ignored) {
            // best-effort
        }

=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
        return toResumo(grupo);
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
        GrupoStatus status = statusDoGrupo(g);
        long count = grupoAlunos.countByGrupoId(g.getId());
        return new GrupoResumoDTO(
                g.getId(),
                g.getTitulo(),
                g.getMateria(),
                status,
                g.getNotaFinal(),
                g.getArquivadoEm(),
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

    private List<GrupoResumoDTO> listarPorStatusDoUsuario(User atual, String busca, GrupoStatus... statusAceitos) {
        Set<GrupoStatus> statuses = new LinkedHashSet<>(List.of(statusAceitos));
        String termoBusca = normalizarBusca(busca);
        List<Grupo> lista = listarGruposAcessiveis(atual);

        List<Grupo> paraSalvar = new ArrayList<>();
        for (Grupo grupo : lista) {
            if (normalizarStatusGrupo(grupo)) {
                paraSalvar.add(grupo);
            }
        }
        if (!paraSalvar.isEmpty()) {
            grupos.saveAll(paraSalvar);
        }

        final Map<Long, List<String>> nomesAlunosPorGrupo = termoBusca == null
                ? Map.of()
                : carregarNomesAlunosPorGrupo(lista);

        return lista.stream()
                .filter(grupo -> statuses.contains(statusDoGrupo(grupo)))
                .filter(grupo -> termoBusca == null || correspondeBusca(grupo, termoBusca, nomesAlunosPorGrupo))
                .sorted(Comparator.comparing(Grupo::getId))
                .map(this::toResumo)
                .toList();
    }

    private List<Grupo> listarGruposAcessiveis(User atual) {
        if (atual.getRole() == Role.ADMIN) {
            return grupos.findAll();
        }
        if (atual.getRole() == Role.PROFESSOR) {
            return grupos.findByOrientadorIdOrCoorientadorId(atual.getId(), atual.getId());
        }
        return grupos.findByAlunoId(atual.getId());
    }

    private boolean normalizarStatusGrupo(Grupo grupo) {
        if (grupo.getStatus() != null) {
            return false;
        }
        grupo.setStatus(GrupoStatus.EM_CURSO);
        return true;
    }

    private GrupoStatus statusDoGrupo(Grupo grupo) {
        return grupo.getStatus() == null ? GrupoStatus.EM_CURSO : grupo.getStatus();
    }

    private void assertAlunoElegivelParaGrupo(User aluno, Grupo grupo) {
        Long alunoId = aluno.getId();
        boolean aprovadoTG = grupoAlunos.existsByAlunoIdAndGrupoMateriaAndGrupoStatus(
                alunoId,
                Materia.TG,
                GrupoStatus.APROVADO
        );
        boolean aprovadoPTG = grupoAlunos.existsByAlunoIdAndGrupoMateriaAndGrupoStatus(
                alunoId,
                Materia.PTG,
                GrupoStatus.APROVADO
        );

        if (aprovadoTG && aprovadoPTG) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aluno " + aluno.getNome() + " ja foi aprovado em TG e PTG e nao pode entrar em novos grupos"
            );
        }

        if (grupo.getMateria() == Materia.TG && aprovadoTG) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aluno " + aluno.getNome() + " ja foi aprovado em TG e nao pode entrar em novo grupo TG"
            );
        }

        if (grupo.getMateria() == Materia.PTG && aprovadoPTG) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aluno " + aluno.getNome() + " ja foi aprovado em PTG e nao pode entrar em novo grupo PTG"
            );
        }
    }

    private double validarENormalizarNota(Double notaInformada) {
        if (notaInformada == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nota final eh obrigatoria");
        }
        if (Double.isNaN(notaInformada) || Double.isInfinite(notaInformada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nota final invalida");
        }
        if (notaInformada < 0d || notaInformada > 10d) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nota final deve estar entre 0 e 10");
        }
        return BigDecimal.valueOf(notaInformada)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private GrupoAlunoStatus mapearStatusMembro(GrupoStatus statusGrupo) {
        return statusGrupo == GrupoStatus.APROVADO ? GrupoAlunoStatus.APROVADO : GrupoAlunoStatus.REPROVADO;
    }

    private Map<Long, List<String>> carregarNomesAlunosPorGrupo(List<Grupo> gruposAcessiveis) {
        List<Long> grupoIds = gruposAcessiveis.stream()
                .map(Grupo::getId)
                .distinct()
                .toList();
        if (grupoIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> linhas = grupoAlunos.findGrupoIdAndAlunoNomeByGrupoIds(grupoIds);
        Map<Long, List<String>> nomesPorGrupo = new HashMap<>();
        for (Object[] linha : linhas) {
            if (linha == null || linha.length < 2 || !(linha[0] instanceof Long grupoId)) {
                continue;
            }
            String nomeAluno = linha[1] == null ? "" : linha[1].toString();
            String nomeNormalizado = normalizarBusca(nomeAluno);
            if (nomeNormalizado != null) {
                nomesPorGrupo.computeIfAbsent(grupoId, ignored -> new ArrayList<>())
                        .add(nomeNormalizado);
            }
        }
        return nomesPorGrupo;
    }

    private boolean correspondeBusca(Grupo grupo, String termo, Map<Long, List<String>> nomesAlunosPorGrupo) {
        String tituloNormalizado = normalizarBusca(grupo.getTitulo());
        if (tituloNormalizado != null && tituloNormalizado.contains(termo)) {
            return true;
        }
        List<String> nomes = nomesAlunosPorGrupo.getOrDefault(grupo.getId(), List.of());
        return nomes.stream().anyMatch(nome -> nome.contains(termo));
    }

    private String normalizarBusca(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
