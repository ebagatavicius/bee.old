package com.butent.bee.client.modules.transport;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.UserInfo;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.output.PrintFormInterceptor;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.AbstractFormInterceptor;
import com.butent.bee.client.ui.FormFactory.FormInterceptor;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;

public class AssessmentTransportationForm extends PrintFormInterceptor {

  private static final String WIDGET_NAME_FIRST_PARAGRAPH = "firstParagraph";
  private static final String WIDGET_NAME_CUSOTMER_FAX = "customerFax";
  private static final String WIDGET_NAME_TRANSPORTATION_PRICE = "transportationPrice";

  private static final String REPLACEMENT_FAX = "{fax}";
  private static final String REPLACEMENT_PRICE_IN_EUR = "{price_in_eur}";

  private static final List<String> PARAGRAPH_TEXT = Lists.newArrayList("atstovaujama",
      "toliau vadinamas \"Užsakovu\" ir", "atstovaujama", "________________________________",
      ", toliau vadinamas \"Vežėju\" sudarė šią krovinio vežimo sutartį");

  private static final String TEXT_PRICE_IN_EUR = "({price_in_eur} pagal kursą EUR)";

  private static final double FIXED_EUR_CURRENCY_RATE = 3.4528;
  private static final String EUR_CURRENCY_LABEL = "eur";

  private Widget totals;
  private Widget cargo;
  private Widget firstParagraph;
  private Widget forwarderDetails;
  private Widget customerDetails;

  private String customerFax = "";
  private String priceInEur = "";

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, "Totals")) {
      totals = widget.asWidget();
    } else if (BeeUtils.same(name, COL_CARGO)) {
      cargo = widget.asWidget();
    } else if (widget instanceof HasEnabled && BeeUtils.same(name, "TransportationCurrency")) {
      ((HasEnabled) widget).setEnabled(false);
      StyleUtils.setStyleName(widget.getElement(), "bee-disabled", true);
    }
    super.afterCreateWidget(name, widget, callback);
  }
  
  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    if (!BeeUtils.anyNotNull(totals, cargo)) {
      return;
    }
    Queries.getRowSet(TBL_ORDER_CARGO, null,
        Filter.in(Data.getIdColumn(TBL_ORDER_CARGO), TBL_CARGO_TRIPS, COL_CARGO,
            Filter.equals(COL_TRIP, row.getId())),
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            int colWeight = result.getColumnIndex("Weight");
            int colWeightUnit = result.getColumnIndex("WeightUnitName");
            Map<String, Double> weight = Maps.newHashMap();
            int colQuantity = result.getColumnIndex("Quantity");
            int colQuantityUnit = result.getColumnIndex("QuantityUnitName");
            Map<String, Double> quantity = Maps.newHashMap();
            int colVolume = result.getColumnIndex("Volume");
            int colVolumeUnit = result.getColumnIndex("VolumeUnitName");
            Map<String, Double> volume = Maps.newHashMap();

            int colLoadingCountry = result.getColumnIndex("LoadingCountryName");
            int colLoadingAddress = result.getColumnIndex("LoadingAddress");
            int colLoadingPostIndex = result.getColumnIndex("LoadingPostIndex");
            int colLoadingDate = result.getColumnIndex("LoadingDate");
            int colUnloadingCountry = result.getColumnIndex("UnloadingCountryName");
            int colUnloadingAddress = result.getColumnIndex("UnloadingAddress");
            int colUnloadingPostIndex = result.getColumnIndex("UnloadingPostIndex");
            int colUnloadingDate = result.getColumnIndex("UnloadingDate");

            LocalizableConstants loc = Localized.getConstants();
            Map<Integer, Pair<Integer, String>> map = Maps.newHashMap();

            map.put(colLoadingAddress, Pair.of(0, loc.trLoadingAddress()));
            map.put(colWeight, Pair.of(1, loc.weight()));
            map.put(colQuantity, Pair.of(2, loc.trdQuantity()));
            map.put(colVolume, Pair.of(3, loc.volume()));
            map.put(colLoadingDate, Pair.of(4, loc.trLoadingDate()));
            map.put(colUnloadingAddress, Pair.of(5, loc.trUnloadingAddress()));
            map.put(colUnloadingDate, Pair.of(6, loc.trUnloadingDate()));

            HtmlTable cargoInfo = new HtmlTable();
            int c = 0;

            for (Pair<Integer, String> pair : map.values()) {
              cargoInfo.setHtml(c, pair.getA(), pair.getB());
            }
            for (BeeRow r : result.getRows()) {
              c++;
              Double value = r.getDouble(colWeight);
              if (BeeUtils.isPositive(value)) {
                String unit = r.getString(colWeightUnit);
                weight.put(unit, BeeUtils.unbox(weight.get(unit)) + value);
                cargoInfo.setHtml(c, map.get(colWeight).getA(), BeeUtils.joinWords(value, unit));
              }
              value = r.getDouble(colQuantity);
              if (BeeUtils.isPositive(value)) {
                String unit = r.getString(colQuantityUnit);
                quantity.put(unit, BeeUtils.unbox(quantity.get(unit)) + value);
                cargoInfo.setHtml(c, map.get(colQuantity).getA(), BeeUtils.joinWords(value, unit));
              }
              value = r.getDouble(colVolume);
              if (BeeUtils.isPositive(value)) {
                String unit = r.getString(colVolumeUnit);
                volume.put(unit, BeeUtils.unbox(volume.get(unit)) + value);
                cargoInfo.setHtml(c, map.get(colVolume).getA(), BeeUtils.joinWords(value, unit));
              }
              for (Integer idx : map.keySet()) {
                if (BeeUtils.inList(idx, colLoadingDate, colUnloadingDate)) {
                  DateTime dt = r.getDateTime(idx);
                  cargoInfo.setHtml(c, map.get(idx).getA(),
                      dt == null ? null : dt.toCompactString());

                } else if (Objects.equal(idx, colLoadingAddress)) {
                  cargoInfo.setHtml(c, map.get(idx).getA(),
                      BeeUtils.joinItems(r.getString(colLoadingCountry), r.getString(idx), r
                          .getString(colLoadingPostIndex)));

                } else if (Objects.equal(idx, colUnloadingAddress)) {
                  cargoInfo.setHtml(c, map.get(idx).getA(),
                      BeeUtils.joinItems(r.getString(colUnloadingCountry), r.getString(idx), r
                          .getString(colUnloadingPostIndex)));
                }
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
          }
        });

    calculateTransportationPriceInEur(form, row);

    forwarderDetails = new Flow();
    customerDetails = new Flow();
    firstParagraph = new Flow();
    DomUtils.setDataProperty(forwarderDetails.getElement(), "locale", Localized.getConstants()
        .languageTag());
    DomUtils.setDataProperty(customerDetails.getElement(), "locale", Localized.getConstants()
        .languageTag());

    ClassifierUtils.getCompanyInfo(row.getLong(form.getDataIndex(COL_FORWARDER)),
        forwarderDetails);
    ClassifierUtils.getCompanyInfo(BeeKeeper.getUser().getCompany(), customerDetails);
    renderFirstParagraph(form, row, firstParagraph);

    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new AbstractFormInterceptor() {
      private String replacementTempalte = "";
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

        if (BeeUtils.same(name, WIDGET_NAME_FIRST_PARAGRAPH)) {
          widget.getElement().setInnerText(firstParagraph.getElement().getInnerText());
        }

        if (BeeUtils.same(name, WIDGET_NAME_CUSOTMER_FAX)) {
          widget.getElement().setInnerText(
              BeeUtils.replace(widget.getElement().getInnerText(), REPLACEMENT_FAX,
                  getCustomerFax()));
        }
        super.afterCreateWidget(name, widget, callback);
      }

      @Override
      public void afterRefresh(FormView form, IsRow row) {
        Widget widget = form.getWidgetByName(WIDGET_NAME_TRANSPORTATION_PRICE);
        if (widget != null) {
          if (BeeUtils.isEmpty(replacementTempalte)) {
            replacementTempalte = widget.getElement().getInnerHTML();
          }
          widget.getElement().setInnerHTML(
              BeeUtils.replace(replacementTempalte, REPLACEMENT_PRICE_IN_EUR,
                  getPriceInEur()));
        }
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

  private String getCustomerFax() {
    return customerFax;
  }

  private void setCustomerFax(String customerFax) {
    this.customerFax = customerFax;
  }

  private String getPriceInEur() {
    return priceInEur;
  }

  private void setPriceInEur(String priceInEur) {
    this.priceInEur = priceInEur;
  }

  private void calculateTransportationPriceInEur(FormView form, IsRow row) {
    String selectedCurrency = row.getString(form.getDataIndex("TransportationCurrencyName"));

    if (BeeUtils.same(selectedCurrency, EUR_CURRENCY_LABEL)) {
      setPriceInEur(BeeConst.STRING_EMPTY);
    } else {
      double price = BeeUtils.unbox(row.getDouble(form.getDataIndex("TransportationPrice")));
      if (!BeeUtils.isPositive(price)) {
        setPriceInEur(BeeConst.STRING_EMPTY);
        return;
      }

      double exchange = price * FIXED_EUR_CURRENCY_RATE;

      setPriceInEur(BeeUtils.replace(TEXT_PRICE_IN_EUR, REPLACEMENT_PRICE_IN_EUR,
          BeeUtils.toString(exchange, 2)));
    }
  }

  private void renderFirstParagraph(final FormView form, final IsRow row,
      final Widget widget) {
    final UserInfo info = BeeKeeper.getUser();
    final DataInfo viewInfo = Data.getDataInfo(ClassifierConstants.VIEW_COMPANIES);
    final String customerPersonSignature =
        BeeUtils.joinWords(info.getFirstName(), info.getLastName());
    long forwarderId = BeeUtils.unbox(row.getLong(form.getDataIndex(COL_FORWARDER)));
    Filter filter = Filter.idIn(Lists.newArrayList(info.getCompany(), forwarderId));
    
    Queries.getRowSet(ClassifierConstants.VIEW_COMPANIES, viewInfo.getColumnNames(false),
        filter, new RowSetCallback() {

          @Override
          public void onSuccess(BeeRowSet result) {
            String customerCompanySignature = "";
            String forwarderCompanySignature = "";
            
            for (IsRow resutlRow : result) {
              long id = resutlRow.getId();

              String signature = BeeUtils.joinWords(resutlRow.getString(viewInfo.getColumnIndex(
                  ClassifierConstants.COL_COMPANY_NAME))
                  + BeeConst.STRING_COMMA, resutlRow.getString(viewInfo.getColumnIndex(
                  ClassifierConstants.ALS_COMPANY_TYPE_NAME)));
              String fax = resutlRow.getString(viewInfo.getColumnIndex(
                  ClassifierConstants.COL_FAX));

              if (id == BeeUtils.unbox(info.getCompany())) {
                customerCompanySignature = signature;
                setCustomerFax(fax);
              } else if (id == BeeUtils.unbox(row.getLong(form.getDataIndex(COL_FORWARDER)))) {
                forwarderCompanySignature = signature;
              }

            }
            
            String pText = BeeUtils.joinWords(customerCompanySignature + BeeConst.STRING_COMMA,
                BeeUtils.join(BeeConst.STRING_SPACE, PARAGRAPH_TEXT.subList(0, 1)),
                customerPersonSignature + BeeConst.STRING_COMMA,
                    BeeUtils.join(BeeConst.STRING_SPACE, PARAGRAPH_TEXT.subList(1, 2)),
                forwarderCompanySignature + BeeConst.STRING_COMMA,
                    BeeUtils.join(BeeConst.STRING_SPACE, PARAGRAPH_TEXT.subList(2, PARAGRAPH_TEXT
                        .size()))
              );

            widget.getElement().setInnerText(pText);
          }
    });
    
  }
}
