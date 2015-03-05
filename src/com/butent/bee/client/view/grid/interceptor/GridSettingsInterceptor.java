package com.butent.bee.client.view.grid.interceptor;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.i18n.Localized;

import static com.butent.bee.shared.ui.GridDescription.*;

import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GridSettingsInterceptor extends AbstractGridInterceptor {

  public static final String GRID_NAME = "GridSettings";

  public GridSettingsInterceptor() {
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    if (presenter != null && copy) {
      final IsRow row = presenter.getActiveRow();
      int index = getDataIndex(COL_GRID_SETTING_KEY);

      final String key = (row == null) ? null : row.getString(index);

      if (!BeeUtils.isEmpty(key)) {
        Queries.getRowSet(VIEW_GRID_SETTINGS,
            Collections.singletonList(COL_GRID_SETTING_USER),
            Filter.isEqual(COL_GRID_SETTING_KEY, new TextValue(key)),
            new Queries.RowSetCallback() {

              @Override
              public void onSuccess(BeeRowSet result) {
                if (!DataUtils.isEmpty(result)) {
                  List<Long> users = DataUtils.getDistinct(result, COL_GRID_SETTING_USER);
                  if (!BeeUtils.isEmpty(users)) {
                    onCopy(presenter, row.getId(), key, users);
                  }
                }
              }
            });
      }

      return false;

    } else {
      return true;
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new GridSettingsInterceptor();
  }

  private static void doCopy(long rowId, Collection<Long> users) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.COPY_GRID_SETTINGS);

    params.addQueryItem(Service.VAR_ID, rowId);
    params.addQueryItem(COL_GRID_SETTING_USER, DataUtils.buildIdList(users));

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse()) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_GRID_SETTINGS);
        }
      }
    });
  }

  private void onCopy(final GridPresenter presenter, final long rowId, String key,
      Collection<Long> existingUsers) {

    Relation relation = Relation.create();
    relation.setViewName(VIEW_USERS);

    List<String> choiceColumns = Arrays.asList(COL_FIRST_NAME, COL_LAST_NAME,
        ALS_COMPANY_NAME, ALS_POSITION_NAME);

    BeeUtils.overwrite(relation.getChoiceColumns(), choiceColumns);
    BeeUtils.overwrite(relation.getSearchableColumns(), choiceColumns);

    relation.disableNewRow();

    List<String> renderColumns = Arrays.asList(COL_FIRST_NAME, COL_LAST_NAME);
    final MultiSelector selector = MultiSelector.autonomous(relation, renderColumns);

    selector.setAdditionalFilter(Filter.idNotIn(existingUsers));

    int width = getGridView().asWidget().getOffsetWidth();
    StyleUtils.setWidth(selector, BeeUtils.clamp(width - 50, 300, 600));

    String caption = BeeUtils.joinWords(key, Localized.getConstants().users());
    Global.inputWidget(caption, selector, new InputCallback() {
      @Override
      public void onSuccess() {
        List<Long> users = DataUtils.parseIdList(selector.getValue());
        if (!users.isEmpty()) {
          doCopy(rowId, users);
        }
      }
    }, null, presenter.getHeader().getElement());
  }
}
