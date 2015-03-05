package com.butent.bee.shared.data;

import com.butent.bee.server.Config;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.modules.administration.SysObject;
import com.butent.bee.shared.utils.BeeUtils;

import org.junit.Test;

public class TestXmlView {
  @Test
  public void testRead() {
    String resource = Config.getPath("modules/Mail/" + SysObject.VIEW.getPath() + "/"
        + "Messages." + SysObject.VIEW.getFileExtension(), true);
    String schemaSource = Config.getSchemaPath(SysObject.VIEW.getSchemaName());

    if (!BeeUtils.isEmpty(resource)) {
      XmlView view = XmlUtils.unmarshal(XmlView.class, resource, schemaSource);
      System.out.println(XmlUtils.marshal(view, schemaSource));
    }
  }
}
