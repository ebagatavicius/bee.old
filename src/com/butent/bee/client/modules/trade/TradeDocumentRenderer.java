package com.butent.bee.client.modules.trade;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.PRM_COMPANY;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Link;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeDocumentData;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TradeDocumentRenderer extends AbstractFormInterceptor {

  private enum ItemColumn implements HasLocalizedCaption {
    ORDINAL("ordinal", false) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.printItemOrdinal();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return BeeUtils.toString(rowIndex + 1);
      }
    },

    NAME("name", false) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.printInvoiceItemName();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return rowSet.getString(rowIndex, ALS_ITEM_NAME);
      }
    },

    ARTICLE("article", false) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.article();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return rowSet.getString(rowIndex, COL_TRADE_ITEM_ARTICLE);
      }
    },

    QUANTITY("quantity", false) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.printItemQuantity();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return rowSet.getString(rowIndex, COL_TRADE_ITEM_QUANTITY);
      }
    },

    UNIT("unit", false) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.printItemUom();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return rowSet.getString(rowIndex, ALS_UNIT_NAME);
      }
    },

    PRICE("price", true) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.price();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        Double price = rowSet.getDouble(rowIndex, COL_TRADE_ITEM_PRICE);
        return PRICE_FORMAT.format(BeeUtils.unbox(price));
      }
    },

    AMOUNT("amount", true) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.amount();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return AMOUNT_FORMAT.format(total - vat);
      }
    },

    TOTAL_WITHOUT_VAT("total-without-vat", true) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.printItemTotalWithoutVat();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return AMOUNT_FORMAT.format(total - vat);
      }
    },

    VAT_RATE("vat-rate", false) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.printItemVatRate();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        Double percent;

        if (BeeUtils.isTrue(rowSet.getBoolean(rowIndex, COL_TRADE_VAT_PERC))) {
          percent = rowSet.getDouble(rowIndex, COL_TRADE_VAT);
        } else if (BeeUtils.isPositive(vat) && BeeUtils.isPositive(total)) {
          percent = vat * 100d / total;
        } else {
          percent = null;
        }

        if (BeeUtils.isDouble(percent)) {
          return BeeUtils.toString(percent, rowSet.getColumn(COL_TRADE_VAT).getScale());
        } else {
          return null;
        }
      }
    },

    VAT_AMOUNT("vat-amount", true) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.printItemVatAmount();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return AMOUNT_FORMAT.format(vat);
      }
    },

    TOTAL_WITH_VAT("total-with-vat", true) {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.printItemTotalWithVat();
      }

      @Override
      String render(BeeRowSet rowSet, int rowIndex, double vat, double total) {
        return AMOUNT_FORMAT.format(total);
      }
    };

    private final String styleSuffix;
    private final boolean hasCurency;

    ItemColumn(String styleSuffix, boolean hasCurency) {
      this.styleSuffix = styleSuffix;
      this.hasCurency = hasCurency;
    }

    abstract String render(BeeRowSet rowSet, int rowIndex, double vat, double total);
  }

  private static final String NAME_DOC_TITLE = "DocTitle";
  private static final String NAME_DOC_NUMBER = "DocNumber";
  private static final String NAME_DOC_ID = "DocId";

  private static final String NAME_ITEMS = "Items";

  private static final String NAME_TOTAL_IN_WORDS = "TotalInWords";

  private static final String PREFIX_SUPPLIER = "Supplier";
  private static final String PREFIX_CUSTOMER = "Customer";
  private static final String PREFIX_PAYER = "Payer";

  private static final String SUFFIX_NAME = "Name";
  private static final String SUFFIX_CODE = "Code";
  private static final String SUFFIX_VAT_CODE = "VatCode";
  private static final String SUFFIX_ADDRESS = "Address";
  private static final String SUFFIX_PHONE = "Phone";
  private static final String SUFFIX_BANK = "Bank";
  private static final String SUFFIX_WEBSITE = "Website";

  private static final String ATTRIBUTE_CURRENCIES = "currencies";
  private static final String ATTRIBUTE_COLUMNS = "columns";

  private static final NumberFormat PRICE_FORMAT = Format.getDecimalFormat(2);
  private static final NumberFormat AMOUNT_FORMAT = Format.getDecimalFormat(2);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "trade-print-";

  private static Long getCompany(FormView form, IsRow row, String colName, boolean checkDefault) {
    int index = form.getDataIndex(colName);

    if (BeeConst.isUndef(index)) {
      return null;

    } else {
      Long company = row.getLong(index);

      if (company == null && checkDefault) {
        company = BeeUtils.nvl(Global.getParameterRelation(PRM_COMPANY),
            BeeKeeper.getUser().getCompany());
      }
      return company;
    }
  }

  private static void hideElement(FormView form, String styleName) {
    Element element = Selectors.getElement(form.getElement(), Selectors.classSelector(styleName));
    if (element != null) {
      StyleUtils.hideDisplay(element);
    }
  }

  private static String join(String label, String value) {
    if (BeeUtils.isEmpty(value)) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeUtils.joinWords(label, value);
    }
  }

  private static String renderBankInfo(long id, TradeDocumentData tdd) {
    BeeRowSet bankAccounts = tdd.getCompanyBankAccounts(id);

    if (DataUtils.isEmpty(bankAccounts)) {
      return BeeConst.STRING_EMPTY;

    } else {
      List<String> info = new ArrayList<>();

      int accountIndex = bankAccounts.getColumnIndex(COL_BANK_ACCOUNT);
      int nameIndex = bankAccounts.getColumnIndex(ALS_BANK_NAME);
      int codeIndex = bankAccounts.getColumnIndex(COL_BANK_CODE);
      int swiftIndex = bankAccounts.getColumnIndex(COL_SWIFT_CODE);

      for (IsRow row : bankAccounts) {
        info.add(BeeUtils.joinItems(
            join(Localized.dictionary().printBankAccount(), row.getString(accountIndex)),
            row.getString(nameIndex),
            join(Localized.dictionary().printBankCode(), row.getString(codeIndex)),
            join(Localized.dictionary().printBankSwift(), row.getString(swiftIndex))));
      }

      return BeeUtils.buildLines(info);
    }
  }

  private static void renderCompany(long id, TradeDocumentData tdd, String prefix,
      Map<String, Widget> widgets) {

    Widget widget = widgets.get(prefix + SUFFIX_NAME);
    if (widget != null) {
      setHtml(widget, tdd.getCompanyValue(id, COL_COMPANY_NAME));
    }

    widget = widgets.get(prefix + SUFFIX_CODE);
    if (widget != null) {
      setHtml(widget, join(Localized.dictionary().companyCode(),
          tdd.getCompanyValue(id, COL_COMPANY_CODE)));
    }

    widget = widgets.get(prefix + SUFFIX_VAT_CODE);
    if (widget != null) {
      setHtml(widget, join(Localized.dictionary().companyVATCode(),
          tdd.getCompanyValue(id, COL_COMPANY_VAT_CODE)));
    }

    widget = widgets.get(prefix + SUFFIX_ADDRESS);
    if (widget != null) {
      setHtml(widget, BeeUtils.joinItems(tdd.getCompanyValue(id, COL_ADDRESS),
          tdd.getCompanyValue(id, COL_POST_INDEX), tdd.getCompanyValue(id, ALS_CITY_NAME),
          tdd.getCompanyValue(id, ALS_COUNTRY_NAME)));
    }

    widget = widgets.get(prefix + SUFFIX_PHONE);
    if (widget != null) {
      setHtml(widget, BeeUtils.joinItems(
          join(Localized.dictionary().phone(), tdd.getCompanyValue(id, COL_PHONE)),
          join(Localized.dictionary().mobile(), tdd.getCompanyValue(id, COL_MOBILE)),
          join(Localized.dictionary().fax(), tdd.getCompanyValue(id, COL_FAX))));
    }

    widget = widgets.get(prefix + SUFFIX_WEBSITE);
    if (widget != null) {
      String website = tdd.getCompanyValue(id, COL_WEBSITE);

      if (widget instanceof Link) {
        if (!BeeUtils.isEmpty(website)) {
          ((Link) widget).setHref(website);
          ((Link) widget).setText(website);
        }
      } else {
        setHtml(widget, website);
      }
    }

    widget = widgets.get(prefix + SUFFIX_BANK);
    if (widget != null) {
      setHtml(widget, renderBankInfo(id, tdd));
    }
  }

  private static String renderDocId(long id) {
    return join(Localized.dictionary().captionId(), BeeUtils.toString(id));
  }

  private static void setHtml(Widget widget, String html) {
    widget.getElement().setInnerHTML(html);
  }

  private final Map<String, Long> companies = new HashMap<>();

  private final Set<String> currencies = new HashSet<>();

  private final String itemViewName;

  private final String itemRelation;
  private final TotalRenderer itemTotalRenderer;

  private final VatRenderer itemVatRenderer;

  public TradeDocumentRenderer(String itemViewName, String itemRelation) {
    this.itemViewName = itemViewName;
    this.itemRelation = itemRelation;

    List<BeeColumn> columns = Data.getColumns(itemViewName);

    this.itemTotalRenderer = new TotalRenderer(columns);
    this.itemVatRenderer = new VatRenderer(columns);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    setCompanies(form, row);
    setCurrencies(form, row);

    getDocumentData(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeDocumentRenderer(itemViewName, itemRelation);
  }

  private void getDocumentData(FormView form, IsRow row) {
    if (form != null && DataUtils.hasId(row)) {
      final long id = row.getId();

      ParameterList params = TradeKeeper.createArgs(SVC_GET_DOCUMENT_DATA);

      params.addDataItem(Service.VAR_ID, id);

      DateTime date = row.getDateTime(form.getDataIndex(COL_TRADE_DATE));
      if (date != null) {
        params.addDataItem(COL_TRADE_DATE, date.getTime());
      }

      Double amount = row.getDouble(form.getDataIndex(COL_TRADE_AMOUNT));
      if (BeeUtils.isDouble(amount)) {
        params.addDataItem(COL_TRADE_AMOUNT, amount);
      }
      Long currency = row.getLong(form.getDataIndex(COL_TRADE_CURRENCY));
      if (DataUtils.isId(currency)) {
        params.addDataItem(COL_TRADE_CURRENCY, currency);
      }

      params.addDataItem(Service.VAR_VIEW_NAME, itemViewName);
      params.addDataItem(Service.VAR_COLUMN, itemRelation);

      if (!companies.isEmpty()) {
        Set<Long> companyIds = new HashSet<>(companies.values());
        params.addDataItem(VIEW_COMPANIES, DataUtils.buildIdList(companyIds));
      }

      if (!currencies.isEmpty()) {
        params.addDataItem(AdministrationConstants.VIEW_CURRENCIES,
            Codec.beeSerialize(currencies));
      }

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasResponse(TradeDocumentData.class)
              && Objects.equals(getActiveRowId(), id)) {
            TradeDocumentData tdd = TradeDocumentData.restore(response.getResponseAsString());
            refresh(tdd);
          }
        }
      });
    }
  }

  private double getItemTotal(BeeRowSet rowSet, int rowIndex) {
    return BeeUtils.unbox(itemTotalRenderer.getTotal(rowSet.getRow(rowIndex)));
  }

  private double getItemVat(BeeRowSet rowSet, int rowIndex) {
    return BeeUtils.unbox(itemVatRenderer.getVat(rowSet.getRow(rowIndex)));
  }

  private String getString(IsRow row, String colName) {
    int index = getDataIndex(colName);
    return BeeConst.isUndef(index) ? null : row.getString(index);
  }

  private void refresh(TradeDocumentData tdd) {
    FormView form = getFormView();
    IsRow row = getActiveRow();

    if (form == null || row == null) {
      return;
    }

    Map<String, Widget> namedWidgets = form.getNamedWidgets();

    Widget widget = namedWidgets.get(NAME_DOC_TITLE);
    if (widget != null) {
      setHtml(widget, renderDocTitle(row, tdd));
    }

    widget = namedWidgets.get(NAME_DOC_NUMBER);
    if (widget != null) {
      setHtml(widget, renderDocNumber(row));
    }

    widget = namedWidgets.get(NAME_DOC_ID);
    if (widget != null) {
      setHtml(widget, renderDocId(row.getId()));
    }

    Long supplier = companies.get(COL_TRADE_SUPPLIER);
    if (DataUtils.isId(supplier) && tdd.containsCompany(supplier)) {
      renderCompany(supplier, tdd, PREFIX_SUPPLIER, namedWidgets);
    } else {
      hideElement(form, STYLE_PREFIX + "supplier");
    }

    Long customer = companies.get(COL_TRADE_CUSTOMER);
    if (DataUtils.isId(customer) && tdd.containsCompany(customer)) {
      renderCompany(customer, tdd, PREFIX_CUSTOMER, namedWidgets);
    } else {
      hideElement(form, STYLE_PREFIX + "customer");
    }

    Long payer = companies.get(COL_SALE_PAYER);
    if (DataUtils.isId(payer) && tdd.containsCompany(payer)) {
      renderCompany(payer, tdd, PREFIX_PAYER, namedWidgets);
    } else {
      hideElement(form, STYLE_PREFIX + "payer");
    }

    widget = namedWidgets.get(NAME_ITEMS);
    if (widget instanceof HtmlTable && !DataUtils.isEmpty(tdd.getItems())) {
      renderItems(tdd.getItems(), (HtmlTable) widget);
    }

    widget = namedWidgets.get(NAME_TOTAL_IN_WORDS);
    if (widget != null) {
      TradeUtils.getTotalInWords(form.getDoubleValue(COL_TRADE_AMOUNT),
          form.getLongValue(AdministrationConstants.COL_CURRENCY), widget);
    }
  }

  private String renderDocNumber(IsRow row) {
    String series = getString(row, COL_TRADE_INVOICE_PREFIX);
    String number = getString(row, COL_TRADE_INVOICE_NO);

    if (BeeUtils.isEmpty(number)) {
      return BeeConst.STRING_EMPTY;
    } else if (BeeUtils.isEmpty(series)) {
      return join(Localized.dictionary().printDocumentNumber(), number);
    } else {
      return Localized.dictionary().printDocumentSeriesAndNumber(series, number);
    }
  }

  private String renderDocTitle(IsRow row, TradeDocumentData tdd) {
    if (BeeUtils.anyEmpty(getString(row, COL_TRADE_INVOICE_PREFIX),
        getString(row, COL_TRADE_INVOICE_NO))) {
      return BeeConst.STRING_EMPTY;

    } else if (BeeUtils.isEmpty(tdd.getCompanyValue(companies.get(COL_TRADE_SUPPLIER),
        COL_COMPANY_VAT_CODE))) {
      return Localized.dictionary().printInvoice();

    } else {
      return Localized.dictionary().printInvoiceVat();
    }
  }

  private void renderItems(BeeRowSet items, HtmlTable table) {
    List<ItemColumn> columns = EnumUtils.parseNameList(ItemColumn.class,
        DomUtils.getDataProperty(table.getElement(), ATTRIBUTE_COLUMNS));

    int r = 0;

    String currencyName = getStringValue(AdministrationConstants.ALS_CURRENCY_NAME);

    for (int j = 0; j < columns.size(); j++) {
      ItemColumn itemColumn = columns.get(j);

      String label = itemColumn.hasCurency
          ? BeeUtils.joinWords(itemColumn.getCaption(), currencyName) : itemColumn.getCaption();
      table.setText(r, j, label);
    }

    table.getRowFormatter().addStyleName(r, STYLE_PREFIX + "items-header");

    double vatSum = BeeConst.DOUBLE_ZERO;
    double totSum = BeeConst.DOUBLE_ZERO;

    for (int i = 0; i < items.getNumberOfRows(); i++) {
      double vat = getItemVat(items, i);
      double total = getItemTotal(items, i);

      r++;
      for (int j = 0; j < columns.size(); j++) {
        ItemColumn itemColumn = columns.get(j);
        table.setText(r, j, itemColumn.render(items, i, vat, total),
            STYLE_PREFIX + "item-" + itemColumn.styleSuffix);
      }

      vatSum += vat;
      totSum += total;
    }

    if (columns.size() > 1) {
      r++;
      table.setText(r, columns.size() - 2, Localized.dictionary().printDocumentSubtotal(),
          STYLE_PREFIX + "items-total-label");
      table.setText(r, columns.size() - 1, AMOUNT_FORMAT.format(totSum - vatSum),
          STYLE_PREFIX + "items-total-value");

      r++;
      table.setText(r, columns.size() - 2, Localized.dictionary().printDocumentVat(),
          STYLE_PREFIX + "items-total-label");
      table.setText(r, columns.size() - 1, AMOUNT_FORMAT.format(vatSum),
          STYLE_PREFIX + "items-total-value");

      r++;
      table.setText(r, columns.size() - 2, Localized.dictionary().printDocumentTotal(),
          STYLE_PREFIX + "items-total-label");
      table.setText(r, columns.size() - 1, AMOUNT_FORMAT.format(totSum),
          STYLE_PREFIX + "items-total-value");
    }
  }

  private void setCompanies(FormView form, IsRow row) {
    if (!companies.isEmpty()) {
      companies.clear();
    }

    if (form != null && row != null) {
      Long supplier = getCompany(form, row, COL_TRADE_SUPPLIER, true);
      if (supplier != null) {
        companies.put(COL_TRADE_SUPPLIER, supplier);
      }

      Long customer = getCompany(form, row, COL_TRADE_CUSTOMER, true);
      if (customer != null) {
        companies.put(COL_TRADE_CUSTOMER, customer);
      }

      Long payer = getCompany(form, row, COL_SALE_PAYER, false);
      if (payer != null) {
        companies.put(COL_SALE_PAYER, payer);
      }
    }
  }

  private void setCurrencies(FormView form, IsRow row) {
    if (!currencies.isEmpty()) {
      currencies.clear();
    }

    if (form != null && row != null) {
      int index = form.getDataIndex(AdministrationConstants.ALS_CURRENCY_NAME);

      if (!BeeConst.isUndef(index)) {
        String currency = row.getString(index);
        if (!BeeUtils.isEmpty(currency)) {
          currencies.add(currency);
        }
      }

      currencies.addAll(NameUtils.toSet(form.getProperty(ATTRIBUTE_CURRENCIES)));
    }
  }
}
