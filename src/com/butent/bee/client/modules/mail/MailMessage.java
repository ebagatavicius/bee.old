package com.butent.bee.client.modules.mail;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.mail.MailConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.BrowsingContext;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DateTimeLabel;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.Link;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailConstants.AddressType;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MailMessage extends AbstractFormInterceptor {

  private class RelationsHandler implements SelectorEvent.Handler {

    @Override
    public void onDataSelector(final SelectorEvent event) {
      if (event.isNewRow()) {
        final String viewName = event.getRelatedViewName();

        switch (viewName) {
          case TBL_REQUESTS:
          case TBL_TASKS:
          case TransportConstants.TBL_ASSESSMENTS:
          case DocumentConstants.TBL_DOCUMENTS:
            event.consume();
            final String formName = event.getNewRowFormName();
            final DataSelector selector = event.getSelector();
            final BeeRow row = event.getNewRow();

            final ScheduledCommand executor = new ScheduledCommand() {
              private int counter;

              @Override
              public void execute() {
                if (++counter == 2) {
                  if (!BeeUtils.same(viewName, TransportConstants.TBL_ASSESSMENTS)) {
                    FileCollector.pushFiles(attachments);
                  }
                  RowFactory.createRelatedRow(formName, row, selector);
                }
              }
            };
            final Filter filter = Filter.isEqual(COL_EMAIL_ADDRESS, Value.getValue(sender.getA()));

            Queries.getRowSet(TBL_COMPANY_PERSONS,
                Lists.newArrayList(COL_COMPANY, "CompanyName", COL_FIRST_NAME, COL_LAST_NAME),
                filter, new Queries.RowSetCallback() {
                  @Override
                  public void onSuccess(BeeRowSet result) {
                    if (result.getNumberOfRows() == 1) {
                      Long company = result.getLong(0, COL_COMPANY);
                      String companyName = result.getString(0, "CompanyName");
                      Long persion = result.getRow(0).getId();
                      String firstName = result.getString(0, COL_FIRST_NAME);
                      String lastName = result.getString(0, COL_LAST_NAME);

                      switch (viewName) {
                        case TBL_TASKS:
                          Data.setValue(viewName, row, COL_COMPANY, company);
                          Data.setValue(viewName, row, "CompanyName", companyName);
                          Data.setValue(viewName, row, "ContactPerson", persion);
                          Data.setValue(viewName, row, "ContactFirstName", firstName);
                          Data.setValue(viewName, row, "ContactLastName", lastName);
                          break;

                        case TBL_REQUESTS:
                        case TransportConstants.TBL_ASSESSMENTS:
                          Data.setValue(viewName, row, "Customer", company);
                          Data.setValue(viewName, row, "CustomerName", companyName);
                          Data.setValue(viewName, row, "CustomerPerson", persion);
                          Data.setValue(viewName, row, "PersonFirstName", firstName);
                          Data.setValue(viewName, row, "PersonLastName", lastName);
                          break;
                      }
                      executor.execute();

                    } else {
                      Queries.getRowSet(TBL_COMPANIES, Lists.newArrayList(COL_COMPANY_NAME),
                          filter, new Queries.RowSetCallback() {
                            @Override
                            public void onSuccess(BeeRowSet res) {
                              if (res.getNumberOfRows() == 1) {
                                Long company = res.getRow(0).getId();
                                String companyName = res.getString(0, COL_COMPANY_NAME);

                                switch (viewName) {
                                  case TBL_TASKS:
                                    Data.setValue(viewName, row, COL_COMPANY, company);
                                    Data.setValue(viewName, row, "CompanyName", companyName);
                                    break;

                                  case TBL_REQUESTS:
                                  case TransportConstants.TBL_ASSESSMENTS:
                                    Data.setValue(viewName, row, "Customer", company);
                                    Data.setValue(viewName, row, "CustomerName", companyName);
                                    break;
                                }
                              }
                              executor.execute();
                            }
                          });
                    }
                  }
                });
            ParameterList params = MailKeeper.createArgs(SVC_STRIP_HTML);
            params.addDataItem(COL_HTML_CONTENT, getContent());

            BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                response.notify(getFormView());

                if (!response.hasErrors()) {
                  switch (viewName) {
                    case TBL_TASKS:
                      Data.setValue(viewName, row, COL_SUMMARY, getSubject());
                      Data.setValue(viewName, row, COL_DESCRIPTION, response.getResponseAsString());
                      break;

                    case TBL_REQUESTS:
                      Data.setValue(viewName, row, COL_CONTENT, response.getResponseAsString());
                      break;

                    case TransportConstants.TBL_ASSESSMENTS:
                      Data.setValue(viewName, row, "OrderNotes", response.getResponseAsString());
                      break;

                    case DocumentConstants.TBL_DOCUMENTS:
                      Data.setValue(viewName, row, DocumentConstants.COL_DOCUMENT_NAME,
                          getSubject());

                      Global.getParameter("DefaultDocumentCategory", new Consumer<String>() {
                        @Override
                        public void accept(String input) {
                          Data.setValue(viewName, row, DocumentConstants.COL_DOCUMENT_CATEGORY,
                              input);
                          executor.execute();
                        }
                      });
                      return;
                  }
                }
                executor.execute();
              }
            });
            break;
        }
      }
    }
  }

  private static enum NewMailMode {
    REPLY, REPLY_ALL, FORWARD
  }

  private final ClickHandler attachmentsHandler = new ClickHandler() {
    @Override
    public void onClick(ClickEvent event) {
      event.stopPropagation();

      if (attachments.size() == 1) {
        FileInfo file = attachments.get(0);
        BrowsingContext.open(FileUtils.getUrl(file.getName(), file.getId()));
      } else {
        final Popup popup = new Popup(OutsideClick.CLOSE,
            BeeConst.CSS_CLASS_PREFIX + "mail-AttachmentsPopup");
        TabBar bar = new TabBar(BeeConst.CSS_CLASS_PREFIX + "mail-AttachmentsMenu-",
            Orientation.VERTICAL);

        for (FileInfo file : attachments) {
          Link link = new Link(BeeUtils.joinWords(file.getName(),
              BeeUtils.parenthesize(FileUtils.sizeToText(file.getSize()))),
              FileUtils.getUrl(file.getName(), file.getId()));

          link.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent ev) {
              popup.close();
            }
          });
          bar.addItem(link);
        }
        popup.setWidget(bar);
        popup.setHideOnEscape(true);
        popup.showRelativeTo(widgets.get(ATTACHMENTS).asWidget().getElement());
      }
    }
  };

  private static final String WAITING = "Waiting";
  private static final String CONTAINER = "Container";
  private static final String RECIPIENTS = "Recipients";
  private static final String ATTACHMENTS = "Attachments";
  private static final String PARTS = "Parts";
  private static final String SENDER = "Sender";
  private static final String DATE = "Date";
  private static final String SUBJECT = "Subject";

  private final MailPanel mailPanel;
  private Long draftId;
  private Long rawId;
  private Pair<String, String> sender;
  private final Multimap<String, Pair<String, String>> recipients = HashMultimap.create();
  private final List<FileInfo> attachments = new ArrayList<>();
  private final Map<String, Widget> widgets = new HashMap<>();

  private Relations relations;

  public MailMessage() {
    this(null);
  }

  MailMessage(MailPanel mailPanel) {
    this.mailPanel = mailPanel;
    widgets.put(WAITING, null);
    widgets.put(CONTAINER, null);
    widgets.put(RECIPIENTS, null);
    widgets.put(ATTACHMENTS, null);
    widgets.put(PARTS, null);
    widgets.put(SENDER, null);
    widgets.put(DATE, null);
    widgets.put(SUBJECT, null);
  }

  @Override
  public void afterCreateWidget(String name, final IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widgets.containsKey(name)) {
      widgets.put(name, widget.asWidget());
    }
    if (widget instanceof HasClickHandlers) {
      HasClickHandlers clickWidget = (HasClickHandlers) widget;

      NewMailMode mode = EnumUtils.getEnumByName(NewMailMode.class, name);

      if (mode != null) {
        initCreateAction(clickWidget, mode);

      } else if (BeeUtils.same(name, "Menu")) {
        clickWidget.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            event.stopPropagation();
            Popup popup = new Popup(OutsideClick.CLOSE,
                BeeConst.CSS_CLASS_PREFIX + "mail-MenuPopup");
            final HtmlTable ft = new HtmlTable(BeeConst.CSS_CLASS_PREFIX + "mail-MenuTable");

            int r = 0;

            ft.setWidget(r, 0, new FaLabel(FontAwesome.FILE_TEXT_O));
            ft.setText(r, 1, Localized.getConstants().mailShowOriginal());
            DomUtils.setDataIndex(ft.getRow(r), r++);

            ft.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent ev) {
                Element targetElement = EventUtils.getEventTargetElement(ev);
                TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);
                int index = DomUtils.getDataIndexInt(rowElement);
                UiHelper.closeDialog(ft);

                switch (index) {
                  case 0:
                    BrowsingContext.open(GWT.getHostPageBaseURL() + FileUtils.OPEN_URL + "/"
                        + rawId);
                    break;
                }
              }
            });
            popup.setWidget(ft);
            popup.setHideOnEscape(true);
            popup.showRelativeTo(widget.getElement());
          }
        });
      } else if (BeeUtils.same(name, RECIPIENTS)) {
        clickWidget.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            event.stopPropagation();
            Popup popup =
                new Popup(OutsideClick.CLOSE, BeeConst.CSS_CLASS_PREFIX + "mail-RecipientsPopup");
            HtmlTable ft = new HtmlTable();
            ft.setBorderSpacing(5);
            LocalizableConstants loc = Localized.getConstants();

            for (Entry<AddressType, String> entry : ImmutableMap.of(AddressType.TO, loc.mailTo(),
                AddressType.CC, loc.mailCc(), AddressType.BCC, loc.mailBcc()).entrySet()) {

              String type = entry.getKey().name();

              if (recipients.containsKey(type)) {
                int c = ft.getRowCount();
                ft.getCellFormatter().setStyleName(c, 0,
                    BeeConst.CSS_CLASS_PREFIX + "mail-RecipientsType");
                ft.setHtml(c, 0, entry.getValue() + ":");
                FlowPanel fp = new FlowPanel();

                for (Pair<String, String> address : recipients.get(type)) {
                  FlowPanel adr = new FlowPanel();
                  adr.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Recipient");

                  String email = address.getA();
                  String label = address.getB();

                  if (!BeeUtils.isEmpty(label)) {
                    InlineLabel nm = new InlineLabel(label);
                    nm.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-RecipientLabel");
                    adr.add(nm);
                  }
                  InlineLabel nm = new InlineLabel(email);
                  nm.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-RecipientEmail");
                  adr.add(nm);

                  fp.add(adr);
                }
                ft.setWidget(c, 1, fp);
              }
            }
            popup.setWidget(ft);
            popup.setHideOnEscape(true);
            popup.showOnTop(widget.asWidget().getElement());
          }
        });
      } else if (BeeUtils.same(name, SENDER)) {
        clickWidget.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            event.stopPropagation();
            Popup popup =
                new Popup(OutsideClick.CLOSE, BeeConst.CSS_CLASS_PREFIX + "mail-RecipientsPopup");

            HtmlTable ft = new HtmlTable();
            ft.setBorderSpacing(5);

            FlowPanel adr = new FlowPanel();
            adr.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Recipient");

            String email = sender.getA();
            String label = sender.getB();

            if (!BeeUtils.isEmpty(label)) {
              InlineLabel nm = new InlineLabel(label);
              nm.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-RecipientLabel");
              adr.add(nm);
            }
            InlineLabel nm = new InlineLabel(email);
            nm.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-RecipientEmail");
            adr.add(nm);

            ft.setWidget(0, 0, adr);
            popup.setWidget(ft);
            popup.setHideOnEscape(true);
            popup.showOnTop(widget.asWidget().getElement());
          }
        });
      }
    }
    if (widget instanceof Relations) {
      this.relations = (Relations) widget;
      relations.setSelectorHandler(new RelationsHandler());
    }
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.PRINT) {
      Printer.print(widgets.get(CONTAINER).getElement().getString(), null);
      return false;
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public FormInterceptor getInstance() {
    return new MailMessage();
  }

  public void reset() {
    for (String name : widgets.keySet()) {
      if (!BeeUtils.inList(name, WAITING, CONTAINER)) {
        setWidgetText(name, null);
      }
    }
    sender = Pair.of(null, null);
    draftId = null;
    rawId = null;
    recipients.clear();
    attachments.clear();

    if (relations != null) {
      relations.reset();
    }
  }

  @Override
  public void onSetActiveRow(IsRow row) {
    if (relations != null && !BeeUtils.isEmpty(relations.getParentId())) {
      relations.setParentId(null);
    }
    super.onSetActiveRow(row);
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    requery(COL_MESSAGE, row != null ? Data.getLong(form.getViewName(), row, COL_MESSAGE)
        : null, false);
    return super.onStartEdit(form, row, focusCommand);
  }

  void requery(String column, Long columnId, boolean showBcc) {
    reset();

    if (BeeUtils.isEmpty(column) || !DataUtils.isId(columnId)) {
      return;
    }
    setLoading(true);

    ParameterList params = MailKeeper.createArgs(SVC_GET_MESSAGE);
    params.addDataItem(column, columnId);
    params.addDataItem("showBcc", Codec.pack(showBcc));

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());

        if (response.hasErrors()) {
          return;
        }
        String[] data = Codec.beeDeserializeCollection((String) response.getResponse());
        Map<String, SimpleRowSet> packet = Maps.newHashMapWithExpectedSize(data.length / 2);

        for (int i = 0; i < data.length; i += 2) {
          packet.put(data[i], SimpleRowSet.restore(data[i + 1]));
        }
        SimpleRow row = packet.get(TBL_MESSAGES).getRow(0);

        if (relations != null) {
          relations.blockRelation(TBL_COMPANIES,
              !BeeKeeper.getUser().isAdministrator()
                  && (mailPanel == null || !mailPanel.getCurrentAccount().isPrivate()));

          relations.requery(row.getLong(COL_MESSAGE));
        }
        draftId = row.getLong(SystemFolder.Drafts.name());
        rawId = row.getLong(COL_RAW_CONTENT);
        String lbl = row.getValue(COL_EMAIL_LABEL);
        String mail = row.getValue(COL_EMAIL_ADDRESS);

        sender = Pair.of(mail, lbl);
        setWidgetText(SENDER, BeeUtils.notEmpty(lbl, mail));

        ((DateTimeLabel) widgets.get(DATE)).setValue(row.getDateTime(COL_DATE));
        setWidgetText(SUBJECT, row.getValue(COL_SUBJECT));

        String txt = null;

        for (SimpleRow address : packet.get(TBL_RECIPIENTS)) {
          String email = address.getValue(COL_EMAIL_ADDRESS);
          String label = address.getValue(COL_EMAIL_LABEL);

          recipients.put(address.getValue(COL_ADDRESS_TYPE), Pair.of(email, label));
          txt = BeeUtils.joinItems(txt, BeeUtils.notEmpty(label, email));
        }
        setWidgetText(RECIPIENTS, BeeUtils.joinWords(Localized.getConstants().mailTo() + ":", txt));

        int cnt = 0;
        long size = 0;
        txt = null;

        for (SimpleRow attachment : packet.get(TBL_ATTACHMENTS)) {
          Long fileSize = attachment.getLong(AdministrationConstants.COL_FILE_SIZE);

          attachments.add(new FileInfo(attachment.getLong(AdministrationConstants.COL_FILE),
              BeeUtils.notEmpty(attachment.getValue(COL_ATTACHMENT_NAME),
                  attachment.getValue(AdministrationConstants.COL_FILE_NAME)), fileSize,
              attachment.getValue(AdministrationConstants.COL_FILE_TYPE)));
          cnt++;
          size += BeeUtils.unbox(fileSize);
        }
        if (cnt > 0) {
          Widget widget = widgets.get(ATTACHMENTS);

          if (widget != null && widget instanceof HasWidgets) {
            ((HasWidgets) widget).clear();

            HtmlTable table = new HtmlTable();

            if (cnt > 1) {
              table.setText(0, 0, BeeUtils.toString(cnt));
              table.setWidget(0, 1, new FaLabel(FontAwesome.PAPERCLIP));
              table.setText(0, 2, BeeUtils.parenthesize(FileUtils.sizeToText(size)));
              table.addClickHandler(attachmentsHandler);
            } else {
              FileInfo file = attachments.get(0);
              table.setWidget(0, 0, new FaLabel(FontAwesome.PAPERCLIP));
              table.setWidget(0, 1, new Link(BeeUtils.joinWords(file.getName(),
                  BeeUtils.parenthesize(FileUtils.sizeToText(file.getSize()))),
                  FileUtils.getUrl(file.getName(), file.getId())));
            }
            ((HasWidgets) widget).add(table);
          }
        }
        String content = null;
        Element sep = Document.get().createHRElement();
        sep.setClassName(BeeConst.CSS_CLASS_PREFIX + "mail-PartSeparator");

        for (SimpleRow part : packet.get(TBL_PARTS)) {
          txt = part.getValue(COL_HTML_CONTENT);

          if (txt == null && part.getValue(COL_CONTENT) != null) {
            Element div = new CustomDiv().getElement();
            div.getStyle().setWhiteSpace(WhiteSpace.PRE_WRAP);
            div.setInnerHTML(part.getValue(COL_CONTENT));
            txt = div.getString();
          }
          content = BeeUtils.join(sep.getString(), content, txt);
        }
        widgets.get(PARTS).getElement().setInnerHTML(content);
        setLoading(false);
      }
    });
  }

  public void setLoading(boolean isLoading) {
    for (String name : new String[] {WAITING, CONTAINER}) {
      Widget widget = widgets.get(name);

      if (widget != null) {
        widget.setVisible(name.equals(WAITING) ? isLoading : !isLoading);
      }
    }
  }

  private Set<String> getBcc() {
    return getRecipients(AddressType.BCC.name());
  }

  private Set<String> getCc() {
    return getRecipients(AddressType.CC.name());
  }

  private String getContent() {
    Widget widget = widgets.get(PARTS);

    if (widget != null) {
      return widget.getElement().getInnerHTML();
    }
    return null;
  }

  private String getDate() {
    Widget widget = widgets.get(DATE);

    if (widget != null && widget instanceof DateTimeLabel) {
      return ((DateTimeLabel) widget).getValue().toString();
    }
    return null;
  }

  private String getRecipients() {
    StringBuilder sb = new StringBuilder();

    for (Pair<String, String> recipient : recipients.values()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(BeeUtils.joinWords(recipient.getB(), "<" + recipient.getA() + ">"));
    }
    return sb.toString();
  }

  private Set<String> getRecipients(String type) {
    Set<String> emails = new HashSet<>();

    if (recipients.containsKey(type)) {
      for (Pair<String, String> r : recipients.get(type)) {
        emails.add(r.getA());
      }
    }
    return emails;
  }

  private String getSender() {
    return BeeUtils.joinWords(sender.getB(), "<" + sender.getA() + ">");
  }

  private String getSubject() {
    return getWidgetText(SUBJECT);
  }

  private Set<String> getTo() {
    return getRecipients(AddressType.TO.name());
  }

  private String getWidgetText(String name) {
    Widget widget = widgets.get(name);

    if (widget != null) {
      return widget.getElement().getInnerText();
    }
    return null;
  }

  private void initCreateAction(HasClickHandlers widget, final NewMailMode mode) {
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent ev) {
        Set<String> to = null;
        Set<String> cc = null;
        Set<String> bcc = null;
        String subject = getSubject();
        String content = null;
        List<FileInfo> attach = null;
        Long draft = null;

        LocalizableConstants loc = Localized.getConstants();

        switch (mode) {
          case REPLY:
          case REPLY_ALL:
            if (!BeeUtils.isEmpty(sender.getA())) {
              to = Sets.newHashSet(sender.getA());
            }
            if (mode == NewMailMode.REPLY_ALL) {
              cc = getTo();
              cc.addAll(getCc());
            }
            Element bq = Document.get().createBlockQuoteElement();
            bq.setAttribute("style",
                "border-left:1px solid #039; margin:0; padding:10px; color:#039;");
            bq.setInnerHTML(getContent());
            content = BeeUtils.join("<br>", "<br>", getDate() + ", "
                + Codec.escapeHtml(getSender() + " " + loc.mailTextWrote().toLowerCase() + ":"),
                bq.getString());

            if (!BeeUtils.isPrefix(subject, loc.mailReplayPrefix())) {
              subject = BeeUtils.joinWords(loc.mailReplayPrefix(), subject);
            }
            break;

          case FORWARD:
            if (DataUtils.isId(draftId)) {
              draft = draftId;
              to = getTo();
              cc = getCc();
              bcc = getBcc();
              subject = getSubject();
              content = getContent();
            } else {
              content = BeeUtils.join("<br>", "<br>", "---------- "
                  + loc.mailForwardedMessage() + " ----------",
                  loc.mailFrom() + ": " + Codec.escapeHtml(getSender()),
                  loc.date() + ": " + getDate(),
                  loc.mailSubject() + ": " + Codec.escapeHtml(getSubject()),
                  loc.mailTo() + ": " + Codec.escapeHtml(getRecipients()),
                  "<br>" + getContent());

              if (!BeeUtils.isPrefix(subject, loc.mailForwardedPrefix())) {
                subject = BeeUtils.joinWords(loc.mailForwardedPrefix(), subject);
              }
            }
            attach = attachments;
            break;
        }
        if (mailPanel != null) {
          final AccountInfo account = mailPanel.getCurrentAccount();

          NewMailMessage newMessage = NewMailMessage.create(mailPanel.getAccounts(), account,
              to, cc, bcc, subject, content, attach, draft);

          newMessage.setScheduled(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean save) {
              if (BeeUtils.isFalse(save)) {
                mailPanel.checkFolder(account.getSystemFolder(SystemFolder.Sent));
              }
              mailPanel.checkFolder(account.getSystemFolder(SystemFolder.Drafts));
            }
          });
        } else {
          NewMailMessage.create(to, cc, bcc, subject, content, attach, draft);
        }
      }
    });
  }

  private void setWidgetText(String name, String text) {
    Widget widget = widgets.get(name);

    if (widget != null) {
      widget.getElement().setInnerText(text);
    }
  }
}
