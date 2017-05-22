package com.butent.bee.server.sql;

import com.butent.bee.shared.data.filter.CompoundType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generates complex condition expressions for SQL statements depending on specific SQL server
 * requirements.
 */

final class CompoundCondition implements HasConditions {

  static CompoundCondition and(IsCondition... conditions) {
    return new CompoundCondition(CompoundType.AND, conditions);
  }

  static CompoundCondition or(IsCondition... conditions) {
    return new CompoundCondition(CompoundType.OR, conditions);
  }

  private final CompoundType joinType;
  private final List<IsCondition> subConditions = new ArrayList<>();

  private CompoundCondition(CompoundType joinType, IsCondition... conditions) {
    this.joinType = joinType;
    add(conditions);
  }

  @Override
  public HasConditions add(IsCondition... conditions) {
    if (conditions != null) {
      for (IsCondition cond : conditions) {
        if (cond != null) {
          subConditions.add(cond);
        }
      }
    }
    return this;
  }

  @Override
  public void clear() {
    subConditions.clear();
  }

  @Override
  public HasConditions copyOf() {
    int c = subConditions.size();
    IsCondition[] subs = new IsCondition[c];

    for (int i = 0; i < c; i++) {
      subs[i] = subConditions.get(i).copyOf();
    }
    return new CompoundCondition(joinType, subs);
  }

  @Override
  public Collection<String> getSources() {
    Collection<String> sources = null;

    for (IsCondition condition : subConditions) {
      sources = SqlUtils.addCollection(sources, condition.getSources());
    }
    return sources;
  }

  @Override
  public String getSqlString(SqlBuilder builder) {
    StringBuilder sb = new StringBuilder();

    for (IsCondition cond : subConditions) {
      String expr = cond.getSqlString(builder);

      if (!BeeUtils.isEmpty(expr) && sb.length() > 0) {
        sb.append(joinType.toSqlString());
      }
      sb.append(expr);
    }
    String flt = sb.toString();

    if (CompoundType.OR == joinType && !BeeUtils.isEmpty(flt)) {
      flt = BeeUtils.parenthesize(flt);
    }
    return flt;
  }

  @Override
  public boolean isEmpty() {
    return subConditions.isEmpty();
  }

  @Override
  public IsCondition peek() {
    return isEmpty() ? null : subConditions.get(0);
  }

  @Override
  public int size() {
    return subConditions.size();
  }
}
