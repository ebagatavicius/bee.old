package com.butent.bee.client.composite;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.InputEvent;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HandlesRendering;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HandlesValueChange;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MultiSelector extends DataSelector implements HandlesRendering, HandlesValueChange {

  private final class ChoiceWidget extends Flow {

    private final Value choice;
    private final String labelId;

    private ChoiceWidget(Value choice, String caption) {
      super();
      this.choice = choice;

      addStyleName(STYLE_CHOICE);

      InlineLabel label = new InlineLabel();
      label.getElement().setInnerText(caption);
      label.addStyleName(STYLE_LABEL);

      if (MultiSelector.this.isEditEnabled()) {
        label.addStyleName(RowEditor.EDITABLE_RELATION_STYLE);
        label.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            event.stopPropagation();

            Long rowId = getChoice().getLong();
            if (DataUtils.isId(rowId)) {
              MultiSelector.this.editChoice(rowId);
            }
          }
        });
      }

      this.labelId = label.getId();
      add(label);

      CustomDiv close = new CustomDiv(STYLE_CLOSE);
      close.setText(String.valueOf(BeeConst.CHAR_TIMES));

      close.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          event.stopPropagation();
          MultiSelector.this.removeChoice(getChoice());
        }
      });

      add(close);
    }

    @Override
    public String getIdPrefix() {
      return "choice";
    }

    private Value getChoice() {
      return choice;
    }

    private void setCaption(String caption) {
      for (Widget child : this) {
        if (DomUtils.idEquals(child, labelId)) {
          child.getElement().setInnerText(caption);
          break;
        }
      }
    }
  }

  private static final String STYLE_PREFIX = "bee-MultiSelector-";
  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_CONTAINER_ACTIVE = STYLE_CONTAINER + "-active";

  private static final String STYLE_CHOICE = STYLE_PREFIX + "choice";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_CLOSE = STYLE_PREFIX + "close";

  private static final String STYLE_INPUT = STYLE_PREFIX + "input";
  private static final String STYLE_PLUS = STYLE_PREFIX + "plus";

  private static final int MIN_INPUT_WIDTH = 25;
  private static final int MAX_INPUT_LENGTH = 30;

  public static MultiSelector autonomous(Relation relation, AbstractCellRenderer renderer) {
    Assert.notNull(relation);

    final MultiSelector selector = new MultiSelector(relation, true, null);
    selector.setRenderer(renderer);

    selector.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        selector.setEditing(true);
      }
    });

    selector.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        selector.setEditing(false);
      }
    });

    return selector;
  }

  public static MultiSelector autonomous(Relation relation, List<String> renderColumns) {
    Assert.notNull(relation);
    Assert.notEmpty(relation.getViewName());
    Assert.notEmpty(renderColumns);

    return autonomous(relation,
        RendererFactory.createRenderer(relation.getViewName(), renderColumns));
  }

  public static MultiSelector autonomous(String viewName, List<String> columns) {
    Assert.notEmpty(viewName);
    Assert.notEmpty(columns);

    return autonomous(Relation.create(viewName, columns),
        RendererFactory.createRenderer(viewName, columns));
  }

  private static List<Long> getIds(Collection<Value> choices) {
    List<Long> ids = new ArrayList<>();

    if (!BeeUtils.isEmpty(choices)) {
      for (Value choice : choices) {
        if (choice != null && choice.getType() == ValueType.LONG) {
          Long id = choice.getLong();
          if (DataUtils.isId(id)) {
            ids.add(id);
          }
        }
      }
    }
    return ids;
  }

  private static boolean hasChoice(Widget widget, Value choice) {
    return widget instanceof ChoiceWidget && ((ChoiceWidget) widget).getChoice().equals(choice);
  }

  private final CellSource cellSource;

  private AbstractCellRenderer renderer;

  private final Map<Value, String> cache = new HashMap<>();
  private String value;

  private String oldValue;

  private final Consumer<InputText> inputResizer;

  private final int emptyContainerSize;

  public MultiSelector(Relation relation, boolean embedded, CellSource cellSource) {
    super(relation, embedded);
    this.cellSource = cellSource;

    this.inputResizer = UiHelper.getTextBoxResizer(MIN_INPUT_WIDTH);

    this.emptyContainerSize = getContainer().getWidgetCount();

    SelectorEvent.fire(this, State.INITIALIZED);
  }

  @Override
  public void clearValue() {
    super.clearValue();
    setOldValue(null);

    clearChoices();
    setExclusions(null);
  }

  public CellSource getCellSource() {
    return cellSource;
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "multi";
  }

  @Override
  public String getLabel() {
    String label = super.getRelationLabel();
    if (BeeUtils.isEmpty(label)) {
      label = Data.getViewCaption(getOracle().getViewName());
    }
    return label;
  }

  @Override
  public AbstractCellRenderer getRenderer() {
    return renderer;
  }

  public String getRowLabel(long rowId) {
    if (selectsIds()) {
      String label = cache.get(new LongValue(rowId));
      if (!BeeUtils.isEmpty(label)) {
        return label;
      }
    }

    BeeRowSet data = getOracle().getViewData();
    if (DataUtils.isEmpty(data)) {
      return null;
    }

    BeeRow row = data.getRowById(rowId);
    return (row == null) ? null : getRenderer().render(row);
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.MULTI_SELECTOR;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return super.isOrHasPartner(node) || getElement().isOrHasChild(node);
  }

  @Override
  public boolean isValueChanged() {
    return !BeeUtils.equalsTrim(getValue(), getOldValue());
  }

  @Override
  public void render(IsRow row) {
    if (cellSource != null) {
      render(cellSource.getString(row));
    }
  }

  public void render(String input) {
    setOldValue(input);
    setValue(input);

    final List<Value> choices = parseChoices(input);
    if (choices.isEmpty()) {
      clearChoices();
      setExclusions(null);

    } else if (hasValueSource()) {
      clearChoices();
      for (Value choice : choices) {
        cache.put(choice, choice.getString());
      }
      renderChoices(choices);

    } else if (cache.keySet().containsAll(choices)) {
      renderChoices(choices);

    } else {
      Set<Long> notCached = new HashSet<>(getIds(choices));
      notCached.removeAll(getIds(cache.keySet()));

      getData(notCached, new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          if (result != null) {
            for (BeeRow r : result.getRows()) {
              cache.put(getRowValue(r), getRenderer().render(r));
            }
          }
          renderChoices(choices);
        }
      });
    }
  }

  @Override
  public void setDisplayValue(String dv) {
    if (!Objects.equals(getDisplayValue(), dv)) {
      super.setDisplayValue(dv);
      inputResizer.accept(getInput());
    }
  }

  @Override
  public void setRenderer(AbstractCellRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public void setSelection(BeeRow row, Value sv, boolean fire) {
    hideSelector();
    reset();
    clearInput();

    Value selectedValue = (sv == null) ? getRowValue(row) : sv;

    if (selectedValue != null) {
      String label = (sv == null) ? renderer.render(row) : sv.getString();
      cache.put(selectedValue, label);

      if (addChoice(new ChoiceWidget(selectedValue, label))) {
        updateValue();

        setRelatedRow(row);

        if (fire) {
          SelectorEvent.fire(this, State.INSERTED);
        }
      }

      getElement().scrollIntoView();
      setFocus(true);
    }
  }

  @Override
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public void startEdit(String oldV, char charCode, EditorAction onEntry, Element sourceElement) {
    SelectorEvent.fireExclusions(this, getOracle().getExclusions());

    super.startEdit(oldV, charCode, onEntry, sourceElement);
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return validate(getNormalizedValue(), checkForNull);
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    List<String> messages = new ArrayList<>();
    messages.addAll(super.validate(normalizedValue, checkForNull));
    if (!messages.isEmpty()) {
      return messages;
    }

    if (BeeUtils.isEmpty(normalizedValue) && checkForNull && !isNullable()) {
      if (!BeeUtils.isEmpty(getLabel())) {
        messages.add(getLabel());
      }
      messages.add(Localized.getConstants().valueRequired());
    }
    return messages;
  }

  @Override
  protected void exit(boolean hideSelector, State state, Integer keyCode, boolean hasModifiers) {
    State st = BeeUtils.same(getOldValue(), getValue()) ? state : State.CHANGED;
    super.exit(hideSelector, st, keyCode, hasModifiers);
  }

  @Override
  protected void init(final InputWidget inputWidget, boolean embed) {
    final Flow container = new Flow(STYLE_CONTAINER);

    int maxLength = inputWidget.getMaxLength();
    maxLength = (maxLength > 0) ? Math.min(maxLength, MAX_INPUT_LENGTH) : MAX_INPUT_LENGTH;
    inputWidget.setMaxLength(maxLength);

    inputWidget.addStyleName(STYLE_INPUT);
    container.add(inputWidget);

    if (isNewRowEnabled()) {
      FaLabel plusWidget = new FaLabel(FontAwesome.PLUS_SQUARE_O, STYLE_PLUS);
      plusWidget.setTitle(BeeUtils.buildLines(Localized.getConstants().actionCreate(),
          BeeUtils.bracket(getLabel())));

      plusWidget.addMouseDownHandler(new MouseDownHandler() {
        @Override
        public void onMouseDown(MouseDownEvent event) {
          event.stopPropagation();
          RowFactory.createRelatedRow(MultiSelector.this, getDisplayValue());
        }
      });

      container.add(plusWidget);
    }

    inputWidget.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        container.addStyleName(STYLE_CONTAINER_ACTIVE);
      }
    });
    inputWidget.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        container.removeStyleName(STYLE_CONTAINER_ACTIVE);
        clearInput();
      }
    });

    inputWidget.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_BACKSPACE) {
          onBackSpace();
        }
      }
    });

    inputWidget.addInputHandler(new InputHandler() {
      @Override
      public void onInput(InputEvent event) {
        inputResizer.accept(inputWidget);
      }
    });

    StyleUtils.setWidth(inputWidget, MIN_INPUT_WIDTH);

    DomUtils.makeFocusable(container);

    Binder.addClickHandler(container, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!isEditing() && isEnabled()) {
          inputWidget.setFocus(true);
          inputWidget.onMouseClick(true);
        }
      }
    });

    initWidget(container);
  }

  private boolean addChoice(ChoiceWidget choiceWidget) {
    Flow container = getContainer();

    for (Widget child : container) {
      if (hasChoice(child, choiceWidget.getChoice())) {
        return false;
      }
    }

    container.insert(choiceWidget, container.getWidgetCount() - emptyContainerSize);
    return true;
  }

  private String buildValue(List<Value> choices) {
    if (BeeUtils.isEmpty(choices)) {
      return null;
    } else if (hasValueSource()) {
      return Codec.beeSerialize(choices);
    } else {
      return DataUtils.buildIdList(getIds(choices));
    }
  }

  private void clearChoices() {
    Flow container = getContainer();
    while (container.getWidgetCount() > emptyContainerSize) {
      container.remove(0);
    }
  }

  private void clearInput() {
    clearDisplay();
  }

  private void editChoice(long rowId) {
    if (BeeConst.isUndef(getEditSourceIndex())) {
      boolean modal = isEditModal();
      RowCallback rowCallback;

      if (modal) {
        rowCallback = new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            updateChoice(result);
          }
        };
      } else {
        rowCallback = null;
      }

      Opener opener = modal ? Opener.relativeTo(getWidget()) : Opener.NEW_TAB;
      RowEditor.openForm(getEditForm(), getOracle().getDataInfo(), rowId, opener, rowCallback);

    } else {
      BeeRow row = getOracle().getCachedRow(rowId);

      if (row == null) {
        Queries.getRow(getOracle().getViewName(), rowId, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            editChoiceSource(result);
          }
        });

      } else {
        editChoiceSource(row);
      }
    }
  }

  private void editChoiceSource(final BeeRow choiceRow) {
    if (choiceRow == null) {
      return;
    }

    final int sourceIndex = getEditSourceIndex();
    if (BeeConst.isUndef(sourceIndex)) {
      return;
    }

    Long sourceId = choiceRow.getLong(sourceIndex);
    if (!DataUtils.isId(sourceId)) {
      return;
    }

    boolean modal = isEditModal();
    RowCallback rowCallback;

    if (modal) {
      rowCallback = new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          if (result != null && !BeeUtils.same(getEditViewName(), getOracle().getViewName())) {
            RelationUtils.updateRow(getOracle().getDataInfo(),
                getOracle().getDataInfo().getColumnId(sourceIndex), choiceRow,
                Data.getDataInfo(getEditViewName()), result, false);
            updateChoice(choiceRow);
          }
        }
      };
    } else {
      rowCallback = null;
    }

    Opener opener = modal ? Opener.relativeTo(getWidget()) : Opener.NEW_TAB;
    RowEditor.openForm(getEditForm(), Data.getDataInfo(getEditViewName()), sourceId, opener,
        rowCallback);
  }

  private Flow getContainer() {
    return (Flow) getWidget();
  }

  private void getData(Collection<Long> ids, Queries.RowSetCallback callback) {
    int size = ids.size();

    BeeRowSet cached = getOracle().getViewData();
    if (cached != null && cached.getNumberOfRows() >= size) {
      BeeRowSet result = new BeeRowSet(cached.getViewName(), cached.getColumns());

      for (BeeRow row : cached.getRows()) {
        if (ids.contains(row.getId())) {
          result.addRow(row);
          if (result.getNumberOfRows() == size) {
            callback.onSuccess(result);
            return;
          }
        }
      }
    }

    Queries.getRowSet(getOracle().getViewName(), null, Filter.idIn(ids),
        getOracle().getViewOrder(), callback);
  }

  private String getOldValue() {
    return oldValue;
  }

  private void onBackSpace() {
    if (getInput().getCursorPos() == 0 && !BeeUtils.isEmpty(getValue())) {
      Flow container = getContainer();

      if (container.getWidgetCount() > emptyContainerSize) {
        Widget child = container.getWidget(container.getWidgetCount() - emptyContainerSize - 1);
        if (child instanceof ChoiceWidget) {
          removeChoice(((ChoiceWidget) child).getChoice());
        }
      }
    }
  }

  private List<Value> parseChoices(String input) {
    List<Value> choices = new ArrayList<>();
    if (BeeUtils.isEmpty(input)) {
      return choices;
    }

    if (hasValueSource()) {
      String[] arr = Codec.beeDeserializeCollection(input);
      if (arr != null) {
        for (String s : arr) {
          Value choice = Value.restore(s);
          if (choice != null) {
            choices.add(choice);
          }
        }
      }

    } else {
      List<Long> ids = DataUtils.parseIdList(input);
      for (Long id : ids) {
        choices.add(new LongValue(id));
      }
    }

    return choices;
  }

  private void removeChoice(Value choice) {
    if (!isEnabled()) {
      return;
    }

    hideSelector();
    reset();
    clearInput();

    Flow panel = getContainer();
    int count = panel.getWidgetCount();

    boolean removed = false;

    for (int i = 0; i < count; i++) {
      if (hasChoice(panel.getWidget(i), choice)) {
        removed = panel.remove(i);
        break;
      }
    }

    updateValue();

    getElement().scrollIntoView();
    setFocus(true);

    if (removed) {
      SelectorEvent.fire(this, State.REMOVED);
    }
  }

  private void renderChoices(List<Value> choices) {
    clearChoices();

    for (Value choice : choices) {
      addChoice(new ChoiceWidget(choice, cache.get(choice)));
    }

    setExclusions(choices);
  }

  private boolean selectsIds() {
    return !hasValueSource();
  }

  private void setExclusions(List<Value> choices) {
    if (selectsIds()) {
      Collection<Long> exclusions = getIds(choices);
      SelectorEvent event = SelectorEvent.fireExclusions(this, exclusions);
      if (!event.isConsumed()) {
        getOracle().setExclusions(exclusions);
      }
    }
  }

  private void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  private void updateChoice(IsRow row) {
    Value choice = getRowValue(row);

    if (choice != null && cache.containsKey(choice)) {
      String label = getRenderer().render(row);

      if (!BeeUtils.isEmpty(label) && !label.equals(cache.get(choice))) {
        cache.put(choice, label);

        Flow panel = getContainer();
        for (Widget child : panel) {
          if (hasChoice(child, choice)) {
            ((ChoiceWidget) child).setCaption(label);
            break;
          }
        }
      }
    }
  }

  private void updateValue() {
    List<Value> choices = new ArrayList<>();

    Flow panel = getContainer();
    for (Widget child : panel) {
      if (child instanceof ChoiceWidget) {
        choices.add(((ChoiceWidget) child).getChoice());
      }
    }

    setValue(buildValue(choices));
    setExclusions(choices);
  }
}
