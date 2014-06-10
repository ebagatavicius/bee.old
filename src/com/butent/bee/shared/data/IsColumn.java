package com.butent.bee.shared.data;

import com.butent.bee.shared.HasPrecision;
import com.butent.bee.shared.HasScale;
import com.butent.bee.shared.data.value.ValueType;

/**
 * Contains necessary methods for column classes, for example {@code getLabel} or {@code setType}.
 */

public interface IsColumn extends HasCustomProperties, HasPrecision, HasScale {

  IsColumn copy();

  String getEnumKey();
  
  String getId();

  String getLabel();

  String getPattern();

  ValueType getType();

  boolean isCharacter();

  boolean isText();
  
  void setEnumKey(String enumKey);
  
  void setId(String id);

  void setLabel(String label);

  void setPattern(String pattern);
 
  void setType(ValueType type);
}