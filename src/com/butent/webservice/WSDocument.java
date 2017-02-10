package com.butent.webservice;

import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class WSDocument {

  public static final class WSDocumentItem {
    private final String itemId;
    private final String quantity;
    private String price;
    private String vatMode;
    private String vat;
    private String vatPercent;
    private String article;
    private String note;

    private WSDocumentItem(String itemId, String quantity) {
      this.itemId = itemId;
      this.quantity = quantity;
    }

    public void setArticle(String article) {
      this.article = article;
    }

    public void setNote(String note) {
      this.note = note;
    }

    public void setPrice(String price) {
      this.price = price;
    }

    public void setVat(String vatAmount, Boolean isPercent, Boolean isPlus) {
      if (!BeeUtils.isEmpty(vatAmount)) {
        this.vat = vatAmount;
        this.vatPercent = BeeUtils.unbox(isPercent) ? "%" : null;
        this.vatMode = BeeUtils.unbox(isPlus) ? "S" : "T";
      }
    }
  }

  private final String documentId;
  private final DateTime date;
  private final String operation;
  private final String warehouse;
  private final String company;

  private String number;
  private String invoicePrefix;
  private String invoiceNumber;

  private JustDate term;

  private String supplier;
  private String customer;
  private String payer;
  private String currency;
  private String manager;

  private String bolSeries;
  private String bolNumber;
  private String bolLoadingPlace;
  private String bolUnloadingPlace;
  private String bolVehicleNumber;
  private String bolDriver;
  private String bolCarrier;
  private String bolDepartureDate;
  private String bolUnloadingDate;
  private String bolIssueDate;
  private String bolDriverTabNo;

  private final List<WSDocumentItem> items = new ArrayList<>();

  public WSDocument(String documentId, DateTime date, String operation, String company,
      String warehouse) {
    this.documentId = documentId;
    this.date = date;
    this.operation = operation;
    this.company = company;
    this.warehouse = warehouse;
  }

  public WSDocumentItem addItem(String itemId, String quantity) {
    WSDocumentItem item = new WSDocumentItem(itemId, quantity);
    items.add(item);
    return item;
  }

  public String getXml() {
    StringBuilder sb = new StringBuilder("<VFPData>");

    for (WSDocumentItem item : items) {
      sb.append("<row>")
          .append(XmlUtils.tag("apyv_id", documentId))
          .append(XmlUtils.tag("data", date))
          .append(XmlUtils.tag("operacija", operation))
          .append(XmlUtils.tag("sandelis", warehouse))
          .append(XmlUtils.tag("klientas", company))
          .append(XmlUtils.tag("tiekejas", supplier))
          .append(XmlUtils.tag("gavejas", customer))
          .append(XmlUtils.tag("moketojas", payer))
          .append(XmlUtils.tag("manager", manager))
          .append(XmlUtils.tag("kitas_dok2", number))
          .append(XmlUtils.tag("dok_serija", invoicePrefix))
          .append(XmlUtils.tag("kitas_dok", invoiceNumber))
          .append(XmlUtils.tag("terminas", term))
          .append(XmlUtils.tag("valiuta", currency))
          .append(XmlUtils.tag("preke", item.itemId))
          .append(XmlUtils.tag("kiekis", item.quantity))
          .append(XmlUtils.tag("artikulas", item.article))
          .append(XmlUtils.tag("pastaba", item.note));

      if (!BeeUtils.isEmpty(bolSeries)) {
        sb.append(XmlUtils.tag("vaz_serija", bolSeries));
      }
      if (!BeeUtils.isEmpty(bolNumber)) {
        sb.append(XmlUtils.tag("vaz_nr", bolNumber));
      }
      if (!BeeUtils.isEmpty(bolLoadingPlace)) {
        sb.append(XmlUtils.tag("pakr_vieta", bolLoadingPlace));
      }
      if (!BeeUtils.isEmpty(bolUnloadingPlace)) {
        sb.append(XmlUtils.tag("iskr_vieta", bolUnloadingPlace));
      }
      if (!BeeUtils.isEmpty(bolVehicleNumber)) {
        sb.append(XmlUtils.tag("tran_priem", bolVehicleNumber));
      }
      if (!BeeUtils.isEmpty(bolDriver)) {
        sb.append(XmlUtils.tag("vairuotoj", bolDriver));
      }
      if (!BeeUtils.isEmpty(bolCarrier)) {
        sb.append(XmlUtils.tag("tran_kl", bolCarrier));
      }
      if (!BeeUtils.isEmpty(bolDepartureDate)) {
        sb.append(XmlUtils.tag("vaz_isgab", bolDepartureDate));
      }
      if (!BeeUtils.isEmpty(bolUnloadingDate)) {
        sb.append(XmlUtils.tag("iskr_time", bolUnloadingDate));
      }
      if (!BeeUtils.isEmpty(bolIssueDate)) {
        sb.append(XmlUtils.tag("pakr_time", bolIssueDate));
      }
      if (!BeeUtils.isEmpty(bolDriverTabNo)) {
        sb.append(XmlUtils.tag("tab_nr", bolDriverTabNo));
      }

      if (!BeeUtils.isEmpty(item.price)) {
        sb.append(XmlUtils.tag("kaina", item.price));

        if (!BeeUtils.isEmpty(item.vat)) {
          sb.append(XmlUtils.tag("pvm_stat", item.vatMode))
              .append(XmlUtils.tag("pvm", item.vat))
              .append(XmlUtils.tag("pvm_p_md", item.vatPercent));
        }
      }
      sb.append("</row>");
    }
    return sb.append("</VFPData>").toString();
  }

  public void setBolSeries(String bolSeries) {
    this.bolSeries = bolSeries;
  }

  public void setBolNumber(String bolNumber) {
    this.bolNumber = bolNumber;
  }

  public void setBolLoadingPlace(String bolLoadingPlace) {
    this.bolLoadingPlace = bolLoadingPlace;
  }

  public void setBolUnloadingPlace(String bolUnloadingPlace) {
    this.bolUnloadingPlace = bolUnloadingPlace;
  }

  public void setBolVehicleNumber(String bolVehicleNumber) {
    this.bolVehicleNumber = bolVehicleNumber;
  }

  public void setBolDriver(String bolDriver) {
    this.bolDriver = bolDriver;
  }

  public void setBolCarrier(String bolCarrier) {
    this.bolCarrier = bolCarrier;
  }

  public void setBolDepartureDate(String bolDepartureDate) {
    this.bolDepartureDate = bolDepartureDate;
  }

  public void setBolUnloadingDate(String bolUnloadingDate) {
    this.bolUnloadingDate = bolUnloadingDate;
  }

  public void setBolIssueDate(String bolIssueDate) {
    this.bolIssueDate = bolIssueDate;
  }

  public void setBolDriverTabNo(String bolDriverTabNo) {
    this.bolDriverTabNo = bolDriverTabNo;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public void setCustomer(String customer) {
    this.customer = customer;
  }

  public void setInvoice(String prefix, String no) {
    this.invoicePrefix = prefix;
    this.invoiceNumber = no;
  }

  public void setManager(String manager) {
    this.manager = manager;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public void setPayer(String payer) {
    this.payer = payer;
  }

  public void setSupplier(String supplier) {
    this.supplier = supplier;
  }

  public void setTerm(JustDate term) {
    this.term = term;
  }
}
