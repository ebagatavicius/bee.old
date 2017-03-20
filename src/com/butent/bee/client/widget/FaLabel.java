package com.butent.bee.client.widget;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;

import com.butent.bee.client.animation.Animatable;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.utils.BeeUtils;

public class FaLabel extends Label implements EnablableWidget, Animatable {

  public static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "fa-label";
  private static final String STYLE_DISABLED = STYLE_NAME + "-" + StyleUtils.SUFFIX_DISABLED;

  private boolean enabled = true;

  private Timer animationTimer;

  public FaLabel(FontAwesome fa) {
    super();
    setChar(fa);
  }

  public FaLabel(FontAwesome fa, boolean inline) {
    super(inline);
    setChar(fa);
  }

  public FaLabel(FontAwesome fa, String styleName) {
    this(fa);
    if (!BeeUtils.isEmpty(styleName)) {
      addStyleName(styleName);
    }
  }

  public void click() {
    if (isEnabled()) {
      EventUtils.click(this);
    }
  }

  @Override
  public void enableAnimation() {
    StyleUtils.animateHover(this);
    StyleUtils.animateActive(this);
  }

  @Override
  public String getIdPrefix() {
    return "fa";
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (EventUtils.isClick(event)) {
      if (!isEnabled()) {
        return;
      }

      if (getElement().hasClassName(StyleUtils.NAME_ANIMATE_ACTIVE)) {
        StyleUtils.restartAnimation(getElement(), StyleUtils.NAME_ACTIVE);
        scheduleAnimationEnd(1_000);
      }
    }

    super.onBrowserEvent(event);
  }

  public void setChar(FontAwesome fa) {
    if (fa == null) {
      clear();
    } else {
      setText(String.valueOf(fa.getCode()));
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;

      setStyleName(STYLE_DISABLED, !enabled);
    }
  }

  @Override
  protected String getDefaultStyleName() {
    return STYLE_NAME;
  }

  @Override
  protected void init() {
    super.init();

    StyleUtils.setFontFamily(this, FontAwesome.FAMILY);
    DomUtils.preventSelection(this);
  }

  private void scheduleAnimationEnd(int millis) {
    if (animationTimer == null) {
      animationTimer = new Timer() {
        @Override
        public void run() {
          getElement().removeClassName(StyleUtils.NAME_ACTIVE);
        }
      };
    }

    animationTimer.schedule(millis);
  }
}
