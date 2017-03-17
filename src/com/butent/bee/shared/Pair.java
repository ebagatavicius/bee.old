package com.butent.bee.shared;

import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;

/**
 * Defines pairs of objects.
 *
 * @param <A> type of first object to pair
 * @param <B> type of second object to pair
 */
public class Pair<A, B> implements BeeSerializable {

  private enum Serial {
    A, B
  }

  public static <A, B> Pair<A, B> empty() {
    return new Pair<>();
  }

  /**
   * Creates the new {@code Pair} object passing the pair of objects.
   *
   * @param a object to pair the object {@code b}
   * @param b object to pair the object {@code a}
   */
  public static <A, B> Pair<A, B> of(A a, B b) {
    return new Pair<>(a, b);
  }

  public static Pair<String, String> restore(String s) {
    Serial[] members = Serial.values();
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, members.length);
    String aa = null;
    String bb = null;

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case A:
          aa = value;
          break;

        case B:
          bb = value;
          break;
      }
    }
    return Pair.of(aa, bb);
  }

  private A a;
  private B b;

  private Pair() {
  }

  protected Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public boolean aEquals(A value) {
    return Objects.equals(getA(), value);
  }

  public boolean bEquals(B value) {
    return Objects.equals(getB(), value);
  }

  @Override
  public void deserialize(String s) {
    Assert.untouchable();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Pair)) {
      return false;
    }
    return Objects.equals(getA(), ((Pair<?, ?>) obj).getA())
        && Objects.equals(getB(), ((Pair<?, ?>) obj).getB());
  }

  /**
   * Returns the first object or value of object.
   *
   * @return the first object or value of object.
   */
  public A getA() {
    return a;
  }

  /**
   * Returns the second object or value of object.
   *
   * @return the second object or value of object.
   */
  public B getB() {
    return b;
  }

  @Override
  public int hashCode() {
    return 1 + Objects.hash(getA(), getB());
  }

  public boolean isNull() {
    return getA() == null && getB() == null;
  }

  public boolean noNulls() {
    return getA() != null && getB() != null;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case A:
          arr[i++] = getA();
          break;

        case B:
          arr[i++] = getB();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setA(A x) {
    this.a = x;
  }

  public void setB(B x) {
    this.b = x;
  }

  /**
   * Converts pair of objects to {@code String}.
   *
   * @return {@code String} of objects pair
   */
  @Override
  public String toString() {
    return BeeUtils.join(BeeConst.STRING_SPACE, a, b);
  }
}
