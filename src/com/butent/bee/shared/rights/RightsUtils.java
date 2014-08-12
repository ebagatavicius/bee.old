package com.butent.bee.shared.rights;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class RightsUtils {

  private static final String NAME_SEPARATOR = ".";

  public static final Joiner NAME_JOINER = Joiner.on(NAME_SEPARATOR).skipNulls();
  public static final Splitter NAME_SPLITTER = Splitter.on(NAME_SEPARATOR);

  private static final String STATE_ROLE_ALIAS_PREFIX = "StateRole";
  private static final String STATE_ROLE_ALIAS_SEPARATOR = "_";

  public static final Splitter STATE_ROLE_ALIAS_SPLITTER = Splitter.on(STATE_ROLE_ALIAS_SEPARATOR);

  private static final Map<String, String> VIEW_MODULES = new HashMap<>();

  public static String buildName(String parent, String child) {
    if (BeeUtils.isEmpty(parent)) {
      return normalizeName(child);
    } else {
      return NAME_JOINER.join(normalizeName(parent), normalizeName(child));
    }
  }

  public static String getAlias(RightsState state, Long role) {
    Assert.notNull(state);
    Assert.isTrue(BeeUtils.isNonNegative(role));

    return STATE_ROLE_ALIAS_PREFIX + STATE_ROLE_ALIAS_SEPARATOR
        + state.ordinal() + STATE_ROLE_ALIAS_SEPARATOR + role;
  }

  public static String getViewModules(String viewName) {
    return VIEW_MODULES.get(BeeUtils.normalize(viewName));
  }

  public static String getViewModulesAsString() {
    return Codec.beeSerialize(VIEW_MODULES);
  }

  public static String normalizeName(String name) {
    return BeeUtils.trim(name);
  }

  public static Pair<RightsState, Long> parseStateRoleAlias(String input) {
    Assert.notEmpty(input);

    List<String> list = Lists.newArrayList(STATE_ROLE_ALIAS_SPLITTER.split(input));

    int size = list.size();
    Assert.isTrue(size >= 2);

    RightsState state = EnumUtils.getEnumByIndex(RightsState.class, list.get(size - 2));
    Long role = BeeUtils.toLongOrNull(list.get(size - 1));

    Assert.notNull(state);
    Assert.isTrue(BeeUtils.isNonNegative(role));

    return Pair.of(state, role);
  }

  public static void setViewModules(Map<String, String> viewModules) {
    VIEW_MODULES.clear();

    if (!BeeUtils.isEmpty(viewModules)) {
      for (Entry<String, String> entry : viewModules.entrySet()) {
        VIEW_MODULES.put(BeeUtils.normalize(entry.getKey()), entry.getValue());
      }
    }
  }

  private RightsUtils() {
  }
}