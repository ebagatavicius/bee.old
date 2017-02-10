package com.butent.webservice;

import com.google.common.collect.ImmutableMap;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.trade.acts.TradeActConstants;
import com.butent.bee.shared.utils.BeeUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
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
  private static ButentWS instance;

  String wsdlAddress;
  String defaultNamespace;
  Dispatch<SOAPMessage> dispatch;

  private ButentWS(String address) throws BeeException {
    Service service = Service.create(new QName("ButentWebService"));
    QName portName = new QName("ButentWebServicePort");

    service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);

    if (BeeUtils.isEmpty(defaultNamespace)) {
      Client client = ClientBuilder.newClient();

      try {
        Document wsdl = client.target(address)
            .request()
            .get(Document.class);

        wsdlAddress = address;
        defaultNamespace = BeeUtils.removeSuffix(wsdl.getDocumentElement()
            .getAttribute("targetNamespace"), "wsdl/");

      } catch (Exception e) {
        throw BeeException.error(e);
      } finally {
        client.close();
      }
    }
    dispatch = service.createDispatch(portName, SOAPMessage.class, Service.Mode.MESSAGE);
    dispatch.getRequestContext().put(Dispatch.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
  }

  public static ButentWS connect(String address, String login, String pass) throws BeeException {
    if (BeeUtils.anyEmpty(address, login, pass)) {
      throw new BeeException("WebService address/login/password not defined");
    }
    logger.debug("Connecting to webservice:", address);

    if (instance == null || !BeeUtils.same(address, instance.wsdlAddress)) {
      instance = new ButentWS(address);
    }
    String answer;
    String error = "Unknown login response";

    try {
      answer = instance.login(login, pass);
    } catch (Exception e) {
      throw BeeException.error(e);
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
    return instance;
  }

  public void disconnect() {
    logger.debug("Logout:", "logging out...");

    try {
      logout();
    } catch (Exception e) {
      logger.error(e);
    }
  }

  public SimpleRowSet getGoods(String filter) throws BeeException {
    logger.debug("GetGoods");
    String answer;

    try {
      answer = process("GetGoods", XmlUtils.tag("filter", filter));
    } catch (Exception e) {
      throw BeeException.error(e);
    }
    SimpleRowSet data =
        xmlToSimpleRowSet(answer, "PAVAD", "PAVAD_1", "PAVAD_3", "PAVAD_4", "PREKE",
            "MATO_VIEN", "ARTIKULAS", "PARD_KAINA", "SAVIKAINA", "KAINA_1", "KAINA_2", "KAINA_3",
            "KAINA_4", "KAINA_5", "KAINA_6", "KAINA_7", "KAINA_8", "KAINA_9", "KAINA_10", "TIPAS",
            "GRUPE", "PARD_VAL", "SAV_VAL", "VAL_1", "VAL_2", "VAL_3", "VAL_4", "VAL_5", "VAL_6",
            "VAL_7", "VAL_8", "VAL_9", "VAL_10", "PREK_SVOR", "PREK_KPN", "PREK_NETO", "KILM_SALIS",
            "ALT_MV", "ALT_KOEF", "TURIS", "BRUTO");
    logger.debug("GetGoods cols:", data.getNumberOfColumns(), "rows:", data.getNumberOfRows());
    return data;
  }

  public SimpleRowSet getGoodsR(String filter) throws BeeException {
    logger.debug("GetGoodsR");
    String answer;

    try {
      answer = process("GetGoodsR", XmlUtils.tag("filter", filter));
    } catch (Exception e) {
      throw BeeException.error(e);
    }
    SimpleRowSet data = xmlToSimpleRowSet(answer, "PAVAD_2", "PREKE");
    logger.debug("GetGoodsR cols:", data.getNumberOfColumns(), "rows:", data.getNumberOfRows());
    return data;
  }

  public SimpleRowSet getStocks(String code) throws BeeException {
    logger.debug("GetStocks");
    String answer;

    try {
      answer = process("GetStocks", XmlUtils.tag("preke", code));
    } catch (Exception e) {
      throw BeeException.error(e);
    }
    SimpleRowSet data =
        xmlToSimpleRowSet(answer, "PREKE", "SANDELIS", "LIKUTIS");
    logger.debug("GetStocks cols:", data.getNumberOfColumns(), "rows:", data.getNumberOfRows());
    return data;
  }

  public SimpleRowSet getSQLData(String query, String... columns) throws BeeException {
    logger.debug("GetSQLData:", query);
    String answer;

    try {
      answer = process("GetSQLData", "<query>" + query + "</query>");
    } catch (Exception e) {
      throw BeeException.error(e);
    }
    SimpleRowSet data = xmlToSimpleRowSet(answer, columns);
    logger.debug("GetSQLData cols:", data.getNumberOfColumns(), "rows:", data.getNumberOfRows());
    return data;
  }

  public String importClient(String companyId, String companyName, String companyCode,
      String companyVATCode, String companyAddress, String companyPostIndex, String companyCity,
      String companyCountry)
      throws BeeException {

    logger.debug("ImportClient:", "importing client...");

    StringBuilder sb = new StringBuilder("<client>")
        .append(XmlUtils.tag("common_id", companyId))
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
      throw BeeException.error(e);
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
      throw BeeException.error(e);
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
      throw BeeException.error(e);
    }
    answer = getNode(answer).getTextContent();

    logger.debug("ImportItem:", "import succeeded. New ItemID =", answer);

    return answer;
  }

  public String importItemReservation(SimpleRowSet rs)
      throws BeeException {

    StringBuilder sb = new StringBuilder("<a>");
    for (SimpleRow row : rs) {
      sb.append("<b>");
      sb.append(XmlUtils.tag("sandelis", row.getValue(COL_WAREHOUSE_CODE)));
      sb.append(XmlUtils.tag("preke", row.getValue(COL_ITEM_EXTERNAL_CODE)));
      sb.append(XmlUtils.tag("kiekis", row.getValue(TradeActConstants.ALS_TOTAL_AMOUNT)));
      sb.append("</b>");
    }
    sb.append("</a>");

    String answer;

    try {
      answer = process("Import_rezervations", sb.toString());
    } catch (Exception e) {
      throw BeeException.error(e);
    }
    answer = getNode(answer).getTextContent();

    return answer;
  }

  private SOAPMessage createMessage(String action, Map<String, String> attributes)
      throws SOAPException {
    SOAPMessage message = MessageFactory.newInstance().createMessage();

    SOAPPart soapPart = message.getSOAPPart();
    SOAPEnvelope envelope = soapPart.getEnvelope();
    SOAPBody body = envelope.getBody();

    SOAPBodyElement bodyElement = body.addBodyElement(envelope.createName(action, "ns",
        defaultNamespace + "message/"));

    if (!BeeUtils.isEmpty(attributes)) {
      for (String attribute : attributes.keySet()) {
        bodyElement.addChildElement(attribute).addTextNode(attributes.get(attribute));
      }
    }
    return message;
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

  private String invoke(SOAPMessage message) throws SOAPException {
    dispatch.getRequestContext().put(Dispatch.SOAPACTION_URI_PROPERTY,
        BeeUtils.join(".", defaultNamespace + "action/ButentWebService",
            message.getSOAPBody().getFirstChild().getLocalName()));

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

        String[] cells = null;

        for (int j = 0; j < c; j++) {
          String col = row.item(j).getLocalName();

          if (data.hasColumn(col)) {
            if (cells == null) {
              cells = new String[data.getNumberOfColumns()];
            }
            cells[data.getColumnIndex(col)] = row.item(j).getTextContent();
          }
        }
        if (cells != null) {
          data.addRow(cells);
        }
      }
    }
    return data;
  }
}