package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import com.vitorcamprubi.sgtc.repo.*;
import com.vitorcamprubi.sgtc.web.dto.UserAdminDTO;
import com.vitorcamprubi.sgtc.web.dto.UserAdminRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserAdminService {
    private final UserRepository users;
    private final PasswordEncoder enc;
    private final GrupoRepository grupos;
    private final GrupoAlunoRepository grupoAlunos;
    private final DocumentoVersaoRepository docs;
    private final DocumentoComentarioRepository comentarios;
    private final ReuniaoRepository reunioes;

    public UserAdminService(UserRepository users, PasswordEncoder enc,
                            GrupoRepository grupos, GrupoAlunoRepository grupoAlunos,
                            DocumentoVersaoRepository docs, DocumentoComentarioRepository comentarios,
                            ReuniaoRepository reunioes) {
        this.users = users;
        this.enc = enc;
        this.grupos = grupos;
        this.grupoAlunos = grupoAlunos;
        this.docs = docs;
        this.comentarios = comentarios;
        this.reunioes = reunioes;
    }

    public List<UserAdminDTO> listar(Role role) {
        List<User> lista = role == null ? users.findAll() : users.findByRole(role);
        return lista.stream().map(UserAdminDTO::of).toList();
    }

    @Transactional
    public UserAdminDTO criar(UserAdminRequest req) {
        validarRolePermitida(req.getRole());
        if (users.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        User u = new User();
        preencher(u, req, true);
        return UserAdminDTO.of(users.save(u));
    }

    @Transactional
    public UserAdminDTO atualizar(Long id, UserAdminRequest req) {
        validarRolePermitida(req.getRole());
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        users.findByEmail(req.getEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado"); });
        preencher(u, req, false);
        return UserAdminDTO.of(users.save(u));
    }

    @Transactional
    public void excluir(Long id) {
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        // Impede excluir se existirem vínculos que inviabilizam integridade
        if (u.getRole() == Role.ORIENTADOR || u.getRole() == Role.COORIENTADOR) {
            long qtdGrupos = grupos.countByOrientadorIdOrCoorientadorId(id, id);
            if (qtdGrupos > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário está atribuído como orientador/coorientador em grupos");
            }
        }
        if (grupoAlunos.countByAlunoId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário está vinculado a grupos como aluno");
        }
        if (docs.countByEnviadoPorId(id) > 0 ||
            comentarios.countByAutorId(id) > 0 ||
            reunioes.countByCriadoPorId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário possui registros (documentos/comentários/reuniões)");
        }

        users.delete(u);
    }

    private void preencher(User u, UserAdminRequest req, boolean isCreate) {
        u.setNome(req.getNome());
        u.setEmail(req.getEmail());
        u.setRole(req.getRole());
        u.setRa(req.getRole() == Role.ALUNO ? req.getRa() : null);

        if (req.getSenha() != null && !req.getSenha().isBlank()) {
            u.setSenhaHash(enc.encode(req.getSenha()));
        } else if (isCreate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha é obrigatória");
        }
    }

    private void validarRolePermitida(Role role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role obrigatória");
        }
        if (!(role == Role.ALUNO || role == Role.ORIENTADOR || role == Role.COORIENTADOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas alunos ou professores podem ser gerenciados aqui");
        }
    }
}
