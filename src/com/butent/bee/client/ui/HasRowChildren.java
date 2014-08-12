package com.butent.bee.client.ui;

import com.butent.bee.shared.data.RowChildren;

import java.util.Collection;

public interface HasRowChildren {

  Collection<RowChildren> getChildrenForInsert();

  Collection<RowChildren> getChildrenForUpdate();
}
