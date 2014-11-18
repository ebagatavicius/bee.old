package com.butent.bee.shared.utils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasLocalizedCaption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EnumUtils {

  public static final String ATTR_ENUM_KEY = "enumKey";

  private static final BeeLogger logger = LogUtils.getLogger(EnumUtils.class);

  private static final BiMap<String, Class<? extends Enum<?>>> CLASSES = HashBiMap.create();

  private static final char LIST_SEPARATOR = ',';

  private static final Joiner joiner = Joiner.on(LIST_SEPARATOR).skipNulls();
  private static final Splitter splitter =
      Splitter.on(LIST_SEPARATOR).omitEmptyStrings().trimResults();

  private static final Function<Enum<?>, Integer> indexFunction = new Function<Enum<?>, Integer>() {
    @Override
    public Integer apply(Enum<?> input) {
      return (input == null) ? null : input.ordinal();
    }
  };

  static {
    AdministrationConstants.register();
    ClassifierConstants.register();
    CalendarConstants.register();
    TaskConstants.register();
    DiscussionsConstants.register();
    EcConstants.register();
    TransportConstants.register();
    ServiceConstants.register();
    MailConstants.register();
    TradeActConstants.register();
  }

  public static String buildIndexList(Collection<? extends Enum<?>> values) {
    if (values == null) {
      return null;
    } else {
      return joiner.join(Iterables.transform(values, indexFunction));
    }
  }

  public static String getCaption(Class<? extends Enum<?>> clazz, Integer index) {
    return getLocalizedCaption(clazz, index, Localized.getConstants());
  }

  public static String getCaption(String key, Integer index) {
    return getLocalizedCaption(key, index, Localized.getConstants());
  }

  public static List<String> getCaptions(Class<? extends Enum<?>> clazz) {
    return getLocalizedCaptions(clazz, Localized.getConstants());
  }

  public static List<String> getCaptions(String key) {
    return getLocalizedCaptions(key, Localized.getConstants());
  }

  public static <E extends Enum<?>> E getEnumByIndex(Class<E> clazz, Integer idx) {
    if (clazz == null || idx == null || idx < 0) {
      return null;
    }
    E[] constants = clazz.getEnumConstants();

    if (constants != null && idx < constants.length) {
      return constants[idx];
    } else {
      return null;
    }
  }

  public static <E extends Enum<?>> E getEnumByIndex(Class<E> clazz, String s) {
    return getEnumByIndex(clazz, BeeUtils.toIntOrNull(s));
  }

  public static <E extends Enum<?>> E getEnumByName(Class<E> clazz, String name) {
    Assert.notNull(clazz);
    if (BeeUtils.isEmpty(name)) {
      return null;
    }
    E result = null;

    for (int i = 0; i < 4; i++) {
      String input = (i == 1) ? NameUtils.normalizeEnumName(name) : name.trim();

      for (E constant : clazz.getEnumConstants()) {
        if (i == 0) {
          if (BeeUtils.same(constant.name(), input)) {
            result = constant;
            break;
          }

        } else if (i == 1) {
          if (!input.isEmpty() && input.equals(NameUtils.normalizeEnumName(constant.name()))) {
            result = constant;
            break;
          }

        } else if (i == 2) {
          if (BeeUtils.startsSame(constant.name(), input)) {
            if (result == null) {
              result = constant;
            } else {
              result = null;
              break;
            }
          }

        } else {
          if (BeeUtils.containsSame(constant.name(), input)) {
            if (result == null) {
              result = constant;
            } else {
              result = null;
              break;
            }
          }
        }
      }
      if (result != null) {
        break;
      }
    }
    return result;
  }

  public static String getLocalizedCaption(Class<? extends Enum<?>> clazz, Integer index,
      LocalizableConstants constants) {

    if (index == null) {
      return null;
    }

    List<String> list = getLocalizedCaptions(clazz, constants);

    if (!BeeUtils.isIndex(list, index)) {
      logger.severe("cannot get caption: class", NameUtils.getClassName(clazz), "index", index);
      return null;
    } else {
      return list.get(index);
    }
  }

  public static String getLocalizedCaption(String key, Integer index,
      LocalizableConstants constants) {

    if (BeeUtils.isEmpty(key)) {
      logger.severe("Caption key not specified");
      return null;
    }
    if (index == null) {
      return null;
    }

    List<String> list = getLocalizedCaptions(key, constants);

    if (!BeeUtils.isIndex(list, index)) {
      logger.severe("cannot get caption: key", key, "index", index);
      return null;
    } else {
      return list.get(index);
    }
  }

  public static List<String> getLocalizedCaptions(Class<? extends Enum<?>> clazz,
      LocalizableConstants constants) {
    Assert.notNull(clazz);
    Assert.notNull(constants);

    List<String> result = new ArrayList<>();

    for (Enum<?> constant : clazz.getEnumConstants()) {
      if (constant instanceof HasLocalizedCaption) {
        result.add(((HasLocalizedCaption) constant).getCaption(constants));
      } else if (constant instanceof HasCaption) {
        result.add(((HasCaption) constant).getCaption());
      } else {
        result.add(BeeUtils.proper(constant));
      }
    }
    return result;
  }

  public static List<String> getLocalizedCaptions(String key, LocalizableConstants constants) {
    Assert.notEmpty(key);
    Class<? extends Enum<?>> clazz = CLASSES.get(BeeUtils.normalize(key));

    if (clazz == null) {
      logger.severe("Captions not registered: " + key);
      return null;
    } else {
      return getLocalizedCaptions(clazz, constants);
    }
  }

  public static Set<String> getRegisteredKeys() {
    return CLASSES.keySet();
  }

  public static <E extends Enum<?>> String getRegistrationKey(Class<E> clazz) {
    String key = CLASSES.inverse().get(clazz);

    if (BeeUtils.isEmpty(key)) {
      key = register(clazz);
    }
    return key;
  }

  @SafeVarargs
  public static <E extends Enum<?>> boolean in(E x, E first, E second, E... rest) {
    if (x == null) {
      return false;

    } else if (x == first || x == second) {
      return true;

    } else if (rest == null) {
      return false;

    } else {
      for (E y : rest) {
        if (x == y) {
          return true;
        }
      }
      return false;
    }
  }

  public static boolean isRegistered(String key) {
    return CLASSES.containsKey(BeeUtils.normalize(key));
  }

  public static <E extends Enum<?>> List<E> parseIndexList(Class<E> clazz, String input) {
    List<E> result = new ArrayList<>();
    if (clazz == null || BeeUtils.isEmpty(input)) {
      return result;
    }

    for (String s : splitter.split(input)) {
      E e = getEnumByIndex(clazz, s);
      if (e != null) {
        result.add(e);
      }
    }

    return result;
  }

  public static <E extends Enum<?>> List<E> parseNameList(Class<E> clazz, String input) {
    List<E> result = new ArrayList<>();
    if (clazz == null || BeeUtils.isEmpty(input)) {
      return result;
    }

    for (String s : splitter.split(input)) {
      E e = getEnumByName(clazz, s);
      if (e != null) {
        result.add(e);
      }
    }

    return result;
  }

  public static <E extends Enum<?>> Set<E> parseIndexSet(Class<E> clazz, String input) {
    Set<E> result = new HashSet<>();
    if (clazz == null || BeeUtils.isEmpty(input)) {
      return result;
    }

    for (String s : splitter.split(input)) {
      E e = getEnumByIndex(clazz, s);
      if (e != null) {
        result.add(e);
      }
    }

    return result;
  }

  public static <E extends Enum<?>> String register(Class<E> clazz) {
    Assert.notNull(clazz);
    return register(NameUtils.getClassName(clazz), clazz);
  }

  public static <E extends Enum<?>> String register(String key, Class<E> clazz) {
    Assert.notEmpty(key);
    Assert.notNull(clazz);
    Assert.state(!isRegistered(key), "Key already registered: " + key);

    String normalized = BeeUtils.normalize(key);

    CLASSES.put(normalized, clazz);
    return normalized;
  }

  private EnumUtils() {
  }
}
