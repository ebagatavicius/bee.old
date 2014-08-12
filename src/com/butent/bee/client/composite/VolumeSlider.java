package com.butent.bee.client.composite;

import com.google.common.primitives.Ints;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.EditStopEvent.Handler;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.List;

/**
 * Implements a volume slider user interface component (note:similar to sound level slider in MS
 * Windows).
 */

public class VolumeSlider extends Absolute implements Editor, HasValueChangeHandlers<String> {

  private final class VolumeSpinner extends SpinnerBase {
    private VolumeSpinner(SpinnerListener spinnerListener, long value, long min, long max,
        int minStep, int maxStep, boolean constrained) {
      super(spinnerListener, value, min, max, minStep, maxStep, constrained);
    }

    @Override
    public ImageResource arrowDown() {
      return IMAGES.arrowLeft();
    }

    @Override
    public ImageResource arrowDownDisabled() {
      return IMAGES.arrowLeftDisabled();
    }

    @Override
    public ImageResource arrowDownHover() {
      return IMAGES.arrowLeftHover();
    }

    @Override
    public ImageResource arrowDownPressed() {
      return IMAGES.arrowLeftPressed();
    }

    @Override
    public ImageResource arrowUp() {
      return IMAGES.arrowRight();
    }

    @Override
    public ImageResource arrowUpDisabled() {
      return IMAGES.arrowRightDisabled();
    }

    @Override
    public ImageResource arrowUpHover() {
      return IMAGES.arrowRightHover();
    }

    @Override
    public ImageResource arrowUpPressed() {
      return IMAGES.arrowRightPressed();
    }
  }

  private final VolumeSpinner spinner;
  private final ProgressBar progressBar;

  private int spacing = 5;
  private int padding = 1;

  private boolean nullable = true;

  private boolean editing;

  private String options;

  private boolean handlesTabulation;

  private SpinnerListener listener = new SpinnerListener() {
    @Override
    public void onSpinning(long value) {
      progressBar.setProgress(value);
      ValueChangeEvent.fire(VolumeSlider.this, BeeUtils.toString(value));
    }
  };

  public VolumeSlider(long value, long min, long max) {
    this(value, min, max, 1, 5);
  }

  public VolumeSlider(long value, long min, long max, int step) {
    this(value, min, max, step, step);
  }

  public VolumeSlider(long value, long min, long max, int minStep, int maxStep) {
    super(Position.RELATIVE, Overflow.HIDDEN);
    setStyleName("bee-VolumeSlider");

    progressBar = new ProgressBar(min, max, value);
    spinner = new VolumeSpinner(listener, value, min, max, minStep, maxStep, true);

    add(spinner.getDecrementArrow());
    spinner.getDecrementArrow().setStyleName("decreaseArrow");

    add(progressBar);

    add(spinner.getIncrementArrow());
    spinner.getIncrementArrow().setStyleName("increaseArrow");

    sinkEvents(Event.ONMOUSEWHEEL);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return progressBar.addBlurHandler(handler);
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
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return progressBar.addFocusHandler(handler);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public void clearValue() {
    spinner.setValue(BeeUtils.toLong(progressBar.getMinProgress()), false);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "volume-slider";
  }

  public long getLong() {
    return spinner.getValue();
  }

  @Override
  public String getNormalizedValue() {
    return getValue();
  }

  @Override
  public String getOptions() {
    return options;
  }

  public int getPadding() {
    return padding;
  }

  public ProgressBar getProgressBar() {
    return progressBar;
  }

  public int getSpacing() {
    return spacing;
  }

  public VolumeSpinner getSpinner() {
    return spinner;
  }

  @Override
  public int getTabIndex() {
    return progressBar.getTabIndex();
  }

  @Override
  public String getValue() {
    return BeeUtils.toString(spinner.getValue());
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.VOLUME_SLIDER;
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
    return spinner.isEnabled();
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return node != null && getElement().isOrHasChild(node);
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (event.getTypeInt() == Event.ONMOUSEWHEEL && isEnabled()) {
      int z = event.getMouseWheelVelocityY();
      if (z != 0) {
        spinner.doStep(spinner.getValue(), z > 0);
        event.preventDefault();
      }
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void onResize() {
    progressBar.onResize();
    redraw();
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  @Override
  public void setAccessKey(char key) {
    progressBar.setAccessKey(key);
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setEnabled(boolean enabled) {
    spinner.setEnabled(enabled);
  }

  @Override
  public void setFocus(boolean focused) {
    progressBar.setFocus(focused);
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
    this.handlesTabulation = handlesTabulation;
  }

  @Override
  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setPadding(int padding) {
    this.padding = padding;
  }

  public void setSpacing(int spacing) {
    this.spacing = spacing;
  }

  @Override
  public void setTabIndex(int index) {
    progressBar.setTabIndex(index);
  }

  @Override
  public void setValue(String value) {
    spinner.setValue(BeeUtils.toLong(value), false);
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  protected void onLoad() {
    redraw();
    super.onLoad();
  }

  private void redraw() {
    if (isAttached()) {
      int panelWidth = getElement().getClientWidth();
      int panelHeight = getElement().getClientHeight();

      int barWidth = progressBar.getElement().getOffsetWidth();
      int barHeight = progressBar.getElement().getOffsetHeight();

      int leftWidth = spinner.getDecrementArrow().getWidth();
      int leftHeight = spinner.getDecrementArrow().getHeight();
      int rightWidth = spinner.getIncrementArrow().getWidth();
      int rightHeight = spinner.getIncrementArrow().getHeight();

      int h = Ints.max(panelHeight, barHeight, leftHeight, rightHeight,
          DomUtils.getTextBoxClientHeight());
      if (panelHeight < h) {
        StyleUtils.setHeight(this, h);
      }
      if (barHeight < h) {
        StyleUtils.setHeight(progressBar, h);
      }

      int w = leftWidth + rightWidth + spacing * 2 + padding * 2;
      if (barWidth <= 0 || panelWidth - w != barWidth) {
        barWidth = (panelWidth > w) ? panelWidth - w : w;
        StyleUtils.setWidth(progressBar, barWidth);
        progressBar.redraw();
      }
      if (panelWidth - w != barWidth) {
        panelWidth = w + barWidth;
        StyleUtils.setWidth(this, panelWidth);
      }

      setWidgetPosition(spinner.getDecrementArrow(), padding, (h - leftHeight) / 2);
      setWidgetPosition(progressBar, padding + leftWidth + spacing, 0);
      setWidgetPosition(spinner.getIncrementArrow(), panelWidth - padding - rightWidth,
          (h - rightHeight) / 2);
    }
  }
}
