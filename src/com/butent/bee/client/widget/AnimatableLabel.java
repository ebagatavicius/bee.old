package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;

import com.butent.bee.client.animation.HasAnimatableActivity;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.shared.BeeConst;

public class AnimatableLabel extends Label implements EnablableWidget, HasAnimatableActivity {

  private boolean enabled = true;

  private Timer animationTimer;
  private int animationDuration;

  public AnimatableLabel() {
    super();
  }

  public AnimatableLabel(boolean inline) {
    super(inline);
  }

  public AnimatableLabel(Element element) {
    super(element);
  }

  public AnimatableLabel(String html) {
    super(html);
  }

  public void click() {
    if (isEnabled()) {
      EventUtils.click(this);
    }
  }

  @Override
  public int getAnimationDuration() {
    return animationDuration;
  }

  @Override
  public Timer getAnimationTimer() {
    return animationTimer;
  }

  @Override
  public String getIdPrefix() {
    return "an-lbl";
  }

  @Override
  public double getSensitivityRatio() {
    return BeeConst.DOUBLE_ONE;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (EventUtils.isClick(event)) {
      if (!isEnabled() || isAnimationRunning()) {
        return;
      }

      maybeAnimate();
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void setAnimationDuration(int animationDuration) {
    this.animationDuration = animationDuration;
  }

  @Override
  public void setAnimationTimer(Timer animationTimer) {
    this.animationTimer = animationTimer;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  protected String getDefaultStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "AnimatableLabel";
  }

  @Override
  protected void onUnload() {
    stop();
    super.onUnload();
  }
}
