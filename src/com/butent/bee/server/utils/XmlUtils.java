package com.butent.bee.server.utils;

import com.butent.bee.server.io.FileUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.ConditionalStyleDeclaration;
import com.butent.bee.shared.ui.StyleDeclaration;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.TypeInfo;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.SchemaFactoryConfigurationError;

/**
 * Manages XML configuration files used by the system.
 */

public final class XmlUtils {

  /**
   * Handles XML parsing errors.
   */

  private static class SAXErrorHandler implements ErrorHandler {
    @Override
    public void error(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      throw exception;
    }
  }

  private static BeeLogger logger = LogUtils.getLogger(XmlUtils.class);

  public static final String DEFAULT_XML_EXTENSION = "xml";
  public static final String DEFAULT_XSD_EXTENSION = "xsd";
  public static final String DEFAULT_XSL_EXTENSION = "xsl";

  private static final String ALL_NS = "*";
  private static final String ALL_TAGS = "*";

  private static DocumentBuilder domBuilder;

  private static TransformerFactory xsltFactory;

  private static SchemaFactory schemaFactory;

  private static Map<Short, String> nodeTypes = new HashMap<>();

  static {
    nodeTypes.put(Node.ELEMENT_NODE, "Element");
    nodeTypes.put(Node.ATTRIBUTE_NODE, "Attribute");
    nodeTypes.put(Node.TEXT_NODE, "Text");
    nodeTypes.put(Node.CDATA_SECTION_NODE, "CDATA Section");
    nodeTypes.put(Node.ENTITY_REFERENCE_NODE, "Entity Reference");
    nodeTypes.put(Node.ENTITY_NODE, "Entity");
    nodeTypes.put(Node.PROCESSING_INSTRUCTION_NODE, "Processing Instruction");
    nodeTypes.put(Node.COMMENT_NODE, "Comment");
    nodeTypes.put(Node.DOCUMENT_NODE, "Document");
    nodeTypes.put(Node.DOCUMENT_TYPE_NODE, "Document Type");
    nodeTypes.put(Node.DOCUMENT_FRAGMENT_NODE, "Document Fragment");
    nodeTypes.put(Node.NOTATION_NODE, "Notation");

    DocumentBuilder bld;

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      dbf.setXIncludeAware(true);
      bld = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      logger.error(ex);
      bld = null;
    }

    domBuilder = bld;

    TransformerFactory tf = null;

    try {
      tf = TransformerFactory.newInstance();
    } catch (TransformerFactoryConfigurationError ex) {
      logger.error(ex);
    }
    xsltFactory = tf;

    SchemaFactory sf = null;

    try {
      sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    } catch (IllegalArgumentException | SchemaFactoryConfigurationError ex) {
      logger.error(ex);
    }
    schemaFactory = sf;
  }

  public static Document createDoc(String rootName, String... nodes) {
    Document doc = createDocument();
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

  public static Document createDocument() {
    return domBuilder.newDocument();
  }

  public static String createString(String rootName, String... nodes) {
    Assert.notEmpty(rootName);
    Assert.notNull(nodes);
    Assert.parameterCount(nodes.length + 1, 3);

    return transformDocument(createDoc(rootName, nodes));
  }

  public static Document fromFileName(String fileName) {
    File fl = new File(fileName);
    if (!FileUtils.isInputFile(fl)) {
      logger.severe(fileName, "not an input file");
      return null;
    }

    Document doc = createDocument(fl);
    return doc;
  }

  public static Document fromString(String xml) {
    Document doc = createDocument(new StringReader(xml));
    return doc;
  }

  public static List<Element> getAllDescendantElements(Node parent) {
    return getElementsByLocalName(parent, ALL_TAGS);
  }

  public static Boolean getAttributeBoolean(Element element, String name) {
    Assert.notNull(element);
    Assert.notEmpty(name);
    return BeeUtils.toBooleanOrNull(element.getAttribute(name));
  }

  public static Integer getAttributeInteger(Element element, String name) {
    Assert.notNull(element);
    Assert.notEmpty(name);
    return BeeUtils.toIntOrNull(element.getAttribute(name));
  }

  public static Map<String, String> getAttributes(Node node) {
    Assert.notNull(node);
    Map<String, String> result = new HashMap<>();

    NamedNodeMap attributes = node.getAttributes();
    if (attributes == null || attributes.getLength() <= 0) {
      return result;
    }

    Attr attr;
    for (int i = 0; i < attributes.getLength(); i++) {
      attr = (Attr) attributes.item(i);
      result.put(attr.getName(), attr.getValue());
    }
    return result;
  }

  public static Property[][] getAttributesFromFile(String src, String tag) {
    return getAttributesFromFile(src, null, tag);
  }

  public static Property[][] getAttributesFromFile(String src, String xsl, String tag) {
    Assert.notEmpty(src);
    Assert.notEmpty(tag);

    Document doc = null;
    if (BeeUtils.isEmpty(xsl)) {
      doc = fromFileName(src);
    } else {
      doc = xsltToDom(src, xsl);
    }

    if (doc == null) {
      logger.warning(src, xsl, "cannot parse xml");
      return null;
    }

    NodeList lst = doc.getElementsByTagNameNS(ALL_NS, tag);
    int r = (lst == null) ? 0 : lst.getLength();
    if (r <= 0) {
      logger.warning("tag", tag, "not found in", src, xsl);
      return null;
    }

    Property[][] arr = new Property[r][];

    NamedNodeMap attributes;
    Attr attr;
    int c;

    for (int i = 0; i < r; i++) {
      attributes = lst.item(i).getAttributes();
      c = (attributes == null) ? 0 : attributes.getLength();

      if (c <= 0) {
        arr[i] = null;
      } else {
        arr[i] = new Property[c];
        for (int j = 0; j < c; j++) {
          attr = (Attr) attributes.item(j);
          arr[i][j] = new Property(attr.getName(), attr.getValue());
        }
      }
    }

    return arr;
  }

  public static List<Property> getAttrInfo(Attr attr) {
    Assert.notNull(attr);
    return PropertyUtils.createProperties("Name", attr.getName(),
        "Value", attr.getValue(),
        "Owner Element", transformElement(attr.getOwnerElement()),
        "Schema Type Info", transformTypeInfo(attr.getSchemaTypeInfo()),
        "Specified", attr.getSpecified(),
        "Is Id", attr.isId());
  }

  public static Calculation getCalculation(Element element) {
    Assert.notNull(element);

    String expr = null;
    String func = null;

    for (Element child : getChildrenElements(element)) {
      String tag = getLocalName(child);
      String text = child.getTextContent();
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

  public static List<Property> getCDataInfo(CDATASection cdata) {
    Assert.notNull(cdata);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Length", cdata.getLength(), "Data", cdata.getData(),
        "Is Element Content Whitespace", cdata.isElementContentWhitespace());
    return lst;
  }

  public static List<Element> getChildrenElements(Node parent) {
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

  public static List<Element> getChildrenElements(Node parent, Collection<String> tagNames) {
    Assert.notNull(parent);
    Assert.notNull(tagNames);
    List<Element> result = new ArrayList<>();

    NodeList nodes = parent.getChildNodes();
    if (isEmpty(nodes)) {
      return result;
    }

    for (int i = 0; i < nodes.getLength(); i++) {
      Element element = asElement(nodes.item(i));
      if (element != null && BeeUtils.containsSame(tagNames, getLocalName(element))) {
        result.add(element);
      }
    }
    return result;
  }

  public static List<Property> getCommentInfo(Comment comm) {
    Assert.notNull(comm);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Length", comm.getLength(),
        "Data", comm.getData(), "To String", comm.toString());
    return lst;
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

  public static List<Property> getDocumentFragmentInfo(DocumentFragment df) {
    Assert.notNull(df);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperty(lst, "To String", df.toString());
    return lst;
  }

  public static List<Property> getDocumentInfo(Document doc) {
    Assert.notNull(doc);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Document URI", doc.getDocumentURI(),
        "Implementation", transformDOMImplementation(doc.getImplementation()),
        "Input Encoding", doc.getInputEncoding(),
        "Strict Error Checking", doc.getStrictErrorChecking(),
        "Xml Encoding", doc.getXmlEncoding(),
        "Xml Standalone", doc.getXmlStandalone(),
        "Xml Version", doc.getXmlVersion(),
        "To String", doc.toString());

    PropertyUtils.appendChildrenToProperties(lst, "Dom Config",
        getDOMConfigurationInfo(doc.getDomConfig()));

    DocumentType dtp = doc.getDoctype();
    if (dtp != null) {
      PropertyUtils.appendChildrenToProperties(lst, "Doctype", getDocumentTypeInfo(dtp));
    }

    Element el = doc.getDocumentElement();
    if (el != null) {
      PropertyUtils.appendChildrenToProperties(lst, "Document Element", getElementInfo(el));
    }
    return lst;
  }

  public static List<Property> getDocumentTypeInfo(DocumentType dtp) {
    Assert.notNull(dtp);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Name", dtp.getName(),
        "Internal Subset", dtp.getInternalSubset(),
        "Public Id", dtp.getPublicId(),
        "System Id", dtp.getSystemId(),
        "To String", dtp.toString());

    lst.addAll(getNamedNodeMapInfo(dtp.getEntities(), "Entities"));
    lst.addAll(getNamedNodeMapInfo(dtp.getNotations(), "Notations"));

    return lst;
  }

  public static List<Property> getDomBuilderInfo() {
    List<Property> lst = new ArrayList<>();

    if (domBuilder == null) {
      PropertyUtils.addProperty(lst, "Error creating builder", DocumentBuilder.class.getName());
    } else {
      PropertyUtils.addProperties(lst, "Schema", domBuilder.getSchema(),
          "Is Namespace Aware", domBuilder.isNamespaceAware(),
          "Is Validating", domBuilder.isValidating(),
          "Is XInclude Aware", domBuilder.isXIncludeAware(),
          "To String", domBuilder.toString());
    }
    return lst;
  }

  public static List<Property> getElementInfo(Element el) {
    Assert.notNull(el);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Tag Name", el.getTagName(),
        "Schema Type Info", transformTypeInfo(el.getSchemaTypeInfo()),
        "To String", el.toString());
    return lst;
  }

  public static Map<String, String> getElements(NodeList nodes, String ignore) {
    Assert.notNull(nodes);
    Map<String, String> ret = new HashMap<>();

    Element el;
    String tg;
    String txt;

    for (int i = 0; i < nodes.getLength(); i++) {
      el = asElement(nodes.item(i));
      if (el == null) {
        continue;
      }

      tg = getLocalName(el);
      if (ignore != null && BeeUtils.same(tg, ignore)) {
        continue;
      }

      txt = el.getTextContent();
      if (!BeeUtils.isEmpty(tg) && !BeeUtils.isEmpty(txt)) {
        ret.put(tg, txt);
      }
    }
    return ret;
  }

  public static Map<String, String> getElements(String xml, String ignore) {
    Assert.notEmpty(xml);
    Map<String, String> ret = new HashMap<>();

    Document doc = fromString(xml);
    if (doc == null) {
      return ret;
    }

    NodeList nodes = doc.getElementsByTagName(ALL_TAGS);
    if (nodes == null) {
      return ret;
    }
    return getElements(nodes, ignore);
  }

  public static List<Element> getElementsByLocalName(Node parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    List<Element> result = new ArrayList<>();
    NodeList nodes;

    if (isElement(parent)) {
      nodes = asElement(parent).getElementsByTagNameNS(ALL_NS, tagName);
    } else if (isDocument(parent)) {
      nodes = asDocument(parent).getElementsByTagNameNS(ALL_NS, tagName);
    } else {
      Assert.untouchable("node must be element or document");
      nodes = null;
    }

    if (isEmpty(nodes)) {
      return result;
    }

    for (int i = 0; i < nodes.getLength(); i++) {
      Element element = asElement(nodes.item(i));
      if (element != null) {
        result.add(element);
      }
    }
    return result;
  }

  public static List<Property> getEntityInfo(Entity ent) {
    Assert.notNull(ent);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Input Encoding", ent.getInputEncoding(),
        "Notation Name", ent.getNotationName(), "Public Id", ent.getPublicId(),
        "System Id", ent.getSystemId(), "Xml Encoding", ent.getXmlEncoding(),
        "XmlVersion", ent.getXmlVersion(), "To String", ent.toString());
    return lst;
  }

  public static List<Property> getEntityReferenceInfo(EntityReference er) {
    Assert.notNull(er);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperty(lst, "To String", er.toString());
    return lst;
  }

  public static List<ExtendedProperty> getFileInfo(String fileName) {
    Assert.notEmpty(fileName);

    Document doc = fromFileName(fileName);
    if (doc == null) {
      logger.warning(fileName, "cannot parse xml");
      return null;
    }
    return getTreeInfo(doc, "0");
  }

  public static Element getFirstChildElement(Element parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    NodeList nodes = parent.getChildNodes();
    if (isEmpty(nodes)) {
      return null;
    }

    for (int i = 0; i < nodes.getLength(); i++) {
      Element element = asElement(nodes.item(i));
      if (element != null && BeeUtils.same(tagName, getLocalName(element))) {
        return element;
      }
    }
    return null;
  }

  public static String getLocalName(Element el) {
    Assert.notNull(el);
    if (BeeUtils.isEmpty(el.getLocalName())) {
      return NameUtils.getLocalPart(el.getTagName());
    } else {
      return el.getLocalName();
    }
  }

  public static List<Property> getNodeInfo(Node nd) {
    Assert.notNull(nd);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Node Type", nd.getNodeType(),
        "Node Name", nd.getNodeName(),
        "Local Name", nd.getLocalName(),
        "Node Value", nd.getNodeValue(),
        "Text Content", nd.getTextContent(),
        "Parent Node", nd.getParentNode(),
        "First Child", nd.getFirstChild(),
        "Last Child", nd.getLastChild(),
        "Previous Sibling", nd.getPreviousSibling(),
        "Next Sibling", nd.getNextSibling(),
        "Base URI", nd.getBaseURI(),
        "Namespace URI", nd.getNamespaceURI(),
        "Prefix", nd.getPrefix());

    if (nd.hasAttributes()) {
      NamedNodeMap attributes = nd.getAttributes();
      int c = (attributes == null) ? 0 : attributes.getLength();
      PropertyUtils.addProperty(lst, "Attributes", BeeUtils.bracket(c));

      for (int i = 0; i < c; i++) {
        Node attr = attributes.item(i);
        if (!isAttribute(attr)) {
          continue;
        }

        PropertyUtils.addProperty(lst, "Attribute", BeeUtils.progress(i + 1, c));
        lst.addAll(getAttrInfo((Attr) attr));
      }
    }
    return lst;
  }

  public static List<Property> getNotationInfo(Notation nt) {
    Assert.notNull(nt);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Public Id", nt.getPublicId(),
        "System Id", nt.getSystemId(), "To String", nt.toString());
    return lst;
  }

  public static List<Property> getOutputKeysInfo() {
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst,
        "CDATA SECTION ELEMENTS", OutputKeys.CDATA_SECTION_ELEMENTS,
        "DOCTYPE PUBLIC", OutputKeys.DOCTYPE_PUBLIC,
        "DOCTYPE SYSTEM", OutputKeys.DOCTYPE_SYSTEM,
        "ENCODING", OutputKeys.ENCODING,
        "INDENT", OutputKeys.INDENT,
        "MEDIA TYPE", OutputKeys.MEDIA_TYPE,
        "METHOD", OutputKeys.METHOD,
        "OMIT XML DECLARATION", OutputKeys.OMIT_XML_DECLARATION,
        "STANDALONE", OutputKeys.STANDALONE,
        "VERSION", OutputKeys.VERSION);
    return lst;
  }

  public static Element getParentElement(Element child) {
    if (child == null) {
      return null;
    } else {
      Node parent = child.getParentNode();
      return isElement(parent) ? (Element) parent : null;
    }
  }

  public static List<Property> getProcessingInstructionInfo(ProcessingInstruction pin) {
    Assert.notNull(pin);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Data", pin.getData(), "Target", pin.getTarget(),
        "To String", pin.toString());
    return lst;
  }

  public static List<ExtendedProperty> getRootInfo(Document doc) {
    Assert.notNull(doc);
    List<ExtendedProperty> lst = new ArrayList<>();

    String root = getNodeName(Node.DOCUMENT_NODE);

    PropertyUtils.addChildren(lst, root,
        "Document URI", doc.getDocumentURI(),
        "Implementation", transformDOMImplementation(doc.getImplementation()),
        "Input Encoding", doc.getInputEncoding(),
        "Strict Error Checking", doc.getStrictErrorChecking(),
        "Xml Encoding", doc.getXmlEncoding(),
        "Xml Standalone", doc.getXmlStandalone(),
        "Xml Version", doc.getXmlVersion(),
        "To String", doc.toString());

    PropertyUtils.appendChildrenToExtended(lst, root + " Dom Config",
        getDOMConfigurationInfo(doc.getDomConfig()));

    DocumentType dtp = doc.getDoctype();
    if (dtp != null) {
      PropertyUtils.appendChildrenToExtended(lst, root + " Doctype", getDocumentTypeInfo(dtp));
    }

    PropertyUtils.appendChildrenToExtended(lst, "Document Node", getNodeInfo(doc));

    NodeList nodes = doc.getElementsByTagName(ALL_TAGS);
    int c = (nodes == null) ? 0 : nodes.getLength();
    PropertyUtils.addExtended(lst, root, "ElementsByTagName " + ALL_TAGS, BeeUtils.bracket(c));

    if (c > 0) {
      for (int i = 0; i < c; i++) {
        PropertyUtils.addExtended(lst, root, "Node " + BeeUtils.progress(i + 1, c),
            transformNode(nodes.item(i)));
      }
    }

    Element el = doc.getDocumentElement();
    PropertyUtils.appendChildrenToExtended(lst, "Document Element", getElementInfo(el));
    PropertyUtils.appendChildrenToExtended(lst, "Document Element Node", getNodeInfo(el));

    return lst;
  }

  public static StyleDeclaration getStyle(Element element) {
    Assert.notNull(element);

    String className = getTextQuietly(getFirstChildElement(element, StyleDeclaration.TAG_CLASS));
    String inline = getTextQuietly(getFirstChildElement(element, StyleDeclaration.TAG_INLINE));
    String font = getTextQuietly(getFirstChildElement(element, StyleDeclaration.TAG_FONT));

    if (BeeUtils.allEmpty(className, inline, font)) {
      return null;
    }
    return new StyleDeclaration(className, inline, font);
  }

  public static StyleDeclaration getStyle(Element parent, String tagName) {
    Assert.notNull(parent);
    Assert.notEmpty(tagName);

    Element element = getFirstChildElement(parent, tagName);
    if (element == null) {
      return null;
    }
    return getStyle(element);
  }

  public static String getText(String xml, String tag) {
    Assert.notEmpty(xml);
    Assert.notEmpty(tag);

    Document doc = fromString(xml);
    if (doc == null) {
      return null;
    }

    NodeList nodes = doc.getElementsByTagNameNS(ALL_NS, tag.trim());
    if (nodes == null) {
      return null;
    }

    Element el;
    String txt;

    for (int i = 0; i < nodes.getLength(); i++) {
      el = asElement(nodes.item(i));
      if (el == null) {
        continue;
      }

      txt = el.getTextContent();
      if (!BeeUtils.isEmpty(txt)) {
        return txt;
      }
    }
    return BeeConst.STRING_EMPTY;
  }

  public static List<Property> getTextInfo(Text txt) {
    Assert.notNull(txt);
    List<Property> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, "Length", txt.getLength(),
        "Data", txt.getData(), "Whole Text", txt.getWholeText(),
        "Is Element Content Whitespace", txt.isElementContentWhitespace(),
        "To String", txt.toString());
    return lst;
  }

  public static String getTextQuietly(Element element) {
    if (element == null) {
      return null;
    }
    return element.getTextContent();
  }

  public static List<ExtendedProperty> getTreeInfo(Node nd, String root) {
    Assert.notNull(nd);
    Assert.notEmpty(root);
    List<ExtendedProperty> lst = new ArrayList<>();

    List<Property> tpInf = null;
    short tp = nd.getNodeType();

    switch (tp) {
      case Node.ELEMENT_NODE:
        tpInf = getElementInfo((Element) nd);
        break;
      case Node.ATTRIBUTE_NODE:
        tpInf = getAttrInfo((Attr) nd);
        break;
      case Node.TEXT_NODE:
        tpInf = getTextInfo((Text) nd);
        break;
      case Node.CDATA_SECTION_NODE:
        tpInf = getCDataInfo((CDATASection) nd);
        break;
      case Node.ENTITY_REFERENCE_NODE:
        tpInf = getEntityReferenceInfo((EntityReference) nd);
        break;
      case Node.ENTITY_NODE:
        tpInf = getEntityInfo((Entity) nd);
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
      case Node.DOCUMENT_TYPE_NODE:
        tpInf = getDocumentTypeInfo((DocumentType) nd);
        break;
      case Node.DOCUMENT_FRAGMENT_NODE:
        tpInf = getDocumentFragmentInfo((DocumentFragment) nd);
        break;
      case Node.NOTATION_NODE:
        tpInf = getNotationInfo((Notation) nd);
        break;
      default:
        logger.warning("unknown node type", tp);
        tpInf = PropertyUtils.createProperties(nd.toString(),
            BeeUtils.joinWords("unknown node type", tp));
    }

    if (!BeeUtils.isEmpty(tpInf)) {
      PropertyUtils.appendChildrenToExtended(lst, BeeUtils.joinWords(root, getNodeName(tp)), tpInf);
    }

    PropertyUtils.appendChildrenToExtended(lst, BeeUtils.joinWords(root, "Node"), getNodeInfo(nd));

    if (nd.hasChildNodes()) {
      NodeList children = nd.getChildNodes();
      int c = (children == null) ? 0 : children.getLength();
      PropertyUtils.addExtended(lst, root, "Children", BeeUtils.bracket(c));

      for (int i = 0; i < c; i++) {
        lst.addAll(getTreeInfo(children.item(i), BeeUtils.join(".", root, i + 1)));
      }
    }
    return lst;
  }

  public static synchronized Document getXmlResource(String resource, String resourceSchema) {
    if (BeeUtils.isEmpty(resource)) {
      return null;
    }
    Document ret = null;
    String error = null;
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setNamespaceAware(true);
    builderFactory.setXIncludeAware(true);

    try {
      Schema schema = schemaFactory.newSchema(new StreamSource(resourceSchema));
      builderFactory.setSchema(schema);
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      builder.setErrorHandler(new SAXErrorHandler());

      if (FileUtils.isFile(resource)) {
        ret = builder.parse(new InputSource(resource));
      } else {
        ret = builder.parse(new ByteArrayInputStream(resource.getBytes()));
      }
    } catch (SAXException e) {
      error = e.getException() == null ? e.getMessage() : e.getException().getMessage();

    } catch (IOException | ParserConfigurationException e) {
      error = e.getMessage();
    }
    if (!BeeUtils.isEmpty(error)) {
      logger.severe(resource, error);
    }
    return ret;
  }

  public static List<Property> getXsltFactoryInfo() {
    List<Property> lst = new ArrayList<>();

    if (xsltFactory == null) {
      PropertyUtils.addProperty(lst, "Error instantiating factory",
          TransformerFactory.class.getName());
    } else {
      PropertyUtils.addProperties(lst, "Class", NameUtils.transformClass(xsltFactory),
          "Error Listener", NameUtils.transformClass(xsltFactory.getErrorListener()),
          "URI Resolver", NameUtils.transformClass(xsltFactory.getURIResolver()));
    }
    return lst;
  }

  public static boolean hasChildElements(Element parent) {
    if (parent == null) {
      return false;
    }

    NodeList nodes = parent.getChildNodes();

    if (!isEmpty(nodes)) {
      for (int i = 0; i < nodes.getLength(); i++) {
        if (isElement(nodes.item(i))) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isEmpty(NodeList nodes) {
    return nodes == null || nodes.getLength() <= 0;
  }

  public static synchronized String marshal(Object obj, String schemaPath) {
    Assert.notNull(obj);
    StringWriter result = new StringWriter();

    try {
      Marshaller marshaller = JAXBContext.newInstance(obj.getClass()).createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

      if (!BeeUtils.isEmpty(schemaPath)) {
        marshaller.setSchema(schemaFactory.newSchema(new File(schemaPath)));
      }
      marshaller.marshal(obj, result);

    } catch (JAXBException e) {
      throw new BeeRuntimeException(e.getLinkedException() == null ? e : e.getLinkedException());

    } catch (SAXException e) {
      throw new BeeRuntimeException(e.getException() == null ? e : e.getException());
    }
    return result.toString();
  }

  public static boolean removeFromParent(Node node) {
    if (node == null || node.getParentNode() == null) {
      return false;
    } else {
      node.getParentNode().removeChild(node);
      return true;
    }
  }

  public static String tag(String tagName, Object value) {
    if (value == null) {
      return "";
    }
    String val = value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

    return new StringBuilder("<").append(tagName).append(">")
        .append(val)
        .append("</").append(tagName).append(">")
        .toString();
  }

  public static String toString(Node nd, boolean indent) {
    Assert.notNull(nd);
    Transformer transformer;
    try {
      transformer = xsltFactory.newTransformer();
    } catch (TransformerConfigurationException ex) {
      logger.error(ex);
      transformer = null;
    }
    if (transformer == null) {
      return null;
    }

    if (indent) {
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    }

    StringWriter writer = new StringWriter();
    try {
      transformer.transform(new DOMSource(nd), new StreamResult(writer));
    } catch (TransformerException ex) {
      logger.error(ex);
    }
    return writer.getBuffer().toString();
  }

  @SuppressWarnings("unchecked")
  public static synchronized <T> T unmarshal(Class<T> clazz, String resource, String schemaPath) {
    T result = null;

    if (!BeeUtils.isEmpty(resource)) {
      try {
        Unmarshaller unmarshaller = JAXBContext.newInstance(clazz).createUnmarshaller();
        InputStream source;

        if (FileUtils.isFile(resource)) {
          source = new FileInputStream(resource);
        } else {
          source = new ByteArrayInputStream(resource.getBytes());
        }
        if (BeeUtils.isEmpty(schemaPath)) {
          result = (T) unmarshaller.unmarshal(source);
        } else {
          DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
          builderFactory.setNamespaceAware(true);
          builderFactory.setXIncludeAware(true);
          builderFactory.setSchema(schemaFactory.newSchema(new File(schemaPath)));
          DocumentBuilder builder = builderFactory.newDocumentBuilder();
          builder.setErrorHandler(new SAXErrorHandler());
          result = (T) unmarshaller.unmarshal(builder.parse(source));
        }
      } catch (JAXBException e) {
        throw new BeeRuntimeException(e.getLinkedException() == null ? e : e.getLinkedException());

      } catch (SAXException e) {
        throw new BeeRuntimeException(e.getException() == null ? e : e.getException());

      } catch (ParserConfigurationException | IOException e) {
        throw new BeeRuntimeException(e);
      }
    }
    return result;
  }

  public static Document xsltToDom(String src, String xsl) {
    Assert.notEmpty(src);
    Assert.notEmpty(xsl);

    StreamSource in = new StreamSource(src);
    StreamSource tr = new StreamSource(xsl);

    Document doc = domBuilder.newDocument();
    Element nd = doc.createElement(NameUtils.createUniqueName("x2d"));

    DOMResult out = new DOMResult(nd);

    if (doXslt(in, tr, out)) {
      doc.appendChild(nd);
      return doc;
    } else {
      return null;
    }
  }

  public static boolean xsltToFile(String src, String xsl, String dst) {
    Assert.notEmpty(src);
    Assert.notEmpty(xsl);
    Assert.notEmpty(dst);

    StreamSource in = new StreamSource(src);
    StreamSource tr = new StreamSource(xsl);
    StreamResult out = new StreamResult(dst);

    return doXslt(in, tr, out);
  }

  public static List<ExtendedProperty> xsltToInfo(String src, String xsl) {
    Assert.notEmpty(src);
    Assert.notEmpty(xsl);

    Document doc = xsltToDom(src, xsl);
    if (doc == null) {
      return null;
    } else {
      return getTreeInfo(doc, "0");
    }
  }

  public static String xsltToString(String src, String xsl) {
    Assert.notEmpty(src);
    Assert.notEmpty(xsl);

    StreamSource in = new StreamSource(src);
    StreamSource tr = new StreamSource(xsl);

    StringWriter wrt = new StringWriter();
    StreamResult out = new StreamResult(wrt);

    if (doXslt(in, tr, out)) {
      return wrt.getBuffer().toString();
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  private static void appendElementWithText(Document doc, Element root, String tag, String txt) {
    Element el = doc.createElement(tag);
    Text x = doc.createTextNode(txt);

    el.appendChild(x);
    root.appendChild(el);
  }

  private static Document asDocument(Node nd) {
    if (isDocument(nd)) {
      return (Document) nd;
    } else {
      return null;
    }
  }

  private static Element asElement(Node nd) {
    if (isElement(nd)) {
      return (Element) nd;
    } else {
      return null;
    }
  }

  private static boolean checkBuilder() {
    if (domBuilder == null) {
      logger.severe("Document Builder not available");
      return false;
    } else {
      return true;
    }
  }

  private static synchronized Document createDocument(File fl) {
    Document ret = null;
    if (!checkBuilder()) {
      return ret;
    }

    try {
      ret = domBuilder.parse(fl);
    } catch (SAXException ex) {
      logger.error(ex);
    } catch (IOException ex) {
      logger.error(ex);
    }
    return ret;
  }

  private static synchronized Document createDocument(Reader rdr) {
    Document ret = null;
    if (!checkBuilder()) {
      return ret;
    }

    try {
      ret = domBuilder.parse(new InputSource(rdr));
    } catch (SAXException | IOException ex) {
      throw new BeeRuntimeException(ex);
    }
    return ret;
  }

  private static boolean doXslt(Source src, Source xsl, Result dst) {
    boolean ok = true;

    try {
      Transformer transf = xsltFactory.newTransformer(xsl);
      transf.transform(src, dst);
    } catch (TransformerException ex) {
      logger.error(ex);
      ok = false;
    }
    return ok;
  }

  private static List<Property> getDOMConfigurationInfo(DOMConfiguration cfg) {
    List<Property> lst = new ArrayList<>();
    if (cfg == null) {
      return lst;
    }

    DOMStringList names = cfg.getParameterNames();
    if (names == null) {
      return lst;
    }

    String key;

    for (int i = 0; i < names.getLength(); i++) {
      key = names.item(i);
      PropertyUtils.addProperty(lst, key, cfg.getParameter(key));
    }
    return lst;
  }

  private static List<Property> getNamedNodeMapInfo(NamedNodeMap nodes, String msg) {
    List<Property> lst = new ArrayList<>();
    if (nodes == null) {
      return lst;
    }

    int c = nodes.getLength();
    PropertyUtils.addProperty(lst, BeeUtils.notEmpty(msg, "Named Nodes"), BeeUtils.bracket(c));
    if (c > 0) {
      for (int i = 0; i < c; i++) {
        PropertyUtils.addProperty(lst, "Node " + BeeUtils.progress(i + 1, c),
            transformNode(nodes.item(i)));
      }
    }
    return lst;
  }

  private static String getNodeName(short type) {
    return nodeTypes.get(type);
  }

  private static boolean isAttribute(Node nd) {
    return nd.getNodeType() == Node.ATTRIBUTE_NODE;
  }

  private static boolean isDocument(Node nd) {
    return nd != null && nd.getNodeType() == Node.DOCUMENT_NODE;
  }

  private static boolean isElement(Node nd) {
    return nd != null && nd.getNodeType() == Node.ELEMENT_NODE;
  }

  private static String transformDocument(Document doc) {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(doc), new StreamResult(writer));
      return writer.toString();

    } catch (TransformerException ex) {
      LogUtils.getRootLogger().error(ex);
      return null;
    }
  }

  private static String transformDOMImplementation(DOMImplementation imp) {
    if (imp == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return imp.toString();
    }
  }

  private static String transformElement(Element el) {
    if (el == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeUtils.joinWords(el.getNodeName(), el.getTagName());
    }
  }

  private static String transformNode(Node nd) {
    if (nd == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeUtils.joinWords(nd.getNodeName(), nd.getNodeValue());
    }
  }

  private static String transformTypeInfo(TypeInfo ti) {
    if (ti == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeUtils.joinWords(ti.getTypeName(), ti.getTypeNamespace());
    }
  }

  private XmlUtils() {
  }
}
