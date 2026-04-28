package com.vitorcamprubi.sgtc.service;

import com.vitorcamprubi.sgtc.domain.Reuniao;
import com.vitorcamprubi.sgtc.domain.ReuniaoDesempenhoGrupo;
import com.vitorcamprubi.sgtc.domain.User;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReuniaoPdfService {
    private static final String TEMPLATE_PATH = "templates/reuniao-template.pdf";
    private static final DateTimeFormatter DATE_DAY_FORMAT = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter DATE_MONTH_FORMAT = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DATE_YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");

    private static final PDFont FONT_TEXT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float ATIVIDADES_FONT_SIZE = 9f;
    private static final float ATIVIDADES_LINE_HEIGHT = 21.90f;
    private static final float ATIVIDADES_FIRST_LINE_CENTER_OFFSET_Y = -0.95f;
    private static final float DESEMPENHO_MARK_FONT_SIZE = 10.4f;
    private static final float DESEMPENHO_MARK_OFFSET_Y = -0.4f;

    private record Slot(int pagina,
                        float dataMinX,
                        float dataMaxX,
                        float dataY,
                        float atividadesX,
                        float atividadesY,
                        float atividadesMaxWidth,
                        int atividadesMaxLinhas,
                        float atividadesLineHeight,
                        float desempenhoY,
                        float assinaturaY) {
    }

    private static final Map<Integer, Slot> SLOTS = Map.of(
            1, new Slot(0, 103.6f, 203.6f, 666f, 244f, 684f, 297f, 4, ATIVIDADES_LINE_HEIGHT, 587f, 526f),
            2, new Slot(0, 103.6f, 203.6f, 452f, 244f, 470f, 297f, 4, ATIVIDADES_LINE_HEIGHT, 373f, 309f),
            3, new Slot(0, 103.6f, 203.6f, 235f, 244f, 253f, 297f, 4, ATIVIDADES_LINE_HEIGHT, 156f, 92f),
            4, new Slot(1, 103.6f, 203.6f, 687f, 244f, 705f, 297f, 4, ATIVIDADES_LINE_HEIGHT, 608f, 547f),
            5, new Slot(1, 103.6f, 203.6f, 473f, 244f, 491f, 297f, 4, ATIVIDADES_LINE_HEIGHT, 394f, 329f),
            6, new Slot(1, 103.6f, 203.6f, 256f, 244f, 274f, 297f, 4, ATIVIDADES_LINE_HEIGHT, 177f, 112f)
    );

    private final ReuniaoService reuniaoService;

    public ReuniaoPdfService(ReuniaoService reuniaoService) {
        this.reuniaoService = reuniaoService;
    }

    public byte[] gerarPdfExecutadasDoGrupo(Long grupoId, User atual) {
        List<Reuniao> executadas = reuniaoService.listarExecutadasParaPdf(grupoId, atual);
        Map<Integer, Reuniao> porEncontro = indexarPorEncontro(executadas);

        byte[] templateBytes = carregarTemplate();
        try (PDDocument doc = Loader.loadPDF(templateBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (Map.Entry<Integer, Reuniao> item : porEncontro.entrySet()) {
                Slot slot = SLOTS.get(item.getKey());
                if (slot == null) {
                    continue;
                }
                preencherEncontro(doc, slot, item.getValue());
            }

            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao gerar PDF das reunioes",
                    e
            );
        }
    }

    private Map<Integer, Reuniao> indexarPorEncontro(List<Reuniao> executadas) {
        Map<Integer, Reuniao> porEncontro = new LinkedHashMap<>();
        for (Reuniao reuniao : executadas) {
            Integer encontro = reuniao.getNumeroEncontro();
            if (encontro == null || encontro < 1 || encontro > 6) {
                continue;
            }
            porEncontro.put(encontro, reuniao);
        }
        return porEncontro;
    }

    private byte[] carregarTemplate() {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Template de reuniao nao encontrado",
                    e
            );
        }
    }

    private void preencherEncontro(PDDocument doc, Slot slot, Reuniao reuniao) throws IOException {
        PDPage page = doc.getPage(slot.pagina());
        try (PDPageContentStream stream = new PDPageContentStream(
                doc,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true)) {
            desenharDataEmBlocos(
                    stream,
                    slot,
                    reuniao.getDataAtividadesRealizadas()
            );

            desenharMultilinha(
                    stream,
                    FONT_TEXT,
                    ATIVIDADES_FONT_SIZE,
                    slot.atividadesX(),
                    slot.atividadesY(),
                    slot.atividadesMaxWidth(),
                    slot.atividadesMaxLinhas(),
                    slot.atividadesLineHeight(),
                    reuniao.getAtividadesRealizadas()
            );

            desenharMarcacaoDesempenho(stream, slot.desempenhoY(), reuniao.getDesempenhoGrupo());

            desenharTextoCentralizado(
                    stream,
                    FONT_TEXT,
                    9f,
                    72.9f,
                    234.4f,
                    slot.assinaturaY(),
                    reuniao.getProfessorDisciplina()
            );
            desenharTextoCentralizado(
                    stream,
                    FONT_TEXT,
                    9f,
                    234.9f,
                    428.0f,
                    slot.assinaturaY(),
                    reuniao.getOrientadorAssinatura()
            );
            desenharTextoCentralizado(
                    stream,
                    FONT_TEXT,
                    9f,
                    428.5f,
                    551.0f,
                    slot.assinaturaY(),
                    reuniao.getCoorientadorAssinatura()
            );
        }
    }

    private void desenharMarcacaoDesempenho(PDPageContentStream stream, float y, ReuniaoDesempenhoGrupo desempenho)
            throws IOException {
        if (desempenho == null) {
            return;
        }
        float centerX;
        switch (desempenho) {
            case RUIM -> centerX = 181.13f;
            case REGULAR -> centerX = 264.78f;
            case BOM -> centerX = 370.16f;
            case OTIMO -> centerX = 472.06f;
            default -> {
                return;
            }
        }
        desenharTextoNoCentro(
                stream,
                FONT_BOLD,
                DESEMPENHO_MARK_FONT_SIZE,
                centerX,
                y + DESEMPENHO_MARK_OFFSET_Y,
                "X"
        );
    }

    private void desenharDataEmBlocos(PDPageContentStream stream, Slot slot, java.time.LocalDate data)
            throws IOException {
        if (data == null) {
            return;
        }

        float y = slot.dataY();
        float xMin = slot.dataMinX();
        float xMax = slot.dataMaxX();

        float dayMin = xMin;
        float dayMax = xMin + 26.5f;
        float monthMin = xMin + 33f;
        float monthMax = xMin + 59.5f;
        float yearMin = xMin + 66f;
        float yearMax = xMax;

        desenharTextoCentralizado(stream, FONT_BOLD, 8.5f, dayMin, dayMax, y, data.format(DATE_DAY_FORMAT));
        desenharTextoCentralizado(stream, FONT_BOLD, 8.5f, monthMin, monthMax, y, data.format(DATE_MONTH_FORMAT));
        desenharTextoCentralizado(stream, FONT_BOLD, 8.5f, yearMin, yearMax, y, data.format(DATE_YEAR_FORMAT));
    }

    private void desenharTexto(PDPageContentStream stream, PDFont font, float fontSize, float x, float y, String text)
            throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(sanitizar(text));
        stream.endText();
    }

    private void desenharTextoCentralizado(PDPageContentStream stream,
                                           PDFont font,
                                           float fontSize,
                                           float xMin,
                                           float xMax,
                                           float y,
                                           String text) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        String ajustado = cortarParaLargura(font, fontSize, text.trim(), (xMax - xMin) - 6f);
        float largura = larguraTexto(font, fontSize, ajustado);
        float x = xMin + ((xMax - xMin - largura) / 2f);
        desenharTexto(stream, font, fontSize, x, y, ajustado);
    }

    private void desenharMultilinha(PDPageContentStream stream,
                                    PDFont font,
                                    float fontSize,
                                    float x,
                                    float yInicial,
                                    float maxWidth,
                                    int maxLinhas,
                                    float lineHeight,
                                    String text) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        List<String> linhas = quebrarLinhas(font, fontSize, text.trim(), maxWidth, maxLinhas);
        float centerY = yInicial + ATIVIDADES_FIRST_LINE_CENTER_OFFSET_Y;
        for (String linha : linhas) {
            desenharTextoComCentroY(stream, font, fontSize, x, centerY, linha);
            centerY -= lineHeight;
        }
    }

    private void desenharTextoComCentroY(PDPageContentStream stream,
                                         PDFont font,
                                         float fontSize,
                                         float x,
                                         float centerY,
                                         String text) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        float ascent = 700f;
        float descent = -200f;
        PDFontDescriptor descriptor = font.getFontDescriptor();
        if (descriptor != null) {
            ascent = descriptor.getAscent();
            descent = descriptor.getDescent();
        }
        float baseline = centerY - ((ascent + descent) / 2f) * (fontSize / 1000f);
        desenharTexto(stream, font, fontSize, x, baseline, text);
    }

    private void desenharTextoNoCentro(PDPageContentStream stream,
                                       PDFont font,
                                       float fontSize,
                                       float centerX,
                                       float centerY,
                                       String text) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        String ajustado = sanitizar(text);
        float largura = larguraTexto(font, fontSize, ajustado);
        float ascent = 700f;
        float descent = -200f;
        PDFontDescriptor descriptor = font.getFontDescriptor();
        if (descriptor != null) {
            ascent = descriptor.getAscent();
            descent = descriptor.getDescent();
        }
        float baseline = centerY - ((ascent + descent) / 2f) * (fontSize / 1000f);
        desenharTexto(stream, font, fontSize, centerX - (largura / 2f), baseline, ajustado);
    }

    private List<String> quebrarLinhas(PDFont font, float fontSize, String text, float maxWidth, int maxLinhas)
            throws IOException {
        String[] palavras = text.replace("\r", " ").replace("\n", " ").trim().split("\\s+");
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean excedeuLimite = false;

        for (String palavra : palavras) {
            String candidata = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (larguraTexto(font, fontSize, candidata) <= maxWidth) {
                atual.setLength(0);
                atual.append(candidata);
                continue;
            }

            if (!atual.isEmpty()) {
                linhas.add(atual.toString());
                atual.setLength(0);
            }

            if (larguraTexto(font, fontSize, palavra) > maxWidth) {
                linhas.add(cortarParaLargura(font, fontSize, palavra, maxWidth));
            } else {
                atual.append(palavra);
            }

            if (linhas.size() >= maxLinhas) {
                excedeuLimite = true;
                break;
            }
        }

        if (!atual.isEmpty() && linhas.size() < maxLinhas) {
            linhas.add(atual.toString());
        } else if (!atual.isEmpty()) {
            excedeuLimite = true;
        }

        if (linhas.size() > maxLinhas) {
            linhas = linhas.subList(0, maxLinhas);
            excedeuLimite = true;
        }

        if (excedeuLimite && !linhas.isEmpty()) {
            int idx = linhas.size() - 1;
            linhas.set(idx, cortarComReticencias(font, fontSize, linhas.get(idx), maxWidth));
        }

        return linhas;
    }

    private String cortarParaLargura(PDFont font, float fontSize, String text, float maxWidth) throws IOException {
        String valor = sanitizar(text);
        if (larguraTexto(font, fontSize, valor) <= maxWidth) {
            return valor;
        }
        StringBuilder sb = new StringBuilder(valor);
        while (sb.length() > 0 && larguraTexto(font, fontSize, sb.toString()) > maxWidth) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString().trim();
    }

    private String cortarComReticencias(PDFont font, float fontSize, String text, float maxWidth) throws IOException {
        String base = cortarParaLargura(font, fontSize, text, maxWidth);
        if (base.isEmpty()) {
            return base;
        }
        String withDots = base + "...";
        if (larguraTexto(font, fontSize, withDots) <= maxWidth) {
            return withDots;
        }
        float larguraPontos = larguraTexto(font, fontSize, "...");
        String prefixo = cortarParaLargura(font, fontSize, base, Math.max(0f, maxWidth - larguraPontos));
        return prefixo + "...";
    }

    private float larguraTexto(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(sanitizar(text)) * fontSize / 1000f;
    }

    private String sanitizar(String text) {
        // PDFBox com Helvetica padrao usa WinAnsi.
        return text == null ? "" : text.replace("\u0000", "").trim();
    }
}
