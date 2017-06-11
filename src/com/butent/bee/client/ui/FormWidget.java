package com.butent.bee.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.media.client.MediaBase;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.ChildSelector;
import com.butent.bee.client.composite.ColorEditor;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.Disclosure;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.FileGroup;
import com.butent.bee.client.composite.Gallery;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.composite.RadioGroup;
import com.butent.bee.client.composite.SliderBar;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.composite.TabGroup;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.composite.VolumeSlider;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.decorator.TuningFactory;
import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.grid.CellKind;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.grid.TableKind;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.layout.CellVector;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Details;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.FieldSet;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.HeaderContentFooter;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.HtmlPanel;
import com.butent.bee.client.layout.IsHtmlTable;
import com.butent.bee.client.layout.LayoutPanel;
import com.butent.bee.client.layout.ResizePanel;
import com.butent.bee.client.layout.Scroll;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.SimpleInline;
import com.butent.bee.client.layout.Span;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.layout.Stack;
import com.butent.bee.client.layout.SummaryProxy;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.layout.Vertical;
import com.butent.bee.client.composite.Relations;
import com.butent.bee.client.presenter.TreePresenter;
import com.butent.bee.client.richtext.RichTextEditor;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.style.HasTextAlign;
import com.butent.bee.client.style.HasVerticalAlign;
import com.butent.bee.client.style.StyleProvider;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.tree.HasTreeItems;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.tree.TreeItem;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.view.TreeContainer;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.Audio;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Canvas;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.CustomWidget;
import com.butent.bee.client.widget.DateLabel;
import com.butent.bee.client.widget.DateTimeLabel;
import com.butent.bee.client.widget.DecimalLabel;
import com.butent.bee.client.widget.DoubleLabel;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Flag;
import com.butent.bee.client.widget.Frame;
import com.butent.bee.client.widget.Heading;
import com.butent.bee.client.widget.HtmlList;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputFile;
import com.butent.bee.client.widget.InputInteger;
import com.butent.bee.client.widget.InputLong;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputRange;
import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.InputTimeOfDay;
import com.butent.bee.client.widget.IntegerLabel;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Legend;
import com.butent.bee.client.widget.Line;
import com.butent.bee.client.widget.Link;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.client.widget.LongLabel;
import com.butent.bee.client.widget.Meter;
import com.butent.bee.client.widget.Progress;
import com.butent.bee.client.widget.RowIdLabel;
import com.butent.bee.client.widget.Summary;
import com.butent.bee.client.widget.Svg;
import com.butent.bee.client.widget.TextLabel;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.client.widget.Video;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasBounds;
import com.butent.bee.shared.HasIntStep;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Launchable;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.CustomProperties;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasPercentageTag;
import com.butent.bee.shared.data.HasRelatedCurrency;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.Captions;
import com.butent.bee.shared.ui.ConditionalStyleDeclaration;
import com.butent.bee.shared.ui.HasCapsLock;
import com.butent.bee.shared.ui.HasMaxLength;
import com.butent.bee.shared.ui.HasTextDimensions;
import com.butent.bee.shared.ui.HasValueStartIndex;
import com.butent.bee.shared.ui.HasVisibleLines;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.RenderableToken;
import com.butent.bee.shared.ui.RendererDescription;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.XmlHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains available form elements.
 */

public enum FormWidget {
  ABSOLUTE_PANEL("AbsolutePanel", EnumSet.of(Type.HAS_LAYERS)),
  AUDIO("Audio", EnumSet.of(Type.DISPLAY)),
  BR("br", null),
  BUTTON("Button", EnumSet.of(Type.DISPLAY)),
  CANVAS("Canvas", EnumSet.of(Type.DISPLAY)),
  CHECK_BOX("CheckBox", EnumSet.of(Type.EDITABLE)),
  CHILD_GRID(UiConstants.TAG_CHILD_GRID, EnumSet.of(Type.IS_CHILD, Type.IS_GRID)),
  CHILD_SELECTOR("ChildSelector", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.IS_CHILD)),
  COLOR_EDITOR("ColorEditor", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  COMPLEX_PANEL("ComplexPanel", EnumSet.of(Type.HAS_LAYERS)),
  CUSTOM("Custom", EnumSet.of(Type.IS_CUSTOM)),
  CUSTOM_CHILD("CustomChild", EnumSet.of(Type.IS_CUSTOM, Type.IS_CHILD)),
  CUSTOM_DISPLAY("CustomDisplay", EnumSet.of(Type.IS_CUSTOM, Type.DISPLAY)),
  CUSTOM_EDITABLE("CustomEditable", EnumSet.of(Type.IS_CUSTOM, Type.EDITABLE)),
  CUSTOM_FOCUSABLE("CustomFocusable", EnumSet.of(Type.IS_CUSTOM, Type.FOCUSABLE, Type.EDITABLE)),
  DATA_SELECTOR("DataSelector", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.DISPLAY)),
  DATE_LABEL("DateLabel", EnumSet.of(Type.DISPLAY)),
  DATE_TIME_LABEL("DateTimeLabel", EnumSet.of(Type.DISPLAY)),
  DECIMAL_LABEL("DecimalLabel", EnumSet.of(Type.DISPLAY)),
  DECORATOR("decorator", EnumSet.of(Type.IS_DECORATOR)),
  DETAILS("Details", EnumSet.of(Type.HAS_CHILDREN)),
  DISCLOSURE("Disclosure", EnumSet.of(Type.HAS_CHILDREN)),
  DIV("div", null),
  DOUBLE_LABEL("DoubleLabel", EnumSet.of(Type.DISPLAY)),
  FA_LABEL("FaLabel", null),
  FIELD_SET("FieldSet", EnumSet.of(Type.HAS_CHILDREN)),
  FILE_COLLECTOR("FileCollector", null),
  FILE_GROUP("FileGroup", EnumSet.of(Type.DISPLAY)),
  FLAG("Flag", EnumSet.of(Type.DISPLAY)),
  FLOW_PANEL("FlowPanel", EnumSet.of(Type.HAS_CHILDREN)),
  FRAME("Frame", EnumSet.of(Type.DISPLAY)),
  GALLERY("Gallery", EnumSet.of(Type.IS_CHILD)),
  GRID_PANEL(UiConstants.TAG_GRID_PANEL, EnumSet.of(Type.IS_GRID)),
  HEADER_CONTENT_FOOTER("HeaderContentFooter", EnumSet.of(Type.PANEL)),
  HEADING("Heading", null),
  HORIZONTAL_PANEL("HorizontalPanel", EnumSet.of(Type.CELL_VECTOR)),
  HR("hr", null),
  HTML_PANEL("HtmlPanel", EnumSet.of(Type.PANEL)),
  IMAGE("Image", EnumSet.of(Type.DISPLAY)),
  INLINE_LABEL("InlineLabel", EnumSet.of(Type.IS_LABEL)),
  INPUT_AREA("InputArea", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_DATE("InputDate", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_DATE_TIME("InputDateTime", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_DECIMAL("InputDecimal", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_DOUBLE("InputDouble", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_FILE("InputFile", EnumSet.of(Type.INPUT)),
  INPUT_INTEGER("InputInteger", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_LONG("InputLong", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_MONEY("InputMoney", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_RANGE("InputRange", EnumSet.of(Type.EDITABLE, Type.INPUT)),
  INPUT_SPINNER("InputSpinner", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_TEXT("InputText", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_TIME("InputTime", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_TIME_OF_DAY("InputTimeOfDay", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INTEGER_LABEL("IntegerLabel", EnumSet.of(Type.DISPLAY)),
  INTERNAL_LINK("InternalLink", EnumSet.of(Type.DISPLAY)),
  LABEL("Label", EnumSet.of(Type.IS_LABEL)),
  LAYOUT_PANEL("LayoutPanel", EnumSet.of(Type.HAS_LAYERS)),
  LEGEND("Legend", null),
  LINE("Line", null),
  LINK("Link", EnumSet.of(Type.DISPLAY)),
  LIST_BOX("ListBox", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE)),
  LONG_LABEL("LongLabel", EnumSet.of(Type.DISPLAY)),
  METER("Meter", EnumSet.of(Type.DISPLAY)),
  MONEY_LABEL("MoneyLabel", EnumSet.of(Type.DISPLAY)),
  MULTI_SELECTOR(UiConstants.TAG_MULTI_SELECTOR,
      EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.DISPLAY)),
  ORDERED_LIST("OrderedList", null),
  PROGRESS("Progress", EnumSet.of(Type.DISPLAY)),
  RADIO("Radio", EnumSet.of(Type.EDITABLE)),
  RELATIONS("Relations", EnumSet.of(Type.EDITABLE, Type.IS_CHILD)),
  RESIZE_PANEL("ResizePanel", EnumSet.of(Type.HAS_ONE_CHILD)),
  RICH_TEXT_EDITOR("RichTextEditor", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE)),
  ROW_ID_LABEL("RowIdLabel", EnumSet.of(Type.DISPLAY)),
  SCROLL_PANEL("ScrollPanel", EnumSet.of(Type.HAS_ONE_CHILD)),
  SIMPLE_INLINE_PANEL("SimpleInlinePanel", EnumSet.of(Type.HAS_ONE_CHILD)),
  SIMPLE_PANEL("SimplePanel", EnumSet.of(Type.HAS_ONE_CHILD)),
  SLIDER_BAR("SliderBar", EnumSet.of(Type.EDITABLE)),
  SPAN_PANEL("SpanPanel", EnumSet.of(Type.HAS_CHILDREN)),
  SPLIT_PANEL("SplitPanel", EnumSet.of(Type.PANEL)),
  STACK_PANEL("StackPanel", EnumSet.of(Type.PANEL)),
  SUMMARY("Summary", null),
  SUMMARY_PROXY("SummaryProxy", null),
  SVG("Svg", EnumSet.of(Type.DISPLAY)),
  TAB_BAR("TabBar", EnumSet.of(Type.DISPLAY)),
  TAB_GROUP("TabGroup", EnumSet.of(Type.DISPLAY)),
  TABBED_PAGES("TabbedPages", EnumSet.of(Type.PANEL)),
  TABLE("Table", EnumSet.of(Type.IS_TABLE)),
  TEXT_LABEL("TextLabel", EnumSet.of(Type.DISPLAY)),
  TOGGLE("Toggle", EnumSet.of(Type.EDITABLE)),
  UNBOUND_SELECTOR("UnboundSelector", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE)),
  UNORDERED_LIST("UnorderedList", null),
  VERTICAL_PANEL("VerticalPanel", EnumSet.of(Type.CELL_VECTOR)),
  VIDEO("Video", EnumSet.of(Type.DISPLAY)),
  VOLUME_SLIDER("VolumeSlider", EnumSet.of(Type.EDITABLE)),
  TREE("Tree", EnumSet.of(Type.FOCUSABLE)),
  DATA_TREE(UiConstants.TAG_DATA_TREE, EnumSet.of(Type.FOCUSABLE));

  private final class HeaderAndContent {

    private final String headerTag;
    private final String headerString;
    private final IdentifiableWidget headerWidget;

    private final IdentifiableWidget content;

    private HeaderAndContent(String headerTag, String headerString,
        IdentifiableWidget headerWidget, IdentifiableWidget content) {

      this.headerTag = headerTag;
      this.headerString = headerString;
      this.headerWidget = headerWidget;

      this.content = content;
    }

    private IdentifiableWidget getContent() {
      return content;
    }

    private String getHeaderString() {
      return headerString;
    }

    private String getHeaderTag() {
      return headerTag;
    }

    private IdentifiableWidget getHeaderWidget() {
      return headerWidget;
    }

    private boolean isHeaderHtml() {
      return BeeUtils.same(getHeaderTag(), TAG_HTML);
    }

    private boolean isHeaderText() {
      return BeeUtils.same(getHeaderTag(), TAG_TEXT);
    }

    private boolean isValid() {
      if (getContent() == null) {
        return false;
      }

      if (isHeaderHtml() || isHeaderText()) {
        return !BeeUtils.isEmpty(getHeaderString());
      } else {
        return getHeaderWidget() != null;
      }
    }
  }

  /**
   * Contains a list of possible form element parameters like editable or focusable.
   */

  private enum Type {
    FOCUSABLE, EDITABLE, IS_LABEL, DISPLAY, HAS_ONE_CHILD, HAS_CHILDREN, HAS_LAYERS,
    IS_TABLE, IS_CHILD, IS_GRID, PANEL, CELL_VECTOR, INPUT, IS_CUSTOM, IS_DECORATOR
  }

  public static Relation createRelation(String viewName, Map<String, String> attributes,
      List<Element> children, Relation.RenderMode renderMode) {

    Relation relation = XmlUtils.getRelation(attributes, children);

    String source = attributes.get(UiConstants.ATTR_SOURCE);
    List<String> renderColumns =
        NameUtils.toList(attributes.get(RendererDescription.ATTR_RENDER_COLUMNS));

    if (renderColumns.isEmpty() && !children.isEmpty()) {
      for (Element child : children) {
        if (BeeUtils.same(XmlUtils.getLocalName(child), RenderableToken.TAG_RENDER_TOKEN)) {
          String tokenSource = child.getAttribute(UiConstants.ATTR_SOURCE);
          if (!BeeUtils.isEmpty(tokenSource)) {
            renderColumns.add(tokenSource.trim());
          }
        }
      }
    }

    Holder<String> sourceHolder = Holder.of(source);
    Holder<List<String>> listHolder = Holder.of(renderColumns);

    relation.initialize(Data.getDataInfoProvider(), viewName, sourceHolder, listHolder, renderMode,
        BeeKeeper.getUser().getUserId());
    if (relation.getViewName() == null) {
      logger.severe("Cannot create relation:");
      logger.severe(viewName, source, renderColumns);
      return null;
    }

    source = sourceHolder.get();
    renderColumns = listHolder.get();

    if (!BeeUtils.isEmpty(source)) {
      attributes.put(UiConstants.ATTR_SOURCE, source);
    }
    if (!BeeUtils.isEmpty(renderColumns)) {
      attributes.put(RendererDescription.ATTR_RENDER_COLUMNS, XmlHelper.getList(renderColumns));
    }

    return relation;
  }

  public static FormWidget getByTagName(String tagName) {
    if (!BeeUtils.isEmpty(tagName)) {
      for (FormWidget widget : FormWidget.values()) {
        if (BeeUtils.same(widget.getTagName(), tagName)) {
          return widget;
        }
      }
    }
    return null;
  }

  private static void addHandler(IdentifiableWidget widget, String event, String handler) {
    Assert.notNull(widget);

    if (BeeUtils.isEmpty(event)) {
      logger.warning("add handler:", NameUtils.getClassName(widget.getClass()),
          widget.getId(), "event type not specified");
      return;
    }
    if (BeeUtils.isEmpty(handler)) {
      logger.warning("add handler:", NameUtils.getClassName(widget.getClass()),
          widget.getId(), event, "event handler not specified");
      return;
    }

    EventUtils.addDomHandler(widget.asWidget(), event, handler);
  }

  private static void associateLabel(IdentifiableWidget label, BeeColumn column) {
    if (BeeUtils.isEmpty(label.getElement().getInnerHTML())) {
      label.getElement().setInnerHTML(Localized.getLabel(column));
    }

    if (column.isEditable()) {
      if (!column.isNullable() && !label.getElement().hasClassName(StyleUtils.NAME_REQUIRED)) {
        label.getElement().addClassName(StyleUtils.NAME_REQUIRED);
      }

      if (column.hasDefaults() && !label.getElement().hasClassName(StyleUtils.NAME_HAS_DEFAULTS)) {
        label.getElement().addClassName(StyleUtils.NAME_HAS_DEFAULTS);
      }
    }
  }

  private static IdentifiableWidget createFace(Element element) {
    Pair<String, Image> faceOptions = getFaceOptions(element);
    final IdentifiableWidget result;

    if (faceOptions.getB() != null) {
      result = faceOptions.getB();
    } else if (!BeeUtils.isEmpty(faceOptions.getA())) {
      result = new Button(faceOptions.getA());
    } else {
      result = null;
    }

    if (result != null) {
      StyleUtils.updateAppearance(result.asWidget(), element.getAttribute(UiConstants.ATTR_CLASS),
          element.getAttribute(UiConstants.ATTR_STYLE));
    }
    return result;
  }

  private static IdentifiableWidget createIfWidget(String formName, Element element,
      String viewName, List<BeeColumn> columns,
      WidgetDescriptionCallback widgetDescriptionCallback, WidgetInterceptor widgetInterceptor) {

    if (element == null) {
      return null;
    }
    FormWidget fw = getByTagName(XmlUtils.getLocalName(element));
    if (fw == null) {
      return null;
    }
    return fw.create(formName, element, viewName, columns, widgetDescriptionCallback,
        widgetInterceptor);
  }

  private static IdentifiableWidget createIfWidgetOrHtmlOrText(String formName, Element element,
      String viewName, List<BeeColumn> columns, WidgetDescriptionCallback wdcb,
      WidgetInterceptor widgetInterceptor) {

    if (element == null) {
      return null;
    }
    IdentifiableWidget widget = null;
    String tag = XmlUtils.getLocalName(element);

    if (BeeUtils.same(tag, TAG_TEXT)) {
      String text = Localized.maybeTranslate(XmlUtils.getText(element));
      if (!BeeUtils.isEmpty(text)) {
        widget = new InlineLabel(text);
      }

    } else if (BeeUtils.same(tag, TAG_HTML)) {
      String html = XmlUtils.getText(element);
      if (!BeeUtils.isEmpty(html)) {
        widget = new Label(html);
      }

    } else {
      widget = createIfWidget(formName, element, viewName, columns, wdcb, widgetInterceptor);
    }
    return widget;
  }

  private static IdentifiableWidget createInputNumber(Map<String, String> attributes,
      BeeColumn column, boolean money) {

    InputNumber widget = new InputNumber();

    String s = attributes.get(UiConstants.ATTR_SCALE);
    int scale;

    if (BeeUtils.isDigit(s)) {
      scale = BeeUtils.toInt(s);
    } else if (column != null && !BeeConst.isUndef(column.getScale())) {
      scale = money
          ? Math.min(column.getScale(), Format.getDefaultMoneyScale()) : column.getScale();
    } else {
      scale = money ? Format.getDefaultMoneyScale() : BeeConst.UNDEF;
    }

    widget.setScale(scale);

    String pattern = attributes.get(UiConstants.ATTR_FORMAT);
    NumberFormat format;

    if (BeeUtils.isEmpty(pattern)) {
      if (money && scale == Format.getDefaultMoneyScale()) {
        format = Format.getDefaultMoneyFormat();
      } else {
        format = Format.getDecimalFormat(scale);
      }
    } else {
      format = Format.getNumberFormat(pattern);
    }

    widget.setNumberFormat(format);

    String currencySource = attributes.get(HasRelatedCurrency.ATTR_CURRENCY_SOURCE);
    if (!BeeUtils.isEmpty(currencySource)) {
      widget.setCurrencySource(currencySource);
    }

    String percentageTag = attributes.get(HasPercentageTag.ATTR_PERCENTAGE_TAG);
    if (!BeeUtils.isEmpty(percentageTag)) {
      widget.setPercentageTag(percentageTag);
    }

    return widget;
  }

  private static IdentifiableWidget createOneChild(String formName, Element parent,
      String viewName, List<BeeColumn> columns,
      WidgetDescriptionCallback widgetDescriptionCallback, WidgetInterceptor widgetInterceptor) {

    for (Element child : XmlUtils.getChildrenElements(parent)) {
      IdentifiableWidget widget = createIfWidget(formName, child, viewName, columns,
          widgetDescriptionCallback, widgetInterceptor);
      if (widget != null) {
        return widget;
      }
    }
    return null;
  }

  private static boolean createTableCell(HtmlTable table, String formName, Element element,
      int row, int col, String viewName, List<BeeColumn> columns, WidgetDescriptionCallback wdcb,
      WidgetInterceptor widgetInterceptor) {

    boolean ok;
    String tag = XmlUtils.getLocalName(element);

    if (BeeUtils.same(tag, TAG_TEXT)) {
      String text = Localized.maybeTranslate(XmlUtils.getText(element));
      table.setHtml(row, col, text);
      ok = true;

    } else if (BeeUtils.same(tag, TAG_HTML)) {
      String html = XmlUtils.getText(element);
      table.setHtml(row, col, html);
      ok = true;

    } else {
      IdentifiableWidget widget = createIfWidget(formName, element, viewName, columns, wdcb,
          widgetInterceptor);
      ok = widget != null;
      if (ok) {
        table.setWidget(row, col, widget.asWidget());
      }
    }
    return ok;
  }

  private static BeeColumn getColumn(List<BeeColumn> columns, Map<String, String> attributes,
      String key) {

    if (columns == null && attributes == null) {
      return null;
    }

    String colName = attributes.get(key);
    if (BeeUtils.isEmpty(colName)) {
      return null;
    }
    return DataUtils.getColumn(colName, columns);
  }

  private static Edges getEdges(Element element) {
    return new Edges(XmlUtils.getAttributeDouble(element, ATTR_TOP),
        XmlUtils.getAttributeUnit(element, ATTR_TOP_UNIT),
        XmlUtils.getAttributeDouble(element, ATTR_RIGHT),
        XmlUtils.getAttributeUnit(element, ATTR_RIGHT_UNIT),
        XmlUtils.getAttributeDouble(element, ATTR_BOTTOM),
        XmlUtils.getAttributeUnit(element, ATTR_BOTTOM_UNIT),
        XmlUtils.getAttributeDouble(element, ATTR_LEFT),
        XmlUtils.getAttributeUnit(element, ATTR_LEFT_UNIT));
  }

  private static Pair<String, Image> getFaceOptions(Element element) {
    String html = getTextOrHtml(element);
    Image image = null;

    String name = element.getAttribute(ATTR_RESOURCE);
    if (!BeeUtils.isEmpty(name)) {
      ImageResource resource = Images.get(name);
      if (resource != null) {
        image = new Image(resource);
      }
    }
    if (image == null) {
      String url = element.getAttribute(ATTR_URL);
      if (!BeeUtils.isEmpty(url)) {
        image = new Image(url);
      }
    }
    return Pair.of(html, image);
  }

  private static String getTextOrHtml(Element element) {
    String text = element.getAttribute(UiConstants.ATTR_TEXT);
    if (BeeUtils.isEmpty(text)) {
      return element.getAttribute(UiConstants.ATTR_HTML);
    } else {
      return Localized.maybeTranslate(text);
    }
  }

  private static void initMedia(MediaBase widget, Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      widget.setAutoplay(false);
      widget.setControls(true);
      return;
    }

    String value = attributes.get(ATTR_AUTOPLAY);
    widget.setAutoplay(BeeConst.isTrue(value));

    value = attributes.get(ATTR_CONTROLS);
    widget.setControls(BeeUtils.isBoolean(value) ? BeeUtils.toBoolean(value) : true);

    value = attributes.get(ATTR_PRELOAD);
    if (!BeeUtils.isEmpty(value)) {
      widget.setPreload(value);
    }

    value = attributes.get(ATTR_DEFAULT_PLAYBACK_RATE);
    if (BeeUtils.isPositiveDouble(value)) {
      widget.setDefaultPlaybackRate(BeeUtils.toDouble(value));
    }

    value = attributes.get(ATTR_URL);
    if (!BeeUtils.isEmpty(value)) {
      widget.setSrc(value);
    }

    value = attributes.get(ATTR_CURRENT_TIME);
    if (BeeUtils.isPositiveDouble(value)) {
      widget.setCurrentTime(BeeUtils.toDouble(value));
    }

    value = attributes.get(ATTR_PLAYBACK_RATE);
    if (BeeUtils.isPositiveDouble(value)) {
      widget.setPlaybackRate(BeeUtils.toDouble(value));
    }

    value = attributes.get(ATTR_LOOP);
    if (BeeUtils.isBoolean(value)) {
      widget.setLoop(BeeUtils.toBoolean(value));
    }

    value = attributes.get(ATTR_MUTED);
    if (BeeUtils.isBoolean(value)) {
      widget.setMuted(BeeUtils.toBoolean(value));
    }

    value = attributes.get(ATTR_VOLUME);
    if (BeeUtils.isDouble(value, BeeConst.DOUBLE_ZERO, true, BeeConst.DOUBLE_ONE, true)) {
      widget.setCurrentTime(BeeUtils.toDouble(value));
    }
  }

  private static void setAttributes(IdentifiableWidget widget, Map<String, String> attributes) {
    for (Map.Entry<String, String> attr : attributes.entrySet()) {
      String name = attr.getKey();
      String value = attr.getValue();
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (BeeUtils.same(name, UiConstants.ATTR_CLASS)) {
        StyleUtils.updateClasses(widget.asWidget(), value);
      } else if (BeeUtils.same(name, UiConstants.ATTR_STYLE)) {
        StyleUtils.updateStyle(widget.asWidget().getElement().getStyle(), value);

      } else if (BeeUtils.same(name, ATTR_TITLE)) {
        widget.asWidget().setTitle(Localized.maybeTranslate(value));

      } else if (BeeUtils.same(name, HasOptions.ATTR_OPTIONS)) {
        if (widget instanceof HasOptions) {
          ((HasOptions) widget).setOptions(value);
        }

      } else if (BeeUtils.same(name, ATTR_TAB_INDEX)) {
        if (widget instanceof Focusable) {
          ((Focusable) widget).setTabIndex(BeeUtils.toInt(value));
        }

      } else if (BeeUtils.same(name, UiConstants.ATTR_HORIZONTAL_ALIGNMENT)) {
        if (widget instanceof HasTextAlign) {
          UiHelper.setHorizontalAlignment((HasTextAlign) widget, value);
        } else {
          UiHelper.setHorizontalAlignment(widget.getElement(), value);
        }

      } else if (BeeUtils.same(name, UiConstants.ATTR_VERTICAL_ALIGNMENT)) {
        if (widget instanceof HasVerticalAlign) {
          UiHelper.setVerticalAlignment((HasVerticalAlign) widget, value);
        } else {
          UiHelper.setVerticalAlignment(widget.getElement(), value);
        }

      } else if (BeeUtils.same(name, ATTR_CELL_CLASS)) {
        if (widget instanceof IsHtmlTable) {
          ((IsHtmlTable) widget).setDefaultCellClasses(value);
        }
      } else if (BeeUtils.same(name, ATTR_CELL_STYLE)) {
        if (widget instanceof IsHtmlTable) {
          ((IsHtmlTable) widget).setDefaultCellStyles(value);
        }

      } else if (BeeUtils.same(name, ATTR_MIN) || BeeUtils.same(name, HasBounds.ATTR_MIN_VALUE)) {
        if (widget instanceof HasBounds) {
          ((HasBounds) widget).setMinValue(value);
        }
      } else if (BeeUtils.same(name, ATTR_MAX) || BeeUtils.same(name, HasBounds.ATTR_MAX_VALUE)) {
        if (widget instanceof HasBounds) {
          ((HasBounds) widget).setMaxValue(value);
        }

      } else if (BeeUtils.same(name, ATTR_STEP)) {
        if (widget instanceof HasIntStep && BeeUtils.isPositiveInt(value)) {
          ((HasIntStep) widget).setStepValue(BeeUtils.toInt(value));
        }

      } else if (BeeUtils.same(name, UiConstants.ATTR_VALUE)) {
        if (widget instanceof Editor) {
          ((Editor) widget).setValue(value);
        }
      } else if (BeeUtils.same(name, HasValueStartIndex.ATTR_VALUE_START_INDEX)
          && BeeUtils.isDigit(value)) {
        if (widget instanceof HasValueStartIndex) {
          ((HasValueStartIndex) widget).setValueStartIndex(BeeUtils.toInt(value));
        }

      } else if (BeeUtils.same(name, HasVisibleLines.ATTR_VISIBLE_LINES)) {
        if (widget instanceof HasVisibleLines && BeeUtils.isPositiveInt(value)) {
          ((HasVisibleLines) widget).setVisibleLines(BeeUtils.toInt(value));
        }
      } else if (BeeUtils.same(name, HasTextDimensions.ATTR_CHARACTER_WIDTH)) {
        if (widget instanceof HasTextDimensions && BeeUtils.isPositiveInt(value)) {
          ((HasTextDimensions) widget).setCharacterWidth(BeeUtils.toInt(value));
        }

      } else if (BeeUtils.same(name, ATTR_MAX_LENGTH)) {
        if (widget instanceof HasMaxLength && BeeUtils.isPositiveInt(value)) {
          ((HasMaxLength) widget).setMaxLength(BeeUtils.toInt(value));
        }

      } else if (BeeUtils.same(name, EnumUtils.ATTR_ENUM_KEY)) {
        if (widget instanceof AcceptsCaptions) {
          ((AcceptsCaptions) widget).setCaptions(value);
        }

      } else if (BeeUtils.same(name, ATTR_PLACEHOLDER)) {
        DomUtils.setPlaceholder(widget.asWidget(), value);

      } else if (BeeUtils.same(name, HasCapsLock.ATTR_UPPER_CASE)) {
        if (widget instanceof HasCapsLock && BeeConst.isTrue(value)) {
          ((HasCapsLock) widget).setUpperCase(true);
        }

      } else if (BeeUtils.same(name, ATTR_SUMMARIZE)) {
        if (widget instanceof HasSummaryChangeHandlers) {
          ((HasSummaryChangeHandlers) widget).setSummarize(BeeUtils.toBoolean(value));
        }

      } else if (BeeUtils.same(name, Attributes.CONTENT_EDITABLE)) {
        widget.getElement().setPropertyString(name, value);
      }
    }
  }

  private static void setTableCellAttributes(HtmlTable table, Element element, int row, int col) {
    String z = element.getAttribute(UiConstants.ATTR_HORIZONTAL_ALIGNMENT);
    if (!BeeUtils.isEmpty(z)) {
      TextAlign horAlign = StyleUtils.parseTextAlign(z);
      if (horAlign != null) {
        table.getCellFormatter().setHorizontalAlignment(row, col, horAlign);
      }
    }

    z = element.getAttribute(UiConstants.ATTR_VERTICAL_ALIGNMENT);
    if (!BeeUtils.isEmpty(z)) {
      VerticalAlign vertAlign = StyleUtils.parseVerticalAlign(z);
      if (vertAlign != null) {
        table.getCellFormatter().setVerticalAlignment(row, col, vertAlign);
      }
    }

    Dimensions dimensions = XmlUtils.getDimensions(element);
    if (dimensions.hasWidth()) {
      table.getCellFormatter().setWidth(row, col, dimensions.getWidthValue(),
          Dimensions.normalizeUnit(dimensions.getWidthUnit()));
    }
    if (dimensions.hasHeight()) {
      table.getCellFormatter().setHeight(row, col, dimensions.getHeightValue(),
          Dimensions.normalizeUnit(dimensions.getHeightUnit()));
    }

    if (XmlUtils.tagIs(element, UiConstants.TAG_CELL)) {
      z = element.getAttribute(ATTR_WORD_WRAP);
      if (BeeUtils.isBoolean(z)) {
        table.getCellFormatter().setWordWrap(row, col, BeeUtils.toBoolean(z));
      }

      z = element.getAttribute(UiConstants.ATTR_CLASS);
      if (!BeeUtils.isEmpty(z)) {
        StyleUtils.updateClasses(table.getCellFormatter().ensureElement(row, col), z);
      }

      z = element.getAttribute(ATTR_KIND);
      if (!BeeUtils.isEmpty(z)) {
        CellKind cellKind = CellKind.parse(z);
        if (cellKind != null) {
          table.getCellFormatter().addStyleName(row, col, cellKind.getStyleName());
        }
      }

      z = element.getAttribute(UiConstants.ATTR_STYLE);
      if (!BeeUtils.isEmpty(z)) {
        StyleUtils.updateStyle(table.getCellFormatter().ensureElement(row, col), z);
      }

      String span = element.getAttribute(ATTR_COL_SPAN);
      if (BeeUtils.toInt(span) > 1) {
        table.getCellFormatter().setColSpan(row, col, BeeUtils.toInt(span));
      }
      span = element.getAttribute(ATTR_ROW_SPAN);
      if (BeeUtils.toInt(span) > 1) {
        table.getCellFormatter().setRowSpan(row, col, BeeUtils.toInt(span));
      }
    }
  }

  private static void setTableRowAttributes(HtmlTable table, Element element, int row) {
    String z = element.getAttribute(UiConstants.ATTR_VERTICAL_ALIGNMENT);
    if (!BeeUtils.isEmpty(z)) {
      VerticalAlign vertAlign = StyleUtils.parseVerticalAlign(z);
      if (vertAlign != null) {
        table.getRowFormatter().setVerticalAlign(row, vertAlign);
      }
    }

    if (XmlUtils.tagIs(element, UiConstants.TAG_ROW)) {
      z = element.getAttribute(UiConstants.ATTR_CLASS);
      if (!BeeUtils.isEmpty(z)) {
        StyleUtils.updateClasses(table.getRowFormatter().ensureElement(row), z);
      }

      z = element.getAttribute(UiConstants.ATTR_STYLE);
      if (!BeeUtils.isEmpty(z)) {
        StyleUtils.updateStyle(table.getRowFormatter().ensureElement(row), z);
      }
    }
  }

  private static void setVectorCellAttributes(CellVector parent, Element element,
      IdentifiableWidget cellContent) {

    String z = element.getAttribute(UiConstants.ATTR_HORIZONTAL_ALIGNMENT);
    if (!BeeUtils.isEmpty(z)) {
      TextAlign horAlign = StyleUtils.parseTextAlign(z);
      if (horAlign != null) {
        parent.setCellHorizontalAlignment(cellContent.asWidget(), horAlign);
      }
    }

    z = element.getAttribute(UiConstants.ATTR_VERTICAL_ALIGNMENT);
    if (!BeeUtils.isEmpty(z)) {
      VerticalAlign vertAlign = StyleUtils.parseVerticalAlign(z);
      if (vertAlign != null) {
        parent.setCellVerticalAlignment(cellContent.asWidget(), vertAlign);
      }
    }

    Dimensions dimensions = XmlUtils.getDimensions(element);
    if (dimensions.hasWidth()) {
      parent.setCellWidth(cellContent.asWidget(), dimensions.getWidthValue(),
          Dimensions.normalizeUnit(dimensions.getWidthUnit()));
    }
    if (dimensions.hasHeight()) {
      parent.setCellHeight(cellContent.asWidget(), dimensions.getHeightValue(),
          Dimensions.normalizeUnit(dimensions.getHeightUnit()));
    }

    if (XmlUtils.tagIs(element, UiConstants.TAG_CELL)) {
      TableCellElement cell = DomUtils.getParentCell(cellContent.getElement(), false);

      if (cell != null && parent.getElement().isOrHasChild(cell)) {
        StyleUtils.updateAppearance(cell, element.getAttribute(UiConstants.ATTR_CLASS),
            element.getAttribute(UiConstants.ATTR_STYLE));

        z = element.getAttribute(ATTR_KIND);
        if (!BeeUtils.isEmpty(z)) {
          CellKind cellKind = CellKind.parse(z);
          if (cellKind != null) {
            cell.addClassName(cellKind.getStyleName());
          }
        }
      }
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(FormWidget.class);

  private static final String ATTR_STYLE_PREFIX = "stylePrefix";

  private static final String ATTR_TITLE = "title";

  private static final String ATTR_DISABLABLE = "disablable";

  private static final String ATTR_INLINE = "inline";
  private static final String ATTR_CHAR = "char";

  private static final String ATTR_URL = "url";
  private static final String ATTR_ALT = "alt";
  private static final String ATTR_TAB_INDEX = "tabIndex";

  private static final String ATTR_LEFT = "left";
  private static final String ATTR_LEFT_UNIT = "leftUnit";
  private static final String ATTR_RIGHT = "right";
  private static final String ATTR_RIGHT_UNIT = "rightUnit";
  private static final String ATTR_TOP = "top";
  private static final String ATTR_TOP_UNIT = "topUnit";
  private static final String ATTR_BOTTOM = "bottom";
  private static final String ATTR_BOTTOM_UNIT = "bottomUnit";

  private static final String ATTR_CELL_CLASS = "cellClass";
  private static final String ATTR_CELL_STYLE = "cellStyle";

  private static final String ATTR_COL_SPAN = "colSpan";
  private static final String ATTR_ROW_SPAN = "rowSpan";

  private static final String ATTR_WORD_WRAP = "wordWrap";
  private static final String ATTR_INDEX = "index";
  private static final String ATTR_HEADER_SIZE = "headerSize";
  private static final String ATTR_RESOURCE = "resource";
  private static final String ATTR_VERTICAL = "vertical";
  private static final String ATTR_MULTI_SELECT = "multiSelect";
  private static final String ATTR_ALL_ITEMS_VISIBLE = "allItemsVisible";
  private static final String ATTR_MIN_SIZE = "minSize";
  private static final String ATTR_MAX_SIZE = "maxSize";

  private static final String ATTR_VALUE_NUMERIC = "valueNumeric";

  private static final String ATTR_MIN = "min";
  private static final String ATTR_MAX = "max";

  private static final String ATTR_STEP = "step";

  private static final String ATTR_MIN_STEP = "minStep";

  private static final String ATTR_MAX_STEP = "maxStep";
  private static final String ATTR_HIGH = "high";
  private static final String ATTR_LOW = "low";

  private static final String ATTR_OPTIMUM = "optimum";
  private static final String ATTR_NUM_LABELS = "numLabels";

  private static final String ATTR_NUM_TICKS = "numTicks";
  private static final String ATTR_AUTOPLAY = "autoplay";

  private static final String ATTR_CONTROLS = "controls";
  private static final String ATTR_CURRENT_TIME = "currentTime";

  private static final String ATTR_DEFAULT_PLAYBACK_RATE = "defaultPlaybackRate";

  private static final String ATTR_LOOP = "loop";
  private static final String ATTR_MUTED = "muted";

  private static final String ATTR_PLAYBACK_RATE = "playbackRate";

  private static final String ATTR_PRELOAD = "preload";
  private static final String ATTR_VOLUME = "volume";

  private static final String ATTR_OPEN = "open";
  private static final String ATTR_EVENT = "event";

  private static final String ATTR_ID = "id";
  private static final String ATTR_CHECKED = "checked";

  private static final String ATTR_MULTIPLE = "multiple";
  private static final String ATTR_ACCEPT = "accept";
  private static final String ATTR_DECORATOR = "decorator";
  private static final String ATTR_DEFAULT_DECORATOR = "defaultDecorator";
  private static final String ATTR_PLACEHOLDER = "placeholder";
  private static final String ATTR_MAX_LENGTH = "maxLength";
  private static final String ATTR_VISIBLE_COLUMNS = "visibleColumns";
  private static final String ATTR_EDITABLE_COLUMNS = "editableColumns";
  private static final String ATTR_TEXT_ONLY = "textOnly";

  private static final String ATTR_UP_FACE = "upFace";
  private static final String ATTR_DOWN_FACE = "downFace";

  private static final String ATTR_CHILD = "child";
  private static final String ATTR_KIND = "kind";

  private static final String ATTR_SUMMARIZE = "summarize";

  private static final String ATTR_RESIZABLE = "resizable";

  private static final String ATTR_SPELL_CHECK = "spellcheck";

  private static final String ATTR_PREDICATE = "predicate";

  private static final String TAG_CSS = "css";

  private static final String TAG_HANDLER = "handler";

  private static final String TAG_CALC = "calc";

  private static final String TAG_VALIDATION = "validation";

  private static final String TAG_EDITABLE = "editable";

  private static final String TAG_CARRY = "carry";

  private static final String TAG_HTML = "html";

  private static final String TAG_TEXT = "text";

  private static final String TAG_LAYER = "layer";

  private static final String TAG_HEADER = "header";

  private static final String TAG_CONTENT = "content";

  private static final String TAG_FOOTER = "footer";

  private static final String TAG_STACK = "stack";

  private static final String TAG_PAGE = "page";

  private static final String TAG_OPTION = "option";

  private static final String TAG_TREE_ITEM = "TreeItem";

  private static final String TAG_TAB = "tab";

  private static final String TAG_FACE = "face";

  private final String tagName;

  private final Set<Type> types;

  FormWidget(String tagName, Set<Type> types) {
    this.tagName = tagName;
    this.types = types;
  }

  public IdentifiableWidget create(String formName, Element element, String viewName,
      List<BeeColumn> columns, WidgetDescriptionCallback widgetDescriptionCallback,
      WidgetInterceptor widgetInterceptor) {

    Assert.notNull(element);

    String name = element.getAttribute(UiConstants.ATTR_NAME);
    if (formName != null && !BeeUtils.isEmpty(name) && FormFactory.isHidden(formName, name)) {
      return null;
    }
    if (widgetInterceptor != null && !widgetInterceptor.beforeCreateWidget(name, element)) {
      return null;
    }

    Map<String, String> attributes = XmlUtils.getAttributes(element);
    List<Element> children = XmlUtils.getChildrenElements(element);

    BeeColumn column = getColumn(columns, attributes, UiConstants.ATTR_SOURCE);

    String html = getTextOrHtml(element);

    String url;
    String format;
    String min;
    String max;
    String step;
    boolean inline;
    String stylePrefix;
    String accept;

    Relation relation = null;
    IdentifiableWidget widget = null;

    switch (this) {
      case ABSOLUTE_PANEL:
        widget = new Absolute();
        break;

      case AUDIO:
        widget = new Audio();
        initMedia((Audio) widget, attributes);
        break;

      case BR:
        widget = new CustomWidget(Document.get().createBRElement());
        break;

      case BUTTON:
        widget = new Button(html);
        break;

      case CANVAS:
        widget = new Canvas();
        break;

      case CHECK_BOX:
        String label;
        if (BeeUtils.isEmpty(html)) {
          label = (column == null) ? null : Localized.getLabel(column);
        } else if (Captions.isCaption(html)) {
          label = html;
        } else {
          label = null;
        }

        widget = new InputBoolean(label);
        if (BeeConst.isTrue(attributes.get(ATTR_CHECKED))) {
          ((InputBoolean) widget).setValue(BeeConst.STRING_TRUE);
        }
        break;

      case CHILD_GRID:
        String gridName = BeeUtils.notEmpty(attributes.get(UiConstants.ATTR_GRID_NAME), name);

        String relColumn = attributes.get(UiConstants.ATTR_REL_COLUMN);
        String source = attributes.get(UiConstants.ATTR_SOURCE);
        int sourceIndex = BeeUtils.isEmpty(source)
            ? DataUtils.ID_INDEX : DataUtils.getColumnIndex(source, columns);

        if (!BeeUtils.isEmpty(gridName) && !BeeUtils.isEmpty(relColumn)
            && !BeeConst.isUndef(sourceIndex)) {
          widget = new ChildGrid(gridName, GridFactory.getGridOptions(attributes),
              sourceIndex, relColumn, !BeeConst.isFalse(attributes.get(ATTR_DISABLABLE)));
        }
        break;

      case GALLERY:
        widget = Gallery.create(attributes);
        break;

      case CHILD_SELECTOR:
        relation = createRelation(null, attributes, children, Relation.RenderMode.SOURCE);
        if (relation != null) {
          if (widgetInterceptor != null) {
            widgetInterceptor.configureRelation(name, relation);
          }
          widget = ChildSelector.create(viewName, relation, attributes);
        }
        break;

      case COLOR_EDITOR:
        if (Features.supportsInputColor()) {
          widget = new ColorEditor();
        } else {
          widget = new InputText();
        }
        break;

      case COMPLEX_PANEL:
        widget = new Complex();
        break;

      case CUSTOM:
      case CUSTOM_CHILD:
      case CUSTOM_DISPLAY:
      case CUSTOM_EDITABLE:
      case CUSTOM_FOCUSABLE:
        if (widgetInterceptor != null) {
          widget = widgetInterceptor.createCustomWidget(name, element);
        }
        break;

      case DATA_SELECTOR:
        relation = createRelation(viewName, attributes, children, Relation.RenderMode.TARGET);
        if (relation != null) {
          if (widgetInterceptor != null) {
            widgetInterceptor.configureRelation(name, relation);
          }
          widget = new DataSelector(relation, true);
        }
        break;

      case DATE_LABEL:
        format = attributes.get(UiConstants.ATTR_FORMAT);
        inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
        if (BeeUtils.isEmpty(format)) {
          widget = new DateLabel(inline);
        } else {
          widget = new DateLabel(format, inline);
        }
        break;

      case DATE_TIME_LABEL:
        format = attributes.get(UiConstants.ATTR_FORMAT);
        inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
        if (BeeUtils.isEmpty(format)) {
          widget = new DateTimeLabel(inline);
        } else {
          widget = new DateTimeLabel(format, inline);
        }
        break;

      case DECIMAL_LABEL:
        format = attributes.get(UiConstants.ATTR_FORMAT);
        inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
        if (BeeUtils.isEmpty(format)) {
          widget = new DecimalLabel(BeeUtils.toInt(attributes.get(UiConstants.ATTR_SCALE)), inline);
        } else {
          widget = new DecimalLabel(format, inline);
        }
        break;

      case DETAILS:
        widget = new Details(BeeConst.isTrue(attributes.get(ATTR_OPEN)));
        break;

      case DISCLOSURE:
        boolean open = BeeConst.isTrue(attributes.get(ATTR_OPEN));
        if (BeeUtils.isEmpty(html)) {
          widget = new Disclosure(open);
        } else {
          widget = new Disclosure(open, new Label(html));
        }
        break;

      case DIV:
        widget = new CustomDiv();
        if (!BeeUtils.isEmpty(html)) {
          ((CustomDiv) widget).setHtml(html);
        }
        break;

      case DOUBLE_LABEL:
        format = attributes.get(UiConstants.ATTR_FORMAT);
        inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
        if (BeeUtils.isEmpty(format)) {
          widget = new DoubleLabel(inline);
        } else {
          widget = new DoubleLabel(format, inline);
        }
        break;

      case FA_LABEL:
        FontAwesome fa = FontAwesome.parse(attributes.get(ATTR_CHAR));
        if (fa == null) {
          logger.warning(FontAwesome.FAMILY, "cannot parse", ATTR_CHAR, attributes.get(ATTR_CHAR));
        } else {
          inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
          widget = new FaLabel(fa, inline);
        }
        break;

      case FIELD_SET:
        widget = new FieldSet();
        break;

      case FILE_COLLECTOR:
        IdentifiableWidget face = null;
        for (Iterator<Element> it = children.iterator(); it.hasNext(); ) {
          Element child = it.next();
          if (BeeUtils.same(XmlUtils.getLocalName(child), TAG_FACE)) {
            face = createFace(child);
            it.remove();
          }
        }
        if (face == null) {
          face = FileCollector.getDefaultFace();
        }

        widget = new FileCollector(face,
            FileCollector.parseColumns(attributes.get(ATTR_VISIBLE_COLUMNS)),
            FileCollector.parseColumns(attributes.get(ATTR_EDITABLE_COLUMNS)));

        accept = attributes.get(ATTR_ACCEPT);
        if (!BeeUtils.isEmpty(accept)) {
          ((FileCollector) widget).setAccept(accept);
        }
        break;

      case FILE_GROUP:
        widget = new FileGroup(FileGroup.parseColumns(attributes.get(ATTR_VISIBLE_COLUMNS)),
            FileGroup.parseColumns(attributes.get(ATTR_EDITABLE_COLUMNS)));
        break;

      case FLAG:
        String country = attributes.get(Flag.ATTR_COUNTRY);
        if (BeeUtils.isEmpty(country)) {
          widget = new Flag();
        } else {
          widget = new Flag(country);
        }
        break;

      case FLOW_PANEL:
        widget = new Flow();
        break;

      case FRAME:
        url = attributes.get(ATTR_URL);
        if (BeeUtils.isEmpty(url)) {
          widget = new Frame();
        } else {
          widget = new Frame(url);
        }
        break;

      case GRID_PANEL:
        String gName = BeeUtils.notEmpty(attributes.get(UiConstants.ATTR_GRID_NAME), name);
        if (!BeeUtils.isEmpty(gName)) {
          widget = new GridPanel(gName, GridFactory.getGridOptions(attributes),
              BeeConst.isTrue(attributes.get(ATTR_CHILD)));
        }
        break;

      case HEADER_CONTENT_FOOTER:
        widget = new HeaderContentFooter();
        break;

      case HEADING:
        String rank = attributes.get(Heading.ATTR_RANK);
        if (BeeUtils.isPositiveInt(rank)) {
          widget = new Heading(BeeUtils.toInt(rank), html);
        }
        break;

      case HORIZONTAL_PANEL:
        widget = new Horizontal();
        break;

      case HR:
        widget = new CustomWidget(Document.get().createHRElement());
        break;

      case HTML_PANEL:
        if (!children.isEmpty()) {
          StringBuilder sb = new StringBuilder();
          for (Element child : children) {
            sb.append(child.toString());
          }
          widget = new HtmlPanel(sb.toString());
          children.clear();
        }
        break;

      case IMAGE:
        String resource = attributes.get(ATTR_RESOURCE);
        if (!BeeUtils.isEmpty(resource)) {
          widget = new Image(Images.get(resource));
        } else {
          url = attributes.get(ATTR_URL);
          if (!BeeUtils.isEmpty(url)) {
            widget = new Image(url);
          } else {
            widget = new Image();
          }
        }

        String alt = attributes.get(ATTR_ALT);
        if (!BeeUtils.isEmpty(alt)) {
          ((Image) widget).setAlt(alt);
        }
        break;

      case INLINE_LABEL:
        widget = new InlineLabel(html);
        break;

      case INPUT_AREA:
        widget = new InputArea();
        String spellCheck = attributes.get(ATTR_SPELL_CHECK);
        if (BeeUtils.isBoolean(spellCheck)) {
          ((InputArea) widget).setSpellCheck(BeeUtils.toBoolean(spellCheck));
        }
        break;

      case INPUT_DATE:
        widget = new InputDate();
        format = attributes.get(UiConstants.ATTR_FORMAT);
        if (!BeeUtils.isEmpty(format)) {
          ((InputDate) widget).setDateTimeFormat(Format.parseDateTimeFormat(format));
        }
        break;

      case INPUT_DATE_TIME:
        widget = new InputDateTime();
        format = attributes.get(UiConstants.ATTR_FORMAT);
        if (!BeeUtils.isEmpty(format)) {
          ((InputDateTime) widget).setDateTimeFormat(Format.parseDateTimeFormat(format));
        }
        break;

      case INPUT_DECIMAL:
        widget = createInputNumber(attributes, column, false);
        break;

      case INPUT_DOUBLE:
        widget = new InputNumber();
        ((InputNumber) widget).setNumberFormat(Format.getNumberFormat(
            attributes.get(UiConstants.ATTR_FORMAT), Format.getDefaultDoubleFormat()));
        break;

      case INPUT_FILE:
        widget = new InputFile(BeeConst.isTrue(attributes.get(ATTR_MULTIPLE)));
        if (!BeeUtils.isEmpty(name)) {
          ((InputFile) widget).setName(name.trim());
        }
        accept = attributes.get(ATTR_ACCEPT);
        if (!BeeUtils.isEmpty(accept)) {
          ((InputFile) widget).setAccept(accept);
        }
        break;

      case INPUT_INTEGER:
        widget = new InputInteger();
        ((InputInteger) widget).setNumberFormat(Format.getNumberFormat(
            attributes.get(UiConstants.ATTR_FORMAT), Format.getDefaultIntegerFormat()));
        break;

      case INPUT_LONG:
        widget = new InputLong();
        ((InputLong) widget).setNumberFormat(Format.getNumberFormat(
            attributes.get(UiConstants.ATTR_FORMAT), Format.getDefaultLongFormat()));
        break;

      case INPUT_MONEY:
        widget = createInputNumber(attributes, column, true);
        break;

      case INPUT_RANGE:
        widget = new InputRange();
        format = attributes.get(UiConstants.ATTR_FORMAT);
        if (!BeeUtils.isEmpty(format)) {
          ((InputRange) widget).setNumberFormat(Format.getNumberFormat(format));
        }
        break;

      case INPUT_SPINNER:
        widget = new InputSpinner();
        format = attributes.get(UiConstants.ATTR_FORMAT);
        if (!BeeUtils.isEmpty(format)) {
          ((InputSpinner) widget).setNumberFormat(Format.getNumberFormat(format));
        }
        break;

      case INPUT_TEXT:
        widget = new InputText();
        break;

      case INPUT_TIME:
        widget = new InputTime();
        break;

      case INPUT_TIME_OF_DAY:
        widget = new InputTimeOfDay();
        break;

      case INTEGER_LABEL:
        format = attributes.get(UiConstants.ATTR_FORMAT);
        inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
        if (BeeUtils.isEmpty(format)) {
          widget = new IntegerLabel(inline);
        } else {
          widget = new IntegerLabel(format, inline);
        }
        break;

      case INTERNAL_LINK:
        widget = new InternalLink(html);
        break;

      case LABEL:
        widget = new Label(html);
        break;

      case LAYOUT_PANEL:
        widget = new LayoutPanel();
        break;

      case LEGEND:
        widget = new Legend(html);
        break;

      case LINE:
        Double x1 = XmlUtils.getAttributeDouble(element, "x1");
        Double y1 = XmlUtils.getAttributeDouble(element, "y1");
        Double x2 = XmlUtils.getAttributeDouble(element, "x2");
        Double y2 = XmlUtils.getAttributeDouble(element, "y2");

        if (BeeUtils.isDouble(x1) && BeeUtils.isDouble(y1)
            && BeeUtils.isDouble(x2) && BeeUtils.isDouble(y2)) {
          widget = new Line(x1, y1, x2, y2);
        }
        break;

      case LINK:
        url = attributes.get(ATTR_URL);
        widget = new Link(html, url);
        break;

      case LIST_BOX:
        widget = new ListBox(BeeUtils.toBoolean(attributes.get(ATTR_MULTI_SELECT)));
        String isNum = attributes.get(ATTR_VALUE_NUMERIC);
        if (BeeUtils.isBoolean(isNum)) {
          ((ListBox) widget).setValueNumeric(BeeUtils.toBoolean(isNum));
        } else if (column != null && ValueType.isNumeric(column.getType())) {
          ((ListBox) widget).setValueNumeric(true);
        }
        break;

      case LONG_LABEL:
        format = attributes.get(UiConstants.ATTR_FORMAT);
        inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
        if (BeeUtils.isEmpty(format)) {
          widget = new LongLabel(inline);
        } else {
          widget = new LongLabel(format, inline);
        }
        break;

      case METER:
        min = attributes.get(ATTR_MIN);
        max = attributes.get(ATTR_MAX);

        if (Features.supportsElementMeter() && BeeUtils.isDouble(min) && BeeUtils.isDouble(max)
            && BeeUtils.toDouble(min) < BeeUtils.toDouble(max)) {
          widget = new Meter();
          ((Meter) widget).setMin(BeeUtils.toDouble(min));
          ((Meter) widget).setMax(BeeUtils.toDouble(max));

          String z = attributes.get(ATTR_LOW);
          if (BeeUtils.isDouble(z)) {
            ((Meter) widget).setLow(BeeUtils.toDouble(z));
          }
          z = attributes.get(ATTR_HIGH);
          if (BeeUtils.isDouble(z)) {
            ((Meter) widget).setHigh(BeeUtils.toDouble(z));
          }
          z = attributes.get(ATTR_OPTIMUM);
          if (BeeUtils.isDouble(z)) {
            ((Meter) widget).setOptimum(BeeUtils.toDouble(z));
          }
        }
        break;

      case MONEY_LABEL:
        format = attributes.get(UiConstants.ATTR_FORMAT);
        inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
        if (BeeUtils.isEmpty(format)) {
          widget = new DecimalLabel(Format.getDefaultMoneyFormat(), inline);
        } else {
          widget = new DecimalLabel(format, inline);
        }
        break;

      case MULTI_SELECTOR:
        relation = createRelation(null, attributes, children, Relation.RenderMode.SOURCE);
        if (relation != null) {
          if (widgetInterceptor != null) {
            widgetInterceptor.configureRelation(name, relation);
          }

          String property = attributes.get(UiConstants.ATTR_PROPERTY);

          CellSource cellSource = null;
          if (!BeeUtils.isEmpty(property)) {
            boolean userMode = BeeUtils.toBoolean(attributes.get(UiConstants.ATTR_USER_MODE));
            cellSource = CellSource.forProperty(property, BeeKeeper.getUser().idOrNull(userMode),
                ValueType.TEXT);

          } else if (column != null) {
            int columnIndex = DataUtils.getColumnIndex(column.getId(), columns);
            if (!BeeConst.isUndef(columnIndex)) {
              cellSource = CellSource.forColumn(column, columnIndex);
            }
          }

          widget = new MultiSelector(relation, true, cellSource);

          String separators = attributes.get(MultiSelector.ATTR_SEPARATORS);
          if (BeeUtils.hasLength(separators)) {
            ((MultiSelector) widget).setSeparators(separators);
          }
        }
        break;

      case ORDERED_LIST:
        widget = new HtmlList(true);
        break;

      case PROGRESS:
        if (Features.supportsElementProgress()) {
          widget = new Progress();

          max = attributes.get(ATTR_MAX);
          if (BeeUtils.isPositiveDouble(max)) {
            ((Progress) widget).setMax(BeeUtils.toDouble(max));
          }

          String value = attributes.get(UiConstants.ATTR_VALUE);
          if (BeeUtils.isNonNegativeDouble(value)) {
            ((Progress) widget).setValue(BeeUtils.toDouble(value));
          }
        }
        break;

      case RADIO:
        widget = new RadioGroup(BeeUtils.toBoolean(attributes.get(ATTR_VERTICAL))
            ? Orientation.VERTICAL : Orientation.HORIZONTAL);
        break;

      case RELATIONS:
        Collection<Relation> relations = new ArrayList<>();

        for (Element child : children) {
          if (BeeUtils.same(XmlUtils.getLocalName(child), "Relation")) {
            relation = createRelation(null, XmlUtils.getAttributes(child),
                XmlUtils.getChildrenElements(child), Relation.RenderMode.SOURCE);

            if (relation != null) {
              if (widgetInterceptor != null) {
                widgetInterceptor.configureRelation(name, relation);
              }
              relations.add(relation);
            }
          }
        }
        widget = new Relations(attributes.get(UiConstants.ATTR_REL_COLUMN),
            BeeUtils.toBoolean(attributes.get(ATTR_INLINE)), relations,
            NameUtils.toList(attributes.get("defaultRelations")),
            NameUtils.toList(attributes.get("blockedRelations")));
        break;

      case RESIZE_PANEL:
        widget = new ResizePanel();
        break;

      case RICH_TEXT_EDITOR:
        widget = new RichTextEditor(true);
        break;

      case ROW_ID_LABEL:
        format = attributes.get(UiConstants.ATTR_FORMAT);
        inline = BeeUtils.toBoolean(attributes.get(ATTR_INLINE));
        widget = new RowIdLabel(format, inline);
        break;

      case SCROLL_PANEL:
        widget = new Scroll();
        break;

      case SIMPLE_INLINE_PANEL:
        widget = new SimpleInline();
        break;

      case SIMPLE_PANEL:
        widget = new Simple();
        break;

      case SLIDER_BAR:
        min = attributes.get(ATTR_MIN);
        max = attributes.get(ATTR_MAX);
        step = attributes.get(ATTR_STEP);

        if (BeeUtils.isDouble(min) && BeeUtils.isDouble(max) && BeeUtils.isDouble(step)
            && BeeUtils.toLong(min) < BeeUtils.toLong(max) && BeeUtils.isPositiveDouble(step)) {

          double pMin = BeeUtils.toDouble(min);
          double pMax = BeeUtils.toDouble(max);

          widget = new SliderBar((pMin + pMax) / 2, pMin, pMax, BeeUtils.toDouble(step));

          String z = attributes.get(ATTR_NUM_LABELS);
          if (BeeUtils.isDigit(z)) {
            ((SliderBar) widget).setNumLabels(BeeUtils.toInt(z));
          }
          z = attributes.get(ATTR_NUM_TICKS);
          if (BeeUtils.isDigit(z)) {
            ((SliderBar) widget).setNumTicks(BeeUtils.toInt(z));
          }
        }
        break;

      case SPAN_PANEL:
        widget = new Span();
        break;

      case SPLIT_PANEL:
        String ss = attributes.get(UiConstants.ATTR_SPLITTER_SIZE);
        if (BeeUtils.isDigit(ss)) {
          widget = new Split(BeeUtils.toInt(ss));
        } else {
          widget = new Split();
        }
        break;

      case STACK_PANEL:
        widget = new Stack();
        break;

      case SVG:
        if (Features.supportsSvg()) {
          widget = new Svg();
        }
        break;

      case SUMMARY:
        widget = new Summary(html);
        break;

      case SUMMARY_PROXY:
        widget = new SummaryProxy();
        break;

      case TAB_BAR:
        stylePrefix = attributes.get(ATTR_STYLE_PREFIX);
        Orientation orientation = BeeUtils.toBoolean(attributes.get(ATTR_VERTICAL))
            ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        widget = BeeUtils.isEmpty(stylePrefix)
            ? new TabBar(orientation) : new TabBar(stylePrefix, orientation);
        break;

      case TAB_GROUP:
        stylePrefix = attributes.get(ATTR_STYLE_PREFIX);
        widget = new TabGroup(stylePrefix);
        break;

      case TABBED_PAGES:
        stylePrefix = attributes.get(ATTR_STYLE_PREFIX);
        widget = BeeUtils.isEmpty(stylePrefix) ? new TabbedPages() : new TabbedPages(stylePrefix);
        if (attributes.containsKey(ATTR_RESIZABLE)) {
          ((TabbedPages) widget).setResizable(BeeUtils.toBoolean(attributes.get(ATTR_RESIZABLE)));
        }
        break;

      case TABLE:
        widget = new HtmlTable();
        TableKind tableKind = TableKind.parse(attributes.get(ATTR_KIND));
        if (tableKind != null) {
          ((HtmlTable) widget).setKind(tableKind);
        }
        break;

      case TEXT_LABEL:
        widget = new TextLabel(BeeUtils.toBoolean(attributes.get(ATTR_INLINE)));
        if (BeeConst.isTrue(attributes.get(ATTR_TEXT_ONLY))) {
          ((TextLabel) widget).setTextOnly(true);
        }
        break;

      case TOGGLE:
        String upFace = attributes.get(ATTR_UP_FACE);
        String dnFace = attributes.get(ATTR_DOWN_FACE);

        if (BeeUtils.allNotEmpty(upFace, dnFace)) {
          stylePrefix = attributes.get(ATTR_STYLE_PREFIX);
          boolean checked = BeeConst.isTrue(attributes.get(ATTR_CHECKED));

          if (Localized.maybeTranslatable(upFace) && Localized.maybeTranslatable(dnFace)) {
            widget = new Toggle(Localized.maybeTranslate(upFace), Localized.maybeTranslate(dnFace),
                stylePrefix, checked);

          } else {
            FontAwesome faUp = FontAwesome.parse(upFace);
            FontAwesome faDn = FontAwesome.parse(dnFace);

            if (faUp != null && faDn != null) {
              widget = new Toggle(faUp, faDn, stylePrefix, checked);
            } else {
              widget = new Toggle(upFace, dnFace, stylePrefix, checked);
            }
          }

        } else {
          widget = new Toggle();
        }
        break;

      case UNBOUND_SELECTOR:
        relation = createRelation(null, attributes, children, Relation.RenderMode.SOURCE);
        if (relation != null) {
          if (widgetInterceptor != null) {
            widgetInterceptor.configureRelation(name, relation);
          }
          widget = UnboundSelector.create(relation);
        }
        break;

      case UNORDERED_LIST:
        widget = new HtmlList(false);
        break;

      case VERTICAL_PANEL:
        widget = new Vertical();
        break;

      case VIDEO:
        widget = new Video();
        initMedia((Video) widget, attributes);
        break;

      case VOLUME_SLIDER:
        min = attributes.get(ATTR_MIN);
        max = attributes.get(ATTR_MAX);

        if (BeeUtils.isLong(min) && BeeUtils.isLong(max)
            && BeeUtils.toLong(min) < BeeUtils.toLong(max)) {
          step = attributes.get(ATTR_STEP);
          String minStep = attributes.get(ATTR_MIN_STEP);
          String maxStep = attributes.get(ATTR_MAX_STEP);

          long pMin = BeeUtils.toLong(min);
          long pMax = BeeUtils.toLong(max);
          long value = (pMin + pMax) / 2;

          boolean hasStep = BeeUtils.isInt(step) && BeeUtils.toInt(step) > 0;
          boolean hasStepBounds = BeeUtils.isInt(minStep) && BeeUtils.isInt(maxStep)
              && BeeUtils.toInt(minStep) > 0 && BeeUtils.toInt(maxStep) >= BeeUtils.toInt(minStep);

          if (hasStepBounds) {
            widget = new VolumeSlider(value, pMin, pMax,
                BeeUtils.toInt(minStep), BeeUtils.toInt(maxStep));
          } else if (hasStep) {
            widget = new VolumeSlider(value, pMin, pMax, BeeUtils.toInt(step));
          } else {
            widget = new VolumeSlider(value, pMin, pMax);
          }
        }
        break;

      case TREE:
        widget = new Tree(attributes.get(UiConstants.ATTR_CAPTION));
        break;

      case DATA_TREE:
        String treeViewName = attributes.get(UiConstants.ATTR_VIEW_NAME);
        String treeFavoriteName = attributes.get(UiConstants.ATTR_FAVORITE);
        Element editForm = XmlUtils.getFirstChildElement(element, "form");

        widget = new TreeContainer(attributes.get(UiConstants.ATTR_CAPTION),
            editForm != null ? Action.parse(attributes.get(FormDescription.ATTR_DISABLED_ACTIONS))
                : EnumSet.allOf(Action.class), treeViewName, treeFavoriteName);

        ((TreeView) widget).setViewPresenter(TreePresenter.create(name, (TreeView) widget,
            treeViewName, attributes, XmlUtils.getCalculation(element, TAG_CALC), editForm));
        break;

      case DECORATOR:
        String id = attributes.get(ATTR_ID);
        if (!BeeUtils.isEmpty(id) && children.size() == 1) {
          IdentifiableWidget child = createIfWidget(formName, children.get(0), viewName, columns,
              widgetDescriptionCallback, widgetInterceptor);
          if (child != null) {
            widget = TuningFactory.decorate(id, element, child,
                widgetDescriptionCallback.getLastWidgetDescription());
          }
          children.clear();
          attributes.clear();
        }
        break;
    }

    if (widget == null) {
      widgetDescriptionCallback.onFailure("cannot create widget", getTagName());
      return null;
    }

    WidgetDescription widgetDescription = new WidgetDescription(this, widget.getId(), name);

    if (relation != null) {
      widgetDescription.setRelation(relation);
    }

    boolean disablable = widget instanceof EnablableWidget;

    if (!attributes.isEmpty()) {
      if (column != null) {
        String enumKey = column.getEnumKey();

        if (!BeeUtils.isEmpty(enumKey)) {
          if (widget instanceof AcceptsCaptions) {
            ((AcceptsCaptions) widget).setCaptions(enumKey);
          }
        }
        widgetDescription.setEnumKey(enumKey);
      }

      setAttributes(widget, attributes);
      widgetDescription.setAttributes(attributes);

      if (disablable && BeeConst.isFalse(attributes.get(ATTR_DISABLABLE))) {
        disablable = false;
      }

      if (column != null) {
        if (Data.isColumnReadOnly(viewName, column)) {
          widgetDescription.setReadOnly(true);
        }

        if (isInput() && widget instanceof HasMaxLength
            && !attributes.containsKey(ATTR_MAX_LENGTH)) {
          int maxLength = UiHelper.getMaxLength(column);
          if (maxLength > 0) {
            int defMaxLength = ((HasMaxLength) widget).getMaxLength();
            if (defMaxLength <= 0 || maxLength < defMaxLength) {
              ((HasMaxLength) widget).setMaxLength(maxLength);
            }
          }
        }

        if (widget instanceof HasBounds) {
          UiHelper.setDefaultBounds((HasBounds) widget, column);
        }

      } else if (isLabel() && attributes.containsKey(UiConstants.ATTR_FOR)) {
        BeeColumn forColumn = getColumn(columns, attributes, UiConstants.ATTR_FOR);
        if (forColumn != null) {
          associateLabel(widget, forColumn);
        }
      }

      if (AutocompleteProvider.isAutocompleteCandidate(widget)) {
        AutocompleteProvider.maybeEnableAutocomplete(widget, attributes,
            formName, name, viewName, (column == null) ? null : column.getId());
      }
    }

    widgetDescription.setDisablable(disablable);

    List<ConditionalStyleDeclaration> dynStyles = new ArrayList<>();
    Calculation calc;

    if (!children.isEmpty()) {
      for (Element child : children) {
        String childTag = XmlUtils.getLocalName(child);

        if (BeeUtils.same(childTag, CustomProperties.TAG_PROPERTIES)) {
          DomUtils.setDataProperties(widget.getElement(), XmlUtils.getAttributes(child));

        } else if (BeeUtils.same(childTag, TAG_CSS)) {
          Global.addStyleSheet(child.getAttribute(ATTR_ID), XmlUtils.getText(child));

        } else if (BeeUtils.same(childTag, ConditionalStyleDeclaration.TAG_DYN_STYLE)) {
          ConditionalStyleDeclaration csd = XmlUtils.getConditionalStyle(child);
          if (csd != null) {
            dynStyles.add(csd);
          }

        } else if (BeeUtils.same(childTag, TAG_HANDLER)) {
          addHandler(widget, child.getAttribute(ATTR_EVENT), XmlUtils.getText(child));

        } else if (BeeUtils.same(childTag, RendererDescription.TAG_RENDERER)) {
          RendererDescription rendererDescription = XmlUtils.getRendererDescription(child);
          if (rendererDescription != null) {
            widgetDescription.setRendererDescription(rendererDescription);
          }
        } else if (BeeUtils.same(childTag, RendererDescription.TAG_RENDER)) {
          Calculation render = XmlUtils.getCalculation(child);
          if (render != null) {
            widgetDescription.setRender(render);
          }
        } else if (BeeUtils.same(childTag, RenderableToken.TAG_RENDER_TOKEN)) {
          RenderableToken token = RenderableToken.create(XmlUtils.getAttributes(child));
          if (token != null) {
            widgetDescription.addRenderToken(token);
          }

        } else if (BeeUtils.same(childTag, TAG_EDITABLE)) {
          calc = XmlUtils.getCalculation(child);
          if (calc != null) {
            widgetDescription.setEditable(calc);
          }

        } else if (BeeUtils.same(childTag, TAG_VALIDATION)) {
          calc = XmlUtils.getCalculation(child);
          if (calc != null) {
            widgetDescription.setValidation(calc);
          }

        } else if (BeeUtils.same(childTag, TAG_CARRY)) {
          calc = XmlUtils.getCalculation(child);
          if (calc != null) {
            widgetDescription.setCarry(calc);
          }

        } else {
          processChild(formName, widget, child, viewName, columns, widgetDescriptionCallback,
              widgetInterceptor);
        }
      }
    }

    if (!dynStyles.isEmpty()) {
      widgetDescription.setConditionalStyle(ConditionalStyle.create(dynStyles,
          widgetDescription.getSource(), columns));
    }

    if (!attributes.isEmpty()) {
      if (this == LIST_BOX && widget instanceof ListBox) {
        int z = BeeUtils.toInt(attributes.get(ATTR_MIN_SIZE));
        if (z > 0) {
          ((ListBox) widget).setMinSize(z);
        }
        z = BeeUtils.toInt(attributes.get(ATTR_MAX_SIZE));
        if (z > 0) {
          ((ListBox) widget).setMaxSize(z);
        }

        int cnt;
        if (BeeUtils.toBoolean(attributes.get(ATTR_ALL_ITEMS_VISIBLE))) {
          cnt = ((ListBox) widget).getItemCount();
        } else {
          cnt = BeeUtils.toInt(attributes.get(UiConstants.ATTR_SIZE));
        }
        if (cnt > 0) {
          ((ListBox) widget).setVisibleItemCount(cnt);
        } else {
          ((ListBox) widget).updateSize();
        }
      }

      if (widgetDescription.getConditionalStyle() == null) {
        String bgSource = attributes.get(UiConstants.ATTR_BACKGROUND_SOURCE);
        String fgSource = attributes.get(UiConstants.ATTR_FOREGROUND_SOURCE);

        if (BeeUtils.anyNotEmpty(bgSource, fgSource)) {
          StyleProvider styleProvider = ColorStyleProvider.create(columns, bgSource, fgSource);
          if (styleProvider != null) {
            widgetDescription.setConditionalStyle(ConditionalStyle.create(styleProvider));
          }
        }
      }
    }

    if (widgetInterceptor != null) {
      widgetInterceptor.afterCreateWidget(name, widget, widgetDescriptionCallback);
    }
    if (widget instanceof Launchable) {
      ((Launchable) widget).launch();
    }

    widgetDescriptionCallback.onSuccess(widgetDescription, widget);

    String decoratorId = attributes.containsKey(ATTR_DECORATOR)
        ? attributes.get(ATTR_DECORATOR) : attributes.get(ATTR_DEFAULT_DECORATOR);
    if (!BeeUtils.isEmpty(decoratorId)) {
      IdentifiableWidget decorated = TuningFactory.decorate(decoratorId, element, widget,
          widgetDescription);
      if (decorated != null) {
        return decorated;
      }
    }

    return widget;
  }

  public String getTagName() {
    return tagName;
  }

  public boolean isChild() {
    return hasType(Type.IS_CHILD);
  }

  public boolean isDecorator() {
    return hasType(Type.IS_DECORATOR);
  }

  public boolean isDisplay() {
    return hasType(Type.DISPLAY);
  }

  public boolean isEditable() {
    return hasType(Type.EDITABLE);
  }

  public boolean isFocusable() {
    return hasType(Type.FOCUSABLE);
  }

  public boolean isGrid() {
    return hasType(Type.IS_GRID);
  }

  public boolean isInput() {
    return hasType(Type.INPUT);
  }

  private HeaderAndContent createHeaderAndContent(String formName, Element parent, String viewName,
      List<BeeColumn> columns, WidgetDescriptionCallback wdcb,
      WidgetInterceptor widgetInterceptor) {

    String headerTag = null;
    String headerString = null;
    IdentifiableWidget headerWidget = null;

    IdentifiableWidget content = null;

    for (Element child : XmlUtils.getChildrenElements(parent)) {
      String childTag = XmlUtils.getLocalName(child);

      if (BeeUtils.same(childTag, TAG_TEXT)) {
        String text = Localized.maybeTranslate(XmlUtils.getText(child));
        if (!BeeUtils.isEmpty(text)) {
          headerTag = TAG_TEXT;
          headerString = text;
        }
        continue;
      }

      if (BeeUtils.same(childTag, TAG_HTML)) {
        String html = XmlUtils.getText(child);
        if (!BeeUtils.isEmpty(html)) {
          headerTag = TAG_HTML;
          headerString = html;
        }
        continue;
      }

      IdentifiableWidget w = createIfWidget(formName, child, viewName, columns, wdcb,
          widgetInterceptor);
      if (w == null) {
        continue;
      }

      if (headerTag == null && headerWidget == null) {
        headerWidget = w;
      } else {
        content = w;
        break;
      }
    }

    return new HeaderAndContent(headerTag, headerString, headerWidget, content);
  }

  private Set<Type> getTypes() {
    return types;
  }

  private boolean hasChildren() {
    return hasType(Type.HAS_CHILDREN);
  }

  private boolean hasLayers() {
    return hasType(Type.HAS_LAYERS);
  }

  private boolean hasOneChild() {
    return hasType(Type.HAS_ONE_CHILD);
  }

  private boolean hasType(Type type) {
    if (type == null || getTypes() == null) {
      return false;
    }
    return getTypes().contains(type);
  }

  private boolean isCellVector() {
    return hasType(Type.CELL_VECTOR);
  }

  private boolean isLabel() {
    return hasType(Type.IS_LABEL);
  }

  private boolean isTable() {
    return hasType(Type.IS_TABLE);
  }

  private void processChild(String formName, IdentifiableWidget parent, Element child,
      String viewName, List<BeeColumn> columns,
      WidgetDescriptionCallback wdcb, WidgetInterceptor widgetInterceptor) {

    String childTag = XmlUtils.getLocalName(child);

    if (hasLayers()) {
      if (BeeUtils.same(childTag, TAG_LAYER) && parent instanceof HasWidgets) {
        IdentifiableWidget w = createOneChild(formName, child, viewName, columns, wdcb,
            widgetInterceptor);

        if (w != null) {
          ((HasWidgets) parent).add(w.asWidget());

          Edges edges = getEdges(child);
          Dimensions dimensions = XmlUtils.getDimensions(child);

          if (parent instanceof LayoutPanel) {
            ((LayoutPanel) parent).setHorizontalLayout(w.asWidget(),
                edges.getLeftValue(), edges.getLeftUnit(),
                edges.getRightValue(), edges.getRightUnit(),
                dimensions.getWidthValue(), dimensions.getWidthUnit());
            ((LayoutPanel) parent).setVerticalLayout(w.asWidget(),
                edges.getTopValue(), edges.getTopUnit(),
                edges.getBottomValue(), edges.getBottomUnit(),
                dimensions.getHeightValue(), dimensions.getHeightUnit());
          } else {
            if (!edges.isEmpty()) {
              StyleUtils.makeAbsolute(w.asWidget());
              edges.applyPosition(w.asWidget());
            }
            if (!dimensions.isEmpty()) {
              dimensions.applyTo(w.asWidget());
            }
          }
        }
      }

    } else if (isTable() && parent instanceof HtmlTable) {
      HtmlTable table = (HtmlTable) parent;

      if (BeeUtils.same(childTag, UiConstants.TAG_COL)) {
        String idx = child.getAttribute(ATTR_INDEX);
        if (BeeUtils.isDigit(idx)) {
          int c = BeeUtils.toInt(idx);

          Double width = XmlUtils.getAttributeDouble(child, HasDimensions.ATTR_WIDTH);
          if (BeeUtils.isPositive(width)) {
            table.getColumnFormatter().setWidth(c, width,
                XmlUtils.getAttributeUnit(child, HasDimensions.ATTR_WIDTH_UNIT, CssUnit.PX));
          }

          StyleUtils.updateAppearance(table.getColumnFormatter().getElement(c),
              child.getAttribute(UiConstants.ATTR_CLASS),
              child.getAttribute(UiConstants.ATTR_STYLE));

          String classes = child.getAttribute(ATTR_CELL_CLASS);
          if (!BeeUtils.isEmpty(classes)) {
            table.setColumnCellClasses(c, classes);
          }

          String kind = child.getAttribute(ATTR_KIND);
          if (!BeeUtils.isEmpty(kind)) {
            CellKind cellKind = CellKind.parse(kind);
            if (cellKind != null) {
              table.setColumnCellKind(c, cellKind);
            }
          }

          String styles = child.getAttribute(ATTR_CELL_STYLE);
          if (!BeeUtils.isEmpty(styles)) {
            table.setColumnCellStyles(c, styles);
          }
        }

      } else if (BeeUtils.same(childTag, UiConstants.TAG_ROW)) {
        int r = table.getRowCount();
        int c = 0;

        for (Element cell : XmlUtils.getChildrenElements(child)) {
          if (XmlUtils.tagIs(cell, UiConstants.TAG_CELL)) {
            for (Element cellContent : XmlUtils.getChildrenElements(cell)) {
              if (createTableCell(table, formName, cellContent, r, c, viewName, columns, wdcb,
                  widgetInterceptor)) {
                break;
              }
            }
            setTableCellAttributes(table, cell, r, c);
            c++;
          } else if (createTableCell(table, formName, cell, r, c, viewName, columns, wdcb,
              widgetInterceptor)) {
            c++;
          }
        }

        if (c > 0) {
          setTableRowAttributes(table, child, r);
        }

      } else {
        createTableCell(table, formName, child, table.getRowCount(), 0, viewName, columns, wdcb,
            widgetInterceptor);
      }

    } else if (isCellVector() && parent instanceof HasWidgets) {
      IdentifiableWidget w = null;

      if (BeeUtils.same(childTag, UiConstants.TAG_CELL)) {
        for (Element cellContent : XmlUtils.getChildrenElements(child)) {
          w = createIfWidgetOrHtmlOrText(formName, cellContent, viewName, columns, wdcb,
              widgetInterceptor);
          if (w != null) {
            break;
          }
        }
      } else {
        w = createIfWidgetOrHtmlOrText(formName, child, viewName, columns, wdcb, widgetInterceptor);
      }

      if (w != null) {
        ((HasWidgets) parent).add(w.asWidget());
        if (parent instanceof CellVector && BeeUtils.same(childTag, UiConstants.TAG_CELL)) {
          setVectorCellAttributes((CellVector) parent, child, w);
        }
      }

    } else if (hasOneChild()) {
      IdentifiableWidget w = createIfWidget(formName, child, viewName, columns, wdcb,
          widgetInterceptor);
      if (w != null && parent instanceof HasOneWidget) {
        ((HasOneWidget) parent).setWidget(w);
      }

    } else if (hasChildren()) {
      IdentifiableWidget w = createIfWidget(formName, child, viewName, columns, wdcb,
          widgetInterceptor);
      if (w != null && parent instanceof HasWidgets) {
        ((HasWidgets) parent).add(w.asWidget());
      }

    } else if (this == SPLIT_PANEL) {
      IdentifiableWidget w = createOneChild(formName, child, viewName, columns, wdcb,
          widgetInterceptor);
      if (w != null && parent instanceof Split) {
        Direction direction = EnumUtils.getEnumByName(Direction.class, childTag);

        if (direction == Direction.CENTER) {
          ((Split) parent).add(w);

        } else if (Split.validDirection(direction, false)) {
          Integer size = XmlUtils.getAttributeInteger(child, UiConstants.ATTR_SIZE);
          Integer splitterSize =
              XmlUtils.getAttributeInteger(child, UiConstants.ATTR_SPLITTER_SIZE);

          if (BeeUtils.isPositive(size)) {
            ((Split) parent).add(w, direction, size, splitterSize);
          }
        }
      }

    } else if (this == STACK_PANEL && BeeUtils.same(childTag, TAG_STACK)) {
      Integer headerSize = XmlUtils.getAttributeInteger(child, ATTR_HEADER_SIZE);
      HeaderAndContent hc = createHeaderAndContent(formName, child, viewName, columns, wdcb,
          widgetInterceptor);

      if (BeeUtils.isPositive(headerSize) && hc != null && hc.isValid()
          && parent instanceof Stack) {

        IdentifiableWidget header;

        if (hc.isHeaderText() || hc.isHeaderHtml()) {
          header = ((Stack) parent).add(hc.getContent().asWidget(), hc.getHeaderString(),
              headerSize);
        } else {
          header = ((Stack) parent).add(hc.getContent().asWidget(),
              hc.getHeaderWidget().asWidget(), headerSize);
        }

        StyleUtils.updateAppearance(header.getElement(),
            child.getAttribute(UiConstants.ATTR_CLASS),
            child.getAttribute(UiConstants.ATTR_STYLE));
      }

    } else if (this == TABBED_PAGES && BeeUtils.same(childTag, TAG_PAGE)) {
      HeaderAndContent hc = createHeaderAndContent(formName, child, viewName, columns, wdcb,
          widgetInterceptor);

      if (hc != null && hc.isValid() && parent instanceof TabbedPages) {
        IdentifiableWidget tab;

        Widget content = hc.getContent().asWidget();
        Collection<HasSummaryChangeHandlers> sources = SummaryChangeEvent.findSources(content);

        String predicate = child.getAttribute(ATTR_PREDICATE);
        Evaluator rowPredicate = BeeUtils.isEmpty(predicate)
            ? null : Evaluator.create(new Calculation(predicate, null), null, columns);

        if (hc.isHeaderText() || hc.isHeaderHtml()) {
          tab = ((TabbedPages) parent).add(content, hc.getHeaderString(), null, sources,
              rowPredicate);
        } else {
          tab = ((TabbedPages) parent).add(content, hc.getHeaderWidget().asWidget(), null, sources,
              rowPredicate);
        }

        StyleUtils.updateAppearance(tab.getElement(), child.getAttribute(UiConstants.ATTR_CLASS),
            child.getAttribute(UiConstants.ATTR_STYLE));

        String key = child.getAttribute(UiConstants.ATTR_KEY);
        if (!BeeUtils.isEmpty(key)) {
          DomUtils.setDataKey(tab.getElement(), key);
        }
      }

    } else if (this == RADIO && BeeUtils.same(childTag, TAG_OPTION)) {
      String opt = XmlUtils.getText(child);
      if (!BeeUtils.isEmpty(opt) && parent instanceof RadioGroup) {
        ((RadioGroup) parent).addOption(Localized.maybeTranslate(opt));
      }

    } else if (BeeUtils.same(childTag, HasItems.TAG_ITEM) && parent instanceof HasItems) {
      String text = Localized.maybeTranslate(XmlUtils.getText(child));
      if (!BeeUtils.isEmpty(text)) {
        if (parent instanceof ListBox && child.hasAttribute(UiConstants.ATTR_VALUE)) {
          String value = child.getAttribute(UiConstants.ATTR_VALUE);
          ((ListBox) parent).addItem(text, value);
        } else {
          ((HasItems) parent).addItem(text);
        }
      }

    } else if (this == HEADER_CONTENT_FOOTER) {
      IdentifiableWidget w = createOneChild(formName, child, viewName, columns, wdcb,
          widgetInterceptor);
      if (w != null && parent instanceof HeaderContentFooter) {
        if (BeeUtils.same(childTag, TAG_HEADER)) {
          ((HeaderContentFooter) parent).setHeaderWidget(w.asWidget());
        } else if (BeeUtils.same(childTag, TAG_CONTENT)) {
          ((HeaderContentFooter) parent).setContentWidget(w.asWidget());
        } else if (BeeUtils.same(childTag, TAG_FOOTER)) {
          ((HeaderContentFooter) parent).setFooterWidget(w.asWidget());
        }
      }

    } else if (this == TREE && parent instanceof Tree) {
      processTree((Tree) parent, child);

    } else if ((this == TAB_BAR || this == TAB_GROUP) && parent instanceof TabGroup
        && BeeUtils.same(childTag, TAG_TAB)) {

      for (Element tabContent : XmlUtils.getChildrenElements(child)) {
        if (XmlUtils.tagIs(tabContent, TAG_TEXT)) {
          String text = Localized.maybeTranslate(XmlUtils.getText(tabContent));
          if (!BeeUtils.isEmpty(text)) {
            ((TabGroup) parent).addItem(text);
            break;
          }
        }

        if (XmlUtils.tagIs(tabContent, TAG_HTML)) {
          String html = XmlUtils.getText(tabContent);
          if (!BeeUtils.isEmpty(html)) {
            ((TabGroup) parent).addItem(html);
            break;
          }
        }

        IdentifiableWidget w = createIfWidget(formName, tabContent, viewName, columns, wdcb,
            widgetInterceptor);
        if (w != null) {
          ((TabGroup) parent).addItem(w.asWidget());
          break;
        }
      }
    }
  }

  private static void processTree(HasTreeItems parent, Element child) {
    if (!XmlUtils.tagIs(child, TAG_TREE_ITEM)) {
      return;
    }
    TreeItem item = new TreeItem(getTextOrHtml(child));
    parent.addItem(item);

    for (Element chld : XmlUtils.getChildrenElements(child)) {
      processTree(item, chld);
    }
  }
}
