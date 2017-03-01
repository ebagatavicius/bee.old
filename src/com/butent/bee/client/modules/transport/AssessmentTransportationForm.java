package com.butent.bee.client.modules.transport;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.ALS_COUNTRY_NAME;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AssessmentTransportationForm extends PrintFormInterceptor {

  private Widget totals;
  private Widget cargo;
  private Widget forwarderDetails;
  private Widget customerDetails;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, "Totals")) {
      totals = widget.asWidget();

    } else if (BeeUtils.same(name, COL_CARGO)) {
      cargo = widget.asWidget();
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    if (!BeeUtils.anyNotNull(totals, cargo)) {
      return;
    }
    Queries.getRowSet(TBL_ASSESSMENTS, null,
        Filter.in(COL_CARGO, TBL_CARGO_TRIPS, COL_CARGO, Filter.equals(COL_TRIP, row.getId())),
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            int cargoIdx = result.getColumnIndex(COL_CARGO);
            int colWeight = result.getColumnIndex("Weight");
            int colWeightUnit = result.getColumnIndex("WeightUnitName");
            Map<String, Double> weight = new HashMap<>();
            int colQuantity = result.getColumnIndex("Quantity");
            int colQuantityUnit = result.getColumnIndex("QuantityUnitName");
            Map<String, Double> quantity = new HashMap<>();
            int colVolume = result.getColumnIndex("Volume");
            int colVolumeUnit = result.getColumnIndex("VolumeUnitName");
            Map<String, Double> volume = new HashMap<>();

            Dictionary loc = Localized.dictionary();
            Map<Integer, Pair<Integer, String>> map = new HashMap<>();

            map.put(colWeight, Pair.of(0, loc.weight()));
            map.put(colQuantity, Pair.of(1, loc.trdQuantity()));
            map.put(colVolume, Pair.of(2, loc.volume()));

            TransportUtils.getCargoPlaces(Filter.any(COL_CARGO,
                result.getDistinctLongs(cargoIdx)), (loading, unloading) -> {
              HtmlTable cargoInfo = new HtmlTable();
              int c = 0;

              for (Pair<Integer, String> pair : map.values()) {
                cargoInfo.setHtml(c, pair.getA(), pair.getB());
              }
              cargoInfo.setText(c, 3, Data.getViewCaption(loading.getViewName()));
              cargoInfo.setText(c, 4, Data.getViewCaption(unloading.getViewName()));

              for (BeeRow r : result.getRows()) {
                c++;
                Double value = r.getDouble(colWeight);
                if (BeeUtils.isPositive(value)) {
                  String unit = r.getString(colWeightUnit);
                  weight.put(unit, BeeUtils.unbox(weight.get(unit)) + value);
                  cargoInfo.setHtml(c, map.get(colWeight).getA(),
                      BeeUtils.joinWords(value, unit));
                }
                value = r.getDouble(colQuantity);
                if (BeeUtils.isPositive(value)) {
                  String unit = r.getString(colQuantityUnit);
                  quantity.put(unit, BeeUtils.unbox(quantity.get(unit)) + value);
                  cargoInfo.setHtml(c, map.get(colQuantity).getA(),
                      BeeUtils.joinWords(value, unit));
                }
                value = r.getDouble(colVolume);
                if (BeeUtils.isPositive(value)) {
                  String unit = r.getString(colVolumeUnit);
                  volume.put(unit, BeeUtils.unbox(volume.get(unit)) + value);
                  cargoInfo.setHtml(c, map.get(colVolume).getA(),
                      BeeUtils.joinWords(value, unit));
                }
                int x = 0;

                for (BeeRowSet places : new BeeRowSet[] {loading, unloading}) {
                  Flow pl = new Flow();

                  for (int i = 0; i < places.getNumberOfRows(); i++) {
                    if (Objects.equals(places.getLong(i, COL_CARGO), r.getLong(cargoIdx))) {
                      pl.add(new Label(BeeUtils.joinItems(Format.renderDateTime(places
                              .getDateTime(i, COL_PLACE_DATE)),
                          places.getString(i, COL_PLACE_ADDRESS),
                          places.getString(i, COL_PLACE_POST_INDEX),
                          places.getString(i, ALS_COUNTRY_NAME))));
                    }
                  }
                  cargoInfo.setWidget(c, 3 + x, pl);
                  x++;
                }
              }
              if (totals != null) {
                HtmlTable table = new HtmlTable();

                for (int i = 0; i < 3; i++) {
                  int r = 0;
                  table.setHtml(r++, i, (i == 0) ? loc.weight()
                      : ((i == 1) ? loc.trdQuantity() : loc.volume()));

                  Map<String, Double> m = (i == 0) ? weight : ((i == 1) ? quantity : volume);

                  for (String unit : m.keySet()) {
                    table.setHtml(r++, i,
                        BeeUtils.joinWords(BeeUtils.toString(m.get(unit), 3), unit));
                  }
                }
                totals.getElement().setInnerHTML(table.getElement().getString());
              }
              if (cargo != null) {
                cargo.getElement().setInnerHTML(cargoInfo.getElement().getString());
              }
            });
          }
        });
    forwarderDetails = new Flow();
    customerDetails = new Flow();

    DomUtils.setDataProperty(forwarderDetails.getElement(), "locale", Localized.dictionary()
        .languageTag());
    DomUtils.setDataProperty(customerDetails.getElement(), "locale", Localized.dictionary()
        .languageTag());

    ClassifierUtils.getCompanyInfo(row.getLong(form.getDataIndex(COL_FORWARDER)),
        forwarderDetails);
    ClassifierUtils.getCompanyInfo(BeeKeeper.getUser().getCompany(), customerDetails);

    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new AbstractFormInterceptor() {
      @Override
      public void afterCreateWidget(String name, IdentifiableWidget widget,
          WidgetDescriptionCallback callback) {

        if (BeeUtils.same(name, COL_CARGO)) {
          widget.getElement().setInnerHTML(cargo.getElement().getInnerHTML());
        }

        if (BeeUtils.same(name, COL_FORWARDER)) {
          widget.getElement().setInnerHTML(forwarderDetails.getElement().getInnerHTML());
        }

        if (BeeUtils.same(name, COL_CUSTOMER)) {
          widget.getElement().setInnerHTML(customerDetails.getElement().getInnerHTML());
        }

        super.afterCreateWidget(name, widget, callback);
      }

      @Override
      public FormInterceptor getInstance() {
        return null;
      }
    };
  }

  @Override
  public FormInterceptor getInstance() {
    return new AssessmentTransportationForm();
  }
}
