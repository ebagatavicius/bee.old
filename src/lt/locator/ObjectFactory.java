package lt.locator;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the lt.locator package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation
 * for XML content. The Java representation of XML content can consist of schema derived interfaces
 * and classes representing the binding of schema type definitions, element declarations and model
 * groups. Factory methods for each of these are provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

  private final static QName _GetSummarizedReport_QNAME = new QName(
      "http://soap.wsbridge.ws.loctracker.msc/", "getSummarizedReport");
  private final static QName _GetSummarizedReportResponse_QNAME = new QName(
      "http://soap.wsbridge.ws.loctracker.msc/", "getSummarizedReportResponse");

  /**
   * Create a new ObjectFactory that can be used to create new instances of schema derived classes
   * for package: lt.locator
   * 
   */
  public ObjectFactory() {
  }

  /**
   * Create an instance of {@link GetSummarizedReportResponse }
   * 
   */
  public GetSummarizedReportResponse createGetSummarizedReportResponse() {
    return new GetSummarizedReportResponse();
  }

  /**
   * Create an instance of {@link GetSummarizedReport }
   * 
   */
  public GetSummarizedReport createGetSummarizedReport() {
    return new GetSummarizedReport();
  }

  /**
   * Create an instance of {@link ReportSummarizedPeriod }
   * 
   */
  public ReportSummarizedPeriod createReportSummarizedPeriod() {
    return new ReportSummarizedPeriod();
  }

  /**
   * Create an instance of {@link TripSumRepData }
   * 
   */
  public TripSumRepData createTripSumRepData() {
    return new TripSumRepData();
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link GetSummarizedReport }{@code >}
   * 
   */
  @XmlElementDecl(namespace = "http://soap.wsbridge.ws.loctracker.msc/", name = "getSummarizedReport")
  public JAXBElement<GetSummarizedReport> createGetSummarizedReport(GetSummarizedReport value) {
    return new JAXBElement<>(_GetSummarizedReport_QNAME, GetSummarizedReport.class, null, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link GetSummarizedReportResponse }{@code >}
   * 
   */
  @XmlElementDecl(namespace = "http://soap.wsbridge.ws.loctracker.msc/", name = "getSummarizedReportResponse")
  public JAXBElement<GetSummarizedReportResponse> createGetSummarizedReportResponse(
      GetSummarizedReportResponse value) {
    return new JAXBElement<>(_GetSummarizedReportResponse_QNAME, GetSummarizedReportResponse.class,
        null, value);
  }

}
