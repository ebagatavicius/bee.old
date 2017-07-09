package com.butent.bee.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FocusWidget;

import com.butent.bee.client.animation.HasAnimatableActivity;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.HasCommand;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;

/**
 * Implements a push button user interface component.
 */

public class Button extends FocusWidget implements IdentifiableWidget, HasCommand, HasHtml,
    EnablableWidget, HasAnimatableActivity {

  private Scheduler.ScheduledCommand command;

  private Timer animationTimer;
  private int animationDuration;

  public Button() {
    super(Document.get().createPushButtonElement());
    init();
  }

  public Button(String html) {
    this();
    setHtml(html);
  }

  public Button(String html, ClickHandler handler) {
    this(html);
    addClickHandler(handler);
  }

  public Button(String html, Scheduler.ScheduledCommand command) {
    this(html);
    setCommand(command);
  }

  public void click() {
    ButtonElement.as(getElement()).click();
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
  public Scheduler.ScheduledCommand getCommand() {
    return command;
  }

  @Override
  public String getHtml() {
    return getElement().getInnerHTML();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "b";
  }

  @Override
  public double getSensitivityRatio() {
    return BeeConst.DOUBLE_UNDEF;
  }

  @Override
  public String getText() {
    return getElement().getInnerText();
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (EventUtils.isClick(event)) {
      if (!isEnabled() || isAnimationRunning()) {
        return;
      }

      maybeAnimate();

      if (getCommand() != null) {
        getCommand().execute();
      }
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
  public void setCommand(Scheduler.ScheduledCommand command) {
    this.command = command;
    if (command != null) {
      initEvents();
    }
  }

  @Override
  public void setHtml(String html) {
    getElement().setInnerHTML(html);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setText(String text) {
    getElement().setInnerText(text);
  }

  @Override
  protected void onUnload() {
    stop();
    super.onUnload();
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "Button");
  }

  private void initEvents() {
    sinkEvents(Event.ONCLICK);
  }
}
