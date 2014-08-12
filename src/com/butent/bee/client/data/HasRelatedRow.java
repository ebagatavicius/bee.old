package com.butent.bee.client.data;

import com.butent.bee.shared.data.BeeRow;

public interface HasRelatedRow {

  Long getRelatedId();

  BeeRow getRelatedRow();
}
