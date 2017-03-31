package com.butent.bee.shared.modules.trade;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.modules.finance.TradeAccounts;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.Map;

public class TradeDocumentItem implements BeeSerializable {

  private enum Serial {
    ID, VERSION, TRADE_DOCUMENT, ITEM, ARTICLE, QUANTITY, PRICE,
    DISCOUNT, DISCOUNT_IS_PERCENT, VAT, VAT_IS_PERCENT,
    ITEM_WAREHOUSE_FROM, ITEM_WAREHOUSE_TO, EMPLOYEE, ITEM_VEHICLE, NOTE, PARENT,
    EXTRA_DIMENSIONS, TRADE_ACCOUNTS
  }

  public static TradeDocumentItem restore(String s) {
    TradeDocumentItem tradeDocumentItem = new TradeDocumentItem();
    tradeDocumentItem.deserialize(s);
    return tradeDocumentItem;
  }

  private long id;
  private long version;

  private Long tradeDocument;

  private Long item;
  private String article;

  private Double quantity;
  private Double price;

  private Double discount;
  private Boolean discountIsPercent;

  private Double vat;
  private Boolean vatIsPercent;

  private Long itemWarehouseFrom;
  private Long itemWarehouseTo;

  private Long employee;
  private Long itemVehicle;

  private String note;

  private Long parent;

  private Dimensions extraDimensions;
  private TradeAccounts tradeAccounts;

  TradeDocumentItem(Long item, Double quantity) {
    this.item = item;
    this.quantity = quantity;

    this.id = DataUtils.NEW_ROW_ID;
    this.version = DataUtils.NEW_ROW_VERSION;
  }

  private TradeDocumentItem() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      String value = arr[i];

      if (!BeeUtils.isEmpty(value)) {
        switch (members[i]) {
          case ID:
            setId(BeeUtils.toLong(value));
            break;
          case VERSION:
            setVersion(BeeUtils.toLong(value));
            break;
          case TRADE_DOCUMENT:
            setTradeDocument(BeeUtils.toLongOrNull(value));
            break;
          case ITEM:
            setItem(BeeUtils.toLongOrNull(value));
            break;
          case ARTICLE:
            setArticle(value);
            break;
          case QUANTITY:
            setQuantity(BeeUtils.toDoubleOrNull(value));
            break;
          case PRICE:
            setPrice(BeeUtils.toDoubleOrNull(value));
            break;
          case DISCOUNT:
            setDiscount(BeeUtils.toDoubleOrNull(value));
            break;
          case DISCOUNT_IS_PERCENT:
            setDiscountIsPercent(BeeUtils.toBooleanOrNull(value));
            break;
          case VAT:
            setVat(BeeUtils.toDoubleOrNull(value));
            break;
          case VAT_IS_PERCENT:
            setVatIsPercent(BeeUtils.toBooleanOrNull(value));
            break;
          case ITEM_WAREHOUSE_FROM:
            setItemWarehouseFrom(BeeUtils.toLongOrNull(value));
            break;
          case ITEM_WAREHOUSE_TO:
            setItemWarehouseTo(BeeUtils.toLongOrNull(value));
            break;
          case EMPLOYEE:
            setEmployee(BeeUtils.toLongOrNull(value));
            break;
          case ITEM_VEHICLE:
            setItemVehicle(BeeUtils.toLongOrNull(value));
            break;
          case NOTE:
            setNote(value);
            break;
          case PARENT:
            setParent(BeeUtils.toLongOrNull(value));
            break;
          case EXTRA_DIMENSIONS:
            setExtraDimensions(Dimensions.restore(value));
            break;
          case TRADE_ACCOUNTS:
            setTradeAccounts(TradeAccounts.restore(value));
            break;
        }
      }
    }

  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ID:
          arr[i++] = getId();
          break;
        case VERSION:
          arr[i++] = getVersion();
          break;
        case TRADE_DOCUMENT:
          arr[i++] = getTradeDocument();
          break;
        case ITEM:
          arr[i++] = getItem();
          break;
        case ARTICLE:
          arr[i++] = getArticle();
          break;
        case QUANTITY:
          arr[i++] = getQuantity();
          break;
        case PRICE:
          arr[i++] = getPrice();
          break;
        case DISCOUNT:
          arr[i++] = getDiscount();
          break;
        case DISCOUNT_IS_PERCENT:
          arr[i++] = getDiscountIsPercent();
          break;
        case VAT:
          arr[i++] = getVat();
          break;
        case VAT_IS_PERCENT:
          arr[i++] = getVatIsPercent();
          break;
        case ITEM_WAREHOUSE_FROM:
          arr[i++] = getItemWarehouseFrom();
          break;
        case ITEM_WAREHOUSE_TO:
          arr[i++] = getItemWarehouseTo();
          break;
        case EMPLOYEE:
          arr[i++] = getEmployee();
          break;
        case ITEM_VEHICLE:
          arr[i++] = getItemVehicle();
          break;
        case NOTE:
          arr[i++] = getNote();
          break;
        case PARENT:
          arr[i++] = getParent();
          break;
        case EXTRA_DIMENSIONS:
          arr[i++] = getExtraDimensions();
          break;
        case TRADE_ACCOUNTS:
          arr[i++] = getTradeAccounts();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public Long getTradeDocument() {
    return tradeDocument;
  }

  public void setTradeDocument(Long tradeDocument) {
    this.tradeDocument = tradeDocument;
  }

  public Long getItem() {
    return item;
  }

  public void setItem(Long item) {
    this.item = item;
  }

  public String getArticle() {
    return article;
  }

  public void setArticle(String article) {
    this.article = article;
  }

  public Double getQuantity() {
    return quantity;
  }

  public void setQuantity(Double quantity) {
    this.quantity = quantity;
  }

  public Double getPrice() {
    return price;
  }

  public void setPrice(Double price) {
    this.price = price;
  }

  public Double getDiscount() {
    return discount;
  }

  public void setDiscount(Double discount) {
    this.discount = discount;
  }

  public Boolean getDiscountIsPercent() {
    return discountIsPercent;
  }

  public void setDiscountIsPercent(Boolean discountIsPercent) {
    this.discountIsPercent = discountIsPercent;
  }

  public Double getVat() {
    return vat;
  }

  public void setVat(Double vat) {
    this.vat = vat;
  }

  public Boolean getVatIsPercent() {
    return vatIsPercent;
  }

  public void setVatIsPercent(Boolean vatIsPercent) {
    this.vatIsPercent = vatIsPercent;
  }

  public Long getItemWarehouseFrom() {
    return itemWarehouseFrom;
  }

  public void setItemWarehouseFrom(Long itemWarehouseFrom) {
    this.itemWarehouseFrom = itemWarehouseFrom;
  }

  public Long getItemWarehouseTo() {
    return itemWarehouseTo;
  }

  public void setItemWarehouseTo(Long itemWarehouseTo) {
    this.itemWarehouseTo = itemWarehouseTo;
  }

  public Long getEmployee() {
    return employee;
  }

  public void setEmployee(Long employee) {
    this.employee = employee;
  }

  public Long getItemVehicle() {
    return itemVehicle;
  }

  public void setItemVehicle(Long itemVehicle) {
    this.itemVehicle = itemVehicle;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public Long getParent() {
    return parent;
  }

  public void setParent(Long parent) {
    this.parent = parent;
  }

  public Dimensions getExtraDimensions() {
    return extraDimensions;
  }

  public void setExtraDimensions(Dimensions extraDimensions) {
    this.extraDimensions = extraDimensions;
  }

  public TradeAccounts getTradeAccounts() {
    return tradeAccounts;
  }

  public void setTradeAccounts(TradeAccounts tradeAccounts) {
    this.tradeAccounts = tradeAccounts;
  }

  public boolean isValid() {
    return DataUtils.isId(getItem()) && BeeUtils.nonZero(getQuantity());
  }

  public Map<String, Value> getValues() {
    Map<String, Value> values = new HashMap<>();

    if (getTradeDocument() != null) {
      values.put(COL_TRADE_DOCUMENT, new LongValue(getTradeDocument()));
    }
    if (getItem() != null) {
      values.put(ClassifierConstants.COL_ITEM, new LongValue(getItem()));
    }

    if (!BeeUtils.isEmpty(getArticle())) {
      values.put(COL_TRADE_ITEM_ARTICLE, new TextValue(getArticle()));
    }

    if (BeeUtils.nonZero(getQuantity())) {
      values.put(COL_TRADE_ITEM_QUANTITY, new NumberValue(getQuantity()));
    }
    if (BeeUtils.nonZero(getPrice())) {
      values.put(COL_TRADE_ITEM_PRICE, new NumberValue(getPrice()));
    }

    if (BeeUtils.nonZero(getDiscount())) {
      values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT, new NumberValue(getDiscount()));
    }
    if (getDiscountIsPercent() != null) {
      values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
          BooleanValue.of(getDiscountIsPercent()));
    }

    if (BeeUtils.nonZero(getVat())) {
      values.put(COL_TRADE_DOCUMENT_ITEM_VAT, new NumberValue(getVat()));
    }
    if (getVatIsPercent() != null) {
      values.put(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT, BooleanValue.of(getVatIsPercent()));
    }

    if (getItemWarehouseFrom() != null) {
      values.put(COL_TRADE_ITEM_WAREHOUSE_FROM, new LongValue(getItemWarehouseFrom()));
    }
    if (getItemWarehouseTo() != null) {
      values.put(COL_TRADE_ITEM_WAREHOUSE_TO, new LongValue(getItemWarehouseTo()));
    }

    if (getEmployee() != null) {
      values.put(COL_TRADE_ITEM_EMPLOYEE, new LongValue(getEmployee()));
    }
    if (getItemVehicle() != null) {
      values.put(COL_TRADE_ITEM_VEHICLE, new LongValue(getItemVehicle()));
    }

    if (!BeeUtils.isEmpty(getNote())) {
      values.put(COL_TRADE_ITEM_NOTE, new TextValue(getNote()));
    }

    if (getParent() != null) {
      values.put(COL_TRADE_ITEM_PARENT, new LongValue(getParent()));
    }

    return values;
  }

  public Map<String, Value> getDimensionValues() {
    Map<String, Value> values = new HashMap<>();

    if (getExtraDimensions() != null) {
      getExtraDimensions().getRelationValues()
          .forEach((key, value) -> values.put(key, new LongValue(value)));
    }

    return values;
  }

  public Map<String, Value> getTradeAccountValues() {
    Map<String, Value> values = new HashMap<>();

    if (getTradeAccounts() != null) {
      getTradeAccounts().getValues()
          .forEach((key, value) -> values.put(key, new LongValue(value)));
    }

    return values;
  }
}