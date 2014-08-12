package com.butent.bee.shared.news;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

public final class Headline implements BeeSerializable, HasCaption {

  private enum Type {
    NEW, UPDATED
  }

  public static final String SEPARATOR = " ";

  public static Headline create(long id, String caption, boolean isNew) {
    if (isNew) {
      return forInsert(id, caption);
    } else {
      return forUpdate(id, caption);
    }
  }

  public static Headline create(long id, String caption, String subtitle, boolean isNew) {
    if (isNew) {
      return forInsert(id, caption, subtitle);
    } else {
      return forUpdate(id, caption, subtitle);
    }
  }

  public static Headline create(long id, String caption, List<String> subtitles, boolean isNew) {
    if (isNew) {
      return forInsert(id, caption, subtitles);
    } else {
      return forUpdate(id, caption, subtitles);
    }
  }

  public static Headline forInsert(long id, String caption) {
    return new Headline(id, caption, null, Type.NEW);
  }

  public static Headline forInsert(long id, String caption, List<String> subtitles) {
    return forInsert(id, caption, BeeUtils.join(SEPARATOR, subtitles));
  }

  public static Headline forInsert(long id, String caption, String subtitle) {
    return new Headline(id, caption, subtitle, Type.NEW);
  }

  public static Headline forUpdate(long id, String caption) {
    return new Headline(id, caption, null, Type.UPDATED);
  }

  public static Headline forUpdate(long id, String caption, List<String> subtitles) {
    return forUpdate(id, caption, BeeUtils.join(SEPARATOR, subtitles));
  }

  public static Headline forUpdate(long id, String caption, String subtitle) {
    return new Headline(id, caption, subtitle, Type.UPDATED);
  }

  public static Headline restore(String s) {
    Assert.notEmpty(s);

    Headline headline = new Headline();
    headline.deserialize(s);
    return headline;
  }

  private long id;

  private String caption;
  private String subtitle;

  private Type type;

  private Headline() {
  }

  private Headline(long id, String caption, String subtitle, Type type) {
    this.id = id;
    this.caption = caption;
    this.subtitle = subtitle;
    this.type = type;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 4);

    int i = 0;
    setId(BeeUtils.toLong(arr[i++]));
    setCaption(arr[i++]);
    setSubtitle(arr[i++]);
    setType(Codec.unpack(Type.class, arr[i++]));
  }

  @Override
  public String getCaption() {
    return caption;
  }

  public long getId() {
    return id;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public boolean isNew() {
    return getType() == Type.NEW;
  }

  public boolean isUpdated() {
    return getType() == Type.UPDATED;
  }

  @Override
  public String serialize() {
    List<String> values = Lists.newArrayList(Long.toString(getId()), getCaption(), getSubtitle(),
        Codec.pack(getType()));
    return Codec.beeSerialize(values);
  }

  private Type getType() {
    return type;
  }

  private void setCaption(String caption) {
    this.caption = caption;
  }

  private void setId(long id) {
    this.id = id;
  }

  private void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }

  private void setType(Type type) {
    this.type = type;
  }
}
