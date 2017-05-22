package com.butent.bee;

import com.butent.bee.server.sql.TestHasFrom;
import com.butent.bee.server.sql.TestIsCondition;
import com.butent.bee.server.sql.TestIsExpression;
import com.butent.bee.server.sql.TestSqlCreate;
import com.butent.bee.server.sql.TestSqlDelete;
import com.butent.bee.server.sql.TestSqlInsert;
import com.butent.bee.server.sql.TestSqlSelect;
import com.butent.bee.server.sql.TestSqlUpdate;
import com.butent.bee.server.sql.TestSqlUtilsIsQuery;
import com.butent.bee.shared.TestBeeConst;
import com.butent.bee.shared.TestListSequence;
import com.butent.bee.shared.TestPair;
import com.butent.bee.shared.TestResource;
import com.butent.bee.shared.TestService;
import com.butent.bee.shared.TestStringArray;
import com.butent.bee.shared.data.TestDataUtils;
import com.butent.bee.shared.data.value.TestBooleanValue;
import com.butent.bee.shared.data.value.TestDateTimeValue;
import com.butent.bee.shared.data.value.TestDateValue;
import com.butent.bee.shared.data.value.TestIntValue;
import com.butent.bee.shared.data.value.TestLongValue;
import com.butent.bee.shared.data.value.TestNumberValue;
import com.butent.bee.shared.data.value.TestTextValue;
import com.butent.bee.shared.data.value.TestTimeOfDayValue;
import com.butent.bee.shared.data.value.TestValueType;
import com.butent.bee.shared.html.builder.TestBuilder;
import com.butent.bee.shared.i18n.TestDateOrdering;
import com.butent.bee.shared.time.TestDateTime;
import com.butent.bee.shared.time.TestJustDate;
import com.butent.bee.shared.utils.TestArrayUtils;
import com.butent.bee.shared.utils.TestBeeUtils;
import com.butent.bee.shared.utils.TestCodec;
import com.butent.bee.shared.utils.TestIntRangeSet;
import com.butent.bee.shared.utils.TestPropertyUtils;
import com.butent.bee.shared.utils.TestTimeUtils;
import com.butent.bee.shared.utils.TestWildcards;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Contains the Test Suite.
 */
@RunWith(value = org.junit.runners.Suite.class)
@SuiteClasses(value = {
    TestArrayUtils.class, TestPropertyUtils.class,
    TestTimeUtils.class,
    TestIsExpression.class, TestIsCondition.class,
    TestSqlCreate.class, TestSqlUtilsIsQuery.class, TestSqlInsert.class,
    TestSqlUpdate.class, TestSqlDelete.class, TestSqlSelect.class,
    TestHasFrom.class, TestWildcards.class,
    TestDateTime.class, TestJustDate.class, TestPair.class, TestBeeConst.class,
    TestResource.class, TestIntValue.class,
    TestLongValue.class, TestListSequence.class, TestValueType.class,
    TestService.class, TestStringArray.class,
    TestBooleanValue.class, TestDateTimeValue.class,
    TestDateValue.class, TestNumberValue.class, TestTextValue.class,
    TestTimeOfDayValue.class,
    TestDataUtils.class,
    TestBuilder.class,
    TestDateOrdering.class,
    TestBeeUtils.class, TestCodec.class, TestIntRangeSet.class})
public class AllTests {
}
