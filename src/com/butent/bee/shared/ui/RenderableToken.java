package com.butent.bee.shared.ui;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RenderableToken implements BeeSerializable, HasInfo {

  private enum Serial {
    SOURCE, PREFIX, SUFFIX, ADD_PREFIX_WHEN_EMPTY, ADD_SUFFIX_WHEN_EMPTY, FORMAT, SCALE
  }

  public static final String TAG_RENDER_TOKEN = "renderToken";

  private static final String ATTR_PREFIX = "prefix";
  private static final String ATTR_SUFFIX = "suffix";
  private static final String ATTR_ADD_PREFIX_WHEN_EMPTY = "addPrefixWhenEmpty";
  private static final String ATTR_ADD_SUFFIX_WHEN_EMPTY = "addSuffixWhenEmpty";

  public static RenderableToken create(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return null;
    }

    RenderableToken renderableToken = new RenderableToken();
    renderableToken.setAttributes(attributes);

    return renderableToken;
  }

  public static RenderableToken restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    RenderableToken renderableToken = new RenderableToken();
    renderableToken.deserialize(s);

    return renderableToken;
  }

  public static List<RenderableToken> restoreList(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    String[] tokens = Codec.beeDeserializeCollection(s);
    if (ArrayUtils.isEmpty(tokens)) {
      return null;
    }

    List<RenderableToken> result = new ArrayList<>();
    for (String token : tokens) {
      RenderableToken renderableToken = restore(token);
      if (renderableToken != null) {
        result.add(renderableToken);
      }
    }
    return result;
  }

  private String source;

  private String prefix;
  private String suffix;

  private Boolean addPrefixWhenEmpty;
  private Boolean addSuffixWhenEmpty;

  private String format;
  private Integer scale;

  private RenderableToken() {
    super();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];
      if (!BeeUtils.hasLength(value, 1)) {
        continue;
      }

      switch (member) {
        case SOURCE:
          setSource(value.trim());
          break;
        case PREFIX:
          setPrefix(value);
          break;
        case SUFFIX:
          setSuffix(value);
          break;
        case ADD_PREFIX_WHEN_EMPTY:
          setAddPrefixWhenEmpty(BeeUtils.toBooleanOrNull(value.trim()));
          break;
        case ADD_SUFFIX_WHEN_EMPTY:
          setAddSuffixWhenEmpty(BeeUtils.toBooleanOrNull(value.trim()));
          break;
        case FORMAT:
          setFormat(value.trim());
          break;
        case SCALE:
          setScale(BeeUtils.toIntOrNull(value.trim()));
          break;
      }
    }
  }

  public Boolean getAddPrefixWhenEmpty() {
    return addPrefixWhenEmpty;
  }

  public Boolean getAddSuffixWhenEmpty() {
    return addSuffixWhenEmpty;
  }

  public String getFormat() {
    return format;
  }

  @Override
  public List<Property> getInfo() {
    return PropertyUtils.createProperties("Source", getSource(),
        "Prefix", getPrefix(),
        "Suffix", getSuffix(),
        "Add Prefix When Empty", getAddPrefixWhenEmpty(),
        "Add Suffix When Empty", getAddSuffixWhenEmpty(),
        "Format", getFormat(),
        "Scale", getScale());
  }

  public String getPrefix() {
    return prefix;
  }

  public Integer getScale() {
    return scale;
  }

  public String getSource() {
    return source;
  }

  public String getSuffix() {
    return suffix;
  }

  public void replaceSource(String oldId, String newId) {
    if (BeeUtils.same(getSource(), oldId) && !BeeUtils.isEmpty(newId)) {
      setSource(newId.trim());
    }
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case SOURCE:
          arr[i++] = getSource();
          break;
        case PREFIX:
          arr[i++] = getPrefix();
          break;
        case SUFFIX:
          arr[i++] = getSuffix();
          break;
        case ADD_PREFIX_WHEN_EMPTY:
          arr[i++] = getAddPrefixWhenEmpty();
          break;
        case ADD_SUFFIX_WHEN_EMPTY:
          arr[i++] = getAddSuffixWhenEmpty();
          break;
        case FORMAT:
          arr[i++] = getFormat();
          break;
        case SCALE:
          arr[i++] = getScale();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAddPrefixWhenEmpty(Boolean addPrefixWhenEmpty) {
    this.addPrefixWhenEmpty = addPrefixWhenEmpty;
  }

  public void setAddSuffixWhenEmpty(Boolean addSuffixWhenEmpty) {
    this.addSuffixWhenEmpty = addSuffixWhenEmpty;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setScale(Integer scale) {
    this.scale = scale;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  private void setAttributes(Map<String, String> attributes) {
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      String key = attribute.getKey();
      String value = attribute.getValue();
      if (!BeeUtils.hasLength(value, 1)) {
        continue;
      }

      if (BeeUtils.same(key, UiConstants.ATTR_SOURCE)) {
        setSource(value.trim());
      } else if (BeeUtils.same(key, ATTR_PREFIX)) {
        setPrefix(value);
      } else if (BeeUtils.same(key, ATTR_SUFFIX)) {
        setSuffix(value);
      } else if (BeeUtils.same(key, ATTR_ADD_PREFIX_WHEN_EMPTY)) {
        setAddPrefixWhenEmpty(BeeUtils.toBooleanOrNull(value.trim()));
      } else if (BeeUtils.same(key, ATTR_ADD_SUFFIX_WHEN_EMPTY)) {
        setAddSuffixWhenEmpty(BeeUtils.toBooleanOrNull(value.trim()));
      } else if (BeeUtils.same(key, UiConstants.ATTR_FORMAT)) {
        setFormat(value.trim());
      } else if (BeeUtils.same(key, UiConstants.ATTR_SCALE)) {
        setScale(BeeUtils.toIntOrNull(value.trim()));
      }
    }
  }
}
