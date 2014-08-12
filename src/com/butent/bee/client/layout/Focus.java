package com.butent.bee.client.layout;

import com.google.gwt.user.client.ui.FocusPanel;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;

/**
 * Enables to use a panel that makes its contents focusable, and adds the ability to catch mouse and
 * keyboard events.
 */

public class Focus extends FocusPanel implements IdentifiableWidget {

  public Focus() {
    DomUtils.createId(this, getIdPrefix());
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "focus";
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }
}
