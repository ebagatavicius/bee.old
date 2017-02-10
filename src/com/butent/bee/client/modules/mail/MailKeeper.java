package com.butent.bee.client.modules.mail;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.NewsAggregator.HeadlineAccessor;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.ScreenImpl;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.interceptor.FileGridInterceptor;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailConstants.*;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

public final class MailKeeper {

  private static MailController controller;
  private static MailPanel activePanel;
  private static final Set<MailPanel> mailPanels = new HashSet<>();

  public static void getUnreadCount() {
    ParameterList params = createArgs(SVC_GET_UNREAD_COUNT);
    params.addQueryItem(COL_USER, BeeKeeper.getUser().getUserId());

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse()) {
          Integer count = response.getResponseAsInt();
          if (count != null) {
            ScreenImpl.updateOnlineEmails(count);
          }
        }
      }
    });
  }

  public static void refreshActivePanel(boolean refreshFolders, final Long folderId) {
    if (activePanel != null) {
      ScheduledCommand refreshMessages = null;

      if (DataUtils.isId(folderId)) {
        refreshMessages = () -> {
          if (activePanel != null && Objects.equals(activePanel.getCurrentFolder(), folderId)) {
            activePanel.refreshMessages(true);
          }
        };
      }
      if (refreshFolders) {
        activePanel.requeryFolders(refreshMessages);

      } else if (refreshMessages != null) {
        refreshMessages.execute();
      }
    }
  }

  public static void register() {
    MenuService.OPEN_MAIL.setHandler(parameters -> openMail(ViewHelper.getPresenterCallback()));

    ViewFactory.registerSupplier(FormFactory.getSupplierKey(FORM_MAIL),
        callback -> openMail(ViewFactory.getPresenterCallback(callback)));

    GridFactory.registerGridInterceptor(TBL_ACCOUNTS, new AccountsGrid());

    FormFactory.registerFormInterceptor(FORM_ACCOUNT, new AccountEditor());
    FormFactory.registerFormInterceptor(FORM_NEW_ACCOUNT, new AccountEditor());
    FormFactory.registerFormInterceptor(FORM_MAIL_MESSAGE, new MailMessage());
    FormFactory.registerFormInterceptor(FORM_RULE, new RuleForm());
    FormFactory.registerFormInterceptor(FORM_RECIPIENTS_GROUP, new RecipientsGroupForm());
    FormFactory.registerFormInterceptor(COL_NEWSLETTER, new NewsletterForm());

    GridFactory.registerGridInterceptor(VIEW_NEWSLETTER_FILES,
        new FileGridInterceptor(COL_NEWSLETTER, AdministrationConstants.COL_FILE,
            AdministrationConstants.COL_FILE_CAPTION, AdministrationConstants.ALS_FILE_NAME));

    Global.getNewsAggregator().registerFilterHandler(Feed.MAIL,
        (gridOptions, callback) -> {
        });

    Global.getNewsAggregator().registerAccessHandler(TBL_PLACES, new HeadlineAccessor() {
      @Override
      public void access(Long id) {
        ParameterList params = MailKeeper.createArgs(SVC_FLAG_MESSAGE);
        params.addDataItem(COL_PLACE, id);
        params.addDataItem(COL_FLAGS, MessageFlag.SEEN.name());
        params.addDataItem("on", Codec.pack(true));

        BeeKeeper.getRpc().makePostRequest(params, (ResponseCallback) null);
      }

      @Override
      public boolean read(Long id) {
        return false;
      }
    });
    BeeKeeper.getBus().registerRowActionHandler(event -> {
      if (event.hasView(ClassifierConstants.TBL_EMAILS) && event.isEditRow()) {
        event.consume();
        NewMailMessage.create(Data.getString(ClassifierConstants.TBL_EMAILS, event.getRow(),
            ClassifierConstants.COL_EMAIL_ADDRESS), null, null, null, null);
      }
    });
  }

  static void activateController(MailPanel mailPanel) {
    if (controller == null) {
      controller = new MailController();
      BeeKeeper.getScreen().addDomainEntry(Domain.MAIL, controller, null,
          Localized.dictionary().mails());
    }
    activePanel = mailPanel;
    rebuildController();
    BeeKeeper.getScreen().activateDomainEntry(Domain.MAIL, null);
  }

  static void activateMailPanel() {
    BeeKeeper.getScreen()
        .activateWidget(activePanel.getFormView().getViewPresenter().getMainView());
  }

  static void clickFolder(Long folderId) {
    clickFolder(folderId, false);
  }

  static void clickFolder(Long folderId, boolean syncAll) {
    if (DataUtils.isId(folderId) && Objects.equals(folderId, activePanel.getCurrentFolder())) {
      activePanel.checkFolder(folderId, syncAll);
    } else {
      activePanel.refreshFolder(folderId);
    }
  }

  static void copyMessage(String places, final Long folderTo, final boolean move) {
    final MailPanel panel = activePanel;
    ParameterList params = createArgs(SVC_COPY_MESSAGES);
    params.addDataItem(COL_ACCOUNT, panel.getCurrentAccount().getAccountId());
    params.addDataItem(COL_FOLDER, folderTo);
    params.addDataItem(COL_PLACE, places);
    params.addDataItem("move", move ? 1 : 0);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(panel.getFormView());

        if (!response.hasErrors()) {
          Dictionary loc = Localized.dictionary();

          panel.getFormView().notifyInfo(move
                  ? loc.mailMovedMessagesToFolder(response.getResponseAsString())
                  : loc.mailCopiedMessagesToFolder(response.getResponseAsString()),
              BeeUtils.bracket(panel.getCurrentAccount().findFolder(folderTo).getName()));
        }
      }
    });
    if (move) {
      panel.removeRows(DataUtils.parseIdList(places));
    }
  }

  static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.MAIL, method);
  }

  static void createFolder(String title) {
    final MailPanel panel = activePanel;
    final AccountInfo account = panel.getCurrentAccount();
    final Long parentId = panel.getCurrentFolder();
    final boolean isParent = DataUtils.isId(parentId) && !account.isSystemFolder(parentId);
    String caption = null;

    if (isParent) {
      caption = Localized.dictionary().mailInFolder() + " "
          + BeeUtils.bracket(account.findFolder(parentId).getName());
    }
    Global.inputString(title, caption, new StringCallback() {
      @Override
      public void onSuccess(String value) {
        ParameterList params = createArgs(SVC_CREATE_FOLDER);
        params.addDataItem(COL_ACCOUNT, account.getAccountId());
        params.addDataItem(COL_FOLDER_NAME, value);

        if (isParent) {
          params.addDataItem(COL_FOLDER, parentId);
        }
        BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            response.notify(panel.getFormView());

            if (!response.hasErrors()) {
              panel.requeryFolders();
            }
          }
        });
      }
    }, null);
  }

  static void disconnectFolder(final AccountInfo account, final Long folderId) {
    final MailPanel panel = activePanel;
    ParameterList params = createArgs(SVC_DISCONNECT_FOLDER);
    params.addDataItem(COL_ACCOUNT, account.getAccountId());
    params.addDataItem(COL_FOLDER, folderId);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(panel.getFormView());

        if (!response.hasErrors()) {
          panel.requeryFolders();
        }
      }
    });
  }

  static void getAccounts(final BiConsumer<List<AccountInfo>, AccountInfo> consumer) {
    ParameterList params = createArgs(SVC_GET_ACCOUNTS);
    params.addDataItem(COL_USER, BeeKeeper.getUser().getUserId());

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (response.hasErrors()) {
          return;
        }
        SimpleRowSet rs = SimpleRowSet.restore(response.getResponseAsString());
        List<AccountInfo> availableAccounts = new ArrayList<>();
        AccountInfo defaultAccount = null;

        for (SimpleRow row : rs) {
          AccountInfo account = new AccountInfo(row);

          if (defaultAccount == null || BeeUtils.unbox(row.getBoolean(COL_ACCOUNT_DEFAULT))) {
            defaultAccount = account;
          }
          availableAccounts.add(account);
        }
        consumer.accept(availableAccounts, defaultAccount);
      }
    });
  }

  static void rebuildController() {
    controller.rebuild(activePanel.getCurrentAccount());
    refreshController();
  }

  static void refreshController() {
    controller.refresh(activePanel.getCurrentFolder());
  }

  static void removeFolder(final AccountInfo account, final Long folderId) {
    final MailPanel panel = activePanel;
    ParameterList params = createArgs(SVC_DROP_FOLDER);
    params.addDataItem(COL_ACCOUNT, account.getAccountId());
    params.addDataItem(COL_FOLDER, folderId);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(panel.getFormView());

        if (!response.hasErrors()) {
          panel.requeryFolders(() -> {
            if (Objects.equals(folderId, panel.getCurrentFolder())) {
              panel.refreshFolder(account.getInboxId());
            }
          });
        }
      }
    });
  }

  static void removeMailPanel(MailPanel mailPanel) {
    mailPanels.remove(mailPanel);

    if (mailPanels.size() > 0) {
      if (mailPanel == activePanel) {
        activePanel = mailPanels.iterator().next();
        rebuildController();
      }
    } else {
      activePanel = null;
      controller = null;
      BeeKeeper.getScreen().removeDomainEntry(Domain.MAIL, null);
    }
  }

  static void removeMailPanels() {
    for (MailPanel mailPanel : mailPanels) {
      BeeKeeper.getScreen().closeWidget(mailPanel.getFormView().getViewPresenter().getMainView());
    }
    mailPanels.clear();
    activePanel = null;
    controller = null;
  }

  static void renameFolder(AccountInfo account, final Long folderId, String name) {
    final MailPanel panel = activePanel;
    ParameterList params = createArgs(SVC_RENAME_FOLDER);
    params.addDataItem(COL_ACCOUNT, account.getAccountId());
    params.addDataItem(COL_FOLDER, folderId);
    params.addDataItem(COL_FOLDER_NAME, name);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(panel.getFormView());

        if (!response.hasErrors()) {
          panel.requeryFolders(() -> panel.checkFolder(folderId, false));
        }
      }
    });
  }

  private static void openMail(final PresenterCallback callback) {
    getAccounts((availableAccounts, defaultAccount) -> {
      if (!BeeUtils.isEmpty(availableAccounts)) {
        final MailPanel mailPanel = new MailPanel(availableAccounts, defaultAccount);
        mailPanels.add(mailPanel);

        FormFactory.getFormDescription(FORM_MAIL, result ->
            FormFactory.openForm(result, mailPanel, callback));

      } else {
        BeeKeeper.getScreen().notifyWarning("No accounts found");
      }
    });
  }

  static void syncFolders(boolean syncAll) {
    final ParameterList params = MailKeeper.createArgs(SVC_CHECK_MAIL);
    params.addDataItem(COL_ACCOUNT, activePanel.getCurrentAccount().getAccountId());

    if (syncAll) {
      params.addDataItem(Service.VAR_CHECK, BeeUtils.toString(syncAll));
    }
    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(activePanel.getFormView());
      }
    });
  }

  private MailKeeper() {
  }
}
