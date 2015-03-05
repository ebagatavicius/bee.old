package com.butent.bee.server.sql;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Enables to form any SQL statement with specified parameters, mainly used for rarer SQL commands
 * like dropping indexes.
 */

class SqlCommand extends SqlQuery<SqlCommand> {

  private final SqlKeyword command;
  private final Map<String, Object> parameters;

  public SqlCommand(SqlKeyword command, Map<String, Object> parameters) {
    Assert.notNull(command);

    this.command = command;
    this.parameters = parameters;
  }

  public SqlKeyword getCommand() {
    return command;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  @Override
  public Collection<String> getSources() {
    Collection<String> sources = null;

    if (!BeeUtils.isEmpty(parameters)) {
      for (Object prm : parameters.values()) {
        if (prm instanceof HasSource) {
          sources = SqlUtils.addCollection(sources, ((HasSource) prm).getSources());
        }
      }
    }
    return sources;
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
    return builder.sqlKeyword(command, params);
  }

  @Override
  public boolean isEmpty() {
    return command == null;
  }

  @Override
  public SqlCommand reset() {
    return getReference();
  }
}
