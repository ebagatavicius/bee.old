package com.butent.bee.client.modules.mail;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEndHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.Thermometer;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.DateTimeLabel;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.client.widget.TextLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssProperties;
import com.butent.bee.shared.css.values.Cursor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.LocalizableMessages;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.mail.AccountInfo;
import com.butent.bee.shared.modules.mail.MailConstants.MessageFlag;
import com.butent.bee.shared.modules.mail.MailConstants.RuleCondition;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MailPanel extends AbstractFormInterceptor {

  private class ContentHandler implements ActiveRowChangeEvent.Handler {
    private final int sender;
    private final int flags;

    public ContentHandler(int sender, int flags) {
      this.sender = sender;
      this.flags = flags;
    }

    @Override
    public void onActiveRowChange(ActiveRowChangeEvent event) {
      currentMessage = event.getRowValue();
      showMessage(currentMessage != null);

      if (currentMessage != null) {
        if (!MessageFlag.SEEN.isSet(currentMessage.getInteger(flags))) {
          message.setLoading(true);

          flagMessage(currentMessage, flags, MessageFlag.SEEN, new ScheduledCommand() {
            @Override
            public void execute() {
              getMessagesPresenter().getGridView().getGrid().refresh();
            }
          });
        } else {
          message.requery(COL_PLACE, currentMessage.getId(),
              Objects.equals(currentMessage.getLong(sender), getCurrentAccount().getAddressId()));
        }
      }
    }

    private void showMessage(boolean show) {
      if (!show) {
        message.reset();
      }
      messageWidget.setVisible(show);
      emptySelectionWidget.setVisible(!show);
    }
  }

  private class EnvelopeRenderer extends AbstractCellRenderer {
    private final int senderEmail;
    private final int senderLabel;
    private final int dateIdx;
    private final int subjectIdx;
    private final int flagsIdx;
    private final int attachmentCount;

    public EnvelopeRenderer(List<? extends IsColumn> dataColumns) {
      super(null);

      senderEmail = DataUtils.getColumnIndex("SenderEmail", dataColumns);
      senderLabel = DataUtils.getColumnIndex("SenderLabel", dataColumns);
      dateIdx = DataUtils.getColumnIndex(COL_DATE, dataColumns);
      subjectIdx = DataUtils.getColumnIndex(COL_SUBJECT, dataColumns);
      flagsIdx = DataUtils.getColumnIndex(COL_FLAGS, dataColumns);
      attachmentCount = DataUtils.getColumnIndex("AttachmentCount", dataColumns);
    }

    @Override
    public String render(final IsRow row) {
      Flow fp = new Flow();
      fp.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-Header");

      DomUtils.setDraggable(fp);

      if (!MessageFlag.SEEN.isSet(row.getInteger(flagsIdx))) {
        fp.addStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-HeaderUnread");
      }
      TextLabel sender = new TextLabel(false);
      sender.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-HeaderAddress");
      String address;

      if (isSenderFolder(getCurrentFolderId())) {
        address = BeeUtils.notEmpty(row.getProperty(COL_EMAIL_LABEL),
            row.getProperty(ClassifierConstants.COL_EMAIL_ADDRESS));

        int cnt = BeeUtils.toInt(row.getProperty(COL_ADDRESS)) - 1;

        if (cnt > 0) {
          address += " (" + cnt + "+)";
        }
      } else {
        address = BeeUtils.notEmpty(row.getString(senderLabel), row.getString(senderEmail));
      }
      sender.setHtml(address);
      fp.add(sender);

      Integer att = row.getInteger(attachmentCount);

      if (BeeUtils.isPositive(att)) {
        Widget image = new FaLabel(FontAwesome.PAPERCLIP);
        image.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-AttachmentImage");
        fp.add(image);

        if (att > 1) {
          TextLabel attachments = new TextLabel(false);
          attachments.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-AttachmentCount");
          attachments.setHtml(BeeUtils.toString(att));
          fp.add(attachments);
        }
      }
      DateTime date = row.getDateTime(dateIdx);
      DateTimeLabel dt = TimeUtils.isToday(date)
          ? new DateTimeLabel("TIME_SHORT", false) : new DateTimeLabel("DATE_SHORT", false);
      dt.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-HeaderDate");
      dt.setValue(date);
      fp.add(dt);

      TextLabel subject = new TextLabel(false);
      subject.setStyleName(BeeConst.CSS_CLASS_PREFIX + "mail-HeaderSubject");
      subject.setHtml(row.getString(subjectIdx));
      fp.add(subject);

      return fp.toString();
    }
  }

  private class MessagesGrid extends AbstractGridInterceptor {
    private final Horizontal dummy = new Horizontal();

    @Override
    public void afterCreate(final GridView gridView) {
      Binder.addDragStartHandler(gridView.getGrid(), new DragStartHandler() {
        @Override
        public void onDragStart(DragStartEvent event) {
          EventTarget target = event.getNativeEvent().getEventTarget();

          if (!Element.is(target)) {
            return;
          }
          Integer rowIdx = BeeUtils.toIntOrNull(DomUtils
              .getDataRow(Element.as(target).getParentElement()));

          if (rowIdx == null || !BeeUtils.isIndex(gridView.getGrid().getRowData(), rowIdx)) {
            return;
          }
          Long placeId = gridView.getGrid().getRowData().get(rowIdx).getId();

          EventUtils.allowCopyMove(event);
          EventUtils.setDndData(event, placeId);

          Collection<RowInfo> rows =
              getGridPresenter().getGridView().getSelectedRows(SelectedRows.ALL);
          Set<Long> ids = new HashSet<>();

          if (!BeeUtils.isEmpty(rows)) {
            for (RowInfo info : rows) {
              ids.add(info.getId());
            }
          }
          Label dragLabel = new Label();

          if (ids.contains(placeId)) {
            int cnt = ids.size();
            String ending = (cnt % 10 == 0 || BeeUtils.betweenInclusive(cnt % 100, 11, 19))
                ? "ų" : (cnt % 10 == 1 ? "as" : "ai");
            dragLabel = new Label(BeeUtils.joinWords(cnt, "pažymėt" + ending, "laišk" + ending));
          } else {
            dragLabel = new Label("1 laiškas");
            ids.clear();
          }
          dummy.add(dragLabel);
          BodyPanel.get().add(dummy);
          dragLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
          dragLabel.getElement().getStyle().setBackgroundColor("whiteSmoke");

          event.getDataTransfer().setDragImage(dragLabel.getElement(), 0,
              dummy.getElement().getOffsetHeight());

          DndHelper.fillContent(DATA_TYPE_MESSAGE, placeId, getCurrentFolderId(),
              Codec.beeSerialize(ids));
        }
      });

      Binder.addDragEndHandler(gridView.getGrid(), new DragEndHandler() {
        @Override
        public void onDragEnd(DragEndEvent event) {
          dummy.clear();
          BodyPanel.get().remove(dummy);
          DndHelper.reset();
        }
      });
    }

    @Override
    public void afterCreatePresenter(GridPresenter presenter) {
      GridView grid = presenter.getGridView();
      grid.getGrid().addActiveRowChangeHandler(new ContentHandler(grid.getDataIndex(COL_SENDER),
          grid.getDataIndex(COL_FLAGS)));
      activateAccount(getCurrentAccount());
    }

    @Override
    public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
      return Data.createRowSet(gridDescription.getViewName());
    }

    @Override
    public GridInterceptor getInstance() {
      return new MessagesGrid();
    }

    @Override
    public AbstractCellRenderer getRenderer(String columnName,
        List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
        CellSource cellSource) {

      if (BeeUtils.same(columnName, "Star")) {
        return new StarRenderer(dataColumns);
      } else if (BeeUtils.same(columnName, "Envelope")) {
        return new EnvelopeRenderer(dataColumns);
      }
      return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }

    @Override
    public void onEditStart(final EditStartEvent event) {
      if ("Star".equals(event.getColumnId())) {
        final IsRow row = event.getRowValue();

        if (row == null) {
          return;
        }
        final int flagIdx = Data.getColumnIndex(getGridPresenter().getViewName(), COL_FLAGS);
        event.getSourceElement().setInnerHTML(StarRenderer
            .render(!MessageFlag.FLAGGED.isSet(row.getInteger(flagIdx)),
                row.getProperty(AdministrationConstants.COL_RELATION)));

        flagMessage(row, flagIdx, MessageFlag.FLAGGED, new ScheduledCommand() {
          @Override
          public void execute() {
            event.getSourceElement().setInnerHTML(StarRenderer
                .render(MessageFlag.FLAGGED.isSet(row.getInteger(flagIdx)),
                    row.getProperty(AdministrationConstants.COL_RELATION)));
          }
        });
      }
    }
  }

  private static class StarRenderer extends AbstractCellRenderer {

    private static final FaLabel CHAIN = new FaLabel(FontAwesome.CHAIN);

    static {
      StyleUtils.setProperty(CHAIN, CssProperties.CURSOR, Cursor.DEFAULT);
    }

    public static String render(boolean flagged, String chained) {
      String star = flagged ? Stars.getHtml(0) : Stars.getDefaultHeader();

      if (!BeeUtils.isEmpty(chained)) {
        CHAIN.setTitle(chained);
        star += CHAIN.getElement().getString();
      }
      return star;
    }

    private final int flags;

    public StarRenderer(List<? extends IsColumn> dataColumns) {
      super(null);
      flags = DataUtils.getColumnIndex(COL_FLAGS, dataColumns);
    }

    @Override
    public String render(IsRow row) {
      return render(MessageFlag.FLAGGED.isSet(row.getInteger(flags)),
          row.getProperty(AdministrationConstants.COL_RELATION));
    }
  }

  private static final String MESSAGES_FILTER = "MessagesFilter";

  private AccountInfo currentAccount;
  private Long currentFolder;

  private IsRow currentMessage;

  private final MessagesGrid messages = new MessagesGrid();
  private final MailMessage message = new MailMessage(this);

  private final List<AccountInfo> accounts;

  private Widget messageWidget;
  private Widget emptySelectionWidget;
  private InputText searchWidget;
  private String searchValue;

  MailPanel(List<AccountInfo> availableAccounts, AccountInfo defaultAccount) {
    this.accounts = availableAccounts;
    this.currentAccount = defaultAccount;
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {
    if (widget instanceof GridPanel && BeeUtils.same(name, TBL_PLACES)) {
      ((GridPanel) widget).setGridInterceptor(messages);

    } else if (BeeUtils.same(name, COL_MESSAGE)) {
      messageWidget = widget.asWidget();

    } else if (BeeUtils.same(name, "EmptySelection")) {
      emptySelectionWidget = widget.asWidget();

    } else if (widget instanceof InputText && BeeUtils.same(name, "Search")) {
      searchWidget = (InputText) widget;
      searchWidget.addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> search) {
          doSearch();
        }
      });
      searchWidget.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent ev) {
          if (ev.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            doSearch();
          }
        }
      });
      DomUtils.setSearch(searchWidget);
    }
    message.afterCreateWidget(name, widget, callback);
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    switch (action) {
      case ADD:
        createMessage();
        break;

      case DELETE:
        removeMessages();
        break;

      case PRINT:
        message.beforeAction(action, presenter);
        break;

      default:
        return true;
    }
    return false;
  }

  @Override
  public Domain getDomain() {
    return Domain.MAIL;
  }

  @Override
  public FormInterceptor getInstance() {
    return null;
  }

  @Override
  public void onShow(Presenter presenter) {
    HeaderView header = presenter.getHeader();
    header.clearCommandPanel();

    ListBox accountsWidget = new ListBox();
    initAccounts(accountsWidget);
    header.addCommandItem(accountsWidget);

    if (BeeKeeper.getUser().isDataVisible(TBL_ACCOUNTS)
        && BeeKeeper.getUser().canCreateData(TBL_RULES)) {
      FaLabel accountSettings = new FaLabel(FontAwesome.MAGIC);

      accountSettings.setTitle(Localized.getConstants().mailRule());
      accountSettings.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent arg0) {
          DataInfo dataInfo = Data.getDataInfo(TBL_RULES);
          BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);
          Data.setValue(TBL_RULES, newRow, COL_ACCOUNT, getCurrentAccount().getAccountId());

          if (currentMessage != null) {
            if (isSenderFolder(getCurrentFolderId())) {
              Data.setValue(TBL_RULES, newRow, COL_RULE_CONDITION,
                  RuleCondition.RECIPIENTS.ordinal());
              Data.setValue(TBL_RULES, newRow, COL_RULE_CONDITION_OPTIONS,
                  currentMessage.getProperty(ClassifierConstants.COL_EMAIL_ADDRESS));
            } else {
              Data.setValue(TBL_RULES, newRow, COL_RULE_CONDITION, RuleCondition.SENDER.ordinal());
              Data.setValue(TBL_RULES, newRow, COL_RULE_CONDITION_OPTIONS,
                  Data.getString(TBL_PLACES, currentMessage, "SenderEmail"));
            }
          }
          RowFactory.createRow(dataInfo, newRow);
        }
      });
      header.addCommandItem(accountSettings);
    }
    FaLabel refreshWidget = new FaLabel(FontAwesome.REFRESH);

    refreshWidget.setTitle(Localized.getConstants().actionRefresh());
    refreshWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg0) {
        checkFolderRecursively(getCurrentAccount().getRootFolder());
      }
    });
    header.addCommandItem(refreshWidget);

    FaLabel unseenWidget = new FaLabel(FontAwesome.EYE_SLASH);

    unseenWidget.setTitle(Localized.getConstants().mailMarkAsUnread());
    unseenWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg0) {
        if (currentMessage != null) {
          flagMessage(currentMessage,
              DataUtils.getColumnIndex(COL_FLAGS, getMessagesPresenter().getDataColumns()),
              MessageFlag.SEEN, new ScheduledCommand() {
                @Override
                public void execute() {
                  refreshMessages();
                }
              });
        }
      }
    });
    header.addCommandItem(unseenWidget);

    message.setFormView(getFormView());
  }

  @Override
  public void onStateChange(State state) {
    if (State.ACTIVATED.equals(state)) {
      MailKeeper.activateController(this);

    } else if (State.REMOVED.equals(state)) {
      MailKeeper.removeMailPanel(this);
    }
  }

  void checkFolder(final Long folderId) {
    final AccountInfo account = getCurrentAccount();
    final ParameterList params = MailKeeper.createArgs(SVC_CHECK_MAIL);
    params.addDataItem(COL_ACCOUNT, account.getAccountId());
    params.addDataItem(COL_FOLDER, folderId);

    final String progressId;

    if (Endpoint.isOpen()) {
      String cap;

      if (account.isDraftsFolder(folderId)) {
        cap = Localized.getConstants().mailFolderDrafts();
      } else if (account.isInboxFolder(folderId)) {
        cap = Localized.getConstants().mailFolderInbox();
      } else if (account.isSentFolder(folderId)) {
        cap = Localized.getConstants().mailFolderSent();
      } else if (account.isTrashFolder(folderId)) {
        cap = Localized.getConstants().mailFolderTrash();
      } else {
        cap = account.findFolder(folderId).getName();
      }
      InlineLabel close = new InlineLabel(String.valueOf(BeeConst.CHAR_TIMES));
      Thermometer th = new Thermometer(cap, BeeConst.DOUBLE_ONE, close);

      progressId = BeeKeeper.getScreen().addProgress(th);

      if (progressId != null) {
        close.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Endpoint.cancelProgress(progressId);
          }
        });
      }
    } else {
      progressId = null;
    }
    final ResponseCallback callback = new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());
      }
    };
    if (progressId == null) {
      BeeKeeper.getRpc().makePostRequest(params, callback);
    } else {
      Endpoint.enqueuePropgress(progressId, new Consumer<String>() {
        @Override
        public void accept(String input) {
          if (!BeeUtils.isEmpty(input)) {
            params.addDataItem(Service.VAR_PROGRESS, input);
          } else {
            Endpoint.cancelProgress(progressId);
          }
          BeeKeeper.getRpc().makePostRequest(params, callback);
        }
      });
    }
  }

  List<AccountInfo> getAccounts() {
    return accounts;
  }

  AccountInfo getCurrentAccount() {
    return currentAccount;
  }

  Long getCurrentFolderId() {
    return currentFolder;
  }

  void refreshFolder(Long folderId) {
    if (Objects.equals(folderId, getCurrentFolderId())) {
      checkFolder(folderId);
    } else {
      currentFolder = folderId;
      MailKeeper.refreshController();
      refreshMessages();
    }
  }

  void refreshMessages() {
    GridPresenter presenter = getMessagesPresenter();

    if (presenter != null) {
      Long folderId = getCurrentFolderId();

      if (!BeeUtils.isEmpty(searchWidget.getValue())) {
        presenter.getDataProvider().setUserFilter(Filter.custom(TBL_PLACES,
            Codec.beeSerialize(ImmutableMap.of(COL_FOLDER, folderId,
                COL_CONTENT, searchWidget.getValue(),
                SystemFolder.Sent.name(), isSenderFolder(folderId)))));
      } else {
        presenter.getDataProvider().setUserFilter(null);
      }
      presenter.getDataProvider().setParentFilter(MESSAGES_FILTER,
          Filter.equals(COL_FOLDER, folderId));

      presenter.getGridView().getGrid().reset();
      presenter.refresh(false);
    }
  }

  void requeryFolders() {
    requeryFolders(null);
  }

  void requeryFolders(final ScheduledCommand afterRequery) {
    final AccountInfo account = getCurrentAccount();

    ParameterList params = MailKeeper.createArgs(SVC_GET_FOLDERS);
    params.addDataItem(COL_ACCOUNT, account.getAccountId());

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        Assert.state(response.hasResponse(MailFolder.class));
        response.notify(getFormView());

        account.setRootFolder(MailFolder.restore(response.getResponseAsString()));

        if (account == getCurrentAccount()) {
          MailKeeper.rebuildController();

          if (afterRequery != null) {
            afterRequery.execute();
          }
        }
      }
    });
  }

  private void activateAccount(AccountInfo selectedAccount) {
    currentAccount = selectedAccount;

    requeryFolders(new ScheduledCommand() {
      @Override
      public void execute() {
        if (searchWidget != null) {
          searchWidget.clearValue();
          searchValue = null;
        }
        currentFolder = null;
        refreshFolder(getCurrentAccount().getInboxId());
      }
    });
  }

  private void checkFolderRecursively(MailFolder folder) {
    if (DataUtils.isId(folder.getId())) {
      checkFolder(folder.getId());
    }
    for (MailFolder subFolder : folder.getSubFolders()) {
      checkFolderRecursively(subFolder);
    }
  }

  private void createMessage() {
    NewMailMessage newMessage = NewMailMessage.create(accounts, getCurrentAccount(), null, null,
        null, null, null, null, null);

    newMessage.setScheduled(new Consumer<Boolean>() {
      @Override
      public void accept(Boolean save) {
        if (BeeUtils.isFalse(save)) {
          checkFolder(getCurrentAccount().getSystemFolder(SystemFolder.Sent));
        }
        checkFolder(getCurrentAccount().getSystemFolder(SystemFolder.Drafts));
      }
    });
  }

  private void doSearch() {
    if (searchWidget != null) {
      String value = searchWidget.getValue();

      if (!BeeUtils.same(value, searchValue)) {
        searchValue = value;
        refreshMessages();
      }
    }
  }

  private void flagMessage(final IsRow row, final int flagIdx, MessageFlag flag,
      final ScheduledCommand command) {

    ParameterList params = MailKeeper.createArgs(SVC_FLAG_MESSAGE);
    params.addDataItem(COL_PLACE, row.getId());
    params.addDataItem(COL_FLAGS, flag.name());
    params.addDataItem("on", Codec.pack(!flag.isSet(row.getInteger(flagIdx))));

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());

        if (!response.hasErrors()) {
          Integer flags = BeeUtils.toIntOrNull((String) response.getResponse());
          row.setValue(flagIdx, flags);

          if (command != null) {
            command.execute();
          }
        }
      }
    });
  }

  private GridPresenter getMessagesPresenter() {
    return messages.getGridPresenter();
  }

  private void initAccounts(final ListBox accountsWidget) {
    accountsWidget.clear();

    for (int i = 0; i < accounts.size(); i++) {
      AccountInfo accountInfo = accounts.get(i);
      accountsWidget.addItem(accountInfo.getDescription() + " <" + accountInfo.getAddress() + ">");

      if (Objects.equals(accountInfo, currentAccount)) {
        accountsWidget.setSelectedIndex(i);
      }
    }
    accountsWidget.setEnabled(accountsWidget.getItemCount() > 1);
    accountsWidget.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        AccountInfo selectedAccount = accounts.get(accountsWidget.getSelectedIndex());

        if (!Objects.equals(selectedAccount, currentAccount)) {
          activateAccount(selectedAccount);
        }
      }
    });
  }

  private boolean isSenderFolder(Long folderId) {
    return getCurrentAccount().isSentFolder(folderId)
        || getCurrentAccount().isDraftsFolder(folderId);
  }

  private void removeMessages() {
    if (currentMessage == null) {
      return;
    }
    final List<String> options = Lists.newArrayList(Localized.getConstants().mailCurrentMessage());
    final Collection<RowInfo> rows = getMessagesPresenter().getGridView()
        .getSelectedRows(SelectedRows.ALL);

    if (!BeeUtils.isEmpty(rows)) {
      options.add(Localized.getMessages().mailSelectedMessages(rows.size()));
    }
    final Long folderId = getCurrentFolderId();
    final boolean purge = getCurrentAccount().isTrashFolder(folderId);

    options.add(Localized.getConstants().cancel());
    Icon icon = purge ? Icon.ALARM : Icon.WARNING;

    Global.messageBox(purge ? Localized.getConstants().actionDelete() : Localized.getConstants()
        .mailActionMoveToTrash(), icon, null, options, options.size() - 1, new ChoiceCallback() {
      @Override
      public void onSuccess(int value) {
        if (value == options.size() - 1) {
          return;
        }
        List<Long> ids = null;

        if (value == 0) {
          ids = Lists.newArrayList(currentMessage.getId());
        } else if (value == 1) {
          ids = new ArrayList<>();

          for (RowInfo info : rows) {
            ids.add(info.getId());
          }
        }
        ParameterList params = MailKeeper.createArgs(SVC_REMOVE_MESSAGES);
        params.addDataItem(COL_ACCOUNT, getCurrentAccount().getAccountId());
        params.addDataItem(COL_FOLDER, getCurrentFolderId());
        params.addDataItem(COL_PLACE, Codec.beeSerialize(ids));
        params.addDataItem("Purge", Codec.pack(purge));

        BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            response.notify(getFormView());

            if (!response.hasErrors()) {
              String msg = response.getResponseAsString();
              LocalizableMessages loc = Localized.getMessages();

              getFormView().notifyInfo(purge
                  ? loc.mailDeletedMessages(msg)
                  : loc.mailMovedMessagesToTrash(msg));

              if (Objects.equals(folderId, getCurrentFolderId())) {
                refreshMessages();
              }
              if (!purge) {
                checkFolder(getCurrentAccount().getSystemFolder(SystemFolder.Trash));
              }
            }
          }
        });
      }
    });
  }
}
