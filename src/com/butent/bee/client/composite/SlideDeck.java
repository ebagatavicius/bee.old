package com.butent.bee.client.composite;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.animation.AnimationState;
import com.butent.bee.client.animation.RafCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.PreviewHandler;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.CustomComplex;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputRange;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Progress;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.css.CssProperties;
import com.butent.bee.shared.css.values.BackgroundSize;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.List;

public final class SlideDeck extends CustomComplex implements PreviewHandler {

  private final class Animation extends RafCallback {

    private int prevPosition;
    private int nextPosition;

    private Style prevStyle;
    private Style nextStyle;

    private Size size;

    private int cycleDuration;
    private double cycleValue;

    private AnimationState state;
    private boolean transition;

    private double cut;
    private Double pausedAt;

    private Animation() {
      super(Integer.MAX_VALUE);
      setState(AnimationState.NOT_STARTED);
    }

    @Override
    public void start() {
      updateActiveWrapper(getPosition());

      setPrevPosition(getPosition());
      setNextPosition(BeeConst.UNDEF);

      setPrevStyle(getActiveWrapper().getElement().getStyle());
      setNextStyle(getPassiveWrapper().getElement().getStyle());

      setSize(new Size(canvas.getOffsetWidth(), canvas.getOffsetHeight()));

      updateCycleDuration();

      setTransition(false);
      setState(AnimationState.RUNNING);

      super.start();
    }

    @Override
    protected void onComplete() {
    }

    @Override
    protected boolean run(double elapsed) {
      if (isPaused()) {
        if (getPausedAt() == null) {
          logger.debug("paused", BeeUtils.round(elapsed));
          setPausedAt(elapsed);
        }
        return true;

      } else if (isResumed()) {
        if (getPausedAt() != null) {
          setCut(getCut() + elapsed - getPausedAt());
          setPausedAt(null);
          logger.debug("resumed", BeeUtils.round(elapsed), BeeUtils.round(getCut()));
        }
        setState(AnimationState.RUNNING);

      } else if (!isRunning()) {
        return false;
      }

      setCycleValue((elapsed - getCut()) % getCycleDuration() / getCycleDuration());
      updateProgressControl(getCycleValue() * (slides.size() - 1));

      double stepValue = getStepValue();
      if (stepValue > stepTransitionStartValue()) {
        if (!isTransition()) {
          setTransition(true);

          setNextPosition(BeeUtils.rotateForwardExclusive(getPrevPosition(), 0, slides.size()));
          updatePassiveWrapper(getNextPosition());

          getEffect().start(getPrevStyle(), getNextStyle(), getSize());
        }

        applyTransition(getEffect(), stepValue);

      } else if (isTransition()) {
        setTransition(false);
        getEffect().end(getPrevStyle(), getNextStyle(), getSize());

        setPrevPosition(getNextPosition());
        setNextPosition(BeeConst.UNDEF);

        setActiveWrapperIndex(getPassiveWrapperIndex());

        setPrevStyle(getActiveWrapper().getElement().getStyle());
        setNextStyle(getPassiveWrapper().getElement().getStyle());
      }

      return true;
    }

    private void applyTransition(Effect eff, double stepValue) {
      double startValue = stepTransitionStartValue();
      double transitionValue = (stepValue - startValue) / (1 - startValue);
      eff.apply(getPrevStyle(), getNextStyle(), getSize(), transitionValue);
    }

    private void cancel() {
      setState(AnimationState.CANCELED);
      if (getPrevStyle() != null) {
        getEffect().reset(getPrevStyle(), getNextStyle());
      }
    }

    private int getCycleDuration() {
      return cycleDuration;
    }

    private double getCycleValue() {
      return cycleValue;
    }

    private double getCut() {
      return cut;
    }

    private int getNextPosition() {
      return nextPosition;
    }

    private Style getNextStyle() {
      return nextStyle;
    }

    private Double getPausedAt() {
      return pausedAt;
    }

    private int getPrevPosition() {
      return prevPosition;
    }

    private Style getPrevStyle() {
      return prevStyle;
    }

    private Size getSize() {
      return size;
    }

    private AnimationState getState() {
      return state;
    }

    private double getStepValue() {
      return getCycleValue() * slides.size() % 1;
    }

    private boolean isPaused() {
      return getState() == AnimationState.PAUSED;
    }

    private boolean isResumed() {
      return getState() == AnimationState.RESUMED;
    }

    private boolean isRunning() {
      return getState() == AnimationState.RUNNING;
    }

    private boolean isTransition() {
      return transition;
    }

    private void onEffectChanged(Effect oldEff, Effect newEff) {
      if (isRunning() || isPaused()) {
        oldEff.reset(getPrevStyle(), getNextStyle());
        if (isTransition()) {
          applyTransition(newEff, getStepValue());
        }
      }
    }

    private void resume() {
      setState(AnimationState.RESUMED);
    }

    private void setCycleValue(double cycleValue) {
      this.cycleValue = cycleValue;
    }

    private void setCut(double cut) {
      this.cut = cut;
    }

    private void setNextPosition(int nextPosition) {
      this.nextPosition = nextPosition;
    }

    private void setNextStyle(Style nextStyle) {
      this.nextStyle = nextStyle;
    }

    private void setPausedAt(Double pausedAt) {
      this.pausedAt = pausedAt;
    }

    private void setPrevPosition(int prevPosition) {
      this.prevPosition = prevPosition;
    }

    private void setPrevStyle(Style prevStyle) {
      this.prevStyle = prevStyle;
    }

    private void setSize(Size size) {
      this.size = size;
    }

    private void setState(AnimationState state) {
      this.state = state;
    }

    private void setTransition(boolean transition) {
      this.transition = transition;
    }

    private double stepTransitionStartValue() {
      return (double) getIdleMillis() / (getIdleMillis() + getTransitionMillis());
    }

    private void suspend() {
      setState(AnimationState.PAUSED);
    }

    private void updateCycleDuration() {
      this.cycleDuration = (getIdleMillis() + getTransitionMillis()) * slides.size();
    }
  }

  private enum Effect {
    FADE {
      @Override
      public void apply(Style prev, Style next, Size size, double value) {
        prev.setOpacity(1 - value);
        next.setOpacity(value);
      }

      @Override
      public void reset(Style prev, Style next) {
        prev.clearOpacity();
        next.clearOpacity();
      }
    },
    SLIDE {
      @Override
      public void apply(Style prev, Style next, Size size, double value) {
        prev.setLeft(-value * size.getWidth(), Unit.PX);
        next.setLeft((1 - value) * size.getWidth(), Unit.PX);
      }

      @Override
      public void reset(Style prev, Style next) {
        prev.setLeft(0, Unit.PX);
        next.setLeft(0, Unit.PX);
      }
    },
    FLIP {
      @Override
      public void apply(Style prev, Style next, Size size, double value) {
        prev.setTop(value * size.getHeight(), Unit.PX);
        next.setTop((value - 1) * size.getHeight(), Unit.PX);
      }

      @Override
      public void reset(Style prev, Style next) {
        prev.setTop(0, Unit.PX);
        next.setTop(0, Unit.PX);
      }
    };

    public abstract void apply(Style prev, Style next, Size size, double value);

    public abstract void reset(Style prev, Style next);

    private void end(Style prev, Style next, Size size) {
      apply(prev, next, size, BeeConst.DOUBLE_ONE);
    }

    private void start(Style prev, Style next, Size size) {
      apply(prev, next, size, BeeConst.DOUBLE_ZERO);
    }
  }

  private static final class Slide {
    private final String source;

    private final int naturalWidth;
    private final int naturalHeight;

    private boolean contain;

    private Slide(String source, int naturalWidth, int naturalHeight) {
      this.source = source;

      this.naturalWidth = naturalWidth;
      this.naturalHeight = naturalHeight;
    }

    private void embed(Widget display) {
      StyleUtils.setBackgroundImage(display, source);
      StyleUtils.setProperty(display, CssProperties.BACKGROUND_SIZE,
          contain ? BackgroundSize.CONTAIN : BackgroundSize.AUTO);
    }

    private void setContain(boolean contain) {
      this.contain = contain;
    }
  }

  private static BeeLogger logger = LogUtils.getLogger(SlideDeck.class);

  private static final int MIN_WIDTH = 200;
  private static final int MIN_HEIGHT = 200;

  private static final int TN_HEIGHT = 70;
  private static final int CONTROLS_HEIGHT = 30;

  private static final int TN_HEIGHT_MARGIN = DomUtils.getScrollBarHeight() + 3;

  private static final double MAX_WIDTH_FACTOR = 0.9;
  private static final double MAX_HEIGHT_FACTOR = 0.9;

  private static final int IDLE_MILLIS_MIN = 100;
  private static final int IDLE_MILLIS_STEP = 100;
  private static final int IDLE_MILLIS_MAX = 10000;

  private static final int IDLE_MILLIS_DEFAULT = 3000;
  private static final String IDLE_MILLIS_KEY = "SlideDeckIdleMillis";

  private static final int TRANSITION_MILLIS_MIN = 100;
  private static final int TRANSITION_MILLIS_STEP = 100;
  private static final int TRANSITION_MILLIS_MAX = 10000;

  private static final int TRANSITION_MILLIS_DEFAULT = 1000;
  private static final String TRANSITION_MILLIS_KEY = "SlideDeckTransitionMillis";

  private static final String EFFECT_KEY = "SlideDeckEffect";

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "SlideDeck-";

  private static final String STYLE_TN_ACTIVE = STYLE_PREFIX + "tn-active";
  private static final String STYLE_NAV_DISABLED = STYLE_PREFIX + "nav-disabled";

  private static final String STYLE_PLAYING = STYLE_PREFIX + "playing";
  private static final String STYLE_NOT_PLAYING = STYLE_PREFIX + "not-playing";
  private static final String STYLE_PAUSED = STYLE_PREFIX + "paused";

  private static final String STYLE_REPEAT = STYLE_PREFIX + "repeat";
  private static final String STYLE_REPEAT_ON = STYLE_REPEAT + "-on";
  private static final String STYLE_REPEAT_OFF = STYLE_REPEAT + "-off";

  private static final String STYLE_SETTINGS_PREFIX = STYLE_PREFIX + "settings-";
  private static final String STYLE_SETTINGS_HIDDEN = STYLE_SETTINGS_PREFIX + "hidden";
  private static final String STYLE_SETTINGS_LABEL = STYLE_SETTINGS_PREFIX + "label";

  public static void create(List<String> sources, Callback<SlideDeck> callback) {
    int maxWidth = BeeUtils.round(Window.getClientWidth() * MAX_WIDTH_FACTOR);
    int maxHeight = Math.min(BeeUtils.round(Window.getClientHeight() * MAX_HEIGHT_FACTOR),
        Window.getClientHeight() - DialogBox.HEADER_HEIGHT);

    if (maxWidth < MIN_WIDTH || maxHeight < MIN_HEIGHT) {
      callback.onFailure("browser window is suffering from being too small");
    } else {
      create(sources, new Size(maxWidth, maxHeight), callback);
    }
  }

  public static void create(final List<String> sources, final Size maxSize,
      final Callback<SlideDeck> callback) {

    Assert.notEmpty(sources);
    Assert.notNull(callback);

    getNaturalSizes(sources, new Consumer<Size[]>() {
      @Override
      public void accept(Size[] input) {
        List<Slide> list = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
          list.add(new Slide(sources.get(i), input[i].getWidth(), input[i].getHeight()));
        }

        SlideDeck slideDeck = new SlideDeck(list, maxSize);
        callback.onSuccess(slideDeck);
      }
    });
  }

  private static void getNaturalSizes(List<String> sources, final Consumer<Size[]> callback) {
    final Size[] sizes = new Size[sources.size()];
    final Holder<Integer> latch = Holder.of(0);

    for (int i = 0; i < sources.size(); i++) {
      final int index = i;

      Rulers.getImageNaturalSize(sources.get(i), new Consumer<Size>() {
        @Override
        public void accept(Size input) {
          sizes[index] = input;

          latch.set(latch.get() + 1);
          if (latch.get() >= sizes.length) {
            callback.accept(sizes);
          }
        }
      });
    }
  }

  private final List<Slide> slides;

  private final Flow thumbnails;
  private final Flow canvas;
  private final Flow controls;
  private Flow settingsPanel;

  private final Widget[] wrappers = new Widget[2];
  private int activeWrapperIndex;

  private String playId;
  private String progressId;
  private String backwardId;
  private String labelId;
  private String forwardId;

  private int position = BeeConst.UNDEF;
  private boolean repeat;

  private final Animation animation;

  private Effect effect;

  private int idleMillis;
  private int transitionMillis;

  private SlideDeck(List<Slide> slides, Size maxSize) {
    super(Document.get().createDivElement(), STYLE_PREFIX + "container");
    this.slides = slides;

    Integer eff = BeeKeeper.getStorage().getInteger(EFFECT_KEY);
    if (!EnumUtils.isOrdinal(Effect.class, eff)) {
      eff = 0;
    }
    this.effect = Effect.values()[eff];

    Integer millis = BeeKeeper.getStorage().getInteger(IDLE_MILLIS_KEY);
    this.idleMillis = BeeUtils.isPositive(millis) ? millis : IDLE_MILLIS_DEFAULT;

    millis = BeeKeeper.getStorage().getInteger(TRANSITION_MILLIS_KEY);
    this.transitionMillis = BeeUtils.isPositive(millis) ? millis : TRANSITION_MILLIS_DEFAULT;

    this.animation = new Animation();

    this.thumbnails = new Flow(STYLE_PREFIX + "thumbnails");
    add(thumbnails);

    this.canvas = new Flow(STYLE_PREFIX + "canvas");

    for (int i = 0; i < wrappers.length; i++) {
      wrappers[i] = new CustomDiv(STYLE_PREFIX + "wrapper");
      canvas.add(wrappers[i]);
    }
    add(canvas);

    this.controls = new Flow(STYLE_PREFIX + "controls");
    add(controls);

    renderThumbnails();
    renderControls();

    doSizing(maxSize);

    setActiveWrapperIndex(0);
    activateSlide(0);

    updateStyle();
  }

  @Override
  public String getIdPrefix() {
    return "slide-deck";
  }

  public int getWidth() {
    return StyleUtils.getWidth(this);
  }

  public void handleKeyboard() {
    Previewer.ensureRegistered(this);
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  public void onEventPreview(NativePreviewEvent event, Node targetNode) {
    if (EventUtils.isKeyDown(event.getNativeEvent().getType())) {
      int keyCode = event.getNativeEvent().getKeyCode();

      switch (keyCode) {
        case KeyCodes.KEY_LEFT:
        case KeyCodes.KEY_UP:
          activateSlide(BeeUtils.rotateBackwardExclusive(getPosition(), 0, slides.size()));
          break;

        case KeyCodes.KEY_RIGHT:
        case KeyCodes.KEY_DOWN:
          activateSlide(BeeUtils.rotateForwardExclusive(getPosition(), 0, slides.size()));
          break;

        case KeyCodes.KEY_HOME:
        case KeyCodes.KEY_PAGEUP:
          activateSlide(0);
          break;

        case KeyCodes.KEY_END:
        case KeyCodes.KEY_PAGEDOWN:
          activateSlide(slides.size() - 1);
          break;

        case KeyCodes.KEY_ENTER:
        case BeeConst.CHAR_SPACE:
          togglePlay();
          break;
      }
    }
  }

  public boolean repeat() {
    return repeat;
  }

  public void setRepeat(boolean repeat) {
    this.repeat = repeat;
  }

  @Override
  protected void onUnload() {
    super.onUnload();

    Previewer.ensureUnregistered(this);
    if (animation.isRunning()) {
      animation.cancel();
    }
  }

  private void activateSlide(int index) {
    if (animation.isRunning() || animation.isResumed() || animation.isPaused()) {
      animation.cancel();
      updateStyle();
      updatePlayIcon();
    }

    if (getPosition() != index && BeeUtils.isIndex(slides, index)) {
      if (BeeUtils.isIndex(slides, getPosition())) {
        getThumbnail(getPosition()).removeStyleName(STYLE_TN_ACTIVE);
      }

      if (BeeUtils.isIndex(slides, index)) {
        setPosition(index);

        updateCanvas(index);
        Widget thumbnail = getThumbnail(index);
        if (thumbnail != null) {
          thumbnail.addStyleName(STYLE_TN_ACTIVE);
        }

        updateProgressControl(index);
        updateLabel(index + 1);
        updateNavigation(index);
      }
    }
  }

  private void doSizing(Size maxSize) {
    int canvasWidth = MIN_WIDTH;
    int canvasHeight = MIN_HEIGHT;

    for (Slide slide : slides) {
      canvasWidth = Math.max(canvasWidth, slide.naturalWidth);
      canvasHeight = Math.max(canvasHeight, slide.naturalHeight);
    }

    if (maxSize != null) {
      if (maxSize.getWidth() > 0) {
        canvasWidth = Math.min(canvasWidth, maxSize.getWidth());
      }

      if (maxSize.getHeight() > TN_HEIGHT + CONTROLS_HEIGHT) {
        canvasHeight = Math.min(canvasHeight, maxSize.getHeight() - TN_HEIGHT - CONTROLS_HEIGHT);
      }
    }

    for (Slide slide : slides) {
      slide.setContain(slide.naturalWidth > canvasWidth || slide.naturalHeight > canvasHeight);
    }

    StyleUtils.setWidth(this, canvasWidth);

    StyleUtils.setSize(thumbnails, canvasWidth, TN_HEIGHT);
    StyleUtils.setSize(canvas, canvasWidth, canvasHeight);
    StyleUtils.setSize(controls, canvasWidth, CONTROLS_HEIGHT);

    int tnWidth = 0;
    int maxHeight = TN_HEIGHT - TN_HEIGHT_MARGIN;

    for (int i = 0; i < slides.size(); i++) {
      int width = slides.get(i).naturalWidth;
      int height = slides.get(i).naturalHeight;

      if (height > maxHeight) {
        width = width * maxHeight / height + 1;
        height = maxHeight;
      }

      Widget thumbnail = getThumbnail(i);
      if (thumbnail != null) {
        StyleUtils.setSize(thumbnail, width, height);
      }

      tnWidth += width;
    }

    String styleSuffix = (tnWidth > canvasWidth) ? "over" : "under";
    thumbnails.addStyleName(STYLE_PREFIX + "thumbnails-" + styleSuffix);

    for (Widget wrapper : wrappers) {
      StyleUtils.setSize(wrapper, canvasWidth, canvasHeight);
    }
  }

  private Widget getActiveWrapper() {
    return wrappers[getActiveWrapperIndex()];
  }

  private int getActiveWrapperIndex() {
    return activeWrapperIndex;
  }

  private String getBackwardId() {
    return backwardId;
  }

  private Effect getEffect() {
    return effect;
  }

  private String getForwardId() {
    return forwardId;
  }

  private int getIdleMillis() {
    return idleMillis;
  }

  private String getLabelId() {
    return labelId;
  }

  private Widget getPassiveWrapper() {
    return wrappers[getPassiveWrapperIndex()];
  }

  private int getPassiveWrapperIndex() {
    return 1 - getActiveWrapperIndex();
  }

  private String getPlayId() {
    return playId;
  }

  private int getPosition() {
    return position;
  }

  private String getProgressId() {
    return progressId;
  }

  private Flow getSettingsPanel() {
    return settingsPanel;
  }

  private Widget getThumbnail(int index) {
    return thumbnails.getWidget(index);
  }

  private int getTransitionMillis() {
    return transitionMillis;
  }

  private void renderControls() {
    FaLabel play = new FaLabel(FontAwesome.PLAY);
    play.addStyleName(STYLE_PREFIX + "play");

    play.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        togglePlay();
      }
    });

    setPlayId(play.getId());
    controls.add(play);

    Progress progress = new Progress(slides.size() - 1);
    progress.addStyleName(STYLE_PREFIX + "progress");

    progress.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Element relativeElement = event.getRelativeElement();
        int x = event.getX();

        if (x > 0 && relativeElement != null && relativeElement.getOffsetWidth() > slides.size()) {
          int pos = x / (relativeElement.getOffsetWidth() / slides.size());
          activateSlide(pos);
        }
      }
    });

    setProgressId(progress.getId());
    controls.add(progress);

    FaLabel cycle = new FaLabel(FontAwesome.REPEAT);
    cycle.addStyleName(STYLE_REPEAT);
    updateRepeat(cycle.getElement());

    cycle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        setRepeat(!repeat());
        updateRepeat(EventUtils.getEventTargetElement(event));
      }
    });

    controls.add(cycle);

    FaLabel backward = new FaLabel(FontAwesome.BACKWARD);
    backward.addStyleName(STYLE_PREFIX + "backward");

    backward.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getPosition() > 0) {
          activateSlide(getPosition() - 1);
        } else if (repeat()) {
          activateSlide(slides.size() - 1);
        }
      }
    });

    setBackwardId(backward.getId());
    controls.add(backward);

    Label label = new Label();
    label.addStyleName(STYLE_PREFIX + "label");
    setLabelId(label.getId());
    controls.add(label);

    FaLabel forward = new FaLabel(FontAwesome.FORWARD);
    forward.addStyleName(STYLE_PREFIX + "forward");

    forward.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getPosition() < slides.size() - 1) {
          activateSlide(getPosition() + 1);
        } else if (repeat()) {
          activateSlide(0);
        }
      }
    });

    setForwardId(forward.getId());
    controls.add(forward);

    FaLabel settingsToggle = new FaLabel(FontAwesome.COG);
    settingsToggle.addStyleName(STYLE_SETTINGS_PREFIX + "toggle");

    settingsToggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        toggleSettingsPanel();
      }
    });

    controls.add(settingsToggle);
  }

  private Flow renderSettingsPanel() {
    Flow panel = new Flow(STYLE_SETTINGS_PREFIX + "panel");
    StyleUtils.setBottom(panel, CONTROLS_HEIGHT);

    HtmlTable table = new HtmlTable(STYLE_SETTINGS_PREFIX + "table");
    int row = 0;

    Label effLabel = new Label("Effect");
    table.setWidgetAndStyle(row, 0, effLabel, STYLE_SETTINGS_LABEL);

    final ListBox effectWidget = new ListBox();
    effectWidget.setCaptions(Effect.class);
    if (getEffect() != null) {
      effectWidget.setSelectedIndex(getEffect().ordinal());
    }

    effectWidget.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        Effect eff = EnumUtils.getEnumByIndex(Effect.class, effectWidget.getSelectedIndex());
        if (eff != null && eff != getEffect()) {
          animation.onEffectChanged(getEffect(), eff);
          setEffect(eff);
        }
      }
    });

    table.setWidgetAndStyle(row, 1, effectWidget, STYLE_SETTINGS_PREFIX + "effect");
    row++;

    Label idleLabel = new Label("Idle");
    table.setWidgetAndStyle(row, 0, idleLabel, STYLE_SETTINGS_LABEL);

    InputRange idleWidget = new InputRange(IDLE_MILLIS_MIN, IDLE_MILLIS_MAX, IDLE_MILLIS_STEP);
    idleWidget.setValue(getIdleMillis());

    idleWidget.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        int millis = BeeUtils.toInt(event.getValue());
        if (millis > 0) {
          setIdleMillis(millis);
          animation.updateCycleDuration();
        }
      }
    });

    table.setWidgetAndStyle(row, 1, idleWidget, STYLE_SETTINGS_PREFIX + "idle");
    row++;

    Label transitionLabel = new Label("Transition");
    table.setWidgetAndStyle(row, 0, transitionLabel, STYLE_SETTINGS_LABEL);

    InputRange transitionWidget = new InputRange(TRANSITION_MILLIS_MIN, TRANSITION_MILLIS_MAX,
        TRANSITION_MILLIS_STEP);
    transitionWidget.setValue(getTransitionMillis());

    transitionWidget.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        int millis = BeeUtils.toInt(event.getValue());
        if (millis > 0) {
          setTransitionMillis(millis);
          animation.updateCycleDuration();
        }
      }
    });

    table.setWidgetAndStyle(row, 1, transitionWidget, STYLE_SETTINGS_PREFIX + "transition");

    panel.add(table);

    FaLabel close = new FaLabel(FontAwesome.TIMES);
    close.addStyleName(STYLE_SETTINGS_PREFIX + "close");

    close.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getSettingsPanel() != null) {
          getSettingsPanel().addStyleName(STYLE_SETTINGS_HIDDEN);
        }
      }
    });

    panel.add(close);

    return panel;
  }

  private void renderThumbnails() {
    ClickHandler clickHandler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        activateSlide(DomUtils.getDataIndexInt(EventUtils.getEventTargetElement(event)));
      }
    };

    for (int i = 0; i < slides.size(); i++) {
      CustomDiv thumbnail = new CustomDiv(STYLE_PREFIX + "tn");
      StyleUtils.setBackgroundImage(thumbnail, slides.get(i).source);

      DomUtils.setDataIndex(thumbnail.getElement(), i);
      thumbnail.addClickHandler(clickHandler);

      thumbnails.add(thumbnail);
    }
  }

  private void setActiveWrapperIndex(int activeWrapperIndex) {
    this.activeWrapperIndex = activeWrapperIndex;
  }

  private void setBackwardId(String backwardId) {
    this.backwardId = backwardId;
  }

  private void setEffect(Effect effect) {
    this.effect = effect;
    BeeKeeper.getStorage().set(EFFECT_KEY, effect.ordinal());
  }

  private void setForwardId(String forwardId) {
    this.forwardId = forwardId;
  }

  private void setIdleMillis(int idleMillis) {
    this.idleMillis = idleMillis;
    BeeKeeper.getStorage().set(IDLE_MILLIS_KEY, idleMillis);
  }

  private void setLabelId(String labelId) {
    this.labelId = labelId;
  }

  private void setPlayId(String playId) {
    this.playId = playId;
  }

  private void setPosition(int position) {
    this.position = position;
  }

  private void setProgressId(String progressId) {
    this.progressId = progressId;
  }

  private void setSettingsPanel(Flow settingsPanel) {
    this.settingsPanel = settingsPanel;
  }

  private void setTransitionMillis(int transitionMillis) {
    this.transitionMillis = transitionMillis;
    BeeKeeper.getStorage().set(TRANSITION_MILLIS_KEY, transitionMillis);
  }

  private void togglePlay() {
    if (animation.isRunning()) {
      animation.suspend();
    } else if (animation.isPaused()) {
      animation.resume();
    } else {
      animation.start();
    }

    updateStyle();
    updatePlayIcon();
  }

  private void toggleSettingsPanel() {
    if (getSettingsPanel() == null) {
      setSettingsPanel(renderSettingsPanel());
      add(getSettingsPanel());
    } else {
      getSettingsPanel().getElement().toggleClassName(STYLE_SETTINGS_HIDDEN);
    }
  }

  private void updateActiveWrapper(int slideIndex) {
    slides.get(slideIndex).embed(getActiveWrapper());
  }

  private void updateCanvas(int slideIndex) {
    slides.get(slideIndex).embed(canvas);
  }

  private void updateLabel(int value) {
    Widget label = DomUtils.getChildById(controls, getLabelId());
    if (label != null) {
      String separator = BeeConst.STRING_SPACE + BeeConst.STRING_SLASH + BeeConst.STRING_SPACE;
      label.getElement().setInnerText(BeeUtils.join(separator, value, slides.size()));
    }
  }

  private void updateNavigation(int value) {
    if (!repeat()) {
      Widget backward = DomUtils.getChildById(controls, getBackwardId());
      if (backward != null) {
        backward.setStyleName(STYLE_NAV_DISABLED, value <= 0);
      }

      Widget forward = DomUtils.getChildById(controls, getForwardId());
      if (forward != null) {
        forward.setStyleName(STYLE_NAV_DISABLED, value >= slides.size() - 1);
      }
    }
  }

  private void updatePassiveWrapper(int slideIndex) {
    slides.get(slideIndex).embed(getPassiveWrapper());
  }

  private void updatePlayIcon() {
    Widget play = DomUtils.getChildById(controls, getPlayId());
    if (play instanceof FaLabel) {
      ((FaLabel) play).setChar(animation.isRunning() || animation.isResumed()
          ? FontAwesome.PAUSE : FontAwesome.PLAY);
    }
  }

  private void updateProgressControl(double value) {
    Widget progress = DomUtils.getChildById(controls, getProgressId());
    if (progress instanceof Progress) {
      ((Progress) progress).setValue(value);
    }
  }

  private void updateRepeat(Element el) {
    if (el != null) {
      el.setTitle("Turn repeat " + (repeat() ? "off" : "on"));

      if (repeat()) {
        el.removeClassName(STYLE_REPEAT_OFF);
        el.addClassName(STYLE_REPEAT_ON);
      } else {
        el.removeClassName(STYLE_REPEAT_ON);
        el.addClassName(STYLE_REPEAT_OFF);
      }
    }
  }

  private void updateStyle() {
    if (animation.isRunning() || animation.isResumed()) {
      removeStyleName(STYLE_NOT_PLAYING);
      removeStyleName(STYLE_PAUSED);
      addStyleName(STYLE_PLAYING);

    } else if (animation.isPaused()) {
      removeStyleName(STYLE_PLAYING);
      removeStyleName(STYLE_NOT_PLAYING);
      addStyleName(STYLE_PAUSED);

    } else {
      removeStyleName(STYLE_PLAYING);
      removeStyleName(STYLE_PAUSED);
      addStyleName(STYLE_NOT_PLAYING);
    }
  }
}
