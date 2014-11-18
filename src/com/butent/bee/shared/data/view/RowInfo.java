package com.butent.bee.shared.data.view;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class RowInfo implements BeeSerializable, Comparable<RowInfo> {

  public static RowInfo restore(String s) {
    Assert.notEmpty(s);
    RowInfo rowInfo = new RowInfo();
    rowInfo.deserialize(s);
    return rowInfo;
  }

  private long id;
  private long version;

  private boolean editable;
  private boolean removable;

  public RowInfo(IsRow row, boolean editable) {
    this(row.getId(), row.getVersion(), editable, row.isRemovable());
  }

  public RowInfo(long id, long version) {
    this(id, version, true, true);
  }

  private RowInfo(long id, long version, boolean editable, boolean removable) {
    super();

    this.id = id;
    this.version = version;
    this.editable = editable;
    this.removable = removable;
  }

  private RowInfo() {
  }

  @Override
  public int compareTo(RowInfo o) {
    Assert.notNull(o);
    int res = Long.valueOf(getId()).compareTo(o.getId());

    if (res == BeeConst.COMPARE_EQUAL) {
      res = Long.valueOf(getVersion()).compareTo(o.getVersion());
      if (res == BeeConst.COMPARE_EQUAL) {
        res = Boolean.valueOf(isEditable()).compareTo(o.isEditable());
        if (res == BeeConst.COMPARE_EQUAL) {
          res = Boolean.valueOf(isRemovable()).compareTo(o.isRemovable());
        }
      }
    }
    return res;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 4);

    setId(BeeUtils.toLong(arr[0]));
    setVersion(BeeUtils.toLong(arr[1]));
    setEditable(Codec.unpack(arr[2]));
    setRemovable(Codec.unpack(arr[3]));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RowInfo)) {
      return false;
    }

    RowInfo other = (RowInfo) obj;
    return getId() == other.getId() && getVersion() == other.getVersion()
        && isEditable() == other.isEditable() && isRemovable() == other.isRemovable();
  }

  public long getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Long.valueOf(getId()).hashCode();
    result = prime * result + Long.valueOf(getVersion()).hashCode();
    result = prime * result + Boolean.valueOf(isEditable()).hashCode();
    result = prime * result + Boolean.valueOf(isRemovable()).hashCode();
    return result;
  }

  public boolean isEditable() {
    return editable;
  }

  public boolean isRemovable() {
    return removable;
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {getId(), getVersion(),
        Codec.pack(isEditable()), Codec.pack(isRemovable())});
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public void setRemovable(boolean removable) {
    this.removable = removable;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords("id:", getId(), "version:", getVersion(),
        "editable:", isEditable(), "removable:", isRemovable());
  }

  private void setId(long id) {
    this.id = id;
  }
}
