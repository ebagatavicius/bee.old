package com.butent.bee.client.modules.service;

import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.orders.OrdersConstants.VIEW_ORDER_CHILD_INVOICES;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

final class InvoiceBuilder {

  private static final class InvoiceInterceptor extends AbstractFormInterceptor {

    private Long mainItem;

    private InvoiceInterceptor() {
      super();
    }

    @Override
    public FormInterceptor getInstance() {
      return this;
    }

    @Override
    public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
      setMainItem(BeeUtils.toLongOrNull(getActiveRow().getProperty(PROP_MAIN_ITEM)));
    }

    private Long getMainItem() {
      return mainItem;
    }

    private void setMainItem(Long mainItem) {
      this.mainItem = mainItem;
    }
  }

  private static final String STYLE_PREFIX = ServiceKeeper.STYLE_PREFIX + "invoice-builder-";

  static void start(IdentifiableWidget sourceWidget) {
    if (sourceWidget == null) {
      return;
    }

    final FormView form = ViewHelper.getForm(sourceWidget.asWidget());
    if (form == null || !form.isEnabled()
        || !BeeUtils.inList(form.getViewName(), VIEW_SERVICE_OBJECTS, COL_SERVICE_MAINTENANCE)) {
      return;
    }

    final long objId;
    long maintenanceId = 0;

    if (BeeUtils.equals(form.getViewName(), VIEW_SERVICE_OBJECTS)) {
      objId = form.getActiveRowId();

    } else {
      objId = form.getActiveRow().getLong(
          Data.getColumnIndex(form.getViewName(), COL_SERVICE_OBJECT));
      maintenanceId = form.getActiveRowId();
    }

    if (!DataUtils.isId(objId)) {
      return;
    }

    Filter filter;

    if (BeeUtils.equals(form.getViewName(), VIEW_SERVICE_OBJECTS)
        || !DataUtils.isId(maintenanceId)) {
      filter = Filter.and(Filter.equals(COL_SERVICE_OBJECT, objId),
          Filter.isNull(COL_MAINTENANCE_INVOICE));

    } else {
      filter = Filter.and(Filter.equals(COL_SERVICE_OBJECT, objId),
          Filter.isNull(COL_MAINTENANCE_INVOICE),
          Filter.equals(COL_SERVICE_MAINTENANCE, maintenanceId));
    }

    Queries.getRowSet(VIEW_MAINTENANCE, null, filter, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          form.notifyInfo(Localized.dictionary().noData());

        } else {
          ServiceHelper.selectMaintenanceItems(objId, result,
              Localized.dictionary().trdNewInvoice(), Localized.dictionary().createInvoice(),
              STYLE_PREFIX, new BiConsumer<Long, BeeRowSet>() {
                @Override
                public void accept(Long t, BeeRowSet u) {
                  buildHeader(t, u);
                }
              });
        }
      }
    });
  }

  private static void buildHeader(long objId, final BeeRowSet items) {
    Queries.getRow(VIEW_SERVICE_OBJECTS, objId, new RowCallback() {
      @Override
      public void onSuccess(BeeRow objRow) {
        DataInfo invInfo = Data.getDataInfo(VIEW_SERVICE_INVOICES);
        BeeRow invRow = RowFactory.createEmptyRow(invInfo, true);

        invRow.setValue(invInfo.getColumnIndex(TradeConstants.COL_TRADE_KIND), 1);

        Long customer = Data.getLong(VIEW_SERVICE_OBJECTS, objRow, COL_SERVICE_CUSTOMER);
        if (DataUtils.isId(customer)) {
          invRow.setValue(invInfo.getColumnIndex(TradeConstants.COL_TRADE_CUSTOMER), customer);
          invRow.setValue(invInfo.getColumnIndex(TradeConstants.ALS_CUSTOMER_NAME),
              Data.getString(VIEW_SERVICE_OBJECTS, objRow, ALS_SERVICE_CUSTOMER_NAME));
        }

        UserData userData = BeeKeeper.getUser().getUserData();
        if (userData != null) {
          invRow.setValue(invInfo.getColumnIndex(TradeConstants.COL_TRADE_MANAGER),
              userData.getUserId());
          RelationUtils.setUserFields(invInfo, invRow, TradeConstants.COL_TRADE_MANAGER, userData);

          invRow.setValue(invInfo.getColumnIndex(TradeConstants.COL_TRADE_SUPPLIER),
              userData.getCompany());
          invRow.setValue(invInfo.getColumnIndex(TradeConstants.ALS_SUPPLIER_NAME),
              userData.getCompanyName());
        }

        Integer days = Data.getInteger(VIEW_SERVICE_OBJECTS, objRow,
            ClassifierConstants.COL_COMPANY_CREDIT_DAYS);
        if (BeeUtils.isPositive(days)) {
          invRow.setValue(invInfo.getColumnIndex(TradeConstants.COL_TRADE_TERM),
              TimeUtils.nextDay(TimeUtils.today(), days));
        }

        for (BeeRow row : items) {
          Long currency = row.getLong(items.getColumnIndex(AdministrationConstants.COL_CURRENCY));

          if (DataUtils.isId(currency)) {
            invRow.setValue(invInfo.getColumnIndex(AdministrationConstants.COL_CURRENCY), currency);
            invRow.setValue(invInfo.getColumnIndex(AdministrationConstants.ALS_CURRENCY_NAME),
                row.getString(items.getColumnIndex(AdministrationConstants.ALS_CURRENCY_NAME)));
            break;
          }
        }

        final InvoiceInterceptor interceptor = new InvoiceInterceptor();

        RowFactory.createRow("NewServiceInvoice", null, invInfo, invRow, Modality.ENABLED, null,
            interceptor, null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                ParameterList params = ServiceKeeper.createArgs(SVC_CREATE_INVOICE_ITEMS);

                params.addQueryItem(COL_MAINTENANCE_INVOICE, result.getId());

                Long currency = Data.getLong(VIEW_SERVICE_INVOICES, result,
                    AdministrationConstants.COL_CURRENCY);
                if (DataUtils.isId(currency)) {
                  params.addQueryItem(AdministrationConstants.COL_CURRENCY, currency);
                }

                if (DataUtils.isId(interceptor.getMainItem())) {
                  params.addQueryItem(PROP_MAIN_ITEM, interceptor.getMainItem());
                }

                List<Long> ids = new ArrayList<>();
                for (IsRow item : items) {
                  ids.add(item.getId());
                }

                params.addDataItem(VIEW_MAINTENANCE, DataUtils.buildIdList(ids));

                final long invId = result.getId();

                BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
                  @Override
                  public void onResponse(ResponseObject response) {
                    response.notify(BeeKeeper.getScreen());

                    if (!response.hasErrors()) {
                      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_MAINTENANCE);
                      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ORDER_CHILD_INVOICES);

                      RowEditor.open(VIEW_ORDER_CHILD_INVOICES, invId, Opener.MODAL);
                    }
                  }
                });
              }
            });
      }
    });
  }

  private InvoiceBuilder() {
  }
}
