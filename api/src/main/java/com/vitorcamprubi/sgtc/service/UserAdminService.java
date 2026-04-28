package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.notification.EmailService;
import com.vitorcamprubi.sgtc.repo.DocumentoComentarioRepository;
import com.vitorcamprubi.sgtc.repo.DocumentoVersaoRepository;
import com.vitorcamprubi.sgtc.repo.GrupoAlunoRepository;
import com.vitorcamprubi.sgtc.repo.GrupoRepository;
import com.vitorcamprubi.sgtc.repo.ReuniaoRepository;
import com.vitorcamprubi.sgtc.repo.UserRepository;
import com.vitorcamprubi.sgtc.web.dto.UserAdminDTO;
import com.vitorcamprubi.sgtc.web.dto.UserAdminRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserAdminService {
    private static final int VERIFICATION_TOKEN_TTL_HOURS = 48;

    private final UserRepository users;
    private final PasswordEncoder enc;
    private final GrupoRepository grupos;
    private final GrupoAlunoRepository grupoAlunos;
    private final DocumentoVersaoRepository docs;
    private final DocumentoComentarioRepository comentarios;
    private final ReuniaoRepository reunioes;
    private final EmailService emailService;

    public UserAdminService(UserRepository users, PasswordEncoder enc,
                            GrupoRepository grupos, GrupoAlunoRepository grupoAlunos,
                            DocumentoVersaoRepository docs, DocumentoComentarioRepository comentarios,
                            ReuniaoRepository reunioes, EmailService emailService) {
        this.users = users;
        this.enc = enc;
        this.grupos = grupos;
        this.grupoAlunos = grupoAlunos;
        this.docs = docs;
        this.comentarios = comentarios;
        this.reunioes = reunioes;
        this.emailService = emailService;
    }

    public List<UserAdminDTO> listar(Role role) {
        List<User> lista = role == null ? users.findAll() : users.findByRole(role);
        return lista.stream().map(UserAdminDTO::of).toList();
    }

    @Transactional
    public UserAdminDTO criar(UserAdminRequest req) {
        validarRolePermitida(req.getRole());
        if (users.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado");
        }

        User u = new User();
        preencher(u, req, true);

        // Cadastro nasce com email NAO confirmado e token de verificacao
        u.setEmailConfirmado(false);
        String token = UUID.randomUUID().toString();
        u.setTokenConfirmacao(token);
        u.setTokenConfirmacaoExpiraEm(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_TTL_HOURS));

        User salvo = users.save(u);
        emailService.enviarConfirmacaoCadastro(salvo, token);
        return UserAdminDTO.of(salvo);
    }

    /**
     * Confirma o e-mail de um usuario a partir do token enviado por e-mail.
     * Retorna o usuario verificado.
     */
    @Transactional
    public User confirmarEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
        }
        User u = users.findByTokenConfirmacao(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token nao encontrado"));
        if (u.isEmailConfirmado()) {
            return u;
        }
        if (u.getTokenConfirmacaoExpiraEm() != null
                && u.getTokenConfirmacaoExpiraEm().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token expirado");
        }
        u.setEmailConfirmado(true);
        u.setTokenConfirmacao(null);
        u.setTokenConfirmacaoExpiraEm(null);
        return users.save(u);
    }

    /**
     * Reenvia o e-mail de verificacao gerando um novo token.
     */
    @Transactional
    public void reenviarConfirmacao(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email obrigatorio");
        }
        User u = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        if (u.isEmailConfirmado()) {
            return; // nada a fazer
        }
        String token = UUID.randomUUID().toString();
        u.setTokenConfirmacao(token);
        u.setTokenConfirmacaoExpiraEm(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_TTL_HOURS));
        users.save(u);
        emailService.enviarConfirmacaoCadastro(u, token);
    }

    @Transactional
    public UserAdminDTO atualizar(Long id, UserAdminRequest req) {
        validarRolePermitida(req.getRole());

        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        users.findByEmail(req.getEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado");
                });

        preencher(u, req, false);
        return UserAdminDTO.of(users.save(u));
    }

    @Transactional
    public void excluir(Long id) {
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));

        if (u.getRole() == Role.PROFESSOR) {
            long qtdGrupos = grupos.countByOrientadorIdOrCoorientadorId(id, id);
            if (qtdGrupos > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario esta atribuido como orientador/coorientador em grupos");
            }
        }

        if (grupoAlunos.countByAlunoId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario esta vinculado a grupos como aluno");
        }

        if (docs.countByEnviadoPorId(id) > 0 ||
                comentarios.countByAutorId(id) > 0 ||
                reunioes.countByCriadoPorId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario possui registros (documentos/comentarios/reunioes)");
        }

        users.delete(u);
    }

    private void preencher(User u, UserAdminRequest req, boolean isCreate) {
        u.setNome(req.getNome());
        u.setEmail(req.getEmail());
        u.setRole(req.getRole());
        if (req.getRole() == Role.ALUNO) {
            String ra = normalizarTexto(req.getRa());
            if (ra == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RA eh obrigatorio para alunos");
            }
            u.setRa(ra);
        } else {
            u.setRa(null);
        }

        if (req.getSenha() != null && !req.getSenha().isBlank()) {
            u.setSenhaHash(enc.encode(req.getSenha()));
        } else if (isCreate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha eh obrigatoria");
        }
    }

    private void validarRolePermitida(Role role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role obrigatoria");
        }
        if (!(role == Role.ALUNO || role == Role.PROFESSOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas alunos ou professores podem ser gerenciados aqui");
        }
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String ajustado = valor.trim();
        return ajustado.isEmpty() ? null : ajustado;
    }
}
