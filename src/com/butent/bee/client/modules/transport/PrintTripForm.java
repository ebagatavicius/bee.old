package com.butent.bee.client.modules.transport;

import com.google.common.collect.HashMultimap;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PrintTripForm extends AbstractFormInterceptor {

  @Override
  public FormInterceptor getInstance() {
    return null;
  }

  @Override
  public void onSetActiveRow(IsRow row) {
    final FormView form = getFormView();
    ParameterList args = TransportHandler.createArgs(SVC_GET_TRIP_INFO);
    args.addDataItem(COL_TRIP, row.getId());

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            response.notify(form);

            if (response.hasErrors()) {
              return;
            }
            final Map<String, String> pack =
                Codec.deserializeLinkedHashMap(response.getResponseAsString());

            // DRIVERS
            final Map<Long, String> drivers = new LinkedHashMap<>();
            Long mainDriver = null;

            for (SimpleRowSet.SimpleRow driver : SimpleRowSet.restore(pack.get(TBL_TRIP_DRIVERS))) {
              if (!DataUtils.isId(mainDriver)) {
                mainDriver = driver.getLong(COL_MAIN_DRIVER);
              }
              drivers.put(driver.getLong(COL_DRIVER),
                  BeeUtils.joinWords(driver.getValue(COL_FIRST_NAME),
                      driver.getValue(COL_LAST_NAME)));
            }
            List<String> allDrivers = new ArrayList<>();

            for (Long driver : drivers.keySet()) {
              if (!Objects.equals(driver, mainDriver)) {
                allDrivers.add(drivers.get(driver));
              }
            }
            for (Map.Entry<String, Widget> entry : form.getNamedWidgets().entrySet()) {
              String name = entry.getKey();

              if (BeeUtils.isPrefix(name, TBL_TRIP_DRIVERS)
                  && !BeeUtils.isSuffix(name, "Container")) {

                String idx = BeeUtils.removePrefix(name, TBL_TRIP_DRIVERS);

                String value = BeeUtils.isNonNegativeInt(idx)
                    ? BeeUtils.getQuietly(allDrivers, BeeUtils.toInt(idx))
                    : BeeUtils.joinItems(allDrivers);

                entry.getValue().getElement().setInnerText(value);
                Widget containerWidget = form.getWidgetByName(name + "Container", false);

                if (Objects.nonNull(containerWidget)) {
                  StyleUtils.setVisible(containerWidget, !BeeUtils.isEmpty(value));
                }
              }
            }
            // FUEL
            Widget fuelWidget = form.getWidgetByName(COL_FUEL, false);

            if (fuelWidget != null) {
              double before = BeeUtils.unbox(form.getDoubleValue("FuelBefore"));
              double after = BeeUtils.unbox(form.getDoubleValue("FuelAfter"));
              double fuel = before + BeeUtils.toDouble(pack.get(COL_FUEL)) - after;
              fuelWidget.getElement()
                  .setInnerText(BeeUtils.toString(BeeUtils.round(fuel, 2)));

              fuelWidget = form.getWidgetByName(TBL_TRIP_FUEL_CONSUMPTIONS, false);

              if (fuelWidget != null) {
                before = BeeUtils.unbox(form.getDoubleValue("SpeedometerBefore"));
                after = BeeUtils.unbox(form.getDoubleValue("SpeedometerAfter"));
                fuelWidget.getElement().setInnerText(after == before ? null
                    : BeeUtils.toString(BeeUtils.round(fuel / (after - before) * 100, 2)));
              }
            }
            fuelWidget = form.getWidgetByName(TBL_TRIP_FUEL_COSTS, false);

            if (fuelWidget != null) {
              fuelWidget.getElement()
                  .setInnerText(BeeUtils.round(pack.get(TBL_TRIP_FUEL_COSTS), 2));
            }
            // COSTS
            final Set<Long> dailyCostsItems = new HashSet<>();

            for (String item : Codec.beeDeserializeCollection(pack.get(COL_DAILY_COSTS_ITEM))) {
              dailyCostsItems.add(BeeUtils.toLong(item));
            }
            Map<String, Integer> index = new HashMap<>();
            HtmlTable driverCosts = new HtmlTable();
            driverCosts.getRowFormatter().addStyleName(0, "header");
            int c = 0;

            for (String name : new String[] {
                COL_NUMBER, COL_COSTS_DATE, COL_COSTS_ITEM, COL_DRIVER, COL_COSTS_QUANTITY,
                COL_UNIT, COL_COSTS_PRICE, COL_AMOUNT}) {

              index.put(name, c);
              driverCosts.setColumnCellClasses(c, name);
              driverCosts.setWidget(0, c, new Label());
              c++;
            }
            final HashMultimap<Long, SimpleRowSet.SimpleRow> driverInfo = HashMultimap.create();
            Map<String, Double> otherInfo = new HashMap<>();

            for (SimpleRowSet.SimpleRow cost : SimpleRowSet.restore(pack.get(TBL_TRIP_COSTS))) {
              String itemName = cost.getValue(COL_ITEM_NAME);
              double amount = BeeUtils.round(BeeUtils.unbox(cost.getDouble(COL_AMOUNT)), 2);

              if (!BeeUtils.unbox(cost.getBoolean(COL_PAYMENT_CASH))) {
                otherInfo.put(itemName, BeeUtils.round(BeeUtils.unbox(otherInfo.get(itemName))
                    + amount, 2));
                continue;
              }
              Long driver = BeeUtils.nvl(cost.getLong(COL_DRIVER), mainDriver);
              double quantity = BeeUtils.unbox(cost.getDouble(COL_COSTS_QUANTITY));
              driverInfo.put(driver, cost);

              int r = driverCosts.getRowCount();
              driverCosts.setText(r, index.get(COL_NUMBER), cost.getValue(COL_NUMBER));
              driverCosts.setText(r, index.get(COL_COSTS_DATE),
                  Format.renderDate(cost.getDateTime(COL_COSTS_DATE)));
              driverCosts.setText(r, index.get(COL_COSTS_ITEM), BeeUtils.joinWords(itemName,
                  dailyCostsItems.contains(cost.getLong(COL_COSTS_ITEM))
                      ? BeeUtils.parenthesize(cost.getValue(COL_COSTS_COUNTRY)) : null));
              driverCosts.setText(r, index.get(COL_DRIVER), drivers.get(driver));
              driverCosts.setText(r, index.get(COL_COSTS_QUANTITY), BeeUtils.toString(quantity));
              driverCosts.setText(r, index.get(COL_UNIT), cost.getValue(COL_UNIT));
              driverCosts.setText(r, index.get(COL_COSTS_PRICE), quantity > 0
                  ? BeeUtils.toString(BeeUtils.round(amount / quantity, 2)) : null);
              driverCosts.setText(r, index.get(COL_AMOUNT), BeeUtils.toString(amount));
            }
            double driverTotal = 0;
            double otherTotal = 0;
            Widget driverCostsWidget = form.getWidgetByName("DriverCosts", false);

            if (driverCostsWidget != null) {
              if (driverInfo.size() > 0) {
                int r = driverCosts.getRowCount();

                for (Long driver : driverInfo.keySet()) {
                  double total = 0;

                  for (SimpleRowSet.SimpleRow cost : driverInfo.get(driver)) {
                    total += BeeUtils.round(BeeUtils.unbox(cost.getDouble(COL_AMOUNT)), 2);
                  }
                  driverCosts.setText(r, index.get(COL_COSTS_ITEM),
                      Localized.dictionary().total());
                  driverCosts.setText(r, index.get(COL_DRIVER), drivers.get(driver));
                  driverCosts.setText(r, index.get(COL_AMOUNT),
                      BeeUtils.toString(BeeUtils.round(total, 2)));
                  r++;
                  driverTotal += total;
                }
                driverTotal = BeeUtils.round(driverTotal, 2);
                driverCosts.getRowFormatter().addStyleName(r, "footer");
                driverCosts.setText(r, index.get(COL_COSTS_ITEM),
                    BeeUtils.joinWords(Localized.dictionary().totalOf(),
                        BeeUtils.parenthesize(pack.get(AdministrationConstants.COL_CURRENCY))));
                driverCosts.setText(r, index.get(COL_AMOUNT), BeeUtils.toString(driverTotal));
              }
              Widget driverCostsTotal = form.getWidgetByName("DriverCostsTotal", false);

              if (driverCostsTotal != null) {
                driverCostsTotal.getElement().setInnerText(BeeUtils.toString(driverTotal));
              }
              driverCostsWidget.getElement().setInnerHTML(driverCosts.toString());
            }
            Widget otherCostsWidget = form.getWidgetByName("OtherCosts", false);

            if (otherCostsWidget != null) {
              HtmlTable otherCosts = new HtmlTable();
              otherCosts.getRowFormatter().addStyleName(0, "header");
              otherCosts.setColumnCellClasses(0, COL_COSTS_ITEM);
              otherCosts.setColumnCellClasses(1, COL_AMOUNT);
              otherCosts.setWidget(0, 0, new Label());
              otherCosts.setWidget(0, 1, new Label());

              if (otherInfo.size() > 0) {
                int r = otherCosts.getRowCount();

                for (Map.Entry<String, Double> entry : otherInfo.entrySet()) {
                  double amount = entry.getValue();
                  otherCosts.setText(r, 0, entry.getKey());
                  otherCosts.setText(r, 1, BeeUtils.toString(amount));
                  otherTotal += amount;
                  r++;
                }
                otherTotal = BeeUtils.round(otherTotal, 2);
                otherCosts.getRowFormatter().addStyleName(r, "footer");
                otherCosts.setText(r, 0, BeeUtils.joinWords(Localized.dictionary().totalOf(),
                    BeeUtils.parenthesize(pack.get(AdministrationConstants.COL_CURRENCY))));
                otherCosts.setText(r, 1, BeeUtils.toString(otherTotal));

                Widget otherCostsTotal = form.getWidgetByName("OtherCostsTotal", false);

                if (otherCostsTotal != null) {
                  otherCostsTotal.getElement().setInnerText(BeeUtils.toString(otherTotal));
                }
              }
              otherCostsWidget.getElement().setInnerHTML(otherCosts.toString());
            }
            Widget costsTotal = form.getWidgetByName("CostsTotal", false);

            if (costsTotal != null) {
              costsTotal.getElement()
                  .setInnerText(BeeUtils.toString(BeeUtils.round(driverTotal + otherTotal, 2)));
            }
            // ADVANCES
            Widget advancesWidget = form.getWidgetByName(TBL_DRIVER_ADVANCES, false);

            if (advancesWidget != null) {
              index = new HashMap<>();
              HtmlTable driverAdvances = new HtmlTable();
              driverAdvances.getRowFormatter().addStyleName(0, "header");
              c = 0;
              String remainder = "Remainder";

              for (String name : new String[] {
                  COL_COSTS_ITEM, COL_DATE, COL_AMOUNT, remainder}) {

                index.put(name, c);
                driverAdvances.setColumnCellClasses(c, name);
                driverAdvances.setWidget(0, c, new Label());
                c++;
              }
              int r = 1;

              for (Long driver : drivers.keySet()) {
                double total = 0;

                driverAdvances.getRowFormatter().addStyleName(r, "header");
                driverAdvances.getCellFormatter()
                    .setColSpan(r, 0, BeeUtils.max(index.values()) + 1);
                driverAdvances.setText(r, 0, drivers.get(driver));
                r++;

                for (SimpleRowSet.SimpleRow advance
                    : SimpleRowSet.restore(pack.get(TBL_DRIVER_ADVANCES))) {

                  if (!Objects.equals(advance.getLong(COL_DRIVER), driver)) {
                    continue;
                  }
                  double amount = BeeUtils.round(BeeUtils.unbox(advance.getDouble(COL_AMOUNT)), 2);
                  total -= amount;

                  driverAdvances.setText(r, index.get(COL_COSTS_ITEM),
                      Localized.dictionary().advance());
                  driverAdvances.setText(r, index.get(COL_DATE),
                      Format.renderDate(advance.getDate(COL_DATE)));
                  driverAdvances.setText(r, index.get(COL_AMOUNT), BeeUtils.toString(amount));
                  driverAdvances.setText(r, index.get(remainder),
                      BeeUtils.toString(BeeUtils.round(total, 2)));
                  r++;
                }
                double daily = 0;
                double other = 0;

                for (SimpleRowSet.SimpleRow cost : driverInfo.get(driver)) {
                  double amount = BeeUtils.round(BeeUtils.unbox(cost.getDouble(COL_AMOUNT)), 2);

                  if (dailyCostsItems.contains(cost.getLong(COL_COSTS_ITEM))) {
                    daily += amount;
                  } else {
                    other += amount;
                  }
                }
                total += daily;
                driverAdvances.setText(r, index.get(COL_COSTS_ITEM),
                    Localized.dictionary().trDailyCosts());
                driverAdvances.setText(r, index.get(COL_AMOUNT),
                    BeeUtils.toString(BeeUtils.round(daily * (-1), 2)));
                driverAdvances.setText(r, index.get(remainder),
                    BeeUtils.toString(BeeUtils.round(total, 2)));
                r++;
                total += other;
                driverAdvances.setText(r, index.get(COL_COSTS_ITEM),
                    Localized.dictionary().trOtherCosts());
                driverAdvances.setText(r, index.get(COL_AMOUNT),
                    BeeUtils.toString(BeeUtils.round(other * (-1), 2)));
                driverAdvances.setText(r, index.get(remainder),
                    BeeUtils.toString(BeeUtils.round(total, 2)));
                r++;
                driverAdvances.getRowFormatter().addStyleName(r, "footer");
                driverAdvances.setText(r, index.get(COL_COSTS_ITEM),
                    BeeUtils.joinWords(Localized.dictionary().total(),
                        BeeUtils.parenthesize(pack.get(AdministrationConstants.COL_CURRENCY))));
                driverAdvances.setText(r, index.get(remainder),
                    BeeUtils.toString(BeeUtils.round(total, 2)));
                r++;
              }
              advancesWidget.getElement().setInnerHTML(driverAdvances.toString());
            }
            // DAILY COSTS
            final Widget currentDriverWidget = form.getWidgetByName("CurrentDriver", false);

            if (currentDriverWidget != null) {
              ChoiceCallback choice = value -> {
                Long driver = BeeUtils.getQuietly(new ArrayList<>(drivers.keySet()), value);

                if (!DataUtils.isId(driver)) {
                  return;
                }
                currentDriverWidget.getElement().setInnerText(drivers.get(driver));
                double daily = 0;
                HtmlTable dailyCosts = new HtmlTable();
                int r = 0;

                for (SimpleRowSet.SimpleRow cost : driverInfo.get(driver)) {
                  if (dailyCostsItems.contains(cost.getLong(COL_COSTS_ITEM))) {
                    dailyCosts.setText(r, 0, BeeUtils.joinWords(cost.getValue(COL_ITEM_NAME),
                        BeeUtils.parenthesize(cost.getValue(COL_COSTS_COUNTRY))));
                    dailyCosts.setText(r, 1, BeeUtils.joinWords(cost.getValue(COL_COSTS_QUANTITY),
                        cost.getValue(COL_UNIT)));
                    daily += BeeUtils.round(BeeUtils.unbox(cost.getDouble(COL_AMOUNT)), 2);
                    r++;
                  }
                }
                Widget dailyWidget = form.getWidgetByName("DailyCosts", false);

                if (dailyWidget != null) {
                  dailyWidget.getElement().setInnerHTML(dailyCosts.toString());
                }
                Widget dailyTotal = form.getWidgetByName("DailyCostsTotal", false);

                if (dailyTotal != null) {
                  dailyTotal.getElement().setInnerText(BeeUtils
                      .joinWords(BeeUtils.toString(BeeUtils.round(daily, 2)),
                          pack.get(AdministrationConstants.COL_CURRENCY)));
                }

              };
              if (drivers.size() > 1) {
                Global.choice(Localized.dictionary().drivers(), null,
                    new ArrayList<>(drivers.values()), choice);
              } else {
                choice.onSuccess(0);
              }
            }
            // USER
            Widget userWidget = form.getWidgetByName(AdministrationConstants.COL_USER, false);

            if (userWidget != null) {
              userWidget.getElement().setInnerText(BeeKeeper.getUser().getUserSign());
            }
          }
        }

    );
    super.onSetActiveRow(row);
  }
}
