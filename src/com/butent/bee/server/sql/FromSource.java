package com.butent.bee.server.sql;

import com.google.common.collect.Sets;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

/**
 * Generates FROM part of a SQL statement depending on requirements of a specific SQL server,
 * supports alias names.
 */

class FromSource implements IsFrom {

  private final Object source;
  private final String alias;

  protected FromSource(String source, String alias) {
    Assert.notEmpty(source);

    this.source = source;
    this.alias = alias;
  }

  protected FromSource(SqlSelect source, String alias) {
    Assert.notNull(source);
    Assert.state(!source.isEmpty());
    Assert.notEmpty(alias);

    this.source = source;
    this.alias = alias;
  }

  @Override
  public FromSource copyOf() {
    if (source instanceof SqlSelect) {
      return new FromSource(((SqlSelect) source).copyOf(true), alias);
    }
    return new FromSource((String) source, alias);
  }

  @Override
  public String getAlias() {
    return alias;
  }

  @Override
  public Object getSource() {
    return source;
  }

  @Override
  public Collection<String> getSources() {
    Collection<String> sources = null;

    if (source instanceof HasSource) {
      sources = ((HasSource) source).getSources();
    } else if (source instanceof String) {
      sources = Sets.newHashSet((String) source);
    } else {
      Assert.untouchable();
    }
    return sources;
  }

  @Override
  public String getSqlString(SqlBuilder builder) {
    StringBuilder from = new StringBuilder();

    if (source instanceof SqlSelect) {
      from.append("(").append(((SqlSelect) source).getSqlString(builder)).append(")");
    } else if (source instanceof String) {
      from.append(SqlUtils.name((String) source).getSqlString(builder));
    } else {
      Assert.untouchable();
    }

    if (!BeeUtils.isEmpty(alias)) {
      from.append(" ").append(SqlUtils.name(alias).getSqlString(builder));
    }
    return from.toString();
  }
}
