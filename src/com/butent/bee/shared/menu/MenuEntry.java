package com.butent.bee.shared.menu;

import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Entry", namespace = DataUtils.MENU_NAMESPACE)
public class MenuEntry extends Menu {
  @XmlElementRef
  private List<Menu> items;

  @Override
  public void deserialize(String s) {
    String[] itms = Codec.beeDeserializeCollection(s);

    if (itms != null) {
      items = new ArrayList<>();

      for (String item : itms) {
        items.add(restore(item));
      }
    }
  }

  public List<Menu> getItems() {
    return items;
  }

  @Override
  public int getSize() {
    int size = 1;

    if (items != null) {
      for (Menu menu : items) {
        size += menu.getSize();
      }
    }

    return size;
  }

  @Override
  public String serialize() {
    return super.serialize(items);
  }
}
