package com.butent.bee.client.modules.mail;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DateTimeLabel;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.Link;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailConstants.AddressType;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MailMessage extends AbstractFormInterceptor {

  private static enum NewMailMode {
    REPLY, REPLY_ALL, FORWARD
  }

  private static final String WAITING = "Waiting";
  private static final String CONTAINER = "Container";
  private static final String RECIPIENTS = "Recipients";
  private static final String ATTACHMENTS = "Attachments";
  private static final String PARTS = "Parts";
  private static final String SENDER_LABEL = "SenderLabel";
  private static final String SENDER_EMAIL = "SenderEmail";
  private static final String DATE = "Date";
  private static final String SUBJECT = "Subject";

  private static final int ATTA_ID = 0;
  private static final int ATTA_NAME = 1;
  private static final int ATTA_SIZE = 2;

  private final MailPanel mailPanel;
  private Long draftId;
  private Pair<String, String> sender;
  private final Multimap<String, Pair<String, String>> recipients = HashMultimap.create();
  private final List<String[]> attachments = Lists.newArrayList();
  private final Map<String, Widget> widgets = Maps.newHashMap();

  public MailMessage(MailPanel mailPanel) {
    this.mailPanel = mailPanel;
    widgets.put(WAITING, null);
    widgets.put(CONTAINER, null);
    widgets.put(RECIPIENTS, null);
    widgets.put(ATTACHMENTS, null);
    widgets.put(PARTS, null);
    widgets.put(SENDER_LABEL, null);
    widgets.put(SENDER_EMAIL, null);
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
      }
      if (BeeUtils.same(name, RECIPIENTS)) {
        clickWidget.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            event.stopPropagation();
            final Popup popup = new Popup(OutsideClick.CLOSE, "bee-mail-RecipientsPopup");
            HtmlTable ft = new HtmlTable();
            ft.setBorderSpacing(5);
            LocalizableConstants loc = Localized.getConstants();

            for (Entry<AddressType, String> entry : ImmutableMap.of(AddressType.TO, loc.mailTo(),
                AddressType.CC, loc.mailCc(), AddressType.BCC, loc.mailBcc()).entrySet()) {

              String type = entry.getKey().name();

              if (recipients.containsKey(type)) {
                int c = ft.getRowCount();
                ft.getCellFormatter().setStyleName(c, 0, "bee-mail-RecipientsType");
                ft.setHtml(c, 0, entry.getValue() + ":");
                FlowPanel fp = new FlowPanel();

                for (Pair<String, String> address : recipients.get(type)) {
                  FlowPanel adr = new FlowPanel();
                  adr.setStyleName("bee-mail-Recipient");

                  String email = address.getA();
                  String label = address.getB();

                  InlineLabel nm = new InlineLabel(BeeUtils.notEmpty(label, email));
                  nm.setStyleName("bee-mail-RecipientLabel");
                  adr.add(nm);

                  if (!BeeUtils.isEmpty(label)) {
                    nm = new InlineLabel(email);
                    nm.setStyleName("bee-mail-RecipientEmail");
                    adr.add(nm);
                  }
                  fp.add(adr);
                }
                ft.setWidget(c, 1, fp);
              }
            }
            popup.setWidget(ft);
            popup.showRelativeTo(widget.asWidget().getElement());
          }
        });
      } else if (BeeUtils.same(name, ATTACHMENTS)) {
        clickWidget.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            event.stopPropagation();
            final Popup popup = new Popup(OutsideClick.CLOSE, "bee-mail-AttachmentsPopup");
            TabBar bar = new TabBar("bee-mail-AttachmentsMenu-", Orientation.VERTICAL);

            for (String[] item : attachments) {
              Link link = new Link(BeeUtils.joinWords(item[ATTA_NAME],
                  BeeUtils.parenthesize(FileUtils.sizeToText(BeeUtils.toLong(item[ATTA_SIZE])))),
                  FileUtils.getUrl(item[ATTA_NAME], BeeUtils.toLongOrNull(item[ATTA_ID])));

              link.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent ev) {
                  popup.close();
                }
              });
              bar.addItem(link);
            }
            popup.setWidget(bar);
            popup.showRelativeTo(widget.asWidget().getElement());
          }
        });
      }
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return null;
  }

  public void reset() {
    for (String name : widgets.keySet()) {
      if (!BeeUtils.inList(name, WAITING, CONTAINER)) {
        setWidgetText(name, null);
      }
    }
    sender = Pair.of(null, null);
    draftId = null;
    recipients.clear();
    attachments.clear();
  }

  void requery(Long placeId, boolean showBcc) {
    reset();

    if (!DataUtils.isId(placeId)) {
      return;
    }
    setLoading(true);

    ParameterList params = MailKeeper.createArgs(SVC_GET_MESSAGE);
    params.addDataItem(COL_PLACE, placeId);
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
        draftId = row.getLong(SystemFolder.Drafts.name());
        String lbl = row.getValue(COL_EMAIL_LABEL);
        String mail = row.getValue(ClassifierConstants.COL_EMAIL_ADDRESS);

        sender = Pair.of(mail, lbl);

        setWidgetText(SENDER_LABEL, BeeUtils.notEmpty(lbl, mail));
        setWidgetText(SENDER_EMAIL, BeeUtils.isEmpty(lbl) ? "" : mail);
        ((DateTimeLabel) widgets.get(DATE)).setValue(row.getDateTime(COL_DATE));
        setWidgetText(SUBJECT, row.getValue(COL_SUBJECT));

        String txt = null;

        for (SimpleRow address : packet.get(TBL_RECIPIENTS)) {
          String email = address.getValue(ClassifierConstants.COL_EMAIL_ADDRESS);
          String label = address.getValue(COL_EMAIL_LABEL);

          recipients.put(address.getValue(COL_ADDRESS_TYPE), Pair.of(email, label));
          txt = BeeUtils.joinItems(txt, BeeUtils.notEmpty(label, email));
        }
        setWidgetText(RECIPIENTS, BeeUtils.joinWords(Localized.getConstants().mailTo() + ":", txt));

        int cnt = 0;
        long size = 0;
        txt = null;

        for (SimpleRow attachment : packet.get(TBL_ATTACHMENTS)) {
          String[] info = new String[Ints.max(ATTA_ID, ATTA_NAME, ATTA_SIZE)];
          info[ATTA_ID] = attachment.getValue(AdministrationConstants.COL_FILE);
          info[ATTA_NAME] = BeeUtils.notEmpty(attachment.getValue(COL_ATTACHMENT_NAME),
              attachment.getValue(AdministrationConstants.COL_FILE_NAME));
          info[ATTA_SIZE] = attachment.getValue(AdministrationConstants.COL_FILE_SIZE);

          attachments.add(info);
          cnt++;
          size += BeeUtils.toLong(info[ATTA_SIZE]);
        }
        if (cnt > 0) {
          HtmlTable table = new HtmlTable();
          int c = 0;

          if (cnt > 1) {
            table.setText(0, c++, BeeUtils.toString(cnt));
          }
          table.setWidget(0, c++, new FaLabel(FontAwesome.PAPERCLIP));
          table.setText(0, c, BeeUtils.parenthesize(FileUtils.sizeToText(size)));

          Widget widget = widgets.get(ATTACHMENTS);

          if (widget != null) {
            widget.getElement().setInnerHTML(table.getElement().getString());
          }
        }
        String content = null;
        Element sep = Document.get().createHRElement();
        sep.setClassName("bee-mail-PartSeparator");

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

  private Map<Long, NewFileInfo> getAttachments() {
    Map<Long, NewFileInfo> attach = Maps.newLinkedHashMap();

    for (final String[] att : attachments) {
      attach.put(BeeUtils.toLong(att[ATTA_ID]),
          new NewFileInfo(att[ATTA_NAME], BeeUtils.toLong(att[ATTA_SIZE]), null));
    }
    return attach;
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
    Set<String> emails = Sets.newHashSet();

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
        Map<Long, NewFileInfo> attach = null;
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
              bcc = getBcc();
            }
            Element bq = Document.get().createBlockQuoteElement();
            bq.setAttribute("style",
                "border-left:1px solid #039; margin:0; padding:10px; color:#039;");
            bq.setInnerHTML(getContent());
            content = BeeUtils.join("<br>", "<br>", getDate() + ", "
                + SafeHtmlUtils.htmlEscape(getSender() + " "
                    + loc.mailTextWrote().toLowerCase() + ":"),
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
                  loc.mailFrom() + ": " + SafeHtmlUtils.htmlEscape(getSender()),
                  loc.date() + ": " + getDate(),
                  loc.mailSubject() + ": " + SafeHtmlUtils.htmlEscape(getSubject()),
                  loc.mailTo() + ": " + SafeHtmlUtils.htmlEscape(getRecipients()),
                  "<br>" + getContent());

              if (!BeeUtils.isPrefix(subject, loc.mailForwardedPrefix())) {
                subject = BeeUtils.joinWords(loc.mailForwardedPrefix(), subject);
              }
            }
            attach = getAttachments();
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

  private void setLoading(boolean isLoading) {
    for (String name : new String[] {WAITING, CONTAINER}) {
      Widget widget = widgets.get(name);

      if (widget != null) {
        widget.setVisible(name.equals(WAITING) ? isLoading : !isLoading);
      }
    }
  }

  private void setWidgetText(String name, String text) {
    Widget widget = widgets.get(name);

    if (widget != null) {
      widget.getElement().setInnerText(text);
    }
  }
}
