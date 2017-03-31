package com.butent.bee.client.ui;

import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public enum UiOption {
  CHILD(EnumSet.of(Type.SEARCH, Type.SETTINGS)),
  EDITOR(EnumSet.of(Type.CLOSABLE)),
  EMBEDDED(EnumSet.of(Type.PAGING, Type.SEARCH, Type.SETTINGS)),
  GRID(EnumSet.of(Type.PAGING, Type.SEARCH, Type.SETTINGS, Type.CLOSABLE)),
  VIEW(EnumSet.of(Type.CLOSABLE));

  private enum Type {
    PAGING, SEARCH, SETTINGS, CLOSABLE
  }

  public static String getStyleName(Collection<UiOption> options) {
    if (BeeUtils.isEmpty(options)) {
      return null;

    } else {
      Set<String> classes = new HashSet<>();
      for (UiOption option : options) {
        if (option != null) {
          classes.add(BeeConst.CSS_CLASS_PREFIX + "ui-" + option.name().toLowerCase());
        }
      }
      return StyleUtils.buildClasses(classes);
    }
  }

  public static boolean hasPaging(Collection<UiOption> options) {
    return hasType(options, Type.PAGING);
  }

  public static boolean hasSearch(Collection<UiOption> options) {
    return hasType(options, Type.SEARCH);
  }

  public static boolean hasSettings(Collection<UiOption> options) {
    return hasType(options, Type.SETTINGS);
  }

  public static boolean isChild(Collection<UiOption> options) {
    return options != null && options.contains(CHILD);
  }

  public static boolean isChildOrEmbedded(Collection<UiOption> options) {
    return options != null && (options.contains(CHILD) || options.contains(EMBEDDED));
  }

  public static boolean isClosable(Collection<UiOption> options) {
    return hasType(options, Type.CLOSABLE);
  }

  private static boolean hasType(Collection<UiOption> options, Type type) {
    if (options == null) {
      return false;
    }

    for (UiOption option : options) {
      if (option.hasType(type)) {
        return true;
      }
    }
    return false;
  }

  private final Set<Type> types;

  UiOption(Set<Type> types) {
    this.types = types;
  }

  private Set<Type> getTypes() {
    return types;
  }

  private boolean hasType(Type type) {
    if (type == null || getTypes() == null) {
      return false;
    }
    return getTypes().contains(type);
  }
}
