package com.butent.bee.shared;

import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface BeeSerializable {

  void deserialize(String data);

  default <T extends Enum> void processMembers(Class<T> serial, String data,
      BiConsumer<T, String> processor) {

    String[] arr = Codec.beeDeserializeCollection(data);
    T[] members = serial.getEnumConstants();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      processor.accept(members[i], arr[i]);
    }
  }

  String serialize();

  default <T extends Enum> String serializeMembers(Class<T> serial, Function<T, Object> processor) {
    T[] members = serial.getEnumConstants();
    Object[] arr = new Object[members.length];
    Latch i = new Latch(0);

    for (T member : members) {
      arr[i.get()] = processor.apply(member);
      i.increment();
    }
    return Codec.beeSerialize(arr);
  }

  static <T extends BeeSerializable> T restore(String s, Supplier<T> supplier) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    T instance = supplier.get();
    instance.deserialize(s);
    return instance;
  }
}
