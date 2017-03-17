package com.butent.bee.server.sql;

import com.butent.bee.server.sql.FromJoin.JoinMode;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An abstract class, contains an engine for generation of FROM parts for SQL statements.
 */

public abstract class HasFrom<T> extends SqlQuery<T> {

  private List<IsFrom> fromList;

  public T addFrom(String source) {
    addFrom(source, null);
    return getReference();
  }

  public T addFrom(String source, String alias) {
    if (BeeUtils.isEmpty(fromList)) {
      addFrom(FromJoin.fromSingle(source, alias));
    } else {
      addFromJoin(FromJoin.fromList(source, alias));
    }
    return getReference();
  }

  public T addFrom(SqlSelect source, String alias) {
    if (BeeUtils.isEmpty(fromList)) {
      addFrom(FromJoin.fromSingle(source, alias));
    } else {
      addFromJoin(FromJoin.fromList(source, alias));
    }
    return getReference();
  }

  public T addFromFull(String source, IsCondition on) {
    addFromFull(source, null, on);
    return getReference();
  }

  public T addFromFull(String source, String alias, IsCondition on) {
    addFromJoin(FromJoin.fromFull(source, alias, on));
    return getReference();
  }

  public T addFromFull(SqlSelect source, String alias, IsCondition on) {
    addFromJoin(FromJoin.fromFull(source, alias, on));
    return getReference();
  }

  public T addFromInner(String source, IsCondition on) {
    addFromInner(source, null, on);
    return getReference();
  }

  public T addFromInner(String source, String alias, IsCondition on) {
    addFromJoin(FromJoin.fromInner(source, alias, on));
    return getReference();
  }

  public T addFromInner(SqlSelect source, String alias, IsCondition on) {
    addFromJoin(FromJoin.fromInner(source, alias, on));
    return getReference();
  }

  public T addFromLeft(String source, IsCondition on) {
    addFromLeft(source, null, on);
    return getReference();
  }

  public T addFromLeft(String source, String alias, IsCondition on) {
    addFromJoin(FromJoin.fromLeft(source, alias, on));
    return getReference();
  }

  public T addFromLeft(SqlSelect source, String alias, IsCondition on) {
    addFromJoin(FromJoin.fromLeft(source, alias, on));
    return getReference();
  }

  public T addFromRight(String source, IsCondition on) {
    addFromRight(source, null, on);
    return getReference();
  }

  public T addFromRight(String source, String alias, IsCondition on) {
    addFromJoin(FromJoin.fromRight(source, alias, on));
    return getReference();
  }

  public T addFromRight(SqlSelect source, String alias, IsCondition on) {
    addFromJoin(FromJoin.fromRight(source, alias, on));
    return getReference();
  }

  public List<IsFrom> getFrom() {
    return fromList;
  }

  @Override
  public Collection<String> getSources() {
    Collection<String> sources = null;

    if (!BeeUtils.isEmpty(fromList)) {
      for (IsFrom from : fromList) {
        sources = SqlUtils.addCollection(sources, from.getSources());
      }
    }
    return sources;
  }

  @Override
  public boolean isEmpty() {
    return BeeUtils.isEmpty(fromList);
  }

  public void setFrom(List<IsFrom> from) {
    this.fromList = from;
  }

  private void addFrom(IsFrom from) {
    if (BeeUtils.isEmpty(fromList)) {
      fromList = new ArrayList<>();
    }
    fromList.add(from);
  }

  private void addFromJoin(FromJoin from) {
    Assert.notEmpty(fromList, "First FROM source cannot be of type JOIN");
    boolean listMode = JoinMode.LIST == from.getJoinMode();

    for (IsFrom fr : fromList) {
      if (fr instanceof FromJoin) {
        boolean isList = JoinMode.LIST == ((FromJoin) fr).getJoinMode();

        Assert.state((listMode && isList) || !(listMode || isList),
            "Mix of incompatible join types");
      }
    }
    addFrom(from);
  }
}
