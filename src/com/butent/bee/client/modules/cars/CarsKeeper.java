package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.administration.StageUtils;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.modules.classifiers.VehiclesGrid;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.grid.interceptor.FileGridInterceptor;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.Module;

public final class CarsKeeper {

  public static ParameterList createSvcArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.CARS, method);
  }

  public static void register() {
    MenuService.CAR_SERVICE_CALENDAR.setHandler(parameters ->
        BeeKeeper.getRpc().makeGetRequest(createSvcArgs(SVC_GET_CALENDAR), new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            CalendarKeeper.openCalendar(response.getResponseAsLong(), result ->
                BeeKeeper.getScreen().showInNewPlace(result));
          }
        }));

    FormFactory.registerFormInterceptor(TBL_CONF_PRICELIST, new ConfPricelistForm());
    FormFactory.registerFormInterceptor(FORM_CONF_OPTION, new ConfOptionForm());
    FormFactory.registerFormInterceptor(FORM_CAR_ORDER, new CarOrderForm());
    FormFactory.registerFormInterceptor(FORM_CAR, new CarForm());
    FormFactory.registerFormInterceptor(FORM_TEMPLATE, new SpecificationForm());

    FormFactory.registerFormInterceptor(FORM_CAR_SERVICE_ORDER, new CarServiceOrderForm());
    FormFactory.registerFormInterceptor(FORM_CAR_SERVICE_EVENT, new CarServiceEventForm());

    GridFactory.registerGridInterceptor(VIEW_CARS, new VehiclesGrid());
    GridFactory.registerGridInterceptor(TBL_CAR_BUNDLE_JOBS, new CarBundleJobsGrid());

    GridFactory.registerGridInterceptor(VIEW_CAR_FILES, new FileGridInterceptor(COL_CAR,
        AdministrationConstants.COL_FILE, AdministrationConstants.COL_FILE_CAPTION,
        AdministrationConstants.ALS_FILE_NAME));
    GridFactory.registerGridInterceptor(VIEW_CAR_SERVICE_FILES,
        new FileGridInterceptor(COL_SERVICE_ORDER, AdministrationConstants.COL_FILE,
            AdministrationConstants.COL_FILE_CAPTION, AdministrationConstants.ALS_FILE_NAME));

    Dictionary loc = Localized.dictionary();
    StageUtils.registerStageAction(TBL_CAR_ORDERS, STAGE_ACTION_READONLY, loc.rowIsReadOnly());
    StageUtils.registerStageAction(TBL_CAR_ORDERS, STAGE_ACTION_LOST, loc.reason());

    StageUtils.registerStageTrigger(TBL_CAR_ORDERS, STAGE_TRIGGER_NEW, loc.newOrder());
    StageUtils.registerStageTrigger(TBL_CAR_ORDERS, STAGE_TRIGGER_SENT, loc.messageSent());
  }

  private CarsKeeper() {
  }
}
