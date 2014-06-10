package com.butent.bee.shared.modules.ec;

import com.google.common.primitives.Longs;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;

public class EcCarType implements BeeSerializable {

  private enum Serial {
    MODEL_ID, MODEL_NAME, MANUFACTURER, TYPE_ID, TYPE_NAME,
    PRODUCED_FROM, PRODUCED_TO, CCM, KW_FROM, KW_TO, CYLINDERS, MAX_WEIGHT,
    ENGINE, FUEL, BODY, AXLE
  }

  public static EcCarType restore(String s) {
    EcCarType carType = new EcCarType();
    carType.deserialize(s);
    return carType;
  }

  private long modelId;
  private String modelName;
  private String manufacturer;

  private long typeId;
  private String typeName;

  private Integer producedFrom;
  private Integer producedTo;

  private Integer ccm;

  private Integer kwFrom;
  private Integer kwTo;

  private Integer cylinders;
  private Double maxWeight;

  private String engine;
  private String fuel;
  private String body;
  private String axle;

  public EcCarType(SimpleRow row) {
    this.modelId = row.getLong(COL_TCD_MODEL);
    this.modelName = row.getValue(COL_TCD_MODEL_NAME);
    this.manufacturer = row.getValue(COL_TCD_MANUFACTURER_NAME);

    this.typeId = row.getLong(COL_TCD_TYPE);
    this.typeName = row.getValue(COL_TCD_TYPE_NAME);

    this.producedFrom = row.getInt(COL_TCD_PRODUCED_FROM);
    this.producedTo = row.getInt(COL_TCD_PRODUCED_TO);

    this.ccm = row.getInt(COL_TCD_CCM);

    this.kwFrom = row.getInt(COL_TCD_KW_FROM);
    this.kwTo = row.getInt(COL_TCD_KW_TO);

    this.cylinders = row.getInt(COL_TCD_CYLINDERS);

    this.maxWeight = row.getDouble(COL_TCD_MAX_WEIGHT);

    this.engine = row.getValue(COL_TCD_ENGINE);
    this.fuel = row.getValue(COL_TCD_FUEL);
    this.body = row.getValue(COL_TCD_BODY);
    this.axle = row.getValue(COL_TCD_AXLE);
  }

  private EcCarType() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case AXLE:
          setAxle(value);
          break;

        case BODY:
          setBody(value);
          break;

        case CCM:
          setCcm(BeeUtils.toIntOrNull(value));
          break;

        case CYLINDERS:
          setCylinders(BeeUtils.toIntOrNull(value));
          break;

        case ENGINE:
          setEngine(value);
          break;

        case FUEL:
          setFuel(value);
          break;

        case KW_FROM:
          setKwFrom(BeeUtils.toIntOrNull(value));
          break;

        case KW_TO:
          setKwTo(BeeUtils.toIntOrNull(value));
          break;

        case MANUFACTURER:
          setManufacturer(value);
          break;

        case MAX_WEIGHT:
          setMaxWeight(BeeUtils.toDoubleOrNull(value));
          break;

        case MODEL_ID:
          setModelId(BeeUtils.toLong(value));
          break;

        case MODEL_NAME:
          setModelName(value);
          break;

        case PRODUCED_FROM:
          setProducedFrom(BeeUtils.toIntOrNull(value));
          break;

        case PRODUCED_TO:
          setProducedTo(BeeUtils.toIntOrNull(value));
          break;

        case TYPE_ID:
          setTypeId(BeeUtils.toLong(value));
          break;

        case TYPE_NAME:
          setTypeName(value);
          break;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof EcCarType) {
      return Objects.equals(getTypeId(), ((EcCarType) obj).getTypeId());
    } else {
      return false;
    }
  }

  public String getAxle() {
    return axle;
  }

  public String getBody() {
    return body;
  }

  public Integer getCcm() {
    return ccm;
  }

  public Integer getCylinders() {
    return cylinders;
  }

  public String getEngine() {
    return engine;
  }

  public String getFuel() {
    return fuel;
  }

  public String getInfo() {
    return BeeUtils.joinItems(getManufacturer(), getModelName(), getTypeName(),
        EcUtils.formatProduced(getProducedFrom(), getProducedTo()), getPower());
  }

  public Integer getKwFrom() {
    return kwFrom;
  }

  public Integer getKwTo() {
    return kwTo;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public Double getMaxWeight() {
    return maxWeight;
  }

  public long getModelId() {
    return modelId;
  }

  public String getModelName() {
    return modelName;
  }

  public String getPower() {
    if (BeeUtils.isPositive(getKwFrom())) {
      return BeeUtils.join(BeeConst.STRING_MINUS, getKwFrom(), getKwTo()) + " kW";
    } else {
      return null;
    }
  }

  public Integer getProducedFrom() {
    return producedFrom;
  }

  public Integer getProducedTo() {
    return producedTo;
  }

  public long getTypeId() {
    return typeId;
  }

  public String getTypeName() {
    return typeName;
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(getTypeId());
  }

  public boolean isProduced(int year) {
    return EcUtils.isProduced(getProducedFrom(), getProducedTo(), year);
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case AXLE:
          arr[i++] = getAxle();
          break;

        case BODY:
          arr[i++] = getBody();
          break;

        case CCM:
          arr[i++] = getCcm();
          break;

        case CYLINDERS:
          arr[i++] = getCylinders();
          break;

        case ENGINE:
          arr[i++] = getEngine();
          break;

        case FUEL:
          arr[i++] = getFuel();
          break;

        case KW_FROM:
          arr[i++] = getKwFrom();
          break;

        case KW_TO:
          arr[i++] = getKwTo();
          break;

        case MANUFACTURER:
          arr[i++] = getManufacturer();
          break;

        case MAX_WEIGHT:
          arr[i++] = getMaxWeight();
          break;

        case MODEL_ID:
          arr[i++] = getModelId();
          break;

        case MODEL_NAME:
          arr[i++] = getModelName();
          break;

        case PRODUCED_FROM:
          arr[i++] = getProducedFrom();
          break;

        case PRODUCED_TO:
          arr[i++] = getProducedTo();
          break;

        case TYPE_ID:
          arr[i++] = getTypeId();
          break;

        case TYPE_NAME:
          arr[i++] = getTypeName();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  public void setAxle(String axle) {
    this.axle = axle;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setCcm(Integer ccm) {
    this.ccm = ccm;
  }

  public void setCylinders(Integer cylinders) {
    this.cylinders = cylinders;
  }

  public void setEngine(String engine) {
    this.engine = engine;
  }

  public void setFuel(String fuel) {
    this.fuel = fuel;
  }

  public void setKwFrom(Integer kwFrom) {
    this.kwFrom = kwFrom;
  }

  public void setKwTo(Integer kwTo) {
    this.kwTo = kwTo;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public void setMaxWeight(Double maxWeight) {
    this.maxWeight = maxWeight;
  }

  public void setModelId(long modelId) {
    this.modelId = modelId;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public void setProducedFrom(Integer producedFrom) {
    this.producedFrom = producedFrom;
  }

  public void setProducedTo(Integer producedTo) {
    this.producedTo = producedTo;
  }

  public void setTypeId(long typeId) {
    this.typeId = typeId;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }
}
