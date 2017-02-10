package com.butent.bee.shared.modules.finance.analysis;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.time.YearQuarter;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AnalysisSplitValue implements BeeSerializable, Comparable<AnalysisSplitValue> {

  public static AnalysisSplitValue absent() {
    return new AnalysisSplitValue();
  }

  public static AnalysisSplitValue of(Integer v) {
    if (v == null) {
      return new AnalysisSplitValue();
    } else {
      return new AnalysisSplitValue(BeeUtils.toString(v));
    }
  }

  public static AnalysisSplitValue of(YearMonth ym) {
    if (ym == null) {
      return new AnalysisSplitValue();
    } else {
      return new AnalysisSplitValue(ym.serialize());
    }
  }

  public static AnalysisSplitValue of(YearQuarter yq) {
    if (yq == null) {
      return new AnalysisSplitValue();
    } else {
      return new AnalysisSplitValue(yq.serialize());
    }
  }

  public static AnalysisSplitValue of(String value, Long id) {
    return new AnalysisSplitValue(value, id);
  }

  public static AnalysisSplitValue restore(String s) {
    AnalysisSplitValue asv = new AnalysisSplitValue();
    asv.deserialize(s);
    return asv;
  }

  public static List<Map<AnalysisSplitType, AnalysisSplitValue>> getPermutations(
      Map<AnalysisSplitType, AnalysisSplitValue> parent,
      List<AnalysisSplitType> splitTypes, int typeIndex,
      Map<AnalysisSplitType, List<AnalysisSplitValue>> splitValues, int valueIndex) {

    List<Map<AnalysisSplitType, AnalysisSplitValue>> result = new ArrayList<>();
    if (BeeUtils.isEmpty(splitTypes) || BeeUtils.isEmpty(splitValues)) {
      return result;
    }

    AnalysisSplitType splitType = BeeUtils.getQuietly(splitTypes, typeIndex);
    if (splitType == null) {
      return result;
    }

    List<AnalysisSplitValue> typeValues = splitValues.get(splitType);
    AnalysisSplitValue splitValue = BeeUtils.getQuietly(typeValues, valueIndex);
    if (splitValue == null) {
      return result;
    }

    Map<AnalysisSplitType, AnalysisSplitValue> map = new EnumMap<>(AnalysisSplitType.class);
    if (!BeeUtils.isEmpty(parent)) {
      map.putAll(parent);
    }

    map.put(splitType, splitValue);

    if (typeIndex < splitTypes.size() - 1) {
      result.addAll(getPermutations(map, splitTypes, typeIndex + 1, splitValues, 0));

    } else {
      result.add(map);
    }

    if (valueIndex < typeValues.size() - 1) {
      result.addAll(getPermutations(parent, splitTypes, typeIndex, splitValues, valueIndex + 1));
    }

    return result;
  }

  public static Map<AnalysisSplitType, List<AnalysisSplitValue>> mergeSplitValues(
      List<AnalysisSplitType> splitTypes,
      Collection<Map<AnalysisSplitType, List<AnalysisSplitValue>>> input) {

    Map<AnalysisSplitType, List<AnalysisSplitValue>> result = new HashMap<>();

    if (!BeeUtils.isEmpty(splitTypes) && !BeeUtils.isEmpty(input)) {
      for (Map<AnalysisSplitType, List<AnalysisSplitValue>> map : input) {
        if (!BeeUtils.isEmpty(map)) {
          for (AnalysisSplitType splitType : splitTypes) {
            List<AnalysisSplitValue> splitValues = map.get(splitType);
            if (!BeeUtils.isEmpty(splitValues)) {
              putSplitValues(result, splitType, splitValues);
            }
          }
        }
      }
    }
    return result;
  }

  public static void putSplitValues(Map<AnalysisSplitType, List<AnalysisSplitValue>> map,
      AnalysisSplitType splitType, List<AnalysisSplitValue> splitValues) {

    if (map.containsKey(splitType)) {
      List<AnalysisSplitValue> list = map.get(splitType);
      int size = list.size();

      for (AnalysisSplitValue splitValue : splitValues) {
        if (!list.contains(splitValue)) {
          list.add(splitValue);
        }
      }

      if (list.size() > size) {
        list.sort(null);
      }

    } else {
      map.put(splitType, splitValues);
    }
  }

  private enum Serial {
    VALUE, ID, BACKGROUND, FOREGROUND
  }

  private String value;
  private Long id;

  private String background;
  private String foreground;

  private AnalysisSplitValue() {
    this(null);
  }

  private AnalysisSplitValue(String value) {
    this(value, null);
  }

  private AnalysisSplitValue(String value, Long id) {
    this.value = value;
    this.id = id;
  }

  public String getValue() {
    return value;
  }

  public Long getId() {
    return id;
  }

  public String getBackground() {
    return background;
  }

  public void setBackground(String background) {
    this.background = background;
  }

  public String getForeground() {
    return foreground;
  }

  public void setForeground(String foreground) {
    this.foreground = foreground;
  }

  private void setValue(String value) {
    this.value = value;
  }

  private void setId(Long id) {
    this.id = id;
  }

  public Integer getYear() {
    return BeeUtils.toIntOrNull(value);
  }

  public YearMonth getYearMonth() {
    return YearMonth.parse(value);
  }

  public YearQuarter getYearQuarter() {
    return YearQuarter.parse(value);
  }

  public boolean isEmpty() {
    return BeeUtils.isEmpty(getValue());
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, Serial.values().length);

    for (int i = 0; i < arr.length; i++) {
      String v = arr[i];

      if (!BeeUtils.isEmpty(v)) {
        switch (Serial.values()[i]) {
          case VALUE:
            setValue(v);
            break;
          case ID:
            setId(BeeUtils.toLongOrNull(v));
            break;

          case BACKGROUND:
            setBackground(v);
            break;
          case FOREGROUND:
            setForeground(v);
            break;
        }
      }
    }
  }

  @Override
  public int compareTo(AnalysisSplitValue o) {
    return BeeUtils.compareNullsFirst(value, o.value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AnalysisSplitValue)) {
      return false;
    }

    AnalysisSplitValue that = (AnalysisSplitValue) o;
    return Objects.equals(value, that.value) && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (id != null ? id.hashCode() : 0);
    return result;
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[Serial.values().length];
    int i = 0;

    for (Serial member : Serial.values()) {
      switch (member) {
        case VALUE:
          arr[i++] = getValue();
          break;
        case ID:
          arr[i++] = getId();
          break;

        case BACKGROUND:
          arr[i++] = getBackground();
          break;
        case FOREGROUND:
          arr[i++] = getForeground();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return BeeConst.EMPTY;
    } else {
      return BeeUtils.joinWords(getValue(), getId(), getBackground(), getForeground());
    }
  }
}
