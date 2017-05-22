package com.butent.bee.shared.data;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

/**
 * Specifies implementing classes to be able to set and get their properties.
 */

public interface HasCustomProperties {

  char USER_SEPARATOR = '¦';
  int MAX_DECIMALS = 8;

  static String extractPropertyNameFromUserPropertyName(String key) {
    return BeeUtils.getPrefix(key, USER_SEPARATOR);
  }

  static Long extractUserIdFromUserPropertyName(String key) {
    return BeeUtils.toLongOrNull(BeeUtils.getSuffix(key, USER_SEPARATOR));
  }

  CustomProperties getProperties();

  String getProperty(String key);

  default String getProperty(String key, Long userId) {
    return getProperty(userPropertyName(key, userId));
  }

  default Double getPropertyDouble(String key) {
    return BeeUtils.toDoubleOrNull(getProperty(key));
  }

  default Integer getPropertyInteger(String key) {
    return BeeUtils.toIntOrNull(getProperty(key));
  }

  default Long getPropertyLong(String key) {
    return BeeUtils.toLongOrNull(getProperty(key));
  }

  default Double getPropertyDouble(String key, Long userId) {
    return getPropertyDouble(userPropertyName(key, userId));
  }

  default Integer getPropertyInteger(String key, Long userId) {
    return getPropertyInteger(userPropertyName(key, userId));
  }

  default Long getPropertyLong(String key, Long userId) {
    return getPropertyLong(userPropertyName(key, userId));
  }

  default boolean hasPropertyValue(String key) {
    return !BeeUtils.isEmpty(getProperty(key));
  }

  default boolean hasPropertyValue(String key, Long userId) {
    return hasPropertyValue(userPropertyName(key, userId));
  }

  static boolean isUserPropertyName(String key) {
    return BeeUtils.count(key, USER_SEPARATOR) == 1
        && DataUtils.isId(BeeUtils.getSuffix(key, USER_SEPARATOR));
  }

  static boolean isUserPropertyName(String key, Long userId) {
    return isUserPropertyName(key)
        && Objects.equals(extractUserIdFromUserPropertyName(key), userId);
  }

  void removeProperty(String key);

  default void removeProperty(String key, Long userId) {
    removeProperty(userPropertyName(key, userId));
  }

  default boolean sameProperties(HasCustomProperties other) {
    return other != null && BeeUtils.sameEntries(getProperties(), other.getProperties());
  }

  default void setNonZero(String key, Double value) {
    setNonZero(key, value, MAX_DECIMALS);
  }

  default void setNonZero(String key, Double value, int maxDec) {
    String s = BeeUtils.nonZero(value) ? BeeUtils.toString(value, maxDec) : null;

    if (s == null || BeeConst.STRING_ZERO.equals(s)) {
      removeProperty(key);
    } else {
      setProperty(key, s);
    }
  }

  void setProperties(CustomProperties properties);

  default void setProperty(String key, Double value) {
    String s = BeeUtils.isDouble(value) ? BeeUtils.toString(value, MAX_DECIMALS) : null;
    setProperty(key, s);
  }

  default void setProperty(String key, Integer value) {
    String s = (value == null) ? null : BeeUtils.toString(value);
    setProperty(key, s);
  }

  default void setProperty(String key, Long value) {
    String s = (value == null) ? null : BeeUtils.toString(value);
    setProperty(key, s);
  }

  void setProperty(String key, String value);

  default void setProperty(String key, Long userId, Double value) {
    setProperty(userPropertyName(key, userId), value);
  }

  default void setProperty(String key, Long userId, Integer value) {
    setProperty(userPropertyName(key, userId), value);
  }

  default void setProperty(String key, Long userId, Long value) {
    setProperty(userPropertyName(key, userId), value);
  }

  default void setProperty(String key, Long userId, String value) {
    setProperty(userPropertyName(key, userId), value);
  }

  static String userPropertyName(String key, Long userId) {
    if (!BeeUtils.isEmpty(key) && DataUtils.isId(userId)) {
      return key.trim() + USER_SEPARATOR + userId;
    } else {
      return key;
    }
  }
}
