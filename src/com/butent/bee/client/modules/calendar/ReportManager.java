package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.ProviderType;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.calendar.CalendarConstants.Report;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

class ReportManager {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "cal-ReportOptions-";

  private static void addStyle(Widget widget, String styleName) {
    widget.addStyleName(STYLE_PREFIX + styleName);
  }

  private static Editor createDateEditor(ValueType type) {
    if (ValueType.DATE.equals(type)) {
      return new InputDate();
    } else {
      return new InputDateTime();
    }
  }

  private static void doReport(final Report report, final BeeRow row) {
    ParameterList params = CalendarKeeper.createArgs(SVC_DO_REPORT);
    params.addQueryItem(PARAM_REPORT, report.ordinal());

    BeeRowSet rowSet = new BeeRowSet(VIEW_REPORT_OPTIONS, Data.getColumns(VIEW_REPORT_OPTIONS));
    rowSet.addRow(row);

    BeeKeeper.getRpc().sendText(params, Codec.beeSerialize(rowSet), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        BeeRowSet rs = null;
        if (response.hasResponse(BeeRowSet.class)) {
          rs = BeeRowSet.restore((String) response.getResponse());
        }

        if (rs == null || rs.isEmpty()) {
          BeeKeeper.getScreen().notifyWarning(report.getCaption(),
              Localized.dictionary().noData());
        } else {
          showReport(report, getReportCaption(report, row), rs);
        }
      }
    });
  }

  private static String getReportCaption(Report report, BeeRow row) {
    String caption = Data.getString(VIEW_REPORT_OPTIONS, row, COL_CAPTION);
    if (!BeeUtils.isEmpty(caption)) {
      return caption.trim();
    }

    StringBuilder sb = new StringBuilder(report.getCaption());
    String separator = BeeUtils.space(2);

    JustDate lower = Data.getDate(VIEW_REPORT_OPTIONS, row, COL_LOWER_DATE);
    if (lower != null) {
      sb.append(separator).append(Localized.dictionary().dateFromShort().toLowerCase()).append(
          separator).append(Format.renderDate(lower));
    }

    JustDate upper = Data.getDate(VIEW_REPORT_OPTIONS, row, COL_UPPER_DATE);
    if (upper != null) {
      sb.append(separator).append(Localized.dictionary().dateToShort().toLowerCase()).append(
          separator).append(Format.renderDate(upper));
    }

    return sb.toString();
  }

  private static void showReport(Report report, String caption, BeeRowSet rowSet) {
    String gridName = "CalendarReport" + report.name();
    GridDescription gridDescription = new GridDescription(gridName);

    gridDescription.setCaption(caption);
    gridDescription.setReadOnly(true);

    for (int i = 0; i < rowSet.getNumberOfColumns(); i++) {
      String colName = rowSet.getColumn(i).getId();
      ColumnDescription columnDescription = new ColumnDescription(ColType.DATA, colName);

      switch (i) {
        case 0:
          columnDescription.setCaption(Localized.dictionary().calReportLowerHour());
          break;
        case 1:
          columnDescription.setCaption(Localized.dictionary().calAttendee());
          break;
        default:
          columnDescription.setHorAlign(TextAlign.RIGHT.getCssName());
          break;
      }

      columnDescription.setSource(colName);
      columnDescription.setSortable(true);

      gridDescription.addColumn(columnDescription);
    }

    Collection<UiOption> uiOptions = EnumSet.of(UiOption.GRID);

    GridInterceptor gridInterceptor = GridFactory.getGridInterceptor(gridName);

    GridView gridView = GridFactory.createGridView(gridDescription,
        GridFactory.getSupplierKey(gridName, gridInterceptor), rowSet.getColumns(), null,
        uiOptions, gridInterceptor, null, null);

    gridView.initData(rowSet.getNumberOfRows(), rowSet);

    GridPresenter presenter = new GridPresenter(gridDescription, gridView,
        rowSet.getNumberOfRows(), rowSet, ProviderType.LOCAL, CachingPolicy.NONE, uiOptions);

    BeeKeeper.getScreen().show(presenter.getMainView());
  }

  private final Map<Report, BeeRow> reportOptions = new HashMap<>();

  ReportManager() {
    super();
  }

  void onSelectReport(final Report report) {
    Assert.notNull(report);

    BeeRow options = reportOptions.get(report);
    if (options != null) {
      openDialog(report, options);
      return;
    }

    ParameterList params = CalendarKeeper.createArgs(SVC_GET_REPORT_OPTIONS);
    params.addQueryItem(PARAM_REPORT, report.ordinal());

    BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        BeeRow row = null;
        if (response.hasResponse(BeeRow.class)) {
          row = BeeRow.restore((String) response.getResponse());
        }

        if (row == null) {
          BeeKeeper.getScreen().notifyWarning(report.getCaption(),
              Localized.dictionary().noData());
        } else {
          reportOptions.put(report, row);
          openDialog(report, row);
        }
      }
    });
  }

  private void openDialog(final Report report, final BeeRow options) {
    final String viewName = VIEW_REPORT_OPTIONS;

    final Flow container = new Flow();
    addStyle(container, "container");

    Label capLabel = new Label(Localized.dictionary().calName());
    addStyle(capLabel, "capLabel");

    container.add(capLabel);

    final InputText caption = new InputText();
    caption.setValue(BeeUtils.trim(Data.getString(viewName, options, COL_CAPTION)));
    Integer precision = Data.getColumnPrecision(viewName, COL_CAPTION);
    if (BeeUtils.isPositive(precision)) {
      caption.setMaxLength(precision);
    }
    addStyle(caption, "caption");
    AutocompleteProvider.enableAutocomplete(caption, viewName, COL_CAPTION);

    container.add(caption);

    Label ldLabel = new Label(Localized.dictionary().calReportLowerDate());
    addStyle(ldLabel, "ldLabel");

    container.add(ldLabel);

    final Editor lowerDate = createDateEditor(Data.getColumnType(viewName, COL_LOWER_DATE));
    lowerDate.setValue(Data.getString(viewName, options, COL_LOWER_DATE));
    addStyle(lowerDate.asWidget(), "lowerDate");

    container.add(lowerDate);

    Label udLabel = new Label(Localized.dictionary().calReportUpperDate());
    addStyle(udLabel, "udLabel");

    container.add(udLabel);

    final Editor upperDate = createDateEditor(Data.getColumnType(viewName, COL_UPPER_DATE));
    upperDate.setValue(Data.getString(viewName, options, COL_UPPER_DATE));
    addStyle(upperDate.asWidget(), "upperDate");

    container.add(upperDate);

    final InputSpinner lowerHour;
    final InputSpinner upperHour;

    if (EnumSet.of(Report.BUSY_HOURS, Report.CANCEL_HOURS).contains(report)) {
      Label lhLabel = new Label(Localized.dictionary().calReportLowerHour());
      addStyle(lhLabel, "lhLabel");

      container.add(lhLabel);

      lowerHour = new InputSpinner(0, TimeUtils.HOURS_PER_DAY - 1);
      lowerHour.setValue(BeeUtils.unbox(Data.getInteger(viewName, options, COL_LOWER_HOUR)));
      addStyle(lowerHour, "lowerHour");

      container.add(lowerHour);

      Label uhLabel = new Label(Localized.dictionary().calReportUpperHour());
      addStyle(uhLabel, "uhLabel");

      container.add(uhLabel);

      upperHour = new InputSpinner(0, TimeUtils.HOURS_PER_DAY);
      int value = BeeUtils.positive(BeeUtils.unbox(Data.getInteger(viewName, options,
          COL_UPPER_HOUR)), TimeUtils.HOURS_PER_DAY);
      upperHour.setValue(value);
      addStyle(upperHour, "upperHour");

      container.add(upperHour);

    } else {
      lowerHour = null;
      upperHour = null;
    }

    Label atpLabel = new Label(Localized.dictionary().calAttendeeTypes());
    addStyle(atpLabel, "atpLabel");

    container.add(atpLabel);

    Relation atpRel = Relation.create(VIEW_ATTENDEE_TYPES,
        Lists.newArrayList(COL_APPOINTMENT_TYPE_NAME));
    atpRel.disableNewRow();
    final MultiSelector atpSelector = MultiSelector.autonomous(atpRel,
        RendererFactory.createRenderer(VIEW_ATTENDEE_TYPES,
            Lists.newArrayList(COL_APPOINTMENT_TYPE_NAME)));

    atpSelector.setIds(Data.getString(viewName, options, COL_ATTENDEE_TYPES));
    addStyle(atpSelector, "attendeeTypes");

    container.add(atpSelector);

    Label attLabel = new Label(Localized.dictionary().calAttendees());
    addStyle(attLabel, "attLabel");

    container.add(attLabel);

    Relation attRel = Relation.create(VIEW_ATTENDEES,
        Lists.newArrayList(COL_ATTENDEE_NAME, ALS_ATTENDEE_TYPE_NAME));
    attRel.disableNewRow();

    final MultiSelector attSelector = MultiSelector.autonomous(attRel,
        RendererFactory.createRenderer(VIEW_ATTENDEES, Lists.newArrayList(COL_ATTENDEE_NAME)));

    attSelector.setIds(Data.getString(viewName, options, COL_ATTENDEES));
    addStyle(attSelector, "attendees");

    container.add(attSelector);

    final Button tableCommand = new Button(Localized.dictionary().calTable(), new Command() {
      @Override
      public void execute() {
        String vCap = caption.getValue();
        if (!BeeUtils.isEmpty(vCap)) {
          AutocompleteProvider.retainValue(caption);
        }

        JustDate vLd = TimeUtils.parseDate(lowerDate.getValue());
        JustDate vUd = TimeUtils.parseDate(upperDate.getValue());

        if (vLd != null && vUd != null && TimeUtils.isMeq(vLd, vUd)) {
          Global.showError(Localized.dictionary().calInvalidDateInterval());
          return;
        }

        int vLh = (lowerHour == null) ? 0 : lowerHour.getIntValue();
        int vUh = (upperHour == null) ? 0 : upperHour.getIntValue();
        if (vUh > 0 && vUh <= vLh) {
          Global.showError(Localized.dictionary().calInvalidHoursInterval());
          return;
        }

        BeeRow newRow = DataUtils.cloneRow(options);
        Data.setValue(viewName, newRow, COL_CAPTION, vCap);
        Data.setValue(viewName, newRow, COL_LOWER_DATE, vLd);
        Data.setValue(viewName, newRow, COL_UPPER_DATE, vUd);

        if (lowerHour != null) {
          Data.setValue(viewName, newRow, COL_LOWER_HOUR, vLh);
        }
        if (upperHour != null) {
          Data.setValue(viewName, newRow, COL_UPPER_HOUR, vUh);
        }

        Data.setValue(viewName, newRow, COL_ATTENDEE_TYPES, atpSelector.getValue());
        Data.setValue(viewName, newRow, COL_ATTENDEES, attSelector.getValue());

        reportOptions.put(report, newRow);
        doReport(report, newRow);

        UiHelper.closeDialog(container);
      }
    });
    addStyle(tableCommand, "tableCommand");

    container.add(tableCommand);

    DialogBox dialog = DialogBox.create(report.getCaption(), DialogConstants.STYLE_REPORT_OPTIONS);
    dialog.setWidget(container);

    dialog.setAnimationEnabled(true);
    dialog.center();

    caption.setFocus(true);
  }
}
