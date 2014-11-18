package com.butent.bee.client.composite;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RequiresResize;

import com.butent.bee.client.Global;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.layout.Focus;
import com.butent.bee.client.style.Font;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.EditStopEvent.Handler;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains presentation and logic of a slider bar user interface component.
 */
public class SliderBar extends Focus implements RequiresResize, Editor,
    HasValueChangeHandlers<String> {

  public interface LabelFormatter {
    String formatLabel(double value);
  }

  private class KeyTimer extends Timer {
    private boolean firstRun = true;
    private int repeatDelay = 30;
    private boolean shiftRight;
    private int multiplier = 1;

    @Override
    public void run() {
      if (firstRun) {
        firstRun = false;
        startSliding(true);
      }

      if (shiftRight) {
        setCurrentValue(curValue + multiplier * stepSize, true);
      } else {
        setCurrentValue(curValue - multiplier * stepSize, true);
      }

      schedule(repeatDelay);
    }

    public void schedule(int delayMillis, boolean right, int multi) {
      firstRun = true;
      this.shiftRight = right;
      this.multiplier = multi;
      super.schedule(delayMillis);
    }
  }

  private static final String STYLE_SHELL = BeeConst.CSS_CLASS_PREFIX + "SliderBar-shell";
  private static final String STYLE_LINE = BeeConst.CSS_CLASS_PREFIX + "SliderBar-line";
  private static final String STYLE_KNOB = BeeConst.CSS_CLASS_PREFIX + "SliderBar-knob";
  private static final String STYLE_LABEL = BeeConst.CSS_CLASS_PREFIX + "SliderBar-label";
  private static final String STYLE_TICK = BeeConst.CSS_CLASS_PREFIX + "SliderBar-tick";

  private static final String STYLE_SLIDING = "sliding";

  private double curValue;
  private double maxValue;
  private double minValue;
  private double stepSize;

  private final Image knobImage;

  private int numLabels;
  private final List<Element> labelElements = new ArrayList<>();
  private LabelFormatter labelFormatter;

  private int numTicks;
  private final List<Element> tickElements = new ArrayList<>();

  private final Element lineElement;
  private int lineLeftOffset;

  private boolean slidingKeyboard;
  private boolean slidingMouse;

  private boolean enabled = true;

  private KeyTimer keyTimer = new KeyTimer();

  private boolean nullable = true;

  private boolean editing;

  private String options;

  private boolean handlesTabulation;

  private boolean summarize;

  public SliderBar(double value, double min, double max, double step) {
    this(value, min, max, step, 0, 0);
  }

  public SliderBar(double value, double min, double max, double step, int labels, int ticks) {
    super();
    this.curValue = value;

    this.minValue = min;
    this.maxValue = max;
    this.stepSize = step;

    this.numLabels = labels;
    this.numTicks = ticks;

    setStyleName(STYLE_SHELL);

    lineElement = DOM.createDiv();
    getElement().appendChild(lineElement);
    lineElement.setClassName(STYLE_LINE);
    DomUtils.createId(lineElement, "line");
    lineElement.getStyle().setPosition(Position.ABSOLUTE);

    knobImage = new Image(knobDefault());
    Element knobElement = knobImage.getElement();
    getElement().appendChild(knobElement);
    knobElement.setClassName(STYLE_KNOB);
    DomUtils.createId(knobElement, "knob");
    knobElement.getStyle().setPosition(Position.ABSOLUTE);

    sinkEvents(Event.MOUSEEVENTS | Event.ONMOUSEWHEEL | Event.KEYEVENTS | Event.FOCUSEVENTS);
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    return addValueChangeHandler(handler);
  }

  @Override
  public HandlerRegistration addEditStopHandler(Handler handler) {
    return addHandler(handler, EditStopEvent.getType());
  }

  @Override
  public HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public void clearValue() {
    setCurrentValue(getMinValue(), false);
  }

  public double getCurrentValue() {
    return curValue;
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "slider";
  }

  public LabelFormatter getLabelFormatter() {
    return labelFormatter;
  }

  public double getMaxValue() {
    return maxValue;
  }

  public double getMinValue() {
    return minValue;
  }

  @Override
  public String getNormalizedValue() {
    return getValue();
  }

  public int getNumLabels() {
    return numLabels;
  }

  public int getNumTicks() {
    return numTicks;
  }

  @Override
  public String getOptions() {
    return this.options;
  }

  public double getStepSize() {
    return stepSize;
  }

  public double getTotalRange() {
    if (minValue > maxValue) {
      return 0;
    } else {
      return maxValue - minValue;
    }
  }

  @Override
  public Value getSummary() {
    return BooleanValue.of(BeeUtils.isMore(getCurrentValue(), getMinValue()));
  }

  @Override
  public String getValue() {
    return BeeUtils.toString(getCurrentValue());
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.SLIDER_BAR;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return false;
  }

  @Override
  public boolean handlesTabulation() {
    return handlesTabulation;
  }

  @Override
  public boolean isEditing() {
    return editing;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return node != null && getElement().isOrHasChild(node);
  }

  public ImageResource knobDefault() {
    return Global.getImages().slider();
  }

  public ImageResource knobDisabled() {
    return Global.getImages().sliderDisabled();
  }

  public ImageResource knobSliding() {
    return Global.getImages().sliderSliding();
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    if (enabled) {
      switch (event.getTypeInt()) {
        case Event.ONBLUR:
          keyTimer.cancel();
          if (slidingMouse) {
            DOM.releaseCapture(getElement());
            slidingMouse = false;
            slideKnob(event);
            stopSliding(true);
          } else if (slidingKeyboard) {
            slidingKeyboard = false;
            stopSliding(true);
          }
          unhighlight();
          break;

        case Event.ONFOCUS:
          highlight();
          break;

        case Event.ONMOUSEWHEEL:
          int velocityY = event.getMouseWheelVelocityY();
          event.preventDefault();
          if (velocityY > 0) {
            shiftRight(1);
          } else {
            shiftLeft(1);
          }
          break;

        case Event.ONKEYDOWN:
          if (!slidingKeyboard) {
            int multiplier = 1;
            if (event.getCtrlKey()) {
              multiplier = (int) (getTotalRange() / stepSize / 10);
            }

            switch (event.getKeyCode()) {
              case KeyCodes.KEY_HOME:
                event.preventDefault();
                setCurrentValue(minValue, true);
                break;
              case KeyCodes.KEY_END:
                event.preventDefault();
                setCurrentValue(maxValue, true);
                break;
              case KeyCodes.KEY_LEFT:
                event.preventDefault();
                slidingKeyboard = true;
                startSliding(false);
                shiftLeft(multiplier);
                keyTimer.schedule(400, false, multiplier);
                break;
              case KeyCodes.KEY_RIGHT:
                event.preventDefault();
                slidingKeyboard = true;
                startSliding(false);
                shiftRight(multiplier);
                keyTimer.schedule(400, true, multiplier);
                break;
              case 32:
                event.preventDefault();
                setCurrentValue(minValue + getTotalRange() / 2, true);
                break;
            }
          }
          break;

        case Event.ONKEYUP:
          keyTimer.cancel();
          if (slidingKeyboard) {
            slidingKeyboard = false;
            stopSliding(true);
          }
          break;

        case Event.ONMOUSEDOWN:
          setFocus(true);
          slidingMouse = true;
          DOM.setCapture(getElement());
          startSliding(true);
          event.preventDefault();
          slideKnob(event);
          break;

        case Event.ONMOUSEUP:
          if (slidingMouse) {
            DOM.releaseCapture(getElement());
            slidingMouse = false;
            slideKnob(event);
            stopSliding(true);
          }
          break;

        case Event.ONMOUSEMOVE:
          if (slidingMouse) {
            slideKnob(event);
          }
          break;
      }
    }
  }

  @Override
  public void onResize() {
    redraw(false);
  }

  public void redraw(boolean force) {
    if (force || isReady()) {
      int width = getShellWidth();
      int lineWidth = getLineWidth();

      lineLeftOffset = (width / 2) - (lineWidth / 2);
      StyleUtils.setLeft(lineElement, lineLeftOffset);

      drawLabels(force);
      drawTicks(force);
      drawKnob(force);
    }
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  public void setCurrentValue(double cv, boolean fireEvents) {
    this.curValue = Math.max(minValue, Math.min(maxValue, cv));
    double remainder = (this.curValue - minValue) % stepSize;
    this.curValue -= remainder;

    if ((remainder > (stepSize / 2)) && ((this.curValue + stepSize) <= maxValue)) {
      this.curValue += stepSize;
    }

    drawKnob(false);

    if (fireEvents) {
      ValueChangeEvent.fire(this, BeeUtils.toString(this.curValue));
    }
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (enabled) {
      knobImage.setResource(knobDefault());
      StyleUtils.removeStyleDependentName(lineElement, StyleUtils.SUFFIX_DISABLED);
    } else {
      knobImage.setResource(knobDisabled());
      StyleUtils.addStyleDependentName(lineElement, StyleUtils.SUFFIX_DISABLED);
    }
    redraw(false);
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
    this.handlesTabulation = handlesTabulation;
  }

  public void setLabelFormatter(LabelFormatter labelFormatter) {
    this.labelFormatter = labelFormatter;
  }

  public void setMaxValue(double maxValue) {
    this.maxValue = maxValue;
    drawLabels(false);
    resetCurrentValue();
  }

  public void setMinValue(double minValue) {
    this.minValue = minValue;
    drawLabels(false);
    resetCurrentValue();
  }

  @Override
  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  public void setNumLabels(int numLabels) {
    this.numLabels = numLabels;
  }

  public void setNumTicks(int numTicks) {
    this.numTicks = numTicks;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setStepSize(double stepSize) {
    this.stepSize = stepSize;
    resetCurrentValue();
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public void setValue(String value) {
    if (BeeUtils.toDouble(value) != getCurrentValue()) {
      setCurrentValue(BeeUtils.toDouble(value), false);
    }
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return Collections.emptyList();
  }

  protected String formatLabel(double value) {
    if (labelFormatter != null) {
      return labelFormatter.formatLabel(value);
    } else {
      return BeeUtils.toString(BeeUtils.round(value, 1));
    }
  }

  protected double getKnobPercent() {
    if (maxValue <= minValue) {
      return 0;
    }

    double percent = (curValue - minValue) / (maxValue - minValue);
    return Math.max(0.0, Math.min(1.0, percent));
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        redraw(true);
      }
    });
  }

  private void drawKnob(boolean force) {
    if (force || isReady()) {
      int lineWidth = getLineWidth();
      int knobWidth = getKnobWidth();

      int knobLeftOffset = (int) (lineLeftOffset + (getKnobPercent() * lineWidth)
          - (knobWidth / 2));
      knobLeftOffset = Math.min(knobLeftOffset, lineLeftOffset + lineWidth - (knobWidth / 2) - 1);
      StyleUtils.setLeft(knobImage.getElement(), knobLeftOffset);
    }
  }

  private void drawLabels(boolean force) {
    if (!force && !isReady()) {
      return;
    }
    if (numLabels > 0) {
      int shellWidth = getShellWidth();
      int lineWidth = getLineWidth();

      Element label;
      for (int i = 0; i <= numLabels; i++) {
        if (i < labelElements.size()) {
          label = labelElements.get(i);
        } else {
          label = DOM.createDiv();
          DomUtils.createId(label, "label");
          label.getStyle().setPosition(Position.ABSOLUTE);
          label.getStyle().setDisplay(Display.NONE);
          label.setClassName(STYLE_LABEL);
          if (!enabled) {
            StyleUtils.addStyleDependentName(label, StyleUtils.SUFFIX_DISABLED);
          }
          getElement().appendChild(label);
          labelElements.add(label);
        }

        double value = minValue + (getTotalRange() * i / numLabels);
        label.getStyle().setVisibility(Visibility.HIDDEN);
        label.getStyle().clearDisplay();
        label.setInnerHTML(formatLabel(value));

        StyleUtils.zeroLeft(label);
        int labelWidth = label.getOffsetWidth();
        if (labelWidth <= 0) {
          labelWidth = Rulers.getLineWidth(new Font.Builder().size(FontSize.X_SMALL).build(),
              label.getInnerHTML(), true);
        }

        int labelLeftOffset = lineLeftOffset + (lineWidth * i / numLabels) - (labelWidth / 2);
        labelLeftOffset = Math.min(labelLeftOffset, shellWidth - labelWidth - 1);
        labelLeftOffset = Math.max(labelLeftOffset, 1);
        StyleUtils.setLeft(label, labelLeftOffset);
        label.getStyle().setVisibility(Visibility.VISIBLE);
      }

      for (int i = numLabels + 1; i < labelElements.size(); i++) {
        labelElements.get(i).getStyle().setDisplay(Display.NONE);
      }
    } else {
      for (Element elem : labelElements) {
        elem.getStyle().setDisplay(Display.NONE);
      }
    }
  }

  private void drawTicks(boolean force) {
    if (!force && !isReady()) {
      return;
    }

    int lineWidth = getLineWidth();
    if (numTicks > 0) {
      Element tick;
      for (int i = 0; i <= numTicks; i++) {
        if (i < tickElements.size()) {
          tick = tickElements.get(i);
        } else {
          tick = DOM.createDiv();
          DomUtils.createId(tick, "tick");
          tick.getStyle().setPosition(Position.ABSOLUTE);
          tick.getStyle().setDisplay(Display.NONE);
          tick.setClassName(STYLE_TICK);

          getElement().appendChild(tick);
          tickElements.add(tick);
        }
        if (!enabled) {
          StyleUtils.addStyleDependentName(tick, StyleUtils.SUFFIX_DISABLED);
        }

        tick.getStyle().setVisibility(Visibility.HIDDEN);
        tick.getStyle().clearDisplay();
        int tickWidth = tick.getOffsetWidth();
        if (tickWidth <= 0) {
          tickWidth = 1;
        }

        int tickLeftOffset = lineLeftOffset + (lineWidth * i / numTicks) - (tickWidth / 2);
        tickLeftOffset = Math.min(tickLeftOffset, lineLeftOffset + lineWidth - tickWidth);
        StyleUtils.setLeft(tick, tickLeftOffset);
        tick.getStyle().setVisibility(Visibility.VISIBLE);
      }

      for (int i = numTicks + 1; i < tickElements.size(); i++) {
        tickElements.get(i).getStyle().setDisplay(Display.NONE);
      }
    } else {
      for (Element elem : tickElements) {
        elem.getStyle().setDisplay(Display.NONE);
      }
    }
  }

  private int getKnobWidth() {
    int width = knobImage.getElement().getOffsetWidth();
    if (width <= 0) {
      width = 11;
    }
    return width;
  }

  private int getLineWidth() {
    int width = lineElement.getOffsetWidth();
    if (width <= 0) {
      width = getShellWidth() * 95 / 100;
    }
    return width;
  }

  private int getShellWidth() {
    int width = getElement().getClientWidth();
    if (width <= 0) {
      width = 400;
    }
    return width;
  }

  private void highlight() {
    addStyleDependentName(StyleUtils.SUFFIX_FOCUSED);
  }

  private boolean isReady() {
    return isAttached() && getElement().getClientWidth() > 0 && lineElement.getOffsetWidth() > 0;
  }

  private void resetCurrentValue() {
    setCurrentValue(getCurrentValue(), false);
  }

  private void shiftLeft(int numSteps) {
    setCurrentValue(getCurrentValue() - numSteps * stepSize, true);
  }

  private void shiftRight(int numSteps) {
    setCurrentValue(getCurrentValue() + numSteps * stepSize, true);
  }

  private void slideKnob(Event event) {
    int x = event.getClientX();
    if (x > 0) {
      int lineWidth = getLineWidth();
      int lineLeft = lineElement.getAbsoluteLeft();
      double percent = (double) (x - lineLeft) / lineWidth * 1.0;
      setCurrentValue(getTotalRange() * percent + minValue, true);
    }
  }

  private void startSliding(boolean highlight) {
    if (highlight) {
      StyleUtils.addStyleDependentName(lineElement, STYLE_SLIDING);
      StyleUtils.addStyleDependentName(knobImage.getElement(), STYLE_SLIDING);
      knobImage.setResource(knobSliding());
    }
  }

  private void stopSliding(boolean unhighlight) {
    if (unhighlight) {
      StyleUtils.removeStyleDependentName(lineElement, STYLE_SLIDING);
      StyleUtils.removeStyleDependentName(knobImage.getElement(), STYLE_SLIDING);
      knobImage.setResource(knobDefault());
    }
  }

  private void unhighlight() {
    removeStyleDependentName(StyleUtils.SUFFIX_FOCUSED);
  }
}