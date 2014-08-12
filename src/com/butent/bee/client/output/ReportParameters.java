package com.butent.bee.client.output;

import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

  public DateTime getDateTime(String key) {
    return TimeUtils.toDateTimeOrNull(get(key));
  }

  public Integer getInteger(String key) {
    return BeeUtils.toIntOrNull(get(key));
  }

  public Long getLong(String key) {
    return BeeUtils.toLongOrNull(get(key));
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
