package com.butent.bee.client.utils;

import com.google.common.collect.Lists;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.CDATASection;
import com.google.gwt.xml.client.Comment;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.ProcessingInstruction;
import com.google.gwt.xml.client.Text;
import com.google.gwt.xml.client.XMLParser;
import com.google.gwt.xml.client.impl.DOMParseException;

import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.style.StyleUtils.ScrollBars;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.ConditionalStyleDeclaration;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.RenderableToken;
import com.butent.bee.shared.ui.RendererDescription;
import com.butent.bee.shared.ui.RendererType;
import com.butent.bee.shared.ui.SelectorColumn;
import com.butent.bee.shared.ui.StyleDeclaration;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.utils.XmlHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains utility functions for working with xml on the client side.
 */

public final class XmlUtils {

  private static final BeeLogger logger = LogUtils.getLogger(XmlUtils.class);

  private static final Map<Short, String> NODE_TYPES = new HashMap<>();

  static {
    NODE_TYPES.put(Node.ELEMENT_NODE, "Element");
    NODE_TYPES.put(Node.ATTRIBUTE_NODE, "Attribute");
    NODE_TYPES.put(Node.TEXT_NODE, "Text");
    NODE_TYPES.put(Node.CDATA_SECTION_NODE, "CDATA Section");
    NODE_TYPES.put(Node.ENTITY_REFERENCE_NODE, "Entity Reference");
    NODE_TYPES.put(Node.ENTITY_NODE, "Entity");
    NODE_TYPES.put(Node.PROCESSING_INSTRUCTION_NODE, "Processing Instruction");
    NODE_TYPES.put(Node.COMMENT_NODE, "Comment");
    NODE_TYPES.put(Node.DOCUMENT_NODE, "Document");
    NODE_TYPES.put(Node.DOCUMENT_TYPE_NODE, "Document Type");
    NODE_TYPES.put(Node.DOCUMENT_FRAGMENT_NODE, "Document Fragment");
    NODE_TYPES.put(Node.NOTATION_NODE, "Notation");
  }

  public static String createString(String rootName, Map<String, String> input) {
    Assert.notEmpty(rootName);
    Assert.notEmpty(input);

    String[] nodes = new String[input.size() * 2];
    int i = 0;

    for (Map.Entry<String, String> entry : input.entrySet()) {
      nodes[i] = entry.getKey();
      nodes[i + 1] = entry.getValue();

      i += 2;
    }

    return transformDocument(createDoc(rootName, nodes));
  }

  public static String createString(String rootName, String... nodes) {
    Assert.notEmpty(rootName);
    Assert.notNull(nodes);
    Assert.parameterCount(nodes.length + 1, 3);

    return transformDocument(createDoc(rootName, nodes));
  }

  public static Boolean getAttributeBoolean(Element element, String name) {
    Assert.notNull(element);
    Assert.notEmpty(name);
    return BeeUtils.toBooleanOrNull(element.getAttribute(name));
  }

  public static Double getAttributeDouble(Element element, String name) {
    Assert.notNull(element);
    Assert.notEmpty(name);
    return BeeUtils.toDoubleOrNull(element.getAttribute(name));
  }

  public static Integer getAttributeInteger(Element element, String name) {
    Assert.notNull(element);
    Assert.notEmpty(name);
    return BeeUtils.toIntOrNull(element.getAttribute(name));
  }

  public static Map<String, String> getAttributes(Element element) {
    return getAttributes(element, false);
  }

  public static Map<String, String> getAttributes(Element element, boolean includeNSDeclaration) {
    Assert.notNull(element);
    Map<String, String> result = new HashMap<>();

    NamedNodeMap attributes = element.getAttributes();
    if (attributes == null || attributes.getLength() <= 0) {
      return result;
    }

    Attr attr;
    for (int i = 0; i < attributes.getLength(); i++) {
      attr = (Attr) attributes.item(i);
      if (includeNSDeclaration || !isNamespaceDeclaration(attr)) {
        result.put(attr.getName(), attr.getValue());
      }
    }
    return result;
  }

  public static ScrollBars getAttributeScrollBars(Element element, String name, ScrollBars def) {
    Assert.notNull(element);
    Assert.notEmpty(name);
    return StyleUtils.parseScrollBars(element.getAttribute(name), def);
  }

  public static CssUnit getAttributeUnit(Element element, String name) {
    return getAttributeUnit(element, name, null);
  }

  public static CssUnit getAttributeUnit(Element element, String name, CssUnit defUnit) {
    Assert.notNull(element);
    Assert.notEmpty(name);
    return CssUnit.parse(element.getAttribute(name), defUnit);
  }

  public static List<Property> getAttrInfo(Attr attr) {
    Assert.notNull(attr);
    return PropertyUtils.createProperties("Name", attr.getName(), "Value", attr.getValue(),
        "Specified", attr.getSpecified());
  }

  public static Calculation getCalculation(Element element) {
    Assert.notNull(element);

    String expr = null;
    String func = null;

    for (Element child : getChildrenElements(element)) {
      String tag = getLocalName(child);
      String text = getText(child);
      if (BeeUtils.isEmpty(text)) {
        continue;
      }

      if (BeeUtils.same(tag, Calculation.TAG_EXPRESSION)) {
        expr = text;
      } else if (BeeUtils.same(tag, Calculation.TAG_FUNCTION)) {
        func = text;
      }
    }
    return new Calculation(expr, func);
  }

  public static Calculation getCalculation(Element parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    Element element = getFirstChildElement(parent, tagName);
    if (element == null) {
      return null;
    }
    return getCalculation(element);
  }

  public static List<Property> getCDATAInfo(CDATASection cdata) {
    Assert.notNull(cdata);
    return PropertyUtils.createProperties("Length", cdata.getLength(), "Data", cdata.getData());
  }

  public static Map<String, String> getChildAttributes(Element parent, String tagName) {
    return getChildAttributes(parent, tagName, false);
  }

  public static Map<String, String> getChildAttributes(Element parent, String tagName,
      boolean includeNSDeclaration) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    Map<String, String> result = new HashMap<>();
    List<Element> children = getElementsByLocalName(parent, tagName);

    for (Element child : children) {
      result.putAll(getAttributes(child, includeNSDeclaration));
    }
    return result;
  }

  public static List<Element> getChildrenElements(Element parent) {
    Assert.notNull(parent);
    List<Element> result = new ArrayList<>();

    NodeList nodes = parent.getChildNodes();
    if (nodes == null || nodes.getLength() <= 0) {
      return result;
    }

    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (isElement(node)) {
        result.add((Element) node);
      }
    }
    return result;
  }

  public static List<String> getChildrenText(Element parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);
    List<String> result = new ArrayList<>();

    List<Element> children = getElementsByLocalName(parent, tagName);
    if (children.isEmpty()) {
      return result;
    }

    for (int i = 0; i < children.size(); i++) {
      String text = getText(children.get(i));
      if (!BeeUtils.isEmpty(text)) {
        result.add(text);
      }
    }
    return result;
  }

  public static List<Property> getCommentInfo(Comment comm) {
    Assert.notNull(comm);
    return PropertyUtils.createProperties("Length", comm.getLength(), "Data", comm.getData());
  }

  public static ConditionalStyleDeclaration getConditionalStyle(Element element) {
    Assert.notNull(element);

    StyleDeclaration style = getStyle(element);
    Calculation condition = getCalculation(element);

    if (style == null && condition == null) {
      return null;
    }
    return new ConditionalStyleDeclaration(style, condition);
  }

  public static ConditionalStyleDeclaration getConditionalStyle(Element parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    Element element = getFirstChildElement(parent, tagName);
    if (element == null) {
      return null;
    }
    return getConditionalStyle(element);
  }

  public static Dimensions getDimensions(Element element) {
    Assert.notNull(element);
    return new Dimensions(getAttributeDouble(element, HasDimensions.ATTR_WIDTH),
        getAttributeUnit(element, HasDimensions.ATTR_WIDTH_UNIT),
        getAttributeDouble(element, HasDimensions.ATTR_HEIGHT),
        getAttributeUnit(element, HasDimensions.ATTR_HEIGHT_UNIT));
  }

  public static List<Property> getDocumentInfo(Document doc) {
    Assert.notNull(doc);
    List<Property> lst = new ArrayList<>();

    Element el = doc.getDocumentElement();
    if (el != null) {
      PropertyUtils.addProperty(lst, "Document Element", el.getTagName());
    }
    return lst;
  }

  public static List<Property> getElementInfo(Element el, boolean detailed) {
    Assert.notNull(el);
    List<Property> lst = PropertyUtils.createProperties("Tag Name", el.getTagName());
    if (detailed) {
      lst.addAll(getNodeInfo(el));
    }

    NamedNodeMap attributes = el.getAttributes();
    int c = (attributes == null) ? 0 : attributes.getLength();

    if (c > 0) {
      PropertyUtils.addProperty(lst, "Attributes", BeeUtils.bracket(c));
      Attr attr;
      String pfx;

      for (int i = 0; i < c; i++) {
        attr = (Attr) attributes.item(i);
        pfx = BeeUtils.joinWords("Attr", BeeUtils.progress(i + 1, c));
        PropertyUtils.addProperty(lst, BeeUtils.joinWords(pfx, attr.getName()), attr.getValue());
        if (detailed) {
          PropertyUtils.appendChildrenToProperties(lst, pfx, getNodeInfo(attr));
        }
      }
    }
    return lst;
  }

  public static List<Element> getElementsByLocalName(Element parent, String tagName) {
    return getElementsByLocalName(parent, tagName, BeeConst.UNDEF);
  }

  public static List<Element> getElementsByLocalName(Element parent, String tagName, int max) {
    Assert.notEmpty(tagName);
    List<Element> result = new ArrayList<>();

    for (Element child : getChildrenElements(parent)) {
      if (BeeUtils.same(getLocalName(child), tagName)) {
        result.add(child);
        if (max > 0 && result.size() >= max) {
          break;
        }
      }
    }
    return result;
  }

  public static Element getFirstChildElement(Element parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    List<Element> children = getElementsByLocalName(parent, tagName, 1);
    if (children.isEmpty()) {
      return null;
    }
    return children.get(0);
  }

  public static List<ExtendedProperty> getInfo(String xml, boolean detailed) {
    Document doc = parse(xml);
    if (doc == null) {
      return null;
    }
    return getTreeInfo(doc, BeeConst.STRING_EMPTY, detailed);
  }

  public static String getInnerXml(Element element) {
    if (element == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();

    NodeList children = element.getChildNodes();
    if (children != null) {
      for (int i = 0; i < children.getLength(); i++) {
        Node node = children.item(i);
        if (isElement(node)) {
          sb.append(node.toString());
        } else {
          sb.append(BeeUtils.trim(node.getNodeValue()));
        }
      }
    }
    return sb.toString();
  }

  public static String getLocalName(Attr attr) {
    Assert.notNull(attr);
    return NameUtils.getLocalPart(attr.getName());
  }

  public static String getLocalName(Element el) {
    Assert.notNull(el);
    return NameUtils.getLocalPart(el.getTagName());
  }

  public static List<Property> getNodeInfo(Node nd) {
    Assert.notNull(nd);
    return PropertyUtils.createProperties("Node Type", nd.getNodeType(),
        "Node Name", nd.getNodeName(),
        "Node Value", nd.getNodeValue(),
        "Namespace URI", nd.getNamespaceURI(),
        "Prefix", nd.getPrefix());
  }

  public static List<Property> getProcessingInstructionInfo(ProcessingInstruction pin) {
    Assert.notNull(pin);
    return PropertyUtils.createProperties("Data", pin.getData(), "Target", pin.getTarget());
  }

  public static Relation getRelation(Map<String, String> attributes, List<Element> children) {
    RendererDescription rowRenderer = null;
    Calculation rowRender = null;
    List<RenderableToken> rowRenderTokens = null;

    List<SelectorColumn> selectorColumns = new ArrayList<>();

    for (Element child : children) {
      String tagName = getLocalName(child);

      if (BeeUtils.same(tagName, Relation.TAG_ROW_RENDERER)) {
        rowRenderer = getRendererDescription(child);

      } else if (BeeUtils.same(tagName, Relation.TAG_ROW_RENDER)) {
        rowRender = getCalculation(child);

      } else if (BeeUtils.same(tagName, Relation.TAG_ROW_RENDER_TOKEN)) {
        RenderableToken token = RenderableToken.create(getAttributes(child, false));
        if (token != null) {
          if (rowRenderTokens == null) {
            rowRenderTokens = Lists.newArrayList(token);
          } else {
            rowRenderTokens.add(token);
          }
        }

      } else if (BeeUtils.same(tagName, Relation.TAG_SELECTOR_COLUMN)) {
        RendererDescription renderer = getRendererDescription(child,
            RendererDescription.TAG_RENDERER);
        Calculation render = getCalculation(child, RendererDescription.TAG_RENDER);
        List<RenderableToken> tokens = getRenderTokens(child, RenderableToken.TAG_RENDER_TOKEN);

        selectorColumns.add(SelectorColumn.create(getAttributes(child, false),
            renderer, render, tokens));
      }
    }

    return Relation.create(attributes, selectorColumns, rowRenderer, rowRender, rowRenderTokens);
  }

  public static RendererDescription getRendererDescription(Element element) {
    Assert.notNull(element);
    String typeCode = element.getAttribute(RendererDescription.ATTR_TYPE);
    if (BeeUtils.isEmpty(typeCode)) {
      return null;
    }

    RendererType type = RendererType.getByTypeCode(typeCode);
    if (type == null) {
      return null;
    }

    RendererDescription rendererDescription = new RendererDescription(type);
    rendererDescription.setAttributes(getAttributes(element, false));

    List<String> items = getChildrenText(element, HasItems.TAG_ITEM);
    if (!items.isEmpty()) {
      rendererDescription.setItems(items);
    }

    return rendererDescription;
  }

  public static RendererDescription getRendererDescription(Element parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    Element element = getFirstChildElement(parent, tagName);
    if (element == null) {
      return null;
    }
    return getRendererDescription(element);
  }

  public static List<RenderableToken> getRenderTokens(Element parent, String tagName) {
    if (parent == null) {
      return null;
    }
    List<Element> tokens = getElementsByLocalName(parent, tagName);
    if (tokens.isEmpty()) {
      return null;
    }

    List<RenderableToken> result = new ArrayList<>();
    for (Element token : tokens) {
      RenderableToken renderableToken = RenderableToken.create(getAttributes(token, false));
      if (renderableToken != null) {
        result.add(renderableToken);
      }
    }
    return result;
  }

  public static StyleDeclaration getStyle(Element element) {
    Assert.notNull(element);

    String className = getText(getFirstChildElement(element, StyleDeclaration.TAG_CLASS));
    String inline = getText(getFirstChildElement(element, StyleDeclaration.TAG_INLINE));
    String font = getText(getFirstChildElement(element, StyleDeclaration.TAG_FONT));

    if (BeeUtils.allEmpty(className, inline, font)) {
      return null;
    }
    return new StyleDeclaration(className, inline, font);
  }

  public static String getText(Element element) {
    if (element == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();

    NodeList children = element.getChildNodes();
    if (children != null && children.getLength() > 0) {
      for (int i = 0; i < children.getLength(); i++) {
        Node node = children.item(i);
        short type = node.getNodeType();
        if (type == Node.CDATA_SECTION_NODE || type == Node.TEXT_NODE) {
          sb.append(BeeUtils.trim(node.getNodeValue()));
        }
      }
    }
    return sb.toString();
  }

  public static List<Property> getTextInfo(Text txt) {
    Assert.notNull(txt);
    return PropertyUtils.createProperties(BeeUtils.joinWords("Length", txt.getLength()),
        txt.getData());
  }

  public static List<ExtendedProperty> getTreeInfo(Node nd, String root, boolean detailed) {
    Assert.notNull(nd);
    List<ExtendedProperty> lst = new ArrayList<>();

    List<Property> tpInf = null;
    short tp = nd.getNodeType();

    switch (tp) {
      case Node.ELEMENT_NODE:
        tpInf = getElementInfo((Element) nd, detailed);
        break;
      case Node.ATTRIBUTE_NODE:
        tpInf = getAttrInfo((Attr) nd);
        break;
      case Node.TEXT_NODE:
        if (!BeeUtils.isEmpty(((Text) nd).getData())) {
          tpInf = getTextInfo((Text) nd);
        }
        break;
      case Node.CDATA_SECTION_NODE:
        tpInf = getCDATAInfo((CDATASection) nd);
        break;
      case Node.PROCESSING_INSTRUCTION_NODE:
        tpInf = getProcessingInstructionInfo((ProcessingInstruction) nd);
        break;
      case Node.COMMENT_NODE:
        tpInf = getCommentInfo((Comment) nd);
        break;
      case Node.DOCUMENT_NODE:
        tpInf = getDocumentInfo((Document) nd);
        break;
      default:
        tpInf = PropertyUtils.createProperties(nd.toString(),
            BeeUtils.joinWords("unknown node type", tp));
    }

    if (!BeeUtils.isEmpty(tpInf)) {
      PropertyUtils.appendChildrenToExtended(lst, BeeUtils.joinWords(root, getNodeName(tp)), tpInf);
    }

    if (nd.hasChildNodes()) {
      NodeList children = nd.getChildNodes();
      int c = (children == null) ? 0 : children.getLength();
      PropertyUtils.addExtended(lst, BeeUtils.notEmpty(root, getNodeName(tp)), "Children",
          BeeUtils.bracket(c));
      for (int i = 0; i < c; i++) {
        lst.addAll(getTreeInfo(children.item(i), BeeUtils.join(".", root, i), detailed));
      }
    }
    return lst;
  }

  public static boolean isNamespaceDeclaration(Attr attr) {
    if (attr == null) {
      return false;
    } else {
      return isNamespaceDeclaration(attr.getName());
    }
  }

  public static boolean isNamespaceDeclaration(String name) {
    return BeeUtils.same(name, XmlHelper.ATTR_XMLNS)
        || BeeUtils.same(NameUtils.getNamespacePrefix(name), XmlHelper.ATTR_XMLNS);
  }

  public static Document parse(String xml) {
    Assert.notEmpty(xml);
    Document doc;

    try {
      doc = XMLParser.parse(xml);
    } catch (DOMParseException ex) {
      logger.severe(ex.getMessage());
      doc = null;
    }
    return doc;
  }

  public static void setAttributes(Element element, Map<String, String> attributes) {
    Assert.notNull(element);

    if (!BeeUtils.isEmpty(attributes)) {
      for (Map.Entry<String, String> entry : attributes.entrySet()) {
        element.setAttribute(entry.getKey().trim(), entry.getValue());
      }
    }
  }

  public static boolean tagIs(Element element, String tagName) {
    if (element == null) {
      return false;
    } else {
      return BeeUtils.same(getLocalName(element), tagName);
    }
  }

  private static void appendElementWithText(Document doc, Element root, String tag, String txt) {
    Element el = doc.createElement(tag);
    Text x = doc.createTextNode(txt);

    el.appendChild(x);
    root.appendChild(el);
  }

  private static Document createDoc(String rootName, String... nodes) {
    Document doc = XMLParser.createDocument();
    Element root = doc.createElement(rootName);

    String tag;
    String txt;

    for (int i = 0; i < nodes.length - 1; i += 2) {
      tag = nodes[i];
      txt = nodes[i + 1];

      if (!BeeUtils.anyEmpty(tag, txt)) {
        appendElementWithText(doc, root, tag.trim(), txt.trim());
      }
    }

    doc.appendChild(root);
    return doc;
  }

  private static String getNodeName(short type) {
    return NODE_TYPES.get(type);
  }

  private static boolean isElement(Node nd) {
    return nd != null && nd.getNodeType() == Node.ELEMENT_NODE;
  }

  private static String transformDocument(Document doc) {
    return transformDocument(doc, XmlHelper.DEFAULT_PROLOG);
  }

  private static String transformDocument(Document doc, String prolog) {
    String xml = doc.toString();

    if (BeeUtils.isEmpty(prolog) || xml.matches("^<[?][xX][mM][lL].*")) {
      return xml;
    } else {
      return prolog + xml;
    }
  }

  private XmlUtils() {
  }
}
