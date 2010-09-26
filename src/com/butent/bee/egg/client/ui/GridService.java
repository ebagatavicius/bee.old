package com.butent.bee.egg.client.ui;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.Panel;

import com.butent.bee.egg.client.BeeGlobal;
import com.butent.bee.egg.client.BeeKeeper;
import com.butent.bee.egg.client.data.ResponseData;
import com.butent.bee.egg.client.utils.BeeXml;
import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.BeeService;
import com.butent.bee.egg.shared.data.BeeView;
import com.butent.bee.egg.shared.utils.BeeUtils;

public class GridService extends CompositeService {

  private enum Stages {
    REQUEST_GRID, SHOW_GRID
  }

  private Stages stage = null;
  private Panel destination = null;

  public GridService() {
  }

  public GridService(String serviceId) {
    super(serviceId);
    nextStage();
  }

  @Override
  public CompositeService createInstance(String serviceId) {
    Assert.notEmpty(serviceId);

    return new GridService(serviceId);
  }

  @Override
  public boolean doService(Object... params) {
    Assert.notNull(stage);
    boolean ok = true;

    switch (stage) {
      case REQUEST_GRID:
        destination = (Panel) params[0];
        String grd = (String) params[1];

        BeeKeeper.getRpc().makePostRequest(
            appendId("rpc_ui_grid"),
            BeeXml.createString(BeeService.XML_TAG_DATA, "grid_name",
                grd.replaceFirst("[,].*", "").replaceAll("[\\[\\]\"]", "'")));
        break;

      case SHOW_GRID:
        JsArrayString arr = (JsArrayString) params[0];
        int cc = (Integer) params[1];

        BeeView view = new ResponseData(arr, cc);
        destination.add(BeeGlobal.simpleGrid(view));
        break;

      default:
        BeeGlobal.showError("Unhandled stage: " + stage);
        ok = false;
        break;
    }

    if (ok) {
      nextStage();
    } else {
      BeeGlobal.unregisterService(serviceId);
    }
    return ok;
  }

  private void nextStage() {
    int x = 0;

    if (!BeeUtils.isEmpty(stage)) {
      x = stage.ordinal() + 1;
    }

    if (x < Stages.values().length) {
      stage = Stages.values()[x];
    } else {
      BeeGlobal.unregisterService(serviceId);
    }
  }
}
