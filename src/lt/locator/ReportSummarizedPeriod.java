
package lt.locator;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for reportSummarizedPeriod complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="reportSummarizedPeriod">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="deviceName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="errorCode" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="errorMessage" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="timeFrom" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="timeTo" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="tripSummary" type="{http://soap.wsbridge.ws.loctracker.msc/}tripSumRepData" minOccurs="0"/>
 *         &lt;element name="tripSummaryByCountries" type="{http://soap.wsbridge.ws.loctracker.msc/}tripSumRepData" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="tripSummaryByCountriesGrouped" type="{http://soap.wsbridge.ws.loctracker.msc/}tripSumRepData" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="tripSummaryByDays" type="{http://soap.wsbridge.ws.loctracker.msc/}tripSumRepData" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="vehicleRegistrationNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "reportSummarizedPeriod", propOrder = {
    "deviceName",
    "errorCode",
    "errorMessage",
    "timeFrom",
    "timeTo",
    "tripSummary",
    "tripSummaryByCountries",
    "tripSummaryByCountriesGrouped",
    "tripSummaryByDays",
    "vehicleRegistrationNumber"
})
public class ReportSummarizedPeriod {

    protected String deviceName;
    protected int errorCode;
    protected String errorMessage;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar timeFrom;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar timeTo;
    protected TripSumRepData tripSummary;
    @XmlElement(nillable = true)
    protected List<TripSumRepData> tripSummaryByCountries;
    @XmlElement(nillable = true)
    protected List<TripSumRepData> tripSummaryByCountriesGrouped;
    @XmlElement(nillable = true)
    protected List<TripSumRepData> tripSummaryByDays;
    protected String vehicleRegistrationNumber;

    /**
     * Gets the value of the deviceName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Sets the value of the deviceName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeviceName(String value) {
        this.deviceName = value;
    }

    /**
     * Gets the value of the errorCode property.
     * 
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the value of the errorCode property.
     * 
     */
    public void setErrorCode(int value) {
        this.errorCode = value;
    }

    /**
     * Gets the value of the errorMessage property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the value of the errorMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorMessage(String value) {
        this.errorMessage = value;
    }

    /**
     * Gets the value of the timeFrom property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimeFrom() {
        return timeFrom;
    }

    /**
     * Sets the value of the timeFrom property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTimeFrom(XMLGregorianCalendar value) {
        this.timeFrom = value;
    }

    /**
     * Gets the value of the timeTo property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimeTo() {
        return timeTo;
    }

    /**
     * Sets the value of the timeTo property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTimeTo(XMLGregorianCalendar value) {
        this.timeTo = value;
    }

    /**
     * Gets the value of the tripSummary property.
     * 
     * @return
     *     possible object is
     *     {@link TripSumRepData }
     *     
     */
    public TripSumRepData getTripSummary() {
        return tripSummary;
    }

    /**
     * Sets the value of the tripSummary property.
     * 
     * @param value
     *     allowed object is
     *     {@link TripSumRepData }
     *     
     */
    public void setTripSummary(TripSumRepData value) {
        this.tripSummary = value;
    }

    /**
     * Gets the value of the tripSummaryByCountries property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tripSummaryByCountries property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTripSummaryByCountries().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TripSumRepData }
     * 
     * 
     */
    public List<TripSumRepData> getTripSummaryByCountries() {
        if (tripSummaryByCountries == null) {
            tripSummaryByCountries = new ArrayList<TripSumRepData>();
        }
        return this.tripSummaryByCountries;
    }

    /**
     * Gets the value of the tripSummaryByCountriesGrouped property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tripSummaryByCountriesGrouped property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTripSummaryByCountriesGrouped().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TripSumRepData }
     * 
     * 
     */
    public List<TripSumRepData> getTripSummaryByCountriesGrouped() {
        if (tripSummaryByCountriesGrouped == null) {
            tripSummaryByCountriesGrouped = new ArrayList<TripSumRepData>();
        }
        return this.tripSummaryByCountriesGrouped;
    }

    /**
     * Gets the value of the tripSummaryByDays property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tripSummaryByDays property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTripSummaryByDays().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TripSumRepData }
     * 
     * 
     */
    public List<TripSumRepData> getTripSummaryByDays() {
        if (tripSummaryByDays == null) {
            tripSummaryByDays = new ArrayList<TripSumRepData>();
        }
        return this.tripSummaryByDays;
    }

    /**
     * Gets the value of the vehicleRegistrationNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    /**
     * Sets the value of the vehicleRegistrationNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVehicleRegistrationNumber(String value) {
        this.vehicleRegistrationNumber = value;
    }

}
