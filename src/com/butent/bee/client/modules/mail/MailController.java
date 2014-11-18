package com.butent.bee.client.modules.mail;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.Settings;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.HandlesStateChange;
import com.butent.bee.client.screen.HasDomain;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;

public class MailController extends Flow implements HasDomain, HandlesStateChange {

  private static final String STYLE_SELECTED = "selected";
  private static final String STYLE_DND_TARGET = "dragOver";

  private final FlowPanel sysFoldersPanel;
  private final FlowPanel foldersPanel;

  MailController() {
    super();

    FlowPanel panel = new FlowPanel();
    panel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Controller");
    add(panel);

    sysFoldersPanel = new FlowPanel();
    sysFoldersPanel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-SysFolders");
    panel.add(sysFoldersPanel);

    Horizontal caption = new Horizontal();
    caption.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-FolderRow");
    panel.add(caption);

    Label label = new Label(Localized.getConstants().mailFolders());
    label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-FolderCaption");
    caption.add(label);

    final FaLabel create =
        new FaLabel(FontAwesome.PLUS, BeeConst.CSS_CLASS_PREFIX + "mail-FolderAction");
    create.setTitle(Localized.getConstants().mailCreateNewFolder());

    create.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        MailKeeper.createFolder(create.getTitle());
      }
    });
    caption.add(create);

    foldersPanel = new FlowPanel();
    foldersPanel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Folders");
    panel.add(foldersPanel);
  }

  @Override
  public Domain getDomain() {
    return Domain.MAIL;
  }

  @Override
  public void onStateChange(State state) {
    if (State.ACTIVATED.equals(state)) {
      MailKeeper.activateMailPanel();
    } else if (State.REMOVED.equals(state)) {
      MailKeeper.removeMailPanels();
    }
    LogUtils.getRootLogger().debug("MailController", state);
  }

  void rebuild(AccountInfo account) {
    sysFoldersPanel.clear();
    foldersPanel.clear();

    if (BeeUtils.isEmpty(account.getRootFolder().getSubFolders())) {
      return;
    }
    for (final SystemFolder sysFolder : SystemFolder.values()) {
      String cap = null;

      switch (sysFolder) {
        case Drafts:
          cap = Localized.getConstants().mailFolderDrafts();
          break;
        case Inbox:
          cap = Localized.getConstants().mailFolderInbox();
          break;
        case Sent:
          cap = Localized.getConstants().mailFolderSent();
          break;
        case Trash:
          cap = Localized.getConstants().mailFolderTrash();
          break;
      }
      final Long folderId = account.getSystemFolder(sysFolder);
      MailFolder folder = account.findFolder(folderId);
      Label label = new Label();
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-SysFolder");

      label.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          MailKeeper.clickFolder(folderId);
        }
      });
      if (folder.getUnread() > 0) {
        cap += " (" + BeeUtils.toString(folder.getUnread()) + ")";
        label.addStyleDependentName("unread");
      }
      label.setHtml(cap);
      setDndTarget(label, folderId);
      DomUtils.setDataProperty(label.getElement(), MailConstants.COL_FOLDER, folderId);
      sysFoldersPanel.add(label);
    }
    buildTree(account, account.getRootFolder(), 0);
  }

  void refresh(Long folderId) {
    for (FlowPanel panel : new FlowPanel[] {sysFoldersPanel, foldersPanel}) {
      for (int i = 0; i < panel.getWidgetCount(); i++) {
        Widget widget = panel.getWidget(i);

        widget.setStyleDependentName(STYLE_SELECTED,
            Objects.equals(DomUtils.getDataPropertyLong(widget.getElement(),
                MailConstants.COL_FOLDER), folderId));
      }
    }
  }

  private void buildTree(final AccountInfo account, MailFolder folder, int margin) {
    for (MailFolder subFolder : folder.getSubFolders()) {
      final long folderId = subFolder.getId();
      int mrg = margin;

      if (!account.isSystemFolder(folderId)) {
        Horizontal row = new Horizontal();
        row.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-FolderRow");
        row.setDefaultCellStyles("padding: 0px;");

        final String cap = subFolder.getName();
        Label label = new Label();
        label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Folder");
        label.getElement().getStyle().setMarginLeft(margin, Unit.PX);
        label.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            MailKeeper.clickFolder(folderId);
          }
        });
        if (subFolder.getUnread() > 0) {
          label.setHtml(cap + " (" + BeeUtils.toString(subFolder.getUnread()) + ")");
          label.addStyleDependentName("unread");
        } else {
          label.setHtml(cap);
        }
        if (BeeUtils.isEmpty(subFolder.getSubFolders())) {
          label.addStyleDependentName("leaf");
        } else {
          label.addStyleDependentName("branch");
        }
        setDndTarget(label, folderId);
        row.add(label);

        if (subFolder.isConnected()) {
          final FaLabel disconnect =
              new FaLabel(FontAwesome.CHAIN_BROKEN, BeeConst.CSS_CLASS_PREFIX
                  + "mail-FolderAction");
          disconnect.setTitle(Localized.getMessages()
              .mailCancelFolderSynchronizationQuestion(BeeUtils.bracket(cap)));

          disconnect.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              Global.confirmDelete(Settings.getAppName(), Icon.WARNING,
                  Lists.newArrayList(disconnect.getTitle(), "(" + Localized.getConstants()
                      .mailFolderContentsWillBeRemovedFromTheMailServer() + ")"),
                  new ConfirmationCallback() {
                    @Override
                    public void onConfirm() {
                      MailKeeper.disconnectFolder(account, folderId);
                    }
                  });
            }
          });
          row.add(disconnect);
        } else {
          label.addStyleDependentName("disconnected");
        }
        final FaLabel edit =
            new FaLabel(FontAwesome.EDIT, BeeConst.CSS_CLASS_PREFIX + "mail-FolderAction");
        edit.setTitle(Localized.getMessages().mailRenameFolder(BeeUtils.bracket(cap)));

        edit.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Global.inputString(edit.getTitle(), null, new StringCallback() {
              @Override
              public void onSuccess(String value) {
                if (!cap.equals(value)) {
                  MailKeeper.renameFolder(account, folderId, value);
                }
              }
            }, cap);
          }
        });
        row.add(edit);

        final FaLabel delete =
            new FaLabel(FontAwesome.TRASH_O, BeeConst.CSS_CLASS_PREFIX + "mail-FolderAction");
        delete.setTitle(Localized.getMessages()
            .mailDeleteFolderQuestion(BeeUtils.bracket(cap)));

        delete.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Global.confirmDelete(Settings.getAppName(), Icon.ALARM,
                Lists.newArrayList(delete.getTitle()),
                new ConfirmationCallback() {
                  @Override
                  public void onConfirm() {
                    MailKeeper.removeFolder(account, folderId);
                  }
                });
          }
        });
        row.add(delete);

        DomUtils.setDataProperty(row.getElement(), MailConstants.COL_FOLDER, folderId);
        foldersPanel.add(row);
        mrg += 10;
      }
      buildTree(account, subFolder, mrg);
    }
  }

  private static void setDndTarget(final Widget label, final Long folderId) {
    Binder.addDragEnterHandler(label, new DragEnterHandler() {
      @Override
      public void onDragEnter(DragEnterEvent event) {
        if (BeeUtils.same(DndHelper.getDataType(), MailConstants.DATA_TYPE_MESSAGE)) {
          label.addStyleDependentName(STYLE_DND_TARGET);
        }
      }
    });
    Binder.addDragOverHandler(label, new DragOverHandler() {
      @Override
      public void onDragOver(DragOverEvent event) {
        if (DndHelper.isDataType(MailConstants.DATA_TYPE_MESSAGE)) {
          if (EventUtils.hasModifierKey(event.getNativeEvent())) {
            EventUtils.selectDropCopy(event);
          } else if (!Objects.equals(DndHelper.getRelatedId(), folderId)) {
            EventUtils.selectDropMove(event);
          } else {
            EventUtils.selectDropNone(event);
          }
        } else {
          EventUtils.selectDropNone(event);
        }
      }
    });
    Binder.addDragLeaveHandler(label, new DragLeaveHandler() {
      @Override
      public void onDragLeave(DragLeaveEvent event) {
        if (DndHelper.isDataType(MailConstants.DATA_TYPE_MESSAGE)) {
          label.removeStyleDependentName(STYLE_DND_TARGET);
        }
      }
    });
    Binder.addDropHandler(label, new DropHandler() {
      @Override
      public void onDrop(DropEvent event) {
        if (DndHelper.isDataType(MailConstants.DATA_TYPE_MESSAGE)) {
          label.removeStyleDependentName(STYLE_DND_TARGET);
          String[] places = Codec.beeDeserializeCollection((String) DndHelper.getData());

          if (ArrayUtils.isEmpty(places)) {
            places = new String[] {BeeUtils.toString(DndHelper.getDataId())};
          }

          MailKeeper.copyMessage(DndHelper.getRelatedId(), folderId, places,
              !EventUtils.hasModifierKey(event.getNativeEvent()));

          event.stopPropagation();
        }
      }
    });
  }
}
