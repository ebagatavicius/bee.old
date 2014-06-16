package com.butent.bee.client.modules.administration;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.xml.client.Element;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.ui.UserInterface.Component;

class UserSettingsForm extends AbstractFormInterceptor {

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (row != null) {
      Long userId = row.getLong(form.getDataIndex(AdministrationConstants.COL_USER));
      
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