package com.butent.bee.client.dialog;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasAnimation;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.animation.Animation;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Stacking;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.PreviewHandler;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.event.Previewer.PreviewConsumer;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Popup extends Simple implements HasAnimation, CloseEvent.HasCloseHandlers,
    OpenEvent.HasOpenHandlers, PreviewHandler {

  public enum AnimationType {
    CENTER, ONE_WAY_CORNER, ROLL_DOWN
  }

  public enum OutsideClick {
    CLOSE, IGNORE
  }

  public interface PositionCallback {
    void setPosition(int offsetWidth, int offsetHeight);
  }

  private final class MouseHandler implements MouseDownHandler, MouseUpHandler, MouseMoveHandler {
    private int startX;
    private int startY;

    private MouseHandler() {
    }

    @Override
    public void onMouseDown(MouseDownEvent event) {
      if (DOM.getCaptureElement() == null && isCaptionEvent(event.getNativeEvent())) {
        event.preventDefault();
        DOM.setCapture(getElement());

        setDragging(true);

        startX = event.getX();
        startY = event.getY();
      }
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
      if (isDragging()) {
        int x = getAbsoluteLeft() + event.getX() - startX;
        int y = getAbsoluteTop() + event.getY() - startY;

        x = BeeUtils.clamp(x, 0, DomUtils.getClientWidth() - getOffsetWidth());
        y = BeeUtils.clamp(y, 0, DomUtils.getClientHeight() - getOffsetHeight());

        setPopupPosition(x, y);
      }
    }

    @Override
    public void onMouseUp(MouseUpEvent event) {
      if (isDragging()) {
        setDragging(false);
        DOM.releaseCapture(getElement());
      }
    }
  }

  private final class ResizeAnimation extends Animation {

    private final Popup curPanel;

    private boolean show;
    private boolean isUnloading;

    private int offsetHeight = BeeConst.UNDEF;
    private int offsetWidth = BeeConst.UNDEF;

    private Timer showTimer;

    private ResizeAnimation(Popup panel) {
      this.curPanel = panel;
    }

    @Override
    protected void onComplete() {
      if (!show) {
        if (!isUnloading) {
          BodyPanel.get().remove(curPanel);
        }
      }
      StyleUtils.clearClip(curPanel);
      curPanel.getElement().getStyle().clearOverflow();
    }

    @Override
    protected void onStart() {
      offsetHeight = curPanel.getOffsetHeight();
      offsetWidth = curPanel.getOffsetWidth();
      curPanel.getElement().getStyle().setOverflow(Overflow.HIDDEN);
      super.onStart();
    }

    @Override
    protected void onUpdate(double progress) {
      double p = show ? progress : (1.0 - progress);

      int top = 0;
      int left = 0;
      int right = 0;
      int bottom = 0;

      int height = (int) (p * offsetHeight);
      int width = (int) (p * offsetWidth);

      switch (curPanel.getAnimationType()) {
        case ROLL_DOWN:
          right = offsetWidth;
          bottom = height;
          break;

        case CENTER:
          top = (offsetHeight - height) >> 1;
          left = (offsetWidth - width) >> 1;
          right = left + width;
          bottom = top + height;
          break;

        case ONE_WAY_CORNER:
          right = left + width;
          bottom = top + height;
          break;
      }
      StyleUtils.setClip(curPanel, top, right, bottom, left);
    }

    private void onInstantaneousRun() {
      if (show) {
        curPanel.getElement().getStyle().setPosition(Position.ABSOLUTE);
        if (!BeeConst.isUndef(curPanel.getTopPosition())) {
          curPanel.setPopupPosition(curPanel.getLeftPosition(), curPanel.getTopPosition());
        }
        BodyPanel.get().add(curPanel);
      } else {
        if (!isUnloading) {
          BodyPanel.get().remove(curPanel);
        }
      }
      curPanel.getElement().getStyle().clearOverflow();
    }

    private void setState(boolean sh, boolean unl) {
      this.isUnloading = unl;

      cancel();

      if (showTimer != null) {
        showTimer.cancel();
        showTimer = null;
        onComplete();
      }

      curPanel.setShowing(sh);

      boolean animate = !unl && curPanel.isAnimationEnabled();
      if (curPanel.getAnimationType() != AnimationType.CENTER && !sh) {
        animate = false;
      }

      this.show = sh;
      if (animate) {
        if (sh) {
          curPanel.getElement().getStyle().setPosition(Position.ABSOLUTE);
          if (!BeeConst.isUndef(curPanel.getTopPosition())) {
            curPanel.setPopupPosition(curPanel.getLeftPosition(), curPanel.getTopPosition());
          }
          StyleUtils.setClip(curPanel, 0, 0, 0, 0);
          BodyPanel.get().add(curPanel);

          showTimer = new Timer() {
            @Override
            public void run() {
              showTimer = null;
              ResizeAnimation.this.run(ANIMATION_DURATION);
            }
          };
          showTimer.schedule(1);
        } else {
          run(ANIMATION_DURATION);
        }
      } else {
        onInstantaneousRun();
      }
    }
  }

  private static final String STYLE_POPUP = BeeConst.CSS_CLASS_PREFIX + "Popup";

  private static final int ANIMATION_DURATION = 250;

  public static Popup getActivePopup() {
    int widgetCount = BodyPanel.get().getWidgetCount();

    for (int i = widgetCount - 1; i >= 0; i--) {
      Widget child = BodyPanel.get().getWidget(i);
      if (child instanceof Popup && ((Popup) child).isShowing()) {
        return (Popup) child;
      }
    }

    return null;
  }

  public static Collection<Popup> getVisiblePopups() {
    List<Popup> popups = new ArrayList<>();

    for (Widget child : BodyPanel.get()) {
      if (child instanceof Popup && ((Popup) child).isShowing()) {
        popups.add((Popup) child);
      }
    }

    return popups;
  }

  private static int clampLeft(int left, int width) {
    int windowLeft = Window.getScrollLeft();
    int windowRight = Window.getClientWidth() + Window.getScrollLeft();

    return Math.max(Math.min(left, windowRight - width), windowLeft);
  }

  private static int clampTop(int top, int height) {
    int windowTop = Window.getScrollTop();
    int windowBottom = Window.getScrollTop() + Window.getClientHeight();

    return Math.max(Math.min(top, windowBottom - height), windowTop);
  }

  private AnimationType animationType = AnimationType.CENTER;

  private final OutsideClick onOutsideClick;
  private boolean showing;

  private String desiredHeight;
  private String desiredWidth;

  private int leftPosition = BeeConst.UNDEF;
  private int topPosition = BeeConst.UNDEF;

  private boolean hideOnEscape;
  private boolean hideOnSave;

  private PreviewConsumer onSave;
  private PreviewConsumer onEscape;

  private boolean isAnimationEnabled;
  private final ResizeAnimation resizeAnimation = new ResizeAnimation(this);

  private boolean dragging;

  private MouseHandler mouseHandler;

  private Element keyboardPartner;

  public Popup(OutsideClick onOutsideClick) {
    this(onOutsideClick, STYLE_POPUP);
  }

  public Popup(OutsideClick onOutsideClick, String styleName) {
    super();

    this.onOutsideClick = onOutsideClick;

    setPopupPosition(0, 0);
    DomUtils.createId(this, getIdPrefix());

    if (styleName != null) {
      setStyleName(styleName);
    }
  }

  @Override
  public HandlerRegistration addCloseHandler(CloseEvent.Handler handler) {
    return addHandler(handler, CloseEvent.getType());
  }

  @Override
  public HandlerRegistration addOpenHandler(OpenEvent.Handler handler) {
    return addHandler(handler, OpenEvent.getType());
  }

  public void attachAmendDetach(ScheduledCommand command) {
    Assert.notNull(command);
    Assert.state(!isShowing());

    boolean animationEnabled = isAnimationEnabled();
    boolean visible = isVisible();

    setAnimationEnabled(false);
    setVisible(false);
    show();

    command.execute();

    hide(false);
    setAnimationEnabled(animationEnabled);
    setVisible(visible);
  }

  public void cascade() {
    int x = BeeUtils.clamp(Window.getClientWidth() / 30, 5, 30);
    int y = BeeUtils.clamp(Window.getClientHeight() / 30, 5, 30);
    cascade(x, y);
  }

  public void cascade(int x, int y) {
    Popup activePopup = getActivePopup();
    if (activePopup == null) {
      center();
    } else {
      showAt(activePopup.getAbsoluteLeft() + x, activePopup.getAbsoluteTop() + y);
    }
  }

  public void center() {
    boolean initiallyShowing = isShowing();
    boolean initiallyAnimated = isAnimationEnabled();

    if (!initiallyShowing) {
      setVisible(false);
      setAnimationEnabled(false);
      show();
    }

    Style style = getElement().getStyle();
    StyleUtils.setLeft(style, 0);
    StyleUtils.setTop(style, 0);

    int left = (Window.getClientWidth() - getOffsetWidth()) >> 1;
    int top = (Window.getClientHeight() - getOffsetHeight()) >> 1;
    setPopupPosition(Math.max(Window.getScrollLeft() + left, 0),
        Math.max(Window.getScrollTop() + top, 0));

    if (!initiallyShowing) {
      setAnimationEnabled(initiallyAnimated);
      if (initiallyAnimated) {
        StyleUtils.setClip(this, 0, 0, 0, 0);
        setVisible(true);
        resizeAnimation.run(ANIMATION_DURATION);
      } else {
        setVisible(true);
      }
    }

    OpenEvent.fire(this);
  }

  public void close() {
    hide(true);
  }

  public AnimationType getAnimationType() {
    return animationType;
  }

  public Widget getContent() {
    return getWidget();
  }

  @Override
  public String getIdPrefix() {
    return "popup";
  }

  public Element getKeyboardPartner() {
    return keyboardPartner;
  }

  public PreviewConsumer getOnEscape() {
    return onEscape;
  }

  public PreviewConsumer getOnSave() {
    return onSave;
  }

  public int getPopupLeft() {
    return getElement().getAbsoluteLeft();
  }

  public int getPopupTop() {
    return getElement().getAbsoluteTop();
  }

  public boolean hideOnEscape() {
    return hideOnEscape;
  }

  public boolean hideOnSave() {
    return hideOnSave;
  }

  @Override
  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  @Override
  public boolean isModal() {
    return OutsideClick.IGNORE.equals(onOutsideClick);
  }

  public boolean isShowing() {
    return showing;
  }

  @Override
  public boolean isVisible() {
    return !BeeUtils.same(getElement().getStyle().getVisibility(), Visibility.HIDDEN.getCssName());
  }

  @Override
  public void onEventPreview(NativePreviewEvent event, Node targetNode) {
    if (!isShowing() || DOM.getCaptureElement() != null) {
      return;
    }

    NativeEvent nativeEvent = event.getNativeEvent();
    boolean eventTargetsPopup = (targetNode != null) && getElement().isOrHasChild(targetNode);

    String type = nativeEvent.getType();

    if (EventUtils.isMouseDown(type)) {
      if (eventTargetsPopup) {
        event.consume();
      } else if (isModal()) {
        event.cancel();
      } else {
        hide(CloseEvent.Cause.MOUSE_OUTSIDE, targetNode, true);
      }

    } else if (EventUtils.isKeyEvent(type)) {

      if (EventUtils.isKeyDown(type)) {
        if (nativeEvent.getKeyCode() == KeyCodes.KEY_ESCAPE) {
          if (hideOnEscape()) {
            event.cancel();
            hide(CloseEvent.Cause.KEYBOARD_ESCAPE, targetNode, true);
          }
          if (getOnEscape() != null) {
            getOnEscape().accept(event);
          }

        } else if (UiHelper.isSave(nativeEvent)) {
          if (hideOnSave()) {
            event.cancel();
            hide(CloseEvent.Cause.KEYBOARD_SAVE, targetNode, true);
          }
          if (getOnSave() != null) {
            getOnSave().accept(event);
          }

        } else if (nativeEvent.getKeyCode() == KeyCodes.KEY_TAB) {
          if (!eventTargetsPopup
              || Element.is(targetNode) && handleTabulation(Element.as(targetNode))) {
            event.cancel();
            UiHelper.moveFocus(getWidget(), !EventUtils.hasModifierKey(nativeEvent));
          }
        }
      }

      if (!event.isCanceled()) {
        if (eventTargetsPopup || getKeyboardPartner() != null && targetNode != null
            && getKeyboardPartner().isOrHasChild(targetNode)) {
          event.consume();

        } else {
          event.cancel();
        }
      }

    } else if (eventTargetsPopup) {
      event.consume();
    } else {
      event.cancel();
    }
  }

  @Override
  public void onResize() {
    int x = Math.min(getAbsoluteLeft(), DomUtils.getClientWidth() - getOffsetWidth());
    int y = Math.min(getAbsoluteTop(), DomUtils.getClientHeight() - getOffsetHeight());

    setPopupPosition(Math.max(x, 0), Math.max(y, 0));

    super.onResize();
  }

  @Override
  public void setAnimationEnabled(boolean enable) {
    isAnimationEnabled = enable;
  }

  public void setAnimationType(AnimationType animationType) {
    this.animationType = animationType;
  }

  @Override
  public void setHeight(String height) {
    setDesiredHeight(height);
    maybeUpdateSize();
    if (BeeUtils.isEmpty(height)) {
      setDesiredHeight(null);
    }
  }

  public void setHideOnEscape(boolean hideOnEscape) {
    this.hideOnEscape = hideOnEscape;
  }

  public void setHideOnSave(boolean hideOnSave) {
    this.hideOnSave = hideOnSave;
  }

  public void setKeyboardPartner(Element keyboardPartner) {
    this.keyboardPartner = keyboardPartner;
  }

  public void setOnEscape(PreviewConsumer onEscape) {
    this.onEscape = onEscape;
  }

  public void setOnSave(PreviewConsumer onSave) {
    this.onSave = onSave;
  }

  public void setPopupPosition(int left, int top) {
    setLeftPosition(left);
    setTopPosition(top);

    Style style = getElement().getStyle();
    StyleUtils.setLeft(style, left);
    StyleUtils.setTop(style, top);
  }

  public void setPopupPositionAndShow(PositionCallback callback) {
    setVisible(false);
    show();
    callback.setPosition(getOffsetWidth(), getOffsetHeight());
    setVisible(true);

    OpenEvent.fire(this);
  }

  @Override
  public void setVisible(boolean visible) {
    getElement().getStyle().setVisibility(visible ? Visibility.VISIBLE : Visibility.HIDDEN);
  }

  @Override
  public void setWidget(Widget w) {
    super.setWidget(w);
    maybeUpdateSize();
  }

  @Override
  public void setWidth(String width) {
    setDesiredWidth(width);
    maybeUpdateSize();
    if (BeeUtils.isEmpty(width)) {
      setDesiredWidth(null);
    }
  }

  public void showAt(final int x, final int y) {
    setPopupPositionAndShow(new PositionCallback() {
      @Override
      public void setPosition(int offsetWidth, int offsetHeight) {
        int left = clampLeft(x, offsetWidth);
        int top = clampTop(y, offsetHeight);

        setPopupPosition(left, top);
      }
    });
  }

  public void showOnTop(final Element target) {
    if (target == null) {
      center();
    } else {
      setPopupPositionAndShow(new PositionCallback() {
        @Override
        public void setPosition(int offsetWidth, int offsetHeight) {
          int x = target.getAbsoluteLeft();
          int y = target.getAbsoluteTop();

          int left = clampLeft(x, offsetWidth);
          int top = clampTop(y, offsetHeight);

          setPopupPosition(left, top);
        }
      });
    }
  }

  public void showRelativeTo(final Element target) {
    showRelativeTo(target, null);
  }

  public void showRelativeTo(final Element target, final Edges margins) {
    if (target == null) {
      center();
    } else {
      setPopupPositionAndShow(new PositionCallback() {
        @Override
        public void setPosition(int offsetWidth, int offsetHeight) {
          position(target, margins, offsetWidth, offsetHeight);
        }
      });
    }
  }

  protected void enableDragging() {
    if (getMouseHandler() == null) {
      setMouseHandler(new MouseHandler());

      addDomHandler(getMouseHandler(), MouseDownEvent.getType());
      addDomHandler(getMouseHandler(), MouseUpEvent.getType());
      addDomHandler(getMouseHandler(), MouseMoveEvent.getType());
    }
  }

  protected void hide(CloseEvent.Cause cause, Node target, boolean fireEvent) {
    if (!isShowing()) {
      return;
    }
    Stacking.removeContext(this);

    resizeAnimation.setState(false, false);
    if (fireEvent) {
      CloseEvent.fire(this, cause, target);
    }
  }

  /**
   * @param event
   */
  protected boolean isCaptionEvent(NativeEvent event) {
    return false;
  }

  @Override
  protected void onLoad() {
    Previewer.ensureRegistered(this);
    super.onLoad();
  }

  @Override
  protected void onUnload() {
    super.onUnload();

    Previewer.ensureUnregistered(this);
    if (isShowing()) {
      resizeAnimation.setState(false, true);
    }
  }

  private String getDesiredHeight() {
    return desiredHeight;
  }

  private String getDesiredWidth() {
    return desiredWidth;
  }

  private int getLeftPosition() {
    return leftPosition;
  }

  private MouseHandler getMouseHandler() {
    return mouseHandler;
  }

  private int getTopPosition() {
    return topPosition;
  }

  private boolean handleTabulation(Element target) {
    if (target == null) {
      return true;
    }

    Widget content = getContent();
    if (content == null) {
      return true;
    }
    if (content instanceof TabulationHandler && ((TabulationHandler) content).handlesTabulation()) {
      return false;
    }

    Widget w = DomUtils.getChildByElement(content, target);
    for (Widget p = w; p != null && p != content; p = p.getParent()) {
      if (p instanceof TabulationHandler && ((TabulationHandler) p).handlesTabulation()) {
        return false;
      }
    }

    return true;
  }

  private void hide(boolean fireEvent) {
    hide(CloseEvent.Cause.SCRIPT, null, fireEvent);
  }

  private boolean isDragging() {
    return dragging;
  }

  private void maybeUpdateSize() {
    Widget w = super.getWidget();
    if (w != null) {
      if (!BeeUtils.isEmpty(getDesiredHeight())) {
        w.setHeight(getDesiredHeight());
      }
      if (!BeeUtils.isEmpty(getDesiredWidth())) {
        w.setWidth(getDesiredWidth());
      }
    }
  }

  private void position(Element relativeElement, Edges margins, int offsetWidth, int offsetHeight) {
    int left = relativeElement.getAbsoluteLeft();
    int top = relativeElement.getAbsoluteTop();

    int objectWidth = relativeElement.getOffsetWidth();
    int objectHeight = relativeElement.getOffsetHeight();

    if (margins != null) {
      left -= margins.getIntLeft();
      top -= margins.getIntTop();

      objectWidth += margins.getIntRight();
      objectHeight += margins.getIntBottom();
    }

    int offsetWidthDiff = offsetWidth - objectWidth;
    if (offsetWidthDiff > 0) {
      int windowRight = Window.getClientWidth() + Window.getScrollLeft();
      int windowLeft = Window.getScrollLeft();

      int distanceToWindowRight = windowRight - left;
      int distanceFromWindowLeft = left - windowLeft;

      if (distanceToWindowRight < offsetWidth && distanceFromWindowLeft >= offsetWidthDiff) {
        left -= offsetWidthDiff;
      }
    } else if (offsetWidthDiff < 0) {
      left -= offsetWidthDiff;
    }

    int windowTop = Window.getScrollTop();
    int windowBottom = Window.getScrollTop() + Window.getClientHeight();

    int distanceFromWindowTop = top - windowTop;
    int distanceToWindowBottom = windowBottom - (top + objectHeight);

    if (distanceToWindowBottom < offsetHeight && distanceFromWindowTop >= offsetHeight) {
      top -= offsetHeight;
    } else {
      top += objectHeight;
    }

    left = clampLeft(left, offsetWidth);
    top = clampTop(top, offsetHeight);

    setPopupPosition(left, top);
  }

  private void setDesiredHeight(String desiredHeight) {
    this.desiredHeight = desiredHeight;
  }

  private void setDesiredWidth(String desiredWidth) {
    this.desiredWidth = desiredWidth;
  }

  private void setDragging(boolean dragging) {
    this.dragging = dragging;
  }

  private void setLeftPosition(int leftPosition) {
    this.leftPosition = leftPosition;
  }

  private void setMouseHandler(MouseHandler mouseHandler) {
    this.mouseHandler = mouseHandler;
  }

  private void setShowing(boolean showing) {
    this.showing = showing;
  }

  private void setTopPosition(int topPosition) {
    this.topPosition = topPosition;
  }

  private void show() {
    if (isShowing()) {
      return;
    }
    if (isAttached()) {
      this.removeFromParent();
    }

    Stacking.addContext(this);
    resizeAnimation.setState(true, false);
  }
}
