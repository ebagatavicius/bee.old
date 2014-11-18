package com.butent.bee.shared.ui;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Lists possible actions with a user interface component (like refresh, save, close etc).
 */

public enum Action implements BeeSerializable, HasCaption {
  ADD(FontAwesome.PLUS, Localized.getConstants().actionAdd(), "add"),
  AUDIT(FontAwesome.HISTORY, Localized.getConstants().actionAudit(), "audit"),
  BOOKMARK(FontAwesome.BOOKMARK_O, Localized.getConstants().actionBookmark(), "bookmark"),
  CANCEL(FontAwesome.CLOSE, Localized.getConstants().actionCancel(), "cancel"),
  CLOSE(FontAwesome.CLOSE, Localized.getConstants().actionClose(), "close"),
  CONFIGURE(FontAwesome.COG, Localized.getConstants().actionConfigure(), "configure"),
  COPY(FontAwesome.COPY, Localized.getConstants().actionCopy(), "copy"),
  DELETE(FontAwesome.TRASH_O, Localized.getConstants().actionDelete(), "delete"),
  EDIT(FontAwesome.EDIT, Localized.getConstants().actionEdit(), "edit"),
  EXPORT(FontAwesome.FILE_EXCEL_O, Localized.getConstants().actionExport(), "export"),
  FILTER(FontAwesome.FILTER, Localized.getConstants().actionFilter(), "filter"),
  MENU(FontAwesome.NAVICON, Localized.getConstants().menu(), "menu"),
  PRINT(FontAwesome.PRINT, Localized.getConstants().actionPrint(), "print"),
  REFRESH(FontAwesome.REFRESH, Localized.getConstants().actionRefresh(), "refresh"),
  REMOVE_FILTER(FontAwesome.REMOVE, Localized.getConstants().actionRemoveFilter(), "removeFilter"),
  RESET_SETTINGS(FontAwesome.TIMES_CIRCLE_O, Localized.getConstants().actionResetSettings(),
      "resetSettings"),
  RIGHTS(FontAwesome.EYE, Localized.getConstants().rights(), "rights"),
  SAVE(FontAwesome.SAVE, Localized.getConstants().actionSave(), "save");

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

  private final FontAwesome icon;
  private final String caption;
  private final String styleSuffix;

  private Action(FontAwesome icon, String caption, String styleSuffix) {
    this.icon = icon;
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

  public FontAwesome getIcon() {
    return icon;
  }

  public String getStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "Action-" + styleSuffix;
  }

  @Override
  public String serialize() {
    return this.name();
  }
}
