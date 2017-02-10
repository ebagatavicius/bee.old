package com.butent.bee.shared.imports;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.cars.CarsConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.HasLocalizedCaption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public enum ImportType implements HasLocalizedCaption {
  COSTS {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trImportCosts();
    }

    @Override
    protected void init() {
      Dictionary locale = Localized.dictionary();

      addSimpleProperty(VAR_IMPORT_SHEET, locale.sheetName());
      addSimpleProperty(VAR_IMPORT_START_ROW, locale.startRow());
      addSimpleProperty(VAR_IMPORT_END_ROW, locale.endRow());
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
    public String getCaption(Dictionary constants) {
      return constants.trImportTracking();
    }

    @Override
    protected void init() {
      Dictionary locale = Localized.dictionary();

      addSimpleProperty(VAR_IMPORT_LOGIN, locale.loginUserName());
      addSimpleProperty(VAR_IMPORT_PASSWORD, locale.loginPassword());
    }
  },
  DATA {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.data();
    }

    @Override
    protected void init() {
      addSimpleProperty(VAR_IMPORT_SHEET, Localized.dictionary().sheetName());
      addSimpleProperty(VAR_IMPORT_START_ROW, Localized.dictionary().startRow());
      addSimpleProperty(VAR_IMPORT_END_ROW, Localized.dictionary().endRow());
    }
  },
  CONFIGURATION {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.configuration();
    }

    @Override
    protected void init() {
      Dictionary locale = Localized.dictionary();
      String prfx = CarsConstants.TBL_CONF_BRANCH_BUNDLES;
      String capPrfx = locale.configuration() + ": ";

      addRelationProperty(CarsConstants.TBL_CONF_OPTIONS, locale.options(),
          CarsConstants.TBL_CONF_OPTIONS);
      addSimpleProperty(prfx + VAR_IMPORT_SHEET, capPrfx + locale.sheetName());
      addSimpleProperty(prfx + VAR_IMPORT_START_ROW, capPrfx + locale.startRow());
      addSimpleProperty(prfx + VAR_IMPORT_END_ROW, capPrfx + locale.endRow());
      addSimpleProperty(prfx + CarsConstants.TBL_CONF_OPTIONS, capPrfx + locale.options()
          + " (" + locale.code() + ",...)");
      addDataProperty(prfx + CarsConstants.COL_PRICE, capPrfx + locale.price());
      addDataProperty(prfx + CarsConstants.COL_DESCRIPTION, capPrfx + locale.description());
      addSimpleProperty(prfx + CarsConstants.COL_CRITERIA + "Names", capPrfx + locale.criteria()
          + " (" + locale.name() + ",...)");
      addSimpleProperty(prfx + CarsConstants.COL_CRITERIA + "Values", capPrfx + locale.criteria()
          + " (" + locale.value() + ",...)");

      prfx = CarsConstants.TBL_CONF_BRANCH_OPTIONS;
      capPrfx = locale.options() + ": ";
      addSimpleProperty(prfx + VAR_IMPORT_SHEET, capPrfx + locale.sheetName());
      addSimpleProperty(prfx + VAR_IMPORT_START_ROW, capPrfx + locale.startRow());
      addSimpleProperty(prfx + VAR_IMPORT_END_ROW, capPrfx + locale.endRow());
      addDataProperty(prfx + CarsConstants.COL_CODE, capPrfx + locale.code());
      addDataProperty(prfx + CarsConstants.COL_PRICE, capPrfx + locale.price());
      addDataProperty(prfx + CarsConstants.COL_DESCRIPTION, capPrfx + locale.description());
      addSimpleProperty(prfx + CarsConstants.COL_CRITERIA + "Names", capPrfx + locale.criteria()
          + " (" + locale.name() + ",...)");
      addSimpleProperty(prfx + CarsConstants.COL_CRITERIA + "Values", capPrfx + locale.criteria()
          + " (" + locale.value() + ",...)");

      prfx = CarsConstants.TBL_CONF_RELATIONS;
      capPrfx = locale.relations() + ": ";
      addSimpleProperty(prfx + VAR_IMPORT_SHEET, capPrfx + locale.sheetName());
      addSimpleProperty(prfx + VAR_IMPORT_START_ROW, capPrfx + locale.startRow());
      addSimpleProperty(prfx + VAR_IMPORT_END_ROW, capPrfx + locale.endRow());
      addSimpleProperty(prfx + CarsConstants.TBL_CONF_OPTIONS, capPrfx + locale.options()
          + " (" + locale.code() + ",...)");
      addDataProperty(prfx + CarsConstants.COL_CODE, capPrfx + locale.code());
      addDataProperty(prfx + CarsConstants.COL_PRICE, capPrfx + locale.price());
      addSimpleProperty(prfx + CarsConstants.VAR_PRICE_DEFAULT, capPrfx + locale.price()
          + " <span style=\"font-family:" + FontAwesome.class.getSimpleName() + ";\">"
          + FontAwesome.CIRCLE.getCode() + "</span>");
      addSimpleProperty(prfx + CarsConstants.VAR_PRICE_OPTIONAL, capPrfx + locale.price()
          + " <span style=\"font-family:" + FontAwesome.class.getSimpleName() + ";\">"
          + FontAwesome.CIRCLE_THIN.getCode() + "</span>");
      addDataProperty(prfx + CarsConstants.COL_DESCRIPTION, capPrfx + locale.description());
      addSimpleProperty(prfx + CarsConstants.COL_CRITERIA + "Names", capPrfx + locale.criteria()
          + " (" + locale.name() + ",...)");
      addSimpleProperty(prfx + CarsConstants.COL_CRITERIA + "Values", capPrfx + locale.criteria()
          + " (" + locale.value() + ",...)");
      addDataProperty(prfx + CarsConstants.COL_PACKET, capPrfx + locale.packet()
          + " (<span style=\"font-family:" + FontAwesome.class.getSimpleName() + "; color:red;\">"
          + FontAwesome.BAN.getCode() + "</span> " + locale.code() + ",...)");

      prfx = CarsConstants.TBL_CONF_RESTRICTIONS;
      capPrfx = locale.restrictions() + ": ";
      addSimpleProperty(prfx + VAR_IMPORT_SHEET, capPrfx + locale.sheetName());
      addSimpleProperty(prfx + VAR_IMPORT_START_ROW, capPrfx + locale.startRow());
      addSimpleProperty(prfx + VAR_IMPORT_END_ROW, capPrfx + locale.endRow());
      addDataProperty(prfx + CarsConstants.COL_CODE + 1, capPrfx + locale.code() + 1);
      addDataProperty(prfx + CarsConstants.COL_CODE + 2, capPrfx + locale.code() + 2);
      addDataProperty(prfx + CarsConstants.COL_DENIED, capPrfx + locale.status());
      addSimpleProperty(prfx + CarsConstants.VAR_REL_REQUIRED, capPrfx + locale.status()
          + " <span style=\"font-family:" + FontAwesome.class.getSimpleName() + "; color:green;\">"
          + FontAwesome.PLUS_CIRCLE.getCode() + "</span>");
      addSimpleProperty(prfx + CarsConstants.VAR_REL_DENIED, capPrfx + locale.status()
          + " <span style=\"font-family:" + FontAwesome.class.getSimpleName() + "; color:red;\">"
          + FontAwesome.BAN.getCode() + "</span>");
    }
  };

  private final List<ImportProperty> properties = new ArrayList<>();

  ImportType() {
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

  public Collection<ImportProperty> getProperties() {
    return Collections.unmodifiableCollection(properties);
  }

  protected abstract void init();
}