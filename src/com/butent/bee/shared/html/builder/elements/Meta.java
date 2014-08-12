package com.butent.bee.shared.html.builder.elements;

import com.google.common.net.MediaType;

import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.builder.Element;

public class Meta extends Element {

  public enum HttpEquiv {
    CONTENT_LANGUAGE("content-language"),
    CONTENT_TYPE("content-type"),
    DEFAULT_STYLE("default-style"),
    REFRESH("refresh"),
    SET_COOKIE("set-cookie");

    private final String keyword;

    private HttpEquiv(String keyword) {
      this.keyword = keyword;
    }

    public String getKeyword() {
      return keyword;
    }
  }

  public Meta() {
    super();
  }

  public Meta addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Meta charset(String value) {
    setAttribute(Attributes.CHARSET, value);
    return this;
  }

  public Meta content(String value) {
    setAttribute(Attributes.CONTENT, value);
    return this;
  }

  public Meta encodingDeclarationUtf8() {
    return httpEquiv(HttpEquiv.CONTENT_TYPE).content(MediaType.HTML_UTF_8.toString());
  }

  public Meta httpEquiv(HttpEquiv httpEquiv) {
    if (httpEquiv == null) {
      removeAttribute(Attributes.HTTP_EQUIV);
    } else {
      setAttribute(Attributes.HTTP_EQUIV, httpEquiv.getKeyword());
    }
    return this;
  }

  public Meta id(String value) {
    setId(value);
    return this;
  }

  public Meta lang(String value) {
    setLang(value);
    return this;
  }

  public Meta name(String value) {
    setAttribute(Attributes.NAME, value);
    return this;
  }

  public Meta title(String value) {
    setTitle(value);
    return this;
  }
}
