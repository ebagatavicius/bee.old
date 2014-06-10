package com.butent.bee.server.data;

import com.butent.bee.server.sql.HasFrom;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.shared.rights.RightsState;

import java.util.Collection;

/**
 * Contains required methods for classes that can have different states when they are used.
 */

public interface HasStates {

  boolean activateState(RightsState state, long bit);

  IsCondition checkState(String stateAlias, RightsState state, long... bits);

  SqlCreate createStateTable(SqlCreate query, RightsState state);

  String getStateTable(RightsState state);

  void initState(RightsState state, Collection<String> flds);

  SqlInsert insertState(long id, RightsState state, long bit, boolean on);

  String joinState(HasFrom<?> query, String tblAlias, RightsState state);

  SqlUpdate updateState(long id, RightsState state, long bit, boolean on);

  SqlUpdate updateStateDefaults(long id, RightsState state, boolean on, long... bits);

  SqlSelect verifyState(SqlSelect query, String tblAlias, RightsState state, long... bits);
}
