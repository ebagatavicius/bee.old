package com.butent.bee.shared.modules.trade.acts;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;

import java.util.EnumSet;

public enum TradeActKind implements HasLocalizedCaption {
  SALE(Option.AUTO_NUMBER, Option.BUILD_INVOICES, Option.ENABLE_COPY, Option.ENABLE_RETURN,
      Option.ENABLE_SUPPLEMENT, Option.HAS_SERVICES, Option.SAVE_AS_TEMPLATE, Option.SHOW_STOCK) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taKindSale();
    }

    @Override
    public Filter getFilter() {
      return Filter.or(super.getFilter(), SUPPLEMENT.getFilter(), RETURN.getFilter());
    }
  },

  SUPPLEMENT(Option.BUILD_INVOICES, Option.ENABLE_RETURN, Option.HAS_SERVICES, Option.SHOW_STOCK) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taKindSupplement();
    }

    @Override
    public String getGridSupplierKey() {
      return null;
    }
  },

  RETURN(EnumSet.noneOf(Option.class)) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taKindReturn();
    }

    @Override
    public String getGridSupplierKey() {
      return null;
    }
  },

  TENDER(Option.CONVERT_TO_SALE, Option.ENABLE_COPY, Option.HAS_SERVICES,
      Option.SAVE_AS_TEMPLATE, Option.SHOW_STOCK) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taKindTender();
    }
  },

  PURCHASE(Option.AUTO_NUMBER, Option.ENABLE_COPY, Option.SAVE_AS_TEMPLATE) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taKindPurchase();
    }
  },

  WRITE_OFF(Option.AUTO_NUMBER, Option.SHOW_STOCK) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taKindWriteOff();
    }

    @Override
    public String getStyleSuffix() {
      return "write-off";
    }
  },

  RESERVE(Option.CONVERT_TO_SALE, Option.SHOW_STOCK) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taKindReserve();
    }
  };

  private enum Option {
    AUTO_NUMBER,
    BUILD_INVOICES,
    CONVERT_TO_SALE,
    ENABLE_COPY,
    ENABLE_RETURN,
    ENABLE_SUPPLEMENT,
    HAS_SERVICES,
    SAVE_AS_TEMPLATE,
    SHOW_STOCK
  }

  public static Filter getFilterForInvoiceBuilder() {
    CompoundFilter filter = Filter.or();

    for (TradeActKind kind : values()) {
      if (kind.enableInvoices()) {
        filter.add(kind.getFilter());
      }
    }
    return filter;
  }

  private final EnumSet<Option> options;

  private TradeActKind(EnumSet<Option> options) {
    this.options = options;
  }

  private TradeActKind(Option first, Option... rest) {
    if (rest == null) {
      this.options = EnumSet.of(first);
    } else {
      this.options = EnumSet.of(first, rest);
    }
  }

  public boolean autoNumber() {
    return options.contains(Option.AUTO_NUMBER);
  }

  public boolean enableCopy() {
    return options.contains(Option.ENABLE_COPY);
  }

  public boolean enableInvoices() {
    return options.contains(Option.BUILD_INVOICES);
  }

  public boolean enableReturn() {
    return options.contains(Option.ENABLE_RETURN);
  }

  public boolean enableSale() {
    return options.contains(Option.CONVERT_TO_SALE);
  }

  public boolean enableServices() {
    return options.contains(Option.HAS_SERVICES);
  }

  public boolean enableSupplement() {
    return options.contains(Option.ENABLE_SUPPLEMENT);
  }

  public boolean enableTemplate() {
    return options.contains(Option.SAVE_AS_TEMPLATE);
  }

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }

  public Filter getFilter() {
    return Filter.equals(TradeActConstants.COL_TA_KIND, this);
  }

  public String getGridSupplierKey() {
    return TradeActConstants.GRID_TRADE_ACTS + BeeConst.STRING_UNDER + name().toLowerCase();
  }

  public String getStyleSuffix() {
    return name().toLowerCase();
  }

  public boolean showStock() {
    return options.contains(Option.SHOW_STOCK);
  }
}
