package com.butent.bee.client.modules.service;

import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.HandlesDeleteEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MaintenanceGrid extends AbstractGridInterceptor implements HandlesDeleteEvents {

  private static final String STYLE_COMMAND = ServiceKeeper.STYLE_PREFIX + "build-command";

  private static final String STYLE_BUILD_INVOICE_DISABLED = ServiceKeeper.STYLE_PREFIX
      + "build-invoice-disabled";
  private static final String STYLE_BUILD_DEFECT_DISABLED = ServiceKeeper.STYLE_PREFIX
      + "build-defect-disabled";

  private final List<HandlerRegistration> registry = new ArrayList<>();

  Button invoiceCommand;
  Button defectCommand;

  MaintenanceGrid() {
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (getInvoiceCommand() == null) {
      setInvoiceCommand(new Button(Localized.getConstants().createInvoice()));
      getInvoiceCommand().addStyleName(STYLE_COMMAND);

      getInvoiceCommand().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          InvoiceBuilder.start(getGridView());
        }
      });

      presenter.getHeader().addCommandItem(getInvoiceCommand());
    }

    if (getDefectCommand() == null) {
      setDefectCommand(new Button(Localized.getConstants().svcDefect()));
      getDefectCommand().addStyleName(STYLE_COMMAND);

      getDefectCommand().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          DefectBuilder.start(getGridView());
        }
      });

      presenter.getHeader().addCommandItem(getDefectCommand());
    }
  }

  @Override
  public void afterRender(GridView gridView, RenderingEvent event) {
    refreshCommands();
  }

  @Override
  public GridInterceptor getInstance() {
    return new MaintenanceGrid();
  }

  @Override
  public void onLoad(GridView gridView) {
    EventUtils.clearRegistry(registry);
    registry.addAll(BeeKeeper.getBus().registerDeleteHandler(this, false));
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (event != null) {
      Set<Long> ids = event.getRowIds();

      if (isRelevantInvoice(event.getViewName(), ids)
          || isRelevantDefect(event.getViewName(), ids)) {
        getGridPresenter().refresh(false);
      }
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (event != null) {
      Set<Long> ids = Sets.newHashSet(event.getRowId());

      if (isRelevantInvoice(event.getViewName(), ids)
          || isRelevantDefect(event.getViewName(), ids)) {
        getGridPresenter().refresh(false);
      }
    }
  }

  @Override
  public void onUnload(GridView gridView) {
    EventUtils.clearRegistry(registry);
  }

  private Button getDefectCommand() {
    return defectCommand;
  }

  private Button getInvoiceCommand() {
    return invoiceCommand;
  }

  private boolean isRelevantDefect(String viewName, Collection<Long> ids) {
    if (!BeeUtils.same(viewName, VIEW_SERVICE_DEFECTS) || BeeUtils.isEmpty(ids)) {
      return false;
    }

    List<IsRow> data = getGridView().getGrid().getRowData();
    if (BeeUtils.isEmpty(data)) {
      return false;
    }

    int index = getDataIndex(COL_MAINTENANCE_DEFECT);

    for (IsRow row : data) {
      Long dfId = row.getLong(index);
      if (DataUtils.isId(dfId) && ids.contains(dfId)) {
        return true;
      }
    }
    return false;
  }

  private boolean isRelevantInvoice(String viewName, Collection<Long> ids) {
    if (!BeeUtils.same(viewName, VIEW_SERVICE_INVOICES) || BeeUtils.isEmpty(ids)) {
      return false;
    }

    List<IsRow> data = getGridView().getGrid().getRowData();
    if (BeeUtils.isEmpty(data)) {
      return false;
    }

    int index = getDataIndex(COL_MAINTENANCE_INVOICE);

    for (IsRow row : data) {
      Long invId = row.getLong(index);
      if (DataUtils.isId(invId) && ids.contains(invId)) {
        return true;
      }
    }
    return false;
  }

  private void refreshCommands() {
    if (getGridView() != null && getInvoiceCommand() != null && getDefectCommand() != null) {
      boolean invEnable = false;
      boolean dfEnable = false;

      List<? extends IsRow> data = getGridView().getRowData();

      if (!BeeUtils.isEmpty(data)) {
        int invIndex = getDataIndex(COL_MAINTENANCE_INVOICE);
        int dfIndex = getDataIndex(COL_MAINTENANCE_DEFECT);

        for (IsRow row : data) {
          if (row.isNull(invIndex)) {
            invEnable = true;
            if (dfEnable) {
              break;
            }
          }

          if (row.isNull(dfIndex)) {
            dfEnable = true;
            if (invEnable) {
              break;
            }
          }
        }
      }

      invEnable &= BeeKeeper.getUser().canCreateData(VIEW_SERVICE_INVOICES);
      dfEnable &= BeeKeeper.getUser().canCreateData(VIEW_SERVICE_DEFECTS);

      FormView form = UiHelper.getForm(getGridView().asWidget());

      if (getInvoiceCommand().isEnabled() != invEnable) {
        getInvoiceCommand().setEnabled(invEnable);
        if (form != null) {
          form.asWidget().setStyleName(STYLE_BUILD_INVOICE_DISABLED, !invEnable);
        }
      }

      if (getDefectCommand().isEnabled() != dfEnable) {
        getDefectCommand().setEnabled(dfEnable);
        if (form != null) {
          form.asWidget().setStyleName(STYLE_BUILD_DEFECT_DISABLED, !dfEnable);
        }
      }
    }
  }

  private void setDefectCommand(Button defectCommand) {
    this.defectCommand = defectCommand;
  }

  private void setInvoiceCommand(Button invoiceCommand) {
    this.invoiceCommand = invoiceCommand;
  }
}
