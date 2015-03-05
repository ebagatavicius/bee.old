package lt.lb.webservices.exchangerates;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService(name = ExchangeRatesWS.NAME, targetNamespace = ExchangeRatesWS.NAMESPACE)
public interface ExchangeRatesSoapPort {

  @WebMethod(action = ExchangeRatesWS.ACTION + "/getCurrencyList")
  @WebResult(name = "getCurrencyListResult", targetNamespace = ExchangeRatesWS.NAMESPACE)
  public List<Object> getCurrencyList();

  @WebMethod(action = ExchangeRatesWS.ACTION + "/getCurrentFxRates")
  @WebResult(name = "getCurrentFxRatesResult", targetNamespace = ExchangeRatesWS.NAMESPACE)
  public List<Object> getCurrentFxRates(
      @WebParam(name = "tp", targetNamespace = ExchangeRatesWS.NAMESPACE) String tp);

  @WebMethod(action = ExchangeRatesWS.ACTION + "/getFxRates")
  @WebResult(name = "getFxRatesResult", targetNamespace = ExchangeRatesWS.NAMESPACE)
  public List<Object> getFxRates(
      @WebParam(name = "tp", targetNamespace = ExchangeRatesWS.NAMESPACE) String tp,
      @WebParam(name = "dt", targetNamespace = ExchangeRatesWS.NAMESPACE) String dt);

  @WebMethod(action = ExchangeRatesWS.ACTION + "/getFxRatesForCurrency")
  @WebResult(name = "getFxRatesForCurrencyResult", targetNamespace = ExchangeRatesWS.NAMESPACE)
  public List<Object> getFxRatesForCurrency(
      @WebParam(name = "tp", targetNamespace = ExchangeRatesWS.NAMESPACE) String tp,
      @WebParam(name = "ccy", targetNamespace = ExchangeRatesWS.NAMESPACE) String ccy,
      @WebParam(name = "dtFrom", targetNamespace = ExchangeRatesWS.NAMESPACE) String dtFrom,
      @WebParam(name = "dtTo", targetNamespace = ExchangeRatesWS.NAMESPACE) String dtTo);
}
