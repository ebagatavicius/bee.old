package com.butent.bee.client;

import com.butent.bee.client.dom.Features;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * enables to store information on client side using Local Storage element from HTML5.
 */

public class Storage {

  private final Map<String, String> items = new LinkedHashMap<>();
  private final boolean localStorage;

  Storage() {
    this.localStorage = Features.supportsLocalStorage();
  }

  public void clear() {
    if (localStorage) {
      lsClear();
    } else {
      items.clear();
    }
  }

  public String get(String key) {
    Assert.notEmpty(key);

    if (localStorage) {
      return lsGetItem(key);
    } else {
      return items.get(key);
    }
  }

  public List<Property> getAll() {
    List<Property> lst = new ArrayList<>();
    int len = length();
    String z;

    for (int i = 0; i < len; i++) {
      z = key(i);
      lst.add(new Property(z, get(z)));
    }

    return lst;
  }

  public JustDate getDate(String key) {
    return TimeUtils.toDateOrNull(get(key));
  }

  public DateTime getDateTime(String key) {
    return TimeUtils.toDateTimeOrNull(get(key));
  }

  public Integer getInteger(String key) {
    return BeeUtils.toIntOrNull(get(key));
  }

  public Long getLong(String key) {
    return BeeUtils.toLongOrNull(get(key));
  }

  public Map<String, String> getSubMap(String prefix) {
    Assert.notEmpty(prefix);
    Map<String, String> result = new HashMap<>();

    int len = length();
    for (int i = 0; i < len; i++) {
      String key = key(i);
      if (BeeUtils.isPrefix(key, prefix)) {
        result.put(BeeUtils.removePrefix(key, prefix), get(key));
      }
    }

    return result;
  }

  public boolean hasItem(String key) {
    if (BeeUtils.isEmpty(key)) {
      return false;
    }
    return get(key) != null;
  }

  public String key(int index) {
    String key = null;

    if (validIndex(index)) {
      if (localStorage) {
        key = lsKey(index);
      } else {
        int i = 0;
        for (String z : items.keySet()) {
          if (i == index) {
            key = z;
            break;
          }
          i++;
        }
      }
    }
    return key;
  }

  public int length() {
    if (localStorage) {
      return lsLength();
    } else {
      return items.size();
    }
  }

  public void remove(String key) {
    Assert.notEmpty(key);

    if (localStorage) {
      lsRemoveItem(key);
    } else {
      items.remove(key);
    }
  }

  public void set(String key, DateTime value) {
    if (value == null) {
      remove(key);
    } else {
      set(key, value.serialize());
    }
  }

  public void set(String key, Integer value) {
    if (value == null) {
      remove(key);
    } else {
      set(key, BeeUtils.toString(value));
    }
  }

  public void set(String key, JustDate value) {
    if (value == null) {
      remove(key);
    } else {
      set(key, value.serialize());
    }
  }

  public void set(String key, Long value) {
    if (value == null) {
      remove(key);
    } else {
      set(key, BeeUtils.toString(value));
    }
  }

  public void set(String key, String value) {
    Assert.notEmpty(key);

    if (BeeUtils.isEmpty(value)) {
      remove(key);
    } else if (localStorage) {
      lsSetItem(key, value);
    } else {
      items.put(key, value);
    }
  }

//@formatter:off
  private native void lsClear() /*-{
    $wnd.localStorage.clear();
  }-*/;

  private native String lsGetItem(String key) /*-{
    return $wnd.localStorage.getItem(key);
  }-*/;

  private native String lsKey(int index) /*-{
    return $wnd.localStorage.key(index);
  }-*/;

  private native int lsLength() /*-{
    return $wnd.localStorage.length;
  }-*/;

  private native void lsRemoveItem(String key) /*-{
    $wnd.localStorage.removeItem(key);
  }-*/;

  private native void lsSetItem(String key, String value) /*-{
    $wnd.localStorage.setItem(key, value);
  }-*/;
//@formatter:on

  private boolean validIndex(int index) {
    return index >= 0 && index < length();
  }
}
