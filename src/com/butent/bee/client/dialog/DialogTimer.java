package com.butent.bee.client.dialog;

import com.google.gwt.user.client.Timer;

import com.butent.bee.shared.Holder;
import com.butent.bee.shared.State;

public class DialogTimer extends Timer {

  private final Popup dialog;
  private final Holder<State> state;

  public DialogTimer(Popup dialog, Holder<State> state) {
    super();
    this.dialog = dialog;
    this.state = state;
  }

  @Override
  public void run() {
    state.set(State.EXPIRED);
    dialog.close();
  }
}
