package com.butent.bee.client.ui;

import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Settings;
import com.butent.bee.client.animation.HasAnimatableActivity;
import com.butent.bee.client.animation.HasHoverAnimation;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.style.Font;
import com.butent.bee.client.style.HasTextAlign;
import com.butent.bee.client.style.HasVerticalAlign;
import com.butent.bee.client.style.HasWhiteSpace;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.edit.TextBox;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasBounds;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.css.CssProperties;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.ui.HasMaxLength;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class UiHelper {

  public static void add(HasWidgets container, Holder<Widget> holder,
      WidgetInitializer initializer, String name) {

    Assert.notNull(holder);
    holder.set(add(container, holder.get(), initializer, name));
  }

  public static Widget add(HasWidgets container, Widget widget, WidgetInitializer initializer,
      String name) {

    if (container == null || widget == null) {
      return null;
    }

    if (initializer == null) {
      container.add(widget);
      return widget;
    }

    Widget w = initializer.initialize(widget, name);
    if (w != null) {
      container.add(w);
    }
    return w;
  }

  public static void clearTitle(UIObject obj) {
    Assert.notNull(obj);
    obj.setTitle(null);
  }

  public static void closeChildPopups(Widget widget) {
    Assert.notNull(widget);

    List<Popup> popups = Popup.getVisiblePopups();
    if (!popups.isEmpty()) {
      int index = 0;
      Popup parentPopup = getParentPopup(widget);

      if (parentPopup != null) {
        for (int i = 0; i < popups.size(); i++) {
          if (DomUtils.sameId(popups.get(i), parentPopup)) {
            index = i + 1;
            break;
          }
        }
      }

      if (index < popups.size()) {
        for (int i = popups.size() - 1; i >= index; i--) {
          popups.get(i).close();
        }
      }
    }
  }

  public static boolean closeDialog(Widget source) {
    if (source != null) {
      Popup popup = getParentPopup(source);
      if (popup != null) {
        popup.close();
        return true;
      }
    }
    return false;
  }

  public static void enableAndStyle(EnablableWidget widget, boolean enabled) {
    Assert.notNull(widget);
    widget.setEnabled(enabled);
    widget.setStyleName(StyleUtils.NAME_DISABLED, !enabled);
  }

  public static void enableAndStyleChildren(HasWidgets parent, boolean enabled) {
    Assert.notNull(parent);
    for (Widget child : parent) {
      if (child instanceof EnablableWidget) {
        enableAndStyle((EnablableWidget) child, enabled);
      }
    }
  }

  public static void enableChildren(HasWidgets parent, boolean enabled) {
    Assert.notNull(parent);
    for (Widget child : parent) {
      if (child instanceof HasEnabled) {
        ((HasEnabled) child).setEnabled(enabled);
      }
    }
  }

  public static boolean focus(Widget target) {
    if (target instanceof Focusable && isEnabled(target) && DomUtils.isVisible(target)) {
      ((Focusable) target).setFocus(true);
      return true;

    } else if (target instanceof HasOneWidget) {
      return focus(((HasOneWidget) target).getWidget());

    } else if (target instanceof HasWidgets) {
      for (Widget child : (HasWidgets) target) {
        if (focus(child)) {
          return true;
        }
      }
      return false;

    } else {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends Widget> Collection<T> getChildren(Widget parent, Class<T> clazz) {
    Collection<T> result = new ArrayList<>();
    if (parent == null || clazz == null) {
      return result;
    }

    if (parent.getClass().equals(clazz)) {
      result.add((T) parent);
    }

    if (parent instanceof HasOneWidget) {
      result.addAll(getChildren(((HasOneWidget) parent).getWidget(), clazz));

    } else if (parent instanceof HasWidgets) {
      for (Widget child : (HasWidgets) parent) {
        result.addAll(getChildren(child, clazz));
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Widget> T getChild(Widget parent, Class<T> clazz) {
    if (parent == null || clazz == null) {
      return null;

    } else if (parent.getClass().equals(clazz)) {
      return (T) parent;

    } else if (parent instanceof HasOneWidget) {
      return getChild(((HasOneWidget) parent).getWidget(), clazz);

    } else if (parent instanceof HasWidgets) {
      for (Widget widget : (HasWidgets) parent) {
        T child = getChild(widget, clazz);
        if (child != null) {
          return child;
        }
      }
      return null;

    } else {
      return null;
    }
  }

  public static Widget getChildByStyleName(Widget parent, String styleName) {
    Collection<Widget> children = getChildrenByStyleName(parent, Sets.newHashSet(styleName));

    if (children.size() == 1) {
      return BeeUtils.peek(children);
    } else {
      return null;
    }
  }

  public static Collection<Widget> getChildrenByStyleName(Widget parent,
      Collection<String> styleNames) {

    Collection<Widget> result = new ArrayList<>();
    if (parent == null || BeeUtils.isEmpty(styleNames)) {
      return result;
    }

    if (StyleUtils.hasAnyClass(parent.getElement(), styleNames)) {
      result.add(parent);
    }

    if (parent instanceof HasOneWidget) {
      result.addAll(getChildrenByStyleName(((HasOneWidget) parent).getWidget(), styleNames));

    } else if (parent instanceof HasWidgets) {
      for (Widget child : (HasWidgets) parent) {
        result.addAll(getChildrenByStyleName(child, styleNames));
      }
    }

    return result;
  }

  public static TextAlign getDefaultHorizontalAlignment(ValueType type) {
    if (type == null) {
      return null;
    }

    TextAlign align;

    switch (type) {
      case BOOLEAN:
        align = TextAlign.CENTER;
        break;

      case DECIMAL:
      case INTEGER:
      case LONG:
      case NUMBER:
        align = TextAlign.RIGHT;
        break;

      default:
        align = null;
    }
    return align;
  }

  public static WhiteSpace getDefaultWhiteSpace(ValueType type) {
    return ValueType.isNumeric(type) ? WhiteSpace.NOWRAP : null;
  }

  public static List<Focusable> getFocusableChildren(Widget parent) {
    List<Focusable> result = new ArrayList<>();
    if (parent == null) {
      return result;
    }

    if (parent instanceof HasOneWidget) {
      result.addAll(getFocusableChildren(((HasOneWidget) parent).getWidget()));

    } else if (parent instanceof HasWidgets) {
      for (Widget child : (HasWidgets) parent) {
        result.addAll(getFocusableChildren(child));
      }

    } else if (parent instanceof Focusable) {
      if (DomUtils.isVisible(parent)) {
        result.add((Focusable) parent);
      }
    }
    return result;
  }

  public static List<Widget> getImmediateChildren(Widget parent) {
    List<Widget> result = new ArrayList<>();
    if (parent == null) {
      return result;
    }

    if (parent instanceof HasOneWidget) {
      Widget child = ((HasOneWidget) parent).getWidget();
      if (child != null) {
        result.add(child);
      }

    } else if (parent instanceof HasWidgets) {
      for (Widget child : (HasWidgets) parent) {
        if (child != null) {
          result.add(child);
        }
      }
    }
    return result;
  }

  public static int getLayoutColumns(int size, int minCols, int maxCols) {
    Assert.isPositive(size);
    Assert.isTrue(minCols > 1);
    Assert.isTrue(minCols < maxCols);

    if (size <= minCols) {
      return size;
    }

    int cols = minCols;
    int rows = (size - 1) / cols + 1;
    int rem = (cols - size % cols) % cols;

    for (int c = minCols + 1; c <= Math.min(maxCols, size); c++) {
      int r = (size - 1) / c + 1;
      int x = (c - size % c) % c;

      if (x < rem || x == rem && Math.abs(r - c) <= Math.abs(cols - rows)) {
        cols = c;
        rows = r;
        rem = x;
      }
    }

    return cols;
  }

  public static int getLoadingStateDelayMillis() {
    int millis = BeeKeeper.getUser().getLoadingStateDelayMillis();
    if (BeeConst.isUndef(millis)) {
      millis = Settings.getLoadingStateDelayMillis();
    }

    return millis;
  }

  public static int getMaxLength(IsColumn column) {
    if (column == null) {
      return BeeConst.UNDEF;
    }

    ValueType type = column.getType();
    int precision = column.getPrecision();
    int scale = Math.max(column.getScale(), 0);

    if (precision <= 0) {
      switch (type) {
        case BOOLEAN:
          precision = 1;
          break;

        case DATE:
          precision = 10;
          break;

        case DATE_TIME:
          precision = 23;
          break;

        case INTEGER:
          precision = Integer.toString(Integer.MAX_VALUE).length();
          break;

        case LONG:
          precision = Long.toString(Long.MAX_VALUE).length();
          break;

        case NUMBER:
          precision = 20;
          break;

        case TIME_OF_DAY:
          precision = 8;
          break;

        case DECIMAL:
        case TEXT:
        case BLOB:
      }
    }

    if (precision <= 0) {
      return BeeConst.UNDEF;
    } else if (ValueType.isNumeric(type)) {
      return precision + (precision - scale) / 3 + ((scale > 0) ? 2 : 1);
    } else {
      return precision;
    }
  }

  public static Popup getParentPopup(Widget w) {
    Assert.notNull(w);

    Widget p = w;
    for (int i = 0; i < DomUtils.MAX_GENERATIONS; i++) {
      if (p instanceof Popup) {
        return (Popup) p;
      }

      p = p.getParent();
      if (p == null) {
        break;
      }
    }
    return null;
  }

  public static HtmlTable getParentTable(Widget w) {
    Assert.notNull(w);

    Widget p = w;
    for (int i = 0; i < DomUtils.MAX_GENERATIONS; i++) {
      if (p instanceof HtmlTable) {
        return (HtmlTable) p;
      }

      p = p.getParent();
      if (p == null) {
        break;
      }
    }
    return null;
  }

  public static Consumer<InputText> getTextBoxResizer(final int reserve) {
    return input -> {
      String value = input.getValue();

      int oldWidth = input.getOffsetWidth();
      int newWidth = reserve;

      if (value != null && value.length() > 0) {
        if (value.contains(BeeConst.STRING_SPACE)) {
          value = value.replace(BeeConst.STRING_SPACE, BeeConst.HTML_NBSP);
        }
        Font font = Font.getComputed(input.getElement());
        newWidth += Rulers.getAreaWidth(font, value, true);
      }

      if (newWidth != oldWidth) {
        StyleUtils.setWidth(input, newWidth);
      }
    };
  }

  public static boolean hasImmediateChild(HasWidgets parent, String id) {
    if (parent == null || BeeUtils.isEmpty(id)) {
      return false;
    }

    for (Widget child : parent) {
      if (DomUtils.idEquals(child, id)) {
        return true;
      }
    }
    return false;
  }

  public static void initActionWidget(Action action, Widget widget) {
    if (action != null && widget != null) {
      if (BeeUtils.isEmpty(widget.getTitle())) {
        widget.setTitle(action.getCaption());
      }

      if (action.animate() && widget instanceof HasAnimatableActivity) {
        int duration = BeeUtils.round(Previewer.getActionSensitivityMillis()
            * action.getSensitivityRatio());
        if (duration > 0) {
          ((HasAnimatableActivity) widget).enableAnimation(duration);
        }
      }

      HasHoverAnimation.init(widget);
    }
  }

  public static Widget initialize(Widget widget, WidgetInitializer initializer, String name) {
    if (widget == null) {
      return null;
    }
    if (initializer == null) {
      return widget;
    }
    return initializer.initialize(widget, name);
  }

  public static boolean isCopy(NativeEvent event) {
    return event != null && EventUtils.isKeyEvent(event.getType())
        && event.getKeyCode() == KeyCodes.KEY_C && (event.getCtrlKey() || event.getMetaKey());
  }

  public static boolean isInteractive(Widget widget) {
    Assert.notNull(widget);

    if (!widget.isAttached() || !DomUtils.isVisible(widget)) {
      return false;
    }

    Popup activePopup = Popup.getActivePopup();
    if (activePopup == null) {
      return true;
    }

    Popup parentPopup = getParentPopup(widget);
    return parentPopup != null && DomUtils.sameId(activePopup, parentPopup);
  }

  public static boolean isModal(Widget widget) {
    return getParentPopup(widget) != null;
  }

  public static boolean isOrHasChild(Widget parent, Class<? extends Widget> clazz) {
    return getChild(parent, clazz) != null;
  }

  public static boolean isSave(NativeEvent event) {
    if (event == null) {
      return false;
    }
    return EventUtils.isKeyDown(event.getType()) && event.getKeyCode() == KeyCodes.KEY_ENTER
        && EventUtils.hasModifierKey(event);
  }

  public static void makePotentiallyBold(Element element, String text) {
    if (element != null && !BeeUtils.isEmpty(text)) {
      DomUtils.setDataText(element, text);
      element.addClassName(StyleUtils.NAME_POTENTIALLY_BOLD);
    }
  }

  public static boolean maybeResize(Widget root, String id) {
    Widget child = DomUtils.getChildQuietly(root, id);
    if (child instanceof RequiresResize && DomUtils.isVisible(child)) {
      ((RequiresResize) child).onResize();
      return true;
    } else {
      return false;
    }
  }

  public static void maybeSetTitle(Widget widget, String title) {
    if (widget != null && !BeeUtils.isEmpty(title)) {
      widget.setTitle(title);
    }
  }

  public static boolean moveFocus(Widget parent, boolean forward) {
    return moveFocus(parent, DomUtils.getActiveElement(), forward);
  }

  public static boolean moveFocus(Widget parent, Element activeElement, boolean forward) {
    if (parent == null) {
      return false;
    }

    List<Focusable> children = getFocusableChildren(parent);
    if (children.isEmpty()) {
      return false;
    }
    int count = children.size();

    int index;

    if (activeElement == null) {
      index = forward ? 0 : count - 1;

    } else if (count == 1) {
      index = 0;
      if (isOrHasChild(children.get(index), activeElement)) {
        return false;
      }

    } else {
      index = BeeConst.UNDEF;
      for (int i = 0; i < count; i++) {
        if (isOrHasChild(children.get(i), activeElement)) {
          index = i;
          break;
        }
      }

      if (forward) {
        index = BeeUtils.rotateForwardExclusive(index, 0, count);
      } else {
        index = BeeUtils.rotateBackwardExclusive(index, 0, count);
      }
    }

    children.get(index).setFocus(true);
    return true;
  }

  public static void pressKey(TextBox widget, char key) {
    Assert.notNull(widget);

    String oldText = BeeUtils.nvl(widget.getText(), BeeConst.STRING_EMPTY);

    int pos = widget.getCursorPos();
    int len = widget.getSelectionLength();

    if (len <= 0 && widget instanceof HasMaxLength) {
      int maxLength = ((HasMaxLength) widget).getMaxLength();
      if (maxLength > 0 && BeeUtils.hasLength(oldText, maxLength)) {
        return;
      }
    }

    String newText;
    if (len > 0) {
      newText = BeeUtils.replace(oldText, pos, pos + len, key);
    } else {
      newText = BeeUtils.insert(oldText, pos, key);
    }

    widget.setText(newText);
    widget.setCursorPos(pos + 1);
  }

  public static int removeChildStyleName(Widget parent, String styleName) {
    if (parent == null || BeeUtils.isEmpty(styleName)) {
      return 0;
    }

    Collection<Widget> children = getChildrenByStyleName(parent, Sets.newHashSet(styleName));

    for (Widget child : children) {
      child.removeStyleName(styleName);
    }

    return children.size();
  }

  public static void selectDeferred(final TextBox widget) {
    Assert.notNull(widget);
    final String text = widget.getText();
    if (BeeUtils.isEmpty(text)) {
      return;
    }

    Scheduler.get().scheduleDeferred(() -> {
      if (text.equals(widget.getText())) {
        widget.selectAll();
      }
    });
  }

  public static void setBounds(HasBounds obj, String min, String max) {
    Assert.notNull(obj);
    if (!BeeUtils.isEmpty(min)) {
      obj.setMinValue(min);
    }
    if (!BeeUtils.isEmpty(max)) {
      obj.setMaxValue(max);
    }
  }

  public static void setColor(UIObject obj, Color color) {
    Assert.notNull(color);
    setColor(obj, color.getBackground(), color.getForeground());
  }

  public static void setColor(UIObject obj, String background, String foreground) {
    Assert.notNull(obj);

    if (!BeeUtils.isEmpty(background)) {
      StyleUtils.setBackgroundColor(obj, background);
    }
    if (!BeeUtils.isEmpty(foreground)) {
      StyleUtils.setColor(obj, foreground);
    }
  }

  public static void setDefaultBounds(HasBounds obj, IsColumn column) {
    Assert.notNull(obj);
    Assert.notNull(column);

    if (BeeUtils.isEmpty(obj.getMinValue())) {
      String min = DataUtils.getMinValue(column);
      if (!BeeUtils.isEmpty(min)) {
        obj.setMinValue(min);
      }
    }

    if (BeeUtils.isEmpty(obj.getMaxValue())) {
      String max = DataUtils.getMaxValue(column);
      if (!BeeUtils.isEmpty(max)) {
        obj.setMaxValue(max);
      }
    }
  }

  public static void setDefaultHorizontalAlignment(HasTextAlign obj, ValueType type) {
    Assert.notNull(obj);
    TextAlign align = getDefaultHorizontalAlignment(type);
    if (align != null) {
      obj.setTextAlign(align);
    }
  }

  public static void setDefaultWhiteSpace(HasWhiteSpace obj, ValueType type) {
    Assert.notNull(obj);
    WhiteSpace whiteSpace = getDefaultWhiteSpace(type);
    if (whiteSpace != null) {
      obj.setWhiteSpace(whiteSpace);
    }
  }

  public static void setHorizontalAlignment(Element elem, String text) {
    Assert.notNull(elem);
    if (BeeUtils.isEmpty(text)) {
      return;
    }

    TextAlign align = StyleUtils.parseTextAlign(text);
    if (align != null) {
      StyleUtils.setTextAlign(elem, align);
    }
  }

  public static void setHorizontalAlignment(HasTextAlign obj, String text) {
    Assert.notNull(obj);
    if (BeeUtils.isEmpty(text)) {
      return;
    }

    TextAlign align = StyleUtils.parseTextAlign(text);
    if (align != null) {
      obj.setTextAlign(align);
    }
  }

  public static void setVerticalAlignment(Element elem, String text) {
    Assert.notNull(elem);
    if (BeeUtils.isEmpty(text)) {
      return;
    }

    VerticalAlign align = StyleUtils.parseVerticalAlign(text);
    if (align != null) {
      StyleUtils.setProperty(elem.getStyle(), CssProperties.VERTICAL_ALIGN, align);
    }
  }

  public static void setVerticalAlignment(HasVerticalAlign obj, String text) {
    Assert.notNull(obj);
    if (BeeUtils.isEmpty(text)) {
      return;
    }

    VerticalAlign align = StyleUtils.parseVerticalAlign(text);
    if (align != null) {
      obj.setVerticalAlign(align);
    }
  }

  public static void setWhiteSpace(HasWhiteSpace obj, String input) {
    Assert.notNull(obj);

    WhiteSpace whiteSpace = StyleUtils.parseWhiteSpace(input);
    if (whiteSpace != null) {
      obj.setWhiteSpace(whiteSpace);
    }
  }

  public static void setWidget(HasOneWidget container, Holder<Widget> holder,
      WidgetInitializer initializer, String name) {
    Assert.notNull(holder);
    holder.set(setWidget(container, holder.get(), initializer, name));
  }

  public static Widget setWidget(HasOneWidget container, Widget widget,
      WidgetInitializer initializer, String name) {
    if (container == null || widget == null) {
      return null;
    }

    if (initializer == null) {
      container.setWidget(widget);
      return widget;
    }

    Widget w = initializer.initialize(widget, name);
    if (w != null) {
      container.setWidget(w);
    }
    return w;
  }

  private static boolean isEnabled(UIObject obj) {
    if (obj instanceof HasEnabled) {
      return ((HasEnabled) obj).isEnabled();
    } else {
      return obj != null;
    }
  }

  private static boolean isOrHasChild(Focusable widget, Element element) {
    return widget instanceof Widget && ((Widget) widget).getElement().isOrHasChild(element);
  }

  private UiHelper() {
  }
}
