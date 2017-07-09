package com.butent.bee.shared.ui;

import com.google.common.collect.Sets;

import static com.butent.bee.shared.ui.UiConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ColumnRelation implements BeeSerializable, HasInfo {

  private static final Set<String> relevantAttributes =
      Sets.newHashSet(ATTR_EDIT_ENABLED, ATTR_EDIT_FORM, ATTR_EDIT_KEY, ATTR_EDIT_WINDOW,
          ATTR_EDIT_SOURCE, ATTR_EDIT_TARGET, ATTR_EDIT_VIEW_NAME);

  public static ColumnRelation maybeCreate(Map<String, String> input) {
    Map<String, String> map = new HashMap<>();

    if (!BeeUtils.isEmpty(input)) {
      for (Map.Entry<String, String> entry : input.entrySet()) {
        if (relevantAttributes.contains(entry.getKey()) && !BeeUtils.isEmpty(entry.getValue())) {
          map.put(entry.getKey(), entry.getValue());
        }
      }
    }

    if (map.isEmpty()) {
      return null;

    } else {
      ColumnRelation result = new ColumnRelation();
      result.attributes.putAll(map);
      return result;
    }
  }

  public static ColumnRelation restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    } else {
      return maybeCreate(Codec.deserializeLinkedHashMap(s));
    }
  }

  private final Map<String, String> attributes = new HashMap<>();

  private ColumnRelation() {
  }

  @Override
  public void deserialize(String s) {
    if (!attributes.isEmpty()) {
      attributes.clear();
    }

    attributes.putAll(Codec.deserializeLinkedHashMap(s));
  }

  public String getEditForm() {
    return getAttribute(ATTR_EDIT_FORM);
  }

  public Integer getEditKey() {
    return BeeUtils.toIntOrNull(getAttribute(ATTR_EDIT_KEY));
  }

  public String getEditTarget() {
    return getAttribute(ATTR_EDIT_TARGET);
  }

  @Override
  public List<Property> getInfo() {
    return PropertyUtils.createProperties(attributes);
  }

  public boolean isEditEnabled() {
    return !attributes.isEmpty() && !BeeConst.isFalse(getAttribute(ATTR_EDIT_ENABLED));
  }

  public WindowType getEditWindowType() {
    return WindowType.parse(getAttribute(ATTR_EDIT_WINDOW));
  }

  public String getSourceColumn(DataInfo dataInfo, String defSource) {
    String source = getEditSource();

    if (BeeUtils.isEmpty(source) && dataInfo != null && !BeeUtils.isEmpty(defSource)) {
      return dataInfo.getEditableRelationSource(defSource);
    } else {
      return source;
    }
  }

  public String getViewName(DataInfo sourceInfo, String defSource) {
    String viewName = getEditViewName();

    if (BeeUtils.isEmpty(viewName) && sourceInfo != null) {
      String source = getSourceColumn(sourceInfo, defSource);
      return BeeUtils.isEmpty(source) ? null : sourceInfo.getEditableRelationView(source);

    } else {
      return viewName;
    }
  }

  public void replaceSource(String oldId, String newId) {
    if (!BeeUtils.anyEmpty(oldId, newId) && Objects.equals(getEditSource(), oldId)) {
      attributes.put(ATTR_EDIT_SOURCE, newId);
    }
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(attributes);
  }

  private String getAttribute(String name) {
    return attributes.get(name);
  }

  private String getEditSource() {
    return getAttribute(ATTR_EDIT_SOURCE);
  }

  private String getEditViewName() {
    return getAttribute(ATTR_EDIT_VIEW_NAME);
  }
}
