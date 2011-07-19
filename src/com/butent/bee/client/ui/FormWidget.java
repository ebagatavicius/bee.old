package com.butent.bee.client.ui;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import com.butent.bee.client.composite.InputDate;
import com.butent.bee.client.composite.RadioGroup;
import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.StyleUtils;
import com.butent.bee.client.dom.StyleUtils.ScrollBars;
import com.butent.bee.client.grid.FlexTable;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.HasDateTimeFormat;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.layout.BeeLayoutPanel;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.HeaderContentFooter;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.ResizePanel;
import com.butent.bee.client.layout.Scroll;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.SimpleInline;
import com.butent.bee.client.layout.Span;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.layout.Stack;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.layout.Vertical;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.widget.BeeButton;
import com.butent.bee.client.widget.BeeCheckBox;
import com.butent.bee.client.widget.BeeFrame;
import com.butent.bee.client.widget.BeeImage;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.BeeListBox;
import com.butent.bee.client.widget.DateLabel;
import com.butent.bee.client.widget.DateTimeLabel;
import com.butent.bee.client.widget.DecimalLabel;
import com.butent.bee.client.widget.DoubleLabel;
import com.butent.bee.client.widget.Html;
import com.butent.bee.client.widget.InlineHtml;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputInteger;
import com.butent.bee.client.widget.InputLong;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.IntegerLabel;
import com.butent.bee.client.widget.Link;
import com.butent.bee.client.widget.LongLabel;
import com.butent.bee.client.widget.Meter;
import com.butent.bee.client.widget.Progress;
import com.butent.bee.client.widget.SimpleBoolean;
import com.butent.bee.client.widget.Svg;
import com.butent.bee.client.widget.TextLabel;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasService;
import com.butent.bee.shared.HasStage;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum FormWidget {
  ABSOLUTE_PANEL("AbsolutePanel", EnumSet.of(Type.HAS_LAYERS)),
  BUTTON("Button", EnumSet.of(Type.FOCUSABLE, Type.DISPLAY)),
  CANVAS("Canvas", EnumSet.of(Type.DISPLAY, Type.SPECIFIC)),
  CHECK_BOX("CheckBox", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE)),
  COMPLEX_PANEL("ComplexPanel", EnumSet.of(Type.HAS_LAYERS)),
  DATE_LABEL("DateLabel", EnumSet.of(Type.DISPLAY)),
  DATE_TIME_LABEL("DateTimeLabel", EnumSet.of(Type.DISPLAY)),
  DECIMAL_LABEL("DecimalLabel", EnumSet.of(Type.DISPLAY, Type.HAS_SCALE)),
  DOUBLE_LABEL("DoubleLabel", EnumSet.of(Type.DISPLAY)),
  FLEX_TABLE("FlexTable", EnumSet.of(Type.TABLE)),
  FLOW_PANEL("FlowPanel", EnumSet.of(Type.HAS_CHILDREN)),
  FRAME("Frame", EnumSet.of(Type.HAS_DIMENSION)),
  GRID("Grid", EnumSet.of(Type.IS_GRID)),
  HEADER_CONTENT_FOOTER("HeaderContentFooter", EnumSet.of(Type.SPECIFIC)),
  HORIZONTAL_PANEL("HorizontalPanel", EnumSet.of(Type.CELL_VECTOR)),
  HYPERLINK("Hyperlink", EnumSet.of(Type.FOCUSABLE, Type.DISPLAY)),
  HTML("Html", EnumSet.of(Type.IS_HTML)),
  IMAGE("Image", EnumSet.of(Type.FOCUSABLE, Type.DISPLAY)),
  INLINE_HTML("InlineHtml", EnumSet.of(Type.IS_HTML)),
  INLINE_HYPERLINK("InlineHyperlink", EnumSet.of(Type.FOCUSABLE)),
  INLINE_LABEL("InlineLabel", EnumSet.of(Type.IS_LABEL)),
  INPUT_AREA("InputArea", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_CURRENCY("InputCurrency", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_DATE("InputDate", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_DATE_TIME("InputDateTime", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_DECIMAL("InputDecimal", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_DOUBLE("InputDouble", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_INTEGER("InputInteger", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_LONG("InputLong", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INPUT_TEXT("InputText", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.INPUT)),
  INTEGER_LABEL("IntegerLabel", EnumSet.of(Type.DISPLAY)),
  LABEL("Label", EnumSet.of(Type.IS_LABEL)),
  LAYOUT_PANEL("LayoutPanel", EnumSet.of(Type.HAS_LAYERS)),
  LINK("Link", EnumSet.of(Type.FOCUSABLE, Type.DISPLAY)),
  LIST_BOX("ListBox", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.SPECIFIC)),
  LONG_LABEL("LongLabel", EnumSet.of(Type.DISPLAY)),
  METER("Meter", EnumSet.of(Type.DISPLAY, Type.SPECIFIC)),
  PROGRESS("Progress", EnumSet.of(Type.DISPLAY, Type.SPECIFIC)),
  RADIO("Radio", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE, Type.SPECIFIC)),
  RESIZE_PANEL("ResizePanel", EnumSet.of(Type.HAS_ONE_CHILD)),
  SCROLL_PANEL("ScrollPanel", EnumSet.of(Type.HAS_ONE_CHILD, Type.SPECIFIC)),
  SIMPLE_INLINE_PANEL("SimpleInlinePanel", EnumSet.of(Type.HAS_ONE_CHILD)),
  SIMPLE_PANEL("SimplePanel", EnumSet.of(Type.HAS_ONE_CHILD)),
  SLIDER("Slider", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE)),
  SPAN_PANEL("SpanPanel", EnumSet.of(Type.HAS_CHILDREN)),
  SPINNER("Spinner", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE)),
  SPLIT_PANEL("SplitPanel", EnumSet.of(Type.SPECIFIC)),
  STACK_PANEL("StackPanel", EnumSet.of(Type.SPECIFIC)),
  SVG("Svg", EnumSet.of(Type.DISPLAY, Type.SPECIFIC)),
  TABBED_PAGES("TabbedPages", EnumSet.of(Type.SPECIFIC)),
  TEXT_LABEL("TextLabel", EnumSet.of(Type.DISPLAY)),
  TOGGLE("Toggle", EnumSet.of(Type.FOCUSABLE, Type.EDITABLE ,Type.SPECIFIC)),
  VERTICAL_PANEL("VerticalPanel", EnumSet.of(Type.CELL_VECTOR));
  
  private enum Type {
    FOCUSABLE, EDITABLE, IS_LABEL, DISPLAY, HAS_ONE_CHILD, HAS_CHILDREN, HAS_LAYERS,
    HAS_SCALE, TABLE, HAS_DIMENSION, IS_GRID, SPECIFIC, CELL_VECTOR, IS_HTML, INPUT
  }
  
  private static final String ATTR_CLASS = "class";
  private static final String ATTR_STYLE = "style";

  private static final String ATTR_HTML = "html";
  private static final String ATTR_TITLE = "title";
  private static final String ATTR_VISIBLE = "visible";

  private static final String ATTR_FORMAT = "format";
  private static final String ATTR_SCALE = "scale";
  private static final String ATTR_HORIZONTAL_ALIGNMENT = "horizontalAlignment";
  private static final String ATTR_VERTICAL_ALIGNMENT = "verticalAlignment";

  private static final String ATTR_URL = "url";
  private static final String ATTR_TARGET = "target";
  private static final String ATTR_SERVICE = "service";
  private static final String ATTR_STAGE = "stage";
  private static final String ATTR_UNIT = "unit";
  private static final String ATTR_TAB_INDEX = "tabIndex";

  private static final String ATTR_WIDTH = "width";
  private static final String ATTR_WIDTH_UNIT = "widthUnit";
  private static final String ATTR_HEIGHT = "height";
  private static final String ATTR_HEIGHT_UNIT = "heightUnit";
  private static final String ATTR_LEFT = "left";
  private static final String ATTR_LEFT_UNIT = "leftUnit";

  private static final String ATTR_RIGHT = "right";
  private static final String ATTR_RIGHT_UNIT = "rightUnit";
  private static final String ATTR_TOP = "top";
  private static final String ATTR_TOP_UNIT = "topUnit";
  private static final String ATTR_BOTTOM = "bottom";
  private static final String ATTR_BOTTOM_UNIT = "bottomUnit";
  
  private static final String ATTR_COL_SPAN = "colSpan";
  private static final String ATTR_ROW_SPAN = "rowSpan";
  private static final String ATTR_CELL_PADDING = "cellPadding";
  private static final String ATTR_CELL_SPACING = "cellSpacing";
  private static final String ATTR_BORDER_WIDTH = "borderWidth";

  private static final String ATTR_SCROLL_BARS = "scrollBars";
  private static final String ATTR_SPLITTER_SIZE = "splitterSize";
  private static final String ATTR_SIZE = "size";

  private static final String ATTR_BAR_HEIGHT = "barHeight";
  private static final String ATTR_BAR_UNIT = "barUnit";
  private static final String ATTR_HEADER_SIZE = "headerSize";

  private static final String ATTR_NAME = "name";
  private static final String ATTR_VERTICAL = "vertical";

  private static final String ATTR_MULTI_SELECT = "multiSelect";
  private static final String ATTR_ALL_ITEMS_VISIBLE = "allItemsVisible";
  private static final String ATTR_VISIBLE_ITEM_COUNT = "visibleItemCount";

  private static final String ATTR_SOURCE = "source";
  
  private static final String TAG_DYN_STYLE = "dynStyle";
  private static final String TAG_CALC = "calc";

  private static final String TAG_ITEM = "item";
  private static final String TAG_LAYER = "layer";
  private static final String TAG_ROW = "row";
  private static final String TAG_CELL = "cell";
  private static final String TAG_HEADER = "header";
  private static final String TAG_STACK = "stack";
  private static final String TAG_PAGE = "page";
  private static final String TAG_OPTION = "option";

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
  public static boolean isFormWidget(String tagName) {
    return getByTagName(tagName) != null;
  }
  
  private final String tagName;
  private final Set<Type> types;

  private FormWidget(String tagName, Set<Type> types) {
    this.tagName = tagName;
    this.types = types;
  }
  
  public Widget create(Element description) {
    Assert.notNull(description);
    if (BeeUtils.isFalse(XmlUtils.getAttributeBoolean(description, ATTR_VISIBLE))) {
      return null;
    }
    
    Map<String, String> attributes = XmlUtils.getAttributes(description);
    List<Element> children = XmlUtils.getChildrenElements(description);
    
    String html = attributes.get(ATTR_HTML);
    
    Widget widget = null;

    switch (this) {
      case ABSOLUTE_PANEL:
        widget = new Absolute();
        break;

      case BUTTON:
        widget = new BeeButton(html);
        break;
      
      case CANVAS:
        widget = Canvas.createIfSupported();
        break;
      
      case CHECK_BOX:
        if (BeeUtils.isEmpty(html)) {
          widget = new SimpleBoolean();
        } else {
          widget = new BeeCheckBox(html);
        }
        break;
      
      case COMPLEX_PANEL:
        widget = new Complex();
        break;
      
      case DATE_LABEL:
        widget = new DateLabel();
        break;
      
      case DATE_TIME_LABEL:
        widget = new DateTimeLabel();
        break;

      case DECIMAL_LABEL:
        widget = new DecimalLabel(BeeUtils.toInt(attributes.get(ATTR_SCALE)));
        break;
      
      case DOUBLE_LABEL:
        widget = new DoubleLabel();
        break;
        
      case FLEX_TABLE:
        widget = new FlexTable();
        break;

      case FLOW_PANEL:
        widget = new Flow();
        break;

      case FRAME:
        widget = new BeeFrame(attributes.get(ATTR_URL));
        break;

      case GRID:
        break;
      
      case HEADER_CONTENT_FOOTER:
        widget = new HeaderContentFooter();
        break;

      case HORIZONTAL_PANEL:
        widget = new Horizontal();
        break;

      case HTML:
        widget = new Html(html);
        break;

      case HYPERLINK:
        widget = new Hyperlink(html, attributes.get(ATTR_TARGET));
        break;

      case IMAGE:
        widget = new BeeImage(attributes.get(ATTR_URL));
        break;
        
      case INLINE_HTML:
        widget = new InlineHtml(html);
        break;

      case INLINE_HYPERLINK:
        widget = new InlineHyperlink(html, attributes.get(ATTR_TARGET));
        break;
        
      case INLINE_LABEL:
        widget = new InlineLabel(html);
        break;

      case INPUT_AREA:
        widget = new InputArea();
        break;

      case INPUT_CURRENCY:
        widget = new InputNumber();
        break;

      case INPUT_DATE:
        widget = new InputDate(ValueType.DATE);
        break;

      case INPUT_DATE_TIME:
        widget = new InputDate(ValueType.DATETIME);
        break;
      
      case INPUT_DECIMAL:
        widget = new InputNumber();
        break;

      case INPUT_DOUBLE:
        widget = new InputNumber();
        break;

      case INPUT_INTEGER:
        widget = new InputInteger();
        break;

      case INPUT_LONG:
        widget = new InputLong();
        break;

      case INPUT_TEXT:
        widget = new InputText();
        break;

      case INTEGER_LABEL:
        widget = new IntegerLabel();
        break;

      case LABEL:
        widget = new BeeLabel(html);
        break;

      case LAYOUT_PANEL:
        widget = new BeeLayoutPanel();
        break;
        
      case LINK:
        String url = attributes.get(ATTR_URL);
        if (!BeeUtils.isEmpty(url)) {
          widget = new Link(html, true, attributes.get(ATTR_URL));
        } else if (!BeeUtils.isEmpty(html)) {
          widget = new Link(html, true);
        } else {
          widget = new Link();
        }
        break;

      case LIST_BOX:
        widget = new BeeListBox(BeeUtils.toBoolean(attributes.get(ATTR_MULTI_SELECT)));
        break;

      case LONG_LABEL:
        widget = new LongLabel();
        break;

      case METER:
        widget = new Meter();
        break;

      case PROGRESS:
        widget = new Progress(BeeUtils.toDouble(attributes.get(DomUtils.ATTRIBUTE_MAX)));
        break;
      
      case RADIO:
        String name = attributes.get(ATTR_NAME);
        if (!BeeUtils.isEmpty(name)) {
          widget = new RadioGroup(name, BeeUtils.toBoolean(attributes.get(ATTR_VERTICAL)));
        }
        break;

      case RESIZE_PANEL:
        widget = new ResizePanel();
        break;
      
      case SCROLL_PANEL:
        widget = new Scroll();
        break;
      
      case SIMPLE_INLINE_PANEL:
        widget = new SimpleInline();
        break;

      case SLIDER:
        break;
      
      case SIMPLE_PANEL:
        widget = new Simple();
        break;
      
      case SPAN_PANEL:
        widget = new Span();
        break;

      case SPINNER:
        break;

      case SPLIT_PANEL:
        String ss = attributes.get(ATTR_SPLITTER_SIZE);
        if (BeeUtils.isDigit(ss)) {
          widget = new Split(BeeUtils.toInt(ss));
        } else {
          widget = new Split();
        }
        break;

      case STACK_PANEL:
        widget = new Stack(StyleUtils.parseUnit(attributes.get(ATTR_UNIT), Unit.PX));
        break;
        
      case SVG:
        widget = new Svg();
        break;

      case TABBED_PAGES:
        widget = new TabbedPages(BeeUtils.toDouble(attributes.get(ATTR_BAR_HEIGHT)),
            StyleUtils.parseUnit(attributes.get(ATTR_BAR_UNIT), Unit.PX));
        break;
        
      case TEXT_LABEL:
        widget = new TextLabel();
        break;

      case TOGGLE:
        widget = new Toggle();
        break;
      
      case VERTICAL_PANEL:
        widget = new Vertical();
        break;
    }
    
    if (widget == null) {
      return null;
    }
    
    if (attributes.size() > 0) {
      setAttributes(widget, attributes);
    }
    
    if (children.size() > 0) {
      for (Element child : children) {
        processChild(widget, child);
      }
    }
    
    if (attributes.size() > 0) {
      if (this == LIST_BOX && widget instanceof BeeListBox) {
        int cnt;
        if (BeeUtils.toBoolean(attributes.get(ATTR_ALL_ITEMS_VISIBLE))) {
          cnt = ((BeeListBox) widget).getItemCount();
        } else {
          cnt = BeeUtils.toInt(attributes.get(ATTR_VISIBLE_ITEM_COUNT));
        }
        if (cnt > 0) {
          ((BeeListBox) widget).setVisibleItemCount(cnt);
        }
      }
    }

    return widget;
  }
  
  private Widget createIfWidget(Element description) {
    if (description == null) {
      return null;
    }
    FormWidget fw = getByTagName(description.getTagName());
    if (fw == null) {
      return null;
    }
    return fw.create(description);
  }

  private Widget createOneChild(Element parent) {
    for (Element child : XmlUtils.getChildrenElements(parent)) {
      Widget widget = createIfWidget(child);
      if (widget != null) {
        return widget;
      }
    }
    return null; 
  }
  
  private Pair<Widget, Widget> createPair(Element parent) {
    Widget first = null;
    Widget second = null;
    
    for (Element child : XmlUtils.getChildrenElements(parent)) {
      Widget w = createIfWidget(child);
      if (w == null) {
        continue;
      }

      if (first == null) {
        first = w;
      } else {
        second = w;
        break;
      }
    }
    
    if (first == null || second == null) {
      return null;
    } else {
      return new Pair<Widget, Widget>(first, second);
    }
  }

  private Dimensions getDimensions(Element description) {
    return new Dimensions(XmlUtils.getAttributeDouble(description, ATTR_WIDTH),
        XmlUtils.getAttributeUnit(description, ATTR_WIDTH_UNIT),
        XmlUtils.getAttributeDouble(description, ATTR_HEIGHT),
        XmlUtils.getAttributeUnit(description, ATTR_HEIGHT_UNIT));
  }
  
  private Edges getEdges(Element description) {
    return new Edges(XmlUtils.getAttributeDouble(description, ATTR_TOP),
        XmlUtils.getAttributeUnit(description, ATTR_TOP_UNIT),
        XmlUtils.getAttributeDouble(description, ATTR_RIGHT),
        XmlUtils.getAttributeUnit(description, ATTR_RIGHT_UNIT),
        XmlUtils.getAttributeDouble(description, ATTR_BOTTOM),
        XmlUtils.getAttributeUnit(description, ATTR_BOTTOM_UNIT),
        XmlUtils.getAttributeDouble(description, ATTR_LEFT),
        XmlUtils.getAttributeUnit(description, ATTR_LEFT_UNIT));
  }

  private String getTagName() {
    return tagName;
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
  
  private boolean isFocusable() {
    return hasType(Type.FOCUSABLE);
  }
  
  private boolean isTable() {
    return hasType(Type.TABLE);
  }

  private void processChild(Widget parent, Element child) {
    String childTag = child.getTagName();
    
    if (BeeUtils.same(childTag, TAG_DYN_STYLE)) {
      return;
    }
    
    if (hasLayers()) {
      if (BeeUtils.same(childTag, TAG_LAYER) && parent instanceof HasWidgets) {
        Widget w = createOneChild(child);

        if (w != null) {
          ((HasWidgets) parent).add(w);

          Edges edges = getEdges(child);
          Dimensions dimensions = getDimensions(child);

          if (parent instanceof BeeLayoutPanel) {
            ((BeeLayoutPanel) parent).setHorizontalLayout(w,
                edges.getLeftValue(), edges.getLeftUnit(),
                edges.getRightValue(), edges.getRightUnit(),
                dimensions.getWidthValue(), dimensions.getWidthUnit());
            ((BeeLayoutPanel) parent).setVerticalLayout(w,
                edges.getTopValue(), edges.getTopUnit(),
                edges.getBottomValue(), edges.getBottomUnit(),
                dimensions.getHeightValue(), dimensions.getHeightUnit());
          } else {
            if (!edges.isEmpty()) {
              edges.applyPosition(w);
            }
            if (!dimensions.isEmpty()) {
              dimensions.applyTo(w);
            }
          }
        }
      }

    } else if (isTable()) {
      if (BeeUtils.same(childTag, TAG_ROW) && parent instanceof HtmlTable) {
        int r = ((HtmlTable) parent).getRowCount();
        int c = 0;

        for (Element cell : XmlUtils.getChildrenElements(child)) {
          if (!BeeUtils.same(cell.getTagName(), TAG_CELL)) {
            continue;
          }
          Widget w = createOneChild(cell);
          if (w != null) {
            ((HtmlTable) parent).setWidget(r, c, w);
          }
          c++;
        }
      }

    } else if (isCellVector()) {
      if (BeeUtils.same(childTag, TAG_CELL) && parent instanceof HasWidgets) {
        Widget w = createOneChild(child);
        if (w != null) {
         ((HasWidgets) parent).add(w);
        }
      }
      
    } else if (hasOneChild()) {
      Widget w = createIfWidget(child);
      if (w != null && parent instanceof HasOneWidget) {
        ((HasOneWidget) parent).setWidget(w);
      }
      
    } else if (hasChildren()) {
      Widget w = createIfWidget(child);
      if (w != null && parent instanceof HasWidgets) {
        ((HasWidgets) parent).add(w);
      }
    
    } else if (this == SPLIT_PANEL) {
      Widget w = createOneChild(child);
      if (w != null && parent instanceof Split) {
        ScrollBars sb = XmlUtils.getAttributeScrollBars(child, ATTR_SCROLL_BARS, ScrollBars.NONE);
        Direction direction = BeeUtils.getConstant(Direction.class, childTag);

        if (direction == Direction.CENTER) {
          ((Split) parent).add(w, sb);
        } else if (Split.validDirection(direction, false)) {
          Integer size = XmlUtils.getAttributeInteger(child, ATTR_SIZE);
          Integer splitterSize = XmlUtils.getAttributeInteger(child, ATTR_SPLITTER_SIZE);
          if (BeeUtils.isPositive(size)) {
            ((Split) parent).add(w, direction, size, sb, splitterSize);
          }
        }
      }
    
    } else if (this == STACK_PANEL && BeeUtils.same(childTag, TAG_STACK)) {
      Double headerSize = XmlUtils.getAttributeDouble(child, ATTR_HEADER_SIZE);
      Pair<Widget,Widget> pair = createPair(child);
      if (BeeUtils.isPositive(headerSize) && pair != null && parent instanceof Stack) {
        ((Stack) parent).add(pair.getB(), pair.getA(), headerSize);
      }

    } else if (this == TABBED_PAGES && BeeUtils.same(childTag, TAG_PAGE)) {
      Pair<Widget,Widget> pair = createPair(child);
      if (pair != null && parent instanceof TabbedPages) {
        ((TabbedPages) parent).add(pair.getB(), pair.getA());
      }

    } else if (this == RADIO && BeeUtils.same(childTag, TAG_OPTION)) {
      String opt = XmlUtils.getText(child);
      if (!BeeUtils.isEmpty(opt) && parent instanceof RadioGroup) {
        ((RadioGroup) parent).addOption(opt, true);
      }

    } else if (this == LIST_BOX && BeeUtils.same(childTag, TAG_ITEM)) {
      String item = XmlUtils.getText(child);
      if (!BeeUtils.isEmpty(item) && parent instanceof BeeListBox) {
        ((BeeListBox) parent).addItem(item);
      }
    }
  }
  
  private void setAttributes(Widget widget, Map<String, String> attributes) {
    for (Map.Entry<String, String> attr : attributes.entrySet()) {
      String name = attr.getKey();
      String value = attr.getValue();
      if (BeeUtils.isEmpty(value)) {
        continue;
      }
      
      if (BeeUtils.same(name, ATTR_CLASS)) {
        widget.addStyleName(value);
      } else if (BeeUtils.same(name, ATTR_STYLE)) {
        StyleUtils.apply(widget.getElement().getStyle(), value);
      } else if (BeeUtils.same(name, ATTR_TITLE)) {
        widget.setTitle(value);

      } else if (BeeUtils.same(name, ATTR_TAB_INDEX)) {
        if (isFocusable() && widget instanceof FocusWidget) {
          ((FocusWidget) widget).setTabIndex(BeeUtils.toInt(value));
        }
      } else if (BeeUtils.same(name, ATTR_FORMAT)) {
        if (widget instanceof HasDateTimeFormat || widget instanceof HasNumberFormat) {
          Format.setFormat(widget, null, value);
        }

      } else if (BeeUtils.same(name, ATTR_CELL_PADDING)) {
        if (isTable() && widget instanceof HtmlTable && BeeUtils.isDigit(value)) {
          ((HtmlTable) widget).setCellPadding(BeeUtils.toInt(value));
        }
      } else if (BeeUtils.same(name, ATTR_CELL_SPACING)) {
        if (isTable() && widget instanceof HtmlTable && BeeUtils.isDigit(value)) {
          ((HtmlTable) widget).setCellSpacing(BeeUtils.toInt(value));
        }

      } else if (BeeUtils.same(name, ATTR_SERVICE)) {
        if (widget instanceof HasService) {
          ((HasService) widget).setService(value);
        }
      } else if (BeeUtils.same(name, ATTR_STAGE)) {
        if (widget instanceof HasStage) {
          ((HasStage) widget).setStage(value);
        }
      }
    }
  }
}
