package com.butent.bee.shared.modules.cars;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.Objects;

public class Dimension implements BeeSerializable, Comparable<Dimension> {
  private long id;
  private String name;
  private boolean required;

  public Dimension(long id, String name) {
    this.id = id;
    this.name = Assert.notEmpty(name);
  }

  private Dimension() {
  }

  @Override
  public int compareTo(Dimension o) {
    int order = BeeUtils.compareNullsFirst(name, Objects.nonNull(o) ? o.name : null);
    return order == BeeConst.COMPARE_EQUAL ? BeeUtils.compareNullsFirst(id, o.id) : order;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    this.id = BeeUtils.toLong(arr[0]);
    this.name = arr[1];
    this.required = BeeUtils.toBoolean(arr[2]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return id == ((Dimension) o).id;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public boolean isRequired() {
    return required;
  }

  public static Dimension restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    Dimension dim = new Dimension();
    dim.deserialize(s);
    return dim;
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(Arrays.asList(id, name, required));
  }

  public Dimension setRequired(Boolean req) {
    this.required = BeeUtils.unbox(req);
    return this;
  }

  @Override
  public String toString() {
    return name;
  }
}
