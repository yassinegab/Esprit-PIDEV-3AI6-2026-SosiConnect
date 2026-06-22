package org.example.aideEtdon.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.example.aideEtdon.model.Alerte;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportService {

    private static final Color TEAL_DARK = new Color(15, 118, 110);
    private static final Color TEAL_LIGHT = new Color(240, 253, 250);
    private static final Color RED_DANGER = new Color(185, 28, 28);
    private static final Color GREEN_SUCCESS = new Color(22, 163, 74);
    private static final Color GRAY_TEXT = new Color(100, 116, 139);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    public static void exportAlertesToPdf(List<Alerte> alertes, File outputFile) throws Exception {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, new FileOutputStream(outputFile));
        document.open();

        // === HEADER ===
        Font headerFont = new Font(Font.HELVETICA, 22, Font.BOLD, TEAL_DARK);
        Paragraph title = new Paragraph("SosiConnect", headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subHeaderFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(51, 65, 85));
        Paragraph subtitle = new Paragraph("Rapport d'Alertes d'Urgence", subHeaderFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(4);
        document.add(subtitle);

        Font dateFont = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
        Paragraph datePara = new Paragraph(
                "Généré le : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                dateFont);
        datePara.setAlignment(Element.ALIGN_CENTER);
        datePara.setSpacingAfter(20);
        document.add(datePara);

        // === STATISTICS SUMMARY ===
        long total = alertes.size();
        long enAttente = alertes.stream().filter(a -> "En Attente".equals(a.getStatut())).count();
        long resolues = alertes.stream().filter(a -> "Résolue".equals(a.getStatut())).count();

        PdfPTable statsTable = new PdfPTable(3);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(20);
        statsTable.setWidths(new float[]{1, 1, 1});

        statsTable.addCell(createStatCell("TOTAL", String.valueOf(total), new Color(241, 245, 249), GRAY_TEXT));
        statsTable.addCell(createStatCell("EN ATTENTE", String.valueOf(enAttente), new Color(254, 252, 232), new Color(180, 83, 9)));
        statsTable.addCell(createStatCell("RÉSOLUES", String.valueOf(resolues), new Color(240, 253, 244), GREEN_SUCCESS));

        document.add(statsTable);

        // === ALERTS TABLE ===
        if (alertes.isEmpty()) {
            Font emptyFont = new Font(Font.HELVETICA, 12, Font.ITALIC, GRAY_TEXT);
            Paragraph emptyPara = new Paragraph("Aucune alerte enregistrée.", emptyFont);
            emptyPara.setAlignment(Element.ALIGN_CENTER);
            emptyPara.setSpacingBefore(30);
            document.add(emptyPara);
        } else {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[]{0.8f, 2.5f, 2f, 2.5f, 1.2f});

            // Header row
            String[] headers = {"#", "Type de Besoin", "Date", "Localisation", "Statut"};
            for (String h : headers) {
                table.addCell(createHeaderCell(h));
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int idx = 1;
            for (Alerte a : alertes) {
                boolean isResolved = "Résolue".equals(a.getStatut());
                Color rowColor = (idx % 2 == 0) ? new Color(248, 250, 252) : WHITE;

                table.addCell(createDataCell(String.valueOf(idx), rowColor, Element.ALIGN_CENTER));
                table.addCell(createDataCell(a.getTypeBesoin(), rowColor, Element.ALIGN_LEFT));
                table.addCell(createDataCell(a.getDateAlerte().format(dtf), rowColor, Element.ALIGN_LEFT));
                table.addCell(createDataCell(
                        "Lat: " + String.format("%.4f", a.getLatitude()) + "\nLng: " + String.format("%.4f", a.getLongitude()),
                        rowColor, Element.ALIGN_LEFT));
                table.addCell(createStatusCell(a.getStatut(), isResolved));
                idx++;
            }

            document.add(table);
        }

        // === FOOTER ===
        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(148, 163, 184));
        Paragraph footer = new Paragraph("Document confidentiel - SosiConnect Medical Platform", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);

        document.close();
    }

    private static PdfPCell createStatCell(String label, String value, Color bgColor, Color valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(1);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(100, 116, 139));
        Font valueFont = new Font(Font.HELVETICA, 20, Font.BOLD, valueColor);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", labelFont));
        p.add(new Chunk(value, valueFont));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        return cell;
    }

    private static PdfPCell createHeaderCell(String text) {
        Font font = new Font(Font.HELVETICA, 11, Font.BOLD, WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(TEAL_DARK);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    private static PdfPCell createDataCell(String text, Color bgColor, int align) {
        Font font = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(51, 65, 85));
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    private static PdfPCell createStatusCell(String statut, boolean isResolved) {
        Color bg = isResolved ? new Color(240, 253, 244) : new Color(254, 242, 242);
        Color fg = isResolved ? GREEN_SUCCESS : RED_DANGER;
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD, fg);

        PdfPCell cell = new PdfPCell(new Phrase(statut.toUpperCase(), font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }
}
