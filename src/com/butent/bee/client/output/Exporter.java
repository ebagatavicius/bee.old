package com.butent.bee.client.output;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Timer;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.ColumnInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.export.XWorkbook;
import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Exporter {

  public abstract static class FileNameCallback extends StringCallback {
    @Override
    public boolean validate(String value) {
      return validateFileName(value);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Exporter.class);

  private static final String EXPORT_URL = "export";

  private static final int SUBMIT_TIMEOUT = 60_000;

  private static final int DEFAULT_STEP_SIZE = 100;

  private static final double PARENT_LABEL_HEIGHT_FACTOR = 1.1;
  private static final double CAPTION_HEIGHT_FACTOR = 1.5;
  private static final double FILTER_LABEL_HEIGHT_FACTOR = 1.0;
  private static final double HEADER_HEIGHT_FACTOR = 1.2;
  private static final double FOOTER_HEIGHT_FACTOR = 1.1;

  private static final int MAX_NUMBER_OF_ROWS_FOR_AUTOSIZE = 1_000;

  public static void addCaption(XSheet sheet, String caption, int rowIndex, int columnCount) {
    Assert.notNull(sheet);
    Assert.notEmpty(caption);

    XFont font = XFont.bold();
    font.setFactor(CAPTION_HEIGHT_FACTOR);

    XStyle style = XStyle.center();
    style.setFontRef(sheet.registerFont(font));

    XCell cell = new XCell(0, caption, sheet.registerStyle(style));
    if (columnCount > 1) {
      cell.setColSpan(columnCount);
    }

    XRow row = new XRow(rowIndex);
    row.setHeightFactor(CAPTION_HEIGHT_FACTOR);

    row.add(cell);
    sheet.add(row);
  }

  public static void addFilterLabel(XSheet sheet, String label, int rowIndex, int columnCount) {
    XStyle style = XStyle.center();

    XCell cell = new XCell(0, label, sheet.registerStyle(style));
    if (columnCount > 1) {
      cell.setColSpan(columnCount);
    }

    XRow row = new XRow(rowIndex);
    row.setHeightFactor(FILTER_LABEL_HEIGHT_FACTOR);

    row.add(cell);
    sheet.add(row);
  }

  public static void confirm(String fileName, FileNameCallback callback) {
    Assert.notNull(callback);

    int width = BeeUtils.resize(BeeUtils.trim(fileName).length(), 20, 100, 300, 600);

    Global.inputString(Localized.getConstants().exportToMsExcel(),
        Localized.getConstants().fileName(), callback, null, fileName, 200, null,
        width, CssUnit.PX, BeeConst.UNDEF,
        Action.EXPORT.getCaption(), Action.CANCEL.getCaption(), null);
  }

  public static XCell createCell(String text, ValueType type, int cellIndex, Integer styleRef) {
    if (BeeUtils.isEmpty(text)) {
      return null;

    } else {
      Value value = null;

      if (ValueType.isNumeric(type)) {
        Double d = BeeUtils.toDoubleOrNull(BeeUtils.removeWhiteSpace(text));
        if (BeeUtils.isDouble(d)) {
          value = new NumberValue(d);
        }
      }

      if (value == null) {
        value = new TextValue(text);
      }

      XCell xc = new XCell(cellIndex, value);
      if (styleRef != null) {
        xc.setStyleRef(styleRef);
      }

      return xc;
    }
  }

  public static XRow createFooterRow(int rowIndex) {
    XRow row = new XRow(rowIndex);
    row.setHeightFactor(FOOTER_HEIGHT_FACTOR);
    return row;
  }

  public static XRow createHeaderRow(int rowIndex) {
    XRow row = new XRow(rowIndex);
    row.setHeightFactor(HEADER_HEIGHT_FACTOR);
    return row;
  }

  public static void export(GridPresenter presenter, String caption, final String fileName) {
    Assert.notNull(presenter);
    Assert.isTrue(validateFileName(fileName));

    CellGrid grid = presenter.getGridView().getGrid();

    final int rowCount = grid.getRowCount();
    if (rowCount <= 0) {
      String message = "grid is empty";
      logger.warning(message);
      BeeKeeper.getScreen().notifyWarning(message);
      return;
    }

    final List<ColumnInfo> columns = new ArrayList<>();

    List<ColumnInfo> gridColumns = grid.getColumns();
    for (ColumnInfo column : gridColumns) {
      if (column.isExportable()) {
        columns.add(column);
      }
    }

    int columnCount = columns.size();
    if (columnCount <= 0) {
      String message = "No exportable columns found";
      logger.warning(message);
      BeeKeeper.getScreen().notifyWarning(message);
      return;
    }

    final XSheet sheet = new XSheet();

    Double rowHeightFactor = getRowHeightFactor(grid);
    if (rowHeightFactor != null) {
      sheet.setRowHeightFactor(rowHeightFactor);
    }

    for (int i = 0; i < columnCount; i++) {
      Double widthFactor = columns.get(i).getExportWidthFactor();
      if (BeeUtils.isPositive(widthFactor)) {
        sheet.setColumnWidthFactor(i, widthFactor);
      }
    }

    int rowIndex = 0;

    XRow row;
    XCell cell;
    XFont font;
    XStyle style;

    Integer styleRef;

    List<String> parentLabels = presenter.getParentLabels();
    if (!BeeUtils.isEmpty(parentLabels)) {
      font = new XFont();
      font.setFactor(PARENT_LABEL_HEIGHT_FACTOR);

      style = new XStyle();
      style.setFontRef(sheet.registerFont(font));
      styleRef = sheet.registerStyle(style);

      for (String label : parentLabels) {
        cell = new XCell(0, label, styleRef);
        if (columnCount > 1) {
          cell.setColSpan(columnCount);
        }

        row = new XRow(rowIndex++);
        row.setHeightFactor(PARENT_LABEL_HEIGHT_FACTOR);

        row.add(cell);
        sheet.add(row);
      }
    }

    if (!BeeUtils.isEmpty(caption)) {
      if (rowHeightFactor != null) {
        row = new XRow(rowIndex);
        row.setHeightFactor(BeeConst.DOUBLE_ONE);
        sheet.add(row);
      }
      rowIndex++;

      addCaption(sheet, caption, rowIndex++, columnCount);
    }

    Filter filter = presenter.getDataProvider().getUserFilter();
    if (filter != null) {
      String label = presenter.getFilterLabel();
      if (BeeUtils.isEmpty(label)) {
        label = filter.toString();
      }

      addFilterLabel(sheet, label, rowIndex++, columnCount);
    }

    if (rowIndex > 0) {
      if (rowHeightFactor != null) {
        row = new XRow(rowIndex);
        row.setHeightFactor(BeeConst.DOUBLE_ONE);
        sheet.add(row);
      }
      rowIndex++;
    }

    if (grid.hasHeaders()) {
      row = createHeaderRow(rowIndex++);

      style = XStyle.center();
      style.setVerticalAlign(VerticalAlign.MIDDLE);
      style.setColor(Colors.LIGHTGRAY);
      style.setFontRef(sheet.registerFont(XFont.bold()));

      styleRef = sheet.registerStyle(style);

      for (int i = 0; i < columnCount; i++) {
        ColumnHeader header = columns.get(i).getHeader();
        if (header != null) {
          row.add(new XCell(i, header.getExportLabel(), styleRef));
        }
      }
      sheet.add(row);
    }

    final Map<Integer, Integer> bodyStyles = new HashMap<>();
    for (int i = 0; i < columnCount; i++) {
      styleRef = columns.get(i).getColumn().initExport(sheet);

      if (styleRef != null) {
        bodyStyles.put(i, styleRef);
      }
    }

    final CellContext context = new CellContext(grid);

    int dataSize = grid.getDataSize();
    String viewName = presenter.getViewName();

    if (dataSize >= rowCount || BeeUtils.isEmpty(viewName)) {
      for (int i = 0; i < dataSize; i++) {
        IsRow dataRow = BeeUtils.getQuietly(grid.getRowData(), i);
        if (dataRow != null) {
          exportRow(context, dataRow, columns, rowIndex++, sheet, bodyStyles);
        }
      }

      if (grid.hasFooters()) {
        row = createFooterRow(rowIndex++);

        style = XStyle.background(Colors.LIGHTGRAY);
        style.setFontRef(sheet.registerFont(XFont.bold()));

        styleRef = sheet.registerStyle(style);

        String footerValue;
        TextAlign footerTextAlign;
        Integer footerStyleRef;

        for (int i = 0; i < columnCount; i++) {
          ColumnFooter footer = columns.get(i).getFooter();

          if (footer == null) {
            footerValue = null;
            footerTextAlign = null;
          } else {
            footerValue = footer.reduce(grid.getRowData());
            footerTextAlign = BeeUtils.isEmpty(footerValue) ? null : footer.getTextAlign();
          }

          if (footerTextAlign == null) {
            footerStyleRef = styleRef;
          } else {
            XStyle footerStyle = style.copy();
            footerStyle.setTextAlign(footerTextAlign);

            footerStyleRef = sheet.registerStyle(footerStyle);
          }

          row.add(new XCell(i, Strings.nullToEmpty(footerValue), footerStyleRef));
        }

        sheet.add(row);
      }

      autosizeNoPictures(sheet, columns.size());
      export(sheet, fileName);

    } else {
      int stepSize = Math.min(rowCount, DEFAULT_STEP_SIZE);
      int numberOfSteps = rowCount / stepSize;
      if (rowCount % stepSize > 0) {
        numberOfSteps++;
      }

      final Holder<Integer> chunks = Holder.of(numberOfSteps);

      final int firstRowIndex = rowIndex;

      filter = presenter.getDataProvider().getFilter();
      Order order = presenter.getDataProvider().getOrder();

      for (int offset = 0; offset < rowCount; offset += stepSize) {
        Queries.getRowSet(viewName, null, filter, order, offset, stepSize, CachingPolicy.NONE,
            new Queries.RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet result) {
                String respOffset = result.getTableProperty(Service.VAR_VIEW_OFFSET);
                Integer remaining = chunks.get();
                logger.debug(remaining, respOffset);

                if (BeeUtils.isDigit(respOffset)) {
                  int index = BeeUtils.toInt(respOffset) + firstRowIndex;
                  for (IsRow dataRow : result) {
                    exportRow(context, dataRow, columns, index++, sheet, bodyStyles);
                  }
                }

                chunks.set(remaining - 1);
                if (remaining <= 1) {
                  if (rowCount <= MAX_NUMBER_OF_ROWS_FOR_AUTOSIZE) {
                    autosizeNoPictures(sheet, columns.size());
                  }
                  export(sheet, fileName);
                }
              }
            });
      }
    }
  }

  public static void export(XSheet sheet, String fileName) {
    if (sheet == null || sheet.isEmpty()) {
      logger.severe(NameUtils.getClassName(Exporter.class), "sheet is empty");

    } else {
      XWorkbook wb = new XWorkbook();
      wb.add(sheet);
      export(wb, fileName);
    }
  }

  public static void export(XWorkbook wb, String fileName) {
    if (wb == null || wb.isEmpty()) {
      logger.severe(NameUtils.getClassName(Exporter.class), "workbook is empty");

    } else if (BeeUtils.isEmpty(fileName)) {
      logger.severe(NameUtils.getClassName(Exporter.class), "file name not specified");

    } else if (!validateFileName(fileName)) {
      logger.severe(NameUtils.getClassName(Exporter.class), "invalid file name", fileName);

    } else {
      String frameName = NameUtils.createUniqueName("frame");

      final IFrameElement frame = Document.get().createIFrameElement();

      frame.setName(frameName);
      frame.setSrc(Keywords.URL_ABOUT_BLANK);

      BodyPanel.conceal(frame);

      final FormElement form = Document.get().createFormElement();

      form.setAcceptCharset(BeeConst.CHARSET_UTF8);
      form.setMethod(Keywords.METHOD_POST);
      form.setAction(GWT.getHostPageBaseURL() + EXPORT_URL);
      form.setTarget(frameName);

      form.appendChild(createFormParameter(Service.RPC_VAR_SVC, Service.EXPORT_WORKBOOK));
      form.appendChild(createFormParameter(Service.VAR_FILE_NAME, sanitizeFileName(fileName)));
      form.appendChild(createFormParameter(Service.VAR_DATA, wb.serialize()));

      BodyPanel.conceal(form);
      form.submit();

      Timer timer = new Timer() {
        @Override
        public void run() {
          form.removeFromParent();
          frame.removeFromParent();
        }
      };

      timer.schedule(SUBMIT_TIMEOUT);
    }
  }

  public static void maybeExport(final XSheet sheet, String fileName) {
    FileNameCallback callback = new FileNameCallback() {
      @Override
      public void onSuccess(String value) {
        export(sheet, BeeUtils.trim(value));
      }
    };

    confirm(fileName, callback);
  }

  public static boolean validateFileName(String input) {
    return !BeeUtils.isEmpty(sanitizeFileName(input));
  }

  private static void autosizeNoPictures(XSheet sheet, int columnCount) {
    for (int i = 0; i < columnCount; i++) {
      if (!sheet.getColumnWidthFactors().containsKey(i) && !sheet.hasPictures(i)) {
        sheet.autoSizeColumn(i);
      }
    }
  }

  private static InputElement createFormParameter(String name, String value) {
    InputElement inputElement = Document.get().createHiddenInputElement();

    inputElement.setName(name);
    inputElement.setValue(value);

    return inputElement;
  }

  private static void exportRow(CellContext context, IsRow dataRow, List<ColumnInfo> columns,
      int rowIndex, XSheet sheet, Map<Integer, Integer> styles) {

    context.setRow(dataRow);

    XRow row = new XRow(rowIndex);

    int columnCount = columns.size();
    for (int j = 0; j < columnCount; j++) {
      context.setColumnIndex(j);

      ColumnInfo columnInfo = columns.get(j);

      Integer styleRef = null;
      if (columnInfo.getDynStyles() != null) {
        styleRef = columnInfo.getDynStyles().getExportStyleRef(dataRow, sheet);
      }
      if (styleRef == null) {
        styleRef = styles.get(j);
      }

      XCell cell = columnInfo.getColumn().export(context, styleRef, sheet);
      if (cell != null) {
        row.add(cell);
      }
    }

    sheet.add(row);
  }

  private static Double getRowHeightFactor(CellGrid grid) {
    int height = grid.getBodyCellHeight();
    if (height >= 30) {
      return height / 20d;
    } else {
      return null;
    }
  }

  private static String sanitizeFileName(String input) {
    return FileNameUtils.sanitize(input, BeeConst.STRING_POINT);
  }

  private Exporter() {
  }
}
