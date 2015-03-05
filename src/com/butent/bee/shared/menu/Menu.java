package com.butent.bee.shared.menu;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.BeeObject;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({MenuEntry.class, MenuItem.class})
public abstract class Menu implements BeeSerializable, BeeObject {

  private enum Serial {
    NAME, LABEL, SEPARATOR, ORDER, MODULE, DATA
  }

  public static Menu restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length + 2);
    String clazz = arr[0];
    String content = arr[1];
    Menu menu = null;

    if (content != null) {
      menu = Menu.getMenu(clazz);

      for (int i = 0; i < members.length; i++) {
        Serial member = members[i];
        String value = arr[i + 2];

        switch (member) {
          case NAME:
            menu.name = value;
            break;
          case LABEL:
            menu.label = value;
            break;
          case SEPARATOR:
            menu.separator = BeeUtils.toBooleanOrNull(value);
            break;
          case ORDER:
            menu.order = BeeUtils.toIntOrNull(value);
            break;
          case MODULE:
            menu.module = value;
            break;
          case DATA:
            menu.data = value;
            break;
        }
      }
      menu.deserialize(content);
    }
    return menu;
  }

  private static Menu getMenu(String clazz) {
    Menu menu = null;

    if (NameUtils.getClassName(MenuEntry.class).equals(clazz)) {
      menu = new MenuEntry();

    } else if (NameUtils.getClassName(MenuItem.class).equals(clazz)) {
      menu = new MenuItem();

    } else {
      Assert.unsupported("Unsupported menu class name: " + clazz);
    }
    return menu;
  }

  @XmlAttribute
  private String name;
  @XmlAttribute
  private String label;
  @XmlAttribute
  private Boolean separator;
  @XmlAttribute
  private Integer order;
  @XmlAttribute
  private String parent;
  @XmlAttribute
  private String module;
  @XmlAttribute
  private String data;

  public String getData() {
    return data;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public String getModule() {
    return module;
  }

  @Override
  public String getName() {
    return name;
  }

  public Integer getOrder() {
    return order;
  }

  public String getParent() {
    return parent;
  }

  public abstract int getSize();

  public Boolean hasSeparator() {
    return BeeUtils.isTrue(separator);
  }

  public void setModuleName(String moduleName) {
    this.module = moduleName;
  }

  protected String serialize(Object obj) {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length + 2];
    int i = 0;
    arr[i++] = NameUtils.getClassName(this.getClass());
    arr[i++] = obj;

    for (Serial member : Serial.values()) {
      switch (member) {
        case NAME:
          arr[i++] = name;
          break;
        case LABEL:
          arr[i++] = label;
          break;
        case SEPARATOR:
          arr[i++] = separator;
          break;
        case ORDER:
          arr[i++] = order;
          break;
        case MODULE:
          arr[i++] = module;
          break;
        case DATA:
          arr[i++] = data;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }
}
