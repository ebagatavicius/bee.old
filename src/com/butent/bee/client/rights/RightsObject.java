package com.butent.bee.client.rights;

import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RightsUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public class RightsObject implements HasCaption {

  private final String name;
  private final String caption;

  private final ModuleAndSub moduleAndSub;

  private final int level;
  private final String parent;

  private boolean hasChildren;

  RightsObject(String name, String caption, Module module) {
    this(name, caption, ModuleAndSub.of(module));
  }

  RightsObject(String name, String caption, ModuleAndSub moduleAndSub) {
    this(name, caption, moduleAndSub, 0, null);
  }

  RightsObject(String name, String caption, String parent) {
    this(name, caption, null, 0, parent);
  }

  RightsObject(String name, String caption, ModuleAndSub moduleAndSub, int level, String parent) {
    this.name = RightsUtils.buildName(parent, name);
    this.caption = caption;

    this.moduleAndSub = moduleAndSub;

    this.level = level;
    this.parent = BeeUtils.isEmpty(parent) ? null : RightsUtils.normalizeName(parent);
  }

  @Override
  public String getCaption() {
    return caption;
  }

  public ModuleAndSub getModuleAndSub() {
    return moduleAndSub;
  }

  public String getName() {
    return name;
  }

  int getLevel() {
    return level;
  }

  String getParent() {
    return parent;
  }

  boolean hasChildren() {
    return hasChildren;
  }

  boolean hasParent() {
    return parent != null;
  }

  void setHasChildren(boolean hasChildren) {
    this.hasChildren = hasChildren;
  }
}
