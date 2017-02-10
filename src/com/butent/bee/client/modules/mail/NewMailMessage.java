package com.butent.bee.client.modules.mail;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.InputBoxes;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class NewMailMessage extends AbstractFormInterceptor
    implements ClickHandler, SelectionHandler<FileInfo> {

  private class DialogCallback implements InputCallback {
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
        Global.decide(null, Lists.newArrayList(Localized.dictionary().mailMessageWasNotSent(),
            Localized.dictionary().mailQuestionSaveToDraft()), new DecisionCallback() {
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

  public static void create(String to, String subject, String content,
      Collection<FileInfo> attachments, BiConsumer<Long, Boolean> callback) {

    create(Collections.singleton(to), null, null, subject, content, attachments, null, false,
        callback);
  }

  public static void create(Set<String> to, Set<String> cc, Set<String> bcc, String subject,
      String content, Collection<FileInfo> attachments, Long relatedId, boolean isDraft,
      BiConsumer<Long, Boolean> callback) {

    MailKeeper.getAccounts((availableAccounts, defaultAccount) -> {
      if (BeeUtils.isEmpty(availableAccounts)) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().mailNoAccountsFound());
      } else {
        create(availableAccounts, defaultAccount, to, cc, bcc, subject, content, attachments,
            relatedId, isDraft).setCallback(callback);
      }
    });
  }

  public static NewMailMessage create(List<AccountInfo> availableAccounts,
      AccountInfo defaultAccount, Set<String> to, Set<String> cc, Set<String> bcc, String subject,
      String content, Collection<FileInfo> attachments, Long relatedId, boolean isDraft) {

    NewMailMessage newMessage = new NewMailMessage(availableAccounts, defaultAccount,
        to, cc, bcc, subject, content, attachments, relatedId, isDraft);

    FormFactory.createFormView(FORM_NEW_MAIL_MESSAGE, null, null, false, newMessage,
        (formDescription, formView) -> {
          if (formView != null) {
            formView.start(null);
            boolean modal = Popup.hasEventPreview();

            DialogBox dialog = Global.inputWidget(formView.getCaption(), formView,
                newMessage.new DialogCallback(), RowFactory.DIALOG_STYLE);

            if (!modal) {
              dialog.setPreviewEnabled(false);
            }
            newMessage.initHeader(dialog);
            dialog.focusOnOpen(newMessage.getFocusWidget());
          }
        });
    newMessage.isSignatureAbove =
        BeeUtils.unbox(Global.getParameterBoolean(PRM_SIGNATURE_POSITION));

    return newMessage;
  }

  private AccountInfo account;
  private final List<AccountInfo> accounts = new ArrayList<>();
  private final Map<Long, String> signatures = new HashMap<>();
  private final Long relatedId;
  private final boolean isDraft;
  private final Div signature = new Div().id(BeeUtils.randomString(5));
  private boolean isSignatureAbove;

  private final Multimap<AddressType, String> recipients = HashMultimap.create();
  private final String subject;
  private final String content;
  private final Collection<FileInfo> defaultAttachments;

  private final Map<AddressType, MultiSelector> recipientWidgets = new HashMap<>();
  private Editor subjectWidget;
  private Editor contentWidget;
  private final ListBox signaturesWidget = new ListBox();
  private FileCollector attachmentsWidget;

  private BiConsumer<Long, Boolean> actionCallback;

  private NewMailMessage(List<AccountInfo> availableAccounts, AccountInfo defaultAccount,
      Set<String> to, Set<String> cc, Set<String> bcc, String subject, String content,
      Collection<FileInfo> attachments, Long relatedId, boolean isDraft) {

    this.account = Assert.notNull(defaultAccount);
    this.accounts.addAll(availableAccounts);
    this.relatedId = relatedId;
    this.isDraft = isDraft;

    this.subject = subject;
    this.content = content;

    this.defaultAttachments = attachments;

    Map<AddressType, Set<String>> recs = new HashMap<>();
    recs.put(AddressType.TO, to);
    recs.put(AddressType.CC, cc);
    recs.put(AddressType.BCC, bcc);

    for (AddressType type : recs.keySet()) {
      Collection<String> emails = recs.get(type);

      if (!BeeUtils.isEmpty(emails)) {
        recipients.putAll(type,
            emails.stream().filter(s -> !BeeUtils.isEmpty(s)).collect(Collectors.toSet()));

        if (!isDraft) {
          recipients.remove(type, defaultAccount.getAddress());
        }
      }
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    AddressType type = EnumUtils.getEnumByName(AddressType.class, name);

    if (widget instanceof MultiSelector && type != null) {
      MultiSelector input = (MultiSelector) widget;

      Collection<String> lst = recipients.get(type);
      if (!BeeUtils.isEmpty(lst)) {
        input.setValues(lst);
      }
      recipientWidgets.put(type, input);

    } else if (widget instanceof Editor && BeeUtils.same(name, COL_SUBJECT)) {
      subjectWidget = (Editor) widget;
      subjectWidget.setValue(subject);

    } else if (widget instanceof Editor && BeeUtils.same(name, COL_MESSAGE)) {
      contentWidget = (Editor) widget;
      contentWidget.setValue(content);

    } else if (widget instanceof FileCollector && BeeUtils.same(name, TBL_ATTACHMENTS)) {
      attachmentsWidget = (FileCollector) widget;
      attachmentsWidget.addFiles(defaultAttachments);
      attachmentsWidget.bindDnd(getFormView());
      attachmentsWidget.addSelectionHandler(this);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    Assert.unsupported();
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
  public void onSelection(SelectionEvent<FileInfo> event) {
    final FileInfo file = event.getSelectedItem();

    if (attachmentsWidget.contains(file)) {
      FileUtils.uploadFile(file, new Callback<Long>() {
        @Override
        public void onFailure(String... reason) {
          attachmentsWidget.removeFile(file);
          Callback.super.onFailure(reason);
        }

        @Override
        public void onSuccess(Long id) {
          file.setFileId(id);
          attachmentsWidget.refreshFile(file);
        }
      });
    }
  }

  public void setCallback(BiConsumer<Long, Boolean> callback) {
    this.actionCallback = callback;
  }

  private void applySignature(Long signatureId) {
    String value = null;
    String currentContent = contentWidget.getValue();
    String oldSignature = signature.toString();
    signature.clearChildren();

    if (DataUtils.isId(signatureId)) {
      value = BeeUtils.toString(signatureId);
      signature.text(signatures.get(signatureId));
    }
    if (!isDraft || DataUtils.isId(signatureId)) {
      String newSignature = signature.toString();

      if (currentContent.contains(oldSignature)) {
        currentContent = currentContent.replace(oldSignature, newSignature);
      } else {
        if (isSignatureAbove) {
          currentContent = SIGNATURE_SEPARATOR + newSignature + currentContent;
        } else {
          currentContent = currentContent + SIGNATURE_SEPARATOR + newSignature;
        }
      }
    }
    contentWidget.setValue(currentContent);
    signaturesWidget.setValue(value);
  }

  private Widget getFocusWidget() {
    Editor w = null;

    if (BeeUtils.isEmpty(recipients.get(AddressType.TO))) {
      w = recipientWidgets.get(AddressType.TO);
    }
    if (Objects.isNull(w) && BeeUtils.isEmpty(subject)) {
      w = subjectWidget;
    }
    if (Objects.isNull(w)) {
      w = contentWidget;
    }
    return Objects.isNull(w) ? null : w.asWidget();
  }

  private boolean hasChanges() {
    for (AddressType type : recipientWidgets.keySet()) {
      Set<String> values = new HashSet<>(recipientWidgets.get(type).getValues());
      if (!BeeUtils.sameElements(recipients.get(type), values)) {
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
    return !BeeUtils.sameElements(defaultAttachments, attachmentsWidget.getFiles());
  }

  private void initHeader(DialogBox dialog) {
    FaLabel send = new FaLabel(FontAwesome.PAPER_PLANE);
    send.setTitle(Localized.dictionary().send());
    send.enableAnimation();
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
            signaturesWidget.setEnabled(signaturesWidget.getItemCount() > 0);
            signaturesWidget.addChangeHandler(
                event -> applySignature(BeeUtils.toLongOrNull(signaturesWidget.getValue())));

            applySignature(isDraft ? null : account.getSignatureId());
          }
        });
    dialog.insertAction(1, signaturesWidget);
    dialog.getHeader().insert(new Label(Localized.dictionary().mailSignature() + ":"), 1);

    final ListBox accountsWidget = new ListBox();

    for (int i = 0; i < accounts.size(); i++) {
      AccountInfo accountInfo = accounts.get(i);
      accountsWidget.addItem(accountInfo.getDescription() + " <" + accountInfo.getAddress() + ">");

      if (Objects.equals(accountInfo, account)) {
        accountsWidget.setSelectedIndex(i);
      }
    }
    accountsWidget.setEnabled(accountsWidget.getItemCount() > 1);
    accountsWidget.addChangeHandler(event -> {
      AccountInfo selectedAccount = accounts.get(accountsWidget.getSelectedIndex());

      if (!Objects.equals(selectedAccount, account)) {
        account = selectedAccount;
        applySignature(account.getSignatureId());
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

    for (AddressType type : recipientWidgets.keySet()) {
      Set<String> values = new HashSet<>(recipientWidgets.get(type).getValues());
      if (!BeeUtils.isEmpty(values)) {
        params.addDataItem(type.name(), Codec.beeSerialize(values));
      }
    }

    if (relatedId != null) {
      params.addDataItem(AdministrationConstants.COL_RELATION, relatedId);
    }
    params.addDataItem("Save", saveMode ? 1 : 0);
    params.addDataItem(COL_SUBJECT, subjectWidget.getValue());
    params.addDataItem(COL_CONTENT, contentWidget.getValue());

    Map<Long, String> attachments = new LinkedHashMap<>();

    for (FileInfo file : attachmentsWidget.getFiles()) {
      attachments.put(file.getId(), BeeUtils.notEmpty(file.getCaption(), file.getName()));
    }
    if (!BeeUtils.isEmpty(attachments)) {
      params.addDataItem(TBL_ATTACHMENTS, Codec.beeSerialize(attachments));
    }
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (actionCallback != null && !response.hasErrors()) {
          actionCallback.accept(response.getResponseAsLong(), saveMode);
        }
      }
    });
  }

  private boolean validate() {
    boolean hasRecipients = false;
    String error = null;

    for (MultiSelector r : recipientWidgets.values()) {
      if (!BeeUtils.isEmpty(r.getValues())) {
        hasRecipients = true;
        break;
      }
    }
    if (!hasRecipients) {
      error = Localized.dictionary().mailSpecifyRecipient();

    } else if (subjectWidget == null || BeeUtils.isEmpty(subjectWidget.getValue())) {
      error = Localized.dictionary().mailSpecifySubject();

    } else if (contentWidget == null || BeeUtils.isEmpty(contentWidget.getValue())) {
      error = Localized.dictionary().mailMessageBodyIsEmpty();

    } else {
      for (FileInfo file : attachmentsWidget.getFiles()) {
        if (!DataUtils.isId(file.getId())) {
          error = Localized.dictionary().mailThereIsStackOfUnfinishedAttachments();
          break;
        }
      }
    }
    if (!BeeUtils.isEmpty(error)) {
      getFormView().notifySevere(error);
    }
    return BeeUtils.isEmpty(error);
  }
}
