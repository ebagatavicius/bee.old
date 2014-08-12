package com.butent.bee.shared;

/**
 * Requires implementing notification management classes to contain methods to handle info, severe
 * and warning level messages.
 */

public interface NotificationListener {

  void clearNotifications();

  boolean hasNotifications();

  void notifyInfo(String... messages);

  void notifySevere(String... messages);

  void notifyWarning(String... messages);
}
