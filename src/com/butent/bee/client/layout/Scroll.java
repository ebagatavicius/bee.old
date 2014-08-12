package com.butent.bee.client.layout;

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;

/**
 * Contains a panel that wraps its contents in a scrollable area.
 */

public class Scroll extends ScrollPanel implements IdentifiableWidget {

  public Scroll() {
    super();
    init();
  }

  public Scroll(Widget child) {
    super(child);
    init();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "scroll";
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
  }
}
