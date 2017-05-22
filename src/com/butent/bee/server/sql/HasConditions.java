package com.butent.bee.server.sql;

/**
 * Extends {@code IsCondition} interface, is the interface for complex conditions building classes.
 */

public interface HasConditions extends IsCondition {

  HasConditions add(IsCondition... conditions);

  void clear();

  boolean isEmpty();

  IsCondition peek();

  int size();
}
