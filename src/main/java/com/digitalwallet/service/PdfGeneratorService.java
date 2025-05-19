package com.digitalwallet.service;

import com.digitalwallet.entity.Card;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfGeneratorService {

    public byte[] generateCardListPdf(List<Card> cards) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.setFont(PDType1Font.HELVETICA, 12);

            float margin = 50;
            float yStart = page.getMediaBox().getHeight() - margin;
            float lineHeight = 20;
            float x = margin;
            float y = yStart;

            contentStream.beginText();
            contentStream.newLineAtOffset(x, y);
            contentStream.showText("Card List");
            contentStream.newLineAtOffset(0, -lineHeight);

            for (Card card : cards) {
                if (y < margin + lineHeight) {
                    contentStream.endText();
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    y = yStart;
                    contentStream.beginText();
                    contentStream.newLineAtOffset(x, y);
                }

                String line = "Code: " + card.getCode() + " | Value: " + card.getValue();
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -lineHeight);
                y -= lineHeight;
            }

            contentStream.endText();
            contentStream.close();

            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
