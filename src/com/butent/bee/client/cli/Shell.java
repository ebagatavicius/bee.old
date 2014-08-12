package com.butent.bee.client.cli;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.shared.utils.BeeUtils;

public class Shell extends InputArea {

  private static final char EOL = '\n';

  private static final String STORAGE_KEY = "shell";

  public Shell(String styleName) {
    super();
    if (!BeeUtils.isEmpty(styleName)) {
      addStyleName(styleName);
    }

    sinkEvents(Event.ONKEYDOWN | Event.ONCLICK);
  }

  @Override
  public String getIdPrefix() {
    return "shell";
  }

  @Override
  public void onBrowserEvent(Event event) {
    boolean ok = !BeeUtils.isEmpty(getValue());
    if (ok) {
      String type = event.getType();
      if (EventUtils.isKeyDown(type)) {
        ok = event.getKeyCode() == KeyCodes.KEY_ENTER;
        if (ok && EventUtils.hasModifierKey(event)) {
          int p = getCursorPos();
          setValue(new StringBuilder(getValue()).insert(p, EOL).toString());
          setCursorPos(p + 1);
          event.preventDefault();
          return;
        }
      } else {
        ok = EventUtils.isClick(type) && event.getAltKey();
      }
    }

    if (ok) {
      int pos = getCursorPos();
      int start = (pos > 0) ? getValue().lastIndexOf(EOL, pos - 1) + 1 : 0;
      int end = getValue().indexOf(EOL, pos);

      String line;
      if (end == start) {
        line = null;
      } else if (end > start) {
        line = getValue().substring(start, end);
      } else {
        line = getValue().substring(start);
      }

      event.preventDefault();
      if (BeeUtils.isEmpty(line)) {
        return;
      }

      if (pos == getValue().length()) {
        setValue(getValue() + EOL);
        setCursorPos(pos + 1);
      }

      ok = CliWorker.execute(line.trim(), true);
      if (ok) {
        save();
      }

    } else {
      super.onBrowserEvent(event);
    }
  }

  public void restore() {
    String stored = BeeKeeper.getStorage().get(STORAGE_KEY);
    if (!BeeUtils.isEmpty(stored)) {
      setValue(stored.trim());
    }
  }

  public void save() {
    if (!BeeUtils.isEmpty(getValue())) {
      BeeKeeper.getStorage().set(STORAGE_KEY, getValue().trim());
    }
  }
}
