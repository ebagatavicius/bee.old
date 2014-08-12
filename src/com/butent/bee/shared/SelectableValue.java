package com.butent.bee.shared;

import com.butent.bee.shared.utils.Codec;

public class SelectableValue implements BeeSerializable {

  public static SelectableValue restore(String s) {
    SelectableValue sv = new SelectableValue();
    sv.deserialize(s);
    return sv;
  }

  private String value;
  private boolean selected;

  public SelectableValue(String value) {
    this.value = value;
  }

  private SelectableValue() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setValue(arr[0]);
    setSelected(Codec.unpack(arr[1]));
  }

  public String getValue() {
    return value;
  }

  public boolean isSelected() {
    return selected;
  }

  @Override
  public String serialize() {
    String[] arr = new String[] {getValue(), Codec.pack(isSelected())};
    return Codec.beeSerialize(arr);
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
