package com.butent.bee.client.layout;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.layout.Layout.AnimationCallback;
import com.butent.bee.client.layout.Layout.Layer;

public class LayoutCommand implements ScheduledCommand {

  private boolean scheduled;
  private boolean canceled;

  private int duration;
  private Layout.AnimationCallback callback;

  private final Layout layout;

  public LayoutCommand(Layout layout) {
    this.layout = layout;
  }

  public void cancel() {
    canceled = true;
  }

  @Override
  public final void execute() {
    scheduled = false;
    if (canceled) {
      return;
    }

    layout.layout(duration, new Layout.AnimationCallback() {
      @Override
      public void onAnimationComplete() {
        if (callback != null) {
          callback.onAnimationComplete();
        }
      }

      @Override
      public void onLayout(Layer layer, double progress) {
        Widget child = (Widget) layer.getUserObject();
        if (child instanceof RequiresResize) {
          ((RequiresResize) child).onResize();
        }

        if (callback != null) {
          callback.onLayout(layer, progress);
        }
      }
    });
  }

  public void schedule(int dur, AnimationCallback acb) {
    this.duration = dur;
    this.callback = acb;

    canceled = false;
    if (!scheduled) {
      scheduled = true;
      Scheduler.get().scheduleFinally(this);
    }
  }
}
