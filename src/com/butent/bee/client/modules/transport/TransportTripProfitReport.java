package com.butent.bee.client.modules.transport;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.output.ReportParameters;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.Arrays;
import java.util.List;

public class TransportTripProfitReport extends ReportInterceptor {

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_END_DATE = "EndDate";

  private static final String NAME_CURRENCY = AdministrationConstants.COL_CURRENCY;

  private static final List<String> FILTER_NAMES = Arrays.asList(COL_VEHICLE, COL_TRIP_NO);

  private static final String STYLE_PREFIX = "bee-ta-report-ibc-";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_HEADER = STYLE_PREFIX + "header";
  private static final String STYLE_BODY = STYLE_PREFIX + "body";
  private static final String STYLE_FOOTER = STYLE_PREFIX + "footer";

  @Override
  public FormInterceptor getInstance() {
    return new TransportTripProfitReport();
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      loadDateTime(parameters, NAME_START_DATE, form);
      loadDateTime(parameters, NAME_END_DATE, form);
      loadId(parameters, NAME_CURRENCY, form);

      loadIds(parameters, COL_VEHICLE, form);
      loadText(parameters, COL_TRIP_NO, form);
    }

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(NAME_START_DATE, NAME_END_DATE);
    storeEditorValues(NAME_CURRENCY);

    storeEditorValues(FILTER_NAMES);
  }

  @Override
  protected void clearFilter() {
    clearEditor(NAME_START_DATE);
    clearEditor(NAME_END_DATE);

    for (String name : FILTER_NAMES) {
      clearEditor(name);
    }
  }

  @Override
  protected void doReport() {
    DateTime start = getDateTime(NAME_START_DATE);
    DateTime end = getDateTime(NAME_END_DATE);

    if (!checkRange(start, end)) {
      return;
    }
    ParameterList params = TransportHandler.createArgs(SVC_TRIP_PROFIT_REPORT);

    if (start != null) {
      params.addDataItem(Service.VAR_FROM, start.getTime());
    }
    if (end != null) {
      params.addDataItem(Service.VAR_TO, end.getTime());
    }
    String currency = getEditorValue(NAME_CURRENCY);

    if (DataUtils.isId(currency)) {
      params.addDataItem(AdministrationConstants.COL_CURRENCY, currency);
    }
    for (String name : FILTER_NAMES) {
      String value = getEditorValue(name);

      if (!BeeUtils.isEmpty(value)) {
        params.addDataItem(name, value);
      }
    }
    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());

        if (response.hasErrors()) {
          return;
        }
        if (response.hasResponse(SimpleRowSet.class)) {
          renderData(SimpleRowSet.restore(response.getResponseAsString()));
        } else {
          getFormView().notifyWarning(Localized.getConstants().nothingFound());
        }
      }
    });
  }

  @Override
  protected String getBookmarkLabel() {
    List<String> labels = StringList.of(getReportCaption(),
        Format.renderPeriod(getDateTime(NAME_START_DATE), getDateTime(NAME_END_DATE)),
        getFilterLabel(NAME_CURRENCY));

    for (String name : FILTER_NAMES) {
      labels.add(getFilterLabel(name));
    }
    return BeeUtils.joinWords(labels);
  }

  @Override
  protected Report getReport() {
    return Report.TRANSPORT_TRIP_PROFIT;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, NAME_START_DATE, NAME_END_DATE);
    addEditorValues(parameters, NAME_CURRENCY);

    addEditorValues(parameters, FILTER_NAMES);

    return parameters;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    DateTime start = parameters.getDateTime(NAME_START_DATE);
    DateTime end = parameters.getDateTime(NAME_END_DATE);

    return checkRange(start, end);
  }

  private void renderData(SimpleRowSet rowSet) {
    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }
    if (!container.isEmpty()) {
      container.clear();
    }
    String styleRightAlign = StyleUtils.className(TextAlign.RIGHT);

    HtmlTable table = new HtmlTable(STYLE_TABLE);
    int c = 0;

    for (String cap : rowSet.getColumnNames()) {
      table.setText(0, c++, cap);
    }
    int r = 0;
    table.getRowFormatter().addStyleName(r, STYLE_HEADER);

    for (final SimpleRow row : rowSet) {
      r++;
      c = 0;

      for (String colName : rowSet.getColumnNames()) {
        if (BeeUtils.same(colName, COL_TRIP_NO)) {
          InternalLink link = new InternalLink(row.getValue(colName));

          link.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              RowEditor.open(TBL_TRIPS, row.getLong(COL_TRIP), Opener.MODAL);
            }
          });
          table.setWidget(r, c++, link);
        } else {
          String style = null;
          String text = null;

          if (BeeUtils.isSuffix(colName, "Costs") || BeeUtils.same(colName, "Incomes")) {
            style = styleRightAlign;
            Double val = row.getDouble(colName);

            if (BeeUtils.isDouble(val)) {
              text = Format.getDefaultMoneyFormat().format(val);
            }
          } else if (BeeUtils.same(colName, "Kilometers")) {
            style = styleRightAlign;
            Double val = row.getDouble(colName);

            if (BeeUtils.isDouble(val)) {
              text = Format.getDefaultDoubleFormat().format(val);
            }
          } else if (BeeUtils.isPrefix(colName, "Date")) {
            text = TimeUtils.renderDate(row.getDate(colName));

          } else {
            text = row.getValue(colName);
          }
          table.setText(r, c++, text, style);
        }
      }
      table.getRowFormatter().addStyleName(r, STYLE_BODY);
    }
    container.add(table);
  }
}
