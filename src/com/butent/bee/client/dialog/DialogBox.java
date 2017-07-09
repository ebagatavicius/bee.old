package com.butent.bee.client.dialog;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

public class DialogBox extends Popup implements Printable {

  public static final int HEADER_HEIGHT = 31;

  private static final String STYLE_DIALOG = BeeConst.CSS_CLASS_PREFIX + "DialogBox";

  private static final String STYLE_HEADER = BeeConst.CSS_CLASS_PREFIX + "Dialog-header";
  private static final String STYLE_CAPTION = BeeConst.CSS_CLASS_PREFIX + "Dialog-caption";

  private static final String STYLE_ACTION = BeeConst.CSS_CLASS_PREFIX + "Dialog-action";
  private static final String STYLE_CLOSE = BeeConst.CSS_CLASS_PREFIX + "Dialog-close";

  private static final String STYLE_CONTENT = BeeConst.CSS_CLASS_PREFIX + "Dialog-content";

  public static DialogBox create(String caption) {
    return create(caption, null);
  }

  public static DialogBox create(String caption, String styleName) {
    DialogBox dialog = new DialogBox(caption, styleName);
    dialog.addDefaultCloseBox();
    return dialog;
  }

  public static DialogBox withoutCloseBox(String caption, String styleName) {
    return new DialogBox(caption, styleName);
  }

  private final Flow container = new Flow();
  private final Flow header = new Flow();

  private final String caption;

  protected DialogBox(String caption) {
    this(caption, null);
  }

  protected DialogBox(String caption, String styleName) {
    super(OutsideClick.IGNORE, STYLE_DIALOG);

    if (!BeeUtils.isEmpty(styleName) && !BeeUtils.same(styleName, STYLE_DIALOG)) {
      addStyleName(styleName);
    }

    header.addStyleName(STYLE_HEADER);

    CustomDiv captionWidget = new CustomDiv(STYLE_CAPTION);
    String text = Localized.maybeTranslate(caption);
    if (!BeeUtils.isEmpty(text)) {
      captionWidget.setHtml(text);
    }

    header.add(captionWidget);

    container.add(header);

    this.caption = caption;

    setResizable(true);
  }

  public void addAction(Action action, Widget widget) {
    if (widget != null) {
      if (action != null) {
        UiHelper.initActionWidget(action, widget);
      }

      insertAction(BeeConst.UNDEF, widget);
    }
  }

  public void addCommand(Widget widget) {
    addAction(null, widget);
  }

  public void addDefaultCloseBox() {
    FaLabel faClose = new FaLabel(FontAwesome.CLOSE);
    addCloseBox(faClose);
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public Widget getContent() {
    if (container.getWidgetCount() > 1) {
      return container.getWidget(container.getWidgetCount() - 1);
    } else {
      return null;
    }
  }

  @Override
  public String getIdPrefix() {
    return "dialog";
  }

  @Override
  public Element getPrintElement() {
    return container.getElement();
  }

  public void insertAction(int beforeIndex, Widget widget) {
    if (widget != null) {
      widget.addStyleName(STYLE_ACTION);

      if (beforeIndex >= 0 && beforeIndex < header.getWidgetCount()) {
        header.insert(widget, beforeIndex);
      } else {
        header.add(widget);
      }
    }
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    return !header.getElement().isOrHasChild(source)
        || !StyleUtils.hasClassName(source, STYLE_ACTION);
  }

  public Flow getHeader() {
    return header;
  }

  @Override
  public void setWidget(Widget w) {
    Assert.notNull(w);

    Widget content = getContent();
    if (content != null) {
      container.remove(content);
    }

    w.addStyleName(STYLE_CONTENT);
    container.add(w);

    super.setWidget(container);
  }

  protected void addCloseBox(FaLabel faLabel) {
    faLabel.addClickHandler(arg0 -> hide(CloseEvent.Cause.MOUSE_CLOSE_BOX, null, true));
    addAction(Action.CLOSE, faLabel);
  }

  protected void addCloseBox(ImageResource imageResource) {
    Image close = new Image(imageResource,
        () -> hide(CloseEvent.Cause.MOUSE_CLOSE_BOX, null, true));

    close.addStyleName(STYLE_CLOSE);
    addAction(Action.CLOSE, close);
  }

  @Override
  protected int getHeaderHeight() {
    return header.getOffsetHeight();
  }

  @Override
  protected boolean isCaptionEvent(NativeEvent event) {
    EventTarget target = event.getEventTarget();

    if (Element.is(target)) {
      Element el = Element.as(target);
      return header.getElement().isOrHasChild(el) && !StyleUtils.hasClassName(el, STYLE_ACTION);
    } else {
      return false;
    }
  }
}
