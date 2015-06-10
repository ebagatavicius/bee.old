package com.butent.webservice;

import com.google.common.collect.ImmutableMap;

import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

public final class ButentWS {

  private static BeeLogger logger = LogUtils.getLogger(ButentWS.class);

  public static ButentWS connect(String namespace, String address, String login, String password)
      throws BeeException {

    if (BeeUtils.anyEmpty(address, login, password)) {
      throw new BeeException("WebService address/login/password not defined");
    }
    logger.info("Connecting to webservice:", address);

    ButentWS butentWS = new ButentWS(address, namespace);

    String answer = null;
    String error = "Unknown login response";

    try {
      answer = butentWS.login(login, password);
    } catch (Exception e) {
      throw new BeeException(e);
    }
    if (BeeUtils.same(answer, "OK")) {
      error = null;
    }
    if (!BeeUtils.isEmpty(error)) {
      Map<String, String> messages = XmlUtils.getElements(answer, null);

      if (BeeUtils.containsKey(messages, "Message")) {
        error = messages.get("Message");

        if (BeeUtils.same(error, "Already logged in")) {
          error = null;
        }
      }
    }
    if (!BeeUtils.isEmpty(error)) {
      throw new BeeException(error);
    }
    return butentWS;
  }

  private static Node getNode(String answer) throws BeeException {
    Node node;

    try {
      node = XmlUtils.fromString(answer).getFirstChild();
    } catch (Exception e) {
      throw new BeeException(answer);
    }
    if (BeeUtils.same(node.getLocalName(), "Error")) {
      throw new BeeException(node.getTextContent());
    }
    return node;
  }

  Dispatch<SOAPMessage> dispatch;
  String actionPrefix;
  String namespace;

  private ButentWS(String address, String defaultNamespace) {
    Service service = Service.create(new QName("ButentWebService"));
    QName portName = new QName("ButentWebServicePort");

    service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);

    actionPrefix = defaultNamespace + "action/ButentWebService";
    namespace = defaultNamespace + "message/";
    dispatch = service.createDispatch(portName, SOAPMessage.class, Service.Mode.MESSAGE);

    dispatch.getRequestContext().put(Dispatch.SOAPACTION_USE_PROPERTY, new Boolean(true));
  }

  public void disconnect() {
    logger.debug("Logout:", "logging out...");

    try {
      logout();
    } catch (Exception e) {
      logger.error(e);
    }
  }

  public SimpleRowSet getSQLData(String query, String... columns) throws BeeException {
    logger.debug("GetSQLData:", query);
    String answer;

    try {
      answer = process("GetSQLData", "<query>" + query + "</query>");
    } catch (Exception e) {
      throw new BeeException(e);
    }
    SimpleRowSet data = xmlToSimpleRowSet(answer, columns);
    logger.debug("GetSQLData cols:", data.getNumberOfColumns(), "rows:", data.getNumberOfRows());
    return data;
  }

  public String importClient(String companyName, String companyCode, String companyVATCode,
      String companyAddress, String companyPostIndex, String companyCity, String companyCountry)
      throws BeeException {

    logger.debug("ImportClient:", "importing client...");

    StringBuilder sb = new StringBuilder("<client>")
        .append(XmlUtils.tag("klientas", companyName))
        .append(XmlUtils.tag("kodas", companyCode))
        .append(XmlUtils.tag("pvm_kodas", companyVATCode))
        .append(XmlUtils.tag("adresas", companyAddress))
        .append(XmlUtils.tag("indeksas", companyPostIndex))
        .append(XmlUtils.tag("miestas", companyCity))
        .append(XmlUtils.tag("salis", companyCountry))
        .append("</client>");

    String answer;

    try {
      answer = process("ImportClient", sb.toString());
    } catch (Exception e) {
      throw new BeeException(e);
    }
    answer = getNode(answer).getTextContent();

    logger.debug("ImportClient:", "import succeeded. ClientName =", answer);

    return answer;
  }

  public void importDoc(WSDocument doc) throws BeeException {
    logger.debug("ImportDoc:", "importing document...");

    String answer;

    try {
      answer = process("ImportDoc", doc.getXml());
    } catch (Exception e) {
      throw new BeeException(e);
    }
    if (!BeeUtils.same(answer, "OK")) {
      getNode(answer);
    } else {
      logger.debug("ImportDoc:", "import succeeded");
    }
  }

  public String importItem(String itemName, String brandName, String brandCode)
      throws BeeException {

    logger.debug("ImportItem:", "importing item...");

    StringBuilder sb = new StringBuilder("<item>")
        .append(XmlUtils.tag("pavad", itemName))
        .append(XmlUtils.tag("gamintojas", brandName))
        .append(XmlUtils.tag("gam_art", brandCode))
        .append("</item>");

    String answer;

    try {
      answer = process("ImportItem", sb.toString());
    } catch (Exception e) {
      throw new BeeException(e);
    }
    answer = getNode(answer).getTextContent();

    logger.debug("ImportItem:", "import succeeded. New ItemID =", answer);

    return answer;
  }

  private SOAPMessage createMessage(String action, Map<String, String> attributes)
      throws SOAPException {
    SOAPMessage message = MessageFactory.newInstance().createMessage();

    SOAPPart soapPart = message.getSOAPPart();
    SOAPEnvelope envelope = soapPart.getEnvelope();
    SOAPBody body = envelope.getBody();

    SOAPBodyElement bodyElement = body.addBodyElement(envelope.createName(action, "ns", namespace));

    if (!BeeUtils.isEmpty(attributes)) {
      for (String attribute : attributes.keySet()) {
        bodyElement.addChildElement(attribute).addTextNode(attributes.get(attribute));
      }
    }
    return message;
  }

  private String invoke(SOAPMessage message) throws SOAPException {
    dispatch.getRequestContext().put(Dispatch.SOAPACTION_URI_PROPERTY,
        BeeUtils.join(".", actionPrefix, message.getSOAPBody().getFirstChild().getLocalName()));

    message.saveChanges();
    SOAPMessage response = dispatch.invoke(message);

    if (response.getSOAPBody().hasFault()) {
      return response.getSOAPBody().getFault().getTextContent();
    } else {
      return response.getSOAPBody().getTextContent();
    }
  }

  private String login(String login, String password) throws SOAPException {
    return invoke(createMessage("Login", ImmutableMap.of("usr", login, "pwd", password)));
  }

  private String logout() throws SOAPException {
    return invoke(createMessage("Logout", null));
  }

  private String process(String method, String param) throws SOAPException {
    return invoke(createMessage("Process", ImmutableMap.of("mthd", method, "prm", param)));
  }

  private static SimpleRowSet xmlToSimpleRowSet(String xml, String... columns) throws BeeException {
    SimpleRowSet data = new SimpleRowSet(columns);
    Node node = getNode(xml);

    if (node.hasChildNodes()) {
      for (int i = 0; i < node.getChildNodes().getLength(); i++) {
        NodeList row = node.getChildNodes().item(i).getChildNodes();
        int c = row.getLength();

        String[] cells = new String[data.getNumberOfColumns()];

        for (int j = 0; j < c; j++) {
          cells[data.getColumnIndex(row.item(j).getLocalName())] = row.item(j).getTextContent();
        }
        data.addRow(cells);
      }
    }
    return data;
  }
}