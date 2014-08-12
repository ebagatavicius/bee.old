package com.butent.bee.client.composite;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Vertical;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;

public class Disclosure extends Vertical implements HasOpenHandlers<Disclosure>,
    HasCloseHandlers<Disclosure> {

  private static class ContentAnimation extends Animation {
    private boolean opening;
    private Disclosure curPanel;

    public void setOpen(Disclosure panel, boolean animate) {
      cancel();

      if (animate) {
        curPanel = panel;
        opening = panel.isOpen();
        run(panel.getAnimationDuration());

      } else {
        panel.contentWrapper.setVisible(panel.isOpen());
        if (panel.isOpen()) {
          panel.getContentWidget().setVisible(true);
        }

        UiHelper.maybeResizeForm(panel);
      }
    }

    @Override
    protected void onComplete() {
      if (opening) {
        curPanel.contentWrapper.getElement().getStyle().clearOpacity();
      } else {
        curPanel.contentWrapper.setVisible(false);
      }
      StyleUtils.autoHeight(curPanel.contentWrapper);

      UiHelper.maybeResizeForm(curPanel);
      curPanel = null;
    }

    @Override
    protected void onStart() {
      super.onStart();
      if (opening) {
        curPanel.contentWrapper.setVisible(true);
        curPanel.getContentWidget().setVisible(true);
      }
    }

    @Override
    protected void onUpdate(double progress) {
      int scrollHeight = curPanel.contentWrapper.getElement().getScrollHeight();
      int height = (int) (progress * scrollHeight);
      if (!opening) {
        height = scrollHeight - height;
      }
      height = Math.max(height, 1);

      double opacity = opening ? progress : 1 - progress;
      StyleUtils.setOpacity(curPanel.contentWrapper, opacity);

      StyleUtils.setHeight(curPanel.contentWrapper, height);
      StyleUtils.autoWidth(curPanel.contentWrapper);
    }
  }

  private final class Header extends Horizontal implements ClickHandler, OpenHandler<Disclosure>,
      CloseHandler<Disclosure> {

    private final Image iconImage;
    private final Imager imager;

    private Header(Imager imager, Widget widget) {
      super();
      this.imager = imager;
      iconImage = imager.makeImage();

      add(iconImage);
      setWidget(widget);

      setStyleName(STYLENAME_HEADER);

      iconImage.addClickHandler(this);
      addOpenHandler(this);
      addCloseHandler(this);

      update();
    }

    private Header(final ImageResource openImage, final ImageResource closedImage, Widget widget) {
      this(new Imager() {
        @Override
        public Image makeImage() {
          return new Image(closedImage);
        }

        @Override
        public void updateImage(boolean open, Image image) {
          if (open) {
            image.setResource(openImage);
          } else {
            image.setResource(closedImage);
          }
        }
      }, widget);
    }

    @Override
    public void onClick(ClickEvent event) {
      setOpen(!isOpen());
    }

    @Override
    public void onClose(CloseEvent<Disclosure> event) {
      update();
    }

    @Override
    public void onOpen(OpenEvent<Disclosure> event) {
      update();
    }

    private Widget getWidget() {
      if (getWidgetCount() > 1) {
        return getWidget(1);
      } else {
        return null;
      }
    }

    private void setWidget(Widget widget) {
      if (getWidget() != null) {
        remove(getWidget());
      }
      if (widget != null) {
        add(widget);
        if (widget instanceof HasClickHandlers) {
          ((HasClickHandlers) widget).addClickHandler(this);
        }
      }
    }

    private void update() {
      imager.updateImage(isOpen(), iconImage);
    }
  }

  private interface Imager {
    Image makeImage();

    void updateImage(boolean open, Image image);
  }

  private static final int DEFAULT_ANIMATION_DURATION = 350;

  private static final String STYLENAME_CONTAINER = "bee-Disclosure";

  private static final String STYLENAME_HEADER = STYLENAME_CONTAINER + "-header";
  private static final String STYLENAME_CONTENT = STYLENAME_CONTAINER + "-content";

  private static final String STYLENAME_SUFFIX_OPEN = "open";
  private static final String STYLENAME_SUFFIX_CLOSED = "closed";

  private static ContentAnimation contentAnimation;

  private final Simple contentWrapper = new Simple();

  private final Header header;

  private int animationDuration = DEFAULT_ANIMATION_DURATION;

  private boolean isOpen;

  public Disclosure() {
    this(null);
  }

  public Disclosure(ImageResource openImage, ImageResource closedImage, Widget headerWidget) {
    super();

    this.header = new Header(openImage, closedImage, headerWidget);
    super.add(header);
    super.add(contentWrapper);

    setStyleName(STYLENAME_CONTAINER);
    setContentDisplay(false);
  }

  public Disclosure(Widget headerWidget) {
    this(Global.getImages().disclosureOpen(), Global.getImages().disclosureClosed(), headerWidget);
  }

  @Override
  public void add(Widget w) {
    Assert.notNull(w);

    if (getHeaderWidget() == null) {
      setHeaderWidget(w);
    } else if (getContentWidget() == null) {
      setContentWidget(w);
    } else {
      Assert.state(false, "Disclosure already contains header and content");
    }
  }

  @Override
  public HandlerRegistration addCloseHandler(CloseHandler<Disclosure> handler) {
    return addHandler(handler, CloseEvent.getType());
  }

  @Override
  public HandlerRegistration addOpenHandler(OpenHandler<Disclosure> handler) {
    return addHandler(handler, OpenEvent.getType());
  }

  public int getAnimationDuration() {
    return animationDuration;
  }

  public Widget getContentWidget() {
    return contentWrapper.getWidget();
  }

  public Widget getHeaderWidget() {
    return header.getWidget();
  }

  @Override
  public String getIdPrefix() {
    return "disclosure";
  }

  public boolean isOpen() {
    return isOpen;
  }

  @Override
  public boolean remove(Widget w) {
    Assert.untouchable();
    return false;
  }

  public void setAnimationDuration(int animationDuration) {
    this.animationDuration = animationDuration;
  }

  public void setContentWidget(Widget widget) {
    Widget currentContent = getContentWidget();

    if (currentContent != null) {
      contentWrapper.setWidget(null);
      currentContent.removeStyleName(STYLENAME_CONTENT);
    }

    if (widget != null) {
      contentWrapper.setWidget(widget);
      widget.addStyleName(STYLENAME_CONTENT);
      setContentDisplay(false);
    }
  }

  public void setHeaderWidget(Widget headerWidget) {
    header.setWidget(headerWidget);
  }

  public void setOpen(boolean open) {
    if (this.isOpen != open) {
      this.isOpen = open;
      setContentDisplay(true);
      fireEvent();
    }
  }

  private void fireEvent() {
    if (isOpen()) {
      OpenEvent.fire(this, this);
    } else {
      CloseEvent.fire(this, this);
    }
  }

  private boolean isAnimationEnabled() {
    return getAnimationDuration() > 0;
  }

  private void setContentDisplay(boolean animate) {
    if (isOpen()) {
      removeStyleDependentName(STYLENAME_SUFFIX_CLOSED);
      addStyleDependentName(STYLENAME_SUFFIX_OPEN);
    } else {
      removeStyleDependentName(STYLENAME_SUFFIX_OPEN);
      addStyleDependentName(STYLENAME_SUFFIX_CLOSED);
    }

    if (getContentWidget() != null) {
      if (contentAnimation == null) {
        contentAnimation = new ContentAnimation();
      }
      contentAnimation.setOpen(this, animate && isAnimationEnabled());
    }
  }
}
