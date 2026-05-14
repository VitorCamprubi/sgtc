package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.TestFixtures;
import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.notification.EmailService;
import com.vitorcamprubi.sgtc.repo.DocumentoComentarioRepository;
import com.vitorcamprubi.sgtc.repo.DocumentoVersaoRepository;
import com.vitorcamprubi.sgtc.repo.GrupoAlunoRepository;
import com.vitorcamprubi.sgtc.repo.GrupoRepository;
import com.vitorcamprubi.sgtc.repo.ReuniaoRepository;
import com.vitorcamprubi.sgtc.repo.UserRepository;
import com.vitorcamprubi.sgtc.web.dto.UserAdminRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAdminService - soft delete e reativacao")
class UserAdminServiceTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder enc;
    @Mock GrupoRepository grupos;
    @Mock GrupoAlunoRepository grupoAlunos;
    @Mock DocumentoVersaoRepository docs;
    @Mock DocumentoComentarioRepository comentarios;
    @Mock ReuniaoRepository reunioes;
    @Mock EmailService emailService;

    @InjectMocks
    UserAdminService service;

    private User admin;
    private User alvo;

    @BeforeEach
    void setUp() {
        admin = TestFixtures.admin(1L);
        alvo = TestFixtures.aluno(99L);
    }

    // ----------------------------------------------------------------
    // excluir (soft delete)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("excluir usuario inexistente -> 404")
    void excluirInexistente404() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.excluir(99L, admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("admin nao pode desativar a propria conta -> 400")
    void selfDeleteBloqueado() {
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.excluir(1L, admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("propria conta");
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("desativar usuario ja inativo -> 400")
    void jaInativo400() {
        alvo.setAtivo(false);
        when(users.findById(99L)).thenReturn(Optional.of(alvo));
        assertThatThrownBy(() -> service.excluir(99L, admin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("desativado");
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("excluir usuario ativo -> soft delete (ativo=false)")
    void softDeleteOk() {
        assertThat(alvo.isAtivo()).isTrue();
        when(users.findById(99L)).thenReturn(Optional.of(alvo));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.excluir(99L, admin);

        assertThat(alvo.isAtivo()).isFalse();
        verify(users).save(alvo);
    }

    @Test
    @DisplayName("nao precisa mais checar vinculos - aluno com grupos arquivados pode ser desativado")
    void desativaMesmoComVinculos() {
        // Cenario: aluno tem grupo arquivado, documentos, comentarios, reunioes.
        // No modelo antigo dava 400. Agora desativa numa boa.
        when(users.findById(99L)).thenReturn(Optional.of(alvo));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.excluir(99L, admin);

        assertThat(alvo.isAtivo()).isFalse();
        verify(grupoAlunos, never()).countByAlunoId(any());
        verify(docs, never()).countByEnviadoPorId(any());
    }

    // ----------------------------------------------------------------
    // reativar
    // ----------------------------------------------------------------

    @Test
    @DisplayName("reativar usuario inexistente -> 404")
    void reativarInexistente404() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reativar(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("reativar usuario ja ativo -> 400")
    void reativarAtivo400() {
        when(users.findById(99L)).thenReturn(Optional.of(alvo)); // alvo ja vem ativo
        assertThatThrownBy(() -> service.reativar(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ativo");
    }

    @Test
    @DisplayName("reativar usuario inativo -> ativo=true")
    void reativarOk() {
        alvo.setAtivo(false);
        when(users.findById(99L)).thenReturn(Optional.of(alvo));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reativar(99L);

        assertThat(alvo.isAtivo()).isTrue();
        verify(users).save(alvo);
    }

    // ----------------------------------------------------------------
    // criar - email de inativo deve reativar em vez de conflitar
    // ----------------------------------------------------------------

    @Test
    @DisplayName("criar com email de usuario ativo existente -> 409")
    void criarComEmailAtivoConflita() {
        UserAdminRequest req = new UserAdminRequest();
        req.setNome("Joao");
        req.setEmail("joao@fatec.edu.br");
        req.setSenha("SenhaForte1");
        req.setRole(Role.PROFESSOR);

        User existenteAtivo = TestFixtures.professor(50L);
        existenteAtivo.setEmail("joao@fatec.edu.br");
        when(users.findByEmail("joao@fatec.edu.br")).thenReturn(Optional.of(existenteAtivo));

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ja cadastrado");
    }

    @Test
    @DisplayName("criar com email de usuario inativo -> reativa em vez de duplicar")
    void criarComEmailInativoReativa() {
        UserAdminRequest req = new UserAdminRequest();
        req.setNome("Joao Novo");
        req.setEmail("joao@fatec.edu.br");
        req.setSenha("SenhaForte1");
        req.setRole(Role.PROFESSOR);

        User existenteInativo = TestFixtures.professor(50L);
        existenteInativo.setEmail("joao@fatec.edu.br");
        existenteInativo.setAtivo(false);
        when(users.findByEmail("joao@fatec.edu.br")).thenReturn(Optional.of(existenteInativo));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enc.encode(any())).thenReturn("hash");

        service.criar(req);

        assertThat(existenteInativo.isAtivo()).isTrue();
        assertThat(existenteInativo.getNome()).isEqualTo("Joao Novo");
        verify(users).save(existenteInativo);
    }
}
