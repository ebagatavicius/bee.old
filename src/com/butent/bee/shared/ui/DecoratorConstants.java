package com.butent.bee.shared.ui;

import com.google.common.base.Splitter;

public final class DecoratorConstants {

  public static final String DIRECTORY = "decorators";
  public static final String SCHEMA = "decorator.xsd";
  public static final String NAMESPACE = "http://www.butent.com/decorator";

  public static final String TAG_DECORATORS = "Decorators";

  public static final String TAG_ABSTRACT = "abstract";
  public static final String TAG_DECORATOR = "decorator";

  public static final String TAG_REQUIRED_PARAM = "requiredParam";
  public static final String TAG_OPTIONAL_PARAM = "optionalParam";
  public static final String TAG_CONST = "const";
  public static final String TAG_STYLE = "style";
  public static final String TAG_LIFECYCLE = "lifecycle";
  public static final String TAG_HANDLER = "handler";
  public static final String TAG_TEMPLATE = "template";

  public static final String TAG_CREATED = "created";
  public static final String TAG_INSERTED = "inserted";
  public static final String TAG_REMOVED = "removed";

  public static final String TAG_CONTENT = "ins";

  public static final String ATTR_ID = "id";
  public static final String ATTR_EXTENDS = "extends";
  public static final String ATTR_EVENT_TARGET = "eventTarget";
  public static final String ATTR_APPEARANCE_TARGET = "appearanceTarget";
  public static final String ATTR_APPEARANCE_DEEP = "appearanceDeep";
  public static final String ATTR_APPLY_AUTHOR_STYLES = "apply-author-styles";

  public static final String ATTR_EVENT = "event";
  public static final String ATTR_TARGET = "target";
  public static final String ATTR_DEEP = "deep";

  public static final String SUBSTITUTE_PREFIX = "{";
  public static final String SUBSTITUTE_SUFFIX = "}";

  public static final String ROLE_ROOT = "root";
  public static final String ROLE_CONTENT = "content";

  public static final String OPTION_CLASS = "class";
  public static final String OPTION_STYLE = "style";
  public static final String OPTION_ROOT_CLASS = "rootClass";
  public static final String OPTION_ROOT_STYLE = "rootStyle";
  public static final String OPTION_CONTENT_CLASS = "contentClass";
  public static final String OPTION_CONTENT_STYLE = "contentStyle";

  public static final String OPTION_ROLE_CLASS = "roleClass";
  public static final String OPTION_ROLE_STYLE = "roleStyle";

  public static final String OPTION_CAPTION = "caption";
  public static final String OPTION_VALUE_REQUIRED = "valueRequired";
  public static final String OPTION_HAS_DEFAULTS = "hasDefaults";

  public static final String ROLE_DEFINITION_SEPARATOR = "|";
  public static final String ROLE_VALUE_SEPARATOR = "=";

  public static final Splitter ROLE_DEFINITION_SPLITTER =
      Splitter.on(ROLE_DEFINITION_SEPARATOR).omitEmptyStrings().trimResults();

  private DecoratorConstants() {
  }
}
