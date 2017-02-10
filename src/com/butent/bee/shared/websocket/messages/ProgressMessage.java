package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.State;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class ProgressMessage extends Message {

  public static ProgressMessage activate(String progressId) {
    return new ProgressMessage(progressId, State.ACTIVATED);
  }

  public static ProgressMessage cancel(String progressId) {
    return new ProgressMessage(progressId, State.CANCELED);
  }

  public static ProgressMessage close(String progressId) {
    return new ProgressMessage(progressId, State.CLOSED);
  }

  public static ProgressMessage open(String progressId, String... label) {
    ProgressMessage message = new ProgressMessage(progressId, State.OPEN);
    message.setLabel(ArrayUtils.joinWords(label));
    return message;
  }

  public static ProgressMessage update(String progressId, String label, double value) {
    ProgressMessage message = new ProgressMessage(progressId, State.UPDATING);
    message.setLabel(label);
    message.setValue(value);
    return message;
  }

  private String progressId;

  private State state;
  private String label;
  private Double value;

  ProgressMessage() {
    super(Type.PROGRESS);
  }

  private ProgressMessage(String progressId, State state) {
    this();

    this.progressId = progressId;
    this.state = state;
  }

  @Override
  public String brief() {
    return (getValue() == null) ? string(getState()) : string(getValue());
  }

  public String getLabel() {
    return label;
  }

  public String getProgressId() {
    return progressId;
  }

  public State getState() {
    return state;
  }

  public Double getValue() {
    return value;
  }

  public boolean isActivated() {
    return getState() == State.ACTIVATED;
  }

  public boolean isCanceled() {
    return getState() == State.CANCELED;
  }

  public boolean isClosed() {
    return getState() == State.CLOSED;
  }

  @Override
  public boolean isLoggable() {
    return getValue() == null;
  }

  public boolean isOpen() {
    return getState() == State.OPEN;
  }

  public boolean isUpdate() {
    return getState() == State.UPDATING;
  }

  @Override
  public boolean isValid() {
    return !BeeUtils.isEmpty(getProgressId()) && getState() != null;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()), "progressId", getProgressId(),
        "state", string(getState()), "value", string(getValue()));
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 4);

    int i = 0;
    setProgressId(arr[i++]);
    setState(Codec.unpack(State.class, arr[i++]));
    setLabel(arr[i++]);
    setValue(BeeUtils.toDoubleOrNull(arr[i]));
  }

  @Override
  protected String serialize() {
    List<Object> values = new ArrayList<>();

    values.add(getProgressId());
    values.add(Codec.pack(getState()));
    values.add(getLabel());
    values.add(getValue());

    return Codec.beeSerialize(values);
  }

  private void setLabel(String label) {
    this.label = label;
  }

  private void setProgressId(String progressId) {
    this.progressId = progressId;
  }

  private void setState(State state) {
    this.state = state;
  }

  private void setValue(Double value) {
    this.value = value;
  }
}
