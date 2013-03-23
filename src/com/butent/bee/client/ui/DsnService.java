package com.butent.bee.client.ui;

import com.google.common.collect.Lists;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dialog.DialogCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeType;
import com.butent.bee.shared.BeeWidget;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.Variable;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class DsnService extends CompositeService {

  private Variable dsn;

  @Override
  protected boolean doStage(final String stg, Object... params) {
    boolean ok = true;

    if (stg.equals(Service.GET_DSNS)) {
      BeeKeeper.getRpc().makeGetRequest(stg,
          new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              Assert.notNull(response);
              String[] dsns = null;

              if (response.hasResponse()) {
                dsns = Codec.beeDeserializeCollection((String) response.getResponse());
              }
              if (!ArrayUtils.isEmpty(dsns)) {
                dsn = new Variable("Choose DSN", BeeType.STRING, BeeKeeper.getUser().getDsn(),
                    BeeWidget.LIST, dsns);

                Global.getInpBoxen().inputVars("Available DSN's", Lists.newArrayList(dsn),
                    new DialogCallback() {
                      @Override
                      public boolean onConfirm(Popup popup) {
                        return doStage(Service.SWITCH_DSN);
                      }
                    });
              } else {
                Global.showError(Lists.newArrayList("No DSN's available"));
                destroy();
              }
            }
          });
      return ok;

    } else if (stg.equals(Service.SWITCH_DSN)) {
      String dsnName = dsn.getValue();

      if (!BeeUtils.isEmpty(dsnName)) {
        ParameterList args = BeeKeeper.getRpc().createParameters(stg);
        args.addQueryItem(Service.VAR_DSN, dsnName);

        BeeKeeper.getRpc().makeGetRequest(args,
            new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                Assert.notNull(response);

                if (response.hasResponse(String.class)) {
                  BeeKeeper.getUser().setDsn((String) response.getResponse());
                }
              }
            });
      }

    } else {
      ok = false;
      Global.showError("Unknown service [" + name() + "] stage: " + stg);
    }
    destroy();
    return ok;
  }

  @Override
  protected CompositeService getInstance() {
    return new DsnService();
  }
}
