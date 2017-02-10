package com.butent.bee.client.animation;

import com.butent.bee.client.dom.Features;
import com.butent.bee.shared.utils.BeeUtils;

import elemental.dom.RequestAnimationFrameCallback;

public abstract class RafCallback implements RequestAnimationFrameCallback {

//@formatter:off
//CHECKSTYLE:OFF
  private static native int request(RequestAnimationFrameCallback callback) /*-{
    return $wnd.requestAnimationFrame($entry(callback.@elemental.dom.RequestAnimationFrameCallback::onRequestAnimationFrameCallback(D)).bind(callback));
  }-*/;
//CHECKSTYLE:ON
//@formatter:on

  private double duration;

  private double startTime;

  public RafCallback(double duration) {
    this.duration = duration;
  }

  protected RafCallback() {
  }

  public double getDuration() {
    return duration;
  }

  public double getStartTime() {
    return startTime;
  }

  public double normalize(double elapsed) {
    return BeeUtils.normalize(elapsed, 0, getDuration());
  }

  @Override
  public boolean onRequestAnimationFrameCallback(double time) {
    double elapsed = time - startTime;

    if (elapsed <= duration && run(elapsed)) {
      request(this);
    } else {
      onComplete();
    }
    return true;
  }

  public void setDuration(double duration) {
    this.duration = duration;
  }

  public void start() {
    if (Features.supportsRequestAnimationFrame()) {
      request(time -> {
        RafCallback.this.setStartTime(time);
        request(RafCallback.this);
        return false;
      });

    } else {
      onComplete();
    }
  }

  protected abstract void onComplete();

  protected abstract boolean run(double elapsed);

  private void setStartTime(double startTime) {
    this.startTime = startTime;
  }
}
