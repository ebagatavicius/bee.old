package com.butent.bee.shared.modules.trade;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.modules.finance.TradeAccounts;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeDocument implements BeeSerializable {

  private enum Serial {
    ID, VERSION, DATE, SERIES, NUMBER, NUMBER_1, NUMBER_2, OPERATION, PHASE, OWNER, STATUS,
    SUPPLIER, CUSTOMER, WAREHOUSE_FROM, WAREHOUSE_TO, CURRENCY, PAYER, TERM, MANAGER, VEHICLE,
    DOCUMENT_DISCOUNT, PRICE_NAME, DOCUMENT_VAT_MODE, DOCUMENT_DISCOUNT_MODE,
    RECEIVED_DATE, NOTES, EXTRA_DIMENSIONS, TRADE_ACCOUNTS, ITEMS
  }

  public static TradeDocument restore(String s) {
    TradeDocument tradeDocument = new TradeDocument();
    tradeDocument.deserialize(s);
    return tradeDocument;
  }

  private long id;
  private long version;

  private DateTime date;

  private String series;
  private String number;

  private String number1;
  private String number2;

  private Long operation;
  private TradeDocumentPhase phase;
  private Long owner;
  private Long status;

  private Long supplier;
  private Long customer;

  private Long warehouseFrom;
  private Long warehouseTo;

  private Long currency;

  private Long payer;
  private DateTime term;

  private Long manager;
  private Long vehicle;

  private Double documentDiscount;
  private ItemPrice priceName;

  private TradeVatMode documentVatMode;
  private TradeDiscountMode documentDiscountMode;

  private DateTime receivedDate;

  private String notes;

  private Dimensions extraDimensions;
  private TradeAccounts tradeAccounts;

  private final List<TradeDocumentItem> items = new ArrayList<>();

  public TradeDocument(Long operation, TradeDocumentPhase phase) {
    this.operation = operation;
    this.phase = phase;

    this.id = DataUtils.NEW_ROW_ID;
    this.version = DataUtils.NEW_ROW_VERSION;
  }

  private TradeDocument() {
  }

  public TradeDocumentItem addItem(Long item, Double quantity) {
    TradeDocumentItem tradeItem = new TradeDocumentItem(item, quantity);
    items.add(tradeItem);
    return tradeItem;
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
          case DATE:
            setDate(DateTime.restore(value));
            break;
          case SERIES:
            setSeries(value);
            break;
          case NUMBER:
            setNumber(value);
            break;
          case NUMBER_1:
            setNumber1(value);
            break;
          case NUMBER_2:
            setNumber2(value);
            break;
          case OPERATION:
            setOperation(BeeUtils.toLongOrNull(value));
            break;
          case PHASE:
            setPhase(Codec.unpack(TradeDocumentPhase.class, value));
            break;
          case OWNER:
            setOwner(BeeUtils.toLongOrNull(value));
            break;
          case STATUS:
            setStatus(BeeUtils.toLongOrNull(value));
            break;
          case SUPPLIER:
            setSupplier(BeeUtils.toLongOrNull(value));
            break;
          case CUSTOMER:
            setCustomer(BeeUtils.toLongOrNull(value));
            break;
          case WAREHOUSE_FROM:
            setWarehouseFrom(BeeUtils.toLongOrNull(value));
            break;
          case WAREHOUSE_TO:
            setWarehouseTo(BeeUtils.toLongOrNull(value));
            break;
          case CURRENCY:
            setCurrency(BeeUtils.toLongOrNull(value));
            break;
          case PAYER:
            setPayer(BeeUtils.toLongOrNull(value));
            break;
          case TERM:
            setTerm(DateTime.restore(value));
            break;
          case MANAGER:
            setManager(BeeUtils.toLongOrNull(value));
            break;
          case VEHICLE:
            setVehicle(BeeUtils.toLongOrNull(value));
            break;
          case DOCUMENT_DISCOUNT:
            setDocumentDiscount(BeeUtils.toDoubleOrNull(value));
            break;
          case PRICE_NAME:
            setPriceName(Codec.unpack(ItemPrice.class, value));
            break;
          case DOCUMENT_VAT_MODE:
            setDocumentVatMode(Codec.unpack(TradeVatMode.class, value));
            break;
          case DOCUMENT_DISCOUNT_MODE:
            setDocumentDiscountMode(Codec.unpack(TradeDiscountMode.class, value));
            break;
          case RECEIVED_DATE:
            setReceivedDate(DateTime.restore(value));
            break;
          case NOTES:
            setNotes(value);
            break;
          case EXTRA_DIMENSIONS:
            setExtraDimensions(Dimensions.restore(value));
            break;
          case TRADE_ACCOUNTS:
            setTradeAccounts(TradeAccounts.restore(value));
            break;
          case ITEMS:
            items.clear();

            for (String item : Codec.beeDeserializeCollection(value)) {
              items.add(TradeDocumentItem.restore(item));
            }
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
        case DATE:
          arr[i++] = getDate();
          break;
        case SERIES:
          arr[i++] = getSeries();
          break;
        case NUMBER:
          arr[i++] = getNumber();
          break;
        case NUMBER_1:
          arr[i++] = getNumber1();
          break;
        case NUMBER_2:
          arr[i++] = getNumber2();
          break;
        case OPERATION:
          arr[i++] = getOperation();
          break;
        case PHASE:
          arr[i++] = Codec.pack(getPhase());
          break;
        case OWNER:
          arr[i++] = getOwner();
          break;
        case STATUS:
          arr[i++] = getStatus();
          break;
        case SUPPLIER:
          arr[i++] = getSupplier();
          break;
        case CUSTOMER:
          arr[i++] = getCustomer();
          break;
        case WAREHOUSE_FROM:
          arr[i++] = getWarehouseFrom();
          break;
        case WAREHOUSE_TO:
          arr[i++] = getWarehouseTo();
          break;
        case CURRENCY:
          arr[i++] = getCurrency();
          break;
        case PAYER:
          arr[i++] = getPayer();
          break;
        case TERM:
          arr[i++] = getTerm();
          break;
        case MANAGER:
          arr[i++] = getManager();
          break;
        case VEHICLE:
          arr[i++] = getVehicle();
          break;
        case DOCUMENT_DISCOUNT:
          arr[i++] = getDocumentDiscount();
          break;
        case PRICE_NAME:
          arr[i++] = Codec.pack(getPriceName());
          break;
        case DOCUMENT_VAT_MODE:
          arr[i++] = Codec.pack(getDocumentVatMode());
          break;
        case DOCUMENT_DISCOUNT_MODE:
          arr[i++] = Codec.pack(getDocumentDiscountMode());
          break;
        case RECEIVED_DATE:
          arr[i++] = getReceivedDate();
          break;
        case NOTES:
          arr[i++] = getNotes();
          break;
        case EXTRA_DIMENSIONS:
          arr[i++] = getExtraDimensions();
          break;
        case TRADE_ACCOUNTS:
          arr[i++] = getTradeAccounts();
          break;
        case ITEMS:
          arr[i++] = getItems();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  public List<TradeDocumentItem> getItems() {
    return items;
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

  public DateTime getDate() {
    return date;
  }

  public void setDate(DateTime date) {
    this.date = date;
  }

  public String getSeries() {
    return series;
  }

  public void setSeries(String series) {
    this.series = series;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public String getNumber1() {
    return number1;
  }

  public void setNumber1(String number1) {
    this.number1 = number1;
  }

  public String getNumber2() {
    return number2;
  }

  public void setNumber2(String number2) {
    this.number2 = number2;
  }

  public Long getOperation() {
    return operation;
  }

  public void setOperation(Long operation) {
    this.operation = operation;
  }

  public TradeDocumentPhase getPhase() {
    return phase;
  }

  public void setPhase(TradeDocumentPhase phase) {
    this.phase = phase;
  }

  public Long getOwner() {
    return owner;
  }

  public void setOwner(Long owner) {
    this.owner = owner;
  }

  public Long getStatus() {
    return status;
  }

  public void setStatus(Long status) {
    this.status = status;
  }

  public Long getSupplier() {
    return supplier;
  }

  public void setSupplier(Long supplier) {
    this.supplier = supplier;
  }

  public Long getCustomer() {
    return customer;
  }

  public void setCustomer(Long customer) {
    this.customer = customer;
  }

  public Long getWarehouseFrom() {
    return warehouseFrom;
  }

  public void setWarehouseFrom(Long warehouseFrom) {
    this.warehouseFrom = warehouseFrom;
  }

  public Long getWarehouseTo() {
    return warehouseTo;
  }

  public void setWarehouseTo(Long warehouseTo) {
    this.warehouseTo = warehouseTo;
  }

  public Long getCurrency() {
    return currency;
  }

  public void setCurrency(Long currency) {
    this.currency = currency;
  }

  public Long getPayer() {
    return payer;
  }

  public void setPayer(Long payer) {
    this.payer = payer;
  }

  public DateTime getTerm() {
    return term;
  }

  public void setTerm(DateTime term) {
    this.term = term;
  }

  public Long getManager() {
    return manager;
  }

  public void setManager(Long manager) {
    this.manager = manager;
  }

  public Long getVehicle() {
    return vehicle;
  }

  public void setVehicle(Long vehicle) {
    this.vehicle = vehicle;
  }

  public Double getDocumentDiscount() {
    return documentDiscount;
  }

  public void setDocumentDiscount(Double documentDiscount) {
    this.documentDiscount = documentDiscount;
  }

  public ItemPrice getPriceName() {
    return priceName;
  }

  public void setPriceName(ItemPrice priceName) {
    this.priceName = priceName;
  }

  public TradeVatMode getDocumentVatMode() {
    return documentVatMode;
  }

  public void setDocumentVatMode(TradeVatMode documentVatMode) {
    this.documentVatMode = documentVatMode;
  }

  public TradeDiscountMode getDocumentDiscountMode() {
    return documentDiscountMode;
  }

  public void setDocumentDiscountMode(TradeDiscountMode documentDiscountMode) {
    this.documentDiscountMode = documentDiscountMode;
  }

  public DateTime getReceivedDate() {
    return receivedDate;
  }

  public void setReceivedDate(DateTime receivedDate) {
    this.receivedDate = receivedDate;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
    return DataUtils.isId(getOperation()) && getPhase() != null && !items.isEmpty();
  }

  public Map<String, Value> getValues() {
    Map<String, Value> values = new HashMap<>();

    if (getDate() != null) {
      values.put(COL_TRADE_DATE, new DateTimeValue(getDate()));
    }

    if (!BeeUtils.isEmpty(getSeries())) {
      values.put(COL_TRADE_SERIES, new TextValue(getSeries()));
    }
    if (!BeeUtils.isEmpty(getNumber())) {
      values.put(COL_TRADE_NUMBER, new TextValue(getNumber()));
    }

    if (!BeeUtils.isEmpty(getNumber1())) {
      values.put(COL_TRADE_DOCUMENT_NUMBER_1, new TextValue(getNumber1()));
    }
    if (!BeeUtils.isEmpty(getNumber2())) {
      values.put(COL_TRADE_DOCUMENT_NUMBER_2, new TextValue(getNumber2()));
    }

    if (getOperation() != null) {
      values.put(COL_TRADE_OPERATION, new LongValue(getOperation()));
    }
    if (getPhase() != null) {
      values.put(COL_TRADE_DOCUMENT_PHASE, IntegerValue.of(getPhase()));
    }
    if (getOwner() != null) {
      values.put(COL_TRADE_DOCUMENT_OWNER, new LongValue(getOwner()));
    }
    if (getStatus() != null) {
      values.put(COL_TRADE_DOCUMENT_STATUS, new LongValue(getStatus()));
    }

    if (getSupplier() != null) {
      values.put(COL_TRADE_SUPPLIER, new LongValue(getSupplier()));
    }
    if (getCustomer() != null) {
      values.put(COL_TRADE_CUSTOMER, new LongValue(getCustomer()));
    }

    if (getWarehouseFrom() != null) {
      values.put(COL_TRADE_WAREHOUSE_FROM, new LongValue(getWarehouseFrom()));
    }
    if (getWarehouseTo() != null) {
      values.put(COL_TRADE_WAREHOUSE_TO, new LongValue(getWarehouseTo()));
    }

    if (getCurrency() != null) {
      values.put(COL_TRADE_CURRENCY, new LongValue(getCurrency()));
    }

    if (getPayer() != null) {
      values.put(COL_TRADE_PAYER, new LongValue(getPayer()));
    }
    if (getTerm() != null) {
      values.put(COL_TRADE_TERM, new DateTimeValue(getTerm()));
    }

    if (getManager() != null) {
      values.put(COL_TRADE_MANAGER, new LongValue(getManager()));
    }
    if (getVehicle() != null) {
      values.put(COL_TRADE_VEHICLE, new LongValue(getVehicle()));
    }

    if (BeeUtils.nonZero(getDocumentDiscount())) {
      values.put(COL_TRADE_DOCUMENT_DISCOUNT, new NumberValue(getDocumentDiscount()));
    }
    if (getPriceName() != null) {
      values.put(COL_TRADE_DOCUMENT_PRICE_NAME, IntegerValue.of(getPriceName()));
    }

    if (getDocumentVatMode() != null) {
      values.put(COL_TRADE_DOCUMENT_VAT_MODE, IntegerValue.of(getDocumentVatMode()));
    }
    if (getDocumentDiscountMode() != null) {
      values.put(COL_TRADE_DOCUMENT_DISCOUNT_MODE, IntegerValue.of(getDocumentDiscountMode()));
    }

    if (getReceivedDate() != null) {
      values.put(COL_TRADE_DOCUMENT_RECEIVED_DATE, new DateTimeValue(getReceivedDate()));
    }

    if (!BeeUtils.isEmpty(getNotes())) {
      values.put(COL_TRADE_NOTES, new TextValue(getNotes()));
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
