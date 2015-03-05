package com.butent.bee.shared.data.filter;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterDescription implements BeeSerializable, HasInfo {

  public static final String TAG_PREDEFINED_FILTER = "predefinedFilter";

  public static final String TAG_COLUMN = "column";

  private static final String ATTR_INITIAL = "initial";
  private static final String ATTR_EDITABLE = "editable";
  private static final String ATTR_REMOVABLE = "removable";

  public static FilterDescription create(String key, Map<String, String> attributes,
      Collection<FilterComponent> components) {

    if (BeeUtils.isEmpty(key) || BeeUtils.isEmpty(attributes) || BeeUtils.isEmpty(components)) {
      return null;
    }

    String name = attributes.get(UiConstants.ATTR_NAME);
    if (BeeUtils.isEmpty(name)) {
      return null;
    }

    String label = attributes.get(UiConstants.ATTR_LABEL);
    if (BeeUtils.isEmpty(label)) {
      return null;
    }

    FilterDescription filterDescription = new FilterDescription(name, label, components);

    if (attributes.containsKey(ATTR_INITIAL)) {
      filterDescription.setInitial(BeeUtils.toBooleanOrNull(attributes.get(ATTR_INITIAL)));
    }

    if (attributes.containsKey(ATTR_EDITABLE)) {
      filterDescription.setEditable(BeeUtils.toBooleanOrNull(attributes.get(ATTR_EDITABLE)));
    }
    if (attributes.containsKey(ATTR_REMOVABLE)) {
      filterDescription.setRemovable(BeeUtils.toBooleanOrNull(attributes.get(ATTR_REMOVABLE)));
    }

    return filterDescription;
  }

  public static String createValue(Map<String, String> values) {
    Assert.notEmpty(values);
    return Codec.beeSerialize(values);
  }

  public static String createValue(String key, String value) {
    Assert.notEmpty(key);
    Assert.notEmpty(value);

    Map<String, String> values = new HashMap<>();
    values.put(key, value);

    return createValue(values);
  }

  public static FilterDescription restore(String s) {
    Assert.notEmpty(s);

    FilterDescription filterDescription = new FilterDescription();
    filterDescription.deserialize(s);

    return filterDescription;
  }

  public static List<FilterComponent> restoreComponents(String serialized) {
    Assert.notEmpty(serialized);
    List<FilterComponent> result = new ArrayList<>();

    String[] arr = Codec.beeDeserializeCollection(serialized);
    if (ArrayUtils.isEmpty(arr)) {
      return result;
    }

    for (String s : arr) {
      if (!BeeUtils.isEmpty(s)) {
        result.add(FilterComponent.restore(s));
      }
    }

    return result;
  }

  public static List<FilterDescription> restoreList(String serialized) {
    Assert.notEmpty(serialized);
    List<FilterDescription> result = new ArrayList<>();

    String[] arr = Codec.beeDeserializeCollection(serialized);
    if (ArrayUtils.isEmpty(arr)) {
      return result;
    }

    for (String s : arr) {
      if (!BeeUtils.isEmpty(s)) {
        result.add(restore(s));
      }
    }

    return result;
  }

  public static FilterDescription userDefined(String label,
      Collection<FilterComponent> components) {

    Assert.notEmpty(label);
    Assert.notEmpty(components);

    String name = BeeUtils.join(BeeConst.STRING_UNDER, BeeUtils.randomString(6),
        System.currentTimeMillis());

    FilterDescription filterDescription = new FilterDescription(name, label, components);

    filterDescription.setEditable(true);
    filterDescription.setRemovable(true);

    return filterDescription;
  }

  private String name;

  private String label;
  private final List<FilterComponent> components = new ArrayList<>();

  private Boolean initial;

  private Boolean editable;

  private Boolean removable;

  public FilterDescription(String name, String label, Collection<FilterComponent> components,
      Boolean initial, Boolean editable, Boolean removable) {

    this.name = name;
    this.label = label;

    if (components != null) {
      this.components.addAll(components);
    }

    this.initial = initial;
    this.editable = editable;
    this.removable = removable;
  }

  protected FilterDescription() {
    super();
  }

  private FilterDescription(String name, String label, Collection<FilterComponent> components) {
    this(name, label, components, null, null, null);
  }

  public boolean containsAnyComponent(Collection<String> names) {
    if (!BeeUtils.isEmpty(names)) {
      for (FilterComponent component : getComponents()) {
        if (names.contains(component.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  public FilterDescription copy() {
    FilterDescription result = new FilterDescription();

    result.setName(getName());
    result.setLabel(getLabel());

    for (FilterComponent component : getComponents()) {
      result.getComponents().add(component.copy());
    }

    result.setInitial(getInitial());

    result.setEditable(getEditable());
    result.setRemovable(getRemovable());

    return result;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 6);

    int i = 0;
    setName(arr[i++]);
    setLabel(arr[i++]);

    setComponents(restoreComponents(arr[i++]));

    setInitial(BeeUtils.toBooleanOrNull(arr[i++]));

    setEditable(BeeUtils.toBooleanOrNull(arr[i++]));
    setRemovable(BeeUtils.toBooleanOrNull(arr[i++]));
  }

  public List<FilterComponent> getComponents() {
    return components;
  }

  public Boolean getEditable() {
    return editable;
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties("Name", getName(),
        "Label", getLabel(),
        "Initial", getInitial(),
        "Editable", getEditable(),
        "Removable", getRemovable());

    info.add(new Property("Components", BeeUtils.bracket(getComponents().size())));
    for (FilterComponent component : getComponents()) {
      PropertyUtils.appendChildrenToProperties(info, "Component", component.getInfo());
    }

    return info;
  }

  public Boolean getInitial() {
    return initial;
  }

  public String getLabel() {
    return label;
  }

  public String getName() {
    return name;
  }

  public Boolean getRemovable() {
    return removable;
  }

  public boolean isEditable() {
    return BeeUtils.isTrue(getEditable());
  }

  public boolean isInitial() {
    return BeeUtils.isTrue(getInitial());
  }

  public boolean isRemovable() {
    return BeeUtils.isTrue(getRemovable());
  }

  public boolean sameComponents(Collection<FilterComponent> otherComponents) {
    return BeeUtils.sameElements(getComponents(), otherComponents);
  }

  @Override
  public String serialize() {
    return Codec.beeSerialize(new Object[] {getName(), getLabel(), getComponents(),
        getInitial(), getEditable(), getRemovable()});
  }

  public String serializeComponents() {
    return Codec.beeSerialize(getComponents());
  }

  public void setComponents(List<FilterComponent> components) {
    BeeUtils.overwrite(this.components, components);
  }

  public void setEditable(Boolean editable) {
    this.editable = editable;
  }

  public void setInitial(Boolean initial) {
    this.initial = initial;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setRemovable(Boolean removable) {
    this.removable = removable;
  }

  private void setName(String name) {
    this.name = name;
  }
}
