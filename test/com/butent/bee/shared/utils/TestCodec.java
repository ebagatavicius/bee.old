package com.butent.bee.shared.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.Pair;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestCodec {
  @Test
  public final void testAdler32ByteArray() {
    byte[] btExpected1 = {0, 97, 0, 98, 0, 99};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101};
    byte[] btExpected4 = {97, 98, 99};

    assertEquals(Integer.toHexString(38600999), Codec.adler32(btExpected4));
    assertEquals("3740127", Codec.adler32(btExpected1));
    assertEquals("405014e", Codec.adler32(btExpected2));
  }

  @Test
  public final void testAdler32String() {
    assertEquals("11e60398", Codec.adler32("Wikipedia"));
    assertEquals("24d0127", Codec.adler32("abc"));
    assertEquals("2a9014e", Codec.adler32("qwe"));
  }

  @Test
  public void testBeeSerializeDeserialize() {
    Object[] arr = new Object[] {
        null, "", "ac10", 5, -26543.735, 'y', true, false, Calendar.getInstance().getTime()};

    for (Object obj : arr) {
      String s = Codec.beeSerialize(obj);

      assertEquals((obj == null) ? obj : obj.toString(), Codec.beeDeserialize(s));
      assertNull(Codec.beeDeserializeCollection(s));
    }

    BeeSerializable ser = new BeeSerializable() {
      @Override
      public void deserialize(String s) {
      }

      @Override
      public String serialize() {
        return "SomeSerialzedText";
      }
    };
    assertEquals(Codec.beeDeserialize(Codec.beeSerialize(ser)), ser.serialize());
    assertArrayEquals(
        Codec.beeDeserializeCollection(Codec.beeSerialize(new Object[] {ser, ser, ser})),
        new Object[] {ser.serialize(), ser.serialize(), ser.serialize()});

    String[] values = new String[] {"aaa", "", "vv", null, "x", "1234623"};

    Map<String, String> valueMap = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      valueMap.put(values[i], values[i + 1]);
    }

    Object[] mArr = new Object[] {
        null,
        new String[0],
        new String[] {null},
        new String[] {""},
        values,
        Lists.newArrayList(values),
        Sets.newLinkedHashSet(Lists.newArrayList(values)),
        valueMap};

    String[] res = Codec.beeDeserializeCollection(Codec.beeSerialize(mArr));
    assertNotNull(res);
    assertEquals(res.length, mArr.length);

    for (int i = 0; i < mArr.length; i++) {
      String[] obj;

      if (mArr[i] instanceof Collection) {
        obj = ((Collection<?>) mArr[i]).toArray(new String[0]);
      } else if (mArr[i] instanceof Map) {
        obj = values;
      } else {
        obj = (String[]) mArr[i];
      }
      assertArrayEquals(obj, Codec.beeDeserializeCollection(res[i]));
    }
  }

  @Test
  public final void testCrc16ByteArray() {
    byte[] btExpected1 = {0, 97, 0, 98, 0, 99};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101};

    assertEquals("3f5c", Codec.crc16(btExpected1));
    assertEquals("3a0c", Codec.crc16(btExpected2));
  }

  @Test
  public final void testCrc16String() {
    assertEquals("9738", Codec.crc16("abc"));
    assertEquals("c0b7", Codec.crc16("qwe"));
  }

  @Test
  public final void testCrc32ByteArray() {
    byte[] btExpected1 = {0, 97, 0, 98, 0, 99};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101};

    assertEquals("8a78d0f2", Codec.crc32(btExpected1));
    assertEquals("191683de", Codec.crc32(btExpected2));
  }

  @Test
  public final void testCrc32DirectByteArray() {
    byte[] btExpected1 = {0, 97, 0, 98, 0, 99};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101};

    assertEquals("8a78d0f2", Codec.crc32Direct(btExpected1));
    assertEquals("191683de", Codec.crc32Direct(btExpected2));
  }

  @Test
  public final void testCrc32DirectString() {
    assertEquals("352441c2", Codec.crc32Direct("abc"));
    assertEquals("f7d4a193", Codec.crc32Direct("qwe"));
  }

  @Test
  public final void testCrc32String() {
    assertEquals("352441c2", Codec.crc32("abc"));
    assertEquals("f7d4a193", Codec.crc32("qwe"));
  }

  @Test
  public final void testDecodeBase64() {
    String g = "";
    for (int i = 0; i < 240; i++) {
      g += "A string";
    }
    String fc = Codec.encodeBase64(g);
    assertEquals(g, Codec.decodeBase64(fc));

    assertEquals("A string", Codec.decodeBase64("QSBzdHJpbmc="));
  }

  @Test
  public final void testDeserializeLength() {

    Pair<Integer, Integer> a = Pair.of(53, 3);
    Pair<Integer, Integer> b = Pair.of(8, 2);

    assertEquals(a.getA(), Codec.deserializeLength("253", 0).getA());
    assertEquals(a.getB(), Codec.deserializeLength("253", 0).getB());

    assertEquals(b.getA(), Codec.deserializeLength("18A String", 0).getA());
    assertEquals(b.getB(), Codec.deserializeLength("18A String", 0).getB());

    assertEquals((Object) 0, Codec.deserializeLength("08A String", 0).getA());
    assertEquals((Object) 1, Codec.deserializeLength("08A String", 0).getB());
  }

  @Test
  public final void testEncodeBase64() {
    String longBase64 = "";
    String longString = "";

    for (int i = 0; i < 2000; i++) {
      longBase64 += "YWJj";
      longString += "abc";
    }

    assertEquals(longBase64, Codec.encodeBase64(longString));
    assertEquals("YQ==", Codec.encodeBase64("a"));
  }

  @Test
  public final void testEscapeHtml() {
    assertEquals("", Codec.escapeHtml(""));
    assertEquals("", Codec.escapeHtml(null));
    assertEquals("This is an &amp;", Codec.escapeHtml("This is an &"));
    assertEquals("A  A", Codec.escapeHtml("A  A"));
  }

  @Test
  public final void testEscapeUnicode() {

    assertEquals("", Codec.escapeUnicode(""));
    assertEquals("", Codec.escapeUnicode(null));

    assertEquals("null char &#x0;", Codec.escapeUnicode("null char \u0000"));
    assertEquals("Backspace &#x8; character",
        Codec.escapeUnicode("Backspace \u0008 character"));
    assertEquals("+ ", Codec.escapeUnicode("+ "));
    assertEquals("normal string", Codec.escapeUnicode("normal string"));
  }

  @Test
  public final void testFromBase64() {
    byte[] btExpected1 = {105, -73, 28};
    byte[] btExpected2 = {97};
    byte[] btExpected3 = {83, 116, 114, 105, 110, 103};

    assertArrayEquals(btExpected1, Codec.fromBase64("abcc"));
    assertArrayEquals(btExpected2, Codec.fromBase64("YQ=="));
    assertArrayEquals(btExpected3, Codec.fromBase64("U3RyaW5n"));
  }

  @Test
  public final void testFromBytes() {
    byte[] btExpected2 = {97};
    byte[] btExpected3 = {83, 116, 114, 105, 110, 103};

    assertEquals("a", Codec.fromBytes(btExpected2));
    assertEquals("String", Codec.fromBytes(btExpected3));
  }

  @Test
  public final void testIsValidUnicodeChar() {
    assertEquals(true, Codec.isValidUnicodeChar('a'));
    assertEquals(false, Codec.isValidUnicodeChar('\u0000'));
    assertEquals(true, Codec.isValidUnicodeChar('\u0021'));
    assertEquals(false, Codec.isValidUnicodeChar('\u007f'));
    assertEquals(false, Codec.isValidUnicodeChar('\u028d'));
    assertEquals(true, Codec.isValidUnicodeChar('\u0244'));
    assertEquals(true, Codec.isValidUnicodeChar('\u0415'));
  }

  @Test
  public final void testMd5() {
    assertEquals("46a93454253d1cac8b7e9c069c00c283", Codec.md5("A string"));
    assertEquals("060bf2d587991d8f090a1309b285291c", Codec.md5("Check"));
  }

  @Test
  public final void testSerializeLength() {
    assertEquals("0", Codec.serializeLength(0));
    assertEquals("15", Codec.serializeLength(5));
    assertEquals("215", Codec.serializeLength(15));
  }

  @Test
  public final void testSerializeWithLength() {

    StringBuilder sb = new StringBuilder();
    Codec.serializeWithLength(sb, null);
    assertEquals("0", sb.toString());

    StringBuilder sb2 = new StringBuilder();
    Codec.serializeWithLength(sb2, "Hello world");
    assertEquals("211Hello world", sb2.toString());

    Codec.serializeWithLength(sb2, "A string");
    assertEquals("211Hello world18A string", sb2.toString());

    Codec.serializeWithLength(sb2, "    ");
    assertEquals("211Hello world18A string14    ", sb2.toString());

    StringBuilder sb3 = new StringBuilder();
    Codec.serializeWithLength(sb3, "");
    assertEquals("0", sb3.toString());
  }

  @Test
  public final void testToBytesString() {
    byte[] btExpected1 = {32, 33, 35};
    byte[] btExpected2 = {113, 119, 101, 32, 33, 35};
    byte[] btExpected3 = {113, 119, 101, 32, 33, 35, 101};
    assertArrayEquals(btExpected1, Codec.toBytes(" !#"));
    assertArrayEquals(btExpected2, Codec.toBytes("qwe !#"));
    assertArrayEquals(btExpected3, Codec.toBytes("qwe !#e"));

  }

  @Test
  public final void testToBytesStringInt() {
    byte[] btExpected1 = {32, 33, 35};
    byte[] btExpected2 = {101, 32, 33, 35, 101};

    assertArrayEquals(btExpected1, Codec.toBytes(" !#", 0));
    assertArrayEquals(btExpected1, Codec.toBytes("qwe !#", 3));
    assertArrayEquals(btExpected2, Codec.toBytes("qwe !#e", 2));
  }

  @Test
  public final void testToBytesStringIntInt() {
    byte[] btExpected1 = {32, 33, 35};
    byte[] btExpected2 = {33, 35};

    assertArrayEquals(btExpected1, Codec.toBytes(" !#", 0, 3));
    assertArrayEquals(btExpected2, Codec.toBytes(" !#", 1, 3));
    assertArrayEquals(btExpected1, Codec.toBytes("qwe !#erty", 3, 6));
    assertArrayEquals(btExpected1, Codec.toBytes("qwe !#", 3, 6));
  }

  @Test
  public final void testToHexByteArray() {

    byte[] bt = {0, 32, 0, 33, 0, 35};
    byte[] bt2 = null;
    byte[] bt3 = {};

    assertEquals("002000210023", Codec.toHex(bt));
    assertEquals(null, Codec.toHex(bt3));
    assertEquals(null, Codec.toHex(bt2));
  }

  @Test
  public final void testToHexChar() {

    assertEquals("0063", Codec.toHex('c'));
    assertEquals("0020", Codec.toHex(' '));
    assertEquals("0023", Codec.toHex('\u0023'));
  }

  @Test
  public final void testToHexCharArray() {

    char[] mas = {'a', 'b', 'c'};
    char[] mas2 = {};
    char[] mas3 = {' '};
    char[] mas4 = null;

    assertEquals("006100620063", Codec.toHex(mas));
    assertEquals(null, Codec.toHex(mas2));
    assertEquals("0020", Codec.toHex(mas3));
    assertEquals(null, Codec.toHex(mas4));
  }
}
