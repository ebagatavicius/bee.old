package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class SearchResult implements BeeSerializable, HasViewName {

  public static SearchResult restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    SearchResult result = new SearchResult();
    result.deserialize(s);
    return result;
  }

  private String viewName;
  private BeeRow row;

  public SearchResult(String viewName, BeeRow row) {
    this.viewName = viewName;
    this.row = row;
  }

  private SearchResult() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    int idx = 0;
    setViewName(arr[idx++]);
    setRow(BeeRow.restore(arr[idx++]));
  }

  public BeeRow getRow() {
    return row;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {getViewName(), getRow()});
  }

  private void setRow(BeeRow row) {
    this.row = row;
  }

  private void setViewName(String viewName) {
    this.viewName = viewName;
  }
}
