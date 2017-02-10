package com.butent.bee.shared.modules.cars;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_PHOTO;

import com.butent.bee.client.data.Data;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;

public class Option implements BeeSerializable, Comparable<Option> {

  private enum Serial {
    ID, NAME, DIMENSION, CODE, DESCRIPTION, PHOTO
  }

  private long id;
  private String name;
  private Dimension dimension;
  private String code;
  private String description;
  private Long photo;

  public Option(IsRow isRow) {
    this(isRow.getId(),
        Data.getString(TBL_CONF_OPTIONS, isRow, COL_OPTION_NAME),
        new Dimension(Data.getLong(TBL_CONF_OPTIONS, isRow, COL_GROUP),
            Data.getString(TBL_CONF_OPTIONS, isRow, COL_GROUP_NAME)));

    setCode(BeeUtils.join("", Data.getString(TBL_CONF_OPTIONS, isRow, COL_CODE),
        BeeUtils.parenthesize(Data.getString(TBL_CONF_OPTIONS, isRow, COL_CODE2))));
  }

  public Option(SimpleRowSet.SimpleRow simpleRow) {
    this(simpleRow.getLong(COL_OPTION), simpleRow.getValue(COL_OPTION_NAME),
        new Dimension(simpleRow.getLong(CarsConstants.COL_GROUP),
            simpleRow.getValue(CarsConstants.COL_GROUP_NAME)));

    if (simpleRow.hasColumn(COL_REQUIRED)) {
      getDimension().setRequired(simpleRow.getBoolean(COL_REQUIRED));
    }
    if (simpleRow.hasColumn(COL_CODE)) {
      setCode(simpleRow.getValue(COL_CODE));
    }
    if (simpleRow.hasColumn(COL_CODE2)) {
      setCode(BeeUtils.join("", getCode(), BeeUtils.parenthesize(simpleRow.getValue(COL_CODE2))));
    }
    if (simpleRow.hasColumn(COL_DESCRIPTION)) {
      setDescription(simpleRow.getValue(COL_DESCRIPTION));
    }
    if (simpleRow.hasColumn(COL_PHOTO)) {
      setPhoto(simpleRow.getLong(COL_PHOTO));
    }
  }

  public Option(long id, String name, Dimension dimension) {
    this.id = id;
    this.name = Assert.notEmpty(name);
    this.dimension = Assert.notNull(dimension);
  }

  private Option() {
  }

  @Override
  public int compareTo(Option o) {
    int order = BeeUtils.compareNullsFirst(getDimension(),
        Objects.nonNull(o) ? o.getDimension() : null);

    if (order == BeeConst.COMPARE_EQUAL) {
      order = BeeUtils.compareNullsFirst(name, o.name);
    }
    return order == BeeConst.COMPARE_EQUAL ? BeeUtils.compareNullsFirst(id, o.id) : order;
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
        case ID:
          this.id = BeeUtils.toLong(value);
          break;
        case NAME:
          this.name = value;
          break;
        case DIMENSION:
          this.dimension = Dimension.restore(value);
          break;
        case CODE:
          this.code = value;
          break;
        case DESCRIPTION:
          this.description = value;
          break;
        case PHOTO:
          this.photo = BeeUtils.toLongOrNull(value);
          break;
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return id == ((Option) o).id;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public Dimension getDimension() {
    return dimension;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Long getPhoto() {
    return photo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public static Option restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    Option option = new Option();
    option.deserialize(s);
    return option;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ID:
          arr[i++] = id;
          break;
        case NAME:
          arr[i++] = name;
          break;
        case DIMENSION:
          arr[i++] = dimension;
          break;
        case CODE:
          arr[i++] = code;
          break;
        case DESCRIPTION:
          arr[i++] = description;
          break;
        case PHOTO:
          arr[i++] = photo;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public Option setCode(String c) {
    this.code = c;
    return this;
  }

  public Option setDescription(String descr) {
    this.description = descr;
    return this;
  }

  public Option setPhoto(Long ph) {
    this.photo = ph;
    return this;
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords(code, name);
  }
}
