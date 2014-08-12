package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.html.HeadElement;
import elemental.html.IFrameElement;
import elemental.html.StyleElement;
import elemental.html.Window;
import elemental.js.dom.JsElement;
import elemental.js.html.JsIFrameElement;

public class Frame extends Widget implements IdentifiableWidget {

  public Frame() {
    super();
    setElement(Element.as((JsIFrameElement) Browser.getDocument().createIFrameElement()));
    init();
  }

  public Frame(String url) {
    this();
    setUrl(url);
  }

  public void clear() {
    if (!isEmpty()) {
      setBodyHtml(BeeConst.STRING_EMPTY);
    }
  }

  public void focus() {
    getContentWindow().focus();
  }

  public Element getBody() {
    return ((JsElement) getContentDocument().getBody()).cast();
  }

  public Document getContentDocument() {
    return getIFrameElement().getContentDocument();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "frame";
  }

  public IFrameElement getIFrameElement() {
    return (JsIFrameElement) getElement().cast();
  }

  public void injectStyleSheet(String css) {
    Assert.notEmpty(css);

    Document document = getContentDocument();
    Assert.notNull(document);
    HeadElement head = document.getHead();
    Assert.notNull(head);

    StyleElement style = document.createStyleElement();
    style.setType("text/css");
    style.setInnerText(css);

    head.appendChild(style);
  }

  public boolean isEmpty() {
    if (getContentDocument() == null) {
      return true;
    }

    Element body = getBody();
    return body == null || body.getChildCount() <= 0;
  }

  public void print() {
    getContentWindow().print();
  }

  public void setBodyHtml(String html) {
    getContentDocument().getBody().setInnerHTML(html);
  }

  public void setHtml(String html) {
    Document document = getContentDocument();

    document.open();
    document.write(html);
    document.close();
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setUrl(String url) {
    getIFrameElement().setSrc(url);
  }

  private Window getContentWindow() {
    return getIFrameElement().getContentWindow();
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
    setStyleName("bee-Frame");
  }
}
