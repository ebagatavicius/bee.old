package com.butent.bee.client.modules.mail;

import com.google.common.collect.Lists;
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
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
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
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.HandlesStateChange;
import com.butent.bee.client.screen.HasDomain;
import com.butent.bee.client.tree.HasTreeItems;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.tree.TreeItem;
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
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MailController extends Flow implements HasDomain, HandlesStateChange {

  private static final String STYLE_SELECTED = "selected";
  private static final String STYLE_DND_TARGET = "dragOver";

  private final FlowPanel sysFoldersPanel;
  private final Tree foldersTree;

  MailController() {
    super();

    FlowPanel panel = new FlowPanel();
    panel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Controller");
    add(panel);

    sysFoldersPanel = new FlowPanel();
    sysFoldersPanel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-SysFolders");
    panel.add(sysFoldersPanel);

    Flow captionPanel = new Flow(BeeConst.CSS_CLASS_PREFIX + "mail-FolderRow");
    panel.add(captionPanel);

    Label caption = new Label(Localized.getConstants().mailFolders());
    caption.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-FolderCaption");
    captionPanel.add(caption);

    Flow actions = new Flow(BeeConst.CSS_CLASS_PREFIX + "mail-FolderActions");
    captionPanel.add(actions);

    final FaLabel create = new FaLabel(FontAwesome.PLUS,
        BeeConst.CSS_CLASS_PREFIX + "mail-FolderAction");
    create.setTitle(Localized.getConstants().mailCreateNewFolder());

    create.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        MailKeeper.createFolder(create.getTitle());
      }
    });
    actions.add(create);

    foldersTree = new Tree();
    foldersTree.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Folders");
    foldersTree.addSelectionHandler(new SelectionHandler<TreeItem>() {
      @Override
      public void onSelection(SelectionEvent<TreeItem> event) {
        TreeItem item = event.getSelectedItem();

        if (item != null) {
          MailKeeper.clickFolder((Long) ((Pair<?, ?>) item.getUserObject()).getA());
        }
      }
    });
    foldersTree.addOpenHandler(new OpenHandler<TreeItem>() {
      @Override
      public void onOpen(OpenEvent<TreeItem> event) {
        onStateChanged(event.getTarget(), true);
      }
    });
    foldersTree.addCloseHandler(new CloseHandler<TreeItem>() {
      @Override
      public void onClose(CloseEvent<TreeItem> event) {
        onStateChanged(event.getTarget(), false);
      }
    });
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

      label.addMouseDownHandler(new MouseDownHandler() {
        @Override
        public void onMouseDown(MouseDownEvent event) {
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
        Flow row = new Flow(BeeConst.CSS_CLASS_PREFIX + "mail-FolderRow");

        final String cap = subFolder.getName();
        Label label = new Label();
        label.setTitle(cap);
        label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Folder");
        label.addMouseDownHandler(new MouseDownHandler() {
          @Override
          public void onMouseDown(MouseDownEvent event) {
            TreeItem selected = foldersTree.getSelectedItem();

            if (selected != null
                && Objects.equals(((Pair<?, ?>) selected.getUserObject()).getA(), folderId)) {
              MailKeeper.clickFolder(folderId);
            }
          }
        });
        setDndTarget(label, folderId);
        row.add(label);

        Flow actions = new Flow(BeeConst.CSS_CLASS_PREFIX + "mail-FolderActions");
        row.add(actions);

        if (subFolder.isConnected()) {
          final FaLabel disconnect = new FaLabel(FontAwesome.CHAIN_BROKEN,
              BeeConst.CSS_CLASS_PREFIX + "mail-FolderAction");
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
          actions.add(disconnect);
        } else {
          label.addStyleDependentName("disconnected");
        }
        final FaLabel edit = new FaLabel(FontAwesome.EDIT,
            BeeConst.CSS_CLASS_PREFIX + "mail-FolderAction");
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
        actions.add(edit);

        final FaLabel delete = new FaLabel(FontAwesome.TRASH_O,
            BeeConst.CSS_CLASS_PREFIX + "mail-FolderAction");
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
