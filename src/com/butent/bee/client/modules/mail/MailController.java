package com.butent.bee.client.modules.mail;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.Settings;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.HandlesStateChange;
import com.butent.bee.client.screen.HasDomain;
import com.butent.bee.client.tree.HasTreeItems;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.tree.TreeItem;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.State;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MailController extends Flow implements HasDomain, HandlesStateChange {

  private static final String STYLE_SELECTED = "selected";
  private static final String STYLE_DND_TARGET = "dragOver";
  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "mail-";

  private final FlowPanel sysFoldersPanel;
  private final Tree foldersTree;

  MailController() {
    super();

    FlowPanel panel = new FlowPanel();
    panel.setStyleName(STYLE_PREFIX + "Controller");
    add(panel);

    sysFoldersPanel = new FlowPanel();
    sysFoldersPanel.setStyleName(STYLE_PREFIX + "SysFolders");
    panel.add(sysFoldersPanel);

    Flow captionPanel = new Flow(STYLE_PREFIX + "FolderRow");
    panel.add(captionPanel);

    Label caption = new Label(Localized.dictionary().mailFolders());
    caption.setStyleName(STYLE_PREFIX + "FolderCaption");
    captionPanel.add(caption);

    Flow actions = new Flow(STYLE_PREFIX + "FolderActions");
    captionPanel.add(actions);

    FaLabel resync = new FaLabel(FontAwesome.REFRESH, STYLE_PREFIX + "FolderAction");
    resync.setTitle(Localized.dictionary().actionRefresh());
    resync.addClickHandler(event -> MailKeeper.syncFolders(event.isShiftKeyDown()));
    actions.add(resync);

    FaLabel create = new FaLabel(FontAwesome.PLUS, STYLE_PREFIX + "FolderAction");
    create.setTitle(Localized.dictionary().mailCreateNewFolder());
    create.addClickHandler(event -> MailKeeper.createFolder(create.getTitle()));
    actions.add(create);

    foldersTree = new Tree();
    foldersTree.setStyleName(STYLE_PREFIX + "Folders");
    foldersTree.addSelectionHandler(event -> {
      TreeItem item = event.getSelectedItem();

      if (item != null) {
        MailKeeper.clickFolder((Long) ((Pair<?, ?>) item.getUserObject()).getA());
      }
    });
    foldersTree.addOpenHandler(event -> onStateChanged(event.getTarget(), true));
    foldersTree.addCloseHandler(event -> onStateChanged(event.getTarget(), false));
    panel.add(foldersTree);
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

    if (BeeUtils.isEmpty(account.getRootFolder().getSubFolders())) {
      foldersTree.clear();
      return;
    }
    for (final SystemFolder sysFolder : SystemFolder.values()) {
      String cap = null;

      switch (sysFolder) {
        case Drafts:
          cap = Localized.dictionary().mailFolderDrafts();
          break;
        case Inbox:
          cap = Localized.dictionary().mailFolderInbox();
          break;
        case Sent:
          cap = Localized.dictionary().mailFolderSent();
          break;
        case Trash:
          cap = Localized.dictionary().mailFolderTrash();
          break;
      }
      final Long folderId = account.getSystemFolder(sysFolder);
      MailFolder folder = account.findFolder(folderId);
      final DndDiv label = new DndDiv(STYLE_PREFIX + "SysFolder");

      label.addMouseDownHandler(event -> MailKeeper.clickFolder(folderId, event.isShiftKeyDown()));
      if (folder.getUnread() > 0) {
        cap += " (" + BeeUtils.toString(folder.getUnread()) + ")";
        label.addStyleDependentName("unread");
      }
      label.setHtml(cap);
      DndHelper.makeTarget(label, Collections.singleton(MailConstants.DATA_TYPE_MESSAGE),
          STYLE_DND_TARGET, DndHelper.ALWAYS_TARGET, (event, data) -> {
            label.setTargetState(null);
            label.removeStyleName(STYLE_DND_TARGET);
            MailKeeper.copyMessage((String) data, folderId,
                !EventUtils.hasModifierKey(event.getNativeEvent()));
          });
      DomUtils.setDataProperty(label.getElement(), MailConstants.COL_FOLDER, folderId);
      sysFoldersPanel.add(label);
    }
    Set<Long> opened = new HashSet<>();

    if (foldersTree.getItemCount() > 0) {
      for (TreeItem child : foldersTree.getTreeItems()) {
        opened.addAll(getOpened(child));
      }
    }
    foldersTree.clear();
    buildTree(account, account.getRootFolder(), foldersTree, opened);
  }

  void refresh(Long folderId) {
    for (Widget widget : sysFoldersPanel) {
      widget.setStyleDependentName(STYLE_SELECTED,
          Objects.equals(DomUtils.getDataPropertyLong(widget.getElement(),
              MailConstants.COL_FOLDER), folderId));
    }
    TreeItem selected = null;

    if (foldersTree.getItemCount() > 0) {
      for (TreeItem child : foldersTree.getTreeItems()) {
        selected = findItem(child, folderId);

        if (selected != null) {
          break;
        }
      }
    }
    foldersTree.setSelectedItem(selected, false);
  }

  private void buildTree(final AccountInfo account, MailFolder folder, HasTreeItems parent,
      Set<Long> opened) {

    for (MailFolder subFolder : folder.getSubFolders()) {
      final long folderId = subFolder.getId();

      if (!account.isSystemFolder(folderId)) {
        Flow row = new Flow(STYLE_PREFIX + "FolderRow");

        final String cap = subFolder.getName();
        final DndDiv label = new DndDiv(STYLE_PREFIX + "Folder");
        label.setTitle(cap);
        label.addMouseDownHandler(event -> {
          TreeItem selected = foldersTree.getSelectedItem();

          if (selected != null
              && Objects.equals(((Pair<?, ?>) selected.getUserObject()).getA(), folderId)) {
            MailKeeper.clickFolder(folderId, event.isShiftKeyDown());
          }
        });
        DndHelper.makeTarget(label, Collections.singleton(MailConstants.DATA_TYPE_MESSAGE),
            STYLE_DND_TARGET, DndHelper.ALWAYS_TARGET, (event, data) -> {
              label.setTargetState(null);
              label.removeStyleName(STYLE_DND_TARGET);
              MailKeeper.copyMessage((String) data, folderId,
                  !EventUtils.hasModifierKey(event.getNativeEvent()));
            });
        row.add(label);

        Flow actions = new Flow(STYLE_PREFIX + "FolderActions");
        row.add(actions);

        if (subFolder.isConnected()) {
          if (Objects.equals(subFolder.getParent(), account.getRootFolder())) {
            final FaLabel disconnect = new FaLabel(FontAwesome.CHAIN_BROKEN,
                STYLE_PREFIX + "FolderAction");
            disconnect.setTitle(Localized.dictionary()
                .mailCancelFolderSynchronizationQuestion(BeeUtils.bracket(cap)));

            disconnect.addClickHandler(event -> Global.confirmDelete(Settings.getAppName(),
                Icon.WARNING, Lists.newArrayList(disconnect.getTitle(),
                    "(" + Localized.dictionary().mailFolderContentsWillBeRemovedFromTheMailServer()
                        + ")"), () -> MailKeeper.disconnectFolder(account, folderId)));
            actions.add(disconnect);
          }
        } else {
          label.addStyleDependentName("disconnected");
        }
        final FaLabel edit = new FaLabel(FontAwesome.EDIT, STYLE_PREFIX + "FolderAction");
        edit.setTitle(Localized.dictionary().mailRenameFolder(BeeUtils.bracket(cap)));

        edit.addClickHandler(
            event -> Global.inputString(edit.getTitle(), null, new StringCallback() {
              @Override
              public void onSuccess(String value) {
                if (!cap.equals(value)) {
                  MailKeeper.renameFolder(account, folderId, value);
                }
              }
            }, null, cap));
        actions.add(edit);

        final FaLabel delete = new FaLabel(FontAwesome.TRASH_O, STYLE_PREFIX + "FolderAction");
        delete.setTitle(Localized.dictionary()
            .mailDeleteFolderQuestion(BeeUtils.bracket(cap)));

        delete.addClickHandler(event -> Global.confirmDelete(Settings.getAppName(), Icon.ALARM,
            Lists.newArrayList(delete.getTitle()),
            () -> MailKeeper.removeFolder(account, folderId)));
        actions.add(delete);

        TreeItem item = parent.addItem(row);
        item.setUserObject(Pair.of(folderId, subFolder.getUnread()));

        buildTree(account, subFolder, item, opened);

        boolean isOpen = opened.contains(folderId);
        item.setOpen(isOpen, false);
        onStateChanged(item, isOpen);
      } else {
        buildTree(account, subFolder, parent, opened);
      }
    }
  }

  private static TreeItem findItem(TreeItem treeItem, Long folderId) {
    if (Objects.equals(((Pair<?, ?>) treeItem.getUserObject()).getA(), folderId)) {
      return treeItem;
    }
    if (treeItem.getItemCount() > 0) {
      for (TreeItem child : treeItem.getTreeItems()) {
        TreeItem item = findItem(child, folderId);

        if (item != null) {
          return item;
        }
      }
    }
    return null;
  }

  private static Set<Long> getOpened(TreeItem treeItem) {
    Set<Long> opened = new HashSet<>();

    if (treeItem.isOpen()) {
      opened.add((Long) ((Pair<?, ?>) treeItem.getUserObject()).getA());
    }
    if (treeItem.getChildCount() > 0) {
      for (TreeItem child : treeItem.getTreeItems()) {
        opened.addAll(getOpened(child));
      }
    }
    return opened;
  }

  private static int getUnread(TreeItem treeItem, final boolean recursive) {
    int unread = BeeUtils.unbox((Integer) ((Pair<?, ?>) treeItem.getUserObject()).getB());

    if (recursive && treeItem.getChildCount() > 0) {
      for (TreeItem child : treeItem.getTreeItems()) {
        unread += getUnread(child, recursive);
      }
    }
    return unread;
  }

  private static void onStateChanged(TreeItem item, boolean open) {
    if (item != null) {
      Widget label = ((HasWidgets) item.getWidget()).iterator().next();
      String cap = label.getTitle();
      int unread = getUnread(item, !open);

      if (unread > 0) {
        cap += " (" + BeeUtils.toString(unread) + ")";
        label.addStyleDependentName("unread");
      } else {
        label.removeStyleDependentName("unread");
      }
      label.getElement().setInnerText(cap);
    }
  }
}
