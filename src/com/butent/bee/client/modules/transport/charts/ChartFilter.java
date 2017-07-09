package com.butent.bee.client.modules.transport.charts;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.modules.transport.TransportConstants.ChartDataType;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

class ChartFilter implements BeeSerializable {

  static final class FilterValue implements BeeSerializable {

    private static FilterValue restore(String s) {
      if (!BeeUtils.isEmpty(s)) {
        FilterValue fv = new FilterValue();
        fv.deserialize(s);

        if (fv.isValid()) {
          return fv;
        }
      }
      return null;
    }

    private ChartDataType type;
    private String name;
    private Long id;

    private FilterValue() {
    }

    FilterValue(ChartDataType type, String name, Long id) {
      this.type = type;
      this.name = name;
      this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof FilterValue) {
        FilterValue other = (FilterValue) obj;

        return Objects.equals(type, other.type)
            && Objects.equals(name, other.name)
            && Objects.equals(id, other.id);

      } else {
        return false;
      }
    }

    @Override
    public void deserialize(String s) {
      String[] arr = Codec.beeDeserializeCollection(s);
      Assert.lengthEquals(arr, 3);

      int i = 0;

      this.type = EnumUtils.getEnumByName(ChartDataType.class, arr[i++]);
      this.name = arr[i++];
      this.id = BeeUtils.toLongOrNull(arr[i++]);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    @Override
    public String serialize() {
      List<String> list = new ArrayList<>();
      list.add(type.name());
      list.add(name);
      list.add((id == null) ? null : id.toString());

      return Codec.beeSerialize(list);
    }

    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public ChartDataType getType() {
      return type;
    }

    boolean isValid() {
      return type != null && !BeeUtils.isEmpty(name);
    }
  }

  static List<ChartFilter> restoreList(String s) {
    List<ChartFilter> result = new ArrayList<>();

    String[] arr = Codec.beeDeserializeCollection(s);
    if (!ArrayUtils.isEmpty(arr)) {
      for (String cfv : arr) {
        ChartFilter cf = new ChartFilter();
        cf.deserialize(cfv);
        if (cf.isValid()) {
          result.add(cf);
        }
      }
    }

    return result;
  }

  static final int MAX_LABEL_LENGTH = 200;

  private String label;
  private boolean initial;

  private final List<FilterValue> values = new ArrayList<>();

  private ChartFilter() {
  }

  ChartFilter(String label, List<FilterValue> values) {
    this.label = label;
    this.values.addAll(values);
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 3);

    int i = 0;
    setLabel(arr[i++]);
    setInitial(Codec.unpack(arr[i++]));
    String[] fvarr = Codec.beeDeserializeCollection(arr[i++]);

    if (!values.isEmpty()) {
      values.clear();
    }
    if (!ArrayUtils.isEmpty(fvarr)) {
      for (String fvs : fvarr) {
        FilterValue fv = FilterValue.restore(fvs);
        if (fv != null) {
          values.add(fv);
        }
      }
    }
  }

  @Override
  public String serialize() {
    List<String> list = new ArrayList<>();
    list.add(getLabel());
    list.add(Codec.pack(isInitial()));
    list.add(Codec.beeSerialize(values));

    return Codec.beeSerialize(list);
  }

  boolean applyTo(Collection<ChartData> data) {
    boolean result = false;

    if (!BeeUtils.isEmpty(data) && !values.isEmpty()) {
      for (FilterValue fv : values) {
        for (ChartData cd : data) {
          if (cd != null && cd.getType() == fv.type) {
            result |= cd.setItemSelected(fv.name, true);
            break;
          }
        }
      }
    }

    return result;
  }

  String getLabel() {
    return label;
  }

  List<FilterValue> getValues() {
    return values;
  }

  boolean isInitial() {
    return initial;
  }

  boolean isValid() {
    return !BeeUtils.isEmpty(getLabel()) && !values.isEmpty();
  }

  boolean matches(Collection<ChartData> data) {
    if (!BeeUtils.isEmpty(data) && !values.isEmpty()) {
      for (FilterValue fv : values) {
        for (ChartData cd : data) {
          if (cd != null && cd.getType() == fv.type) {
            if (cd.contains(fv.name)) {
              return true;
            }
            break;
          }
        }
      }
    }
    return false;
  }

  void setInitial(boolean initial) {
    this.initial = initial;
  }

  private void setLabel(String label) {
    this.label = label;
  }
}
