package com.butent.bee.client.widget;

import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.client.TakesValue;

/**
 * Enables using value label user interface component.
 */

public class ValueLabel<T> extends Label implements TakesValue<T> {

  private T value;
  private final Renderer<? super T> renderer;

  private boolean textOnly;

  public ValueLabel(Renderer<? super T> renderer, boolean inline) {
    super(inline);
    this.renderer = renderer;
  }

  @Override
  public T getValue() {
    return value;
  }

  public boolean isTextOnly() {
    return textOnly;
  }

  public void setTextOnly(boolean textOnly) {
    this.textOnly = textOnly;
  }

  @Override
  public void setValue(T value) {
    this.value = value;

    if (isTextOnly()) {
      getElement().setInnerText(render(value));
    } else {
      setHtml(render(value));
    }
  }

  protected Renderer<? super T> getRenderer() {
    return renderer;
  }

  protected String render(T v) {
    return renderer.render(v);
  }
}