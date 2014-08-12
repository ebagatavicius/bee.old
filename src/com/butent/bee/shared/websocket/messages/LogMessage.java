package com.butent.bee.shared.websocket.messages;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public class LogMessage extends Message {

  public static LogMessage level(LogLevel level) {
    return new LogMessage(level);
  }

  public static LogMessage log(LogLevel level, String text) {
    LogMessage logMessage = new LogMessage(level);
    logMessage.setText(text);
    return logMessage;
  }

  private LogLevel level;
  private String text;

  LogMessage() {
    super(Type.LOG);
  }

  private LogMessage(LogLevel level) {
    this();
    this.level = level;
  }

  @Override
  public String brief() {
    return getText();
  }

  public LogLevel getLevel() {
    return level;
  }

  @Override
  public boolean isValid() {
    return getLevel() != null;
  }

  private void setLevel(LogLevel level) {
    this.level = level;
  }

  public String getText() {
    return text;
  }

  private void setText(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()), "level", string(getLevel()),
        "text", getText());
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    int i = 0;
    setLevel(Codec.unpack(LogLevel.class, arr[i++]));
    setText(arr[i++]);
  }

  @Override
  protected String serialize() {
    List<String> values = Lists.newArrayList(Codec.pack(getLevel()), getText());
    return Codec.beeSerialize(values);
  }
}
