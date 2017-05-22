package com.butent.bee.shared.modules.trade;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public enum TradeReportGroup implements HasLocalizedCaption {

  ITEM_TYPE("type") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.itemType();
    }

    @Override
    public ValueType getType() {
      return ValueType.LONG;
    }

    @Override
    public String valueSource() {
      return TBL_ITEMS;
    }

    @Override
    public String valueColumn() {
      return COL_ITEM_TYPE;
    }

    @Override
    public boolean primaryDocument() {
      return true;
    }

    @Override
    public String labelSource() {
      return TBL_ITEM_CATEGORY_TREE;
    }

    @Override
    public String labelColumn() {
      return COL_CATEGORY_NAME;
    }

    @Override
    public String editViewName() {
      return null;
    }
  },

  ITEM_GROUP("group") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.itemGroup();
    }

    @Override
    public ValueType getType() {
      return ValueType.LONG;
    }

    @Override
    public String valueSource() {
      return TBL_ITEMS;
    }

    @Override
    public String valueColumn() {
      return COL_ITEM_GROUP;
    }

    @Override
    public boolean primaryDocument() {
      return true;
    }

    @Override
    public String labelSource() {
      return TBL_ITEM_CATEGORY_TREE;
    }

    @Override
    public String labelColumn() {
      return COL_CATEGORY_NAME;
    }

    @Override
    public String editViewName() {
      return null;
    }
  },

  ITEM("item") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.item();
    }

    @Override
    public ValueType getType() {
      return ValueType.LONG;
    }

    @Override
    public String valueSource() {
      return TBL_TRADE_DOCUMENT_ITEMS;
    }

    @Override
    public String valueColumn() {
      return COL_ITEM;
    }

    @Override
    public boolean primaryDocument() {
      return true;
    }

    @Override
    public String labelSource() {
      return TBL_ITEMS;
    }

    @Override
    public String labelColumn() {
      return COL_ITEM_NAME;
    }

    @Override
    public String editViewName() {
      return VIEW_ITEMS;
    }
  },

  ARTICLE("article") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.article();
    }

    @Override
    public ValueType getType() {
      return ValueType.TEXT;
    }

    @Override
    public String valueSource() {
      return TBL_TRADE_DOCUMENT_ITEMS;
    }

    @Override
    public String valueColumn() {
      return COL_TRADE_ITEM_ARTICLE;
    }

    @Override
    public boolean primaryDocument() {
      return true;
    }

    @Override
    public String labelSource() {
      return null;
    }

    @Override
    public String labelColumn() {
      return null;
    }

    @Override
    public String editViewName() {
      return null;
    }
  },

  UNIT("unit") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.unitShort();
    }

    @Override
    public ValueType getType() {
      return ValueType.LONG;
    }

    @Override
    public String valueSource() {
      return TBL_ITEMS;
    }

    @Override
    public String valueColumn() {
      return COL_UNIT;
    }

    @Override
    public boolean primaryDocument() {
      return true;
    }

    @Override
    public String labelSource() {
      return TBL_UNITS;
    }

    @Override
    public String labelColumn() {
      return COL_UNIT_NAME;
    }

    @Override
    public String editViewName() {
      return null;
    }
  },

  WAREHOUSE("warehouse") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.warehouse();
    }

    @Override
    public ValueType getType() {
      return ValueType.LONG;
    }

    @Override
    public String valueSource() {
      return TBL_TRADE_STOCK;
    }

    @Override
    public String valueColumn() {
      return COL_STOCK_WAREHOUSE;
    }

    @Override
    public boolean primaryDocument() {
      return false;
    }

    @Override
    public String labelSource() {
      return TBL_WAREHOUSES;
    }

    @Override
    public String labelColumn() {
      return COL_WAREHOUSE_CODE;
    }

    @Override
    public String editViewName() {
      return VIEW_WAREHOUSES;
    }
  },

  SUPPLIER("supplier") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.supplier();
    }

    @Override
    public ValueType getType() {
      return ValueType.LONG;
    }

    @Override
    public String valueSource() {
      return TBL_TRADE_DOCUMENTS;
    }

    @Override
    public String valueColumn() {
      return COL_TRADE_SUPPLIER;
    }

    @Override
    public boolean primaryDocument() {
      return true;
    }

    @Override
    public String labelSource() {
      return TBL_COMPANIES;
    }

    @Override
    public String labelColumn() {
      return COL_COMPANY_NAME;
    }

    @Override
    public String editViewName() {
      return VIEW_COMPANIES;
    }
  },

  YEAR_RECEIVED("year") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.receivedYear();
    }

    @Override
    public ValueType getType() {
      return ValueType.DATE_TIME;
    }

    @Override
    public String valueSource() {
      return TBL_TRADE_DOCUMENTS;
    }

    @Override
    public String valueColumn() {
      return COL_TRADE_DATE;
    }

    @Override
    public boolean primaryDocument() {
      return true;
    }

    @Override
    public String labelSource() {
      return null;
    }

    @Override
    public String labelColumn() {
      return null;
    }

    @Override
    public String editViewName() {
      return null;
    }
  },

  MONTH_RECEIVED("month") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.receivedMonth();
    }

    @Override
    public ValueType getType() {
      return ValueType.DATE_TIME;
    }

    @Override
    public String valueSource() {
      return TBL_TRADE_DOCUMENTS;
    }

    @Override
    public String valueColumn() {
      return COL_TRADE_DATE;
    }

    @Override
    public boolean primaryDocument() {
      return true;
    }

    @Override
    public String labelSource() {
      return null;
    }

    @Override
    public String labelColumn() {
      return null;
    }

    @Override
    public String editViewName() {
      return null;
    }
  };

  private final String code;

  TradeReportGroup(String code) {
    this.code = code;
  }

  public static TradeReportGroup parse(String input) {
    if (!BeeUtils.isEmpty(input)) {
      for (TradeReportGroup trg : values()) {
        if (BeeUtils.same(trg.code, input)) {
          return trg;
        }
      }
    }
    return null;
  }

  public static List<TradeReportGroup> parseList(ReportParameters reportParameters, int count) {
    List<TradeReportGroup> list = new ArrayList<>();

    if (reportParameters != null) {
      for (int i = 0; i < count; i++) {
        TradeReportGroup trg = parse(reportParameters.getText(TradeConstants.reportGroupName(i)));

        if (trg != null && !list.contains(trg)) {
          list.add(trg);
        }
      }
    }

    return list;
  }

  public static boolean needsItem(TradeReportGroup trg) {
    return trg != null && trg.needsItem();
  }

  public static boolean needsItem(Collection<TradeReportGroup> groups) {
    if (!BeeUtils.isEmpty(groups)) {
      return groups.stream()
          .filter(Objects::nonNull)
          .anyMatch(trg -> trg.needsItem());

    } else {
      return false;
    }
  }

  public static boolean needsPrimaryDocument(TradeReportGroup trg) {
    return trg != null && trg.needsPrimaryDocument();
  }

  public static boolean needsPrimaryDocument(Collection<TradeReportGroup> groups) {
    if (!BeeUtils.isEmpty(groups)) {
      return groups.stream()
          .filter(Objects::nonNull)
          .anyMatch(trg -> trg.needsPrimaryDocument());

    } else {
      return false;
    }
  }

  public static boolean needsPrimaryDocumentItem(TradeReportGroup trg) {
    return trg != null && trg.needsPrimaryDocumentItem();
  }

  public static boolean needsPrimaryDocumentItem(Collection<TradeReportGroup> groups) {
    if (!BeeUtils.isEmpty(groups)) {
      return groups.stream()
          .filter(Objects::nonNull)
          .anyMatch(trg -> trg.needsPrimaryDocumentItem());

    } else {
      return false;
    }
  }

  public static boolean containsType(Collection<TradeReportGroup> groups, ValueType type) {
    if (!BeeUtils.isEmpty(groups) && type != null) {
      return groups.stream()
          .filter(Objects::nonNull)
          .anyMatch(trg -> trg.getType() == type);

    } else {
      return false;
    }
  }

  public String getCode() {
    return code;
  }

  public String getValueAlias() {
    return getCode() + "_value";
  }

  public String getLabelAlias() {
    return getCode() + "_label";
  }

  public String getStyleSuffix() {
    return getCode();
  }

  public abstract ValueType getType();

  public abstract String valueSource();

  public abstract String valueColumn();

  public abstract boolean primaryDocument();

  public abstract String labelSource();

  public abstract String labelColumn();

  public abstract String editViewName();

  public boolean isEditable() {
    return !BeeUtils.isEmpty(editViewName());
  }

  public boolean needsItem() {
    return TBL_ITEMS.equals(valueSource());
  }

  public boolean needsPrimaryDocument() {
    return primaryDocument() && TBL_TRADE_DOCUMENTS.equals(valueSource());
  }

  public boolean needsPrimaryDocumentItem() {
    return primaryDocument() && TBL_TRADE_DOCUMENT_ITEMS.equals(valueSource());
  }
}
