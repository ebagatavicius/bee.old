package com.butent.bee.client.modules.trade;

import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class InvoiceForm extends InvoiceERPForm implements SelectorEvent.Handler {

  Holder<Long> mainItem;

  public InvoiceForm(Holder<Long> mainItem) {
    this.mainItem = mainItem;
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    super.afterCreateEditableWidget(editableWidget, widget);

    if (BeeUtils.same(editableWidget.getColumnId(), COL_TRADE_OPERATION)
        && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(this);
    }
  }

  @Override
  public void onReadyForInsert(final HasHandlers listener, final ReadyForInsertEvent event) {
    if (mainItem != null) {
      event.consume();

      Relation relation = Relation.create(TBL_ITEMS, Collections.singletonList(COL_ITEM_NAME));
      relation.disableNewRow();
      final UnboundSelector item = UnboundSelector.create(relation);

      Global.inputWidget(Localized.dictionary().transportMainItemCaption(), item,
          new InputCallback() {
            @Override
            public String getErrorMessage() {
              if (!DataUtils.isId(item.getRelatedId())) {
                return Localized.dictionary().valueRequired();
              }
              return InputCallback.super.getErrorMessage();
            }

            @Override
            public void onSuccess() {
              mainItem.set(item.getRelatedId());
              listener.fireEvent(event);
            }
          });
      return;
    }
  }

  @Override
  public void getERPStocks(Long id) {
  }

  @Override
  public FormInterceptor getInstance() {
    return new InvoiceForm(mainItem);
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new PrintInvoiceInterceptor();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    String viewName = event.getRelatedViewName();
    IsRow relatedRow = event.getRelatedRow();

    if (relatedRow != null && event.isChanged()
        && BeeUtils.same(Data.getViewTable(viewName), TBL_TRADE_OPERATIONS)) {
      OperationType type = EnumUtils.getEnumByIndex(OperationType.class,
          Data.getInteger(viewName, relatedRow, COL_OPERATION_TYPE));

      List<String> fields = new ArrayList<>();

      if (type != null) {
        if (type.consumesStock()) {
          fields.add(COL_TRADE_WAREHOUSE_FROM);
        }
        if (type.producesStock()) {
          fields.add(COL_OPERATION_WAREHOUSE_TO);
        }
      }

      for (String field : fields) {
        Long warehouse = Data.getLong(viewName, relatedRow, field);

        if (DataUtils.isId(warehouse)) {
          int idx = getDataIndex(field);
          int codeIdx = getDataIndex(field + "Code");

          if (!BeeConst.isUndef(idx) && !BeeConst.isUndef(codeIdx)) {
            getActiveRow().setValue(idx, warehouse);
            getActiveRow().setValue(codeIdx, Data.getString(viewName, relatedRow, field + "Code"));

            getFormView().refreshBySource(field);
          }
        }
      }
    }
  }

  @Override
  public void onEditEnd(EditEndEvent ev, Object source) {
    if (ev.valueChanged() && ev.getColumn() != null) {
      calculateTerm(ev.getRowValue(), ev.getColumn().getId(), ev.getNewValue());
    }
    super.onEditEnd(ev, source);
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    calculateTerm(newRow, COL_TRADE_DATE, newRow.getString(getDataIndex(COL_TRADE_DATE)));
    super.onStartNewRow(form, oldRow, newRow);
  }

  private void calculateTerm(final IsRow row, final String column, final String value) {
    if (!BeeUtils.inList(column, COL_TRADE_DATE, COL_TRADE_SUPPLIER, COL_TRADE_CUSTOMER,
        COL_SALE_PAYER)) {
      return;
    }
    final int termIdx = getDataIndex(COL_TRADE_TERM);

    if (BeeConst.isUndef(termIdx) || row == null) {
      return;
    }
    Long companyId = null;
    String creditColumn = null;

    switch (Data.getViewTable(getViewName())) {
      case TBL_PURCHASES:
        creditColumn = COL_COMPANY_SUPPLIER_DAYS;
        companyId = getValue(COL_TRADE_SUPPLIER, row, column, value);
        break;
      case TBL_SALES:
        creditColumn = COL_COMPANY_CREDIT_DAYS;
        companyId = BeeUtils.nvl(getValue(COL_SALE_PAYER, row, column, value),
            getValue(COL_TRADE_CUSTOMER, row, column, value));
        break;
      default:
        return;
    }
    RpcCallback<String> callback = new RpcCallback<String>() {
      @Override
      public void onSuccess(String days) {
        JustDate term = null;

        if (BeeUtils.isPositiveInt(days)) {
          Long millis = getValue(COL_TRADE_DATE, row, column, value);

          if (millis != null) {
            term = TimeUtils.nextDay(new DateTime(millis), BeeUtils.toInt(days));
          }
        }
        row.setValue(termIdx, term);
        getFormView().refreshBySource(COL_TRADE_TERM);
      }
    };
    if (DataUtils.isId(companyId)) {
      Queries.getValue(TBL_COMPANIES, companyId, creditColumn, callback);
    } else {
      callback.onSuccess(null);
    }
  }

  private Long getValue(String targetColumn, IsRow row, String column, String value) {
    return Objects.equals(targetColumn, column)
        ? BeeUtils.toLongOrNull(value) : row.getLong(getDataIndex(targetColumn));
  }
}
