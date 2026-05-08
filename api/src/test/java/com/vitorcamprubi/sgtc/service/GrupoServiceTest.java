package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.TestFixtures;
import com.vitorcamprubi.sgtc.domain.*;
import com.vitorcamprubi.sgtc.notification.EmailService;
import com.vitorcamprubi.sgtc.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrupoService - regras de negocio criticas")
class GrupoServiceTest {

    @Mock GrupoRepository grupos;
    @Mock UserRepository users;
    @Mock GrupoAlunoRepository grupoAlunos;
    @Mock DocumentoVersaoRepository documentos;
    @Mock ReuniaoRepository reunioes;
    @Mock DocumentoService documentoService;
    @Mock PermissaoService perms;
    @Mock EmailService emailService;

    @InjectMocks
    GrupoService service;

    private User admin;
    private User professor;
    private User aluno;
    private Grupo grupoTG;

    @BeforeEach
    void setUp() {
        admin = TestFixtures.admin(1L);
        professor = TestFixtures.professor(2L);
        aluno = TestFixtures.aluno(3L);
        grupoTG = TestFixtures.grupo(10L, professor, Materia.TG);
    }

    // ----------------------------------------------------------------
    // adicionarMembros / regra TG-PTG
    // ----------------------------------------------------------------

    @Test
    @DisplayName("aluno aprovado em TG nao entra em novo grupo TG")
    void alunoAprovadoTGBloqueadoEmNovoTG() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        when(users.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(grupoAlunos.existsByAlunoIdAndGrupoMateriaAndGrupoStatus(
                aluno.getId(), Materia.TG, GrupoStatus.APROVADO)).thenReturn(true);

        assertThatThrownBy(() -> service.adicionarMembros(10L, List.of(aluno.getId())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("TG");

        verify(grupoAlunos, never()).save(any());
    }

    @Test
    @DisplayName("aluno aprovado em PTG pode entrar em grupo TG")
    void alunoAprovadoPTGPodeEntrarTG() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        when(users.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(grupoAlunos.existsByAlunoIdAndGrupoMateriaAndGrupoStatus(
                aluno.getId(), Materia.TG, GrupoStatus.APROVADO)).thenReturn(false);
        when(grupoAlunos.existsByAlunoIdAndGrupoMateriaAndGrupoStatus(
                aluno.getId(), Materia.PTG, GrupoStatus.APROVADO)).thenReturn(true);
        when(grupoAlunos.existsByGrupoIdAndAlunoId(10L, aluno.getId())).thenReturn(false);

        service.adicionarMembros(10L, List.of(aluno.getId()));

        verify(grupoAlunos).save(any(GrupoAluno.class));
    }

    @Test
    @DisplayName("aluno aprovado em TG e PTG nao entra em nenhum novo grupo")
    void alunoAprovadoEmAmbosBloqueado() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        when(users.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(grupoAlunos.existsByAlunoIdAndGrupoMateriaAndGrupoStatus(
                aluno.getId(), Materia.TG, GrupoStatus.APROVADO)).thenReturn(true);
        when(grupoAlunos.existsByAlunoIdAndGrupoMateriaAndGrupoStatus(
                aluno.getId(), Materia.PTG, GrupoStatus.APROVADO)).thenReturn(true);

        assertThatThrownBy(() -> service.adicionarMembros(10L, List.of(aluno.getId())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("TG e PTG");
    }

    @Test
    @DisplayName("usuario PROFESSOR nao pode ser adicionado como membro")
    void professorNaoEhMembro() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        when(users.findById(professor.getId())).thenReturn(Optional.of(professor));

        assertThatThrownBy(() -> service.adicionarMembros(10L, List.of(professor.getId())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ALUNO");
    }

    @Test
    @DisplayName("lista vazia de alunos lanca 400")
    void listaVaziaLanca400() {
        assertThatThrownBy(() -> service.adicionarMembros(10L, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ao menos um");
    }

    // ----------------------------------------------------------------
    // definirNotaFinal
    // ----------------------------------------------------------------

    @Test
    @DisplayName("nota >= 6 marca grupo como APROVADO e arquiva")
    void notaSeisAprovado() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        when(grupoAlunos.findByGrupoId(10L)).thenReturn(List.of());
        when(grupoAlunos.findAlunosByGrupoId(10L)).thenReturn(List.of());
        when(grupoAlunos.countByGrupoId(10L)).thenReturn(0L);

        service.definirNotaFinal(10L, 6.0, admin);

        assertThat(grupoTG.getStatus()).isEqualTo(GrupoStatus.APROVADO);
        assertThat(grupoTG.getNotaFinal()).isEqualTo(6.0);
        assertThat(grupoTG.getArquivadoEm()).isNotNull();
    }

    @Test
    @DisplayName("nota < 6 marca grupo como REPROVADO")
    void notaCincoReprovado() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        when(grupoAlunos.findByGrupoId(10L)).thenReturn(List.of());
        when(grupoAlunos.findAlunosByGrupoId(10L)).thenReturn(List.of());
        when(grupoAlunos.countByGrupoId(10L)).thenReturn(0L);

        service.definirNotaFinal(10L, 5.99, admin);

        assertThat(grupoTG.getStatus()).isEqualTo(GrupoStatus.REPROVADO);
        assertThat(grupoTG.getNotaFinal()).isEqualTo(5.99);
    }

    @Test
    @DisplayName("nota fora de [0,10] lanca 400")
    void notaForaDoIntervalo() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));

        assertThatThrownBy(() -> service.definirNotaFinal(10L, -1.0, admin))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.definirNotaFinal(10L, 10.5, admin))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("nao-admin nao pode definir nota final")
    void apenasAdminDefineNota() {
        assertThatThrownBy(() -> service.definirNotaFinal(10L, 8.0, professor))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    @DisplayName("nao pode definir nota em grupo ja arquivado")
    void naoRedefineNotaEmArquivado() {
        grupoTG.setStatus(GrupoStatus.APROVADO);
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "arquivado"))
                .when(perms).assertGrupoEmCurso(grupoTG);

        assertThatThrownBy(() -> service.definirNotaFinal(10L, 8.0, admin))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("propaga status APROVADO para todos os GrupoAluno")
    void propagaStatusParaMembros() {
        GrupoAluno ga1 = new GrupoAluno();
        ga1.setStatus(GrupoAlunoStatus.EM_CURSO);
        GrupoAluno ga2 = new GrupoAluno();
        ga2.setStatus(GrupoAlunoStatus.EM_CURSO);

        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        when(grupoAlunos.findByGrupoId(10L)).thenReturn(List.of(ga1, ga2));
        when(grupoAlunos.findAlunosByGrupoId(10L)).thenReturn(List.of());
        when(grupoAlunos.countByGrupoId(10L)).thenReturn(2L);

        service.definirNotaFinal(10L, 8.5, admin);

        assertThat(ga1.getStatus()).isEqualTo(GrupoAlunoStatus.APROVADO);
        assertThat(ga2.getStatus()).isEqualTo(GrupoAlunoStatus.APROVADO);
    }

    // ----------------------------------------------------------------
    // listar arquivos: ALUNO bloqueado
    // ----------------------------------------------------------------

    @Test
    @DisplayName("aluno nao acessa lista de arquivos")
    void alunoNaoVeArquivos() {
        assertThatThrownBy(() -> service.listarArquivadosDoUsuario(aluno, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("professores");
        assertThatThrownBy(() -> service.listarArquivadosAprovadosDoUsuario(aluno, null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.listarArquivadosReprovadosDoUsuario(aluno, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ----------------------------------------------------------------
    // excluir
    // ----------------------------------------------------------------

    @Test
    @DisplayName("nao-admin nao pode excluir grupo")
    void apenasAdminExclui() {
        assertThatThrownBy(() -> service.excluir(10L, professor))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ADMIN");

        verify(grupos, never()).delete(any());
    }

    @Test
    @DisplayName("admin nao pode excluir grupo arquivado")
    void adminNaoExcluiArquivado() {
        grupoTG.setStatus(GrupoStatus.APROVADO);
        when(grupos.findById(10L)).thenReturn(Optional.of(grupoTG));
        when(perms.isGrupoArquivado(grupoTG)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(10L, admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("arquivado");
    }
}
