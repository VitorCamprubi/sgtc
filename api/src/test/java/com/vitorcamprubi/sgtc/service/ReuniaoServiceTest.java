package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.TestFixtures;
import com.vitorcamprubi.sgtc.domain.*;
import com.vitorcamprubi.sgtc.notification.EmailService;
import com.vitorcamprubi.sgtc.repo.GrupoAlunoRepository;
import com.vitorcamprubi.sgtc.repo.ReuniaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReuniaoService - ciclo de vida")
class ReuniaoServiceTest {

    @Mock ReuniaoRepository repo;
    @Mock PermissaoService perms;
    @Mock EmailService emailService;
    @Mock GrupoAlunoRepository grupoAlunos;

    @InjectMocks
    ReuniaoService service;

    private User professor;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        professor = TestFixtures.professor(1L);
        grupo = TestFixtures.grupo(10L, professor, Materia.TG);
    }

    private static void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------------
    // agendar
    // ----------------------------------------------------------------

    @Test
    @DisplayName("agendar gera token UUID e expira em 12h")
    void agendarGeraTokenComTtl() {
        when(perms.assertPodeAcessarGrupo(10L, professor)).thenReturn(grupo);
        when(repo.save(any(Reuniao.class))).thenAnswer(inv -> inv.getArgument(0));
        when(grupoAlunos.findAlunosByGrupoId(10L)).thenReturn(List.of());

        Reuniao r = service.agendar(10L, LocalDateTime.now().plusDays(1),
                "Pauta X", "obs", professor);

        assertThat(r.getStatus()).isEqualTo(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);
        assertThat(r.getTokenConfirmacao()).hasSize(36); // UUID
        assertThat(r.getTokenExpiraEm()).isNotNull();
        assertThat(r.getTokenExpiraEm()).isAfter(LocalDateTime.now().plusHours(11));
        assertThat(r.getTokenExpiraEm()).isBefore(LocalDateTime.now().plusHours(13));
    }

    @Test
    @DisplayName("agendar sem pauta lanca 400")
    void agendarSemPauta() {
        assertThatThrownBy(() -> service.agendar(10L, LocalDateTime.now().plusDays(1),
                "  ", null, professor))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pauta");
    }

    @Test
    @DisplayName("agendar sem dataHora lanca 400")
    void agendarSemData() {
        assertThatThrownBy(() -> service.agendar(10L, null, "ok", null, professor))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Data");
    }

    // ----------------------------------------------------------------
    // responderConfirmacao
    // ----------------------------------------------------------------

    @Test
    @DisplayName("token expirado retorna 410 e limpa token")
    void tokenExpirado() {
        Reuniao r = TestFixtures.reuniao(null, grupo, professor);
        r.setTokenConfirmacao("abc");
        r.setTokenExpiraEm(LocalDateTime.now().minusMinutes(1));
        when(repo.findByTokenConfirmacao("abc")).thenReturn(Optional.of(r));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.responderConfirmacao("abc", true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("expirado");

        assertThat(r.getTokenConfirmacao()).isNull();
        assertThat(r.getTokenExpiraEm()).isNull();
    }

    @Test
    @DisplayName("confirmar valida marca como confirmada e invalida token")
    void confirmarOk() {
        Reuniao r = TestFixtures.reuniao(null, grupo, professor);
        r.setTokenConfirmacao("ok");
        r.setTokenExpiraEm(LocalDateTime.now().plusHours(1));
        when(repo.findByTokenConfirmacao("ok")).thenReturn(Optional.of(r));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(grupoAlunos.findAlunosByGrupoId(10L)).thenReturn(List.of());

        Reuniao saida = service.responderConfirmacao("ok", true);

        assertThat(saida.getConfirmadaPeloProfessor()).isTrue();
        assertThat(saida.getRespondidaEm()).isNotNull();
        assertThat(saida.getTokenConfirmacao()).isNull();
        assertThat(saida.getTokenExpiraEm()).isNull();
        assertThat(saida.getStatus()).isEqualTo(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);
    }

    @Test
    @DisplayName("recusar coloca status CANCELADA")
    void recusarCancela() {
        Reuniao r = TestFixtures.reuniao(null, grupo, professor);
        r.setTokenConfirmacao("rec");
        r.setTokenExpiraEm(LocalDateTime.now().plusHours(1));
        when(repo.findByTokenConfirmacao("rec")).thenReturn(Optional.of(r));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(grupoAlunos.findAlunosByGrupoId(10L)).thenReturn(List.of());

        Reuniao saida = service.responderConfirmacao("rec", false);

        assertThat(saida.getConfirmadaPeloProfessor()).isFalse();
        assertThat(saida.getStatus()).isEqualTo(ReuniaoStatus.CANCELADA);
        assertThat(saida.getEncerradaEm()).isNotNull();
    }

    @Test
    @DisplayName("token de reuniao ja encerrada lanca 400")
    void reuniaoJaEncerrada() {
        Reuniao r = TestFixtures.reuniao(null, grupo, professor);
        r.setStatus(ReuniaoStatus.CONCLUIDA);
        r.setTokenConfirmacao("x");
        when(repo.findByTokenConfirmacao("x")).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.responderConfirmacao("x", true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("encerrada");
    }

    // ----------------------------------------------------------------
    // concluir / numeroEncontro
    // ----------------------------------------------------------------

    @Test
    @DisplayName("concluir atribui numero 1 quando nao ha encontros anteriores")
    void primeiroEncontro() {
        Reuniao r = TestFixtures.reuniao(null, grupo, professor);
        setId(r, 100L);
        r.setStatus(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);

        when(repo.findById(100L)).thenReturn(Optional.of(r));
        when(repo.findByGrupoIdAndStatusOrderByNumeroEncontroAscEncerradaEmAsc(
                10L, ReuniaoStatus.CONCLUIDA)).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dados = new ReuniaoService.ExecucaoReuniaoDados(
                LocalDate.now(), "atividade x", ReuniaoDesempenhoGrupo.BOM, "Prof. Y");

        Reuniao saida = service.concluir(100L, dados, professor);

        assertThat(saida.getNumeroEncontro()).isEqualTo(1);
        assertThat(saida.getStatus()).isEqualTo(ReuniaoStatus.CONCLUIDA);
        assertThat(saida.getEncerradaEm()).isNotNull();
        assertThat(saida.getDesempenhoGrupo()).isEqualTo(ReuniaoDesempenhoGrupo.BOM);
    }

    @Test
    @DisplayName("concluir preenche o primeiro buraco na sequencia 1..6")
    void preencheBuraco() {
        Reuniao nova = TestFixtures.reuniao(null, grupo, professor);
        setId(nova, 200L);

        Reuniao c1 = TestFixtures.reuniao(null, grupo, professor);
        c1.setNumeroEncontro(1);
        c1.setStatus(ReuniaoStatus.CONCLUIDA);
        Reuniao c3 = TestFixtures.reuniao(null, grupo, professor);
        c3.setNumeroEncontro(3);
        c3.setStatus(ReuniaoStatus.CONCLUIDA);

        when(repo.findById(200L)).thenReturn(Optional.of(nova));
        when(repo.findByGrupoIdAndStatusOrderByNumeroEncontroAscEncerradaEmAsc(
                10L, ReuniaoStatus.CONCLUIDA)).thenReturn(List.of(c1, c3));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dados = new ReuniaoService.ExecucaoReuniaoDados(
                LocalDate.now(), "x", ReuniaoDesempenhoGrupo.OTIMO, "Prof");
        Reuniao saida = service.concluir(200L, dados, professor);

        // Sequencia tem buracos no 2; o proximo deve ser o 2
        assertThat(saida.getNumeroEncontro()).isEqualTo(2);
    }

    @Test
    @DisplayName("ja com 6 encontros concluidos, concluir lanca 400")
    void seisEncontrosCheios() {
        Reuniao nova = TestFixtures.reuniao(null, grupo, professor);
        setId(nova, 300L);

        List<Reuniao> seis = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Reuniao c = TestFixtures.reuniao(null, grupo, professor);
            c.setNumeroEncontro(i);
            c.setStatus(ReuniaoStatus.CONCLUIDA);
            seis.add(c);
        }

        when(repo.findById(300L)).thenReturn(Optional.of(nova));
        when(repo.findByGrupoIdAndStatusOrderByNumeroEncontroAscEncerradaEmAsc(
                10L, ReuniaoStatus.CONCLUIDA)).thenReturn(seis);

        var dados = new ReuniaoService.ExecucaoReuniaoDados(
                LocalDate.now(), "x", ReuniaoDesempenhoGrupo.BOM, "Prof");

        assertThatThrownBy(() -> service.concluir(300L, dados, professor))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("encontros");
    }

    @Test
    @DisplayName("atividades acima do limite lanca 400")
    void atividadesMuitoLongas() {
        Reuniao r = TestFixtures.reuniao(null, grupo, professor);
        setId(r, 400L);
        when(repo.findById(400L)).thenReturn(Optional.of(r));

        String texto = "x".repeat(ReuniaoService.MAX_ATIVIDADES_REALIZADAS + 1);
        var dados = new ReuniaoService.ExecucaoReuniaoDados(
                LocalDate.now(), texto, ReuniaoDesempenhoGrupo.BOM, "Prof");

        assertThatThrownBy(() -> service.concluir(400L, dados, professor))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(String.valueOf(ReuniaoService.MAX_ATIVIDADES_REALIZADAS));
    }

    // ----------------------------------------------------------------
    // cancelar
    // ----------------------------------------------------------------

    @Test
    @DisplayName("cancelar limpa dados de execucao e marca encerradaEm")
    void cancelarLimpa() {
        Reuniao r = TestFixtures.reuniao(null, grupo, professor);
        setId(r, 500L);
        r.setRelatorio("antigo");
        r.setNumeroEncontro(2);
        when(repo.findById(500L)).thenReturn(Optional.of(r));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Reuniao saida = service.cancelar(500L, professor);

        assertThat(saida.getStatus()).isEqualTo(ReuniaoStatus.CANCELADA);
        assertThat(saida.getRelatorio()).isNull();
        assertThat(saida.getNumeroEncontro()).isNull();
        assertThat(saida.getEncerradaEm()).isNotNull();
    }

    // ----------------------------------------------------------------
    // fechamento automatico
    // ----------------------------------------------------------------

    @Test
    @DisplayName("scheduler marca como NAO_REALIZADA reunioes atrasadas mais de 1 semana")
    void atrasadasViramNaoRealizadas() {
        Reuniao atrasada = TestFixtures.reuniao(null, grupo, professor);
        atrasada.setDataHora(LocalDateTime.now().minusWeeks(2));
        atrasada.setStatus(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);

        Reuniao recente = TestFixtures.reuniao(null, grupo, professor);
        recente.setDataHora(LocalDateTime.now().minusDays(2));
        recente.setStatus(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);

        when(repo.findAtrasadas(any(), any())).thenReturn(List.of(atrasada, recente));
        lenient().when(repo.saveAll(any())).thenReturn(List.of());

        int processadas = service.fecharReunioesAtrasadasAutomaticamente();

        // Apenas a atrasada (>1 semana) deve ser fechada.
        assertThat(processadas).isEqualTo(1);
        assertThat(atrasada.getStatus()).isEqualTo(ReuniaoStatus.NAO_REALIZADA);
        assertThat(atrasada.getEncerradaEm()).isNotNull();
        assertThat(recente.getStatus()).isEqualTo(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);
    }
}
