package com.butent.bee.server.sql;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates complex expressions for SQL statements depending on specific SQL server requirements.
 */

class FunctionExpression implements IsExpression {

  private final SqlFunction function;
  private final Map<String, Object> parameters;

  public FunctionExpression(SqlFunction function, Map<String, Object> parameters) {
    Assert.notNull(function);

    this.function = function;
    this.parameters = parameters;
  }

  public SqlFunction getFunction() {
    return function;
  }

  @Override
  public String getSqlString(SqlBuilder builder) {
    Assert.notNull(builder);
    Map<String, Object> params = new HashMap<>();

    if (!BeeUtils.isEmpty(parameters)) {
      for (String prm : parameters.keySet()) {
        Object value = parameters.get(prm);

        if (value instanceof IsSql) {
          value = ((IsSql) value).getSqlString(builder);
        }
        params.put(prm, value);
      }
    }
    return builder.sqlFunction(function, params);
  }

  @Override
  public Map<String, Object> getValue() {
    return parameters;
  }

}
