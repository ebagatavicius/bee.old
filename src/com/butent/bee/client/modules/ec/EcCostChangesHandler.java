package com.butent.bee.client.modules.ec;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;

import java.util.List;

public class EcCostChangesHandler extends AbstractGridInterceptor {

  @Override
  public void afterCreatePresenter(final GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();
    presenter.getHeader().addCommandItem(new Button(Localized.getConstants().ecUpdateCosts(),
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            final List<Long> idList = Lists.newArrayList();

            for (RowInfo row : presenter.getGridView().getSelectedRows(SelectedRows.ALL)) {
              idList.add(row.getId());
            }
            if (idList.isEmpty()) {
              presenter.getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
              return;
            }
            ParameterList args = EcKeeper.createArgs(SVC_UPDATE_COSTS);
            args.addDataItem(COL_TCD_ARTICLE, DataUtils.buildIdList(idList));

            BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                response.notify(presenter.getGridView());

                if (!response.hasErrors()) {
                  Data.onViewChange(presenter.getViewName(), DataChangeEvent.CANCEL_RESET_REFRESH);
                }
              }
            });
          }
        }));
  }

  @Override
  public GridInterceptor getInstance() {
    return new EcCostChangesHandler();
  }
}
