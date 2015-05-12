package com.butent.bee.shared.imports;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.HasLocalizedCaption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public enum ImportType implements HasLocalizedCaption {
  COSTS {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trImportCosts();
    }

    @Override
    protected void init() {
      LocalizableConstants locale = Localized.getConstants();

      addSimpleProperty(VAR_IMPORT_SHEET, locale.sheetName());
      addSimpleProperty(VAR_IMPORT_START_ROW, locale.startRow());
      addSimpleProperty(VAR_IMPORT_DATE_FORMAT, locale.dateFormat());
      addRelationProperty(COL_VEHICLE, locale.trVehicle(), TBL_VEHICLES);
      addDataProperty(COL_COSTS_DATE, locale.date());
      addRelationProperty(COL_COSTS_ITEM, locale.item(), TBL_ITEMS);
      addDataProperty(COL_COSTS_QUANTITY, locale.quantity());
      addDataProperty(COL_COSTS_PRICE, locale.price());
      addRelationProperty(COL_COSTS_CURRENCY, locale.currency(), TBL_CURRENCIES);
      addDataProperty(TradeConstants.COL_TRADE_VAT_PLUS, locale.vatPlus());
      addDataProperty(COL_COSTS_VAT, locale.vat());
      addDataProperty(TradeConstants.COL_TRADE_VAT_PERC, locale.vatPercent());
      addDataProperty(COL_AMOUNT, locale.amount());
      addRelationProperty(COL_COSTS_SUPPLIER, locale.supplier(), TBL_COMPANIES);
      addDataProperty(COL_NUMBER, locale.number());
      addRelationProperty(COL_COSTS_COUNTRY, locale.country(), TBL_COUNTRIES);
      addDataProperty(COL_COSTS_NOTE, locale.note());
      addDataProperty(COL_COSTS_EXTERNAL_ID, locale.externalId());
    }
  },
  TRACKING {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trImportTracking();
    }

    @Override
    protected void init() {
      LocalizableConstants locale = Localized.getConstants();

      addSimpleProperty(VAR_IMPORT_LOGIN, locale.loginUserName());
      addSimpleProperty(VAR_IMPORT_PASSWORD, locale.loginPassword());
    }
  },
  DATA {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.data();
    }

    @Override
    protected void init() {
      addSimpleProperty(VAR_IMPORT_SHEET, Localized.getConstants().sheetName());
      addSimpleProperty(VAR_IMPORT_START_ROW, Localized.getConstants().startRow());
    }
  };

  private final List<ImportProperty> properties = new ArrayList<>();

  private ImportType() {
    init();
  }

  protected ImportProperty addDataProperty(String name, String caption) {
    ImportProperty prop = new ImportProperty(name, caption, true);
    properties.add(prop);
    return prop;
  }

  protected void addRelationProperty(String name, String caption, String relation) {
    ImportProperty prop = addDataProperty(name, caption);
    prop.setRelation(Assert.notEmpty(relation));
  }

  protected void addSimpleProperty(String name, String caption) {
    properties.add(new ImportProperty(name, caption, false));
  }

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }

  public Collection<ImportProperty> getProperties() {
    return Collections.unmodifiableCollection(properties);
  }

  protected abstract void init();
}