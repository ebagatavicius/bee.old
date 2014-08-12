package com.butent.bee.client.decorator;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.shared.ui.DecoratorConstants;

class Template {

  static Template getTemplate(Element element) {
    if (element == null) {
      return null;
    }

    String id = element.getAttribute(DecoratorConstants.ATTR_ID);

    StringBuilder sb = new StringBuilder();
    for (Element child : XmlUtils.getChildrenElements(element)) {
      sb.append(child.toString());
    }

    if (sb.length() > 0) {
      return new Template(id, sb.toString());
    } else {
      return null;
    }
  }

  private final String id;
  private final String markup;

  Template(String id, String markup) {
    super();
    this.id = id;
    this.markup = markup;
  }

  Template getCopy() {
    return new Template(id, markup);
  }

  String getId() {
    return id;
  }

  String getMarkup() {
    return markup;
  }
}
