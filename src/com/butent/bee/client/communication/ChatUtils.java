package com.butent.bee.client.communication;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Storage;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ChatUtils {

  public static void applyPresence(Widget widget, Multimap<Presence, Long> presence) {
    if (presence.keySet().size() == 1) {
      Presence p = BeeUtils.peek(presence.keySet());
      StyleUtils.setBackgroundColor(widget, p.getBackground());

    } else if (presence.keySet().size() > 1) {
      List<String> colors = new ArrayList<>();

      List<Presence> keys = new ArrayList<>(presence.keySet());
      Collections.sort(keys);

      for (Presence p : keys) {
        String color = p.getBackground();

        for (int i = 0; i < presence.get(p).size(); i++) {
          colors.add(color);
        }
      }

      widget.getElement().getStyle().setBackgroundImage("linear-gradient(to bottom, "
          + BeeUtils.joinItems(colors) + ")");
    }
  }

  public static String buildUserTitle(Collection<Long> users, boolean addPresence) {
    List<String> lines = new ArrayList<>();

    if (!BeeUtils.isEmpty(users)) {
      for (Long u : users) {
        String signature = Global.getUsers().getSignature(u);

        if (addPresence) {
          String presence = EnumUtils.getCaption(Global.getUsers().getUserPresence(u));
          lines.add(BeeUtils.joinItems(signature, BeeUtils.toLowerCase(presence)));
        } else {
          lines.add(signature);
        }
      }
    }

    return BeeUtils.buildLines(lines);
  }

  public static int countOtherUsers(Collection<Long> users) {
    int size = BeeUtils.size(users);
    if (size > 0 && users.contains(BeeKeeper.getUser().getUserId())) {
      size--;
    }
    return size;
  }

  public static String elapsed(long start) {
    if (start <= 0) {
      return null;
    }

    long diff = System.currentTimeMillis() - start;

    DateTime dt = new DateTime(start - start % TimeUtils.MILLIS_PER_MINUTE);
    String time = TimeUtils.renderMinutes(TimeUtils.minutesSinceDayStarted(dt), true);

    if (diff <= 0) {
      return BeeConst.STRING_EMPTY;

    } else if (diff < TimeUtils.MILLIS_PER_SECOND) {
      return time;

    } else if (diff < TimeUtils.MILLIS_PER_MINUTE) {
      return format(time, diff / TimeUtils.MILLIS_PER_SECOND, "s");

    } else if (diff < TimeUtils.MILLIS_PER_HOUR) {
      return format(time, diff / TimeUtils.MILLIS_PER_MINUTE, "m");

    } else if (diff < TimeUtils.MILLIS_PER_DAY / 2) {
      return format(time, diff / TimeUtils.MILLIS_PER_HOUR, "h");

    } else if (diff < TimeUtils.MILLIS_PER_DAY * 2
        && TimeUtils.dayDiff(dt, TimeUtils.today()) == 1) {

      return Localized.dictionary().yesterday() + BeeConst.STRING_SPACE + time;

    } else if (diff < TimeUtils.MILLIS_PER_DAY) {
      return format(time, diff / TimeUtils.MILLIS_PER_HOUR, "h");

    } else {
      return Format.renderDateTime(dt);
    }
  }

  public static String format(String label, long diff, String unit) {
    if (diff > 0) {
      return label + BeeConst.STRING_EMPTY + BeeConst.STRING_SPACE
          + BeeConst.STRING_LEFT_PARENTHESIS + BeeUtils.toString(diff) + unit
          + BeeConst.STRING_RIGHT_PARENTHESIS;
    } else {
      return label;
    }
  }

  public static String getChatCaption(String name, List<Long> otherUsers) {
    if (!BeeUtils.isEmpty(name) || BeeUtils.isEmpty(otherUsers)) {
      return name;

    } else if (otherUsers.size() == 1) {
      return Global.getUsers().getSignature(otherUsers.get(0));

    } else {
      return getFirstNames(otherUsers);
    }
  }

  public static String getFirstNames(Collection<Long> users) {
    List<String> names = new ArrayList<>();

    if (!BeeUtils.isEmpty(users)) {
      for (Long u : users) {
        names.add(Global.getUsers().getFirstName(u));
      }
    }

    return BeeUtils.joinItems(names);
  }

  public static List<Long> getOtherUsers(List<Long> users) {
    List<Long> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(users)) {
      for (Long u : users) {
        if (DataUtils.isId(u) && !result.contains(u) && !BeeKeeper.getUser().is(u)) {
          result.add(u);
        }
      }
    }

    return result;
  }

  public static String getSizeStorageKey(long chatId) {
    return Storage.getUserKey("chat-" + chatId, "size");
  }

  public static String getStyleStorageKey(long chatId) {
    return Storage.getUserKey("chat-" + chatId, "style");
  }

  public static Multimap<Presence, Long> getUserPresence(Collection<Long> users) {
    Multimap<Presence, Long> result = ArrayListMultimap.create();
    Presence p;

    for (Long u : users) {
      if (BeeKeeper.getUser().is(u)) {
        p = BeeKeeper.getUser().getPresence();
      } else {
        p = Global.getUsers().getUserPresence(u);
      }

      if (p == null) {
        p = Presence.OFFLINE;
      }

      result.put(p, u);
    }

    return result;
  }

  public static boolean needsRefresh(long millis) {
    return System.currentTimeMillis() - millis < TimeUtils.MILLIS_PER_DAY * 2;
  }

  public static void renderUsers(HasIndexedWidgets container, List<Long> users,
      String itemStyleName) {

    Assert.notNull(container);
    if (!container.isEmpty()) {
      container.clear();
    }

    if (!BeeUtils.isEmpty(users)) {
      for (int i = 0; i < users.size(); i++) {
        UserData userData = Global.getUsers().getUserData(users.get(i));

        if (userData != null) {
          String text = userData.getFirstName();
          if (i < users.size() - 1) {
            text = BeeUtils.trim(text) + BeeConst.DEFAULT_LIST_SEPARATOR;
          }

          CustomDiv widget = new CustomDiv(itemStyleName);
          widget.setText(text);
          widget.setTitle(userData.getUserSign());

          container.add(widget);
        }
      }
    }
  }

  public static <T extends HasHtml & IsWidget> void updateTime(T widget, long time) {
    if (widget != null && time > 0) {
      widget.setText(elapsed(time));
      widget.asWidget().setTitle(Format.renderDateTimeFull(new DateTime(time)));
    }
  }

  private ChatUtils() {
  }
}
