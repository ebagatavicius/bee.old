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
  ADD(FontAwesome.PLUS, Localized.dictionary().actionAdd(), "add", true, true),
  AUDIT(FontAwesome.HISTORY, Localized.dictionary().actionAudit(), "audit", false, true),
  AUTO_FIT(FontAwesome.ARROWS_H, Localized.dictionary().autoFit(), "auto-fit", false, true),
  BOOKMARK(FontAwesome.BOOKMARK_O, Localized.dictionary().actionBookmark(), "bookmark", false,
      true),
  CANCEL(FontAwesome.CLOSE, Localized.dictionary().actionCancel(), "cancel", false, false),
  CLOSE(FontAwesome.CLOSE, Localized.dictionary().actionClose(), "close", false, false),
  CONFIGURE(FontAwesome.COG, Localized.dictionary().actionConfigure(), "configure", false, true),
  COPY(FontAwesome.COPY, Localized.dictionary().actionCopy(), "copy", true, true),
  DELETE(FontAwesome.TRASH_O, Localized.dictionary().actionDelete(), "delete", true, true, 2.0),
  EDIT(FontAwesome.EDIT, Localized.dictionary().actionEdit(), "edit", true, true),
  EXPORT(FontAwesome.FILE_EXCEL_O, Localized.dictionary().actionExport(), "export", false, true),
  FILTER(FontAwesome.FILTER, Localized.dictionary().actionFilter(), "filter", false, true),
  MAXIMIZE(FontAwesome.SQUARE_O, Localized.dictionary().actionMaximize(), "maximize", false, true),
  MENU(FontAwesome.NAVICON, Localized.dictionary().menu(), "menu", false, true),
  MINIMIZE(FontAwesome.MINUS, Localized.dictionary().actionMinimize(), "minimize", false, true),
  MERGE(FontAwesome.OBJECT_GROUP, Localized.dictionary().actionMerge(), "merge", true, true),
  MOVE(FontAwesome.ARROWS, Localized.dictionary().actionMove(), "move", true, true),
  PRINT(FontAwesome.PRINT, Localized.dictionary().actionPrint(), "print", false, true),
  REFRESH(FontAwesome.REFRESH, Localized.dictionary().actionRefresh(), "refresh", false, true, 2.0),
  REMOVE_FILTER(FontAwesome.REMOVE, Localized.dictionary().actionRemoveFilter(), "removeFilter",
      false, false),
  RESET_SETTINGS(FontAwesome.TIMES_CIRCLE_O, Localized.dictionary().actionResetSettings(),
      "resetSettings", false, true),
  RIGHTS(FontAwesome.EYE, Localized.dictionary().rights(), "rights", true, true),
  SAVE(FontAwesome.SAVE, Localized.dictionary().actionSave(), "save", true, true, 2.0);

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
  private final boolean disablable;
  private final boolean animate;

  private final double sensitivityRatio;

  Action(FontAwesome icon, String caption, String styleSuffix, boolean disablable,
      boolean animate) {
    this(icon, caption, styleSuffix, disablable, animate,
        animate ? BeeConst.DOUBLE_ONE : BeeConst.DOUBLE_UNDEF);
  }

  Action(FontAwesome icon, String caption, String styleSuffix, boolean disablable,
      boolean animate, double sensitivityRatio) {

    this.icon = icon;
    this.caption = caption;
    this.styleSuffix = styleSuffix;
    this.disablable = disablable;
    this.animate = animate;
    this.sensitivityRatio = sensitivityRatio;
  }

  public boolean animate() {
    return animate;
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

  public double getSensitivityRatio() {
    return sensitivityRatio;
  }

  public String getStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "Action-" + styleSuffix;
  }

  public String getStyleSuffix() {
    return styleSuffix;
  }

  public boolean isDisablable() {
    return disablable;
  }

  @Override
  public String serialize() {
    return this.name();
  }
}
