package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public final class RowChildren implements BeeSerializable {

  private enum Serial {
    REPOSITORY, PARENT_COLUMN, ROW_ID, CHILD_COLUMN, CHILDREN_IDS
  }

  public static RowChildren create(String repository, String parentColumn, Long rowId,
      String childColumn, String childrenIds) {
    Assert.notEmpty(repository);
    Assert.notEmpty(parentColumn);
    Assert.notEmpty(childColumn);

    RowChildren result = new RowChildren();

    result.setRepository(repository);
    result.setParentColumn(parentColumn);
    result.setRowId(rowId);
    result.setChildColumn(childColumn);
    result.setChildrenIds(childrenIds);

    return result;
  }

  public static RowChildren restore(String s) {
    Assert.notEmpty(s, "cannot restore row children");

    RowChildren result = new RowChildren();
    result.deserialize(s);

    return result;
  }

  private String repository;

  private String parentColumn;
  private Long rowId;

  private String childColumn;
  private String childrenIds;

  private RowChildren() {
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
        case REPOSITORY:
          setRepository(value);
          break;

        case PARENT_COLUMN:
          setParentColumn(value);
          break;

        case ROW_ID:
          setRowId(BeeUtils.toLongOrNull(value));
          break;

        case CHILD_COLUMN:
          setChildColumn(value);
          break;

        case CHILDREN_IDS:
          setChildrenIds(value);
          break;
      }
    }
  }

  public String getChildColumn() {
    return childColumn;
  }

  public String getChildrenIds() {
    return childrenIds;
  }

  public String getParentColumn() {
    return parentColumn;
  }

  public String getRepository() {
    return repository;
  }

  public Long getRowId() {
    return rowId;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case REPOSITORY:
          arr[i++] = getRepository();
          break;

        case PARENT_COLUMN:
          arr[i++] = getParentColumn();
          break;

        case ROW_ID:
          arr[i++] = getRowId();
          break;

        case CHILD_COLUMN:
          arr[i++] = getChildColumn();
          break;

        case CHILDREN_IDS:
          arr[i++] = getChildrenIds();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  private void setChildColumn(String childColumn) {
    this.childColumn = childColumn;
  }

  private void setChildrenIds(String childrenIds) {
    this.childrenIds = childrenIds;
  }

  private void setParentColumn(String parentColumn) {
    this.parentColumn = parentColumn;
  }

  private void setRepository(String repository) {
    this.repository = repository;
  }

  private void setRowId(Long rowId) {
    this.rowId = rowId;
  }
}
