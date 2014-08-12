package com.butent.bee.client.composite;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.widget.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains core behaviors of spinner type user interface components.
 */
public class SpinnerBase {

  public interface Resources extends ClientBundle {
    @Source("arrowDown.png")
    ImageResource arrowDown();

    @Source("arrowDownDisabled.png")
    ImageResource arrowDownDisabled();

    @Source("arrowDownHover.png")
    ImageResource arrowDownHover();

    @Source("arrowDownPressed.png")
    ImageResource arrowDownPressed();

    @Source("arrowLeft.png")
    ImageResource arrowLeft();

    @Source("arrowLeftDisabled.png")
    ImageResource arrowLeftDisabled();

    @Source("arrowLeftHover.png")
    ImageResource arrowLeftHover();

    @Source("arrowLeftPressed.png")
    ImageResource arrowLeftPressed();

    @Source("arrowRight.png")
    ImageResource arrowRight();

    @Source("arrowRightDisabled.png")
    ImageResource arrowRightDisabled();

    @Source("arrowRightHover.png")
    ImageResource arrowRightHover();

    @Source("arrowRightPressed.png")
    ImageResource arrowRightPressed();

    @Source("arrowUp.png")
    ImageResource arrowUp();

    @Source("arrowUpDisabled.png")
    ImageResource arrowUpDisabled();

    @Source("arrowUpHover.png")
    ImageResource arrowUpHover();

    @Source("arrowUpPressed.png")
    ImageResource arrowUpPressed();
  }

  public static final int INITIAL_SPEED = 7;

  protected static final Resources IMAGES = GWT.create(Resources.class);

  private final Image decrementArrow;
  private final Image incrementArrow;

  private List<SpinnerListener> spinnerListeners = new ArrayList<>();

  private int step;
  private int minStep;
  private int maxStep;
  private int initialSpeed;

  private long value;
  private long min;
  private long max;
  private boolean constrained;

  private boolean enabled = true;
  private boolean increment;

  private final Timer timer = new Timer() {
    private int counter;
    private int speed = initialSpeed;

    @Override
    public void cancel() {
      super.cancel();
      speed = initialSpeed;
      counter = 0;
    }

    @Override
    public void run() {
      counter++;
      if (speed <= 0 || counter % speed == 0) {
        speed--;
        counter = 0;
        if (increment) {
          increase();
        } else {
          decrease();
        }
      }
      if (speed < 0 && step < maxStep) {
        step++;
      }
    }
  };

  private MouseDownHandler mouseDownHandler = new MouseDownHandler() {
    @Override
    public void onMouseDown(MouseDownEvent event) {
      if (enabled) {
        Image sender = (Image) event.getSource();
        if (sender == incrementArrow) {
          sender.setResource(arrowUpPressed());
          increment = true;
          increase();
        } else {
          sender.setResource(arrowDownPressed());
          increment = false;
          decrease();
        }
        timer.scheduleRepeating(30);
      }
    }
  };

  private MouseOverHandler mouseOverHandler = new MouseOverHandler() {
    @Override
    public void onMouseOver(MouseOverEvent event) {
      if (enabled) {
        Image sender = (Image) event.getSource();
        if (sender == incrementArrow) {
          sender.setResource(arrowUpHover());
        } else {
          sender.setResource(arrowDownHover());
        }
      }
    }
  };

  private MouseOutHandler mouseOutHandler = new MouseOutHandler() {
    @Override
    public void onMouseOut(MouseOutEvent event) {
      if (enabled) {
        cancelTimer((Widget) event.getSource());
      }
    }
  };

  private MouseUpHandler mouseUpHandler = new MouseUpHandler() {
    @Override
    public void onMouseUp(MouseUpEvent event) {
      if (enabled) {
        cancelTimer((Widget) event.getSource());
      }
    }
  };

  public SpinnerBase(SpinnerListener spinner, long value) {
    this(spinner, value, 0, 0, 1, 99, false);
  }

  public SpinnerBase(SpinnerListener spinner, long value, long min, long max) {
    this(spinner, value, min, max, 1, 99, true);
  }

  public SpinnerBase(SpinnerListener spinner, long value, long min, long max, int step) {
    this(spinner, value, min, max, step, step, true);
  }

  public SpinnerBase(SpinnerListener spinner, long value, long min, long max,
      int minStep, int maxStep) {
    this(spinner, value, min, max, minStep, maxStep, true);
  }

  public SpinnerBase(SpinnerListener spinner, long value, long min, long max,
      int minStep, int maxStep, boolean constrained) {
    super();
    spinnerListeners.add(spinner);

    this.value = value;
    this.min = min;
    this.max = max;

    this.step = minStep;
    this.minStep = minStep;
    this.maxStep = maxStep;
    this.constrained = constrained;

    this.initialSpeed = INITIAL_SPEED;

    incrementArrow = new Image(arrowUp());
    incrementArrow.addMouseUpHandler(mouseUpHandler);
    incrementArrow.addMouseDownHandler(mouseDownHandler);
    incrementArrow.addMouseOverHandler(mouseOverHandler);
    incrementArrow.addMouseOutHandler(mouseOutHandler);

    decrementArrow = new Image(arrowDown());
    decrementArrow.addMouseUpHandler(mouseUpHandler);
    decrementArrow.addMouseDownHandler(mouseDownHandler);
    decrementArrow.addMouseOverHandler(mouseOverHandler);
    decrementArrow.addMouseOutHandler(mouseOutHandler);

    fireOnValueChanged();
  }

  public void addSpinnerListener(SpinnerListener listener) {
    spinnerListeners.add(listener);
  }

  public ImageResource arrowDown() {
    return IMAGES.arrowDown();
  }

  public ImageResource arrowDownDisabled() {
    return IMAGES.arrowDownDisabled();
  }

  public ImageResource arrowDownHover() {
    return IMAGES.arrowDownHover();
  }

  public ImageResource arrowDownPressed() {
    return IMAGES.arrowDownPressed();
  }

  public ImageResource arrowUp() {
    return IMAGES.arrowUp();
  }

  public ImageResource arrowUpDisabled() {
    return IMAGES.arrowUpDisabled();
  }

  public ImageResource arrowUpHover() {
    return IMAGES.arrowUpHover();
  }

  public ImageResource arrowUpPressed() {
    return IMAGES.arrowUpPressed();
  }

  public void doStep(long z, boolean incr) {
    long v = z;

    if (incr) {
      if (v < getMin() || v >= getMax()) {
        v = getMin();
      } else {
        v += getMinStep();
        if (v > getMax()) {
          v = getMax();
        }
      }
    } else {
      if (v <= getMin() || v > getMax()) {
        v = getMax();
      } else {
        v -= getMinStep();
        if (v < getMin()) {
          v = getMin();
        }
      }
    }

    updateValue(v);
  }

  public Image getDecrementArrow() {
    return decrementArrow;
  }

  public Image getIncrementArrow() {
    return incrementArrow;
  }

  public long getMax() {
    return max;
  }

  public int getMaxStep() {
    return maxStep;
  }

  public long getMin() {
    return min;
  }

  public int getMinStep() {
    return minStep;
  }

  public long getValue() {
    return value;
  }

  public boolean isConstrained() {
    return constrained;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isValid(long v) {
    if (isConstrained()) {
      return v >= getMin() && v <= getMax();
    } else {
      return true;
    }
  }

  public void removeSpinnerListener(SpinnerListener listener) {
    spinnerListeners.remove(listener);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (enabled) {
      incrementArrow.setResource(arrowUp());
      decrementArrow.setResource(arrowDown());
    } else {
      incrementArrow.setResource(arrowUpDisabled());
      decrementArrow.setResource(arrowDownDisabled());
    }
    if (!enabled) {
      timer.cancel();
    }
  }

  public void setInitialSpeed(int initialSpeed) {
    this.initialSpeed = initialSpeed;
  }

  public void setMax(long max) {
    this.max = max;
  }

  public void setMaxStep(int maxStep) {
    this.maxStep = maxStep;
  }

  public void setMin(long min) {
    this.min = min;
  }

  public void setMinStep(int minStep) {
    this.minStep = minStep;
  }

  public void setValue(long v, boolean fireEvent) {
    this.value = v;
    if (fireEvent) {
      fireOnValueChanged();
    }
  }

  public void updateValue(long v) {
    if (isValid(v)) {
      setValue(v, true);
    }
  }

  protected void decrease() {
    value -= step;
    if (constrained && value < min) {
      value = min;
      timer.cancel();
    }
    fireOnValueChanged();
  }

  protected void increase() {
    value += step;
    if (constrained && value > max) {
      value = max;
      timer.cancel();
    }
    fireOnValueChanged();
  }

  private void cancelTimer(Widget sender) {
    step = minStep;
    if (sender == incrementArrow) {
      ((Image) sender).setResource(arrowUp());
    } else {
      ((Image) sender).setResource(arrowDown());
    }
    timer.cancel();
  }

  private void fireOnValueChanged() {
    for (SpinnerListener listener : spinnerListeners) {
      listener.onSpinning(value);
    }
  }
}
