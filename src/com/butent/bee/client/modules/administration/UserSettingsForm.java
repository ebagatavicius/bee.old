package com.butent.bee.client.modules.administration;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

class UserSettingsForm extends AbstractFormInterceptor {

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (row != null) {
      Long userId = row.getLong(form.getDataIndex(COL_USER));

      if (BeeKeeper.getUser().is(userId)) {
        if (!getHeaderView().hasCommands()) {
          getHeaderView().addCommandItem(new Button(Localized.getConstants().changePassword(),
              new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  PasswordService.change();
                }
              }));
        }

      } else if (getHeaderView().hasCommands()) {
        getHeaderView().clearCommandPanel();
      }

      if (DataUtils.isId(userId) && BeeKeeper.getUser().isDataVisible(VIEW_USER_ROLES)) {
        final Widget roleWidget = form.getWidgetByName(VIEW_USER_ROLES);

        Queries.getRowSet(VIEW_USER_ROLES, Lists.newArrayList(ALS_ROLE_NAME),
            Filter.equals(COL_USER, userId), new Queries.RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet result) {
                List<String> roles = new ArrayList<>();

                if (!DataUtils.isEmpty(result)) {
                  int index = result.getColumnIndex(ALS_ROLE_NAME);
                  for (BeeRow r : result) {
                    roles.add(r.getString(index));
                  }
                }

                if (roleWidget != null) {
                  roleWidget.getElement().setInnerText(BeeUtils.joinItems(roles));
                }
              }
            });
      }
    }
  }

  @Override
  public boolean beforeCreateWidget(String name, Element description) {
    if (NewsConstants.GRID_USER_FEEDS.equals(name)) {
      return BeeKeeper.getScreen().getUserInterface().hasComponent(Component.NEWS);
    } else {
      return super.beforeCreateWidget(name, description);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new UserSettingsForm();
  }
}