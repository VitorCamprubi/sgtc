package com.vitorcamprubi.sgtc;

import com.vitorcamprubi.sgtc.domain.*;

import java.time.LocalDateTime;

/**
 * Fabrica de objetos de dominio prontos para uso em testes.
 * Mantem os testes legiveis e os defaults consistentes.
 */
public final class TestFixtures {
    private TestFixtures() {}

    public static User user(Long id, Role role, String nome) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        u.setEmail((nome == null ? "user" : nome.toLowerCase().replace(' ', '.')) + "@sgtc.local");
        u.setSenhaHash("$2a$10$dummyhashdummyhashdummyhashdummyhashdumm");
        u.setRole(role);
        u.setEmailConfirmado(true);
        if (role == Role.ALUNO) u.setRa("RA" + id);
        return u;
    }

    public static User admin(Long id) { return user(id, Role.ADMIN, "Admin" + id); }
    public static User professor(Long id) { return user(id, Role.PROFESSOR, "Prof" + id); }
    public static User aluno(Long id) { return user(id, Role.ALUNO, "Aluno" + id); }

    public static Grupo grupo(Long id, User orientador, Materia materia) {
        Grupo g = new Grupo();
        g.setId(id);
        g.setTitulo("Grupo " + id);
        g.setOrientador(orientador);
        g.setMateria(materia);
        g.setStatus(GrupoStatus.EM_CURSO);
        g.setCreatedAt(LocalDateTime.now());
        return g;
    }

    public static Reuniao reuniao(Long id, Grupo grupo, User criadoPor) {
        Reuniao r = new Reuniao();
        // setId nao existe; usamos reflection nao - ele eh gerado pelo banco.
        // Para testes unitarios em que precisamos do ID setado, criamos
        // a reuniao via salvar mockado retornando a propria entity.
        r.setGrupo(grupo);
        r.setDataHora(LocalDateTime.now().plusDays(1));
        r.setPauta("Pauta de teste");
        r.setStatus(ReuniaoStatus.AGUARDANDO_DATA_REUNIAO);
        r.setCriadoPor(criadoPor);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }
}
