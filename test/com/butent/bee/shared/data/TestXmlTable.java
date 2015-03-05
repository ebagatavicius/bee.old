package com.butent.bee.shared.data;

import com.butent.bee.server.Config;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.modules.administration.SysObject;
import com.butent.bee.shared.utils.BeeUtils;

import org.junit.Test;

public class TestXmlTable {
  @Test
  public void testRead() {
    String resource = Config.getPath("modules/Administration/" + SysObject.TABLE.getPath() + "/"
        + "Users." + SysObject.TABLE.getFileExtension(), true);
    String schemaSource = Config.getSchemaPath(SysObject.TABLE.getSchemaName());

    if (!BeeUtils.isEmpty(resource)) {
      XmlTable table = XmlUtils.unmarshal(XmlTable.class, resource, schemaSource);
      System.out.println(XmlUtils.marshal(table, schemaSource));
    }
  }
}
