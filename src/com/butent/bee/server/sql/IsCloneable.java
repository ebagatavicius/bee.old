package com.butent.bee.server.sql;

public interface IsCloneable<T extends IsSql> {
  T copyOf();
}
