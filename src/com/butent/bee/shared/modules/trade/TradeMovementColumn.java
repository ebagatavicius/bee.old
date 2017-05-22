package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class TradeMovementColumn implements BeeSerializable {

  private enum Serial {
    LABEL, OPERATION_TYPE, OPERATION, OPERATION_NAME,
    WAREHOUSE_FROM, WAREHOUSE_FROM_CODE, WAREHOUSE_TO, WAREHOUSE_TO_CODE
  }

  public static TradeMovementColumn restore(String s) {
    TradeMovementColumn tmc = new TradeMovementColumn();
    tmc.deserialize(s);
    return tmc;
  }

  public static List<TradeMovementColumn> restoreList(String input) {
    return Codec.deserializeList(input).stream()
        .map(TradeMovementColumn::restore)
        .collect(Collectors.toList());
  }

  private static final String SUFFIX_QUANTITY = "_qty";
  private static final String SUFFIX_AMOUNT = "_amt";

  private String label;

  private OperationType operationType;

  private Long operation;
  private String operationName;

  private Long warehouseFrom;
  private String warehouseFromCode;

  private Long warehouseTo;
  private String warehouseToCode;

  private TradeMovementColumn() {
  }

  public TradeMovementColumn(String label) {
    this.label = label;
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
          case LABEL:
            setLabel(value);
            break;

          case OPERATION_TYPE:
            setOperationType(Codec.unpack(OperationType.class, value));
            break;

          case OPERATION:
            setOperation(BeeUtils.toLongOrNull(value));
            break;
          case OPERATION_NAME:
            setOperationName(value);
            break;

          case WAREHOUSE_FROM:
            setWarehouseFrom(BeeUtils.toLongOrNull(value));
            break;
          case WAREHOUSE_FROM_CODE:
            setWarehouseFromCode(value);
            break;

          case WAREHOUSE_TO:
            setWarehouseTo(BeeUtils.toLongOrNull(value));
            break;
          case WAREHOUSE_TO_CODE:
            setWarehouseToCode(value);
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
        case LABEL:
          arr[i++] = getLabel();
          break;

        case OPERATION_TYPE:
          arr[i++] = Codec.pack(getOperationType());
          break;

        case OPERATION:
          arr[i++] = getOperation();
          break;
        case OPERATION_NAME:
          arr[i++] = getOperationName();
          break;

        case WAREHOUSE_FROM:
          arr[i++] = getWarehouseFrom();
          break;
        case WAREHOUSE_FROM_CODE:
          arr[i++] = getWarehouseFromCode();
          break;

        case WAREHOUSE_TO:
          arr[i++] = getWarehouseTo();
          break;
        case WAREHOUSE_TO_CODE:
          arr[i++] = getWarehouseToCode();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  public List<String> getCaptions() {
    List<String> captions = new ArrayList<>();

    if (getOperationType() != null) {
      captions.add(getOperationType().getCaption());
    }

    if (!BeeUtils.isEmpty(getOperationName())) {
      captions.add(getOperationName());
    }

    if (BeeUtils.anyNotEmpty(getWarehouseFromCode(), getWarehouseToCode())) {
      captions.add(BeeUtils.joinWords(getWarehouseFromCode(), getWarehouseToCode()));
    }

    return captions;
  }

  public String getQuantityColumn() {
    return getLabel() + SUFFIX_QUANTITY;
  }

  public String getAmountColumn() {
    return getLabel() + SUFFIX_AMOUNT;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  public Long getOperation() {
    return operation;
  }

  public void setOperation(Long operation) {
    this.operation = operation;
  }

  public String getOperationName() {
    return operationName;
  }

  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  public Long getWarehouseFrom() {
    return warehouseFrom;
  }

  public void setWarehouseFrom(Long warehouseFrom) {
    this.warehouseFrom = warehouseFrom;
  }

  public String getWarehouseFromCode() {
    return warehouseFromCode;
  }

  public void setWarehouseFromCode(String warehouseFromCode) {
    this.warehouseFromCode = warehouseFromCode;
  }

  public Long getWarehouseTo() {
    return warehouseTo;
  }

  public void setWarehouseTo(Long warehouseTo) {
    this.warehouseTo = warehouseTo;
  }

  public String getWarehouseToCode() {
    return warehouseToCode;
  }

  public void setWarehouseToCode(String warehouseToCode) {
    this.warehouseToCode = warehouseToCode;
  }
}
