package com.butent.bee.client.modules.mail;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.HasWidgets;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.Autocomplete;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.InputBoxes;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormFactory.FormViewCallback;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailConstants.AddressType;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class NewMailMessage extends AbstractFormInterceptor
    implements ClickHandler, SelectionHandler<NewFileInfo> {

  private class DialogCallback extends InputCallback {
    @Override
    public String getErrorMessage() {
      if (validate()) {
        return null;
      } else {
        return InputBoxes.SILENT_ERROR;
      }
    }

    @Override
    public void onClose(final CloseCallback closeCallback) {
      Assert.notNull(closeCallback);

      if (hasChanges()) {
        Global.decide(null, Lists.newArrayList(Localized.getConstants().mailMessageWasNotSent(),
            Localized.getConstants().mailQuestionSaveToDraft()), new DecisionCallback() {
          @Override
          public void onCancel() {
            UiHelper.focus(getFormView().asWidget());
          }

          @Override
          public void onConfirm() {
            closeCallback.onSave();
          }

          @Override
          public void onDeny() {
            closeCallback.onClose();
          }
        }, DialogConstants.DECISION_YES);
      } else {
        closeCallback.onClose();
      }
    }

    @Override
    public void onSuccess() {
      save(true);
    }
  }

  private static final String SIGNATURE_SEPARATOR = "<br><br><br>";

  public static void create(final Set<String> to, final Set<String> cc, final Set<String> bcc,
      final String subject, final String content, final Map<Long, NewFileInfo> attach,
      final Long draftId) {

    MailKeeper.getAccounts(new BiConsumer<List<AccountInfo>, AccountInfo>() {
      @Override
      public void accept(List<AccountInfo> availableAccounts, AccountInfo defaultAccount) {
        if (!BeeUtils.isEmpty(availableAccounts)) {
          create(availableAccounts, defaultAccount, to, cc, bcc, subject, content, attach, draftId);
        } else {
          BeeKeeper.getScreen().notifyWarning("No accounts found");
        }
      }
    });
  }

  public static NewMailMessage create(List<AccountInfo> availableAccounts,
      AccountInfo defaultAccount, Set<String> to, Set<String> cc, Set<String> bcc, String subject,
      String content, Map<Long, NewFileInfo> attach, Long draftId) {

    final NewMailMessage newMessage = new NewMailMessage(availableAccounts, defaultAccount,
        to, cc, bcc, subject, content, attach, draftId);

    FormFactory.createFormView(FORM_NEW_MAIL_MESSAGE, null, null, false, newMessage,
        new FormViewCallback() {
          @Override
          public void onSuccess(FormDescription formDescription, FormView formView) {
            if (formView != null) {
              formView.start(null);

              DialogBox dialog = Global.inputWidget(formView.getCaption(), formView,
                  newMessage.new DialogCallback(), RowFactory.DIALOG_STYLE);

              newMessage.initHeader(dialog);
            }
          }
        });
    return newMessage;
  }

  private AccountInfo account;
  private final List<AccountInfo> accounts = new ArrayList<>();
  private final Map<Long, String> signatures = new HashMap<>();
  private final Long draftId;
  private final Div signature = new Div().id(BeeUtils.randomString(5));

  private final Multimap<String, String> recipients = HashMultimap.create();
  private final String subject;
  private final String content;
  private final Map<Long, NewFileInfo> defaultAttachments;

  private final Map<String, Autocomplete> recipientWidgets = Maps.newHashMap();
  private Editor subjectWidget;
  private Editor contentWidget;
  private final ListBox signaturesWidget = new ListBox();
  private final Map<String, Long> attachments = Maps.newLinkedHashMap();

  private Consumer<Boolean> scheduled;

  private NewMailMessage(List<AccountInfo> availableAccounts, AccountInfo defaultAccount,
      Set<String> to, Set<String> cc, Set<String> bcc, String subject, String content,
      Map<Long, NewFileInfo> attach, Long draftId) {

    Assert.notNull(defaultAccount);

    this.account = defaultAccount;
    this.accounts.addAll(availableAccounts);
    this.draftId = draftId;

    this.subject = subject;
    this.content = content;

    if (!BeeUtils.isEmpty(attach)) {
      this.defaultAttachments = attach;
    } else {
      this.defaultAttachments = Maps.newHashMap();
    }
    for (Long id : defaultAttachments.keySet()) {
      attachments.put(defaultAttachments.get(id).getName(), id);
    }
    if (!DataUtils.isId(draftId)) {
      Long addressId = defaultAccount.getAddressId();

      if (to != null) {
        to.remove(addressId);
      }
      if (cc != null) {
        cc.remove(addressId);
      }
      if (bcc != null) {
        bcc.remove(addressId);
      }
    }
    Map<AddressType, Set<String>> recs = new HashMap<>();
    recs.put(AddressType.TO, to);
    recs.put(AddressType.CC, cc);
    recs.put(AddressType.BCC, bcc);

    for (AddressType type : recs.keySet()) {
      Set<String> emails = recs.get(type);

      if (!BeeUtils.isEmpty(emails)) {
        recipients.putAll(type.name(), emails);
      }
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    AddressType type = EnumUtils.getEnumByName(AddressType.class, name);

    if (widget instanceof HasWidgets && type != null) {
      HasWidgets panel = (HasWidgets) widget;
      panel.clear();

      Autocomplete input = Autocomplete.create(Relation.create("UserEmails",
          Lists.newArrayList("Email")), true);

      panel.add(input);

      Collection<String> lst = recipients.get(type.name());

      if (!BeeUtils.isEmpty(lst)) {
        input.setValue(lst.iterator().next());
      }
      recipientWidgets.put(type.name(), input);

    } else if (widget instanceof Editor && BeeUtils.same(name, COL_SUBJECT)) {
      subjectWidget = (Editor) widget;
      subjectWidget.setValue(subject);

    } else if (widget instanceof Editor && BeeUtils.same(name, COL_MESSAGE)) {
      contentWidget = (Editor) widget;
      contentWidget.setValue(content);

    } else if (widget instanceof FileCollector && BeeUtils.same(name, TBL_ATTACHMENTS)) {
      if (!BeeUtils.isEmpty(defaultAttachments)) {
        ((FileCollector) widget).addFiles(defaultAttachments.values());
      }
      ((FileCollector) widget).bindDnd(getFormView());
      ((FileCollector) widget).addSelectionHandler(this);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return null;
  }

  @Override
  public void onClick(ClickEvent event) {
    if (validate()) {
      Popup dialog = UiHelper.getParentPopup(getFormView().asWidget());
      if (dialog != null) {
        dialog.close();
      }
      save(false);
    }
  }

  @Override
  public void onSelection(SelectionEvent<NewFileInfo> event) {
    final String fileName = event.getSelectedItem().getName();

    if (attachments.containsKey(fileName)) {
      attachments.remove(fileName);
    } else {
      attachments.put(fileName, 0L);

      FileUtils.uploadFile(event.getSelectedItem(), new Callback<Long>() {
        @Override
        public void onFailure(String... reason) {
          if (attachments.containsKey(fileName)) {
            attachments.put(fileName, -1L);
            super.onFailure(reason);
          }
        }

        @Override
        public void onSuccess(Long id) {
          if (attachments.containsKey(fileName)) {
            attachments.put(fileName, id);
          }
        }
      });
    }
  }

  public void setScheduled(Consumer<Boolean> scheduled) {
    this.scheduled = scheduled;
  }

  private void applySignature(Long signatureId) {
    if (DataUtils.isId(signatureId)) {
      signaturesWidget.setValue(BeeUtils.toString(signatureId));

      String oldSignature = signature.toString();
      signature.clearChildren();
      String newSignature = signature.text(signatures.get(signatureId)).toString();

      String currentContent = contentWidget.getValue();

      if (currentContent.contains(oldSignature)) {
        currentContent = currentContent.replace(oldSignature, newSignature);
      } else {
        currentContent = SIGNATURE_SEPARATOR + newSignature + currentContent;
      }
      contentWidget.setValue(currentContent);

    } else {
      signaturesWidget.setValue(null);
    }
  }

  private boolean hasChanges() {
    for (String type : recipientWidgets.keySet()) {
      Set<String> recs = new HashSet<>();
      String recipient = recipientWidgets.get(type).getValue();

      if (!BeeUtils.isEmpty(recipient)) {
        recs.add(recipient);
      }
      if (!BeeUtils.sameElements(recipients.get(type), recs)) {
        return true;
      }
    }
    if (subjectWidget == null || !BeeUtils.same(subject, subjectWidget.getValue())) {
      return true;
    }
    if (contentWidget == null || !BeeUtils.same(content,
        contentWidget.getValue().replace(SIGNATURE_SEPARATOR + signature.toString(), ""))) {
      return true;
    }
    if (!BeeUtils.same(DataUtils.buildIdList(defaultAttachments.keySet()),
        DataUtils.buildIdList(attachments.values()))) {
      return true;
    }
    return false;
  }

  private void initHeader(DialogBox dialog) {
    FaLabel send = new FaLabel(FontAwesome.PAPER_PLANE);
    send.addClickHandler(this);

    dialog.insertAction(1, send);

    Queries.getRowSet(TBL_SIGNATURES, null,
        Filter.equals(COL_USER, BeeKeeper.getUser().getUserId()), new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            for (int i = 0; i < result.getNumberOfRows(); i++) {
              Long signatureId = result.getRow(i).getId();

              signatures.put(signatureId, result.getString(i, COL_SIGNATURE_CONTENT));
              signaturesWidget.addItem(result.getString(i, COL_SIGNATURE_NAME),
                  BeeUtils.toString(signatureId));
            }
            signaturesWidget.setEnabled(signaturesWidget.getItemCount() > 1);
            signaturesWidget.addChangeHandler(new ChangeHandler() {
              @Override
              public void onChange(ChangeEvent event) {
                applySignature(BeeUtils.toLongOrNull(signaturesWidget.getValue()));
              }
            });
            if (!DataUtils.isId(draftId)) {
              applySignature(account.getSignatureId());
            }
          }
        });
    dialog.insertAction(1, signaturesWidget);
    dialog.getHeader().insert(new Label(Localized.getConstants().mailSignature() + ":"), 1);

    final ListBox accountsWidget = new ListBox();

    for (int i = 0; i < accounts.size(); i++) {
      AccountInfo accountInfo = accounts.get(i);
      accountsWidget.addItem(accountInfo.getDescription() + " <" + accountInfo.getAddress() + ">");

      if (Objects.equals(accountInfo, account)) {
        accountsWidget.setSelectedIndex(i);
      }
    }
    accountsWidget.setEnabled(accountsWidget.getItemCount() > 1);
    accountsWidget.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        AccountInfo selectedAccount = accounts.get(accountsWidget.getSelectedIndex());

        if (!Objects.equals(selectedAccount, account)) {
          account = selectedAccount;
          applySignature(account.getSignatureId());
        }
      }
    });
    dialog.insertAction(1, accountsWidget);
  }

  private void save(final boolean saveMode) {
    if (saveMode && !hasChanges()) {
      return;
    }
    ParameterList params = MailKeeper.createArgs(SVC_SEND_MAIL);
    params.addDataItem(COL_ACCOUNT, account.getAccountId());

    for (String type : recipientWidgets.keySet()) {
      String recipient = recipientWidgets.get(type).getValue();

      if (!BeeUtils.isEmpty(recipient)) {
        params.addDataItem(type, Codec.beeSerialize(Sets.newHashSet(recipient)));
      }
    }
    if (draftId != null) {
      params.addDataItem("DraftId", draftId);
    }
    params.addDataItem("Save", saveMode ? 1 : 0);
    params.addDataItem(COL_SUBJECT, subjectWidget.getValue());
    params.addDataItem(COL_CONTENT, contentWidget.getValue());

    String ids = DataUtils.buildIdList(attachments.values());

    if (!BeeUtils.isEmpty(ids)) {
      params.addDataItem(TBL_ATTACHMENTS, ids);
    }
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (scheduled != null) {
          scheduled.accept(saveMode);
        }
      }
    });
  }

  private boolean validate() {
    boolean hasRecipients = false;
    String error = null;

    for (Autocomplete r : recipientWidgets.values()) {
      if (!BeeUtils.isEmpty(r.getValue())) {
        hasRecipients = true;
        break;
      }
    }
    if (!hasRecipients) {
      error = Localized.getConstants().mailSpecifyRecipient();

    } else if (subjectWidget == null || BeeUtils.isEmpty(subjectWidget.getValue())) {
      error = Localized.getConstants().mailSpecifySubject();

    } else if (contentWidget == null || BeeUtils.isEmpty(contentWidget.getValue())) {
      error = Localized.getConstants().mailMessageBodyIsEmpty();

    } else if (attachments.values().contains(0L)) {
      error = Localized.getConstants().mailThereIsStackOfUnfinishedAttachments();
    }
    if (!BeeUtils.isEmpty(error)) {
      getFormView().notifySevere(error);
    }
    return BeeUtils.isEmpty(error);
  }
}
