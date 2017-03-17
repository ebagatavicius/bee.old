package com.butent.bee.shared;

import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;

public final class Triplet<A, B, C> implements BeeSerializable {

  public static <A, B, C> Triplet<A, B, C> empty() {
    return new Triplet<>();
  }

  public static <A, B, C> Triplet<A, B, C> of(A a, B b, C c) {
    return new Triplet<>(a, b, c);
  }

  public static Triplet<String, String, String> restore(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 3);

    return Triplet.of(arr[0], arr[1], arr[2]);
  }

  private A a;
  private B b;
  private C c;

  private Triplet() {
  }

  private Triplet(A a, B b, C c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  public boolean aEquals(A value) {
    return Objects.equals(getA(), value);
  }

  public boolean bEquals(B value) {
    return Objects.equals(getB(), value);
  }

  public boolean cEquals(C value) {
    return Objects.equals(getC(), value);
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
    if (!(obj instanceof Triplet)) {
      return false;
    }
    return Objects.equals(getA(), ((Triplet<?, ?, ?>) obj).getA())
        && Objects.equals(getB(), ((Triplet<?, ?, ?>) obj).getB())
        && Objects.equals(getC(), ((Triplet<?, ?, ?>) obj).getC());
  }

  public A getA() {
    return a;
  }

  public B getB() {
    return b;
  }

  public C getC() {
    return c;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getA(), getB(), getC());
  }

  public boolean isNull() {
    return getA() == null && getB() == null;
  }

  public boolean noNulls() {
    return getA() != null && getB() != null;
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[] {getA(), getB(), getC()};
    return Codec.beeSerialize(arr);
  }

  public void setA(A a) {
    this.a = a;
  }

  public void setB(B b) {
    this.b = b;
  }

  public void setC(C c) {
    this.c = c;
  }

  @Override
  public String toString() {
    return BeeUtils.joinItems(a, b, c);
  }
}
