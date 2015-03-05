package com.butent.bee.client.composite;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.RequiresResize;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Enables an user interface component, which indicates to a user how much progress a process has
 * made and how much is still left to be done.
 */
public class ProgressBar extends FocusWidget implements IdentifiableWidget, RequiresResize {

  /**
   * Requires classes implementing this interface to have TextFormatter method.
   */
  public interface TextFormatter {
    String getText(double curProgress);
  }

  private Element barElement;
  private Element textElement;

  private double curProgress;
  private double maxProgress;
  private double minProgress;

  private boolean textVisible = true;
  private TextFormatter textFormatter;

  private String styleNameShell = BeeConst.CSS_CLASS_PREFIX + "ProgressBar-shell";
  private String styleNameBar = BeeConst.CSS_CLASS_PREFIX + "ProgressBar-bar";
  private String styleNameText = BeeConst.CSS_CLASS_PREFIX + "ProgressBar-text";

  private String styleNameFirstHalf = "firstHalf";
  private String styleNameSecondHalf = "secondHalf";

  public ProgressBar() {
    this(0.0, 100.0, 0.0);
  }

  public ProgressBar(double curProgress) {
    this(0.0, 100.0, curProgress);
  }

  public ProgressBar(double minProgress, double maxProgress) {
    this(minProgress, maxProgress, 0.0);
  }

  public ProgressBar(double minProgress, double maxProgress, double curProgress) {
    this(minProgress, maxProgress, curProgress, null);
  }

  public ProgressBar(double minProgress, double maxProgress,
      double curProgress, TextFormatter textFormatter) {
    this.minProgress = minProgress;
    this.maxProgress = maxProgress;
    this.curProgress = curProgress;
    setTextFormatter(textFormatter);

    setElement(Document.get().createDivElement());
    setStyleName(styleNameShell);

    barElement = DOM.createDiv();
    getElement().appendChild(barElement);

    StyleUtils.fullWidth(barElement);
    StyleUtils.fullHeight(barElement);
    barElement.setClassName(styleNameBar);
    DomUtils.createId(barElement, "bar");

    textElement = DOM.createDiv();
    getElement().appendChild(textElement);

    textElement.getStyle().setPosition(Position.ABSOLUTE);
    StyleUtils.zeroTop(textElement);
    textElement.setClassName(styleNameText);
    DomUtils.createId(textElement, "text");

    setProgress(curProgress);
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "progress";
  }

  public double getMaxProgress() {
    return maxProgress;
  }

  public double getMinProgress() {
    return minProgress;
  }

  public double getPercent() {
    if (maxProgress <= minProgress) {
      return 0.0;
    }

    double percent = (curProgress - minProgress) / (maxProgress - minProgress);
    return Math.max(0.0, Math.min(1.0, percent));
  }

  public double getProgress() {
    return curProgress;
  }

  public TextFormatter getTextFormatter() {
    return textFormatter;
  }

  public boolean isTextVisible() {
    return textVisible;
  }

  @Override
  public void onResize() {
    redraw();
  }

  public void redraw() {
    if (isAttached() && textVisible) {
      int width = getElement().getClientWidth();
      int textWidth = textElement.getOffsetWidth();
      int left = (width / 2) - (textWidth / 2);
      StyleUtils.setLeft(textElement, left);
    }
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setMaxProgress(double maxProgress) {
    this.maxProgress = maxProgress;
    curProgress = Math.min(curProgress, maxProgress);
    resetProgress();
  }

  public void setMinProgress(double minProgress) {
    this.minProgress = minProgress;
    curProgress = Math.max(curProgress, minProgress);
    resetProgress();
  }

  public void setProgress(double cp) {
    this.curProgress = Math.max(minProgress, Math.min(maxProgress, cp));

    int percent = (int) (100 * getPercent());
    StyleUtils.setWidth(barElement, percent, CssUnit.PCT);
    textElement.setInnerHTML(generateText());

    String textClassName = textElement.getClassName();

    if (percent < 50) {
      if (!BeeUtils.containsSame(textClassName, styleNameFirstHalf)) {
        StyleUtils.addStyleDependentName(textElement, styleNameFirstHalf);
      }
      if (BeeUtils.containsSame(textClassName, styleNameSecondHalf)) {
        StyleUtils.removeStyleDependentName(textElement, styleNameSecondHalf);
      }
    } else {
      if (BeeUtils.containsSame(textClassName, styleNameFirstHalf)) {
        StyleUtils.removeStyleDependentName(textElement, styleNameFirstHalf);
      }
      if (!BeeUtils.containsSame(textClassName, styleNameSecondHalf)) {
        StyleUtils.addStyleDependentName(textElement, styleNameSecondHalf);
      }
    }
    redraw();
  }

  public void setTextFormatter(TextFormatter textFormatter) {
    this.textFormatter = textFormatter;
  }

  public void setTextVisible(boolean isVisible) {
    this.textVisible = isVisible;
    if (this.textVisible) {
      textElement.getStyle().clearDisplay();
      redraw();
    } else {
      textElement.getStyle().setDisplay(Display.NONE);
    }
  }

  protected String generateText() {
    if (textFormatter != null) {
      return textFormatter.getText(curProgress);
    } else {
      return Double.toString(curProgress);
    }
  }

  protected Element getBarElement() {
    return barElement;
  }

  protected Element getTextElement() {
    return textElement;
  }

  @Override
  protected void onLoad() {
    redraw();
  }

  private void resetProgress() {
    setProgress(getProgress());
  }
}