package com.butent.bee.client.imports;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.websocket.messages.ProgressMessage;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class ImportCallback implements ResponseCallback {

  private String progressId;
  private final CustomAction action;

  public static void makeRequest(ParameterList args, CustomAction action, String progressCaption) {
    ImportCallback callback = new ImportCallback(action);

    Endpoint.initProgress(progressCaption, progress -> {
      if (!BeeUtils.isEmpty(progress)) {
        args.addDataItem(Service.VAR_PROGRESS, progress);
        callback.setProgressId(progress);
      }
      BeeKeeper.getRpc().makePostRequest(args, callback);
    });
  }

  private ImportCallback(CustomAction action) {
    action.running();
    this.action = action;
  }

  @Override
  public void onResponse(ResponseObject response) {
    action.idle();

    if (!BeeUtils.isEmpty(progressId)) {
      Endpoint.removeProgress(progressId);
      Endpoint.send(ProgressMessage.close(progressId));
    }
    if (response.hasErrors()) {
      Global.showError(Arrays.asList(response.getErrors()));
      return;
    }
    if (Objects.nonNull(action.getCallback())) {
      action.getCallback().accept(response);
    } else {
      showResponse(response);
    }
  }

  public void setProgressId(String progressId) {
    this.progressId = progressId;
  }

  public static void showResponse(ResponseObject response) {
    Map<String, String> data = Codec.deserializeLinkedHashMap(response.getResponseAsString());

    HtmlTable table = new HtmlTable(StyleUtils.NAME_INFO_TABLE);
    int r = 0;
    table.setColumnCellClasses(1, StyleUtils.className(TextAlign.CENTER));
    table.setColumnCellClasses(2, StyleUtils.className(TextAlign.CENTER));
    table.setText(r, 1, Localized.dictionary().imported() + " / "
        + Localized.dictionary().updated(), StyleUtils.className(FontWeight.BOLD));
    table.setText(r, 2, Localized.dictionary().errors(), StyleUtils.className(FontWeight.BOLD));

    for (String viewName : data.keySet()) {
      String cap = Objects.isNull(Data.getDataInfo(viewName, false)) ? viewName
          : Data.getViewCaption(viewName);
      table.setText(++r, 0, cap);

      Pair<String, String> pair = Pair.restore(data.get(viewName));

      if (Objects.nonNull(pair.getA())) {
        Pair<String, String> counters = Pair.restore(pair.getA());
        table.setText(r, 1, counters.getA() + " / " + counters.getB());
      }
      InternalLink lbl = null;

      if (pair.getB() != null) {
        BeeRowSet rs = BeeRowSet.restore(pair.getB());
        lbl = new InternalLink(BeeUtils.toString(rs.getNumberOfRows()));

        lbl.addClickHandler(arg0 -> Global.showModalGrid(cap, rs, StyleUtils.NAME_INFO_TABLE));
      }
      table.setWidget(r, 2, lbl);
    }
    Global.showModalWidget(table);
  }
}
