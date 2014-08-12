package com.butent.bee.server.sql;

import com.butent.bee.shared.data.value.Value;

/**
 * Transforms given constant expressions for compliance to specific SQL server requirements.
 */

class ConstantExpression implements IsExpression {

  private final Value constant;

  public ConstantExpression(Value value) {
    this.constant = value;
  }

  @Override
  public String getSqlString(SqlBuilder builder) {
    return builder.sqlTransform(constant);
  }

  @Override
  public Value getValue() {
    return constant;
  }
}
