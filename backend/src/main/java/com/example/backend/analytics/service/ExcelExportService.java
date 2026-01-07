package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.AnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class ExcelExportService {

    /**
     * Export analysis results to Excel format
     * Creates a structured workbook with categories and questions
     * 
     * @param analysis The analysis results to export
     * @return byte array of the Excel file
     * @throws IOException if Excel creation fails
     */
    public byte[] exportToExcel(AnalysisResponse analysis) throws IOException {
        log.info("📊 Creating Excel file from analysis");

        // Create workbook and sheet
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ניתוח שאלות");

            // Set RTL (Right-to-Left) for Hebrew
            sheet.setRightToLeft(true);

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle categoryStyle = createCategoryStyle(workbook);
            CellStyle questionStyle = createQuestionStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle countStyle = createCountStyle(workbook);

            int rowNum = 0;

            // Title row
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ניתוח שאלות - Custom Site Chat");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            // Summary row
            Row summaryRow = sheet.createRow(rowNum++);
            Cell summaryCell = summaryRow.createCell(0);
            summaryCell.setCellValue("סיכום: " + analysis.getSummary());
            summaryCell.setCellStyle(questionStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 3));

            // Total questions row
            Row totalRow = sheet.createRow(rowNum++);
            Cell totalCell = totalRow.createCell(0);
            totalCell.setCellValue("סה\"כ שאלות: " + analysis.getTotalQuestions());
            totalCell.setCellStyle(questionStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 3));

            // Empty row
            rowNum++;

            // Loop through categories
            for (AnalysisResponse.CategoryData category : analysis.getCategories()) {
                // Category header row
                Row categoryRow = sheet.createRow(rowNum++);
                Cell categoryCell = categoryRow.createCell(0);
                categoryCell.setCellValue(category.getCategoryName() + " (" + category.getTotalCount() + " שאלות)");
                categoryCell.setCellStyle(categoryStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

                // Table header row
                Row headerRow = sheet.createRow(rowNum++);
                Cell h1 = headerRow.createCell(0);
                h1.setCellValue("#");
                h1.setCellStyle(headerStyle);

                Cell h2 = headerRow.createCell(1);
                h2.setCellValue("שאלה");
                h2.setCellStyle(headerStyle);

                Cell h3 = headerRow.createCell(2);
                h3.setCellValue("מספר פעמים");
                h3.setCellStyle(headerStyle);

                // Questions rows
                int questionNum = 1;
                for (AnalysisResponse.QuestionData question : category.getQuestions()) {
                    Row questionRow = sheet.createRow(rowNum++);

                    // Number cell
                    Cell numCell = questionRow.createCell(0);
                    numCell.setCellValue(questionNum++);
                    numCell.setCellStyle(numberStyle);

                    // Question cell
                    Cell qCell = questionRow.createCell(1);
                    qCell.setCellValue(question.getQuestion());
                    qCell.setCellStyle(questionStyle);

                    // Count cell
                    Cell countCell = questionRow.createCell(2);
                    countCell.setCellValue(question.getCount());
                    countCell.setCellStyle(countStyle);
                }

                // Empty row after each category
                rowNum++;
            }

            // Auto-size columns
            sheet.setColumnWidth(0, 2000);  // Number column
            sheet.setColumnWidth(1, 15000); // Question column
            sheet.setColumnWidth(2, 4000);  // Count column

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            log.info("✅ Excel file created successfully");
            return outputStream.toByteArray();
        }
    }

    /**
     * Create header style (bold, centered, background color)
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    /**
     * Create category style (bold, larger font, background)
     */
    private CellStyle createCategoryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        return style;
    }

    /**
     * Create question text style (regular, right-aligned)
     */
    private CellStyle createQuestionStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    /**
     * Create number style (centered)
     */
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    /**
     * Create count style (centered, bold)
     */
    private CellStyle createCountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}