package com.butent.bee.client.dialog;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.Composite;

import com.butent.bee.client.animation.Animation;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.PreviewHandler;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Enables using popup notifications with different levels of information (warnings, info messages,
 * errors etc).
 */

public class Notification extends Composite implements PreviewHandler, IdentifiableWidget {

  /**
   * Manages notification message and it's level and text.
   */

  private final class Message {
    private final LogLevel level;
    private final List<String> lines;

    private Message(LogLevel level, List<String> lines) {
      this.level = level;
      this.lines = lines;
    }

    private Message(LogLevel level, String... lines) {
      this(level, Lists.newArrayList(lines));
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Message) {
        return Objects.equals(level, ((Message) obj).level)
            && Objects.equals(lines, ((Message) obj).lines);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((level == null) ? 0 : level.hashCode());
      result = prime * result + ((lines == null) ? 0 : lines.hashCode());
      return result;
    }

    private LogLevel getLevel() {
      return level;
    }

    private String getLine(int index) {
      return getLines().get(index);
    }

    private int getLineCount() {
      return getLines().size();
    }

    private List<String> getLines() {
      return lines;
    }

    private String getStyleName() {
      if (getLevel() == null) {
        return STYLE_DEFAULT;
      } else {
        return BeeConst.CSS_CLASS_PREFIX + "Notification-" + getLevel().name().toLowerCase();
      }
    }
  }

  /**
   * Enables size related animations for notification messages.
   */

  private class MoleAnimation extends Animation {
    private int startSize;
    private int endSize;
    private State endState;

    @Override
    protected void onComplete() {
      if (endSize > 0) {
        StyleUtils.setHeight(getElement(), endSize, CssUnit.PX);
      }
      setState(endState);
    }

    @Override
    protected void onUpdate(double progress) {
      double delta = (endSize - startSize) * progress;
      double newSize = startSize + delta;
      StyleUtils.setHeight(getElement(), newSize, CssUnit.PX);
    }

    private void animate(int start, int end, int duration, State st) {
      this.startSize = start;
      this.endSize = end;
      this.endState = st;

      if (duration > 0) {
        run(duration);
      } else {
        onComplete();
      }
    }
  }

  /**
   * Contains a list of possible notification states.
   */
  private enum State {
    PENDING, OPENING, SHOWING, CLOSING
  }

  public static final int DEFAULT_OPEN_DURATION = 500;
  public static final int DEFAULT_CLOSE_DURATION = 500;

  private static final String STYLE_CONTAINER = BeeConst.CSS_CLASS_PREFIX
      + "NotificationContainer";
  private static final String STYLE_MESSAGES = BeeConst.CSS_CLASS_PREFIX
      + "NotificationMessages";
  private static final String STYLE_TEXT = BeeConst.CSS_CLASS_PREFIX + "NotificationText";

  private static final String STYLE_DEFAULT = BeeConst.CSS_CLASS_PREFIX + "Notification-info";

  private final DivElement messageContainer = Document.get().createDivElement();

  private final MoleAnimation animation = new MoleAnimation();

  private int openDuration = DEFAULT_OPEN_DURATION;
  private int closeDuration = DEFAULT_CLOSE_DURATION;

  private State state = State.PENDING;

  private final LinkedList<Message> pendingMessages = new LinkedList<>();

  public Notification() {
    messageContainer.setId(DomUtils.createUniqueId("note-messages"));
    messageContainer.appendChild(createTextElement());

    DivElement container = Document.get().createDivElement();
    container.setId(DomUtils.createUniqueId(getIdPrefix()));

    container.setClassName(STYLE_CONTAINER);
    container.getStyle().setDisplay(Display.NONE);

    container.appendChild(messageContainer);

    Label html = new Label(container);
    initWidget(html);
  }

  public void clear() {
    getPendingMessages().clear();
    hide();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "notes";
  }

  public void hide() {
    switch (getState()) {
      case PENDING:
        return;
      case SHOWING:
        setState(State.CLOSING);
        getAnimation().animate(getMessageContainer().getOffsetHeight(), 0, getCloseDuration(),
            State.PENDING);
        break;
      case OPENING:
      case CLOSING:
        getAnimation().cancel();
        hideDisplay();
        setState(State.PENDING);
        break;
      default:
        Assert.untouchable();
    }
  }

  public void info(String... lines) {
    show(LogLevel.INFO, lines);
  }

  public boolean isActive() {
    return getState() == State.OPENING || getState() == State.SHOWING;
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  public void onEventPreview(NativePreviewEvent event, Node targetNode) {
    Assert.notNull(event);
    String type = event.getNativeEvent().getType();

    if (EventUtils.isKeyDown(type) || EventUtils.isMouseDown(type)) {
      closePreview();
      hide();
    }
  }

  public void setCloseDuration(int duration) {
    this.closeDuration = duration;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setOpenDuration(int duration) {
    this.openDuration = duration;
  }

  public void severe(String... lines) {
    show(LogLevel.ERROR, lines);
  }

  public void show(LogLevel level, String... lines) {
    Assert.notNull(level);
    Assert.notNull(lines);
    Assert.parameterCount(lines.length, 1);

    Message message = new Message(level, lines);

    if (State.PENDING.equals(getState())) {
      setMessage(message);
      openDisplay();
    } else if (!getPendingMessages().contains(message)) {
      getPendingMessages().add(message);
    }
  }

  public void warning(String... lines) {
    show(LogLevel.WARNING, lines);
  }

  @Override
  protected void onUnload() {
    closePreview();
    super.onUnload();
  }

  private void closePreview() {
    Previewer.ensureUnregistered(this);
  }

  private static Element createTextElement() {
    DivElement element = Document.get().createDivElement();
    element.setId(DomUtils.createUniqueId("note-text"));
    element.setClassName(STYLE_TEXT);
    return element;
  }

  private MoleAnimation getAnimation() {
    return animation;
  }

  private int getCloseDuration() {
    return closeDuration;
  }

  private DivElement getMessageContainer() {
    return messageContainer;
  }

  private int getOpenDuration() {
    return openDuration;
  }

  private LinkedList<Message> getPendingMessages() {
    return pendingMessages;
  }

  private State getState() {
    return state;
  }

  private void hideDisplay() {
    getElement().getStyle().setDisplay(Display.NONE);
  }

  private void openDisplay() {
    setState(State.OPENING);
    showDisplay();
    getAnimation().animate(0, getMessageContainer().getOffsetHeight(), getOpenDuration(),
        State.SHOWING);
  }

  private void setMessage(Message message) {
    Assert.notNull(message);
    int oldCount = getMessageContainer().getChildCount();
    int pendingCount = getPendingMessages().size();
    int newCount = message.getLineCount();
    if (pendingCount > 0) {
      newCount++;
    }

    if (newCount < oldCount) {
      if (oldCount > 1) {
        for (int i = Math.max(newCount, 1); i < oldCount; i++) {
          getMessageContainer().removeChild(getMessageContainer().getLastChild());
        }
      }
      if (newCount <= 0) {
        Element.as(getMessageContainer().getChild(0)).setInnerHTML(BeeConst.STRING_EMPTY);
      }

    } else if (newCount > oldCount) {
      for (int i = oldCount; i < newCount; i++) {
        getMessageContainer().appendChild(createTextElement());
      }
    }

    if (newCount > 0) {
      for (int i = 0; i < message.getLineCount(); i++) {
        Element.as(getMessageContainer().getChild(i)).setInnerHTML(message.getLine(i));
      }
      if (pendingCount > 0) {
        String msg = BeeUtils.joinWords(BeeUtils.bracket(pendingCount), "pending...");
        Element.as(getMessageContainer().getLastChild()).setInnerHTML(msg);
      }

      getMessageContainer().setClassName(
          StyleUtils.buildClasses(STYLE_MESSAGES, message.getStyleName()));
    }
  }

  private void setState(State state) {
    Assert.notNull(state, "notification state cannot be null");
    this.state = state;

    switch (state) {
      case PENDING:
        hideDisplay();
        Message message = getPendingMessages().poll();
        if (message != null) {
          setMessage(message);
          openDisplay();
        }
        break;

      case SHOWING:
        Previewer.ensureRegistered(this);
        break;

      case CLOSING:
        closePreview();
        break;

      default:
    }
  }

  private void showDisplay() {
    Style style = getElement().getStyle();
    Element parent = getElement().getParentElement();

    if (parent != null) {
      if (parent.getScrollTop() > 0) {
        StyleUtils.setTop(style, parent.getScrollTop());
      } else if (!BeeUtils.isEmpty(style.getTop())) {
        style.clearTop();
      }

      if (parent.getScrollLeft() > 0) {
        StyleUtils.setRight(style, -parent.getScrollLeft());
      } else if (!BeeUtils.isEmpty(style.getRight())) {
        style.clearRight();
      }
    }

    style.setDisplay(Display.BLOCK);
  }
}
