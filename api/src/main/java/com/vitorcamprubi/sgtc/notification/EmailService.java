package com.vitorcamprubi.sgtc.notification;

import com.vitorcamprubi.sgtc.domain.DocumentoVersao;
import com.vitorcamprubi.sgtc.domain.Grupo;
import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Servico responsavel por enviar todos os emails do sistema:
 *  - Confirmacao de e-mail (cadastro)
 *  - Notificacao de reuniao agendada (professor + alunos)
 *  - Notificacao de upload de documento (professor)
 *  - Notificacao de comentario do professor (alunos)
 *  - Notificacao de nota final atribuida (alunos)
 *
 * O envio e' "best-effort": se falhar (ex.: SMTP indisponivel) apenas loga
 * o erro, sem quebrar a operacao de negocio que originou o disparo.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy 'as' HH:mm", Locale.of("pt", "BR"));

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@sgtc.local}")
    private String fromAddress;

    @Value("${app.mail.from-name:SGTC}")
    private String fromName;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Value("${app.url.api:http://localhost:8080}")
    private String apiUrl;

    @Value("${app.url.web:http://localhost:4200}")
    private String webUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // -----------------------------------------------------------------
    // 1) Confirmacao de cadastro
    // -----------------------------------------------------------------
    @Async
    public void enviarConfirmacaoCadastro(User usuario, String token) {
        if (usuario == null || usuario.getEmail() == null) return;
        String link = apiUrl + "/api/auth/verify-email?token=" + urlEncode(token);
        String assunto = "Confirme seu e-mail no SGTC";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
                  <h2 style="color:#0d47a1">Bem-vindo(a) ao SGTC</h2>
                  <p>Ola, <strong>%s</strong>!</p>
                  <p>Sua conta foi criada no Sistema de Gerenciamento de Trabalhos
                     Cientificos. Para ativa-la, confirme seu e-mail clicando no botao abaixo:</p>
                  <p style="text-align:center;margin:32px 0">
                    <a href="%s" style="background:#1976d2;color:#fff;padding:12px 24px;
                       border-radius:6px;text-decoration:none;font-weight:bold">
                       Confirmar e-mail
                    </a>
                  </p>
                  <p style="font-size:12px;color:#555">Se o botao nao funcionar, copie e cole no navegador:<br>
                     <a href="%s">%s</a></p>
                  <p style="font-size:12px;color:#999">Se voce nao solicitou este cadastro, ignore este e-mail.</p>
                </div>
                """.formatted(escape(usuario.getNome()), link, link, link);
        enviarHtml(usuario.getEmail(), assunto, html);
    }

    // -----------------------------------------------------------------
    // 2) Reuniao agendada -> professor (com botoes confirmar/recusar)
    // -----------------------------------------------------------------
    @Async
    public void enviarReuniaoParaProfessor(User professor, Reuniao reuniao, String token,
                                           List<String> nomesAlunos) {
        if (professor == null || professor.getEmail() == null) return;
        String confirmar = apiUrl + "/api/auth/reunioes/confirmar?token=" + urlEncode(token);
        String recusar = apiUrl + "/api/auth/reunioes/recusar?token=" + urlEncode(token);
        Grupo g = reuniao.getGrupo();
        String dataFmt = reuniao.getDataHora() != null ? reuniao.getDataHora().format(FMT) : "(sem data)";
        String alunos = nomesAlunos == null || nomesAlunos.isEmpty()
                ? "(sem alunos cadastrados)"
                : String.join(", ", nomesAlunos);

        String assunto = "Nova reuniao agendada - " + (g != null ? g.getTitulo() : "Grupo");
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto">
                  <h2 style="color:#0d47a1">Nova reuniao agendada</h2>
                  <p>Ola, <strong>%s</strong>.</p>
                  <p>Foi agendada uma reuniao para o grupo <strong>%s</strong>:</p>
                  <table style="border-collapse:collapse;width:100%%;margin:16px 0">
                    <tr><td style="padding:6px;border-bottom:1px solid #eee"><b>Data/Hora</b></td><td style="padding:6px;border-bottom:1px solid #eee">%s</td></tr>
                    <tr><td style="padding:6px;border-bottom:1px solid #eee"><b>Pauta</b></td><td style="padding:6px;border-bottom:1px solid #eee">%s</td></tr>
                    <tr><td style="padding:6px;border-bottom:1px solid #eee"><b>Observacoes</b></td><td style="padding:6px;border-bottom:1px solid #eee">%s</td></tr>
                    <tr><td style="padding:6px"><b>Alunos</b></td><td style="padding:6px">%s</td></tr>
                  </table>
                  <p>Por favor, confirme se podera comparecer:</p>
                  <p style="text-align:center;margin:24px 0">
                    <a href="%s" style="background:#2e7d32;color:#fff;padding:12px 24px;
                       border-radius:6px;text-decoration:none;font-weight:bold;margin-right:8px">
                       Confirmar reuniao
                    </a>
                    <a href="%s" style="background:#c62828;color:#fff;padding:12px 24px;
                       border-radius:6px;text-decoration:none;font-weight:bold">
                       Recusar
                    </a>
                  </p>
                  <p style="font-size:12px;color:#777">Se preferir, abra o sistema:
                     <a href="%s">%s</a></p>
                </div>
                """.formatted(
                        escape(professor.getNome()),
                        escape(g != null ? g.getTitulo() : ""),
                        dataFmt,
                        escape(reuniao.getPauta()),
                        escape(reuniao.getObservacoes() == null ? "-" : reuniao.getObservacoes()),
                        escape(alunos),
                        confirmar, recusar,
                        webUrl, webUrl);
        enviarHtml(professor.getEmail(), assunto, html);
    }

    // -----------------------------------------------------------------
    // 3) Reuniao agendada -> alunos do grupo
    // -----------------------------------------------------------------
    @Async
    public void enviarReuniaoParaAlunos(Collection<User> alunos, Reuniao reuniao) {
        if (alunos == null || alunos.isEmpty()) return;
        Grupo g = reuniao.getGrupo();
        String dataFmt = reuniao.getDataHora() != null ? reuniao.getDataHora().format(FMT) : "(sem data)";
        String assunto = "Reuniao agendada - " + (g != null ? g.getTitulo() : "Grupo");

        for (User aluno : alunos) {
            if (aluno == null || aluno.getEmail() == null) continue;
            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
                      <h2 style="color:#0d47a1">Reuniao agendada para o seu grupo</h2>
                      <p>Ola, <strong>%s</strong>.</p>
                      <p>Foi agendada uma nova reuniao para o grupo <strong>%s</strong>.</p>
                      <table style="border-collapse:collapse;margin:12px 0">
                        <tr><td style="padding:4px"><b>Data/Hora:</b></td><td style="padding:4px">%s</td></tr>
                        <tr><td style="padding:4px"><b>Pauta:</b></td><td style="padding:4px">%s</td></tr>
                      </table>
                      <p style="font-size:12px;color:#777">A confirmacao do professor sera comunicada em seguida.</p>
                      <p><a href="%s">Acessar o sistema</a></p>
                    </div>
                    """.formatted(
                            escape(aluno.getNome()),
                            escape(g != null ? g.getTitulo() : ""),
                            dataFmt,
                            escape(reuniao.getPauta()),
                            webUrl);
            enviarHtml(aluno.getEmail(), assunto, html);
        }
    }

    // -----------------------------------------------------------------
    // 4) Resposta do professor -> alunos
    // -----------------------------------------------------------------
    @Async
    public void enviarRespostaReuniaoParaAlunos(Collection<User> alunos, Reuniao reuniao, boolean confirmada) {
        if (alunos == null || alunos.isEmpty()) return;
        Grupo g = reuniao.getGrupo();
        String dataFmt = reuniao.getDataHora() != null ? reuniao.getDataHora().format(FMT) : "(sem data)";
        String assunto = (confirmada ? "Reuniao confirmada" : "Reuniao recusada") +
                " - " + (g != null ? g.getTitulo() : "Grupo");
        String corTitulo = confirmada ? "#2e7d32" : "#c62828";
        String texto = confirmada
                ? "O professor confirmou que ira participar da reuniao agendada."
                : "O professor nao podera participar da reuniao agendada. Considere remarcar.";

        for (User aluno : alunos) {
            if (aluno == null || aluno.getEmail() == null) continue;
            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
                      <h2 style="color:%s">%s</h2>
                      <p>Ola, <strong>%s</strong>.</p>
                      <p>%s</p>
                      <table style="border-collapse:collapse;margin:12px 0">
                        <tr><td style="padding:4px"><b>Grupo:</b></td><td style="padding:4px">%s</td></tr>
                        <tr><td style="padding:4px"><b>Data/Hora:</b></td><td style="padding:4px">%s</td></tr>
                        <tr><td style="padding:4px"><b>Pauta:</b></td><td style="padding:4px">%s</td></tr>
                      </table>
                      <p><a href="%s">Acessar o sistema</a></p>
                    </div>
                    """.formatted(
                            corTitulo,
                            confirmada ? "Reuniao confirmada pelo professor" : "Reuniao recusada pelo professor",
                            escape(aluno.getNome()),
                            texto,
                            escape(g != null ? g.getTitulo() : ""),
                            dataFmt,
                            escape(reuniao.getPauta()),
                            webUrl);
            enviarHtml(aluno.getEmail(), assunto, html);
        }
    }

    // -----------------------------------------------------------------
    // 5) Upload de documento -> professor(es)
    // -----------------------------------------------------------------
    @Async
    public void enviarUploadDocumentoParaProfessores(Collection<User> professores,
                                                     DocumentoVersao doc, User remetente) {
        if (professores == null || professores.isEmpty()) return;
        Grupo g = doc.getGrupo();
        String assunto = "Novo documento enviado - " + (g != null ? g.getTitulo() : "Grupo");
        for (User prof : professores) {
            if (prof == null || prof.getEmail() == null) continue;
            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
                      <h2 style="color:#0d47a1">Novo documento enviado</h2>
                      <p>Ola, <strong>%s</strong>.</p>
                      <p>O aluno <strong>%s</strong> anexou um novo documento ao grupo
                         <strong>%s</strong>:</p>
                      <table style="border-collapse:collapse;margin:12px 0">
                        <tr><td style="padding:4px"><b>Titulo:</b></td><td style="padding:4px">%s</td></tr>
                        <tr><td style="padding:4px"><b>Versao:</b></td><td style="padding:4px">%s</td></tr>
                      </table>
                      <p><a href="%s">Acessar o sistema para visualizar</a></p>
                    </div>
                    """.formatted(
                            escape(prof.getNome()),
                            escape(remetente != null ? remetente.getNome() : "(desconhecido)"),
                            escape(g != null ? g.getTitulo() : ""),
                            escape(doc.getTitulo()),
                            doc.getVersao(),
                            webUrl);
            enviarHtml(prof.getEmail(), assunto, html);
        }
    }

    // -----------------------------------------------------------------
    // 6) Comentario do professor -> alunos do grupo
    // -----------------------------------------------------------------
    @Async
    public void enviarComentarioParaAlunos(Collection<User> alunos, DocumentoVersao doc,
                                           User autor, String texto) {
        if (alunos == null || alunos.isEmpty()) return;
        Grupo g = doc.getGrupo();
        String assunto = "Novo comentario do professor - " + (g != null ? g.getTitulo() : "Grupo");
        for (User aluno : alunos) {
            if (aluno == null || aluno.getEmail() == null) continue;
            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
                      <h2 style="color:#0d47a1">Novo comentario do professor</h2>
                      <p>Ola, <strong>%s</strong>.</p>
                      <p>O professor <strong>%s</strong> comentou o documento
                         <strong>%s</strong> (versao %s) do grupo <strong>%s</strong>:</p>
                      <blockquote style="border-left:4px solid #1976d2;background:#f5f9ff;
                          margin:12px 0;padding:8px 12px;color:#222">%s</blockquote>
                      <p><a href="%s">Acessar o sistema para responder</a></p>
                    </div>
                    """.formatted(
                            escape(aluno.getNome()),
                            escape(autor != null ? autor.getNome() : ""),
                            escape(doc.getTitulo()),
                            doc.getVersao(),
                            escape(g != null ? g.getTitulo() : ""),
                            escape(texto),
                            webUrl);
            enviarHtml(aluno.getEmail(), assunto, html);
        }
    }

    // -----------------------------------------------------------------
    // 7) Nota final atribuida -> alunos do grupo
    // -----------------------------------------------------------------
    @Async
    public void enviarNotaFinalParaAlunos(Collection<User> alunos, Grupo grupo,
                                          double nota, boolean aprovado) {
        if (alunos == null || alunos.isEmpty()) return;
        String assunto = "Nota final do grupo - " + (grupo != null ? grupo.getTitulo() : "Grupo");
        String corTitulo = aprovado ? "#2e7d32" : "#c62828";
        String resultado = aprovado ? "APROVADO" : "REPROVADO";

        for (User aluno : alunos) {
            if (aluno == null || aluno.getEmail() == null) continue;
            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto">
                      <h2 style="color:%s">Nota final atribuida</h2>
                      <p>Ola, <strong>%s</strong>.</p>
                      <p>A nota final do seu grupo <strong>%s</strong> foi lancada:</p>
                      <table style="border-collapse:collapse;margin:12px 0">
                        <tr><td style="padding:4px"><b>Nota final:</b></td><td style="padding:4px">%.2f</td></tr>
                        <tr><td style="padding:4px"><b>Resultado:</b></td><td style="padding:4px;color:%s"><b>%s</b></td></tr>
                      </table>
                      <p><a href="%s">Acessar o sistema</a></p>
                    </div>
                    """.formatted(
                            corTitulo,
                            escape(aluno.getNome()),
                            escape(grupo != null ? grupo.getTitulo() : ""),
                            nota,
                            corTitulo,
                            resultado,
                            webUrl);
            enviarHtml(aluno.getEmail(), assunto, html);
        }
    }

    // -----------------------------------------------------------------
    // Infra: envio HTML (best-effort)
    // -----------------------------------------------------------------
    private void enviarHtml(String para, String assunto, String html) {
        if (!enabled) {
            log.info("[mail-disabled] To={} subject={} (envio desabilitado por app.mail.enabled=false)",
                    para, assunto);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(html, true);
            try {
                helper.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(fromAddress);
            }
            mailSender.send(msg);
            log.info("[mail-sent] To={} subject={}", para, assunto);
        } catch (MessagingException | RuntimeException e) {
            log.warn("[mail-fail] Falha ao enviar email para {}: {}", para, e.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String urlEncode(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
