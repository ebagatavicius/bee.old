package com.butent.bee.client.modules.service;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_STATE;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.data.event.RowUpdateEvent;

public class MaintenanceCommentForm extends AbstractFormInterceptor
    implements SelectorEvent.Handler {

  private static final String WIDGET_LABEL_NAME = "Label";

  private final IsRow serviceMaintenance;
  private IsRow stateProcessRow;
  private UnboundSelector widgetTypeSelector;
  private InputDateTime warrantyValidToDate;
  private InputBoolean urgentCheckbox;

  MaintenanceCommentForm(IsRow serviceMaintenance) {
    this.serviceMaintenance = serviceMaintenance;
  }

  public MaintenanceCommentForm(IsRow maintenanceRow, IsRow stateProcessRow) {
    super();
    this.serviceMaintenance = maintenanceRow;
    this.stateProcessRow = stateProcessRow;
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {
    if (BeeUtils.same(name, COL_WARRANTY_TYPE) && widget instanceof UnboundSelector) {
      widgetTypeSelector = (UnboundSelector) widget;
      widgetTypeSelector.addSelectorHandler(this);

    } else if (BeeUtils.same(name, COL_WARRANTY) && widget instanceof InputDateTime) {
      warrantyValidToDate = (InputDateTime) widget;

    } else if (BeeUtils.same(name, COL_MAINTENANCE_URGENT) && widget instanceof InputBoolean) {
      urgentCheckbox = (InputBoolean) widget;
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    super.afterInsertRow(result, forced);

    if (warrantyValidToDate != null && !warrantyValidToDate.isEmpty()) {
      result.setProperty(COL_WARRANTY_VALID_TO, warrantyValidToDate.getNormalizedValue());
    }

    if (urgentCheckbox != null && urgentCheckbox.isChecked()) {
      result.setProperty(COL_MAINTENANCE_URGENT, BeeConst.STRING_TRUE);
    }

    if (widgetTypeSelector != null && !BeeUtils.isEmpty(widgetTypeSelector.getValue())) {
      result.setProperty(COL_WARRANTY_TYPE, widgetTypeSelector.getValue());
    }

    ServiceUtils.informClient((BeeRow) result);
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    super.onSaveChanges(listener, event);

    ServiceUtils.informClient((BeeRow) event.getNewRow());

    Long serviceMaintenanceId = event.getNewRow().getLong(getDataIndex(COL_SERVICE_MAINTENANCE));

    if (event.getColumns().contains(Data.getColumn(getViewName(), COL_TERM))
        && DataUtils.isId(serviceMaintenanceId)) {
      Queries.getRow(TBL_SERVICE_MAINTENANCE, serviceMaintenanceId, result -> {
        if (result != null) {
          RowUpdateEvent.fire(BeeKeeper.getBus(), TBL_SERVICE_MAINTENANCE, result);
        }
      });
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    updateTermVisibility(form, row);

    Widget customerSentWidget = form.getWidgetBySource(COL_CUSTOMER_SENT);

    if (customerSentWidget instanceof InputBoolean) {
      boolean isSendEmail = BeeUtils.toBoolean(row.getString(getDataIndex(COL_SEND_EMAIL)));
      boolean isSendSms = BeeUtils.toBoolean(row.getString(getDataIndex(COL_SEND_SMS)));
      ((InputBoolean) customerSentWidget).setEnabled(!(isSendEmail && isSendSms));
    }

    if (serviceMaintenance != null && urgentCheckbox != null) {
      urgentCheckbox.setVisible(!BeeUtils.isEmpty(serviceMaintenance
          .getString(Data.getColumnIndex(TBL_SERVICE_MAINTENANCE, COL_ENDING_DATE))));
    }
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
   if (event.isChanged() && event.hasRelatedView(TBL_WARRANTY_TYPES)
       && warrantyValidToDate != null) {
     warrantyValidToDate.setText(Format.renderDate(ServiceUtils.calculateWarrantyDate(event)));
   }
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    super.onStartNewRow(form, oldRow, newRow);

    if (serviceMaintenance != null) {
      newRow.setValue(getDataIndex(COL_SERVICE_MAINTENANCE), serviceMaintenance.getId());

      newRow.setValue(getDataIndex(COL_MAINTENANCE_STATE), serviceMaintenance
          .getLong(Data.getColumnIndex(TBL_SERVICE_MAINTENANCE, COL_STATE)));

      newRow.setValue(getDataIndex(COL_EVENT_NOTE), serviceMaintenance
          .getString(Data.getColumnIndex(TBL_SERVICE_MAINTENANCE, ALS_STATE_NAME)));
      form.refreshBySource(COL_EVENT_NOTE);

      if (stateProcessRow != null) {
        DataInfo processInfo = Data.getDataInfo(TBL_STATE_PROCESS);
        newRow.setValue(getDataIndex(COL_STATE_COMMENT), Boolean.TRUE);

        setWidgetsVisibility(stateProcessRow.isTrue(processInfo.getColumnIndex(COL_TERM)),
            form.getWidgetBySource(COL_TERM),
            form.getWidgetByName(COL_TERM + WIDGET_LABEL_NAME, false));

        Long itemId = stateProcessRow.getLong(processInfo.getColumnIndex(COL_MAINTENANCE_ITEM));

        boolean visibleItem = DataUtils.isId(itemId);
        setWidgetsVisibility(DataUtils.isId(itemId),
            form.getWidgetByName(COL_ITEM + WIDGET_LABEL_NAME, false),
            form.getWidgetBySource(COL_MAINTENANCE_ITEM),
            form.getWidgetBySource(COL_ITEM_PRICE),
            form.getWidgetBySource(COL_ITEM_CURRENCY));

        if (visibleItem) {
          newRow.setValue(getDataIndex(COL_MAINTENANCE_ITEM), BeeUtils.toString(itemId));
          RelationUtils.updateRow(Data.getDataInfo(getViewName()), COL_ITEM, newRow,
              processInfo, stateProcessRow, false);
          form.refreshBySource(COL_MAINTENANCE_ITEM);

          newRow.setValue(getDataIndex(COL_ITEM_PRICE),
              stateProcessRow.getValue(processInfo.getColumnIndex(COL_ITEM_PRICE)));
          form.refreshBySource(COL_ITEM_PRICE);
        }

        String commentValue = stateProcessRow.getString(processInfo.getColumnIndex(COL_MESSAGE));
        newRow.setValue(getDataIndex(COL_COMMENT), commentValue);
        form.refreshBySource(COL_COMMENT);

        boolean notifyCustomer = stateProcessRow
            .isTrue(processInfo.getColumnIndex(COL_NOTIFY_CUSTOMER));

        if (notifyCustomer) {
          newRow.setValue(getDataIndex(COL_CUSTOMER_SENT), notifyCustomer);
          form.refreshBySource(COL_CUSTOMER_SENT);
        }

        boolean showCustomerValue = stateProcessRow
            .isTrue(processInfo.getColumnIndex(COL_SHOW_CUSTOMER));

        if (showCustomerValue) {
          newRow.setValue(getDataIndex(COL_SHOW_CUSTOMER), showCustomerValue);
          form.refreshBySource(COL_SHOW_CUSTOMER);
        }

        boolean isWarrantyVisible = BeeUtils.toBoolean(stateProcessRow
            .getString(processInfo.getColumnIndex(COL_WARRANTY)));
        setWidgetsVisibility(isWarrantyVisible,
            form.getWidgetByName(COL_WARRANTY + WIDGET_LABEL_NAME, false), warrantyValidToDate);

        setWidgetsVisibility(isWarrantyVisible, form.getWidgetByName(
            COL_WARRANTY_TYPE + WIDGET_LABEL_NAME, false), widgetTypeSelector);

        if (widgetTypeSelector != null) {
          widgetTypeSelector.setNullable(!isWarrantyVisible);
          widgetTypeSelector.setValue(Global.getParameterRelation(PRM_DEFAULT_WARRANTY_TYPE), true);
        }
      } else {
        newRow.setValue(getDataIndex(COL_SHOW_CUSTOMER), Boolean.TRUE);
        form.refreshBySource(COL_SHOW_CUSTOMER);
        updateTermVisibility(form, newRow);
      }
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new MaintenanceCommentForm(serviceMaintenance);
  }

  private static void setWidgetsVisibility(boolean visible, Widget... widget) {
    for (Widget element : widget) {
      if (element != null) {
        element.setVisible(visible);
      }
    }
  }

  private void updateTermVisibility(FormView form, IsRow row) {
    boolean isStateComment = row.isTrue(getDataIndex(COL_STATE_COMMENT));
    setWidgetsVisibility(BeeUtils.isTrue(isStateComment),
        form.getWidgetBySource(COL_TERM), form.getWidgetByName(COL_TERM + WIDGET_LABEL_NAME));
  }
}