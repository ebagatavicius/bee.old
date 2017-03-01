package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.output.ReportParameters;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;

public class AssessmentQuantityReport extends ReportInterceptor {

  private static BeeLogger logger = LogUtils.getLogger(AssessmentQuantityReport.class);

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_END_DATE = "EndDate";

  private static final String NAME_DEPARTMENTS = "Departments";
  private static final String NAME_MANAGERS = "Managers";

  private static final List<String> NAME_GROUP_BY =
      Lists.newArrayList("Group0", "Group1", "Group2");

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-aqr-";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_HEADER = STYLE_PREFIX + "header";
  private static final String STYLE_HEADER_1 = STYLE_HEADER + "-1";
  private static final String STYLE_HEADER_2 = STYLE_HEADER + "-2";

  private static final String STYLE_YEAR = STYLE_PREFIX + "year";
  private static final String STYLE_MONTH = STYLE_PREFIX + "month";
  private static final String STYLE_DEPARTMENT = STYLE_PREFIX + "department";
  private static final String STYLE_MANAGER = STYLE_PREFIX + "manager";

  private static final String STYLE_QUANTITY = STYLE_PREFIX + "quantity";
  private static final String STYLE_PERCENT = STYLE_PREFIX + "percent";

  private static final String STYLE_RECEIVED = STYLE_PREFIX + "received";
  private static final String STYLE_ANSWERED = STYLE_PREFIX + "answered";
  private static final String STYLE_LOST = STYLE_PREFIX + "lost";
  private static final String STYLE_APPROVED = STYLE_PREFIX + "approved";
  private static final String STYLE_SECONDARY = STYLE_PREFIX + "secondary";

  private static final String STYLE_DETAILS = STYLE_PREFIX + "details";
  private static final String STYLE_SUMMARY = STYLE_PREFIX + "summary";

  private static final String DRILL_DOWN_GRID_NAME = "AssessmentReportDrillDown";

  private final XSheet sheet = new XSheet();

  public AssessmentQuantityReport() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new AssessmentQuantityReport();
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      Widget widget = form.getWidgetByName(NAME_START_DATE);
      DateTime dateTime = parameters.getDateTime(NAME_START_DATE);
      if (widget instanceof InputDateTime && dateTime != null) {
        ((InputDateTime) widget).setDateTime(dateTime);
      }

      widget = form.getWidgetByName(NAME_END_DATE);
      dateTime = parameters.getDateTime(NAME_END_DATE);
      if (widget instanceof InputDateTime && dateTime != null) {
        ((InputDateTime) widget).setDateTime(dateTime);
      }

      widget = form.getWidgetByName(NAME_DEPARTMENTS);
      String idList = parameters.get(NAME_DEPARTMENTS);
      if (widget instanceof MultiSelector && !BeeUtils.isEmpty(idList)) {
        ((MultiSelector) widget).setIds(idList);
      }

      widget = form.getWidgetByName(NAME_MANAGERS);
      idList = parameters.get(NAME_MANAGERS);
      if (widget instanceof MultiSelector && !BeeUtils.isEmpty(idList)) {
        ((MultiSelector) widget).setIds(idList);
      }

      for (String groupName : NAME_GROUP_BY) {
        widget = form.getWidgetByName(groupName);
        Integer index = parameters.getInteger(groupName);
        if (widget instanceof ListBox && BeeUtils.isPositive(index)) {
          ((ListBox) widget).setSelectedIndex(index);
        }
      }
    }

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(NAME_START_DATE, NAME_END_DATE);
    storeEditorValues(NAME_DEPARTMENTS, NAME_MANAGERS);

    for (String groupName : NAME_GROUP_BY) {
      Integer index = getSelectedIndex(groupName);
      if (!BeeUtils.isPositive(index)) {
        index = null;
      }

      storeValue(groupName, index);
    }
  }

  @Override
  protected void clearFilter() {
    clearEditor(NAME_START_DATE);
    clearEditor(NAME_END_DATE);

    clearEditor(NAME_DEPARTMENTS);
    clearEditor(NAME_MANAGERS);
  }

  @Override
  protected void doReport() {
    DateTime start = getDateTime(NAME_START_DATE);
    DateTime end = getDateTime(NAME_END_DATE);

    if (!checkRange(start, end)) {
      return;
    }

    ParameterList params = TransportHandler.createArgs(SVC_GET_ASSESSMENT_QUANTITY_REPORT);
    final List<String> headers = Lists.newArrayList(getReportCaption());

    if (start != null) {
      params.addDataItem(Service.VAR_FROM, start.getTime());
    }
    if (end != null) {
      params.addDataItem(Service.VAR_TO, end.getTime());
    }
    if (start != null || end != null) {
      headers.add(Format.renderPeriod(start, end));
    }

    String label;

    String departments = getEditorValue(NAME_DEPARTMENTS);
    if (!BeeUtils.isEmpty(departments)) {
      params.addDataItem(AR_DEPARTMENT, departments);

      if (DataUtils.parseIdSet(departments).size() > 1) {
        label = Localized.dictionary().departments();
      } else {
        label = Localized.dictionary().department();
      }
      headers.add(BeeUtils.joinWords(label, getFilterLabel(NAME_DEPARTMENTS)));
    }

    String managers = getEditorValue(NAME_MANAGERS);
    if (!BeeUtils.isEmpty(managers)) {
      params.addDataItem(AR_MANAGER, managers);

      if (DataUtils.parseIdSet(managers).size() > 1) {
        label = Localized.dictionary().managers();
      } else {
        label = Localized.dictionary().manager();
      }
      headers.add(BeeUtils.joinWords(label, getFilterLabel(NAME_MANAGERS)));
    }

    List<String> groupBy = new ArrayList<>();
    for (String groupName : NAME_GROUP_BY) {
      Integer index = getSelectedIndex(groupName);

      if (BeeUtils.isPositive(index)) {
        String group;

        switch (index) {
          case 1:
            group = BeeConst.MONTH;
            break;
          case 2:
            group = AR_DEPARTMENT;
            break;
          case 3:
            group = AR_MANAGER;
            break;
          default:
            group = null;
        }

        if (group != null && !groupBy.contains(group)) {
          groupBy.add(group);
        }
      }
    }

    if (!groupBy.isEmpty()) {
      params.addDataItem(Service.VAR_GROUP_BY, NameUtils.join(groupBy));
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasMessages()) {
          response.notify(getFormView());
        }

        if (response.hasResponse(SimpleRowSet.class)) {
          renderData(SimpleRowSet.restore(response.getResponseAsString()));

          sheet.addHeaders(headers);
          sheet.autoSizeAll();

        } else {
          getFormView().notifyWarning(Localized.dictionary().nothingFound());
        }
      }
    });
  }

  @Override
  protected void export() {
    if (!sheet.isEmpty()) {
      Exporter.maybeExport(sheet, getReportCaption());
    }
  }

  @Override
  protected String getBookmarkLabel() {
    List<String> labels = Lists.newArrayList(getReportCaption(),
        Format.renderPeriod(getDateTime(NAME_START_DATE), getDateTime(NAME_END_DATE)),
        getFilterLabel(NAME_DEPARTMENTS), getFilterLabel(NAME_MANAGERS));

    for (String groupName : NAME_GROUP_BY) {
      if (BeeUtils.isPositive(getSelectedIndex(groupName))) {
        String value = getEditorValue(groupName);
        if (!labels.contains(value)) {
          labels.add(value);
        }
      }
    }

    return BeeUtils.joinWords(labels);
  }

  @Override
  protected Report getReport() {
    return Report.ASSESSMENT_QUANTITY;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, NAME_START_DATE, NAME_END_DATE);
    addEditorValues(parameters, NAME_DEPARTMENTS, NAME_MANAGERS);

    for (String groupName : NAME_GROUP_BY) {
      Integer index = getSelectedIndex(groupName);
      if (BeeUtils.isPositive(index)) {
        parameters.add(groupName, index);
      }
    }

    return parameters;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    DateTime start = parameters.getDateTime(NAME_START_DATE);
    DateTime end = parameters.getDateTime(NAME_END_DATE);

    return checkRange(start, end);
  }

  private void renderData(final SimpleRowSet data) {
    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }

    sheet.clear();

    if (!container.isEmpty()) {
      container.clear();
    }

    final HtmlTable table = new HtmlTable(STYLE_TABLE);

    int row = 0;
    int c1 = 0;
    int c2 = 0;
    int col = 0;

    int colYear = BeeConst.UNDEF;
    int colMonth = BeeConst.UNDEF;
    int colDepartment = BeeConst.UNDEF;
    int colManager = BeeConst.UNDEF;
    int colReceived = BeeConst.UNDEF;
    int colAnswered = BeeConst.UNDEF;
    int colLost = BeeConst.UNDEF;
    int colApproved = BeeConst.UNDEF;
    int colSecondary = BeeConst.UNDEF;

    XRow xr1 = new XRow(row);
    XRow xr2 = new XRow(row + 1);

    Integer boldRef = sheet.registerFont(XFont.bold());

    XStyle xs = XStyle.center();
    xs.setVerticalAlign(VerticalAlign.MIDDLE);
    xs.setColor(Colors.LIGHTGRAY);
    xs.setFontRef(boldRef);

    int styleRef = sheet.registerStyle(xs);

    XCell xc;
    String text;

    for (int j = 0; j < data.getNumberOfColumns(); j++) {
      String colName = data.getColumnName(j);

      switch (colName) {
        case BeeConst.YEAR:
          colYear = col;

          text = Localized.dictionary().year();
          table.setText(row, c1, text, STYLE_HEADER);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case BeeConst.MONTH:
          colMonth = col;

          text = Localized.dictionary().month();
          table.setText(row, c1, text, STYLE_HEADER);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case AdministrationConstants.COL_DEPARTMENT:
          colDepartment = col;

          text = Localized.dictionary().department();
          table.setText(row, c1, text, STYLE_HEADER);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case AdministrationConstants.COL_DEPARTMENT_NAME:
          break;

        case ClassifierConstants.COL_COMPANY_PERSON:
          colManager = col;

          text = Localized.dictionary().manager();
          table.setText(row, c1, text, STYLE_HEADER);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case ClassifierConstants.COL_FIRST_NAME:
        case ClassifierConstants.COL_LAST_NAME:
          break;

        case AR_RECEIVED:
          colReceived = col;

          text = Localized.dictionary().trAssessmentReportReceived();
          table.setText(row, c1, text, STYLE_HEADER);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case AR_ANSWERED:
          colAnswered = col;

          text = Localized.dictionary().trAssessmentReportAnswered();
          table.setText(row, c1, text, STYLE_HEADER_1);
          table.getCellFormatter().setColSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setColSpan(2);
          xr1.add(xc);

          text = Localized.dictionary().trAssessmentReportQuantity();
          table.setText(row + 1, c2, text, STYLE_HEADER_2);
          xr2.add(new XCell(col, text, styleRef));

          text = Localized.dictionary().trAssessmentReportPercent();
          table.setText(row + 1, c2 + 1, text, STYLE_HEADER_2);
          xr2.add(new XCell(col + 1, text, styleRef));

          c1++;
          c2 += 2;
          col += 2;
          break;

        case AR_LOST:
          colLost = col;

          text = Localized.dictionary().trAssessmentReportLost();
          table.setText(row, c1, text, STYLE_HEADER_1);
          table.getCellFormatter().setColSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setColSpan(2);
          xr1.add(xc);

          text = Localized.dictionary().trAssessmentReportQuantity();
          table.setText(row + 1, c2, text, STYLE_HEADER_2);
          xr2.add(new XCell(col, text, styleRef));

          text = Localized.dictionary().trAssessmentReportPercent();
          table.setText(row + 1, c2 + 1, text, STYLE_HEADER_2);
          xr2.add(new XCell(col + 1, text, styleRef));

          c1++;
          c2 += 2;
          col += 2;
          break;

        case AR_APPROVED:
          colApproved = col;

          text = Localized.dictionary().trAssessmentReportApproved();
          table.setText(row, c1, text, STYLE_HEADER_1);
          table.getCellFormatter().setColSpan(row, c1, 3);

          xc = new XCell(col, text, styleRef);
          xc.setColSpan(3);
          xr1.add(xc);

          text = Localized.dictionary().trAssessmentReportQuantity();
          table.setText(row + 1, c2, text, STYLE_HEADER_2);
          xr2.add(new XCell(col, text, styleRef));

          text = Localized.dictionary().trAssessmentReportApprovedToReceived();
          table.setText(row + 1, c2 + 1, text, STYLE_HEADER_2);
          xr2.add(new XCell(col + 1, text, styleRef));

          text = Localized.dictionary().trAssessmentReportApprovedToAnswered();
          table.setText(row + 1, c2 + 2, text, STYLE_HEADER_2);
          xr2.add(new XCell(col + 2, text, styleRef));

          c1++;
          c2 += 3;
          col += 3;
          break;

        case AR_SECONDARY:
          colSecondary = col;

          text = Localized.dictionary().trAssessmentReportSecondary();
          table.setText(row, c1, text, STYLE_HEADER_1);
          table.getCellFormatter().setColSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setColSpan(2);
          xr1.add(xc);

          text = Localized.dictionary().trAssessmentReportQuantity();
          table.setText(row + 1, c2, text, STYLE_HEADER_2);
          xr2.add(new XCell(col, text, styleRef));

          text = Localized.dictionary().trAssessmentReportPercent();
          table.setText(row + 1, c2 + 1, text, STYLE_HEADER_2);
          xr2.add(new XCell(col + 1, text, styleRef));

          c1++;
          c2 += 2;
          col += 2;
          break;

        default:
          logger.warning("column not recognized", colName);
      }
    }

    sheet.add(xr1);
    sheet.add(xr2);

    int totReceived = 0;
    int totAnswered = 0;
    int totLost = 0;
    int totApproved = 0;
    int totSecondary = 0;

    row = 2;
    XRow xr;

    xs = XStyle.right();
    xs.setFormat(QUANTITY_PATTERN);
    int csQty = sheet.registerStyle(xs);

    xs = XStyle.right();
    xs.setFormat(PERCENT_PATTERN);
    int csPct = sheet.registerStyle(xs);

    Double p;

    for (int i = 0; i < data.getNumberOfRows(); i++) {
      xr = new XRow(row);

      int received = BeeUtils.unbox(data.getInt(i, AR_RECEIVED));
      int answered = BeeUtils.unbox(data.getInt(i, AR_ANSWERED));
      int lost = BeeUtils.unbox(data.getInt(i, AR_LOST));
      int approved = BeeUtils.unbox(data.getInt(i, AR_APPROVED));
      int secondary = BeeUtils.unbox(data.getInt(i, AR_SECONDARY));

      for (int j = 0; j < data.getNumberOfColumns(); j++) {
        String colName = data.getColumnName(j);

        switch (colName) {
          case BeeConst.YEAR:
            text = data.getValue(i, colName);
            table.setText(row, colYear, text, STYLE_YEAR);
            xr.add(new XCell(colYear, text));
            break;

          case BeeConst.MONTH:
            text = Format.renderMonthFullStandalone(data.getInt(i, colName));
            table.setText(row, colMonth, text, STYLE_MONTH);
            xr.add(new XCell(colMonth, text));
            break;

          case AdministrationConstants.COL_DEPARTMENT_NAME:
            text = data.getValue(i, colName);
            table.setText(row, colDepartment, text, STYLE_DEPARTMENT);
            xr.add(new XCell(colDepartment, text));
            break;

          case ClassifierConstants.COL_FIRST_NAME:
            text = BeeUtils.joinWords(data.getValue(i, colName),
                data.getValue(i, ClassifierConstants.COL_LAST_NAME));
            table.setText(row, colManager, text, STYLE_MANAGER);
            xr.add(new XCell(colManager, text));
            break;

          case AR_RECEIVED:
            table.setText(row, colReceived, renderQuantity(received),
                STYLE_RECEIVED, STYLE_QUANTITY);
            if (received > 0) {
              xr.add(new XCell(colReceived, received, csQty));
            }
            break;

          case AR_ANSWERED:
            table.setText(row, colAnswered, renderQuantity(answered),
                STYLE_ANSWERED, STYLE_QUANTITY);
            if (answered > 0) {
              xr.add(new XCell(colAnswered, answered, csQty));
            }

            p = percent(answered, received);
            table.setText(row, colAnswered + 1, renderPercent(p), STYLE_ANSWERED, STYLE_PERCENT);
            if (p != null) {
              xr.add(new XCell(colAnswered + 1, p, csPct));
            }
            break;

          case AR_LOST:
            table.setText(row, colLost, renderQuantity(lost), STYLE_LOST, STYLE_QUANTITY);
            if (lost > 0) {
              xr.add(new XCell(colLost, lost, csQty));
            }

            p = percent(lost, received);
            table.setText(row, colLost + 1, renderPercent(p), STYLE_LOST, STYLE_PERCENT);
            if (p != null) {
              xr.add(new XCell(colLost + 1, p, csPct));
            }
            break;

          case AR_APPROVED:
            table.setText(row, colApproved, renderQuantity(approved),
                STYLE_APPROVED, STYLE_QUANTITY);
            if (approved > 0) {
              xr.add(new XCell(colApproved, approved, csQty));
            }

            p = percent(approved, received);
            table.setText(row, colApproved + 1, renderPercent(p), STYLE_APPROVED, STYLE_PERCENT);
            if (p != null) {
              xr.add(new XCell(colApproved + 1, p, csPct));
            }

            p = percent(approved, answered + approved);
            table.setText(row, colApproved + 2, renderPercent(p), STYLE_APPROVED, STYLE_PERCENT);
            if (p != null) {
              xr.add(new XCell(colApproved + 2, p, csPct));
            }
            break;

          case AR_SECONDARY:
            table.setText(row, colSecondary, renderQuantity(secondary),
                STYLE_SECONDARY, STYLE_QUANTITY);
            if (approved > 0) {
              xr.add(new XCell(colSecondary, secondary, csQty));
            }

            p = percent(secondary, received);
            table.setText(row, colSecondary + 1, renderPercent(p), STYLE_SECONDARY, STYLE_PERCENT);
            if (p != null) {
              xr.add(new XCell(colSecondary + 1, p, csPct));
            }
            break;
        }
      }

      totReceived += received;
      totAnswered += answered;
      totLost += lost;
      totApproved += approved;
      totSecondary += secondary;

      table.getRowFormatter().addStyleName(row, STYLE_DETAILS);
      DomUtils.setDataIndex(table.getRow(row), i);

      sheet.add(xr);
      row++;
    }

    if (data.getNumberOfRows() > 1) {
      xr = new XRow(row);

      XFont xf = XFont.bold();
      xf.setFactor(1.2);
      int fontRef = sheet.registerFont(xf);

      xs = sheet.getStyle(csQty).copy();
      xs.setFontRef(fontRef);
      xs.setBorderTop(BorderStyle.SOLID);
      int csTotQty = sheet.registerStyle(xs);

      xs = sheet.getStyle(csPct).copy();
      xs.setFontRef(fontRef);
      xs.setBorderTop(BorderStyle.SOLID);
      int csTotPct = sheet.registerStyle(xs);

      table.setText(row, colReceived, renderQuantity(totReceived),
          STYLE_RECEIVED, STYLE_QUANTITY);
      xr.add(new XCell(colReceived, totReceived, csTotQty));

      table.setText(row, colAnswered, renderQuantity(totAnswered),
          STYLE_ANSWERED, STYLE_QUANTITY);
      xr.add(new XCell(colAnswered, totAnswered, csTotQty));

      p = percent(totAnswered, totReceived);
      table.setText(row, colAnswered + 1, renderPercent(p), STYLE_ANSWERED, STYLE_PERCENT);
      xr.add(new XCell(colAnswered + 1, p, csTotPct));

      table.setText(row, colLost, renderQuantity(totLost), STYLE_LOST, STYLE_QUANTITY);
      xr.add(new XCell(colLost, totLost, csTotQty));

      p = percent(totLost, totReceived);
      table.setText(row, colLost + 1, renderPercent(p), STYLE_LOST, STYLE_PERCENT);
      xr.add(new XCell(colLost + 1, p, csTotPct));

      table.setText(row, colApproved, renderQuantity(totApproved),
          STYLE_APPROVED, STYLE_QUANTITY);
      xr.add(new XCell(colApproved, totApproved, csTotQty));

      p = percent(totApproved, totReceived);
      table.setText(row, colApproved + 1, renderPercent(p), STYLE_APPROVED, STYLE_PERCENT);
      xr.add(new XCell(colApproved + 1, p, csTotPct));

      p = percent(totApproved, totAnswered + totApproved);
      table.setText(row, colApproved + 2, renderPercent(p), STYLE_APPROVED, STYLE_PERCENT);
      xr.add(new XCell(colApproved + 2, p, csTotPct));

      table.setText(row, colSecondary, renderQuantity(totSecondary),
          STYLE_SECONDARY, STYLE_QUANTITY);
      xr.add(new XCell(colSecondary, totSecondary, csTotQty));

      p = percent(totSecondary, totReceived);
      table.setText(row, colSecondary + 1, renderPercent(p), STYLE_SECONDARY, STYLE_PERCENT);
      xr.add(new XCell(colSecondary + 1, p, csTotPct));

      table.getRowFormatter().addStyleName(row, STYLE_SUMMARY);
      sheet.add(xr);
    }

    table.addClickHandler(event -> {
      TableCellElement cellElement =
          DomUtils.getParentCell(EventUtils.getEventTargetElement(event), true);
      TableRowElement rowElement = DomUtils.getParentRow(cellElement, false);

      if (cellElement != null
          && !BeeUtils.isEmpty(cellElement.getInnerText())
          && (cellElement.hasClassName(STYLE_QUANTITY) || cellElement.hasClassName(STYLE_PERCENT))
          && rowElement != null && rowElement.hasClassName(STYLE_DETAILS)) {

        int dataIndex = DomUtils.getDataIndexInt(rowElement);

        if (!BeeConst.isUndef(dataIndex)) {
          showDetails(data.getRow(dataIndex), cellElement);
        }
      }
    });

    container.add(table);
  }

  private void showDetails(SimpleRow dataRow, TableCellElement cellElement) {
    CompoundFilter filter = Filter.and();
    List<String> captions = new ArrayList<>();

    String[] colNames = dataRow.getColumnNames();

    DateTime start = getDateTime(NAME_START_DATE);
    DateTime end = getDateTime(NAME_END_DATE);

    if (ArrayUtils.contains(colNames, BeeConst.YEAR)
        && ArrayUtils.contains(colNames, BeeConst.MONTH)) {

      Integer year = BeeUtils.unbox(dataRow.getInt(BeeConst.YEAR));
      Integer month = BeeUtils.unbox(dataRow.getInt(BeeConst.MONTH));

      if (TimeUtils.isYear(year) && TimeUtils.isMonth(month)) {
        YearMonth ym = new YearMonth(year, month);

        if (start == null && end == null) {
          captions.add(Format.renderYearMonth(ym));
        }

        start = BeeUtils.max(start, ym.getDate().getDateTime());
        end = BeeUtils.min(end, TimeUtils.startOfNextMonth(ym).getDateTime());
      }
    }

    if (start != null) {
      filter.add(Filter.isMoreEqual(COL_ORDER_DATE, new DateTimeValue(start)));
    }
    if (end != null) {
      filter.add(Filter.isLess(COL_ORDER_DATE, new DateTimeValue(end)));
    }

    if (captions.isEmpty() && (start != null || end != null)) {
      captions.add(Format.renderPeriod(start, end));
    }

    if (ArrayUtils.contains(colNames, ClassifierConstants.COL_COMPANY_PERSON)) {
      Long companyPerson = dataRow.getLong(ClassifierConstants.COL_COMPANY_PERSON);
      if (DataUtils.isId(companyPerson)) {
        filter.add(Filter.equals(ClassifierConstants.COL_COMPANY_PERSON, companyPerson));
        captions.add(BeeUtils.joinWords(dataRow.getValue(ClassifierConstants.COL_FIRST_NAME),
            dataRow.getValue(ClassifierConstants.COL_LAST_NAME)));
      }

    } else {
      String managers = getEditorValue(NAME_MANAGERS);
      if (!BeeUtils.isEmpty(managers)) {
        filter.add(Filter.any(ClassifierConstants.COL_COMPANY_PERSON,
            DataUtils.parseIdSet(managers)));
        captions.add(getFilterLabel(NAME_MANAGERS));
      }
    }

    if (ArrayUtils.contains(colNames, AdministrationConstants.COL_DEPARTMENT)) {
      Long department = dataRow.getLong(AdministrationConstants.COL_DEPARTMENT);
      if (DataUtils.isId(department)) {
        filter.add(Filter.equals(AdministrationConstants.COL_DEPARTMENT, department));
        captions.add(dataRow.getValue(AdministrationConstants.COL_DEPARTMENT_NAME));
      }

    } else {
      String departments = getEditorValue(NAME_DEPARTMENTS);
      if (!BeeUtils.isEmpty(departments)) {
        filter.add(Filter.any(AdministrationConstants.COL_DEPARTMENT,
            DataUtils.parseIdSet(departments)));
        captions.add(getFilterLabel(NAME_DEPARTMENTS));
      }
    }

    AssessmentStatus status = null;

    if (cellElement.hasClassName(STYLE_ANSWERED)) {
      status = AssessmentStatus.ANSWERED;
      captions.add(Localized.dictionary().trAssessmentReportAnswered());

    } else if (cellElement.hasClassName(STYLE_LOST)) {
      status = AssessmentStatus.LOST;
      captions.add(Localized.dictionary().trAssessmentReportLost());

    } else if (cellElement.hasClassName(STYLE_APPROVED)) {
      status = AssessmentStatus.APPROVED;
      captions.add(Localized.dictionary().trAssessmentReportApproved());

    } else if (cellElement.hasClassName(STYLE_SECONDARY)) {
      filter.add(Filter.notNull(COL_ASSESSMENT));
      captions.add(Localized.dictionary().trAssessmentReportSecondary());
    }

    if (status == null) {
      filter.add(Filter.notNull(COL_ASSESSMENT_STATUS));
    } else {
      filter.add(Filter.isEqual(COL_ASSESSMENT_STATUS, new IntegerValue(status.ordinal())));
    }

    String caption = BeeUtils.notEmpty(BeeUtils.joinItems(captions),
        Localized.dictionary().trAssessmentRequests());

    drillDown(DRILL_DOWN_GRID_NAME, caption, filter);
  }
}
