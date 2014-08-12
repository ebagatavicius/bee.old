package com.butent.bee.client.dom;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;

import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.style.Font;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.ImageElement;
import elemental.js.dom.JsElement;

public final class Rulers {

  private static final double cmToPx = 1.0;
  private static final double mmToPx = 0.1;
  private static final double inToPx = 2.54;
  private static final double ptToPx = inToPx / 72.0;
  private static final double pcToPx = ptToPx * 12.0;

  private static final SpanElement lineRuler;
  private static final DivElement areaRuler;

  private static final DivElement fixedUnitRuler;
  private static final DivElement relativeUnitRuler;

  static {
    String idPrefix = "ruler";

    lineRuler = Document.get().createSpanElement();
    DomUtils.createId(lineRuler, idPrefix);
    lineRuler.setInnerHTML(BeeConst.HTML_NBSP);

    StyleUtils.makeAbsolute(lineRuler);
    BodyPanel.conceal(lineRuler);

    areaRuler = Document.get().createDivElement();
    DomUtils.createId(areaRuler, idPrefix);
    areaRuler.setInnerHTML(BeeConst.HTML_NBSP);

    StyleUtils.makeAbsolute(areaRuler);
    BodyPanel.conceal(areaRuler);

    fixedUnitRuler = Document.get().createDivElement();
    DomUtils.createId(fixedUnitRuler, idPrefix);
    fixedUnitRuler.setInnerHTML(BeeConst.HTML_NBSP);

    StyleUtils.setWidth(fixedUnitRuler, BeeConst.DOUBLE_ONE, CssUnit.CM);
    StyleUtils.setHeight(fixedUnitRuler, BeeConst.DOUBLE_ONE, CssUnit.CM);

    BodyPanel.conceal(fixedUnitRuler);

    relativeUnitRuler = Document.get().createDivElement();
    DomUtils.createId(relativeUnitRuler, idPrefix);

    BodyPanel.conceal(relativeUnitRuler);
  }

  public static int getAreaHeight(Font font, String content, boolean asHtml) {
    return getHeight(areaRuler, font, content, asHtml);
  }

  public static Size getAreaSize(Font font, String content, boolean asHtml) {
    return getSize(areaRuler, font, content, asHtml);
  }

  public static int getAreaWidth(Font font, String content, boolean asHtml) {
    return getWidth(areaRuler, font, content, asHtml);
  }

  public static void getImageNaturalSize(String src, final Consumer<Size> consumer) {
    Assert.notEmpty(src);
    Assert.notNull(consumer);

    final ImageElement imageElement = (ImageElement) Browser.getDocument().createElement(Tags.IMG);
    imageElement.addEventListener(BrowserEvents.LOAD, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        consumer.accept(new Size(imageElement.getNaturalWidth(), imageElement.getNaturalHeight()));
        if (imageElement.getParentElement() != null) {
          imageElement.getParentElement().removeChild(imageElement);
        }
      }
    }, false);

    imageElement.setSrc(src);
    BodyPanel.conceal((JsElement) imageElement);
  }

  public static int getIntPixels(double value, CssUnit unit) {
    return BeeUtils.toInt(getPixels(value, unit));
  }

  public static int getIntPixels(double value, CssUnit unit, double containerSize) {
    return BeeUtils.toInt(getPixels(value, unit, containerSize));
  }

  public static int getIntPixels(double value, CssUnit unit, Font font) {
    return BeeUtils.toInt(getPixels(value, unit, font));
  }

  public static int getIntPixels(double value, CssUnit unit, Font font, double containerSize) {
    return BeeUtils.toInt(getPixels(value, unit, font, containerSize));
  }

  public static int getLineHeight(Font font, String content, boolean asHtml) {
    return getHeight(lineRuler, font, content, asHtml);
  }

  public static Size getLineSize(Font font, String content, boolean asHtml) {
    return getSize(lineRuler, font, content, asHtml);
  }

  public static int getLineWidth(Font font, String content, boolean asHtml) {
    return getWidth(lineRuler, font, content, asHtml);
  }

  public static double getPixels(double value, CssUnit unit) {
    return getPixels(value, unit, null);
  }

  public static double getPixels(double value, CssUnit unit, double containerSize) {
    return getPixels(value, unit, null, containerSize);
  }

  public static double getPixels(double value, CssUnit unit, Font font) {
    return getPixels(value, unit, font, 0);
  }

  public static double getPixels(double value, CssUnit unit, Font font, double containerSize) {
    Assert.notNull(unit);

    switch (unit.getType()) {
      case ABSOLUTE:
        return value * getFixedUnitSizeInPixels(unit);

      case CONTAINER_PERCENTAGE:
        return value * getContainerDependentUnitSizeInPixels(containerSize);

      case FONT_RELATIVE:
        return value * getFontDependentUnitSizeInPixels(unit, font);

      case VIEWPORT_PERCENTAGE:
        return value * getViewportPercentageUnitSizeInPixels(unit).getWidth();
    }

    Assert.untouchable();
    return BeeConst.DOUBLE_ZERO;
  }

  public static double getUnitSize(Element element, CssUnit unit, Orientation orientation) {
    Assert.notNull(unit);

    switch (unit.getType()) {
      case ABSOLUTE:
        return getFixedUnitSizeInPixels(unit);

      case CONTAINER_PERCENTAGE:
        Assert.notNull(element);
        Assert.notNull(orientation);

        double size = orientation.isVertical()
            ? element.getClientHeight() : element.getClientWidth();
        return getContainerDependentUnitSizeInPixels(size) / 100;

      case FONT_RELATIVE:
        Font font = (element == null) ? null : Font.getComputed(element);
        return getFontDependentUnitSizeInPixels(unit, font);

      case VIEWPORT_PERCENTAGE:
        ClientRect rect = getViewportPercentageUnitSizeInPixels(unit);
        return (orientation != null && orientation.isVertical())
            ? rect.getHeight() : rect.getWidth();
    }

    Assert.untouchable();
    return BeeConst.DOUBLE_ZERO;
  }

  private static double getContainerDependentUnitSizeInPixels(double containerSize) {
    return containerSize / 100.0;
  }

  private static double getFixedUnitSizeInPixels(CssUnit unit) {
    if (unit == CssUnit.PX) {
      return 1;
    }

    double width = ClientRect.createBounding(fixedUnitRuler).getWidth();

    switch (unit) {
      case CM:
        return width * cmToPx;
      case MM:
        return width * mmToPx;
      case IN:
        return width * inToPx;
      case PT:
        return width * ptToPx;
      case PC:
        return width * pcToPx;
      default:
        Assert.untouchable();
        return 0;
    }
  }

  private static double getFontDependentUnitSizeInPixels(CssUnit unit, Font font) {
    double size;
    prepareRuler(relativeUnitRuler, font);

    StyleUtils.setWidth(relativeUnitRuler, BeeConst.DOUBLE_ONE, unit);
    StyleUtils.setHeight(relativeUnitRuler, BeeConst.DOUBLE_ONE, unit);

    ClientRect rect = ClientRect.createBounding(relativeUnitRuler);

    switch (unit) {
      case EM:
      case CH:
      case REM:
        size = rect.getWidth();
        break;
      case EX:
        size = rect.getHeight();
        break;
      default:
        Assert.untouchable();
        size = 0;
    }

    relativeUnitRuler.getStyle().clearWidth();
    relativeUnitRuler.getStyle().clearHeight();

    resetRuler(relativeUnitRuler, font);
    return size;
  }

  private static int getHeight(Element ruler, Font font, String content, boolean asHtml) {
    if (content == null) {
      return 0;
    }

    prepareRuler(ruler, font, content, asHtml);
    int height = ruler.getOffsetHeight();
    resetRuler(ruler, font);

    return height;
  }

  private static Size getSize(Element ruler, Font font, String content, boolean asHtml) {
    if (content == null) {
      return null;
    }

    prepareRuler(ruler, font, content, asHtml);

    int width = ruler.getOffsetWidth();
    int height = ruler.getOffsetHeight();

    resetRuler(ruler, font);

    return new Size(width, height);
  }

  private static ClientRect getViewportPercentageUnitSizeInPixels(CssUnit unit) {
    StyleUtils.setWidth(relativeUnitRuler, BeeConst.DOUBLE_ONE, unit);
    StyleUtils.setHeight(relativeUnitRuler, BeeConst.DOUBLE_ONE, unit);

    ClientRect rect = ClientRect.createBounding(relativeUnitRuler);

    relativeUnitRuler.getStyle().clearWidth();
    relativeUnitRuler.getStyle().clearHeight();

    return rect;
  }

  private static int getWidth(Element ruler, Font font, String content, boolean asHtml) {
    if (content == null) {
      return 0;
    }

    prepareRuler(ruler, font, content, asHtml);
    int width = ruler.getOffsetWidth();
    resetRuler(ruler, font);

    return width;
  }

  private static void prepareRuler(Element ruler, Font font) {
    if (font != null) {
      font.applyTo(ruler);
    }
  }

  private static void prepareRuler(Element ruler, Font font, String content, boolean asHtml) {
    if (font != null) {
      font.applyTo(ruler);
    }
    if (content != null) {
      if (asHtml) {
        StyleUtils.setWhiteSpace(ruler, WhiteSpace.NORMAL);
        ruler.setInnerHTML(content);
      } else {
        StyleUtils.setWhiteSpace(ruler, WhiteSpace.PRE);
        ruler.setInnerText(content);
      }
    }
  }

  private static void resetRuler(Element ruler, Font font) {
    if (font != null) {
      font.removeFrom(ruler);
    }
    ruler.getStyle().clearWhiteSpace();
    ruler.setInnerHTML(BeeConst.HTML_NBSP);
  }

  private Rulers() {
  }
}
