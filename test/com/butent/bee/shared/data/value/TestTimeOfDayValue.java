package com.butent.bee.shared.data.value;

import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.time.DateTime;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.data.value.TimeOfDayValue}.
 */
@SuppressWarnings("static-method")
public class TestTimeOfDayValue {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testCompareTo() {
    TimeOfDayValue value = new TimeOfDayValue(8, 20, 20);
    TimeOfDayValue value2 = new TimeOfDayValue(8, 20, 20, 227);

    assertEquals(0, value.compareTo(value));
    assertEquals(1, value2.compareTo(value));
    assertEquals(-1, value.compareTo(value2));
    assertEquals(1, value.compareTo(TimeOfDayValue.getNullValue()));
    assertEquals(-1, TimeOfDayValue.getNullValue().compareTo(value));

    assertEquals(1, new TimeOfDayValue(8, 20, 20).compareTo(new TimeOfDayValue(8, 20, 19)));
    assertEquals(1, new TimeOfDayValue(8, 20, 20).compareTo(new TimeOfDayValue(8, 19, 21)));
    assertEquals(1, new TimeOfDayValue(8, 20, 20).compareTo(new TimeOfDayValue(7, 20, 19)));

    assertEquals(-1, new TimeOfDayValue(6, 20, 25).compareTo(new TimeOfDayValue(8, 20, 19)));
    assertEquals(-1, new TimeOfDayValue(8, 10, 20).compareTo(new TimeOfDayValue(8, 19, 21)));
    assertEquals(-1, new TimeOfDayValue(8, 20, 15).compareTo(new TimeOfDayValue(8, 20, 19)));

    try {
      value.compareTo(null);
    } catch (BeeRuntimeException e) {
      assertTrue(true);
      System.out.println("compare error tiem of day" + e.getMessage());
    } catch (Exception e) {
      fail("Java runtime error. Need BeeRuntimeException !!!" + e.getMessage());
    }
  }

  @Test
  public final void testGets() {
    DateTime date = new DateTime(1298362388227L);
    TimeOfDayValue value = new TimeOfDayValue(date);
    assertEquals(10, value.getHours());
    assertEquals(13, value.getMinutes());
    assertEquals(8, value.getSeconds());
    assertEquals(227, value.getMilliseconds());

    DateTime date2 = new DateTime(2011, 1, 1, 10, 13, 8, 227);
    assertEquals(date2.toTimeString(), new TimeOfDayValue(date).getObjectValue());

    assertEquals(null, TimeOfDayValue.getNullValue().getObjectValue());

    assertEquals(ValueType.TIME_OF_DAY, value.getType());
  }

  @Test
  public final void testHashCode() {
    TimeOfDayValue value2 = new TimeOfDayValue(8, 20, 20, 227);
    assertEquals(-1, TimeOfDayValue.getNullValue().hashCode());
    assertEquals(1181916, value2.hashCode());
  }

  @Test
  public final void testToString() {
    TimeOfDayValue value2 = new TimeOfDayValue(8, 20, 20, 227);
    assertEquals("null", TimeOfDayValue.getNullValue().toString());
    assertEquals("08:20:20.227", value2.toString());
  }

}
