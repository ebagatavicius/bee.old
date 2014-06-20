package com.butent.bee.shared.websocket.messages;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.List;

public class ConfigMessage extends Message {
  
  public static final String KEY_REMOTE_ENDPOINT = "RemoteEndpoint";
  
  public static ConfigMessage switchRemoteEndpointType(String type) {
    return new ConfigMessage(KEY_REMOTE_ENDPOINT, type);
  }

  private String key;
  private String value;

  private ConfigMessage(String key, String value) {
    this();
    this.key = key;
    this.value = value;
  }

  ConfigMessage() {
    super(Type.CONFIG);
  }

  @Override
  public String brief() {
    return toString();
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean isValid() {
    return !BeeUtils.anyEmpty(getKey(), getValue());
  }

  @Override
  public String toString() {
    return NameUtils.addName(getKey(), getValue());
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    int i = 0;
    setKey(arr[i++]);
    setValue(arr[i++]);
  }

  @Override
  protected String serialize() {
    List<String> values = Lists.newArrayList(getKey(), getValue());
    return Codec.beeSerialize(values);
  }

  private void setKey(String key) {
    this.key = key;
  }

  private void setValue(String value) {
    this.value = value;
  }
}
