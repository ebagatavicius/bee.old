package com.butent.bee.client.modules.service;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.SimpleCheckBox;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.Totalizer;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

final class ServiceHelper {

  static void selectMaintenanceItems(final long objId, final BeeRowSet data,
      String dialogCaption, String selectCaption, String stylePrefix,
      final BiConsumer<Long, BeeRowSet> consumer) {

    final HtmlTable table = new HtmlTable(stylePrefix + "table");
    int r = 0;

    int dateIndex = data.getColumnIndex(COL_MAINTENANCE_DATE);
    int itemNameIndex = data.getColumnIndex(ALS_MAINTENANCE_ITEM_NAME);
    int quantityIndex = data.getColumnIndex(TradeConstants.COL_TRADE_ITEM_QUANTITY);
    int priceIndex = data.getColumnIndex(TradeConstants.COL_TRADE_ITEM_PRICE);
    int currencyNameIndex = data.getColumnIndex(AdministrationConstants.ALS_CURRENCY_NAME);

    Totalizer totalizer = new Totalizer(data.getColumns());

    NumberFormat priceFormat = Format.getDefaultMoneyFormat();
    NumberFormat amountFormat = Format.getDefaultMoneyFormat();

    for (IsRow row : data) {
      DateTime date = row.getDateTime(dateIndex);
      String itemName = row.getString(itemNameIndex);
      Double quantity = row.getDouble(quantityIndex);
      Double price = row.getDouble(priceIndex);
      String currencyName = row.getString(currencyNameIndex);

      int c = 0;

      SimpleCheckBox checkBox = new SimpleCheckBox(true);
      table.setWidgetAndStyle(r, c++, checkBox, stylePrefix + "check");

      Label dateLabel = new Label();
      if (date != null) {
        dateLabel.setText(date.toCompactString());
      }
      table.setWidgetAndStyle(r, c++, dateLabel, stylePrefix + "date");

      Label itemLabel = new Label(itemName);
      table.setWidgetAndStyle(r, c++, itemLabel, stylePrefix + "item");

      Label quantityLabel = new Label();
      if (quantity != null) {
        quantityLabel.setText(BeeUtils.toString(quantity));
      }
      table.setWidgetAndStyle(r, c++, quantityLabel, stylePrefix + "quantity");

      Label priceLabel = new Label();
      if (price != null) {
        priceLabel.setText(priceFormat.format(price));
      }
      table.setWidgetAndStyle(r, c++, priceLabel, stylePrefix + "price");

      Label currencyLabel = new Label(currencyName);
      table.setWidgetAndStyle(r, c++, currencyLabel, stylePrefix + "currency");

      Label amountLabel = new Label();
      if (quantity != null && price != null) {
        Double amount = totalizer.getTotal(row);
        if (amount != null) {
          amountLabel.setText(amountFormat.format(amount));
        }
      }
      table.setWidgetAndStyle(r, c++, amountLabel, stylePrefix + "amount");

      r++;
    }

    Simple wrapper = new Simple(table);
    wrapper.addStyleName(stylePrefix + "wrapper");

    Flow panel = new Flow(stylePrefix + "panel");
    panel.add(wrapper);

    Flow commands = new Flow(stylePrefix + "commands");

    final DialogBox dialog = DialogBox.create(dialogCaption, stylePrefix + "dialog");

    Button select = new Button(selectCaption);
    select.addStyleName(stylePrefix + "select");
    select.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        List<BeeRow> selectedRows = new ArrayList<>();

        for (int i = 0; i < data.getNumberOfRows(); i++) {
          Widget widget = table.getWidget(i, 0);
          if (widget instanceof HasCheckedness && ((HasCheckedness) widget).isChecked()) {
            selectedRows.add(data.getRow(i));
          }
        }

        if (selectedRows.isEmpty()) {
          BeeKeeper.getScreen().notifyWarning(Localized.getConstants().selectAtLeastOneRow());

        } else {
          dialog.close();

          BeeRowSet result = new BeeRowSet(data.getViewName(), data.getColumns(), selectedRows);
          consumer.accept(objId, result);
        }
      }
    });

    Button cancel = new Button(Localized.getConstants().cancel());
    cancel.addStyleName(stylePrefix + "cancel");
    cancel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        dialog.close();
      }
    });

    commands.add(select);
    commands.add(cancel);

    panel.add(commands);

    dialog.setHideOnEscape(true);
    dialog.setAnimationEnabled(true);

    dialog.setWidget(panel);
    dialog.center();
  }

  private ServiceHelper() {
  }
}
