package com.butent.bee.client.style;

import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XSheet;

public interface StyleProvider {

  Integer getExportStyleRef(IsRow row, XSheet sheet);

  StyleDescriptor getStyleDescriptor(IsRow row);
}
