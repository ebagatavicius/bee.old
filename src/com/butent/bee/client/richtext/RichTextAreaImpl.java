package com.butent.bee.client.richtext;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Timer;

import com.butent.bee.client.richtext.RichTextArea.FontSize;
import com.butent.bee.client.richtext.RichTextArea.Justification;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import elemental.js.dom.JsNode;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.js.dom.JsElement;
import elemental.js.html.JsIFrameElement;
import elemental.html.IFrameElement;
import elemental.client.Browser;

class RichTextAreaImpl implements RichTextArea.Formatter, HasHtml {

  private static final BeeLogger logger = LogUtils.getLogger(RichTextAreaImpl.class);

  private static final String DESIGN_MODE_OFF = "off";
  private static final String DESIGN_MODE_ON = "on";

  private final IFrameElement element;

  private String textOrHtml;
  private boolean enabled = true;

  private boolean initializing;
  private boolean isPendingFocus;
  private boolean isReady;

  RichTextAreaImpl(Element el) {
    this.element = (IFrameElement) el;
  }

  @Override
  public void createLink(String url) {
    execCommand("CreateLink", url);
  }

  @Override
  public String getBackColor() {
    return queryCommandValue("BackColor");
  }

  @Override
  public String getForeColor() {
    return queryCommandValue("ForeColor");
  }

  @Override
  public String getHtml() {
    return isReady ? getBody().getInnerHTML() : textOrHtml;
  }

  @Override
  public String getText() {
    return isReady ? getBody().getInnerText() : textOrHtml;
  }

  @Override
  public void insertHorizontalRule() {
    execCommand("InsertHorizontalRule");
  }

  @Override
  public void insertHtml(String html) {
    execCommand("InsertHTML", html);
  }

  @Override
  public void insertImage(String url) {
    execCommand("InsertImage", url);
  }

  @Override
  public void insertOrderedList() {
    execCommand("InsertOrderedList");
  }

  @Override
  public void insertUnorderedList() {
    execCommand("InsertUnorderedList");
  }

  @Override
  public boolean isBold() {
    return queryCommandState("Bold");
  }

  @Override
  public boolean isItalic() {
    return queryCommandState("Italic");
  }

  @Override
  public boolean isStrikethrough() {
    return queryCommandState("Strikethrough");
  }

  @Override
  public boolean isSubscript() {
    return queryCommandState("Subscript");
  }

  @Override
  public boolean isSuperscript() {
    return queryCommandState("Superscript");
  }

  @Override
  public boolean isUnderlined() {
    return queryCommandState("Underline");
  }

  @Override
  public void leftIndent() {
    execCommand("Outdent");
  }

  @Override
  public boolean queryCommandSupported(String cmd) {
    if (element == null || element.getContentDocument() == null) {
      return Browser.getDocument().queryCommandSupported(cmd);
    } else {
      return element.getContentDocument().queryCommandSupported(cmd);
    }
  }

  @Override
  public void redo() {
    execCommand("Redo");
  }

  @Override
  public void removeFormat() {
    execCommand("RemoveFormat");
  }

  @Override
  public void removeLink() {
    execCommand("Unlink");
  }

  @Override
  public void rightIndent() {
    execCommand("Indent");
  }

  @Override
  public void selectAll() {
    execCommand("SelectAll");
  }

  @Override
  public void setBackColor(String color) {
    execCommand("BackColor", color);
  }

  @Override
  public void setFontName(String name) {
    execCommand("FontName", name);
  }

  @Override
  public void setFontSize(FontSize fontSize) {
    execCommand("FontSize", Integer.toString(fontSize.getNumber()));
  }

  @Override
  public void setForeColor(String color) {
    execCommand("ForeColor", color);
  }

  @Override
  public void setHtml(String html) {
    if (isReady) {
      setHtmlImpl(html);
    } else {
      textOrHtml = html;
    }
  }

  @Override
  public void setJustification(Justification justification) {
    if (justification != null) {
      execCommand(justification.getCmd());
    }
  }

  @Override
  public void setText(String text) {
    if (isReady) {
      setTextImpl(text);
    } else {
      textOrHtml = text;
    }
  }

  @Override
  public void toggleBold() {
    execCommand("Bold");
  }

  @Override
  public void toggleItalic() {
    execCommand("Italic");
  }

  @Override
  public void toggleStrikethrough() {
    execCommand("Strikethrough");
  }

  @Override
  public void toggleSubscript() {
    execCommand("Subscript");
  }

  @Override
  public void toggleSuperscript() {
    execCommand("Superscript");
  }

  @Override
  public void toggleUnderline() {
    execCommand("Underline");
  }

  @Override
  public void undo() {
    execCommand("Undo");
  }

  boolean contains(Node node) {
    if (node == null) {
      return false;
    }

    JsNode jsNode = node.cast();
    if (element.contains(jsNode)) {
      return true;
    } else {
      return element.getContentDocument().getBody().contains(jsNode);
    }
  }

  Element getElement() {
    return Element.as((JsIFrameElement) element);
  }

  void initElement() {
    initializing = true;

    element.addEventListener(BrowserEvents.LOAD, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (getBody() != null) {
          setEnabledImpl(isEnabled());
        }
      }
    }, false);

    if (getBody() == null) {
      Timer timer = new Timer() {
        @Override
        public void run() {
          if (getBody() != null) {
            cancel();
            onElementInitialized();
          }
        }
      };
      timer.scheduleRepeating(50);

    } else {
      onElementInitialized();
    }
  }

  boolean isEnabled() {
    return enabled;
  }

  void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (isReady) {
      setEnabledImpl(enabled);
    }
  }

  void setFocus(boolean focused) {
    if (isReady) {
      setFocusImpl(focused);
    } else {
      isPendingFocus = focused;
    }
  }

  void uninitElement() {
    isReady = false;

    if (initializing) {
      initializing = false;
    } else {
      unhookEvents();
    }
  }

  private void execCommand(String cmd) {
    execCommand(cmd, null);
  }

  private void execCommand(String cmd, String param) {
    if (isReady) {
      try {
        element.getContentDocument().execCommand(cmd, false, param);
      } catch (JavaScriptException ex) {
        logger.warning(NameUtils.getName(this), "error executing", cmd, param);
      }
    }
  }

  private Element getBody() {
    if (element.getContentDocument() != null && element.getContentDocument().getBody() != null) {
      return ((JsElement) element.getContentDocument().getBody()).cast();
    } else {
      return null;
    }
  }

//@formatter:off
  // CHECKSTYLE:OFF
  private native void hookEvents() /*-{
    var elem = this.@com.butent.bee.client.richtext.RichTextAreaImpl::element;
    var wnd = elem.contentWindow;

    elem.__gwt_handler = $entry(function(evt) {
      @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/dom/client/Element;)(evt, elem);
    });

    elem.__gwt_focusHandler = function(evt) {
      if (elem.__gwt_isFocused) {
        return;
      }

      elem.__gwt_isFocused = true;
      elem.__gwt_handler(evt);
    };

    elem.__gwt_blurHandler = function(evt) {
      if (!elem.__gwt_isFocused) {
        return;
      }

      elem.__gwt_isFocused = false;
      elem.__gwt_handler(evt);
    };

    wnd.addEventListener('keydown', elem.__gwt_handler, true);
    wnd.addEventListener('keyup', elem.__gwt_handler, true);
    wnd.addEventListener('keypress', elem.__gwt_handler, true);
    wnd.addEventListener('mousedown', elem.__gwt_handler, true);
    wnd.addEventListener('mouseup', elem.__gwt_handler, true);
    wnd.addEventListener('mousemove', elem.__gwt_handler, true);
    wnd.addEventListener('mouseover', elem.__gwt_handler, true);
    wnd.addEventListener('mouseout', elem.__gwt_handler, true);
    wnd.addEventListener('click', elem.__gwt_handler, true);

    wnd.addEventListener('focus', elem.__gwt_focusHandler, true);
    wnd.addEventListener('blur', elem.__gwt_blurHandler, true);
  }-*/;
  // CHECKSTYLE:ON
//@formatter:on

  private void onElementInitialized() {
    if (!initializing) {
      return;
    }

    initializing = false;
    isReady = true;

    setEnabledImpl(isEnabled());
    if (!BeeUtils.isEmpty(textOrHtml)) {
      setHtmlImpl(textOrHtml);
    }

    hookEvents();

    if (isPendingFocus) {
      isPendingFocus = false;
      setFocus(true);
    }
  }

  private boolean queryCommandState(String cmd) {
    if (isReady) {
      try {
        return element.getContentDocument().queryCommandState(cmd);
      } catch (JavaScriptException e) {
        return false;
      }
    }
    return false;
  }

  private String queryCommandValue(String cmd) {
    if (isReady) {
      try {
        return element.getContentDocument().queryCommandValue(cmd);
      } catch (JavaScriptException e) {
        return BeeConst.STRING_EMPTY;
      }
    }
    return BeeConst.STRING_EMPTY;
  }

  private void setEnabledImpl(boolean enbl) {
    String mode = enbl ? DESIGN_MODE_ON : DESIGN_MODE_OFF;
    element.getContentDocument().setDesignMode(mode);
  }

  private void setFocusImpl(boolean focused) {
    if (focused) {
      element.getContentWindow().focus();
    } else {
      element.getContentWindow().blur();
    }
  }

  private void setHtmlImpl(String html) {
    getBody().setInnerHTML(html);
  }

  private void setTextImpl(String text) {
    getBody().setInnerText(text);
  }

//@formatter:off
  private native void unhookEvents() /*-{
    var elem = this.@com.butent.bee.client.richtext.RichTextAreaImpl::element;
    var wnd = elem.contentWindow;

    wnd.removeEventListener('keydown', elem.__gwt_handler, true);
    wnd.removeEventListener('keyup', elem.__gwt_handler, true);
    wnd.removeEventListener('keypress', elem.__gwt_handler, true);
    wnd.removeEventListener('mousedown', elem.__gwt_handler, true);
    wnd.removeEventListener('mouseup', elem.__gwt_handler, true);
    wnd.removeEventListener('mousemove', elem.__gwt_handler, true);
    wnd.removeEventListener('mouseover', elem.__gwt_handler, true);
    wnd.removeEventListener('mouseout', elem.__gwt_handler, true);
    wnd.removeEventListener('click', elem.__gwt_handler, true);

    wnd.removeEventListener('focus', elem.__gwt_focusHandler, true);
    wnd.removeEventListener('blur', elem.__gwt_blurHandler, true);

    elem.__gwt_handler = null;
    elem.__gwt_focusHandler = null;
    elem.__gwt_blurHandler = null;
  }-*/;
//@formatter:on
}
