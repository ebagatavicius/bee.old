package com.butent.bee.server.sql;

/**
 * Extends {@code IsSql, HasSource} interfaces, ensures that all implementing classes of this
 * interface would be able to return their source and alias information.
 */

public interface IsFrom extends IsSql, HasSource, IsCloneable<IsFrom> {

  String getAlias();

  Object getSource();
}
