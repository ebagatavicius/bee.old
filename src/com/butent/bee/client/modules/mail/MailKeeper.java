package com.butent.bee.client.modules.mail;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.NewsAggregator.HeadlineAccessor;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.ViewSupplier;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailConstants.MessageFlag;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MailKeeper {

  private static MailController controller;
  private static MailPanel activePanel;
  private static final Set<MailPanel> mailPanels = Sets.newHashSet();

  public static void refreshActivePanel(boolean refreshFolders, final Long folderId) {
    if (activePanel != null) {
      ScheduledCommand refreshMessages = null;

      if (DataUtils.isId(folderId)) {
        refreshMessages = new ScheduledCommand() {
          @Override
          public void execute() {
            if (activePanel != null && Objects.equals(activePanel.getCurrentFolderId(), folderId)) {
              activePanel.refreshMessages();
            }
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
    MenuService.RESTART_PROXY.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        BeeKeeper.getRpc().makeGetRequest(createArgs(SVC_RESTART_PROXY));
      }
    });

    MenuService.OPEN_MAIL.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        openMail(ViewHelper.getPresenterCallback());
      }
    });

    ViewFactory.registerSupplier(FormFactory.getSupplierKey(FORM_MAIL), new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        openMail(ViewFactory.getPresenterCallback(callback));
      }
    });

    FormFactory.registerFormInterceptor(FORM_ACCOUNT, new AccountEditor());
    FormFactory.registerFormInterceptor(FORM_NEW_ACCOUNT, new AccountEditor());
    FormFactory.registerFormInterceptor(FORM_MAIL_MESSAGE, new MailMessage());
    FormFactory.registerFormInterceptor(FORM_RULE, new RuleForm());

    Global.getNewsAggregator().registerFilterHandler(Feed.MAIL,
        new BiConsumer<GridFactory.GridOptions, PresenterCallback>() {
          @Override
          public void accept(GridOptions gridOptions, PresenterCallback callback) {
          }
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
      public boolean read(final Long id) {
        FormFactory.openForm(FORM_MAIL_MESSAGE, new MailMessage() {
          @Override
          public void onLoad(FormView form) {
            requery(COL_PLACE, id, false);
            super.onLoad(form);
          }
        });
        return true;
      }
    });
  }

  static void activateController(MailPanel mailPanel) {
    if (controller == null) {
      controller = new MailController();
      BeeKeeper.getScreen().addDomainEntry(Domain.MAIL, controller, null,
          Localized.getConstants().mails());
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
    activePanel.refreshFolder(folderId);
  }

  static void copyMessage(final Long folderFrom, final Long folderTo, String[] places,
      final boolean move) {
    final MailPanel panel = activePanel;
    ParameterList params = createArgs(SVC_COPY_MESSAGES);
    params.addDataItem(COL_ACCOUNT, panel.getCurrentAccount().getAccountId());
    params.addDataItem(COL_FOLDER_PARENT, folderFrom);
    params.addDataItem(COL_FOLDER, folderTo);
    params.addDataItem(COL_PLACE, Codec.beeSerialize(places));
    params.addDataItem("move", move ? 1 : 0);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(panel.getFormView());

        if (!response.hasErrors()) {
          panel.getFormView().notifyInfo(
              move ? Localized.getMessages().mailMovedMessagesToFolder(
                  (String) response.getResponse()) : Localized.getMessages()
                  .mailCopiedMessagesToFolder((String) response.getResponse()),
              BeeUtils.bracket(panel.getCurrentAccount().findFolder(folderTo).getName()));

          if (move && Objects.equals(folderFrom, panel.getCurrentFolderId())) {
            panel.refreshMessages();
          }
          panel.checkFolder(folderTo);
        }
      }
    });
  }

  static ParameterList createArgs(String name) {
    ParameterList args = BeeKeeper.getRpc().createParameters(Module.MAIL.getName());
    args.addQueryItem(AdministrationConstants.METHOD, name);
    return args;
  }

  static void createFolder(String title) {
    final MailPanel panel = activePanel;
    final AccountInfo account = panel.getCurrentAccount();
    final Long parentId = panel.getCurrentFolderId();
    final boolean isParent = !account.isSystemFolder(parentId);
    String caption = null;

    if (isParent) {
      caption = Localized.getConstants().mailInFolder() + " "
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
    });
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
        List<AccountInfo> availableAccounts = Lists.newArrayList();
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
    controller.refresh(activePanel.getCurrentFolderId());
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
          panel.requeryFolders(new ScheduledCommand() {
            @Override
            public void execute() {
              if (Objects.equals(folderId, panel.getCurrentFolderId())) {
                panel.refreshFolder(account.getInboxId());
              }
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
          panel.requeryFolders(new ScheduledCommand() {
            @Override
            public void execute() {
              panel.checkFolder(folderId);
            }
          });
        }
      }
    });
  }

  private static void openMail(final PresenterCallback callback) {
    getAccounts(new BiConsumer<List<AccountInfo>, AccountInfo>() {
      @Override
      public void accept(List<AccountInfo> availableAccounts, AccountInfo defaultAccount) {
        if (!BeeUtils.isEmpty(availableAccounts)) {
          final MailPanel mailPanel = new MailPanel(availableAccounts, defaultAccount);
          mailPanels.add(mailPanel);

          FormFactory.getFormDescription(FORM_MAIL, new Callback<FormDescription>() {
            @Override
            public void onSuccess(FormDescription result) {
              FormFactory.openForm(result, mailPanel, callback);
            }
          });

        } else {
          BeeKeeper.getScreen().notifyWarning("No accounts found");
        }
      }
    });
  }

  private MailKeeper() {
  }
}
