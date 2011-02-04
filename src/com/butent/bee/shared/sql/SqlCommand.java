package com.butent.bee.shared.sql;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.sql.BeeConstants.Keywords;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class SqlCommand extends SqlQuery<SqlCommand> {

  private final Keywords command;
  private final Map<String, Object> parameters;

  public SqlCommand(Keywords command, Map<String, Object> parameters) {
    Assert.notEmpty(command);

    this.command = command;
    this.parameters = parameters;
  }

  public Keywords getCommand() {
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
        Collection<String> value = null;

        if (prm instanceof IsFrom) {
          value = ((IsFrom) prm).getSources();
        } else if (prm instanceof IsQuery) {
          value = ((IsQuery) prm).getSources();
        } else {
          continue;
        }
        if (BeeUtils.isEmpty(sources)) {
          sources = value;
        } else {
          sources.addAll(value);
        }
      }
    }
    return sources;
  }

  @Override
  public List<Object> getSqlParams() {
    return null;
  }

  @Override
  public String getSqlString(SqlBuilder builder, boolean paramMode) {
    Assert.notEmpty(builder);
    return builder.getCommand(this, paramMode);
  }

  @Override
  public boolean isEmpty() {
    return BeeUtils.isEmpty(command);
  }

  @Override
  protected SqlCommand getReference() {
    return this;
  }
}
