package com.butent.bee.shared.report;

import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class ReportParameters extends LinkedHashMap<String, String> implements BeeSerializable {

  public static ReportParameters restore(String s) {
    ReportParameters rp = new ReportParameters();
    rp.deserialize(s);
    return rp;
  }

  public ReportParameters() {
    super();
  }

  public ReportParameters(int initialCapacity) {
    super(initialCapacity);
  }

  public ReportParameters(Map<? extends String, ? extends String> m) {
    super(m);
  }

  public void add(String key, Boolean value) {
    if (BeeUtils.isTrue(value)) {
      put(key, Codec.pack(value));
    }
  }

  public void add(String key, DateTime value) {
    if (value != null) {
      put(key, value.serialize());
    }
  }

  public void add(String key, Integer value) {
    if (value != null) {
      put(key, BeeUtils.toString(value));
    }
  }

  public void add(String key, Long value) {
    if (value != null) {
      put(key, BeeUtils.toString(value));
    }
  }

  public void add(String key, String value) {
    if (!BeeUtils.isEmpty(value)) {
      put(key, value.trim());
    }
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    if (arr != null) {
      for (int i = 0; i < arr.length - 1; i += 2) {
        put(arr[i], arr[i + 1]);
      }
    }
  }

  public boolean getBoolean(String key) {
    return Codec.unpack(get(key));
  }

  public DateTime getDateTime(String key) {
    return TimeUtils.toDateTimeOrNull(get(key));
  }

  public <E extends Enum<?>> E getEnum(String key, Class<E> clazz) {
    return EnumUtils.getEnumByIndex(clazz, getInteger(key));
  }

  public Set<Long> getIds(String key) {
    return DataUtils.parseIdSet(get(key));
  }

  public Integer getInteger(String key) {
    return BeeUtils.toIntOrNull(get(key));
  }

  public Long getLong(String key) {
    return BeeUtils.toLongOrNull(get(key));
  }

  public String getText(String key) {
    return get(key);
  }

  @Override
  public String serialize() {
    List<String> list = new ArrayList<>();
    for (Map.Entry<String, String> entry : entrySet()) {
      list.add(entry.getKey());
      list.add(entry.getValue());
    }

    return Codec.beeSerialize(list);
  }
}
