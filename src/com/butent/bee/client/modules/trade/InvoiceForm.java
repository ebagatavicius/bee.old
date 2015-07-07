package com.butent.bee.client.modules.trade;

import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.Objects;

public class InvoiceForm extends PrintFormInterceptor {

  Pair<Boolean, Long> mainItem;

  public InvoiceForm(Pair<Boolean, Long> mainItem) {
    this.mainItem = mainItem;
  }

  @Override
  public void onReadyForInsert(final HasHandlers listener, final ReadyForInsertEvent event) {
    if (mainItem != null) {
      event.consume();

      Relation relation = Relation.create(TBL_ITEMS, Collections.singletonList(COL_ITEM_NAME));
      relation.disableNewRow();
      final UnboundSelector item = UnboundSelector.create(relation);

      Global.inputWidget(Localized.getConstants().transportMainItemCaption(), item,
          new InputCallback() {
            @Override
            public String getErrorMessage() {
              if (mainItem.getA() && !DataUtils.isId(item.getRelatedId())) {
                return Localized.getConstants().valueRequired();
              }
              return super.getErrorMessage();
            }

            @Override
            public void onSuccess() {
              mainItem.setB(item.getRelatedId());
              listener.fireEvent(event);
            }
          });
      return;
    }
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
