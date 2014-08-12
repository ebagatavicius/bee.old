package com.butent.bee.shared.communication;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public class TextMessage implements BeeSerializable {

  public static final int MAX_LENGTH = 1000;

  public static TextMessage restore(String s) {
    TextMessage textMessage = new TextMessage();
    textMessage.deserialize(s);
    return textMessage;
  }

  private long userId;
  private String text;

  private long millis;

  public TextMessage(long userId, String text) {
    this.userId = userId;
    this.text = text;

    this.millis = System.currentTimeMillis();
  }

  private TextMessage() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 3);

    int i = 0;
    setUserId(BeeUtils.toLong(arr[i++]));
    setText(arr[i++]);
    setMillis(BeeUtils.toLong(arr[i++]));
  }

  public long getMillis() {
    return millis;
  }

  public String getText() {
    return text;
  }

  public long getUserId() {
    return userId;
  }

  public boolean isValid() {
    return DataUtils.isId(getUserId()) && !BeeUtils.isEmpty(getText());
  }

  @Override
  public String serialize() {
    List<String> values = Lists.newArrayList(BeeUtils.toString(getUserId()), getText(),
        BeeUtils.toString(getMillis()));
    return Codec.beeSerialize(values);
  }

  public void setMillis(long millis) {
    this.millis = millis;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("userId", BeeUtils.toString(getUserId()),
        "text", BeeUtils.clip(getText(), 50),
        "utc", (getMillis() > 0) ? new DateTime(getMillis()).toUtcString() : null);
  }

  private void setText(String text) {
    this.text = text;
  }

  private void setUserId(long userId) {
    this.userId = userId;
  }
}
