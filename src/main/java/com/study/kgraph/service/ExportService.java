package com.study.kgraph.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.study.kgraph.entity.Note;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.OutputStream;

@Service
public class ExportService {

    public void exportToPdf(Note note, OutputStream out) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        // Font settings for Chinese support
        BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfChinese, 18, Font.BOLD);
        Font normalFont = new Font(bfChinese, 12, Font.NORMAL);
        Font summaryTitleFont = new Font(bfChinese, 14, Font.BOLD, BaseColor.BLUE);

        // Title
        Paragraph title = new Paragraph(note.getTitle(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Original Text
        document.add(new Paragraph("原始内容：", summaryTitleFont));
        Paragraph text = new Paragraph(note.getText() != null ? note.getText() : "无内容", normalFont);
        text.setSpacingAfter(20);
        document.add(text);

        // Summary
        if (note.getSummary() != null && !note.getSummary().isEmpty()) {
            document.add(new Paragraph("AI 总结：", summaryTitleFont));
            Paragraph summary = new Paragraph(note.getSummary(), normalFont);
            document.add(summary);
        }

        document.close();
    }

    public void exportToWord(Note note, OutputStream out) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            // Title
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(note.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            // Original Text Header
            XWPFParagraph textHeaderPara = document.createParagraph();
            XWPFRun textHeaderRun = textHeaderPara.createRun();
            textHeaderRun.setText("原始内容：");
            textHeaderRun.setBold(true);
            textHeaderRun.setFontSize(14);
            textHeaderRun.setColor("0000FF");

            // Original Text Body
            XWPFParagraph textPara = document.createParagraph();
            String[] textLines = (note.getText() != null ? note.getText() : "无内容").split("\n");
            for (String line : textLines) {
                XWPFRun textRun = textPara.createRun();
                textRun.setText(line);
                textRun.addBreak();
            }

            // Summary
            if (note.getSummary() != null && !note.getSummary().isEmpty()) {
                XWPFParagraph summaryHeaderPara = document.createParagraph();
                XWPFRun summaryHeaderRun = summaryHeaderPara.createRun();
                summaryHeaderRun.setText("AI 总结：");
                summaryHeaderRun.setBold(true);
                summaryHeaderRun.setFontSize(14);
                summaryHeaderRun.setColor("0000FF");

                XWPFParagraph summaryPara = document.createParagraph();
                String[] summaryLines = note.getSummary().split("\n");
                for (String line : summaryLines) {
                    XWPFRun summaryRun = summaryPara.createRun();
                    summaryRun.setText(line);
                    summaryRun.addBreak();
                }
            }

            document.write(out);
        }
    }
}
