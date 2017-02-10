package com.butent.bee.client.modules.service;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.trade.InvoicesGrid;
import com.butent.bee.client.timeboard.TimeBoard;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewSupplier;
import com.butent.bee.client.view.grid.interceptor.FileGridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.service.ServiceMaintenanceType;
import com.butent.bee.shared.rights.Module;

public final class ServiceKeeper {

  public static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "svc-";

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_SERVICE_FILES)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_SERVICE_FILES), event.getRow(),
            Lists.newArrayList(COL_SERVICE_OBJECT, AdministrationConstants.COL_FILE_CAPTION,
                AdministrationConstants.ALS_FILE_TYPE),
            BeeConst.STRING_SPACE));
      }
    }
  }

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
    GridFactory.registerGridInterceptor(GRID_SERVICE_INVOICES, new InvoicesGrid());

    for (ServiceMaintenanceType st : ServiceMaintenanceType.values()) {
      GridFactory.registerGridSupplier(st.getSupplierKey(), GRID_SERVICE_MAINTENANCE,
          new ServiceMaintenanceGrid(st));
    }

    MenuService.SERVICE_MAINTENANCE_LIST.setHandler(parameters -> {
      ServiceMaintenanceType type = ServiceMaintenanceType.getByPrefix(parameters);

      if (type == null) {
        Global.showError(Lists.newArrayList(GRID_SERVICE_MAINTENANCE, "Type not recognized:",
            parameters));
      } else {
        ViewFactory.createAndShow(type.getSupplierKey());
      }
    });

    FormFactory.registerFormInterceptor("ServiceObject", new ServiceObjectForm());
    FormFactory.registerFormInterceptor("ServiceInvoice", new ServiceInvoiceForm());
    FormFactory.registerFormInterceptor("ServiceDefect", new ServiceDefectForm());
    FormFactory.registerFormInterceptor("ServiceMaintenance", new ServiceMaintenanceForm());

    SelectorEvent.register(new SelectorHandler());

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler());

    BeeKeeper.getBus().registerRowActionHandler(new RowActionEvent.Handler() {
      @Override
      public void onRowAction(RowActionEvent event) {
        if (event.isEditRow() && event.hasView(VIEW_SERVICE_FILES)) {
          event.consume();
          if (event.hasRow() && event.getOpener() != null) {
            Long objectId = Data.getLong(event.getViewName(), event.getRow(), COL_SERVICE_OBJECT);
            RowEditor.open(VIEW_SERVICE_OBJECTS, objectId, event.getOpener());
          }
        }
      }
    });


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
