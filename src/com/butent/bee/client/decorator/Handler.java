package com.butent.bee.client.decorator;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.shared.ui.DecoratorConstants;
import com.butent.bee.shared.utils.BeeUtils;

class Handler {

  static Handler getHandler(Element element) {
    if (element == null) {
      return null;
    }

    String event = element.getAttribute(DecoratorConstants.ATTR_EVENT);
    String text = XmlUtils.getText(element);
    String target = element.getAttribute(DecoratorConstants.ATTR_TARGET);
    Boolean deep = XmlUtils.getAttributeBoolean(element, DecoratorConstants.ATTR_DEEP);

    if (BeeUtils.isEmpty(event) || BeeUtils.isEmpty(text)) {
      return null;
    } else {
      return new Handler(event.trim(), text.trim(), BeeUtils.trim(target), BeeUtils.unbox(deep));
    }
  }

  private final String type;
  private final String body;

  private String target;
  private boolean deep;

  Handler(String type, String body, String target, boolean deep) {
    super();
    this.type = type;
    this.body = body;
    this.target = target;
    this.deep = deep;
  }

  Handler copyOf() {
    return new Handler(type, body, target, deep);
  }

  String getBody() {
    return body;
  }

  String getTarget() {
    return target;
  }

  String getType() {
    return type;
  }

  boolean isDeep() {
    return deep;
  }

  void setDeep(boolean deep) {
    this.deep = deep;
  }

  void setTarget(String target) {
    this.target = target;
  }
}
