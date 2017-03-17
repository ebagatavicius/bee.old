package lt.lb.webservices.exchangerates;

import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = ExchangeRatesWS.NAME, targetNamespace = ExchangeRatesWS.NAMESPACE)
public class ExchangeRatesWS extends Service {

  public static final String COL_TP = "Tp";
  public static final String COL_DT = "Dt";

  public static final String COL_CCY_1 = "Ccy1";
  public static final String COL_AMT_1 = "Amt1";
  public static final String COL_CCY_2 = "Ccy2";
  public static final String COL_AMT_2 = "Amt2";

  static final String NAMESPACE = "http://www.lb.lt/WebServices/FxRates";
  static final String PORT = "FxRatesSoap";
  static final String NAME = "FxRates";
  static final String ACTION = "http://www.lb.lt/WebServices/FxRates";

  private static final String DEFAULT_WSDL =
      "http://www.lb.lt/WebServices/FxRates/FxRates.asmx?WSDL";
  private static final String DEFAULT_TYPE = "LT";

  private static BeeLogger logger = LogUtils.getLogger(ExchangeRatesWS.class);

  public static ResponseObject getCurrentExchangeRates(String address, String type,
      String currency) {

    ResponseObject response = getPort(BeeUtils.notEmpty(address, DEFAULT_WSDL));
    if (response.hasErrors()) {
      return response;
    }

    String svc = "getCurrentFxRates";

    String tp = BeeUtils.notEmpty(type, DEFAULT_TYPE);
    logger.info(svc, tp, currency);

    List<Object> result;

    try {
      result = ((ExchangeRatesSoapPort) response.getResponse()).getCurrentFxRates(tp);
    } catch (Exception e) {
      logger.error(e);
      return ResponseObject.error(e);
    }
    if (BeeUtils.isEmpty(result)) {
      return ResponseObject.error(svc, "result is empty");
    }

    SimpleRowSet data = new SimpleRowSet(new String[] {
        COL_TP, COL_DT, COL_CCY_1, COL_AMT_1, COL_CCY_2, COL_AMT_2});
    int currencyIndex = data.getColumnIndex(COL_CCY_2);

    for (Object root : result) {
      if (root instanceof Element) {
        NodeList nodes = ((Element) root).getFirstChild().getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
          String[] row = new String[data.getNumberOfColumns()];

          List<Element> children = getLeaves(nodes.item(i));
          for (int j = 0; j < children.size() && j < data.getNumberOfColumns(); j++) {
            row[j] = BeeUtils.trim(children.get(j).getTextContent());
          }

          if (BeeUtils.isEmpty(currency) || BeeUtils.same(currency, row[currencyIndex])) {
            data.addRow(row);
          }
        }

      } else {
        logger.warning("Unknown webservice output type", root.getClass().getName());
      }
    }

    logger.info(svc, "rows:", data.getNumberOfRows());

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.warning(svc, tp, currency, "no data");
    } else {
      return ResponseObject.response(data);
    }
  }

  public static ResponseObject getExchangeRate(String address, String type, String currency,
      JustDate date) {

    ResponseObject response = getPort(BeeUtils.notEmpty(address, DEFAULT_WSDL));
    if (response.hasErrors()) {
      return response;
    }

    String tp = BeeUtils.notEmpty(type, DEFAULT_TYPE);
    String dt = format(date);

    String svc = "getFxRates";

    logger.info(svc, tp, currency, date);

    List<Object> result;

    try {
      result = ((ExchangeRatesSoapPort) response.getResponse()).getFxRates(tp, dt);
    } catch (Exception e) {
      logger.error(e);
      return ResponseObject.error(e);
    }
    if (BeeUtils.isEmpty(result)) {
      return ResponseObject.error(svc, "result is empty");
    }

    SimpleRowSet data = new SimpleRowSet(new String[] {
        COL_TP, COL_DT, COL_CCY_1, COL_AMT_1, COL_CCY_2, COL_AMT_2});
    int currencyIndex = data.getColumnIndex(COL_CCY_2);

    for (Object root : result) {
      if (root instanceof Element) {
        NodeList nodes = ((Element) root).getFirstChild().getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
          String[] row = new String[data.getNumberOfColumns()];

          List<Element> children = getLeaves(nodes.item(i));
          for (int j = 0; j < children.size() && j < data.getNumberOfColumns(); j++) {
            row[j] = BeeUtils.trim(children.get(j).getTextContent());
          }

          if (BeeUtils.isEmpty(currency) || BeeUtils.same(currency, row[currencyIndex])) {
            data.addRow(row);
          }
        }

      } else {
        logger.warning("Unknown webservice output type", root.getClass().getName());
      }
    }

    logger.info(svc, "rows:", data.getNumberOfRows());

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.warning(svc, tp, currency, date, "no data");
    } else {
      return ResponseObject.response(data);
    }
  }

  public static ResponseObject getExchangeRatesForCurrency(String address, String type,
      String currency, JustDate dateLow, JustDate dateHigh) {

    ResponseObject response = getPort(BeeUtils.notEmpty(address, DEFAULT_WSDL));
    if (response.hasErrors()) {
      return response;
    }

    String tp = BeeUtils.notEmpty(type, DEFAULT_TYPE);
    String low = format(dateLow);
    String high = format(dateHigh);

    String svc = "getFxRatesForCurrency";

    logger.info(svc, tp, currency, dateLow, dateHigh);

    List<Object> result;

    try {
      result = ((ExchangeRatesSoapPort) response.getResponse())
          .getFxRatesForCurrency(tp, currency, low, high);
    } catch (Exception e) {
      logger.error(e);
      return ResponseObject.error(e);
    }
    if (BeeUtils.isEmpty(result)) {
      return ResponseObject.error(svc, "result is empty");
    }

    SimpleRowSet data = new SimpleRowSet(new String[] {
        COL_TP, COL_DT, COL_CCY_1, COL_AMT_1, COL_CCY_2, COL_AMT_2});

    for (Object root : result) {
      if (root instanceof Element) {
        NodeList nodes = ((Element) root).getFirstChild().getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
          String[] row = new String[data.getNumberOfColumns()];

          List<Element> children = getLeaves(nodes.item(i));
          for (int j = 0; j < children.size() && j < data.getNumberOfColumns(); j++) {
            row[j] = BeeUtils.trim(children.get(j).getTextContent());
          }

          data.addRow(row);
        }

      } else {
        logger.warning("Unknown webservice output type", root.getClass().getName());
      }
    }

    logger.info(svc, "rows:", data.getNumberOfRows());

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.warning(svc, tp, currency, dateLow, dateHigh, "no data");
    } else {
      return ResponseObject.response(data);
    }
  }

  public static ResponseObject getListOfCurrencies(String address) {
    ResponseObject response = getPort(BeeUtils.notEmpty(address, DEFAULT_WSDL));
    if (response.hasErrors()) {
      return response;
    }

    String svc = "getCurrencyList";
    logger.info(svc);

    List<Object> result;

    try {
      result = ((ExchangeRatesSoapPort) response.getResponse()).getCurrencyList();
    } catch (Exception e) {
      logger.error(e);
      return ResponseObject.error(e);
    }
    if (BeeUtils.isEmpty(result)) {
      return ResponseObject.error(svc, "result is empty");
    }

    SimpleRowSet data = new SimpleRowSet(new String[] {"Ccy", "NmLt", "NmEn", "Nbr", "MnrUnts"});

    for (Object root : result) {
      if (root instanceof Element) {
        NodeList nodes = ((Element) root).getFirstChild().getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
          String[] row = new String[data.getNumberOfColumns()];

          List<Element> children = XmlUtils.getAllDescendantElements(nodes.item(i));
          for (int j = 0; j < children.size() && j < data.getNumberOfColumns(); j++) {
            row[j] = BeeUtils.trim(children.get(j).getTextContent());
          }

          data.addRow(row);
        }
      } else {
        logger.warning("Unknown webservice output type", root.getClass().getName());
      }
    }

    logger.info(svc, "rows:", data.getNumberOfRows());

    return ResponseObject.response(data);
  }

  private static String format(JustDate date) {
    if (date == null) {
      return null;
    } else {
      return DateTimeFormat.of(PredefinedFormat.DATE_SHORT,
          SupportedLocale.LT.getDateTimeFormatInfo()).format(date);
    }
  }

  private static List<Element> getLeaves(Node root) {
    List<Element> leaves = new ArrayList<>();
    if (root == null) {
      return leaves;
    }

    for (Element child : XmlUtils.getChildrenElements(root)) {
      List<Element> descendants = getLeaves(child);

      if (descendants.isEmpty()) {
        leaves.add(child);
      } else {
        leaves.addAll(descendants);
      }
    }

    return leaves;
  }

  private static ResponseObject getPort(String address) {
    if (BeeUtils.isEmpty(address)) {
      return ResponseObject.error("Webservice address not defined");
    }

    logger.info("Connecting to webservice:", address);

    ExchangeRatesWS exchangeRatesWS;

    try {
      exchangeRatesWS = new ExchangeRatesWS(new URL(address));
    } catch (Exception e) {
      logger.error(e);
      return ResponseObject.error(e);
    }

    ExchangeRatesSoapPort port = exchangeRatesWS.getExchangeRatesSoapPort();
    ((BindingProvider) port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        exchangeRatesWS.getWSDLDocumentLocation().toString());

    return ResponseObject.response(port);
  }

  private ExchangeRatesWS(URL wsdlLocation) {
    super(wsdlLocation, new QName(NAMESPACE, NAME));
  }

  @WebEndpoint(name = PORT)
  public ExchangeRatesSoapPort getExchangeRatesSoapPort() {
    return super.getPort(new QName(NAMESPACE, PORT), ExchangeRatesSoapPort.class);
  }

  @WebEndpoint(name = PORT)
  public ExchangeRatesSoapPort getExchangeRatesSoapPort(WebServiceFeature... features) {
    return super.getPort(new QName(NAMESPACE, PORT), ExchangeRatesSoapPort.class, features);
  }
}
