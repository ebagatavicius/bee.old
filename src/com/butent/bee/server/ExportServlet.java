package com.butent.bee.server;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import com.butent.bee.server.http.HttpUtils;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XPicture;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.export.XWorkbook;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/export")
@SuppressWarnings("serial")
public class ExportServlet extends LoginServlet {

  private static BeeLogger logger = LogUtils.getLogger(ExportServlet.class);

  private static ListMultimap<String, XRow> exportedRows =
      Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

  private static void closeWorkbook(Workbook workbook) {
    try {
      workbook.close();
    } catch (IOException ex) {
      logger.warning("exception closing workbook");
    }
  }

  private static short convertBorderStyle(BorderStyle input) {
    if (input == null) {
      return CellStyle.BORDER_NONE;
    }

    switch (input) {
      case DASHED:
        return CellStyle.BORDER_DASHED;
      case DOTTED:
        return CellStyle.BORDER_DOTTED;
      case DOUBLE:
        return CellStyle.BORDER_DOUBLE;

      case HIDDEN:
      case NONE:
        return CellStyle.BORDER_NONE;

      case GROOVE:
      case INSET:
      case OUTSET:
      case RIDGE:
        return CellStyle.BORDER_MEDIUM;

      case SOLID:
        return CellStyle.BORDER_THIN;
    }

    Assert.untouchable();
    return CellStyle.BORDER_NONE;
  }

  private static Font convertFont(XSSFWorkbook wb, XFont input) {
    if (input == null) {
      return null;
    }

    XSSFFont font = wb.createFont();

    if (!BeeUtils.isEmpty(input.getName())) {
      font.setFontName(input.getName());
    }
    if (BeeUtils.isPositive(input.getFactor())) {
      int hp = BeeUtils.round(font.getFontHeightInPoints() * input.getFactor());
      if (hp > 0) {
        font.setFontHeightInPoints((short) hp);
      }
    }

    if (input.getWeight() != null) {
      switch (input.getWeight()) {
        case BOLD:
        case BOLDER:
          font.setBoldweight(Font.BOLDWEIGHT_BOLD);
          break;

        case NORMAL:
          font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
          break;

        default:
          logger.warning("font weight", input.getWeight(), "not converted");
      }
    }

    if (input.getStyle() != null) {
      switch (input.getStyle()) {
        case ITALIC:
        case OBLIQUE:
          font.setItalic(true);
          break;
        case NORMAL:
          font.setItalic(false);
          break;
      }
    }

    if (!BeeUtils.isEmpty(input.getColor())) {
      XSSFColor color = createColor(input.getColor());

      if (color != null) {
        font.setColor(color);
      }
    }

    return font;
  }

  private static CellStyle convertStyle(XSSFWorkbook wb, XStyle input, Map<Integer, Font> fonts) {
    if (input == null) {
      return null;
    }

    XSSFCellStyle cellStyle = wb.createCellStyle();

    if (!BeeUtils.isEmpty(input.getColor())) {
      XSSFColor color = createColor(input.getColor());

      if (color != null) {
        cellStyle.setFillForegroundColor(color);
        cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
      }
    }

    if (input.getFontRef() != null) {
      Font font = fonts.get(input.getFontRef());

      if (font == null) {
        logger.warning("invalid font index", input.getFontRef());
      } else {
        cellStyle.setFont(font);
      }
    }

    if (!BeeUtils.isEmpty(input.getFormat())) {
      cellStyle.setDataFormat(wb.createDataFormat().getFormat(input.getFormat()));
    }

    if (input.getTextAlign() != null) {
      switch (input.getTextAlign()) {
        case CENTER:
          cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
          break;
        case END:
        case RIGHT:
          cellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
          break;
        case JUSTIFY:
          cellStyle.setAlignment(CellStyle.ALIGN_JUSTIFY);
          break;
        case LEFT:
        case START:
          cellStyle.setAlignment(CellStyle.ALIGN_LEFT);
          break;
        case MATCH_PARENT:
          cellStyle.setAlignment(CellStyle.ALIGN_GENERAL);
          break;
        case START_END:
          cellStyle.setAlignment(CellStyle.ALIGN_FILL);
          break;
      }
    }

    if (input.getVerticalAlign() != null) {
      switch (input.getVerticalAlign()) {
        case BOTTOM:
          cellStyle.setVerticalAlignment(CellStyle.VERTICAL_BOTTOM);
          break;
        case CENTRAL:
        case MIDDLE:
          cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
          break;
        case TOP:
          cellStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
          break;
        default:
          logger.warning("vertical align", input.getVerticalAlign(), "not converted");
      }
    }

    if (input.getBorderLeft() != null) {
      cellStyle.setBorderLeft(convertBorderStyle(input.getBorderLeft()));
    }

    if (input.getBorderRight() != null) {
      cellStyle.setBorderRight(convertBorderStyle(input.getBorderRight()));
    }

    if (input.getBorderTop() != null) {
      cellStyle.setBorderTop(convertBorderStyle(input.getBorderTop()));
    }

    if (input.getBorderBottom() != null) {
      cellStyle.setBorderBottom(convertBorderStyle(input.getBorderBottom()));
    }

    return cellStyle;
  }

  private static XSSFColor createColor(String input) {
    byte[] rgb = Color.getRgb(input);

    if (rgb == null) {
      logger.warning("cannot parse color", input);
      return null;

    } else {
      return new XSSFColor(rgb);
    }
  }

  private static Picture createPicture(CreationHelper creationHelper, Drawing drawing,
      int pictureIndex, int rowIndex, int colIndex) {

    ClientAnchor anchor = creationHelper.createClientAnchor();
    anchor.setRow1(rowIndex);
    anchor.setCol1(colIndex);
    anchor.setRow2(rowIndex);
    anchor.setCol2(colIndex);
    anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);

    Picture picture = drawing.createPicture(anchor, pictureIndex);
    picture.resize();

    return picture;
  }

  private static Workbook createWorkbook(XWorkbook inputBook) {
    long start = System.currentTimeMillis();

    XSSFWorkbook wb = new XSSFWorkbook();

    CreationHelper creationHelper = null;

    for (XSheet inputSheet : inputBook.getSheets()) {
      Sheet sheet;
      if (BeeUtils.isEmpty(inputSheet.getName())) {
        sheet = wb.createSheet();
      } else {
        sheet = wb.createSheet(inputSheet.getName());
      }

      float defaultRowHeightInPoints = sheet.getDefaultRowHeightInPoints();
      if (BeeUtils.isPositive(inputSheet.getRowHeightFactor())) {
        sheet.setDefaultRowHeightInPoints(defaultRowHeightInPoints
            * inputSheet.getRowHeightFactor().floatValue());
      }

      if (!inputSheet.getColumnWidthFactors().isEmpty()) {
        for (Map.Entry<Integer, Double> entry : inputSheet.getColumnWidthFactors().entrySet()) {
          Integer columnIndex = entry.getKey();
          Double widthFactor = entry.getValue();

          if (BeeUtils.isNonNegative(columnIndex) && BeeUtils.isPositive(widthFactor)) {
            int width = BeeUtils.round(sheet.getColumnWidth(columnIndex) * widthFactor);
            if (width > 0) {
              sheet.setColumnWidth(columnIndex, width);
            }
          }
        }
      }

      Map<Integer, Font> fonts = new HashMap<>();
      if (!inputSheet.getFonts().isEmpty()) {
        for (int i = 0; i < inputSheet.getFonts().size(); i++) {
          XFont inputFont = inputSheet.getFont(i);
          if (inputFont != null) {
            fonts.put(i, convertFont(wb, inputFont));
          }
        }
      }

      Map<Integer, CellStyle> styles = new HashMap<>();
      if (!inputSheet.getStyles().isEmpty()) {
        for (int i = 0; i < inputSheet.getStyles().size(); i++) {
          XStyle inputStyle = inputSheet.getStyle(i);
          if (inputStyle != null) {
            styles.put(i, convertStyle(wb, inputStyle, fonts));
          }
        }
      }

      Map<Integer, Integer> pictures = addPictures(inputSheet.getPictures(), wb);
      Drawing drawing = null;

      CellStyle wrapStyle = wb.createCellStyle();
      wrapStyle.setWrapText(true);

      for (XRow inputRow : inputSheet.getRows()) {
        Row row = sheet.createRow(inputRow.getIndex());

        Double heightFactor = inputRow.getHeightFactor();
        if (!BeeUtils.isPositive(heightFactor)) {
          heightFactor = inputSheet.getRowHeightFactor();
        }
        if (BeeUtils.isPositive(heightFactor)) {
          row.setHeightInPoints(defaultRowHeightInPoints * heightFactor.floatValue());
        }

        if (inputRow.getStyleRef() != null) {
          CellStyle rowStyle = styles.get(inputRow.getStyleRef());

          if (rowStyle == null) {
            logger.warning("row", inputRow.getIndex(), "invalid style index",
                inputRow.getStyleRef());
          } else {
            row.setRowStyle(rowStyle);
          }
        }

        for (XCell inputCell : inputRow.getCells()) {
          Cell cell = row.createCell(inputCell.getIndex());

          Value value = inputCell.getValue();
          boolean wrap = false;

          if (value != null && !value.isNull()) {
            ValueType type = value.getType();

            switch (type) {
              case BOOLEAN:
                cell.setCellValue(value.getBoolean());
                break;

              case DATE:
                cell.setCellValue(value.getDate().getJava());
                break;

              case DATE_TIME:
                cell.setCellValue(value.getDateTime().getJava());
                break;

              case DECIMAL:
              case INTEGER:
              case LONG:
              case NUMBER:
                cell.setCellValue(value.getDouble());
                break;

              case TEXT:
              case TIME_OF_DAY:
                cell.setCellValue(value.getString());
                wrap = BeeUtils.contains(value.getString(), BeeConst.CHAR_EOL);
                break;

              default:
                logger.warning("cell type", type, "not supported");
            }

          } else if (!BeeUtils.isEmpty(inputCell.getFormula())) {
            cell.setCellFormula(inputCell.getFormula());
          }

          if (inputCell.getStyleRef() != null) {
            CellStyle cellStyle = styles.get(inputCell.getStyleRef());

            if (cellStyle == null) {
              logger.warning("row", inputRow.getIndex(), "cell", inputCell.getIndex(),
                  "invalid style index", inputCell.getStyleRef());
            } else {
              if (wrap && !cellStyle.getWrapText()) {
                cellStyle.setWrapText(true);
              }
              cell.setCellStyle(cellStyle);
            }

          } else if (wrap) {
            cell.setCellStyle(wrapStyle);
          }

          if (inputCell.getPictureRef() != null) {
            Integer pictureIndex = pictures.get(inputCell.getPictureRef());

            if (pictureIndex == null) {
              logger.warning("row", inputRow.getIndex(), "cell", inputCell.getIndex(),
                  "invalid picture index", inputCell.getPictureRef());

            } else {
              if (creationHelper == null) {
                creationHelper = wb.getCreationHelper();
              }
              if (drawing == null) {
                drawing = sheet.createDrawingPatriarch();
              }

              int pr = inputRow.getIndex();
              int pc = inputCell.getIndex();

              Picture picture = createPicture(creationHelper, drawing, pictureIndex, pr, pc);

              if (inputCell.getPictureLayout() != null) {
                switch (inputCell.getPictureLayout()) {
                  case REPAEAT:
                    if (inputCell.getRowSpan() > 1 || inputCell.getColSpan() > 1) {
                      int rowSpan = Math.max(inputCell.getRowSpan(), 1);
                      int colSpan = Math.max(inputCell.getColSpan(), 1);

                      for (int pi = 0; pi < rowSpan; pi++) {
                        for (int pj = 0; pj < colSpan; pj++) {
                          if (pi > 0 || pj > 0) {
                            createPicture(creationHelper, drawing, pictureIndex, pr + pi, pc + pj);
                          }
                        }
                      }
                    }
                    break;

                  case RESIZE:
                    ClientAnchor preferred = picture.getPreferredSize();
                    int scale = Math.max(preferred.getRow2() - preferred.getRow1(),
                        preferred.getCol2() - preferred.getCol1());

                    if (scale > 0) {
                      picture.resize(1d / (scale + 0.5));
                    }
                    break;
                }
              }
            }
          }

          if (inputCell.getRowSpan() > 1 || inputCell.getColSpan() > 1) {
            int firstRow = row.getRowNum();
            int lastRow = row.getRowNum() + Math.max(inputCell.getRowSpan() - 1, 0);
            int firstCol = cell.getColumnIndex();
            int lastCol = cell.getColumnIndex() + Math.max(inputCell.getColSpan() - 1, 0);

            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
          }
        }
      }

      if (!inputSheet.getAutoSize().isEmpty()) {
        for (int column : inputSheet.getAutoSize()) {
          sheet.autoSizeColumn(column, true);
        }
      }
    }

    logger.info("create workbook in", TimeUtils.elapsedMillis(start), "ms");
    return wb;
  }

  private static String getExportedRowsInfo() {
    return BeeUtils.joinWords("exported rows:", exportedRows.keySet().size(), "keys",
        exportedRows.size(), "values");
  }

  private static int getPictureFormat(XPicture input) {
    switch (input.getType()) {
      case DIB:
        return Workbook.PICTURE_TYPE_DIB;
      case EMF:
        return Workbook.PICTURE_TYPE_EMF;
      case JPEG:
        return Workbook.PICTURE_TYPE_JPEG;
      case PICT:
        return Workbook.PICTURE_TYPE_PICT;
      case PNG:
        return Workbook.PICTURE_TYPE_PNG;
      case WMF:
        return Workbook.PICTURE_TYPE_WMF;
    }

    Assert.untouchable();
    return BeeConst.UNDEF;
  }

  private static Map<Integer, Integer> addPictures(List<XPicture> inputPictures, Workbook wb) {
    Map<Integer, Integer> result = new HashMap<>();
    if (BeeUtils.isEmpty(inputPictures)) {
      return result;
    }

    for (int i = 0; i < inputPictures.size(); i++) {
      XPicture inputPicture = inputPictures.get(i);

      byte[] bytes;
      if (inputPicture.isDataUri()) {
        bytes = Codec.fromBase64(inputPicture.getSrc());
      } else {
        bytes = FileUtils.getBytes(new File(Config.WAR_DIR, inputPicture.getSrc()));
      }

      if (bytes != null) {
        int pictureIndex = wb.addPicture(bytes, getPictureFormat(inputPicture));
        result.put(i, pictureIndex);
      }
    }
    return result;
  }

  private static synchronized int pushRows(String id, String serialized, String message) {
    long start = System.currentTimeMillis();

    int count = 0;
    String[] arr = Codec.beeDeserializeCollection(serialized);

    if (arr != null) {
      for (String s : arr) {
        exportedRows.put(id, XRow.restore(s));
        count++;
      }

      logger.info("export", id, message, "push", count, "rows in", TimeUtils.elapsedMillis(start),
          "ms,", getExportedRowsInfo());
    }

    return count;
  }

  private static synchronized int maybePopRows(String id, XWorkbook workbook) {
    long start = System.currentTimeMillis();
    int count = 0;

    if (exportedRows.containsKey(id) && workbook.getSheetCount() == 1) {
      List<XRow> rows = new ArrayList<>(exportedRows.get(id));
      rows.sort(null);

      workbook.getSheets().get(0).addRows(rows);
      exportedRows.removeAll(id);

      count = rows.size();
      logger.info("export", id, "pop", count, "rows in", TimeUtils.elapsedMillis(start), "ms,",
          getExportedRowsInfo());
    }

    return count;
  }

  private static synchronized String maybeClearRows(String id) {
    String message;

    if (BeeUtils.isEmpty(id)) {
      if (!exportedRows.isEmpty()) {
        exportedRows.clear();
        message = "exported rows cleared";
      } else {
        message = "exported rows already empty";
      }

    } else if (exportedRows.containsKey(id)) {
      exportedRows.removeAll(id);
      message = BeeUtils.joinWords("export", id, "removed from row cache,", getExportedRowsInfo());

    } else {
      message = BeeUtils.joinWords("exported rows do not contain", id, getExportedRowsInfo());
    }

    return message;
  }

  @EJB
  FileStorageBean fs;

  @Override
  protected void doService(HttpServletRequest req, HttpServletResponse resp) {
    long start = System.currentTimeMillis();

    RequestInfo reqInfo = new RequestInfo(req, false);

    String svc = BeeUtils.trim(reqInfo.getService());
    String id = reqInfo.getParameter(Service.VAR_ID);

    logger.info(">", svc, id, reqInfo.getContentLen());

    String result;

    switch (svc) {
      case Service.EXPORT_WORKBOOK:
        String fileName = reqInfo.getParameter(Service.VAR_FILE_NAME);
        if (BeeUtils.isEmpty(fileName)) {
          HttpUtils.badRequest(resp, svc, "parameter not found:", Service.VAR_FILE_NAME);
          return;
        }

        String data = reqInfo.getContent();
        if (BeeUtils.isEmpty(data)) {
          HttpUtils.badRequest(resp, svc, "content not available");
          return;
        }

        XWorkbook input = XWorkbook.restore(data);
        if (input == null || input.isEmpty()) {
          HttpUtils.badRequest(resp, svc, "workbook is empty");
          return;
        }

        maybePopRows(id, input);
        Workbook workbook = createWorkbook(input);

        FileInfo fileInfo = storeWorkbook(workbook, fileName);
        closeWorkbook(workbook);

        if (Objects.nonNull(fileInfo)) {
          result = fileInfo.serialize();
        } else {
          String message = BeeUtils.joinWords(svc, fileName, "store workbook failed");
          HttpUtils.sendError(resp, HttpServletResponse.SC_EXPECTATION_FAILED, message);
          return;
        }
        break;

      case Service.EXPORT_ROWS:
        if (BeeUtils.isEmpty(id)) {
          HttpUtils.badRequest(resp, svc, "parameter not found:", Service.VAR_ID);
          return;
        }

        String content = reqInfo.getContent();
        if (BeeUtils.isEmpty(content)) {
          HttpUtils.badRequest(resp, svc, "content not available");
          return;
        }

        String from = reqInfo.getParameter(Service.VAR_FROM);
        int count = pushRows(id, content, from);

        result = BeeUtils.toString(count);
        break;

      case Service.EXPORT_CLEAR:
        result = maybeClearRows(id);
        break;

      default:
        HttpUtils.badRequest(resp, NameUtils.getName(this), "service not recognized", svc);
        return;
    }

    HttpUtils.setDefaultHeaders(resp, ContentType.TEXT);

    logger.info("<", svc, id, result, TimeUtils.elapsedSeconds(start));

    try {
      PrintWriter output = resp.getWriter();
      output.print(BeeUtils.nvl(result, BeeConst.STRING_EMPTY));
      output.flush();

    } catch (IOException ex) {
      logger.error(ex);
    }
  }

  private FileInfo storeWorkbook(Workbook workbook, String fileName) {
    long start = System.currentTimeMillis();
    FileInfo fileInfo = null;

    try {
      File tmp = File.createTempFile("bee_", null);
      tmp.deleteOnExit();

      FileOutputStream fos = new FileOutputStream(tmp);
      workbook.write(fos);
      fos.close();

      FileInputStream fis = new FileInputStream(tmp);
      String name = FileNameUtils.defaultExtension(fileName, XWorkbook.FILE_EXTENSION);

      fileInfo = fs.storeFile(fis, name, null);
      tmp.delete();

      logger.info("store workbook", name, "in", TimeUtils.elapsedMillis(start), "ms");

    } catch (IOException ex) {
      logger.error(ex);
    }

    return fileInfo;
  }
}
