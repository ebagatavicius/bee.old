package com.butent.bee.client.modules.administration;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.rights.RightsForm;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.grid.interceptor.UniqueChildInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;

public final class AdministrationKeeper {

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_USERS)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_USERS), event.getRow(),
            Lists.newArrayList(COL_LOGIN, COL_FIRST_NAME, COL_LAST_NAME, ALS_COMPANY_NAME),
            BeeConst.STRING_SPACE));
      }
    }
  }

  public static ParameterList createArgs(String name) {
    ParameterList args = BeeKeeper.getRpc().createParameters(Module.ADMINISTRATION.getName());
    args.addQueryItem(METHOD, name);
    return args;
  }

  public static void register() {
    MenuService.UPDATE_EXCHANGE_RATES.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        AdministrationUtils.updateExchangeRates();
      }
    });

    FormFactory.registerFormInterceptor(FORM_USER, new UserForm());
    FormFactory.registerFormInterceptor(FORM_USER_SETTINGS, new UserSettingsForm());
    FormFactory.registerFormInterceptor(FORM_DEPARTMENT, new DepartmentForm());
    FormFactory.registerFormInterceptor(FORM_NEW_ROLE, new NewRoleForm());

    GridFactory.registerGridInterceptor(NewsConstants.GRID_USER_FEEDS, new UserFeedsInterceptor());

    GridFactory.registerGridInterceptor(GRID_USER_GROUP_MEMBERS,
        UniqueChildInterceptor.forUsers(Localized.getConstants().userGroupAddMembers(),
            COL_UG_GROUP, COL_UG_USER));
    GridFactory.registerGridInterceptor(GRID_ROLE_USERS,
        UniqueChildInterceptor.forUsers(Localized.getConstants().roleAddUsers(),
            COL_ROLE, COL_USER));

    GridFactory.registerGridInterceptor(GRID_THEME_COLORS,
        new UniqueChildInterceptor(Localized.getConstants().newThemeColors(),
            COL_THEME, COL_COLOR, VIEW_COLORS, Lists.newArrayList(COL_COLOR_NAME),
            Lists.newArrayList(COL_COLOR_NAME, COL_BACKGROUND, COL_FOREGROUND)));

    ColorStyleProvider styleProvider = ColorStyleProvider.createDefault(VIEW_COLORS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_COLORS, COL_BACKGROUND, styleProvider);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_COLORS, COL_FOREGROUND, styleProvider);

    ConditionalStyle.registerGridColumnStyleProvider(GRID_THEMES, ALS_DEFAULT_COLOR_NAME,
        ColorStyleProvider.create(VIEW_THEMES, ALS_DEFAULT_BACKGROUND, ALS_DEFAULT_FOREGROUND));

    styleProvider = ColorStyleProvider.createDefault(VIEW_THEME_COLORS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_THEME_COLORS, COL_BACKGROUND,
        styleProvider);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_THEME_COLORS, COL_FOREGROUND,
        styleProvider);

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler(), false);

    RightsForm.register();
  }

  private AdministrationKeeper() {
  }
}
