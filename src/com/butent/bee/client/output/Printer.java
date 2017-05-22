package com.butent.bee.client.output;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.TextAreaElement;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.utils.LayoutEngine;
import com.butent.bee.client.widget.Frame;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.html.Window;
import elemental.js.dom.JsElement;

public final class Printer {

  private static final BeeLogger logger = LogUtils.getLogger(Printer.class);

  private static final String ID_PREFIX = "p-";

  private static final int DELAY_MS = 100;
  private static final int NUMBER_OF_ATTEMPTS = 10;

  private static final String HTML_START = "<!doctype html><html><head>"
      + "<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\">"
      + "<style type=\"text/css\">";
  private static final String HTML_MIDDLE = "</style></head><body>";
  private static final String HTML_END = "</body></html>";

  private static final String WINDOW_FEATURES = "resizable,scrollbars,menubar,toolbar";

  private static final boolean useFrame = LayoutEngine.detect() == LayoutEngine.WEBKIT;
  private static Frame frame;

  private static String cssRules;

  public static boolean isPrinting() {
    return frame != null && !frame.isEmpty();
  }

  public static void onInjectStyleSheet(String css) {
    if (!BeeUtils.isEmpty(css)) {
      if (frame != null) {
        frame.injectStyleSheet(css);
      }
      cssRules = null;
    }
  }

  public static void print(Element element, Printable widget) {
    Assert.notNull(element);

    String html = element.getString();
    if (BeeUtils.isEmpty(html)) {
      html = DomUtils.getOuterHtml(element);
    }

    print(html, widget);
  }

  public static void print(Printable widget) {
    Assert.notNull(widget);
    Element element = widget.getPrintElement();
    if (element != null) {
      print(element, widget);
    }
  }

  public static void print(String html, Printable widget) {
    Assert.notEmpty(html, "print: html is empty");

    if (useFrame) {
      printUsingFrame(html, widget);
    } else {
      printUsingNewWindow(html, widget);
    }
  }

  private static void createFrame(String html) {
    frame = new Frame();

    StyleUtils.setSize(frame, 0, 0);
    frame.getElement().getStyle().setBorderStyle(BorderStyle.NONE);

    Browser.getDocument().getBody().appendChild(frame.getIFrameElement());

    frame.setHtml(HTML_START + StyleUtils.getRules() + HTML_MIDDLE + html + HTML_END);
  }

//@formatter:off
  private static native NodeList<Element> getElements(Document d) /*-{
    return d.body.getElementsByTagName("*");
  }-*/;
//@formatter:on

  private static boolean prepare(Element target, Printable widget) {
    if (target == null) {
      return true;
    }

    String id = target.getId();
    if (BeeUtils.isEmpty(id)) {
      return true;
    }

    target.setId(ID_PREFIX + id);

    Element source = DomUtils.getElementQuietly(id);
    if (source == null) {
      logger.warning(id, "source not found");

    } else if (DomUtils.isTextAreaElement(source)) {
      String value = TextAreaElement.as(source).getValue();
      if (!BeeUtils.isEmpty(value)) {
        TextAreaElement.as(target).setValue(value);
      }

    } else if (DomUtils.isInputElement(source)) {
      String value = InputElement.as(source).getValue();
      if (!BeeUtils.equalsTrim(value, InputElement.as(target).getValue())) {
        InputElement.as(target).setValue(value);
      }

    } else if (DomUtils.isSelectElement(source)) {
      if (!SelectElement.as(source).isMultiple()) {
        SelectElement.as(target).setSelectedIndex(SelectElement.as(source).getSelectedIndex());
      }
    }

    if (widget != null && source != null) {
      return widget.onPrint(source, target);
    }
    return true;
  }

  private static void prepareBody(Element targetBody) {
    String className = BodyPanel.get().getElement().getClassName();
    if (!BeeUtils.isEmpty(className)) {
      targetBody.addClassName(className);
    }

    targetBody.addClassName(BeeConst.CSS_CLASS_PREFIX + "Print");
    targetBody.addClassName(BeeConst.CSS_CLASS_PREFIX + "Print-"
        + (useFrame ? "Frame" : "Window"));
  }

  private static void prepareElements(NodeList<Element> elements, Printable widget) {
    List<Element> hide = new ArrayList<>();

    for (int i = 0; i < elements.getLength(); i++) {
      Element element = elements.getItem(i);
      if (!prepare(element, widget)) {
        hide.add(element);
      }
    }

    for (Element element : hide) {
      element.getStyle().setDisplay(Display.NONE);
    }
  }

  private static void printFrame() {
    frame.focus();
    frame.print();
    frame.clear();
  }

  private static void printUsingFrame(String html, final Printable widget) {
    if (frame == null) {
      createFrame(html);
    } else {
      frame.setBodyHtml(html);
    }

    Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
      int counter;

      @Override
      public boolean execute() {
        NodeList<Element> elements = getElements(frame.getContentDocument());
        if (elements == null || elements.getLength() <= 0) {
          counter++;
          logger.warning("print attempt", counter, "failed");
          return counter < NUMBER_OF_ATTEMPTS;
        }

        if (widget != null) {
          frame.getContentDocument().setTitle(BeeUtils.trim(widget.getCaption()));
        }

        prepareBody(frame.getBody());
        prepareElements(elements, widget);

        printFrame();
        return false;
      }
    }, DELAY_MS);
  }

  private static void printUsingNewWindow(String html, final Printable widget) {
    if (cssRules == null) {
      cssRules = StyleUtils.getRules();
    }

    Window window = Browser.getWindow().open(null, null, WINDOW_FEATURES);
    if (window == null) {
      logger.warning("print: cannot open new window");
      return;
    }

    Document document = window.getDocument();
    document.open();

    document.writeln(HTML_START);
    document.writeln(cssRules);
    document.writeln(HTML_MIDDLE);
    document.writeln(html);
    document.write(HTML_END);

    document.close();

    String caption = (widget == null) ? null : widget.getCaption();
    if (!BeeUtils.isEmpty(caption)) {
      document.setTitle(caption);
    }

    Element body = ((JsElement) document.getBody()).cast();
    prepareBody(body);

    NodeList<Element> elements = getElements(document);
    prepareElements(elements, widget);

    window.print();
  }

  private Printer() {
    super();
  }
}
