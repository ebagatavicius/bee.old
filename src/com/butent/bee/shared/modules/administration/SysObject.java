package com.butent.bee.shared.modules.administration;

import com.butent.bee.shared.data.BeeObject;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Map;

public enum SysObject {
  TABLE {
    @Override
    public String getPath() {
      return "tables";
    }
  },
  VIEW {
    @Override
    public String getPath() {
      return "views";
    }
  },
  MENU {
    @Override
    public String getPath() {
      return "menu";
    }
  },
  GRID {
    @Override
    public String getPath() {
      return "grids";
    }
  },
  FORM {
    @Override
    public String getPath() {
      return "forms";
    }
  };

  public String getFileExtension() {
    return BeeUtils.join(".", getName(), "xml");
  }

  public String getName() {
    return name().toLowerCase();
  }

  public abstract String getPath();

  public String getSchemaName() {
    return BeeUtils.join(".", getName(), "xsd");
  }

  public static <T extends BeeObject> boolean register(T object, Map<String, T> cache,
      boolean initial, BeeLogger logger) {

    if (object != null) {
      String moduleName = object.getModule();
      String className = NameUtils.getClassName(object.getClass());
      String objectName = object.getName();
      T existingObject = cache.get(BeeUtils.normalize(objectName));
      boolean isNew = existingObject == null;

      if (!isNew && (initial || !BeeUtils.same(moduleName, existingObject.getModule()))) {
        logger.warning(BeeUtils.parenthesize(moduleName), "Duplicate", className, "name:",
            BeeUtils.bracket(objectName), BeeUtils.parenthesize(existingObject.getModule()));
      } else {
        cache.put(BeeUtils.normalize(objectName), object);

        if (!initial) {
          logger.info(BeeUtils.parenthesize(moduleName), isNew ? "Registered" : "Replaced",
              "custom", className, "object:", BeeUtils.bracket(objectName));
        }
        return isNew;
      }
    }
    return false;
  }
}