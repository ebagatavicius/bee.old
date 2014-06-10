package com.butent.bee.shared.data;

import com.butent.bee.shared.data.value.Value;

public interface HasRowValue {
  
  boolean dependsOnSource(String source);
  
  Value getRowValue(IsRow row);
}
