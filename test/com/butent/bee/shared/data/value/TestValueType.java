package com.butent.bee.shared.data.value;

import com.butent.bee.shared.time.DateTime;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.data.value.ValueType}.
 */
@SuppressWarnings("static-method")
public class TestValueType {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCreateValue() {
    assertEquals(BooleanValue.getNullValue(), ValueType.valueOf("BOOLEAN").createValue(null));
    assertEquals(new TextValue("String inside"), ValueType.valueOf("TEXT").createValue(
        "String inside"));
    assertEquals(new NumberValue(5.0), ValueType.valueOf("NUMBER").createValue(5.0));
    assertEquals(BooleanValue.TRUE, ValueType.valueOf("BOOLEAN").createValue(true));
    assertEquals(BooleanValue.FALSE, ValueType.valueOf("BOOLEAN").createValue(false));
    assertEquals(new DateValue(2011, 02, 22), ValueType.valueOf("DATE").createValue(
        new DateTime(1298362388227L)));
    assertEquals(new DateTimeValue(2011, 02, 22, 10, 13, 8), ValueType.valueOf("DATE_TIME")
        .createValue(new DateTime(2011, 02, 22, 10, 13, 8)));
    assertEquals(new DateTimeValue(2011, 02, 22, 10, 13, 8), ValueType.valueOf("DATE_TIME")
        .createValue(new DateTime(2011, 02, 22, 10, 13, 8)));
    assertEquals(new TimeOfDayValue(10, 13, 8, 227), ValueType.valueOf("TIME_OF_DAY").createValue(
        "10:13:08.227"));
  }

  @Test
  public final void testGetByTypeCode() {
    assertEquals(ValueType.BOOLEAN, ValueType.getByTypeCode("boolean"));
    assertEquals(ValueType.DATE_TIME, ValueType.getByTypeCode("Datetime"));
    assertEquals(ValueType.DATE, ValueType.getByTypeCode(" date "));
    assertEquals(ValueType.NUMBER, ValueType.getByTypeCode("double"));
    assertEquals(null, ValueType.getByTypeCode("TEXT"));
    assertEquals(ValueType.TEXT, ValueType.getByTypeCode("STRING"));
    assertEquals(ValueType.TIME_OF_DAY, ValueType.getByTypeCode("timeofday"));
    assertEquals(null, ValueType.getByTypeCode(null));
  }

  @Test
  public final void testGetTypeCode() {
    assertEquals("boolean", ValueType.valueOf("BOOLEAN").getTypeCode());
    assertEquals("double", ValueType.valueOf("NUMBER").getTypeCode());
    assertEquals("string", ValueType.valueOf("TEXT").getTypeCode());
  }

  @Test
  public final void testIsNumber() {
    assertEquals(false, ValueType.isNumeric(ValueType.getByTypeCode("boolean")));
    assertEquals(false, ValueType.isNumeric(null));
    assertEquals(true, ValueType.isNumeric(ValueType.getByTypeCode("decimal")));
  }
}
