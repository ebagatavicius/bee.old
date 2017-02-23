package com.butent.bee;

import com.butent.bee.shared.data.TestDataUtils;
import com.butent.bee.shared.html.builder.TestBuilder;
import com.butent.bee.shared.i18n.TestDateOrdering;
import com.butent.bee.shared.testutils.TestArrayUtils;
import com.butent.bee.shared.testutils.TestBeeConst;
import com.butent.bee.shared.testutils.TestBeeUtilsIntersects;
import com.butent.bee.shared.testutils.TestBeeUtilsTransform;
import com.butent.bee.shared.testutils.TestBooleanValue;
import com.butent.bee.shared.testutils.TestDateTime;
import com.butent.bee.shared.testutils.TestDateTimeValue;
import com.butent.bee.shared.testutils.TestDateValue;
import com.butent.bee.shared.testutils.TestHasFrom;
import com.butent.bee.shared.testutils.TestIntValue;
import com.butent.bee.shared.testutils.TestIsCondition;
import com.butent.bee.shared.testutils.TestIsExpression;
import com.butent.bee.shared.testutils.TestJustDate;
import com.butent.bee.shared.testutils.TestListSequence;
import com.butent.bee.shared.testutils.TestLongValue;
import com.butent.bee.shared.testutils.TestNumberValue;
import com.butent.bee.shared.testutils.TestPair;
import com.butent.bee.shared.testutils.TestPropertyUtils;
import com.butent.bee.shared.testutils.TestResource;
import com.butent.bee.shared.testutils.TestService;
import com.butent.bee.shared.testutils.TestSqlCreate;
import com.butent.bee.shared.testutils.TestSqlDelete;
import com.butent.bee.shared.testutils.TestSqlInsert;
import com.butent.bee.shared.testutils.TestSqlSelect;
import com.butent.bee.shared.testutils.TestSqlUpdate;
import com.butent.bee.shared.testutils.TestSqlUtilsIsQuery;
import com.butent.bee.shared.testutils.TestStringArray;
import com.butent.bee.shared.testutils.TestTextValue;
import com.butent.bee.shared.testutils.TestTimeOfDayValue;
import com.butent.bee.shared.testutils.TestTimeUtils;
import com.butent.bee.shared.testutils.TestValueType;
import com.butent.bee.shared.testutils.TestWildcards;
import com.butent.bee.shared.testutils.TestWildcardsPattern;
import com.butent.bee.shared.utils.TestBeeUtils;
import com.butent.bee.shared.utils.TestCodec;
import com.butent.bee.shared.utils.TestIntRangeSet;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Contains the Test Suite.
 */
@RunWith(value = org.junit.runners.Suite.class)
@SuiteClasses(value = {TestBeeUtilsIntersects.class,
    TestBeeUtilsTransform.class, TestArrayUtils.class, TestPropertyUtils.class,
    TestTimeUtils.class,
    TestIsExpression.class, TestIsCondition.class,
    TestSqlCreate.class, TestSqlUtilsIsQuery.class, TestSqlInsert.class,
    TestSqlUpdate.class, TestSqlDelete.class, TestSqlSelect.class,
    TestHasFrom.class, TestWildcardsPattern.class, TestWildcards.class,
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
