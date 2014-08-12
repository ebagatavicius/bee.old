package com.butent.bee.shared.html.builder.elements;

import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.builder.Element;

public class Link extends Element {

  public enum Rel {
    ALTERNATE("alternate"),
    AUTHOR("author"),
    BOOKMARK("bookmark"),
    HELP("help"),
    ICON("icon"),
    LICENSE("license"),
    NEXT("next"),
    NO_FOLLOW("nofollow"),
    NO_REFERRER("noreferrer"),
    PREFETCH("prefetch"),
    PREV("prev"),
    SEARCH("search"),
    SHORTCUT_ICON("shortcut icon"),
    STYLE_SHEET("stylesheet"),
    TAG("tag");

    private final String keyword;

    private Rel(String keyword) {
      this.keyword = keyword;
    }

    public String getKeyword() {
      return keyword;
    }
  }

  public Link() {
    super();
  }

  public Link addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Link crossoriginAnonymous() {
    setAttribute(Attributes.CROSS_ORIGIN, Keywords.CORS_SETTINGS_ANONYMOUS);
    return this;
  }

  public Link crossoriginUseCredentials() {
    setAttribute(Attributes.CROSS_ORIGIN, Keywords.CORS_SETTINGS_USE_CREDENTIALS);
    return this;
  }

  public Link href(String value) {
    setAttribute(Attributes.HREF, value);
    return this;
  }

  public Link hrefLang(String value) {
    setAttribute(Attributes.HREF_LANG, value);
    return this;
  }

  public Link id(String value) {
    setId(value);
    return this;
  }

  public Link lang(String value) {
    setLang(value);
    return this;
  }

  public Link media(String value) {
    setAttribute(Attributes.MEDIA, value);
    return this;
  }

  public Link rel(Rel rel) {
    if (rel == null) {
      removeAttribute(Attributes.REL);
    } else {
      setAttribute(Attributes.REL, rel.getKeyword());
    }
    return this;
  }

  public Link sizes(String value) {
    setAttribute(Attributes.SIZES, value);
    return this;
  }

  public Link styleSheet(String href) {
    return rel(Rel.STYLE_SHEET).href(href);
  }

  public Link title(String value) {
    setTitle(value);
    return this;
  }

  public Link type(String value) {
    setAttribute(Attributes.TYPE, value);
    return this;
  }
}
