package com.butent.bee.shared.modules.trade;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.ColumnInFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum TradeItemSearch implements HasLocalizedCaption {

  NAME {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.itemName();
    }

    @Override
    public Filter getItemFilter(String query) {
      return condition(COL_ITEM_NAME, query);
    }
  },

  NAME_2 {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.itemName2();
    }

    @Override
    public Filter getItemFilter(String query) {
      return condition(COL_ITEM_NAME_2, query);
    }
  },

  NAME_3 {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.itemName3();
    }

    @Override
    public Filter getItemFilter(String query) {
      return condition(COL_ITEM_NAME_3, query);
    }
  },

  ARTICLE {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.article();
    }

    @Override
    public Filter getItemFilter(String query) {
      return condition(COL_ITEM_ARTICLE, query);
    }
  },

  ARTICLE_2 {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.article2();
    }

    @Override
    public Filter getItemFilter(String query) {
      return condition(COL_ITEM_ARTICLE_2, query);
    }
  },

  BARCODE {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.itemBarcode();
    }

    @Override
    public Filter getItemFilter(String query) {
      return condition(COL_ITEM_BARCODE, query);
    }
  },

  TYPE {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.type();
    }

    @Override
    public Filter getItemFilter(String query) {
      return Filter.or(condition(ALS_PARENT_TYPE_NAME, query),
          condition(ALS_ITEM_TYPE_NAME, query));
    }
  },

  GROUP {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.group();
    }

    @Override
    public Filter getItemFilter(String query) {
      return Filter.or(condition(ALS_PARENT_GROUP_NAME, query),
          condition(ALS_ITEM_GROUP_NAME, query));
    }
  },

  CATEGORY {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.category();
    }

    @Override
    public Filter getItemFilter(String query) {
      return Filter.idIn(VIEW_ITEM_CATEGORIES, COL_ITEM,
          Filter.inId(COL_CATEGORY, VIEW_ITEM_CATEGORY_TREE, condition(COL_CATEGORY_NAME, query)));
    }
  },

  DESCRIPTION {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.description();
    }

    @Override
    public Filter getItemFilter(String query) {
      return condition(COL_ITEM_DESCRIPTION, query);
    }
  },

  SUPPLIER {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.supplier();
    }

    @Override
    public Filter getItemFilter(String query) {
      return Filter.idIn(VIEW_ITEM_SUPPLIERS, COL_ITEM, condition(ALS_ITEM_SUPPLIER_NAME, query),
          ColumnInFilter.OPTION_FROM);
    }
  },

  MANUFACTURER {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.manufacturer();
    }

    @Override
    public Filter getItemFilter(String query) {
      return Filter.idIn(VIEW_ITEM_MANUFACTURERS, COL_ITEM,
          condition(ALS_ITEM_MANUFACTURER_NAME, query), ColumnInFilter.OPTION_FROM);
    }
  },

  ID {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.captionId();
    }

    @Override
    public String validate(String input, Dictionary dictionary) {
      if (DataUtils.isId(input)) {
        return super.validate(input, dictionary);
      } else {
        return dictionary.invalidIdValue(input);
      }
    }

    @Override
    public Filter getItemFilter(String query) {
      return DataUtils.isId(query) ? Filter.compareId(BeeUtils.toLong(query)) : null;
    }
  };

  public abstract Filter getItemFilter(String query);

  public String validate(String input, Dictionary dictionary) {
    return null;
  }

  private static Filter condition(String column, String query) {
    if (BeeUtils.anyEmpty(column, query)) {
      return null;
    }

    Operator operator;
    String value;

    if (query.contains(Operator.CHAR_ANY) || query.contains(Operator.CHAR_ONE)) {
      operator = Operator.MATCHES;
      value = query;

    } else if (BeeUtils.isPrefixOrSuffix(query, BeeConst.CHAR_EQ)) {
      operator = Operator.EQ;
      value = BeeUtils.removePrefixAndSuffix(query, BeeConst.CHAR_EQ);

    } else {
      operator = Operator.CONTAINS;
      value = query;
    }

    return Filter.compareWithValue(column, operator, new TextValue(value));
  }
}
