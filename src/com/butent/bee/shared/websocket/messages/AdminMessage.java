package com.butent.bee.shared.websocket.messages;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public class AdminMessage extends Message implements HasRecipient {

  public static AdminMessage command(String from, String to, String command) {
    AdminMessage adminMessage = new AdminMessage(from, to);
    adminMessage.setCommand(command);
    return adminMessage;
  }

  public static AdminMessage response(String from, String to, String response) {
    AdminMessage adminMessage = new AdminMessage(from, to);
    adminMessage.setResponse(response);
    return adminMessage;
  }

  private String from;
  private String to;

  private String command;
  private String response;

  AdminMessage() {
    super(Type.ADMIN);
  }

  private AdminMessage(String from, String to) {
    this();

    this.from = from;
    this.to = to;
  }

  @Override
  public String brief() {
    return BeeUtils.notEmpty(getResponse(), getCommand());
  }

  public String getCommand() {
    return command;
  }

  public String getFrom() {
    return from;
  }

  public String getResponse() {
    return response;
  }

  @Override
  public String getTo() {
    return to;
  }

  @Override
  public boolean isValid() {
    return !BeeUtils.anyEmpty(getFrom(), getTo())
        && BeeUtils.isEmpty(getCommand()) != BeeUtils.isEmpty(getResponse());
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()), "from", getFrom(), "to", getTo(),
        "command", getCommand(), "response", getResponse());
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 4);

    int i = 0;
    setFrom(arr[i++]);
    setTo(arr[i++]);
    setCommand(arr[i++]);
    setResponse(arr[i++]);
  }

  @Override
  protected String serialize() {
    List<String> values = Lists.newArrayList(getFrom(), getTo(), getCommand(), getResponse());
    return Codec.beeSerialize(values);
  }

  private void setCommand(String command) {
    this.command = command;
  }

  private void setFrom(String from) {
    this.from = from;
  }

  private void setResponse(String response) {
    this.response = response;
  }

  private void setTo(String to) {
    this.to = to;
  }
}
