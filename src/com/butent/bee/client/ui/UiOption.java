package com.butent.bee.client.ui;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public enum UiOption {
  CHILD(EnumSet.of(Type.SEARCH, Type.SETTINGS)),
  EMBEDDED(EnumSet.of(Type.PAGING, Type.SEARCH, Type.SETTINGS)),
  GRID(EnumSet.of(Type.PAGING, Type.SEARCH, Type.SETTINGS, Type.WINDOW)),
  VIEW(EnumSet.of(Type.WINDOW));

  private enum Type {
    PAGING, SEARCH, SETTINGS, WINDOW
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

  public static boolean isWindow(Collection<UiOption> options) {
    return hasType(options, Type.WINDOW);
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

  private UiOption(Set<Type> types) {
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
