package com.butent.bee.client.cli;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains representation structure for client side command line interface, catches command
 * keywords and processes them.
 */

public class CliWidget extends InputText {

  public CliWidget() {
    super();
    sinkEvents(Event.ONKEYDOWN);
  }

  @Override
  public String getIdPrefix() {
    return "cli";
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (event.getTypeInt() != Event.ONKEYDOWN || event.getKeyCode() != KeyCodes.KEY_ENTER
        || BeeUtils.isEmpty(getValue())) {
      super.onBrowserEvent(event);
      return;
    }

    event.preventDefault();
    CliWorker.execute(getValue(), true);
  }

  @Override
  protected String getDefaultStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "CliWidget";
  }
}
