package com.butent.bee.client.decorator;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.shared.ui.DecoratorConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Parameter {

  static List<Parameter> getParameters(Element element) {
    List<Parameter> result = new ArrayList<>();
    if (element == null) {
      return result;
    }

    boolean required =
        BeeUtils.same(XmlUtils.getLocalName(element), DecoratorConstants.TAG_REQUIRED_PARAM);

    Map<String, String> attributes = XmlUtils.getAttributes(element);
    if (attributes.isEmpty()) {
      return result;
    }

    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      String name = NameUtils.getLocalPart(entry.getKey());
      if (!BeeUtils.isEmpty(name)) {
        result.add(new Parameter(name.trim(), BeeUtils.trim(entry.getValue()), required));
      }
    }
    return result;
  }

  private final String name;
  private final String defaultValue;
  private final boolean required;

  Parameter(String name, String defaultValue, boolean required) {
    super();
    this.name = name;
    this.defaultValue = defaultValue;
    this.required = required;
  }

  String getDefaultValue() {
    return defaultValue;
  }

  String getName() {
    return name;
  }

  String getValue(Map<String, String> options) {
    if (options == null || !options.containsKey(name)) {
      return defaultValue;
    }
    return BeeUtils.notEmpty(options.get(name), defaultValue);
  }

  boolean isRequired() {
    return required;
  }
}
