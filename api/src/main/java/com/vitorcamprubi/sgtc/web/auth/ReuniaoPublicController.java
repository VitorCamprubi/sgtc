package com.vitorcamprubi.sgtc.web.auth;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.service.ReuniaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoints publicos invocados pelos botoes "Confirmar / Recusar"
 * presentes no e-mail enviado ao professor quando uma reuniao e' agendada.
 *
 * Por estar sob /api/auth, nao exige autenticacao (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth/reunioes")
public class ReuniaoPublicController {

    private final ReuniaoService service;

    @Value("${app.url.web:http://localhost:4200}")
    private String webUrl;

    public ReuniaoPublicController(ReuniaoService service) {
        this.service = service;
    }

    @GetMapping(value = "/confirmar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmar(@RequestParam("token") String token) {
        return ResponseEntity.ok().body(paginaConfirmacao(token, true));
    }

    @GetMapping(value = "/recusar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> recusar(@RequestParam("token") String token) {
        return ResponseEntity.ok().body(paginaConfirmacao(token, false));
    }

    @PostMapping(value = "/confirmar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmarPost(@RequestParam("token") String token) {
        return responder(token, true);
    }

    @PostMapping(value = "/recusar", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> recusarPost(@RequestParam("token") String token) {
        return responder(token, false);
    }

    private ResponseEntity<String> responder(String token, boolean confirmada) {
        try {
            Reuniao r = service.responderConfirmacao(token, confirmada);
            String corpo = pagina(
                    confirmada ? "Reuniao confirmada" : "Reuniao recusada",
                    confirmada
                            ? "Obrigado! A reuniao foi confirmada e os alunos foram avisados."
                            : "A reuniao foi marcada como recusada e os alunos foram avisados.",
                    confirmada ? "#2e7d32" : "#c62828",
                    r != null && r.getGrupo() != null ? r.getGrupo().getTitulo() : ""
            );
            return ResponseEntity.ok().body(corpo);
        } catch (ResponseStatusException ex) {
            HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
            String corpo = pagina(
                    "Nao foi possivel processar",
                    "Motivo: " + (ex.getReason() == null ? "erro desconhecido" : ex.getReason()),
                    "#c62828",
                    ""
            );
            return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(corpo);
        }
    }

    private String pagina(String titulo, String mensagem, String cor, String grupo) {
        return """
                <!doctype html>
                <html lang="pt-BR"><head><meta charset="utf-8"/>
                  <title>SGTC - %s</title>
                  <meta name="viewport" content="width=device-width,initial-scale=1"/>
                </head>
                <body style="font-family:Arial,sans-serif;background:#f4f6fa;margin:0;padding:0">
                  <div style="max-width:520px;margin:60px auto;background:#fff;border-radius:8px;
                       padding:32px;box-shadow:0 2px 8px rgba(0,0,0,0.08);text-align:center">
                    <h1 style="color:%s;margin-top:0">%s</h1>
                    %s
                    <p>%s</p>
                    <p><a href="%s" style="background:#1976d2;color:#fff;padding:10px 20px;
                       border-radius:6px;text-decoration:none;display:inline-block;margin-top:8px">
                       Abrir o SGTC</a></p>
                  </div>
                </body></html>
                """.formatted(
                        escape(titulo),
                        cor,
                        escape(titulo),
                        grupo == null || grupo.isBlank() ? "" : "<p><b>Grupo:</b> " + escape(grupo) + "</p>",
                        escape(mensagem),
                        webUrl);
    }

    private String paginaConfirmacao(String token, boolean confirmar) {
        String acao = confirmar ? "confirmar" : "recusar";
        String titulo = confirmar ? "Confirmar reuniao" : "Recusar reuniao";
        String mensagem = confirmar
                ? "Clique no botao abaixo para confirmar sua participacao nesta reuniao."
                : "Clique no botao abaixo para recusar esta reuniao. Os alunos serao avisados.";
        String cor = confirmar ? "#2e7d32" : "#c62828";
        String textoBotao = confirmar ? "Confirmar reuniao" : "Recusar reuniao";
        return """
                <!doctype html>
                <html lang="pt-BR"><head><meta charset="utf-8"/>
                  <title>SGTC - %s</title>
                  <meta name="viewport" content="width=device-width,initial-scale=1"/>
                </head>
                <body style="font-family:Arial,sans-serif;background:#f4f6fa;margin:0;padding:0">
                  <div style="max-width:520px;margin:60px auto;background:#fff;border-radius:8px;
                       padding:32px;box-shadow:0 2px 8px rgba(0,0,0,0.08);text-align:center">
                    <h1 style="color:%s;margin-top:0">%s</h1>
                    <p>%s</p>
                    <form method="post" action="/api/auth/reunioes/%s">
                      <input type="hidden" name="token" value="%s"/>
                      <button type="submit" style="background:%s;color:#fff;padding:10px 20px;
                         border:0;border-radius:6px;font-weight:bold;cursor:pointer">%s</button>
                    </form>
                    <p><a href="%s" style="color:#1976d2;text-decoration:none;display:inline-block;margin-top:16px">
                       Voltar ao SGTC</a></p>
                  </div>
                </body></html>
                """.formatted(
                        escape(titulo),
                        cor,
                        escape(titulo),
                        escape(mensagem),
                        acao,
                        escape(token),
                        cor,
                        escape(textoBotao),
                        webUrl);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
