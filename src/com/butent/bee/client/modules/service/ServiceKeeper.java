package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.timeboard.TimeBoard;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewSupplier;
import com.butent.bee.client.view.grid.interceptor.FileGridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.Module;

public final class ServiceKeeper {

  public static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "svc-";

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.SERVICE, method);
  }

  public static void register() {
    GridFactory.registerGridInterceptor(VIEW_SERVICE_OBJECTS, new ServiceObjectsGrid());

    GridFactory.registerGridInterceptor(VIEW_SERVICE_FILES,
        new FileGridInterceptor(COL_SERVICE_OBJECT, AdministrationConstants.COL_FILE,
            AdministrationConstants.COL_FILE_CAPTION, AdministrationConstants.ALS_FILE_NAME));

    GridFactory.registerGridInterceptor(VIEW_MAINTENANCE, new MaintenanceGrid());

    GridFactory.registerGridInterceptor(GRID_OBJECT_INVOICES, new ObjectInvoicesGrid());
    GridFactory.registerGridInterceptor(GRID_OBJECT_DEFECTS, new ObjectDefectsGrid());
    GridFactory.registerGridInterceptor(GRID_SERVICE_INVOICES, new ServiceInvoicesGrid());

    FormFactory.registerFormInterceptor("ServiceObject", new ServiceObjectForm());
    FormFactory.registerFormInterceptor("ServiceInvoice", new ServiceInvoiceForm());
    FormFactory.registerFormInterceptor("ServiceDefect", new ServiceDefectForm());

    SelectorEvent.register(new SelectorHandler());

    TimeBoard.ensureStyleSheet();

    MenuService.SERVICE_CALENDAR.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        ServiceCalendar.open(new ViewCallback() {
          @Override
          public void onSuccess(View result) {
            BeeKeeper.getScreen().show(result);
          }
        });
      }
    });

    ViewFactory.registerSupplier(ServiceCalendar.SUPPLIER_KEY, new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        ServiceCalendar.open(callback);
      }
    });
  }

  private ServiceKeeper() {
  }
}
