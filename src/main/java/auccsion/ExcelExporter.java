package auccsion;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class ExcelExporter {

    public static void export(List<LotData> lots, String filePath) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lots");

            // === СТИЛИ ===
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper helper = workbook.getCreationHelper();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd.MM.yyyy HH:mm"));

            // === ЗАГОЛОВКИ (1-в-1 по ТЗ) ===
            String[] headers = {
                    "Номер аукциона / лота",
                    "Адрес объекта",
                    "Начальная цена",
                    "Шаг аукциона",
                    "Размер задатка",
                    "Дата и время начала торгов",
                    "Дата и время окончания торгов",
                    "Ссылка на документацию (если есть)",
                    "Статус аукциона",
                    "Информация о должнике",
                    "Описание объекта (площадь, этаж и т.д.)"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // === ДАННЫЕ ===
            int rowNum = 1;
            for (LotData lot : lots) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(clean(joinAuctionLot(lot)));         // 1
                row.createCell(1).setCellValue(clean(lot.getAddress()));            // 2
                row.createCell(2).setCellValue(clean(lot.getStartPrice()));         // 3
                row.createCell(3).setCellValue(clean(lot.getBidStep()));            // 4
                row.createCell(4).setCellValue(clean(lot.getDepositAmount()));      // 5

                setDateCell(row, 5, lot.getStartAuc(), dateStyle);                 // 6


                row.createCell(7).setCellValue(clean(lot.getDocumentationUrl()));   // 8
                row.createCell(8).setCellValue(clean(lot.getAuctionStatus()));      // 9
                row.createCell(9).setCellValue(clean(lot.getDebtorInfo()));         // 10
                row.createCell(10).setCellValue(clean(lot.getDescription()));       // 11
            }

            // === АВТОШИРИНА (и ограничение ширины, чтобы ссылки не раздували файл) ===
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                int max = 12000;
                if (w > max) sheet.setColumnWidth(i, max);
            }

            // === СОХРАНЕНИЕ ===
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            System.out.println("✅ Excel файл сохранён: " + filePath);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка экспорта в Excel", e);
        }
    }

    private static String joinAuctionLot(LotData lot) {
        String lotNum = clean(lot.getLotNumber());
        String aucNum = clean(lot.getAuctionNumber());

        String left = lotNum.isEmpty() ? "" : ("Лот №" + lotNum);
        String right = aucNum.isEmpty() ? "" : ("Торги №" + aucNum);

        if (!left.isEmpty() && !right.isEmpty()) return left + " / " + right;
        if (!left.isEmpty()) return left;
        return right;
    }

    private static String clean(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.equalsIgnoreCase("null")
                || value.equals("-")
                || value.equalsIgnoreCase("н/д")
                || value.equalsIgnoreCase("н/б")) {
            return "";
        }
        return value;
    }

    private static void setDateCell(Row row, int index, LocalDateTime dateTime, CellStyle style) {
        Cell cell = row.createCell(index);

        if (dateTime == null) {
            cell.setCellValue("");
            return;
        }

        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        cell.setCellValue(date);
        cell.setCellStyle(style);
    }
}