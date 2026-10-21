package com.gww.website.inquiry.service;

import com.gww.website.inquiry.entity.InquiryRecord;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 简单文本报表使用标准OOXML结构流式输出，无需在内存构造完整工作簿。 */
final class InquiryExcel {
  private static final int MAX_DATA_ROWS = 1_048_575;
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final String[] HEADERS = {
    "提交时间", "姓名", "邮箱", "电话", "公司", "国家/地区", "行业", "语言",
    "跟进状态", "留言内容", "历史产品ID", "历史网站", "询盘ID"
  };

  private InquiryExcel() {}

  /** 超过Excel单表行数时自动分表；所有客户输入使用inlineStr，禁止公式解释。 */
  static long write(Iterable<InquiryRecord> records, OutputStream output) throws IOException {
    // 释放压缩器，但响应流仍由Servlet容器负责关闭。
    try (ZipOutputStream zip = new ZipOutputStream(new FilterOutputStream(output) {
      @Override
      public void close() throws IOException {
        flush();
      }
    }, StandardCharsets.UTF_8)) {
      return writeWorkbook(records, zip);
    }
  }

  private static long writeWorkbook(Iterable<InquiryRecord> records, ZipOutputStream zip)
      throws IOException {
    long count = 0;
    int sheet = 1;
    int row = openSheet(zip, sheet);
    for (InquiryRecord record : records) {
      if (row > MAX_DATA_ROWS + 1) {
        closeSheet(zip);
        row = openSheet(zip, ++sheet);
      }
      writeRow(zip, row++, new String[] {
        record.getCreatedAt() == null ? "" : TIME.format(record.getCreatedAt()),
        record.getName(), record.getEmail(), record.getPhone(), record.getCompany(),
        record.getCountry(), record.getIndustry(), record.getLocale(),
        status(record.getStatus()), record.getMessage(), record.getProduct(),
        record.getWebsite(), record.getId()
      });
      count++;
    }
    closeSheet(zip);
    entry(zip, "_rels/.rels", """
        <?xml version="1.0" encoding="UTF-8"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
        """);
    StringBuilder sheets = new StringBuilder();
    StringBuilder relationships = new StringBuilder();
    StringBuilder overrides = new StringBuilder();
    for (int i = 1; i <= sheet; i++) {
      sheets.append("<sheet name=\"询盘").append(i).append("\" sheetId=\"").append(i)
          .append("\" r:id=\"rId").append(i).append("\"/>");
      relationships.append("<Relationship Id=\"rId").append(i)
          .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
          .append(i).append(".xml\"/>");
      overrides.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
          .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
    }
    entry(zip, "xl/workbook.xml",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"
            + sheets + "</sheets></workbook>");
    entry(zip, "xl/_rels/workbook.xml.rels",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + relationships + "</Relationships>");
    entry(zip, "[Content_Types].xml",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + overrides + "</Types>");
    // 完成ZIP但不关闭调用方管理的HTTP响应流。
    zip.finish();
    zip.flush();
    return count;
  }

  private static int openSheet(ZipOutputStream zip, int number) throws IOException {
    zip.putNextEntry(new ZipEntry("xl/worksheets/sheet" + number + ".xml"));
    text(zip, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"13\" width=\"25\" customWidth=\"1\"/></cols><sheetData>");
    writeRow(zip, 1, HEADERS);
    return 2;
  }

  private static void closeSheet(ZipOutputStream zip) throws IOException {
    text(zip, "</sheetData></worksheet>");
    zip.closeEntry();
  }

  private static void writeRow(ZipOutputStream zip, int row, String[] values) throws IOException {
    StringBuilder xml = new StringBuilder("<row r=\"").append(row).append("\">");
    for (int i = 0; i < values.length; i++) {
      xml.append("<c r=\"").append((char) ('A' + i)).append(row)
          .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
          .append(escape(values[i])).append("</t></is></c>");
    }
    text(zip, xml.append("</row>").toString());
  }

  /** 丢弃XML 1.0不允许的控制字符，保留客户文本及换行。 */
  private static String escape(String value) {
    if (value == null) return "";
    StringBuilder clean = new StringBuilder();
    value.codePoints().filter(c -> c == 9 || c == 10 || c == 13
        || (c >= 32 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD)
        || (c >= 0x10000 && c <= 0x10FFFF)).forEach(clean::appendCodePoint);
    return clean.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String status(String value) {
    if (value == null) return "";
    return switch (value) {
      case "NEW" -> "待处理";
      case "PROCESSING" -> "跟进中";
      case "CLOSED" -> "已完成";
      default -> value;
    };
  }

  private static void entry(ZipOutputStream zip, String name, String value) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    text(zip, value);
    zip.closeEntry();
  }

  private static void text(ZipOutputStream zip, String value) throws IOException {
    zip.write(value.getBytes(StandardCharsets.UTF_8));
  }
}
