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
          .append(XmlUtils.tag("kitas_dok2", number))
          .append(XmlUtils.tag("dok_serija", invoicePrefix))
          .append(XmlUtils.tag("kitas_dok", invoiceNumber))
          .append(XmlUtils.tag("terminas", term))
          .append(XmlUtils.tag("valiuta", currency))
          .append(XmlUtils.tag("preke", item.itemId))
          .append(XmlUtils.tag("kiekis", item.quantity))
          .append(XmlUtils.tag("artikulas", item.article))
          .append(XmlUtils.tag("pastaba", item.note));

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
