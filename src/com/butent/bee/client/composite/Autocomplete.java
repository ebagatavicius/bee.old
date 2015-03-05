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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.data.Data;
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
import com.butent.bee.client.event.logical.AutocompleteEvent;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.menu.MenuBar;
import com.butent.bee.client.menu.MenuItem;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.HasTextBox;
import com.butent.bee.client.view.edit.TextBox;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Launchable;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.menu.MenuConstants;
import com.butent.bee.shared.menu.MenuConstants.ItemType;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasCapsLock;
import com.butent.bee.shared.ui.HasVisibleLines;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.Relation.Caching;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Autocomplete extends Composite implements Editor, HasVisibleLines, HasTextBox,
    HasCapsLock, HasKeyDownHandlers, Launchable {

  protected final class InputWidget extends InputText implements HasMouseWheelHandlers {

    private InputWidget() {
      super();
      addMouseWheelHandler(inputEvents);
      sinkEvents(Event.ONBLUR | Event.ONCLICK | Event.KEYEVENTS);
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
          if (showing) {
            return;
          } else {
            reset();
          }
          break;

        case Event.ONCLICK:
          if (isEmbedded() && !isActive()) {
            start(EditStartEvent.CLICK);
            return;
          }
          if (!getSelector().isShowing() && BeeUtils.isEmpty(getValue())) {
            askOracle();
          }
          break;

        case Event.ONKEYDOWN:
          if (isEmbedded() && event.getKeyCode() == KeyCodes.KEY_DELETE && isNullable()
              && !BeeUtils.isEmpty(getValue())) {
            setSelection(null);
            consumed = true;

          } else if (isEmbedded() && !isActive()) {
            if (event.getKeyCode() == KeyCodes.KEY_BACKSPACE
                && !BeeUtils.isEmpty(getValue())) {
              start(SHOW_SELECTOR);
              consumed = true;
            }

          } else {
            inputEvents.onKeyDown(event);
            consumed = inputEvents.isConsumed();
          }
          break;

        case Event.ONKEYPRESS:
          if (isEmbedded() && !isActive()) {
            int charCode = event.getCharCode();
            if (charCode > BeeConst.CHAR_SPACE
                && Codec.isValidUnicodeChar(BeeUtils.toChar(charCode))) {
              start(charCode);
              consumed = true;
            }
          } else {
            consumed = inputEvents.isConsumed();
          }
          break;

        case Event.ONKEYUP:
          inputEvents.onKeyUp(event);
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
  }

  private class InputEvents implements MouseWheelHandler, Consumable {

    private boolean consumed;

    @Override
    public void consume() {
      setConsumed(true);
    }

    @Override
    public boolean isConsumed() {
      return consumed;
    }

    @Override
    public void onMouseWheel(MouseWheelEvent event) {
      if (!isEnabled() || !isActive() || !getSelector().isShowing()) {
        return;
      }

      int y = event.getDeltaY();
      if (y > 0) {
        nextOffset();
      } else if (y < 0) {
        prevOffset();
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
          }
          if (getSelector().isItemSelected()) {
            getSelector().cancelSelection();
          } else {
            exit(true, State.CANCELED);
          }
          break;

        case KeyCodes.KEY_ENTER:
          consume();
          if (getSelector().isShowing()) {
            getSelector().handleKeyboardSelection(isInstant(), hasModifiers);
          } else if (BeeUtils.isEmpty(getValue())) {
            askOracle();
          } else if (!isWaiting()) {
            setSelection(getValue());
          }
          break;

        case KeyCodes.KEY_TAB:
          consume();
          exit(true, State.CLOSED, keyCode, hasModifiers);
          break;

        default:
          setConsumed(false);
      }
    }

    private void onKeyUp(Event event) {
      if (!isEnabled() || isConsumed() || !isActive()) {
        return;
      }
      int keyCode = event.getKeyCode();

      if (isInstant() || keyCode == KeyCodes.KEY_ENTER) {
        consume();
        askOracle();
      }
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

    private Selector(Element partner, String selectorClass) {
      this.menu = new MenuBar(MenuConstants.ROOT_MENU_INDEX, true);

      menu.addStyleName(STYLE_MENU);

      this.popup = new Popup(OutsideClick.CLOSE, STYLE_POPUP);
      if (!BeeUtils.isEmpty(selectorClass)) {
        popup.addStyleName(selectorClass);
      }

      popup.setWidget(menu);

      popup.setKeyboardPartner(partner);

      popup.addCloseHandler(new CloseEvent.Handler() {
        @Override
        public void onClose(CloseEvent event) {
          if (event.userCaused()) {
            getMenu().clearItems();
            exit(false, State.CANCELED);
          }
        }
      });
    }

    private void cancelSelection() {
      getMenu().selectItem(null);
    }

    private MenuItem createNavigationItem(boolean next) {
      Scheduler.ScheduledCommand command;
      if (next) {
        command = new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            nextPage();
          }
        };
      } else {
        command = new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            prevPage();
          }
        };
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

    private boolean handleKeyboardSelection(boolean instant, boolean hasModifiers) {
      if (!isShowing()) {
        return false;
      }
      if (!instant && !hasModifiers) {
        return false;
      }

      MenuItem item = getMenu().getSelectedItem();
      if (item == null && (hasModifiers || instant && getMenu().getItemCount() == 1)) {
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
        addItem(getMenu(), suggestion.getRow().getString(columnIdx));
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

  public static Autocomplete create(Relation relation, boolean embeded) {
    Assert.notNull(relation);

    relation.setCaching(Caching.LOCAL);
    return new Autocomplete(relation, embeded);
  }

  private static final String ITEM_PREV = String.valueOf('\u25b2');
  private static final String ITEM_NEXT = String.valueOf('\u25bc');

  private static final String STYLE_SELECTOR = BeeConst.CSS_CLASS_PREFIX + "DataSelector";

  private static final String STYLE_EMBEDDED = STYLE_SELECTOR + "-embedded";

  private static final String STYLE_WAITING = STYLE_SELECTOR + "-waiting";
  private static final String STYLE_EMPTY = STYLE_SELECTOR + "-empty";

  private static final String STYLE_POPUP = STYLE_SELECTOR + "-popup";
  private static final String STYLE_MENU = STYLE_SELECTOR + "-menu";

  private static final String STYLE_CELL = STYLE_SELECTOR + "-cell";

  private static final String STYLE_ITEM = STYLE_SELECTOR + "-item";

  private static final String STYLE_NAVIGATION = STYLE_SELECTOR + "-navigation";
  private static final String STYLE_NAVIGATION_CELL = STYLE_SELECTOR + "-navigationCell";

  private static final int DEFAULT_VISIBLE_LINES = 10;

  private static final Operator DEFAULT_SEARCH_TYPE = Operator.CONTAINS;

  private static final char SHOW_SELECTOR = '*';

  private final Callback callback = new Callback() {
    @Override
    public void onSuggestionsReady(Request request, Response response) {
      Autocomplete.this.getInput().removeStyleName(STYLE_WAITING);

      if (request.isEmpty()) {
        Autocomplete.this.setAlive(!response.isEmpty());
      }
      if (isEditing()) {
        setHasMore(response.hasMoreSuggestions());
        getSelector().showSuggestions(response, Autocomplete.this);
      }
      Autocomplete.this.setWaiting(false);
    }
  };

  private int visibleLines = DEFAULT_VISIBLE_LINES;

  private final SelectionOracle oracle;
  private final Operator searchType;

  private final InputWidget input;
  private final Selector selector;

  private final InputEvents inputEvents = new InputEvents();

  private final boolean embedded;
  private final int columnIdx;

  private boolean active;

  private Request lastRequest;
  private int offset;
  private boolean hasMore;

  private boolean alive = true;
  private boolean waiting;

  private String options;

  private boolean handlesTabulation;

  private boolean handledByForm;

  private boolean summarize;

  private Autocomplete(Relation relation, boolean embedded) {
    super();

    this.embedded = embedded;

    DataInfo dataInfo = Data.getDataInfo(relation.getViewName());
    this.columnIdx = dataInfo.getColumnIndex(relation.getSearchableColumns().get(0));

    this.oracle = new SelectionOracle(relation, dataInfo);
    this.searchType =
        (relation.getOperator() == null) ? DEFAULT_SEARCH_TYPE : relation.getOperator();

    this.input = new InputWidget();
    this.selector = new Selector(input.getElement(), relation.getSelectorClass());

    oracle.addRowCountChangeHandler(new Consumer<Integer>() {
      @Override
      public void accept(Integer parameter) {
        Autocomplete.this.setAlive(parameter > 0);
      }
    });
    oracle.addDataReceivedHandler(new Consumer<BeeRowSet>() {
      @Override
      public void accept(BeeRowSet rowSet) {
        if (!DataUtils.isEmpty(rowSet)) {
          AutocompleteEvent.fire(Autocomplete.this, State.LOADED);
        }
      }
    });
    if (BeeUtils.isPositive(relation.getVisibleLines())) {
      setVisibleLines(relation.getVisibleLines());
    }
    Binder.addMouseWheelHandler(selector.getPopup(), inputEvents);

    init(input, embedded);
  }

  public HandlerRegistration addAutocompleteHandler(AutocompleteEvent.Handler handler) {
    return addHandler(handler, AutocompleteEvent.getType());
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

  @Override
  public HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  @Override
  public void clearValue() {
    setValue(null);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return EditorAction.SELECT;
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
    return getInput().getText();
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

  @Override
  public boolean isEditing() {
    return getInput().isEditing();
  }

  public boolean isEmbedded() {
    return embedded;
  }

  @Override
  public boolean isEnabled() {
    return getInput().isEnabled();
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
  public void launch() {
    setHandledByForm(true);
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  @Override
  public void setAccessKey(char key) {
    getInput().setAccessKey(key);
  }

  public void setAdditionalFilter(Filter additionalFilter) {
    if (getOracle().setAdditionalFilter(additionalFilter, false)) {
      setAlive(true);
    }
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
  public void setNullable(boolean nullable) {
    getInput().setNullable(nullable);
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setSelection(String value) {
    setValue(BeeUtils.isEmpty(value) ? null : value);

    hideSelector();
    reset();

    fireEvent(new EditStopEvent(State.CHANGED, KeyCodes.KEY_TAB, false));
    AutocompleteEvent.fire(this, State.CHANGED);
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
    getInput().setValue(value);
  }

  @Override
  public void setVisibleLines(int lines) {
    this.visibleLines = lines;
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
    AutocompleteEvent.fire(this, State.OPEN);

    if (!isEmbedded()) {
      setValue(oldValue);
    }
    setLastRequest(null);
    setOffset(0);
    setHasMore(false);

    if (charCode != BeeConst.CHAR_SPACE && Codec.isValidUnicodeChar(charCode)) {
      if (charCode == SHOW_SELECTOR) {
        clearValue();
      } else {
        setValue(BeeUtils.toString(charCode));
      }
      if (isInstant()) {
        askOracle();
      }

    } else if (!isEmbedded()) {
      String text = (sourceElement == null) ? null : sourceElement.getInnerText();
      if (BeeUtils.isEmpty(text)) {
        clearValue();
      } else {
        setValue(text.trim());
        getInput().selectAll();
        setWaiting(true);
      }
    }
    inputEvents.consume();
    setActive(true);
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

  protected void exit(boolean hideSelector, State state, Integer keyCode, boolean hasModifiers) {
    if (hideSelector) {
      hideSelector();
    }
    reset();

    AutocompleteEvent.fire(this, state);
    fireEvent(new EditStopEvent(state, keyCode, hasModifiers));
  }

  protected InputWidget getInput() {
    return input;
  }

  protected void hideSelector() {
    getSelector().hide();
  }

  protected void init(InputWidget inputWidget, boolean embed) {
    inputWidget.addStyleName(STYLE_SELECTOR);
    if (embed) {
      inputWidget.addStyleName(STYLE_EMBEDDED);
    }
    initWidget(inputWidget);

    AutocompleteEvent.fire(this, State.INITIALIZED);
  }

  protected boolean isActive() {
    return active;
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (!isHandledByForm()) {
      addFocusHandler(new FocusHandler() {
        @Override
        public void onFocus(FocusEvent event) {
          setEditing(true);
        }
      });
    }

    addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if (!isHandledByForm()) {
          setEditing(false);
        }
      }
    });

    addEditStopHandler(new EditStopEvent.Handler() {
      @Override
      public void onEditStop(EditStopEvent event) {
        if (!isHandledByForm()) {
          UiHelper.moveFocus(getParent(), getElement(), true);
        }
      }
    });
  }

  @Override
  protected void onUnload() {
    AutocompleteEvent.fire(this, State.UNLOADING);
    getOracle().onUnload();
    super.onUnload();
  }

  protected void reset() {
    setActive(false);
    setWaiting(false);

    setLastRequest(null);
    setOffset(0);
    setHasMore(false);

    getInput().removeStyleName(STYLE_WAITING);
  }

  protected void setActive(boolean active) {
    this.active = active;
  }

  private static void addClassToCell(MenuItem item, String className) {
    TableCellElement cell = DomUtils.getParentCell(item, true);
    if (cell != null) {
      cell.addClassName(className);
    }
  }

  private void addItem(MenuBar menu, final String value) {
    Scheduler.ScheduledCommand menuCommand = new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        setSelection(value);
      }
    };
    MenuItem item = new MenuItem(menu, value, menuCommand);

    menu.addItem(item);

    item.addStyleName(STYLE_ITEM);

    addClassToCell(item, STYLE_CELL);
  }

  private void askOracle() {
    String query = BeeUtils.trim(getValue());
    Operator type = getSearchType();
    int start = getOffset();
    int size = getVisibleLines();

    if (getLastRequest() != null) {
      if (!BeeUtils.equalsTrim(query, getLastRequest().getQuery())
          || type != getLastRequest().getSearchType()) {
        start = 0;
        setOffset(start);
      }
    }

    Request request = new Request(query, type, start, size);
    if (request.equals(getLastRequest())) {
      return;
    }
    setLastRequest(request);

    getInput().addStyleName(STYLE_WAITING);
    setWaiting(true);

    getOracle().requestSuggestions(request, getCallback());
  }

  private void exit(boolean hideSelector, State state) {
    exit(hideSelector, state, null, false);
  }

  private Callback getCallback() {
    return callback;
  }

  private Request getLastRequest() {
    return lastRequest;
  }

  private int getOffset() {
    return offset;
  }

  private Operator getSearchType() {
    return searchType;
  }

  private Selector getSelector() {
    return selector;
  }

  private boolean hasMore() {
    return hasMore;
  }

  private boolean isAlive() {
    return alive;
  }

  private boolean isHandledByForm() {
    return handledByForm;
  }

  private boolean isInstant() {
    Operator operator = getSearchType();
    return Operator.CONTAINS.equals(operator) || Operator.STARTS.equals(operator);
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

  private void setAlive(boolean alive) {
    if (isAlive() != alive) {
      this.alive = alive;
      getInput().setStyleName(STYLE_EMPTY, !alive);
    }
  }

  private void setHandledByForm(boolean handledByForm) {
    this.handledByForm = handledByForm;
  }

  private void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
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