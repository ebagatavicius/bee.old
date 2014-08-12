package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

public class InfoMessage extends Message implements HasCaption, HasInfo {

  private String caption;
  private List<Property> info;

  public InfoMessage(String caption, List<Property> info) {
    this();

    this.caption = caption;
    this.info = info;
  }

  InfoMessage() {
    super(Type.INFO);
  }

  @Override
  public String brief() {
    return BeeUtils.joinWords(getCaption(), BeeUtils.size(getInfo()));
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public List<Property> getInfo() {
    return info;
  }

  @Override
  public boolean isValid() {
    return !BeeUtils.isEmpty(getInfo());
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()), "caption", getCaption(),
        "info", BeeUtils.isEmpty(info) ? null : info.toString());
  }

  @Override
  protected void deserialize(String s) {
    Pair<String, String> pair = Pair.restore(s);

    this.caption = pair.getA();
    this.info = PropertyUtils.restoreProperties(pair.getB());
  }

  @Override
  protected String serialize() {
    return Pair.of(getCaption(), getInfo()).serialize();
  }
}
