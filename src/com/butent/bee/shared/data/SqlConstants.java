package com.butent.bee.shared.data;

import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains constants lists, such as keywords, data types and empty values for each of them.
 */

public final class SqlConstants {

  /**
   * Contains a list of supported data types.
   */

  public enum SqlDataType {
    BOOLEAN, INTEGER, LONG, DOUBLE, DECIMAL, CHAR, STRING, TEXT, BLOB, DATE, DATETIME;

    public Object getEmptyValue() {
      Object value = null;

      switch (this) {
        case BOOLEAN:
        case INTEGER:
        case LONG:
        case DOUBLE:
        case DECIMAL:
        case DATE:
        case DATETIME:
          value = 0;
          break;
        case CHAR:
        case STRING:
        case TEXT:
        case BLOB:
          value = "";
          break;
      }
      return value;
    }

    public Object parse(String s) {
      if (s == null || s.isEmpty()) {
        return null;
      }

      switch (this) {
        case BOOLEAN:
          return BeeUtils.toBooleanOrNull(s);
        case INTEGER:
          return BeeUtils.toIntOrNull(s);
        case LONG:
          return BeeUtils.toLongOrNull(s);
        case DOUBLE:
          return BeeUtils.toDoubleOrNull(s);
        case DECIMAL:
          return BeeUtils.toDecimalOrNull(s);
        case DATE:
          return BeeUtils.toIntOrNull(s);
        case DATETIME:
          return BeeUtils.toLongOrNull(s);
        case CHAR:
        case STRING:
        case TEXT:
        case BLOB:
          return s;
      }
      return null;
    }

    public ValueType toValueType() {
      switch (this) {
        case BOOLEAN:
          return ValueType.BOOLEAN;
        case INTEGER:
          return ValueType.INTEGER;
        case LONG:
          return ValueType.LONG;
        case DOUBLE:
          return ValueType.NUMBER;
        case DECIMAL:
          return ValueType.DECIMAL;
        case DATE:
          return ValueType.DATE;
        case DATETIME:
          return ValueType.DATE_TIME;
        case CHAR:
        case STRING:
        case TEXT:
        case BLOB:
          return ValueType.TEXT;
      }
      return null;
    }
  }

  /**
   * Contains a list of SQL functions used by the system.
   */

  public enum SqlFunction {
    BITAND, BITOR, IF, CASE, CAST, NVL, CONCAT,
    PLUS, MINUS, MULTIPLY, DIVIDE, BULK,
    MIN, MAX, SUM, AVG, COUNT, SUM_DISTINCT, AVG_DISTINCT, COUNT_DISTINCT,
    LENGTH, SUBSTRING, LEFT, RIGHT
  }

  /**
   * Contains a list of SQL keywords used by the system.
   */

  public enum SqlKeyword {
    DB_NAME, DB_SCHEMA,
    DB_SCHEMAS, DB_TABLES, DB_FIELDS, DB_CONSTRAINTS, DB_FOREIGNKEYS, DB_INDEXES, DB_TRIGGERS,
    RENAME_TABLE, TRUNCATE_TABLE, DROP_TABLE, DROP_FOREIGNKEY, SET_PARAMETER,
    CREATE_SCHEMA, CREATE_INDEX, CREATE_TRIGGER,
    ADD_CONSTRAINT, PRIMARY_KEY, FOREIGN_KEY, UNIQUE, CHECK, DELETE, SET_NULL, LIKE,
    TEMPORARY, TEMPORARY_NAME
  }

  public enum SqlTriggerType {
    CUSTOM, AUDIT, RELATION
  }

  public enum SqlTriggerTiming {
    BEFORE, AFTER
  }

  public enum SqlTriggerEvent {
    INSERT, UPDATE, DELETE
  }

  public enum SqlTriggerScope {
    ROW, STATEMENT
  }

  public static final String TBL_NAME = "tblName";
  public static final String ROW_COUNT = "rowCount";

  public static final String FLD_NAME = "fldName";
  public static final String FLD_TYPE = "fldType";
  public static final String FLD_NULL = "fldNullable";
  public static final String FLD_LENGTH = "fldLength";
  public static final String FLD_PRECISION = "fldPrecision";
  public static final String FLD_SCALE = "fldScale";

  public static final String KEY_NAME = "keyName";
  public static final String KEY_TYPE = "keyType";

  public static final String FK_REF_TABLE = "fkRefTable";

  public static final String TRIGGER_NAME = "triggerName";

  private SqlConstants() {
  }
}
