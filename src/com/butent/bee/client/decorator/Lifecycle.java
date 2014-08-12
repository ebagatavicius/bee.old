package com.butent.bee.client.decorator;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.shared.ui.DecoratorConstants;
import com.butent.bee.shared.utils.BeeUtils;

class Lifecycle {

  static Lifecycle getLifecycle(Element element) {
    if (element == null) {
      return null;
    }

    String created = null;
    String inserted = null;
    String removed = null;

    for (Element child : XmlUtils.getChildrenElements(element)) {
      String tag = XmlUtils.getLocalName(child);
      String text = XmlUtils.getText(child);
      if (BeeUtils.isEmpty(text)) {
        continue;
      }

      if (BeeUtils.same(tag, DecoratorConstants.TAG_CREATED)) {
        created = text;
      } else if (BeeUtils.same(tag, DecoratorConstants.TAG_INSERTED)) {
        inserted = text;
      } else if (BeeUtils.same(tag, DecoratorConstants.TAG_REMOVED)) {
        removed = text;
      }
    }

    if (BeeUtils.allEmpty(created, inserted, removed)) {
      return null;
    } else {
      return new Lifecycle(created, inserted, removed);
    }
  }

  private String created;
  private String inserted;
  private String removed;

  Lifecycle(String created, String inserted, String removed) {
    super();
    this.created = created;
    this.inserted = inserted;
    this.removed = removed;
  }

  Lifecycle getCopy() {
    return new Lifecycle(created, inserted, removed);
  }

  String getCreated() {
    return created;
  }

  String getInserted() {
    return inserted;
  }

  String getRemoved() {
    return removed;
  }

  void setCreated(String created) {
    this.created = created;
  }

  void setInserted(String inserted) {
    this.inserted = inserted;
  }

  void setRemoved(String removed) {
    this.removed = removed;
  }

  void updateFrom(Lifecycle other, boolean override) {
    if (other == null) {
      return;
    }

    if (override || BeeUtils.isEmpty(getCreated()) && !BeeUtils.isEmpty(other.getCreated())) {
      setCreated(other.getCreated());
    }
    if (override || BeeUtils.isEmpty(getInserted()) && !BeeUtils.isEmpty(other.getInserted())) {
      setInserted(other.getInserted());
    }
    if (override || BeeUtils.isEmpty(getRemoved()) && !BeeUtils.isEmpty(other.getRemoved())) {
      setRemoved(other.getRemoved());
    }
  }
}
