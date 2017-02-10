package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

public class ParameterMessage extends Message {

  private BeeParameter parameter;

  public ParameterMessage(BeeParameter parameter) {
    this();
    this.parameter = Assert.notNull(parameter);
  }

  protected ParameterMessage() {
    super(Type.PARAMETER);
  }

  @Override
  public String brief() {
    return toString();
  }

  public BeeParameter getParameter() {
    return parameter;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public String toString() {
    return BeeUtils.joinWords(string(getType()), getParameter().getName());
  }

  @Override
  protected void deserialize(String s) {
    parameter = BeeParameter.restore(s);
  }

  @Override
  protected String serialize() {
    return Codec.beeSerialize(getParameter());
  }
}
