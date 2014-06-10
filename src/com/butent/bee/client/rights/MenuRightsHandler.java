package com.butent.bee.client.rights;

import com.google.common.collect.Lists;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.Menu;
import com.butent.bee.shared.menu.MenuEntry;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

final class MenuRightsHandler extends MultiRoleForm {

  MenuRightsHandler() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new MenuRightsHandler();
  }

  @Override
  protected RightsObjectType getObjectType() {
    return RightsObjectType.MENU;
  }

  @Override
  protected RightsState getRightsState() {
    return RightsState.VIEW;
  }

  @Override
  protected void initObjects(final Consumer<List<RightsObject>> consumer) {
    BeeKeeper.getRpc().makeGetRequest(Service.GET_MENU, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          response.notify(BeeKeeper.getScreen());

        } else if (response.hasResponse()) {
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          List<RightsObject> result = Lists.newArrayList();
          if (arr != null) {
            for (String s : arr) {
              Menu menu = Menu.restore(s);
              if (menu != null) {
                addMenuObject(result, 0, null, menu);
              }
            }
          }

          consumer.accept(result);
        }
      }
    });
  }

  private boolean addMenuObject(List<RightsObject> result, int level, String parent, Menu menu) {
    ModuleAndSub ms = ModuleAndSub.parse(menu.getModule());
    if (ms != null && !ms.isEnabled()) {
      return false;
    }

    RightsObject object = new RightsObject(menu.getName(),
        Localized.maybeTranslate(menu.getLabel()), ms, level, parent);

    result.add(object);

    if (menu instanceof MenuEntry) {
      int count = 0;

      for (Menu child : ((MenuEntry) menu).getItems()) {
        if (addMenuObject(result, level + 1, object.getName(), child)) {
          count++;
        }
      }

      if (count > 0) {
        object.setHasChildren(true);
      }
    }

    return true;
  }
}
