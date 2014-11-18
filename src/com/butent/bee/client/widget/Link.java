package com.butent.bee.client.widget;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusWidget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Enables using hyperlink user interface component.
 */

public class Link extends FocusWidget implements IdentifiableWidget, HasHtml {

  public Link() {
    super(Document.get().createAnchorElement());
    init();
  }

  public Link(String text, String href) {
    this();

    if (!BeeUtils.isEmpty(text)) {
      setHtml(text);
    }
    if (!BeeUtils.isEmpty(href)) {
      setHref(href);
    }
  }

  public String getHref() {
    return getAnchorElement().getHref();
  }

  @Override
  public String getHtml() {
    return getElement().getInnerHTML();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "link";
  }

  public String getTarget() {
    return getAnchorElement().getTarget();
  }

  @Override
  public String getText() {
    return getElement().getInnerText();
  }

  public void setHref(String href) {
    getAnchorElement().setHref(href);
  }

  @Override
  public void setHtml(String html) {
    getElement().setInnerHTML(html);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setTarget(String target) {
    getAnchorElement().setTarget(target);
  }

  @Override
  public void setText(String text) {
    getElement().setInnerText(text);
  }

  public void update(String value) {
    update(value, BeeConst.DEFAULT_VALUE_SEPARATOR);
  }

  public void update(String value, String separator) {
    Assert.notEmpty(value);
    String sep = BeeUtils.notEmpty(separator, BeeConst.DEFAULT_VALUE_SEPARATOR);
    String html;
    String href;

    if (BeeUtils.containsSame(value, sep)) {
      html = BeeUtils.getPrefix(value, sep);
      href = BeeUtils.getSuffix(value, sep);
    } else {
      html = value;
      href = value;
    }
    setHtml(BeeUtils.trim(html));
    setHref(BeeUtils.trim(href));
  }

  private AnchorElement getAnchorElement() {
    return AnchorElement.as(getElement());
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
    setStyleName(BeeConst.CSS_CLASS_PREFIX + "Link");

    setTarget(Keywords.BROWSING_CONTEXT_BLANK);
  }
}
