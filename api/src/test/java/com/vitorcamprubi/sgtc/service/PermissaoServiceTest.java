package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.TestFixtures;
import com.vitorcamprubi.sgtc.domain.Grupo;
import com.vitorcamprubi.sgtc.domain.GrupoStatus;
import com.vitorcamprubi.sgtc.domain.Materia;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.GrupoAlunoRepository;
import com.vitorcamprubi.sgtc.repo.GrupoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissaoService - matriz de papeis")
class PermissaoServiceTest {

    @Mock
    GrupoRepository grupos;

    @Mock
    GrupoAlunoRepository grupoAlunos;

    @InjectMocks
    PermissaoService perms;

    private User admin;
    private User orientador;
    private User outroProfessor;
    private User aluno;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        admin = TestFixtures.admin(1L);
        orientador = TestFixtures.professor(2L);
        outroProfessor = TestFixtures.professor(3L);
        aluno = TestFixtures.aluno(4L);
        grupo = TestFixtures.grupo(10L, orientador, Materia.TG);
    }

    @Test
    @DisplayName("admin acessa qualquer grupo")
    void adminAcessaQualquerGrupo() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupo));
        assertThat(perms.assertPodeAcessarGrupo(10L, admin)).isSameAs(grupo);
    }

    @Test
    @DisplayName("orientador acessa o proprio grupo")
    void orientadorAcessaProprioGrupo() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupo));
        assertThat(perms.assertPodeAcessarGrupo(10L, orientador)).isSameAs(grupo);
    }

    @Test
    @DisplayName("professor que nao orienta o grupo recebe 403")
    void professorSemRelacaoBloqueado() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupo));
        when(grupoAlunos.existsByGrupoIdAndAlunoId(10L, outroProfessor.getId())).thenReturn(false);

        assertThatThrownBy(() -> perms.assertPodeAcessarGrupo(10L, outroProfessor))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    @DisplayName("aluno membro do grupo acessa")
    void alunoMembroAcessa() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupo));
        when(grupoAlunos.existsByGrupoIdAndAlunoId(10L, aluno.getId())).thenReturn(true);
        assertThat(perms.assertPodeAcessarGrupo(10L, aluno)).isSameAs(grupo);
    }

    @Test
    @DisplayName("aluno nao-membro recebe 403")
    void alunoNaoMembroBloqueado() {
        when(grupos.findById(10L)).thenReturn(Optional.of(grupo));
        when(grupoAlunos.existsByGrupoIdAndAlunoId(10L, aluno.getId())).thenReturn(false);

        assertThatThrownBy(() -> perms.assertPodeAcessarGrupo(10L, aluno))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("aluno nao acessa grupo arquivado mesmo sendo membro")
    void alunoNaoAcessaArquivado() {
        grupo.setStatus(GrupoStatus.APROVADO);
        when(grupos.findById(10L)).thenReturn(Optional.of(grupo));
        when(grupoAlunos.existsByGrupoIdAndAlunoId(10L, aluno.getId())).thenReturn(true);

        assertThatThrownBy(() -> perms.assertPodeAcessarGrupo(10L, aluno))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("arquivado");
    }

    @Test
    @DisplayName("professor acessa grupo arquivado do qual orientou")
    void professorAcessaArquivado() {
        grupo.setStatus(GrupoStatus.APROVADO);
        when(grupos.findById(10L)).thenReturn(Optional.of(grupo));

        assertThat(perms.assertPodeAcessarGrupo(10L, orientador)).isSameAs(grupo);
    }

    @Test
    @DisplayName("grupo inexistente lanca 404")
    void grupoInexistente() {
        when(grupos.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> perms.assertPodeAcessarGrupo(99L, admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("assertGrupoEmCurso bloqueia grupo arquivado")
    void assertGrupoEmCursoBloqueia() {
        grupo.setStatus(GrupoStatus.REPROVADO);
        assertThatThrownBy(() -> perms.assertGrupoEmCurso(grupo))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("arquivado");
    }

    @Test
    @DisplayName("assertGrupoEmCurso passa quando esta em curso")
    void assertGrupoEmCursoPassa() {
        grupo.setStatus(GrupoStatus.EM_CURSO);
        perms.assertGrupoEmCurso(grupo); // nao deve lancar
    }
}
