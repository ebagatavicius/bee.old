package com.butent.bee.client.modules.service;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class PrintServiceDefect extends AbstractFormInterceptor {

  private static final String STYLE_PREFIX = ServiceKeeper.STYLE_PREFIX + "print-defect-";

  private static final String STYLE_ITEMS_HEADER = STYLE_PREFIX + "items-header";
  private static final String STYLE_ITEMS_BODY = STYLE_PREFIX + "items-body";
  private static final String STYLE_ITEMS_FOOTER = STYLE_PREFIX + "items-footer";

  private static final String STYLE_ITEM_PREFIX = STYLE_PREFIX + "item-";
  private static final String STYLE_ITEM_ORDINAL = STYLE_ITEM_PREFIX + "ordinal";
  private static final String STYLE_ITEM_NAME = STYLE_ITEM_PREFIX + "name";
  private static final String STYLE_ITEM_ARTICLE = STYLE_ITEM_PREFIX + "article";
  private static final String STYLE_ITEM_UNIT = STYLE_ITEM_PREFIX + "unit";
  private static final String STYLE_ITEM_QUANTITY = STYLE_ITEM_PREFIX + "quantity";
  private static final String STYLE_ITEM_PRICE = STYLE_ITEM_PREFIX + "price";

  private static final String STYLE_ITEMS_TOTAL_LABEL = STYLE_PREFIX + "items-total-label";
  private static final String STYLE_ITEMS_TOTAL_VALUE = STYLE_PREFIX + "items-total-value";

  private static final int COL_ORDINAL = 0;
  private static final int COL_NAME = 1;
  private static final int COL_ARTICLE = 2;
  private static final int COL_UNIT = 3;
  private static final int COL_QUANTITY = 4;
  private static final int COL_PRICE = 5;

  private static void getCompanyInfo(FormView form, IsRow row, String source) {
    Long company = row.getLong(form.getDataIndex(source));
    Widget widget = form.getWidgetByName(source);

    if (DataUtils.isId(company) && widget != null) {
      ClassifierUtils.getCompanyInfo(company, widget);
    }
  }

  private final List<? extends IsRow> items;

  PrintServiceDefect(List<? extends IsRow> items) {
    this.items = items;
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    if (form != null && row != null) {
      getCompanyInfo(form, row, COL_DEFECT_SUPPLIER);
      getCompanyInfo(form, row, COL_SERVICE_CUSTOMER);

      Widget widget = form.getWidgetByName(VIEW_SERVICE_DEFECT_ITEMS);

      if (widget instanceof HtmlTable && !BeeUtils.isEmpty(items)) {
        int index = form.getDataIndex(AdministrationConstants.ALS_CURRENCY_NAME);
        String currencyName = row.getString(index);

        renderItems((HtmlTable) widget, currencyName);
      }
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return this;
  }

  private void renderItems(HtmlTable table, String currencyName) {
    if (!table.isEmpty()) {
      table.clear();
    }

    int r = 0;

    table.setText(r, COL_ORDINAL, Localized.getConstants().ordinal(),
        STYLE_ITEMS_HEADER, STYLE_ITEM_ORDINAL);
    table.setText(r, COL_NAME, Localized.getConstants().svcDefectPrintItemLabel(),
        STYLE_ITEMS_HEADER, STYLE_ITEM_NAME);
    table.setText(r, COL_ARTICLE, Localized.getConstants().article(),
        STYLE_ITEMS_HEADER, STYLE_ITEM_ARTICLE);
    table.setText(r, COL_UNIT, Localized.getConstants().unit(),
        STYLE_ITEMS_HEADER, STYLE_ITEM_UNIT);
    table.setText(r, COL_QUANTITY, Localized.getConstants().quantity(),
        STYLE_ITEMS_HEADER, STYLE_ITEM_QUANTITY);
    table.setText(r, COL_PRICE, BeeUtils.joinWords(Localized.getConstants().price(), currencyName),
        STYLE_ITEMS_HEADER, STYLE_ITEM_PRICE);

    int nameIndex = Data.getColumnIndex(VIEW_SERVICE_DEFECT_ITEMS, ALS_DEFECT_ITEM_NAME);
    int articleIndex = Data.getColumnIndex(VIEW_SERVICE_DEFECT_ITEMS,
        ClassifierConstants.COL_ITEM_ARTICLE);
    int unitIndex = Data.getColumnIndex(VIEW_SERVICE_DEFECT_ITEMS, ALS_DEFECT_UNIT_NAME);
    int quantityIndex = Data.getColumnIndex(VIEW_SERVICE_DEFECT_ITEMS,
        TradeConstants.COL_TRADE_ITEM_QUANTITY);
    int priceIndex = Data.getColumnIndex(VIEW_SERVICE_DEFECT_ITEMS,
        TradeConstants.COL_TRADE_ITEM_PRICE);

    NumberFormat priceFormat = Format.getDefaultCurrencyFormat();

    double total = BeeConst.DOUBLE_ZERO;

    for (IsRow item : items) {
      r++;

      table.setValue(r, COL_ORDINAL, r, STYLE_ITEMS_BODY, STYLE_ITEM_ORDINAL);

      table.setText(r, COL_NAME, item.getString(nameIndex),
          STYLE_ITEMS_BODY, STYLE_ITEM_NAME);
      table.setText(r, COL_ARTICLE, item.getString(articleIndex),
          STYLE_ITEMS_BODY, STYLE_ITEM_ARTICLE);

      table.setText(r, COL_UNIT, item.getString(unitIndex),
          STYLE_ITEMS_BODY, STYLE_ITEM_UNIT);
      table.setText(r, COL_QUANTITY, item.getString(quantityIndex),
          STYLE_ITEMS_BODY, STYLE_ITEM_QUANTITY);

      double price = BeeUtils.unbox(item.getDouble(priceIndex));
      table.setText(r, COL_PRICE, priceFormat.format(price), STYLE_ITEMS_BODY, STYLE_ITEM_PRICE);

      total += BeeUtils.unbox(item.getDouble(quantityIndex)) * price;
    }

    r++;

    table.setText(r, COL_QUANTITY, Localized.getConstants().total(),
        STYLE_ITEMS_FOOTER, STYLE_ITEMS_TOTAL_LABEL);

    NumberFormat totalFormat = Format.getDefaultCurrencyFormat();
    table.setText(r, COL_PRICE, totalFormat.format(total),
        STYLE_ITEMS_FOOTER, STYLE_ITEMS_TOTAL_VALUE);
  }
}
