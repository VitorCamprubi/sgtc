package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.ReuniaoDesempenhoGrupo;
import com.vitorcamprubi.sgtc.domain.ReuniaoStatus;
import com.vitorcamprubi.sgtc.domain.User;
<<<<<<< HEAD
import com.vitorcamprubi.sgtc.notification.EmailService;
import com.vitorcamprubi.sgtc.repo.GrupoAlunoRepository;
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
import com.vitorcamprubi.sgtc.repo.ReuniaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
<<<<<<< HEAD
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
=======
import java.util.List;
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88

@Service
public class ReuniaoService {
    public static final int MAX_ATIVIDADES_REALIZADAS = 340;

    public record ExecucaoReuniaoDados(
            LocalDate dataAtividadesRealizadas,
            String atividadesRealizadas,
            ReuniaoDesempenhoGrupo desempenhoGrupo,
            String professorDisciplina
    ) {
    }

    private final ReuniaoRepository repo;
    private final PermissaoService perms;
<<<<<<< HEAD
    private final EmailService emailService;
    private final GrupoAlunoRepository grupoAlunos;

    public ReuniaoService(ReuniaoRepository repo, PermissaoService perms,
                          EmailService emailService, GrupoAlunoRepository grupoAlunos) {
        this.repo = repo;
        this.perms = perms;
        this.emailService = emailService;
        this.grupoAlunos = grupoAlunos;
=======

    public ReuniaoService(ReuniaoRepository repo, PermissaoService perms) {
        this.repo = repo;
        this.perms = perms;
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
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
        perms.assertGrupoEmCurso(g);

        Reuniao r = new Reuniao();
        r.setGrupo(g);
        r.setDataHora(dataHora);
        r.setPauta(pauta.trim());
        r.setObservacoes(observacoes == null || observacoes.isBlank() ? null : observacoes.trim());
        r.setStatus(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);
        limparDadosExecucao(r);
        r.setCriadoPor(atual);
<<<<<<< HEAD
        // Token de confirmacao (links no e-mail do professor)
        r.setTokenConfirmacao(UUID.randomUUID().toString());
        r.setConfirmadaPeloProfessor(null);
        Reuniao salva = repo.save(r);

        // Notifica orientador / coorientador (com link de confirmar/recusar)
        // e os demais membros do grupo
        notificarReuniaoAgendada(salva);
        return salva;
    }

    private void notificarReuniaoAgendada(Reuniao reuniao) {
        try {
            var grupo = reuniao.getGrupo();
            List<User> alunos = grupoAlunos.findAlunosByGrupoId(grupo.getId());
            List<String> nomesAlunos = alunos.stream().map(User::getNome).toList();

            Set<Long> jaEnviados = new LinkedHashSet<>();
            if (grupo.getOrientador() != null && grupo.getOrientador().isEmailConfirmado()) {
                emailService.enviarReuniaoParaProfessor(grupo.getOrientador(), reuniao,
                        reuniao.getTokenConfirmacao(), nomesAlunos);
                jaEnviados.add(grupo.getOrientador().getId());
            }
            if (grupo.getCoorientador() != null && grupo.getCoorientador().isEmailConfirmado()
                    && !jaEnviados.contains(grupo.getCoorientador().getId())) {
                emailService.enviarReuniaoParaProfessor(grupo.getCoorientador(), reuniao,
                        reuniao.getTokenConfirmacao(), nomesAlunos);
            }
            // Avisa os alunos do grupo (somente os que confirmaram email)
            List<User> alunosVerificados = alunos.stream()
                    .filter(User::isEmailConfirmado)
                    .toList();
            emailService.enviarReuniaoParaAlunos(alunosVerificados, reuniao);
        } catch (RuntimeException ignored) {
            // best-effort
        }
    }

    /**
     * Confirma ou recusa uma reuniao a partir do token enviado por e-mail.
     * Notifica os alunos do grupo sobre o resultado.
     */
    @Transactional
    public Reuniao responderConfirmacao(String token, boolean confirmada) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
        }
        Reuniao r = repo.findByTokenConfirmacao(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reuniao nao encontrada"));
        if (r.getStatus() != ReuniaoStatus.AGUARDANDO_DATA_REUNIAO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reuniao ja encerrada");
        }
        r.setConfirmadaPeloProfessor(confirmada);
        r.setRespondidaEm(LocalDateTime.now());
        if (!confirmada) {
            r.setStatus(ReuniaoStatus.CANCELADA);
            r.setEncerradaEm(LocalDateTime.now());
        }
        // Apos resposta, invalida o token para evitar reuso
        r.setTokenConfirmacao(null);
        Reuniao salva = repo.save(r);

        try {
            List<User> alunos = grupoAlunos.findAlunosByGrupoId(salva.getGrupo().getId())
                    .stream().filter(User::isEmailConfirmado).toList();
            emailService.enviarRespostaReuniaoParaAlunos(alunos, salva, confirmada);
        } catch (RuntimeException ignored) {
            // best-effort
        }
        return salva;
=======
        return repo.save(r);
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
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
        perms.assertGrupoEmCurso(r.getGrupo());
        perms.assertOrientadorOuCoorientadorDoGrupo(r.getGrupo().getId(), atual);
        assertAberta(r);

        r.setDataHora(dataHora);
        r.setPauta(pauta.trim());
        r.setObservacoes(observacoes == null || observacoes.isBlank() ? null : observacoes.trim());
        return repo.save(r);
    }

    @Transactional
    public Reuniao concluir(Long reuniaoId, ExecucaoReuniaoDados dados, User atual) {
        Reuniao r = carregar(reuniaoId);
        atualizarStatusNaoRealizadaSeAtrasada(r, LocalDateTime.now());
        perms.assertGrupoEmCurso(r.getGrupo());
        perms.assertOrientadorOuCoorientadorDoGrupo(r.getGrupo().getId(), atual);
        assertAberta(r);
        validarDadosExecucao(dados);
        Integer numeroEncontro = proximoNumeroEncontro(r.getGrupo().getId());

        r.setStatus(ReuniaoStatus.CONCLUIDA);
        r.setNumeroEncontro(numeroEncontro);
        r.setDataAtividadesRealizadas(dados.dataAtividadesRealizadas());
        r.setAtividadesRealizadas(dados.atividadesRealizadas().trim());
        r.setDesempenhoGrupo(dados.desempenhoGrupo());
        r.setProfessorDisciplina(dados.professorDisciplina().trim());
        r.setOrientadorAssinatura(assinaturaDoUsuario(r.getGrupo().getOrientador()));
        r.setCoorientadorAssinatura(assinaturaDoUsuario(r.getGrupo().getCoorientador()));
        r.setRelatorio(dados.atividadesRealizadas().trim());
        r.setEncerradaEm(LocalDateTime.now());
        return repo.save(r);
    }

    @Transactional
    public Reuniao cancelar(Long reuniaoId, User atual) {
        Reuniao r = carregar(reuniaoId);
        atualizarStatusNaoRealizadaSeAtrasada(r, LocalDateTime.now());
        perms.assertGrupoEmCurso(r.getGrupo());
        perms.assertOrientadorOuCoorientadorDoGrupo(r.getGrupo().getId(), atual);
        assertAberta(r);

        r.setStatus(ReuniaoStatus.CANCELADA);
        limparDadosExecucao(r);
        r.setEncerradaEm(LocalDateTime.now());
        return repo.save(r);
    }

    @Transactional(readOnly = true)
    public List<Reuniao> listarExecutadasParaPdf(Long grupoId, User atual) {
        perms.assertPodeAcessarGrupo(grupoId, atual);
        return repo.findByGrupoIdAndStatusOrderByNumeroEncontroAscEncerradaEmAsc(grupoId, ReuniaoStatus.CONCLUIDA);
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
        limparDadosExecucao(reuniao);
        reuniao.setEncerradaEm(agora);
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

    private void limparDadosExecucao(Reuniao reuniao) {
        reuniao.setRelatorio(null);
        reuniao.setNumeroEncontro(null);
        reuniao.setDataAtividadesRealizadas(null);
        reuniao.setAtividadesRealizadas(null);
        reuniao.setDesempenhoGrupo(null);
        reuniao.setProfessorDisciplina(null);
        reuniao.setOrientadorAssinatura(null);
        reuniao.setCoorientadorAssinatura(null);
        reuniao.setEncerradaEm(null);
    }

    private void validarDadosExecucao(ExecucaoReuniaoDados dados) {
        if (dados == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados de execucao sao obrigatorios");
        }
        if (dados.dataAtividadesRealizadas() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data das atividades eh obrigatoria");
        }
        if (dados.atividadesRealizadas() == null || dados.atividadesRealizadas().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atividades realizadas sao obrigatorias");
        }
        if (dados.atividadesRealizadas().trim().length() > MAX_ATIVIDADES_REALIZADAS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Atividades realizadas ultrapassam o limite de " + MAX_ATIVIDADES_REALIZADAS + " caracteres"
            );
        }
        if (dados.desempenhoGrupo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Desempenho do grupo eh obrigatorio");
        }
        if (dados.professorDisciplina() == null || dados.professorDisciplina().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Professor da disciplina eh obrigatorio");
        }
    }

    private Integer proximoNumeroEncontro(Long grupoId) {
        List<Reuniao> concluidas = repo.findByGrupoIdAndStatusOrderByNumeroEncontroAscEncerradaEmAsc(
                grupoId,
                ReuniaoStatus.CONCLUIDA
        );
        boolean[] usados = new boolean[7];
        for (Reuniao reuniao : concluidas) {
            Integer encontro = reuniao.getNumeroEncontro();
            if (encontro != null && encontro >= 1 && encontro <= 6) {
                usados[encontro] = true;
            }
        }
        for (int encontro = 1; encontro <= 6; encontro += 1) {
            if (!usados[encontro]) {
                return encontro;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos os encontros de 1 a 6 ja foram utilizados");
    }

    private String assinaturaDoUsuario(User usuario) {
        if (usuario == null || usuario.getNome() == null || usuario.getNome().isBlank()) {
            return null;
        }
        return usuario.getNome().trim();
    }
}
