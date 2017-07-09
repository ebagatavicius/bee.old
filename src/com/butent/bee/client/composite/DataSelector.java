package com.butent.bee.client.composite;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.HasMouseWheelHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Settings;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.HasRelatedRow;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.SelectionOracle;
import com.butent.bee.client.data.SelectionOracle.Callback;
import com.butent.bee.client.data.SelectionOracle.Request;
import com.butent.bee.client.data.SelectionOracle.Response;
import com.butent.bee.client.data.SelectionOracle.Suggestion;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.InputEvent;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.menu.MenuBar;
import com.butent.bee.client.menu.MenuItem;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.render.SimpleRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.HasTextBox;
import com.butent.bee.client.view.edit.TextBox;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuConstants;
import com.butent.bee.shared.menu.MenuConstants.BarType;
import com.butent.bee.shared.menu.MenuConstants.ItemType;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasCapsLock;
import com.butent.bee.shared.ui.HasMaxLength;
import com.butent.bee.shared.ui.HasStringValue;
import com.butent.bee.shared.ui.HasVisibleLines;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.SelectorColumn;
import com.butent.bee.shared.ui.WindowType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Enables using user interface component for entering text entries while the system is suggesting
 * possible matching values from the list.
 */

public class DataSelector extends Composite implements Editor, HasVisibleLines, HasTextBox,
    HasRelatedRow, HasCapsLock, HasKeyDownHandlers, HasMaxLength {

  protected final class InputWidget extends InputText implements HasMouseWheelHandlers {

    private InputWidget() {
      super();

      addInputHandler(inputEvents);
      addMouseWheelHandler(inputEvents);

      sinkEvents(Event.ONBLUR | Event.ONMOUSEDOWN | Event.ONCLICK | Event.KEYEVENTS);
    }

    @Override
    public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
      return addDomHandler(handler, MouseWheelEvent.getType());
    }

    @Override
    public void onBrowserEvent(Event event) {
      boolean showing = getSelector().isShowing();
      int type = event.getTypeInt();

      boolean consumed = false;

      switch (type) {
        case Event.ONBLUR:
          if (showing || isAdding()) {
            cancelInputTimer();
            return;
          } else {
            reset();
          }
          break;

        case Event.ONMOUSEDOWN:
          cancelInputTimer();
          break;

        case Event.ONCLICK:
          onMouseClick();
          break;

        case Event.ONKEYDOWN:
          cancelInputTimer();

          inputEvents.setConsumed(false);
          inputEvents.setLastEventType(type);

          if (isEmbedded() && event.getKeyCode() == KeyCodes.KEY_DELETE && isNullable()
              && !BeeUtils.isEmpty(getDisplayValue())) {

            consumed = true;
            setSelection(null, null, true);

          } else if (isEmbedded() && !isActive()) {
            if (event.getKeyCode() == KeyCodes.KEY_BACKSPACE
                && !BeeUtils.isEmpty(getDisplayValue())) {

              consumed = true;
              start(SHOW_SELECTOR);
            }

          } else {
            inputEvents.onKeyDown(event);
            consumed = inputEvents.isConsumed();
          }
          break;

        case Event.ONKEYPRESS:
          cancelInputTimer();
          inputEvents.setLastEventType(type);

          if (isEmbedded() && !isActive()) {
            int charCode = event.getCharCode();
            if (charCode > BeeConst.CHAR_SPACE
                && Codec.isValidUnicodeChar(BeeUtils.toChar(charCode))) {

              consumed = true;
              start(charCode);
            }

          } else if (event.getCharCode() == CREATE_NEW && isNewRowEnabled()) {
            if (showing) {
              getSelector().hide();
            }

            consumed = true;
            RowFactory.createRelatedRow(DataSelector.this, getDisplayValue());

          } else {
            consumed = inputEvents.isConsumed()
                || consumeCharacter(BeeUtils.toChar(event.getCharCode()));
          }
          break;

        case Event.ONKEYUP:
          inputEvents.setLastEventType(type);

          inputEvents.onKeyUp();
          consumed = inputEvents.isConsumed();
          break;
      }

      if (consumed) {
        event.preventDefault();
        inputEvents.consume();
      } else {
        super.onBrowserEvent(event);
      }
    }

    protected void onMouseClick() {
      if (isEmbedded() && !isActive()) {
        start(EditStartEvent.CLICK);

      } else if (!getSelector().isShowing() && getMinQueryLength() <= 0) {
        clearDisplay();
        askOrSchedule();
      }
    }
  }

  private class InputEvents implements InputHandler, MouseWheelHandler, Consumable {

    private boolean consumed;
    private int lastEventType;

    @Override
    public void consume() {
      setConsumed(true);
    }

    @Override
    public boolean isConsumed() {
      return consumed;
    }

    @Override
    public void onInput(InputEvent event) {
      if (isEnabled() && (!isConsumed() || !wasKeyPressed())
          && event.getSource() instanceof HasStringValue) {

        String value = ((HasStringValue) event.getSource()).getValue();
        if (!BeeUtils.isEmpty(value)) {
          consume();

          if (!isActive()) {
            start(BeeConst.UNDEF);
          }

          if (isQueryValid()) {
            askOrSchedule();
          }
        }
      }
    }

    @Override
    public void onMouseWheel(MouseWheelEvent event) {
      if (isEnabled() && isActive() && getSelector().isShowing()) {
        int y = event.getDeltaY();

        if (y > 0) {
          if (EventUtils.hasModifierKey(event)) {
            nextPage();
          } else {
            nextOffset();
          }

        } else if (y < 0) {
          if (EventUtils.hasModifierKey(event)) {
            prevPage();
          } else {
            prevOffset();
          }
        }
      }
    }

    @Override
    public void setConsumed(boolean consumed) {
      this.consumed = consumed;
    }

    private void onKeyDown(Event event) {
      if (!isEnabled() || !isActive()) {
        setConsumed(false);
        return;
      }

      int keyCode = event.getKeyCode();
      boolean hasModifiers = EventUtils.hasModifierKey(event);

      switch (keyCode) {
        case KeyCodes.KEY_DOWN:
          consume();
          if (hasModifiers && hasMore()) {
            nextOffset();
          } else {
            getSelector().moveSelectionDown();
          }
          break;

        case KeyCodes.KEY_UP:
          consume();
          if (hasModifiers && getOffset() > 0) {
            prevOffset();
          } else {
            getSelector().moveSelectionUp();
          }
          break;

        case KeyCodes.KEY_PAGEDOWN:
          consume();
          if (hasMore()) {
            nextPage();
          } else {
            getSelector().selectLast();
          }
          break;

        case KeyCodes.KEY_PAGEUP:
          consume();
          if (getOffset() > 0) {
            prevPage();
          } else {
            getSelector().selectFirst();
          }
          break;

        case KeyCodes.KEY_ESCAPE:
          consume();
          if (getSelector().isShowing()) {
            event.stopPropagation();
            if (getSelector().isItemSelected()) {
              getSelector().cancelSelection();
            } else {
              getSelector().hide();
            }

          } else {
            exit(true, State.CANCELED);
          }
          break;

        case KeyCodes.KEY_ENTER:
          consume();
          if (getSelector().isShowing()) {
            boolean selectorConsumed = getSelector().handleKeyboardSelection(hasModifiers);
            if (!selectorConsumed && !isInstant() && queryChanged()) {
              getSelector().hide();
              askOracle();
            }

          } else if (!isWaiting() && !BeeUtils.isEmpty(getDisplayValue()) && isQueryValid()
              && (isInstant() || hasModifiers || !queryChanged())) {

            if (isNewRowEnabled()) {
              RowFactory.createRelatedRow(DataSelector.this, getDisplayValue());
            } else if (!isStrict()) {
              setSelection(null, parse(getDisplayValue()), true);
            } else {
              askOracle();
            }

          } else {
            askOracle();
          }
          break;

        case KeyCodes.KEY_TAB:
          consume();

          if (!isStrict() && !getSelector().isShowing() && !isWaiting()
              && !BeeUtils.equalsTrim(getValue(), getDisplayValue())) {

            setSelection(null, parse(getDisplayValue()), true);
          } else {
            exit(true, State.CLOSED, keyCode, hasModifiers);
          }
          break;

        default:
          setConsumed(false);
      }
    }

    private void onKeyUp() {
      if (!isEnabled() || isConsumed() || !isActive()) {
        return;
      }

      boolean changed = queryChanged();

      if (isQueryValid()) {
        consume();
        askOrSchedule();

      } else {
        setFound(true);
        setLastRequest(null);
      }

      if (!isConsumed() && changed && getSelector().isShowing()) {
        getSelector().hide();
      }
    }

    private void setLastEventType(int lastEventType) {
      this.lastEventType = lastEventType;
    }

    private boolean wasKeyPressed() {
      return lastEventType == Event.ONKEYDOWN || lastEventType == Event.ONKEYPRESS;
    }
  }

  /**
   * Handles suggestion display.
   */

  private final class Selector {

    private final MenuBar menu;
    private final Popup popup;

    private MenuItem itemPrev;
    private MenuItem itemNext;

    private Boolean pendingSelection;

    private Selector(ItemType itemType, Element partner, String selectorClass) {
      this.menu = new MenuBar(MenuConstants.ROOT_MENU_INDEX, true, BarType.TABLE, itemType,
          STYLE_MENU);

      this.popup = new Popup(OutsideClick.CLOSE, STYLE_POPUP);
      if (!BeeUtils.isEmpty(selectorClass)) {
        popup.addStyleName(selectorClass);
      }

      popup.setWidget(menu);

      popup.setKeyboardPartner(partner);

      popup.addCloseHandler(event -> {
        if (event.userCaused()) {
          getMenu().clearItems();
          exit(false, State.CANCELED);
        }
      });
    }

    private void cancelSelection() {
      getMenu().selectItem(null);
    }

    private MenuItem createNavigationItem(boolean next) {
      Scheduler.ScheduledCommand command;
      if (next) {
        command = DataSelector.this::nextPage;
      } else {
        command = DataSelector.this::prevPage;
      }

      MenuItem item = new MenuItem(menu, next ? ITEM_NEXT : ITEM_PREV, ItemType.LABEL, command);
      item.addStyleName(STYLE_NAVIGATION);

      return item;
    }

    private MenuItem getItemNext() {
      return itemNext;
    }

    private MenuItem getItemPrev() {
      return itemPrev;
    }

    private MenuBar getMenu() {
      return menu;
    }

    private Boolean getPendingSelection() {
      return pendingSelection;
    }

    private Popup getPopup() {
      return popup;
    }

    private boolean handleKeyboardSelection(boolean hasModifiers) {
      MenuItem item = getMenu().getSelectedItem();
      if (item == null && (hasModifiers
          || (isInstant() || !queryChanged()) && getMenu().getItemCount() == 1)) {
        for (MenuItem it : getMenu().getItems()) {
          if (!isNavigationItem(it)) {
            item = it;
            break;
          }
        }
      }

      if (item == null || item.getCommand() == null) {
        return false;
      }

      if (isNavigationItem(item)) {
        setPendingSelection(item == getItemPrev());
      }
      item.getCommand().execute();
      return true;
    }

    private void hide() {
      getPopup().close();
    }

    private void initNavigationItem(MenuItem item) {
      addClassToCell(item, STYLE_NAVIGATION_CELL);
      if (isTableMode() && getColumnCount() > 1) {
        DomUtils.setColSpan(DomUtils.getParentCell(item, true), getColumnCount());
      }
    }

    private boolean isItemSelected() {
      if (isShowing()) {
        return getMenu().getSelectedItem() != null;
      } else {
        return false;
      }
    }

    private boolean isNavigationItem(MenuItem item) {
      return item == getItemPrev() || item == getItemNext();
    }

    private boolean isShowing() {
      return getPopup().isShowing();
    }

    private void moveSelectionDown() {
      if (isShowing()) {
        getMenu().moveDown();
      }
    }

    private void moveSelectionUp() {
      if (isShowing()) {
        getMenu().moveUp();
      }
    }

    private void selectFirst() {
      if (isShowing()) {
        getMenu().selectFirstItem();
      }
    }

    private void selectLast() {
      if (isShowing()) {
        getMenu().selectLastItem();
      }
    }

    private void setItemNext(MenuItem itemNext) {
      this.itemNext = itemNext;
    }

    private void setItemPrev(MenuItem itemPrev) {
      this.itemPrev = itemPrev;
    }

    private void setPendingSelection(Boolean pendingSelection) {
      this.pendingSelection = pendingSelection;
    }

    private void showSuggestions(Response response, UIObject target) {
      Collection<Suggestion> suggestions = response.getSuggestions();
      if (BeeUtils.isEmpty(suggestions)) {
        getPopup().close();
        return;
      }

      getMenu().clearItems();

      if (getOffset() > 0) {
        if (getItemPrev() == null) {
          setItemPrev(createNavigationItem(false));
        }
        getMenu().addItem(getItemPrev());
        initNavigationItem(getItemPrev());
      }

      for (Suggestion suggestion : suggestions) {
        addItem(getMenu(), suggestion.getRow());
      }

      if (response.hasMoreSuggestions()) {
        if (getItemNext() == null) {
          setItemNext(createNavigationItem(true));
        }
        getMenu().addItem(getItemNext());
        initNavigationItem(getItemNext());
      }

      if (getPendingSelection() != null) {
        MenuItem item = getPendingSelection() ? getItemNext() : getItemPrev();
        if (item != null) {
          getMenu().selectItem(item);
        }
        setPendingSelection(null);
      }

      getPopup().showRelativeTo(target.getElement());
    }
  }

  public static final char SHOW_SELECTOR = '*';
  public static final char ASK_ORACLE = BeeConst.CHAR_EOL;
  private static final char CREATE_NEW = '+';

  private static final String ITEM_PREV = String.valueOf('\u25b2');
  private static final String ITEM_NEXT = String.valueOf('\u25bc');

  private static final String STYLE_SELECTOR = BeeConst.CSS_CLASS_PREFIX + "DataSelector";

  private static final String STYLE_EMBEDDED = STYLE_SELECTOR + "-embedded";
  private static final String STYLE_STRICT = STYLE_SELECTOR + "-strict";

  private static final String STYLE_WAITING = STYLE_SELECTOR + "-waiting";
  private static final String STYLE_NOT_FOUND = STYLE_SELECTOR + "-notFound";
  private static final String STYLE_EMPTY = STYLE_SELECTOR + "-empty";

  private static final String STYLE_POPUP = STYLE_SELECTOR + "-popup";
  private static final String STYLE_MENU = STYLE_SELECTOR + "-menu";

  private static final String STYLE_TABLE = STYLE_SELECTOR + "-table";
  private static final String STYLE_ROW = STYLE_SELECTOR + "-row";
  private static final String STYLE_CELL = STYLE_SELECTOR + "-cell";

  private static final String STYLE_CONTENT = STYLE_SELECTOR + "-content";
  private static final String STYLE_ITEM = STYLE_SELECTOR + "-item";

  private static final String STYLE_NAVIGATION = STYLE_SELECTOR + "-navigation";
  private static final String STYLE_NAVIGATION_CELL = STYLE_SELECTOR + "-navigationCell";

  public static final String STYLE_EDITABLE_CONTAINER = STYLE_SELECTOR + "-editableContainer";
  private static final String STYLE_EDITABLE_ACTIVE = STYLE_SELECTOR + "-editable-active";
  private static final String STYLE_EDITABLE_INPUT = STYLE_SELECTOR + "-editableInput";

  private static final String STYLE_DRILL = STYLE_SELECTOR + "-drill";
  private static final String STYLE_DRILL_DISABLED = STYLE_DRILL + "-disabled";

  private static final int DEFAULT_MAX_INPUT_LENGTH = 30;
  private static final int DEFAULT_VISIBLE_LINES = 10;

  private static final int DEFAULT_MAX_ROW_COUNT_FOR_INSTANT_SEARCH = 1_000;
  private static final int DEFAULT_INPUT_DELAY_MILLIS = 500;

  private static final List<Integer> inputDelayMillis = new ArrayList<>();

  static boolean determineInstantSearch(Relation relation, int dataSize) {
    if (relation.getInstant() != null) {
      return relation.getInstant();
    }

    Operator operator = relation.nvlOperator();
    if (!EnumUtils.in(operator, Operator.STARTS, Operator.CONTAINS)) {
      return false;
    }

    Integer max = Settings.getDataSelectorInstantSearchMaxRows();
    if (max == null) {
      max = DEFAULT_MAX_ROW_COUNT_FOR_INSTANT_SEARCH;
    }

    return dataSize <= max;
  }

  private static void addClassToCell(MenuItem item, String className) {
    TableCellElement cell = DomUtils.getParentCell(item, true);
    if (cell != null) {
      cell.addClassName(className);
    }
  }

  private final Callback callback = (request, response) -> {
    DataSelector.this.getInput().removeStyleName(STYLE_WAITING);

    boolean found = !response.isEmpty();
    DataSelector.this.setFound(found);
    if (request.isEmpty()) {
      DataSelector.this.setAlive(found);
    }

    if (isEditing()) {
      setHasMore(response.hasMoreSuggestions());
      getSelector().showSuggestions(response, DataSelector.this);
    }
    DataSelector.this.setWaiting(false);
  };

  private int visibleLines = DEFAULT_VISIBLE_LINES;
  private final SelectionOracle oracle;

  private final int minQueryLength;

  private final InputWidget input;
  private final Selector selector;

  private final List<String> choiceColumns = new ArrayList<>();
  private final Map<Integer, SelectorColumn> selectorColumns = new HashMap<>();

  private final boolean tableMode;
  private final AbstractCellRenderer rowRenderer;

  private final Map<Integer, AbstractCellRenderer> cellRenderers = new HashMap<>();

  private final InputEvents inputEvents = new InputEvents();

  private final boolean embedded;

  private final String newRowForm;
  private final String newRowColumns;
  private final String newRowCaption;

  private final boolean newRowEnabled;

  private final String editViewName;
  private final String editForm;
  private final Boolean editModal;

  private final boolean editEnabled;

  private final int editTargetIndex;
  private final int editSourceIndex;

  private final String relationLabel;

  private final ValueType valueType;
  private final int valueSourceIndex;
  private final boolean strict;

  private boolean instant;
  private Timer inputTimer;

  private Long editRowId;

  private boolean active;
  private BeeRow relatedRow;

  private Value editorValue;
  private Request lastRequest;
  private int offset;

  private boolean hasMore;
  private boolean alive = true;
  private boolean waiting;

  private boolean adding;

  private Widget drill;

  private String options;

  private boolean handlesTabulation;

  private boolean summarize;

  private State initialState;

  public DataSelector(final Relation relation, boolean embedded) {
    super();

    this.embedded = embedded;

    DataInfo dataInfo = Data.getDataInfo(relation.getViewName());
    this.oracle = new SelectionOracle(relation, dataInfo);

    this.minQueryLength = BeeUtils.unbox(relation.getMinQueryLength());

    ItemType itemType = relation.getItemType();

    this.input = new InputWidget();
    this.selector = new Selector(itemType, this.input.getElement(), relation.getSelectorClass());

    this.choiceColumns.addAll(relation.getChoiceColumns());
    for (SelectorColumn selectorColumn : relation.getSelectorColumns()) {
      int index = this.choiceColumns.indexOf(selectorColumn.getSource());
      if (index >= 0) {
        this.selectorColumns.put(index, selectorColumn);
      }
    }
    int size = choiceColumns.size();

    this.tableMode = ItemType.ROW.equals(itemType) || !selectorColumns.isEmpty()
        || itemType == null && size > 1 && !relation.hasRowRenderer();

    BeeColumn dataColumn = null;
    CellSource cellSource = null;

    if (size == 1) {
      int dataIndex = dataInfo.getColumnIndex(choiceColumns.get(0));

      if (BeeConst.isUndef(dataIndex)) {
        cellSource = CellSource.forProperty(choiceColumns.get(0), null, ValueType.TEXT);
      } else {
        dataColumn = dataInfo.getColumns().get(dataIndex);
        cellSource = CellSource.forColumn(dataColumn, dataIndex);
      }
    }

    this.rowRenderer = RendererFactory.getRenderer(relation.getRowRendererDescription(),
        relation.getRowRender(), relation.getRowRenderTokens(), null,
        choiceColumns, dataInfo.getColumns(), cellSource);

    oracle.addRowCountChangeHandler(parameter -> setAlive(parameter > 0));
    oracle.addRowDeleteHandler(this::onRowDelete);

    oracle.addDataReceivedHandler(rowSet -> {
      if (!DataUtils.isEmpty(rowSet)) {
        SelectorEvent.fire(DataSelector.this, State.LOADED);
      }
    });

    if (tableMode) {
      initCellRenderers(dataInfo);
      getSelector().getMenu().addStyleName(STYLE_TABLE);
    }

    if (BeeUtils.isPositive(relation.getVisibleLines())) {
      setVisibleLines(relation.getVisibleLines());
    }

    this.newRowForm = BeeUtils.notEmpty(relation.getNewRowForm(), dataInfo.getNewRowForm());
    this.newRowColumns = BeeUtils.notEmpty(relation.getNewRowColumns(),
        dataInfo.getNewRowColumns());
    this.newRowCaption = BeeUtils.notEmpty(relation.getNewRowCaption(),
        dataInfo.getNewRowCaption());
    this.newRowEnabled = relation.isNewRowEnabled() && Data.isViewEditable(relation.getViewName())
        && BeeKeeper.getUser().canCreateData(relation.getViewName());

    if (relation.isEditEnabled(true)) {
      String es = relation.getEditSource();
      String ev = relation.getEditViewName();

      String targetViewName = relation.getTargetViewName();
      DataInfo editDataInfo;

      if (!BeeUtils.isEmpty(es) && !BeeUtils.isEmpty(targetViewName)) {
        editDataInfo = Data.getDataInfo(targetViewName);
      } else {
        editDataInfo = dataInfo;
      }

      if (BeeUtils.isEmpty(ev)) {
        if (BeeUtils.isEmpty(es)) {
          ev = relation.getViewName();
        } else if (editDataInfo != null) {
          ev = editDataInfo.getRelation(es);
        }
      }

      this.editViewName = ev;

      String ef = relation.getEditForm();

      if (BeeUtils.isEmpty(ef) && !BeeUtils.isEmpty(ev)) {
        if (BeeUtils.same(ev, dataInfo.getViewName())) {
          ef = RowEditor.getFormName(null, dataInfo);
        } else {
          ef = RowEditor.getFormName(null, Data.getDataInfo(ev));
        }
      }

      this.editForm = ef;

      this.editModal = WindowType.MODAL == relation.getEditWindowType();
      this.editEnabled = !BeeUtils.isEmpty(ev) && !BeeUtils.isEmpty(ef)
          && Data.isViewVisible(ev);

      int etIndex = BeeConst.UNDEF;
      int esIndex = BeeConst.UNDEF;

      if (!BeeUtils.isEmpty(es) && editDataInfo != null) {
        if (BeeUtils.isEmpty(targetViewName)) {
          esIndex = editDataInfo.getColumnIndex(es);

        } else {
          etIndex = editDataInfo.getColumnIndex(es);

          ViewColumn vc = editDataInfo.getViewColumn(es);
          if (vc != null) {
            esIndex = dataInfo.getColumnIndexBySource(vc.getTable(), vc.getField(),
                ViewColumn.VISIBLE);
          }
        }
      }

      this.editTargetIndex = etIndex;
      this.editSourceIndex = esIndex;

    } else {
      this.editViewName = null;

      this.editForm = null;
      this.editModal = null;
      this.editEnabled = false;

      this.editTargetIndex = BeeConst.UNDEF;
      this.editSourceIndex = BeeConst.UNDEF;
    }

    this.relationLabel = Localized.maybeTranslate(relation.getLabel());

    if (BeeUtils.isEmpty(relation.getValueSource())) {
      this.valueSourceIndex = BeeConst.UNDEF;
    } else {
      this.valueSourceIndex = dataInfo.getColumnIndex(relation.getValueSource());
    }

    if (BeeConst.isUndef(valueSourceIndex)) {
      this.valueType = ValueType.LONG;
      this.strict = true;
    } else {
      this.valueType = dataInfo.getColumnType(valueSourceIndex);
      this.strict = BeeUtils.isTrue(relation.getStrict());
    }

    Binder.addMouseWheelHandler(selector.getPopup(), inputEvents);

    int maxLen = 0;

    if (dataColumn != null && dataColumn.isCharacter()) {
      maxLen = dataColumn.getPrecision();

    } else if (size > 1) {
      for (String colName : choiceColumns) {
        BeeColumn column = dataInfo.getColumn(colName);
        if (column != null && column.isCharacter()) {
          maxLen = Math.max(maxLen, column.getPrecision());
        }
      }
    }

    input.setMaxLength(BeeUtils.positive(maxLen, DEFAULT_MAX_INPUT_LENGTH));

    init(input, embedded);

    if (strict) {
      input.addStyleName(STYLE_STRICT);
    }

    Data.estimateSize(relation.getViewName(), dataSize -> {
      setInstant(determineInstantSearch(relation, dataSize));
      oracle.init(relation, dataSize);

      State state = getInitialState();
      setInitialState(State.INITIALIZED);

      if (state == State.PENDING) {
        askOracle();
      }

      if (!isInstant()) {
        initInputTimer();
      }
    });
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return getInput().addDomHandler(handler, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    return addKeyDownHandler(handler);
  }

  @Override
  public HandlerRegistration addEditStopHandler(EditStopEvent.Handler handler) {
    return addHandler(handler, EditStopEvent.getType());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return getInput().addDomHandler(handler, FocusEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return getInput().addDomHandler(handler, KeyDownEvent.getType());
  }

  public HandlerRegistration addSelectorHandler(SelectorEvent.Handler handler) {
    return addHandler(handler, SelectorEvent.getType());
  }

  @Override
  public HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  public void clearDisplay() {
    setDisplayValue(BeeConst.STRING_EMPTY);
  }

  @Override
  public void clearValue() {
    setValue(null);
    clearDisplay();
  }

  public List<String> getChoiceColumns() {
    return choiceColumns;
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return EditorAction.SELECT;
  }

  public String getDisplayValue() {
    return getInput().getText();
  }

  public Widget getDrill() {
    return drill;
  }

  public String getEditForm() {
    return editForm;
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "selector";
  }

  @Override
  public int getMaxLength() {
    return getInput().getMaxLength();
  }

  public String getNewRowCaption() {
    return newRowCaption;
  }

  public String getNewRowColumns() {
    return newRowColumns;
  }

  public String getNewRowForm() {
    return newRowForm;
  }

  @Override
  public String getNormalizedValue() {
    return getValue();
  }

  @Override
  public String getOptions() {
    return options;
  }

  public SelectionOracle getOracle() {
    return oracle;
  }

  @Override
  public Long getRelatedId() {
    return (getRelatedRow() == null) ? null : getRelatedRow().getId();
  }

  @Override
  public BeeRow getRelatedRow() {
    return relatedRow;
  }

  @Override
  public Value getSummary() {
    return BooleanValue.of(!BeeUtils.isEmpty(getValue()));
  }

  @Override
  public int getTabIndex() {
    return getInput().getTabIndex();
  }

  @Override
  public TextBox getTextBox() {
    return getInput();
  }

  @Override
  public String getValue() {
    return (getEditorValue() == null) ? null : getEditorValue().getString();
  }

  @Override
  public int getVisibleLines() {
    return visibleLines;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.DATA_SELECTOR;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return isActive();
  }

  @Override
  public boolean handlesTabulation() {
    return handlesTabulation;
  }

  public boolean hasRelatedView(String viewName) {
    return !BeeUtils.isEmpty(viewName) && BeeUtils.same(viewName, getOracle().getViewName());
  }

  public boolean isAdding() {
    return adding;
  }

  public boolean isEditEnabled() {
    return editEnabled;
  }

  @Override
  public boolean isEditing() {
    return getInput().isEditing();
  }

  public Boolean isEditModal() {
    return BeeUtils.isTrue(editModal) || UiHelper.isModal(getWidget());
  }

  public boolean isEmbedded() {
    return embedded;
  }

  @Override
  public boolean isEnabled() {
    return getInput().isEnabled();
  }

  public boolean isNewRowEnabled() {
    return newRowEnabled;
  }

  @Override
  public boolean isNullable() {
    return getInput().isNullable();
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return getInput().isOrHasPartner(node)
        || getSelector().getPopup().getElement().isOrHasChild(node);
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  public void onRefresh(IsRow targetRow) {
    if (!BeeConst.isUndef(getEditTargetIndex())) {
      if (targetRow == null) {
        setEditRowId(null);
      } else {
        setEditRowId(targetRow.getLong(getEditTargetIndex()));
      }
    }
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  @Override
  public void setAccessKey(char key) {
    getInput().setAccessKey(key);
  }

  public void setAdding(boolean adding) {
    this.adding = adding;
  }

  public void setAdditionalFilter(Filter additionalFilter) {
    setAdditionalFilter(additionalFilter, false);
  }

  public void setAdditionalFilter(Filter additionalFilter, boolean force) {
    if (getOracle().setAdditionalFilter(additionalFilter, force)) {
      setAlive(true);
    }
  }

  public void setDisplayValue(String value) {
    getInput().setValue(value);
  }

  @Override
  public void setEditing(boolean editing) {
    getInput().setEditing(editing);
  }

  @Override
  public void setEnabled(boolean enabled) {
    getInput().setEnabled(enabled);
  }

  @Override
  public void setFocus(boolean focused) {
    getInput().setFocus(focused);
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
    this.handlesTabulation = handlesTabulation;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setMaxLength(int maxLength) {
    getInput().setMaxLength(maxLength);
  }

  @Override
  public void setNullable(boolean nullable) {
    getInput().setNullable(nullable);
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setSelection(BeeRow row, Value value, boolean fire) {
    setRelatedRow(row);

    if (row == null) {
      setEditorValue(value);
    } else {
      setEditorValue(getRowValue(row));
    }

    if (!BeeConst.isUndef(getEditSourceIndex())) {
      if (row == null) {
        setEditRowId(null);
      } else {
        setEditRowId(row.getLong(getEditSourceIndex()));
      }
    }

    hideSelector();
    reset();

    if (fire) {
      SelectorEvent.fire(this, State.CHANGE_PENDING);
      fireEvent(new EditStopEvent(State.CHANGED, KeyCodes.KEY_TAB, false));
      SelectorEvent.fire(this, State.CHANGED);
    }
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public void setTabIndex(int index) {
    getInput().setTabIndex(index);
  }

  @Override
  public void setUpperCase(boolean upperCase) {
    getInput().setUpperCase(upperCase);
  }

  @Override
  public void setValue(String value) {
    setEditorValue(parse(value));
  }

  @Override
  public void setVisibleLines(int lines) {
    this.visibleLines = lines;
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {

    SelectorEvent.fire(this, State.OPEN);

    setRelatedRow(null);
    if (!isEmbedded()) {
      setEditorValue(parse(oldValue));
    }

    setLastRequest(null);
    setOffset(0);
    setHasMore(false);

    boolean createNew = false;

    if (charCode != BeeConst.CHAR_SPACE && Codec.isValidUnicodeChar(charCode)) {
      if (charCode == SHOW_SELECTOR) {
        clearDisplay();
        askOrSchedule();

      } else if (charCode == CREATE_NEW && isNewRowEnabled()) {
        if (!isEmbedded() && sourceElement != null) {
          updateDisplay(sourceElement.getInnerText());
        }
        createNew = true;

      } else {
        setDisplayValue(BeeUtils.toString(charCode));
        if (isQueryValid()) {
          askOrSchedule();
        }
      }

    } else if (!isEmbedded()) {
      String text = (sourceElement == null) ? null : sourceElement.getInnerText();
      if (BeeUtils.isEmpty(text)) {
        clearDisplay();
      } else {
        setDisplayValue(text.trim());
        getInput().selectAll();
        setWaiting(true);
      }

    } else if (charCode == ASK_ORACLE) {
      askOrSchedule();
    }

    inputEvents.consume();
    setActive(true);

    if (createNew) {
      RowFactory.createRelatedRow(this, null);
    }
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return Collections.emptyList();
  }

  protected boolean consumeCharacter(char ch) {
    return false;
  }

  protected void exit(boolean hideSelector, State state, Integer keyCode, boolean hasModifiers) {
    if (hideSelector) {
      hideSelector();
    }
    reset();

    SelectorEvent.fire(this, state);
    fireEvent(new EditStopEvent(state, keyCode, hasModifiers));
  }

  protected int getEditSourceIndex() {
    return editSourceIndex;
  }

  protected String getEditViewName() {
    return editViewName;
  }

  protected InputWidget getInput() {
    return input;
  }

  protected String getRelationLabel() {
    return relationLabel;
  }

  protected Value getRowValue(IsRow row) {
    if (row == null) {
      return null;
    }
    if (BeeConst.isUndef(valueSourceIndex)) {
      return new LongValue(row.getId());
    } else {
      return parse(row.getString(valueSourceIndex));
    }
  }

  protected ValueType getValueType() {
    return valueType;
  }

  protected boolean hasValueSource() {
    return !BeeConst.isUndef(valueSourceIndex);
  }

  protected void hideSelector() {
    getSelector().hide();
  }

  protected void init(InputWidget inputWidget, boolean embed) {
    inputWidget.addStyleName(STYLE_SELECTOR);
    if (embed) {
      inputWidget.addStyleName(STYLE_EMBEDDED);
    }

    if (embed && isEditEnabled()) {
      final Flow container = new Flow();
      container.addStyleName(STYLE_EDITABLE_CONTAINER);

      inputWidget.addStyleName(STYLE_EDITABLE_INPUT);

      inputWidget.addFocusHandler(event -> container.addStyleName(STYLE_EDITABLE_ACTIVE));
      inputWidget.addBlurHandler(event -> container.removeStyleName(STYLE_EDITABLE_ACTIVE));

      container.add(inputWidget);

      InlineLabel label = new InlineLabel(String.valueOf(BeeConst.DRILL_DOWN));
      label.addStyleName(STYLE_DRILL);
      label.addStyleName(STYLE_DRILL_DISABLED);

      label.addClickHandler(event -> editRow());

      setDrill(label);
      container.add(label);

      initWidget(container);

    } else {
      initWidget(inputWidget);
    }

    SelectorEvent.fire(this, State.INITIALIZED);
  }

  protected boolean isActive() {
    return active;
  }

  protected boolean isStrict() {
    return strict;
  }

  protected void onRowDelete(long id) {
    if (!hasValueSource() && Objects.equals(getEditorValueAsId(), id)) {
      clearValue();
    }
  }

  @Override
  protected void onUnload() {
    cancelInputTimer();

    SelectorEvent.fire(this, State.UNLOADING);

    getOracle().onUnload();
    reset();

    super.onUnload();
  }

  protected Value parse(String value) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    } else {
      return Value.parseValue(valueType, value, false, null);
    }
  }

  protected void reset() {
    cancelInputTimer();

    setActive(false);
    setWaiting(false);

    setLastRequest(null);
    setOffset(0);
    setHasMore(false);

    getInput().removeStyleName(STYLE_WAITING);
    getInput().removeStyleName(STYLE_NOT_FOUND);

    if (getDrill() != null) {
      getWidget().removeStyleName(STYLE_EDITABLE_ACTIVE);
    }
  }

  protected void setActive(boolean active) {
    this.active = active;
  }

  protected void setRelatedRow(BeeRow relatedRow) {
    this.relatedRow = relatedRow;
  }

  protected void updateDisplay(String value) {
    if (BeeUtils.isEmpty(value)) {
      clearDisplay();
    } else {
      setDisplayValue(value.trim());
    }
  }

  private void addCells(Element rowElement, BeeRow row) {
    for (int i = 0; i < getColumnCount(); i++) {
      String cellContent = getCellRenderers().get(i).render(row);
      SelectorColumn selectorColumn = getSelectorColumns().get(i);

      TableCellElement cellElement = DomUtils.createTableCell();
      cellElement.addClassName(STYLE_CELL);

      Element contentElement = DomUtils.createSpan(cellContent);
      contentElement.addClassName(STYLE_CONTENT);

      if (selectorColumn != null) {
        StyleUtils.updateAppearance(contentElement, selectorColumn.getClasses(),
            selectorColumn.getStyle());

        UiHelper.setHorizontalAlignment(cellElement, selectorColumn.getHorAlign());
        UiHelper.setVerticalAlignment(cellElement, selectorColumn.getVertAlign());
      }

      cellElement.appendChild(contentElement);
      rowElement.appendChild(cellElement);
    }
  }

  private void addItem(MenuBar menu, final BeeRow row) {
    Scheduler.ScheduledCommand menuCommand = () -> setSelection(row, null, true);

    MenuItem item;
    if (isTableMode()) {
      item = new MenuItem(menu, null, ItemType.ROW, menuCommand);
      addCells(item.getElement(), row);
    } else {
      item = new MenuItem(menu, renderItem(row), menuCommand);
    }

    menu.addItem(item);

    item.addStyleName(STYLE_ITEM);
    if (isTableMode()) {
      item.addStyleName(STYLE_ROW);
    } else {
      addClassToCell(item, STYLE_CELL);
    }
  }

  private void askOracle() {
    if (getInitialState() == State.INITIALIZED) {
      String query = BeeUtils.trim(getDisplayValue());
      int start = getOffset();
      int size = getVisibleLines();

      if (getLastRequest() != null && !Objects.equals(query, getLastRequest().getQuery())) {
        start = 0;
        setOffset(start);
      }

      Request request = new Request(query, start, size);
      setLastRequest(request);

      getInput().addStyleName(STYLE_WAITING);
      setWaiting(true);

      SelectorEvent event = SelectorEvent.fireRequest(this, request, getCallback());
      if (!event.isConsumed()) {
        getOracle().requestSuggestions(request, getCallback());
      }

    } else {
      setInitialState(State.PENDING);
    }
  }

  private void askOrSchedule() {
    if (isInstant()) {
      askOracle();
    } else {
      scheduleRequest();
    }
  }

  private void cancelInputTimer() {
    if (getInputTimer() != null) {
      getInputTimer().cancel();
    }
  }

  private void editRow() {
    Long rowId;

    if (BeeConst.isUndef(getEditTargetIndex())) {
      rowId = getEditorValueAsId();
    } else {
      rowId = getEditRowId();
    }

    if (!DataUtils.isId(rowId)) {
      return;
    }

    boolean modal = isEditModal();
    RowCallback rowCallback;

    if (modal) {
      rowCallback = result -> {
        if (BeeUtils.same(getEditViewName(), getOracle().getViewName())) {
          setRelatedRow(result);

        } else {
          BeeRow row = getRelatedRow();
          Long id = getEditorValueAsId();

          if (row == null && DataUtils.isId(id)) {
            row = getOracle().getCachedRow(id);
          }

          if (row != null && !BeeConst.isUndef(getEditSourceIndex())) {
            RelationUtils.updateRow(getOracle().getDataInfo(),
                getOracle().getDataInfo().getColumnId(getEditSourceIndex()), row,
                Data.getDataInfo(getEditViewName()), result, false);
            setRelatedRow(row);
          }
        }

        if (getRelatedRow() != null) {
          fireEvent(new EditStopEvent(State.EDITED));
        }
      };

    } else {
      rowCallback = null;
    }

    Opener opener = modal ? Opener.relativeTo(getWidget()) : Opener.NEW_TAB;
    RowEditor.openForm(getEditForm(), Data.getDataInfo(getEditViewName()), Filter.compareId(rowId),
        opener, rowCallback);
  }

  private void exit(boolean hideSelector, State state) {
    exit(hideSelector, state, null, false);
  }

  private Callback getCallback() {
    return callback;
  }

  private Map<Integer, AbstractCellRenderer> getCellRenderers() {
    return cellRenderers;
  }

  private int getColumnCount() {
    return getChoiceColumns().size();
  }

  private Value getEditorValue() {
    return editorValue;
  }

  private Long getEditorValueAsId() {
    if (getEditorValue() != null && getEditorValue().getType() == ValueType.LONG) {
      return getEditorValue().getLong();
    } else {
      return null;
    }
  }

  private Long getEditRowId() {
    return editRowId;
  }

  private int getEditTargetIndex() {
    return editTargetIndex;
  }

  private State getInitialState() {
    return initialState;
  }

  private Timer getInputTimer() {
    return inputTimer;
  }

  private Request getLastRequest() {
    return lastRequest;
  }

  private int getMinQueryLength() {
    return minQueryLength;
  }

  private int getOffset() {
    return offset;
  }

  private AbstractCellRenderer getRowRenderer() {
    return rowRenderer;
  }

  private Selector getSelector() {
    return selector;
  }

  private Map<Integer, SelectorColumn> getSelectorColumns() {
    return selectorColumns;
  }

  private boolean hasMore() {
    return hasMore;
  }

  private void initCellRenderers(DataInfo dataInfo) {
    if (!getCellRenderers().isEmpty()) {
      getCellRenderers().clear();
    }

    for (Map.Entry<Integer, SelectorColumn> entry : getSelectorColumns().entrySet()) {
      SelectorColumn sc = entry.getValue();
      int index = dataInfo.getColumnIndex(sc.getSource());

      CellSource cellSource = BeeConst.isUndef(index)
          ? null : CellSource.forColumn(dataInfo.getColumns().get(index), index);

      AbstractCellRenderer renderer = RendererFactory.getRenderer(sc.getRendererDescription(),
          sc.getRender(), sc.getRenderTokens(), sc.getEnumKey(), sc.getRenderColumns(),
          dataInfo.getColumns(), cellSource);

      if (renderer != null) {
        getCellRenderers().put(entry.getKey(), renderer);
      }
    }

    if (getCellRenderers().isEmpty() && getColumnCount() == 1 && getRowRenderer() != null) {
      getCellRenderers().put(0, getRowRenderer());
    }

    for (int i = 0; i < getColumnCount(); i++) {
      if (!getCellRenderers().containsKey(i)) {
        int index = dataInfo.getColumnIndex(getChoiceColumns().get(i));

        CellSource cellSource;
        if (BeeConst.isUndef(index)) {
          cellSource = CellSource.forProperty(getChoiceColumns().get(i), null, ValueType.TEXT);
        } else {
          cellSource = CellSource.forColumn(dataInfo.getColumns().get(index), index);
        }

        AbstractCellRenderer renderer = new SimpleRenderer(cellSource);
        getCellRenderers().put(i, renderer);
      }
    }
  }

  private void initInputTimer() {
    this.inputTimer = new Timer() {
      @Override
      public void run() {
        if (!isWaiting() || queryChanged()) {
          askOracle();
        }
      }
    };

    if (inputDelayMillis.isEmpty()) {
      List<Integer> delayMillis = Settings.getDataSelectorInputDelayMillis();
      if (BeeUtils.isEmpty(delayMillis)) {
        inputDelayMillis.add(DEFAULT_INPUT_DELAY_MILLIS);
      } else {
        inputDelayMillis.addAll(delayMillis);
      }
    }
  }

  private boolean isAlive() {
    return alive;
  }

  private boolean isInstant() {
    return instant;
  }

  private boolean isQueryValid() {
    return BeeUtils.trim(getDisplayValue()).length() >= getMinQueryLength();
  }

  private boolean isTableMode() {
    return tableMode;
  }

  private boolean isWaiting() {
    return waiting;
  }

  private void nextOffset() {
    if (hasMore()) {
      setOffset(getOffset() + 1);
      askOracle();
    }
  }

  private void nextPage() {
    if (hasMore()) {
      setOffset(getOffset() + getVisibleLines());
      askOracle();
    }
  }

  private void prevOffset() {
    if (getOffset() > 0) {
      setOffset(getOffset() - 1);
      askOracle();
    }
  }

  private void prevPage() {
    if (getOffset() > 0) {
      setOffset(Math.max(getOffset() - getVisibleLines(), 0));
      askOracle();
    }
  }

  private boolean queryChanged() {
    String oldQuery = (getLastRequest() == null) ? null : getLastRequest().getQuery();
    return !BeeUtils.equalsTrim(oldQuery, getDisplayValue());
  }

  private String renderItem(BeeRow row) {
    return getRowRenderer().render(row);
  }

  private void scheduleRequest() {
    if (getInputTimer() != null) {
      int index = BeeUtils.length(BeeUtils.trim(getDisplayValue()));
      if (index >= inputDelayMillis.size()) {
        index = inputDelayMillis.size() - 1;
      }

      getInputTimer().schedule(inputDelayMillis.get(index));
    }
  }

  private void setAlive(boolean alive) {
    if (isAlive() != alive) {
      this.alive = alive;
      getInput().setStyleName(STYLE_EMPTY, !alive);
    }
  }

  private void setDrill(Widget drill) {
    this.drill = drill;
  }

  private void setEditorValue(Value ev) {
    if (getDrill() != null && (this.editorValue == null) != (ev == null)) {
      getDrill().setStyleName(STYLE_DRILL_DISABLED, ev == null);
    }
    this.editorValue = ev;
  }

  private void setEditRowId(Long editRowId) {
    this.editRowId = editRowId;
  }

  private void setFound(boolean found) {
    getInput().setStyleName(STYLE_NOT_FOUND, !found);
  }

  private void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  private void setInitialState(State initialState) {
    this.initialState = initialState;
  }

  private void setInstant(boolean instant) {
    this.instant = instant;
  }

  private void setLastRequest(Request lastRequest) {
    this.lastRequest = lastRequest;
  }

  private void setOffset(int offset) {
    this.offset = offset;
  }

  private void setWaiting(boolean waiting) {
    this.waiting = waiting;
  }

  private void start(int keyCode) {
    startEdit(null, BeeUtils.toChar(keyCode), null, null);
  }
}