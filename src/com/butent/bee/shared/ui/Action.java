package com.butent.bee.shared.ui;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Lists possible actions with a user interface component (like refresh, save, close etc).
 */

public enum Action implements BeeSerializable, HasCaption {
  ADD(Localized.getConstants().actionAdd(), "add"),
  AUDIT(Localized.getConstants().actionAudit(), "audit"),
  BOOKMARK(Localized.getConstants().actionBookmark(), "bookmark"),
  CANCEL(Localized.getConstants().actionCancel(), "cancel"),
  CLOSE(Localized.getConstants().actionClose(), "close"),
  CONFIGURE(Localized.getConstants().actionConfigure(), "configure"),
  COPY(Localized.getConstants().actionCopy(), "copy"),
  DELETE(Localized.getConstants().actionDelete(), "delete"),
  EDIT(Localized.getConstants().actionEdit(), "edit"),
  EXPORT(Localized.getConstants().actionExport(), "export"),
  FILTER(Localized.getConstants().actionFilter(), "filter"),
  MENU(Localized.getConstants().menu(), "menu"),
  PRINT(Localized.getConstants().actionPrint(), "print"),
  REFRESH(Localized.getConstants().actionRefresh(), "refresh"),
  REMOVE_FILTER(Localized.getConstants().actionRemoveFilter(), "removeFilter"),
  RIGHTS(Localized.getConstants().rights(), "rights"),
  SAVE(Localized.getConstants().actionSave(), "save");

  public static final Set<Action> NO_ACTIONS = new HashSet<>();

  public static Set<Action> parse(String s) {
    Set<Action> result = new HashSet<>();
    if (!BeeUtils.isEmpty(s)) {
      for (String item : NameUtils.NAME_SPLITTER.split(s)) {
        Action action = restore(item);
        if (action != null) {
          result.add(action);
        }
      }
    }
    return result;
  }

  public static Action restore(String s) {
    if (!BeeUtils.isEmpty(s)) {
      for (Action action : Action.values()) {
        if (BeeUtils.same(action.name(), s)) {
          return action;
        }
      }
    }
    return null;
  }

  private final String caption;
  private final String styleSuffix;

  private Action(String caption, String styleSuffix) {
    this.caption = caption;
    this.styleSuffix = styleSuffix;
  }

  @Override
  public void deserialize(String s) {
    Assert.untouchable();
  }

  @Override
  public String getCaption() {
    return caption;
  }

  public String getStyleName() {
    return "bee-Action-" + styleSuffix;
  }

  @Override
  public String serialize() {
    return this.name();
  }
}
