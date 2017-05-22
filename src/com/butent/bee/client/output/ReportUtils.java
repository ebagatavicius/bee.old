package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.Service.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.WidgetInitializer;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.widget.Frame;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class ReportUtils {

  public interface ReportCallback extends Consumer<FileInfo> {
    default Widget getActionWidget() {
      return null;
    }
  }

  public static void getPdf(String html, Consumer<FileInfo> responseConsumer) {
    ParameterList args = new ParameterList(CREATE_PDF);
    args.addDataItem(VAR_REPORT_DATA, Assert.notEmpty(html));

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        responseConsumer.accept(FileInfo.restore(response.getResponseAsString()));
      }
    });
  }

  public static void getPdfReport(String report, Consumer<FileInfo> reportConsumer,
      Map<String, String> parameters, BeeRowSet... data) {

    makeRequest(report, Assert.notNull(reportConsumer), parameters, data);
  }

  public static void getReports(Report report, Consumer<List<ReportInfo>> consumer) {
    Queries.getRowSet(VIEW_REPORT_SETTINGS, Arrays.asList(COL_RS_USER, COL_RS_PARAMETERS),
        Filter.and(Filter.equalsOrIsNull(COL_RS_USER, BeeKeeper.getUser().getUserId()),
            Filter.equals(COL_RS_REPORT, report.getReportName()), Filter.isNull(COL_RS_CAPTION)),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            List<ReportInfo> reports = new ArrayList<>();
            int userIdx = result.getColumnIndex(COL_RS_USER);
            int idx = result.getColumnIndex(COL_RS_PARAMETERS);

            for (ReportInfo rep : report.getReports()) {
              reports.add(ReportInfo.restore(rep.serialize()));
            }
            for (BeeRow row : result) {
              try {
                ReportInfo rep = ReportInfo.restore(row.getString(idx));
                rep.setId(row.getId());
                rep.setGlobal(BeeUtils.isEmpty(row.getString(userIdx)));
                reports.remove(rep);
                reports.add(rep);
              } catch (Throwable ex) {
                LogUtils.getRootLogger().error(ex, row.getId(), row.getString(idx));
              }
            }
            consumer.accept(reports);
          }
        });
  }

  public static void preview(FileInfo repInfo) {
    preview(repInfo, null);
  }

  public static void preview(FileInfo repInfo, ReportCallback callback) {
    String url = FileUtils.getUrl(repInfo.getHash(), BeeUtils.notEmpty(repInfo.getCaption(),
        repInfo.getName()));

    Frame frame = new Frame(url);

    StyleUtils.setWidth(frame, BeeKeeper.getScreen().getWidth() * 0.7, CssUnit.PX);
    StyleUtils.setHeight(frame, BeeKeeper.getScreen().getHeight() * 0.9, CssUnit.PX);

    if (callback != null) {
      WidgetInitializer actionDesigner = (widget, name) -> {
        if (Objects.equals(name, DialogConstants.WIDGET_SAVE)) {
          return BeeUtils.nvl(callback.getActionWidget(), widget);
        }
        return widget;
      };
      Global.inputWidget(Localized.dictionary().preview(), frame, () -> callback.accept(repInfo),
          null, null, null, actionDesigner);
    } else {
      Global.showModalWidget(Localized.dictionary().preview(), frame);
    }
  }

  public static void showReport(String report, ReportCallback callback,
      Map<String, String> parameters, BeeRowSet... data) {

    getPdfReport(report, repInfo -> preview(repInfo, callback), parameters, data);
  }

  private static void makeRequest(String report, Consumer<FileInfo> responseConsumer,
      Map<String, String> parameters, BeeRowSet... data) {

    ParameterList args = new ParameterList(GET_REPORT);
    args.addDataItem(VAR_REPORT, Assert.notEmpty(report));
    args.addNotEmptyData(VAR_REPORT_FORMAT, "pdf");
    args.addDataItem(VAR_REPORT_PARAMETERS, Codec.beeSerialize(parameters));

    if (!ArrayUtils.isEmpty(data)) {
      args.addDataItem(VAR_REPORT_DATA, Codec.beeSerialize(data));
    }
    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (!response.hasErrors()) {
          FileInfo fileInfo = FileInfo.restore(response.getResponseAsString());
          fileInfo.setDescription(report);
          responseConsumer.accept(fileInfo);
        }
      }
    });
  }

  private ReportUtils() {
  }
}
