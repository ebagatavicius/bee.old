package com.butent.bee.client.richtext;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.widget.CustomWidget;
import com.butent.bee.shared.HasHtml;

class RichTextArea extends CustomWidget implements HasHtml, HasAllFocusHandlers, EnablableWidget {

  enum FontSize {
    XX_SMALL(1),
    X_SMALL(2),
    SMALL(3),
    MEDIUM(4),
    LARGE(5),
    X_LARGE(6),
    XX_LARGE(7);

    private final int number;

    private FontSize(int number) {
      this.number = number;
    }

    int getNumber() {
      return number;
    }
  }

  interface Formatter {

    void createLink(String url);

    String getBackColor();

    String getForeColor();

    void insertHorizontalRule();

    void insertHtml(String html);

    void insertImage(String url);

    void insertOrderedList();

    void insertUnorderedList();

    boolean isBold();

    boolean isItalic();

    boolean isStrikethrough();

    boolean isSubscript();

    boolean isSuperscript();

    boolean isUnderlined();

    void leftIndent();

    boolean queryCommandSupported(String cmd);

    void redo();

    void removeFormat();

    void removeLink();

    void rightIndent();

    void selectAll();

    void setBackColor(String color);

    void setFontName(String name);

    void setFontSize(FontSize fontSize);

    void setForeColor(String color);

    void setJustification(Justification justification);

    void toggleBold();

    void toggleItalic();

    void toggleStrikethrough();

    void toggleSubscript();

    void toggleSuperscript();

    void toggleUnderline();

    void undo();
  }

  enum Justification {
    CENTER("JustifyCenter"),
    FULL("JustifyFull"),
    LEFT("JustifyLeft"),
    RIGHT("JustifyRight");

    private final String cmd;

    private Justification(String cmd) {
      this.cmd = cmd;
    }

    String getCmd() {
      return cmd;
    }
  }

  private final RichTextAreaImpl impl;

  RichTextArea() {
    super(Document.get().createIFrameElement());
    this.impl = new RichTextAreaImpl(getElement());

    setTabIndex(0);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return addDomHandler(handler, FocusEvent.getType());
  }

  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(handler, KeyUpEvent.getType());
  }

  public boolean contains(Node node) {
    return impl.contains(node);
  }

  @Override
  public String getHtml() {
    return impl.getHtml();
  }

  public int getTabIndex() {
    return getElement().getTabIndex();
  }

  @Override
  public String getText() {
    return impl.getText();
  }

  @Override
  public boolean isEnabled() {
    return impl.isEnabled();
  }

  @Override
  public void onBrowserEvent(Event event) {
    String type = event.getType();
    if (EventUtils.isMouseButtonEvent(type) || EventUtils.isKeyEvent(type)) {
      EventTarget target = event.getEventTarget();

      if (Node.is(target) && contains(Node.as(target)) && !target.equals(getElement())) {
        if (!Previewer.preview(event, getElement())) {
          return;
        }
      }
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void setEnabled(boolean enabled) {
    impl.setEnabled(enabled);
  }

  public void setFocus(boolean focused) {
    if (isAttached()) {
      impl.setFocus(focused);
    }
  }

  @Override
  public void setHtml(String html) {
    impl.setHtml(html);
  }

  public void setTabIndex(int index) {
    getElement().setTabIndex(index);
  }

  @Override
  public void setText(String text) {
    impl.setText(text);
  }

  @Override
  protected void onAttach() {
    super.onAttach();
    impl.initElement();
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    impl.uninitElement();
  }

  Formatter getFormatter() {
    return impl;
  }
}
