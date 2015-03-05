package com.butent.bee.shared.imports;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.ui.HasLocalizedCaption;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum ImportType implements HasLocalizedCaption {
  COSTS(true) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trImportCosts();
    }

    @Override
    protected void init() {
      addRelationProperty(TransportConstants.COL_VEHICLE, TransportConstants.TBL_VEHICLES,
          TransportConstants.COL_VEHICLE_NUMBER);
      addDataProperty(TransportConstants.COL_COSTS_DATE);
      addRelationProperty(TransportConstants.COL_COSTS_ITEM, TBL_ITEMS, COL_ITEM_NAME);
      addDataProperty(TransportConstants.COL_COSTS_QUANTITY);
      addDataProperty(TransportConstants.COL_COSTS_PRICE);
      addRelationProperty(TransportConstants.COL_COSTS_CURRENCY, TBL_CURRENCIES, COL_CURRENCY_NAME);
      addDataProperty(TradeConstants.COL_TRADE_VAT_PLUS);
      addDataProperty(TransportConstants.COL_COSTS_VAT);
      addDataProperty(TradeConstants.COL_TRADE_VAT_PERC);
      addDataProperty(TransportConstants.COL_AMOUNT);
      addRelationProperty(TransportConstants.COL_COSTS_SUPPLIER, TBL_COMPANIES, COL_COMPANY_NAME);
      addDataProperty(TransportConstants.COL_NUMBER);
      addRelationProperty(TransportConstants.COL_COSTS_COUNTRY, TBL_COUNTRIES, COL_COUNTRY_NAME);
      addDataProperty(TransportConstants.COL_COSTS_NOTE);
      addDataProperty(TransportConstants.COL_COSTS_EXTERNAL_ID);
    }
  },
  TRACKING(false) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.trImportTracking();
    }

    @Override
    protected void init() {
      LocalizableConstants locale = Localized.getConstants();

      addSimpleProperty(VAR_IMPORT_LOGIN, locale.loginUserName());
      addSimpleProperty(VAR_IMPORT_PASSWORD, locale.loginPassword());
      addRelationProperty(TransportConstants.COL_VEHICLE, TransportConstants.TBL_VEHICLES,
          TransportConstants.COL_VEHICLE_NUMBER);
      addRelationProperty(COL_COUNTRY, TBL_COUNTRIES, COL_COUNTRY_NAME);
    }
  },
  DATA(true) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.data();
    }

    @Override
    protected void init() {
    }
  };

  private final Map<String, ImportProperty> properties = new LinkedHashMap<>();

  private ImportType(boolean xls) {
    if (xls) {
      addSimpleProperty(VAR_IMPORT_SHEET, Localized.getConstants().sheetName());
      addSimpleProperty(VAR_IMPORT_START_ROW, Localized.getConstants().startRow());
      addSimpleProperty(VAR_IMPORT_DATE_FORMAT, Localized.getConstants().dateFormat());
    }
    init();
  }

  protected ImportProperty addDataProperty(String name) {
    ImportProperty prop = new ImportProperty(name, null, true);
    properties.put(prop.getName(), prop);
    return prop;
  }

  protected void addRelationProperty(String name, String relTbl, String relFld) {
    ImportProperty prop = addDataProperty(name);
    prop.setRelTable(Assert.notEmpty(relTbl));
    prop.setRelField(Assert.notEmpty(relFld));
  }

  protected void addSimpleProperty(String name, String caption) {
    ImportProperty prop = new ImportProperty(name, caption, false);
    properties.put(prop.getName(), prop);
  }

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }

  public Collection<ImportProperty> getProperties() {
    return Collections.unmodifiableCollection(properties.values());
  }

  public ImportProperty getProperty(String name) {
    return properties.get(name);
  }

  protected abstract void init();
}