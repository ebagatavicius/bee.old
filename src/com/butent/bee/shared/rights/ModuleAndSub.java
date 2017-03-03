package com.butent.bee.shared.rights;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;

public final class ModuleAndSub implements Comparable<ModuleAndSub>, BeeSerializable {

  public static ModuleAndSub of(Module module) {
    return of(module, null);
  }

  public static ModuleAndSub of(Module module, SubModule subModule) {
    Assert.notNull(module);
    return new ModuleAndSub(module, subModule);
  }

  public static ModuleAndSub parse(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    Module module = null;
    SubModule subModule = null;

    List<String> parts = Lists.newArrayList(RightsUtils.NAME_SPLITTER.split(input));

    if (parts.size() > 0) {
      String moduleName = parts.get(0);
      for (Module m : Module.values()) {
        if (BeeUtils.same(m.getName(), moduleName)) {
          module = m;
          break;
        }
      }

      if (parts.size() > 1) {
        subModule = SubModule.parse(parts.get(1));
      }
    }

    if (module == null) {
      return null;
    } else {
      return of(module, subModule);
    }
  }

  public static List<ModuleAndSub> parseList(String input) {
    List<ModuleAndSub> result = new ArrayList<>();

    List<String> list = NameUtils.toList(input);
    for (String s : list) {
      ModuleAndSub ms = parse(s);
      if (ms != null) {
        result.add(ms);
      }
    }

    return result;
  }

  private final Module module;

  private final SubModule subModule;

  private ModuleAndSub(Module module, SubModule subModule) {
    this.module = module;
    this.subModule = subModule;
  }

  @Override
  public int compareTo(ModuleAndSub other) {
    int result = BeeUtils.compareNullsFirst(module, other.module);

    if (result == BeeConst.COMPARE_EQUAL) {
      if (subModule == null || other.subModule == null || subModule == other.subModule) {
        result = BeeUtils.compareNullsFirst(subModule, other.subModule);
      } else {
        result = Integer.compare(module.getSubModules().indexOf(subModule),
            module.getSubModules().indexOf(other.subModule));
      }
    }

    return result;
  }

  @Override
  public void deserialize(String s) {
    Assert.unsupported();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ModuleAndSub) {
      return module == ((ModuleAndSub) obj).module && subModule == ((ModuleAndSub) obj).subModule;
    } else {
      return false;
    }
  }

  public Module getModule() {
    return module;
  }

  public String getName() {
    if (subModule == null) {
      return module.getName();
    } else {
      return RightsUtils.NAME_JOINER.join(module.getName(), subModule.getName());
    }
  }

  public SubModule getSubModule() {
    return subModule;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((module == null) ? 0 : module.hashCode());
    result = prime * result + ((subModule == null) ? 0 : subModule.hashCode());
    return result;
  }

  public boolean hasSubModule() {
    return subModule != null;
  }

  public boolean isEnabled() {
    return Module.ENABLED_MODULES.contains(this);
  }

  @Override
  public String serialize() {
    return getName();
  }

  @Override
  public String toString() {
    return getName();
  }
}
