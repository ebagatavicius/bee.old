package com.butent.bee.client.composite;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HandlesRendering;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HandlesValueChange;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
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
import java.util.function.Consumer;

public class MultiSelector extends DataSelector implements HandlesRendering, HandlesValueChange,
    HasValueChangeHandlers<String> {

  public static class Choice {

    private final Long rowId;
    private final Value value;

    public Choice(Long rowId) {
      this(rowId, null);
    }

    public Choice(Long rowId, Value value) {
      this.rowId = rowId;
      this.value = value;
    }

    public Choice(Value choice) {
      this(null, choice);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj instanceof Choice) {
        Choice other = (Choice) obj;
        return Objects.equals(rowId, other.rowId) && Objects.equals(value, other.value);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(rowId, value);
    }

    private Long getRowId() {
      return rowId;
    }

    private Value getValue() {
      return value;
    }

    private boolean hasRowId() {
      return DataUtils.isId(rowId);
    }

    private boolean hasValue() {
      return value != null && !value.isNull();
    }
  }

  private final class ChoiceWidget extends Flow {

    private final Choice choice;
    private final String labelId;

    private ChoiceWidget(Choice choice, String caption) {
      super(STYLE_CHOICE);

      this.choice = choice;

      InlineLabel label = new InlineLabel();
      label.getElement().setInnerText(caption);
      label.addStyleName(STYLE_LABEL);

      if (MultiSelector.this.isEditEnabled() && DataUtils.isId(choice.getRowId())) {
        label.addStyleName(RowEditor.EDITABLE_RELATION_STYLE);

        label.addClickHandler(event -> {
          event.stopPropagation();
          MultiSelector.this.editChoice(getRowId());
        });
      }

      this.labelId = label.getId();
      add(label);

      CustomDiv close = new CustomDiv(STYLE_CLOSE);
      close.setText(String.valueOf(BeeConst.CHAR_TIMES));

      close.addClickHandler(event -> {
        event.stopPropagation();
        MultiSelector.this.removeChoice(getChoice());
      });

      add(close);
    }

    @Override
    public String getIdPrefix() {
      return "choice";
    }

    private Choice getChoice() {
      return choice;
    }

    private Long getRowId() {
      return choice.getRowId();
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

  public static final String ATTR_SEPARATORS = "separators";

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "MultiSelector-";
  public static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_CONTAINER_ACTIVE = STYLE_CONTAINER + "-active";

  private static final String STYLE_CHOICE = STYLE_PREFIX + "choice";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_CLOSE = STYLE_PREFIX + "close";

  private static final String STYLE_INPUT = STYLE_PREFIX + "input";
  private static final String STYLE_PLUS = STYLE_PREFIX + "plus";

  private static final int MIN_INPUT_WIDTH = 25;

  public static MultiSelector autonomous(Relation relation, AbstractCellRenderer renderer) {
    Assert.notNull(relation);

    MultiSelector selector = new MultiSelector(relation, true, null);
    selector.setRenderer(renderer);

    selector.addFocusHandler(event -> selector.setEditing(true));
    selector.addBlurHandler(event -> selector.setEditing(false));

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

  private static List<Choice> getChoices(Collection<Long> ids) {
    if (BeeUtils.isEmpty(ids)) {
      return new ArrayList<>();

    } else {
      List<Choice> choices = new ArrayList<>();

      for (Long id : ids) {
        if (DataUtils.isId(id)) {
          choices.add(new Choice(id));
        }
      }

      return choices;
    }
  }

  private static List<Long> getIds(Collection<Choice> choices) {
    List<Long> ids = new ArrayList<>();

    if (!BeeUtils.isEmpty(choices)) {
      for (Choice choice : choices) {
        if (choice != null) {
          Long id = choice.getRowId();
          if (DataUtils.isId(id)) {
            ids.add(id);
          }
        }
      }
    }
    return ids;
  }

  private static boolean hasChoice(Widget widget, Choice choice) {
    return widget instanceof ChoiceWidget && ((ChoiceWidget) widget).getChoice().equals(choice);
  }

  private final CellSource cellSource;

  private AbstractCellRenderer renderer;

  private final Map<Choice, String> cache = new HashMap<>();
  private String value;

  private String oldValue;

  private final Consumer<InputText> inputResizer;

  private final int emptyContainerSize;

  private String separators;

  private boolean effectivelyActive;

  public MultiSelector(Relation relation, boolean embedded, CellSource cellSource) {
    super(relation, embedded);

    this.cellSource = cellSource;

    this.inputResizer = UiHelper.getTextBoxResizer(MIN_INPUT_WIDTH);

    this.emptyContainerSize = getContainer().getWidgetCount();

    SelectorEvent.fire(this, State.INITIALIZED);
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    addValueChangeHandler(handler);
    return super.addEditChangeHandler(handler);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public void clearValue() {
    super.clearValue();
    setOldValue(null);

    clearChoices();
    setExclusions(null);

    SummaryChangeEvent.maybeFire(this);
  }

  public CellSource getCellSource() {
    return cellSource;
  }

  public List<Choice> getChoices() {
    return parseChoices(getValue());
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "multi";
  }

  public List<Long> getIds() {
    return getIds(getChoices());
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
      String label = cache.get(new Choice(rowId));
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
  public Value getSummary() {
    return new IntegerValue(getChoices().size());
  }

  /**
   * Returns the internal representation.
   * This method is only provided for compatibility with the HasStringValue interface. Use
   * getIds(), getValues(), getChoices() instead.
   */
  @Override
  public String getValue() {
    return value;
  }

  public List<String> getValues() {
    List<String> values = new ArrayList<>();

    List<Choice> choices = getChoices();
    if (!BeeUtils.isEmpty(choices)) {
      for (Choice choice : choices) {
        if (choice.hasValue()) {
          values.add(choice.getValue().getString());
        }
      }
    }

    return values;
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
      String input = cellSource.getString(row);

      updateValues(input);
      renderChoices(parseChoices(input));
    }
  }

  public void setChoices(Collection<Choice> choices) {
    updateValues(buildValue(choices));
    renderChoices(choices);
  }

  @Override
  public void setDisplayValue(String dv) {
    if (!Objects.equals(getDisplayValue(), dv)) {
      super.setDisplayValue(dv);
      inputResizer.accept(getInput());
    }
  }

  public void setIds(Collection<Long> ids) {
    updateValues(DataUtils.buildIdList(ids));
    renderChoices(getChoices(ids));
  }

  public void setIds(String idList) {
    updateValues(idList);
    renderChoices(parseChoices(idList));
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

    String label;

    if (sv != null) {
      label = sv.getString();
    } else if (row != null && getRenderer() != null) {
      label = renderer.render(row);
    } else {
      label = null;
    }

    Choice choice;

    if (sv != null) {
      choice = new Choice(sv);
    } else if (row == null) {
      choice = null;
    } else if (selectsIds()) {
      choice = new Choice(row.getId());

    } else {
      Value rv = getRowValue(row);

      if (rv == null || rv.isNull()) {
        choice = null;
      } else {
        if (BeeUtils.isEmpty(label)) {
          label = rv.getString();
        }
        choice = new Choice(row.getId(), rv);
      }
    }

    if (choice != null && !BeeUtils.isEmpty(label)) {
      cache.put(choice, label);

      if (addChoice(new ChoiceWidget(choice, label))) {
        updateValue();

        setRelatedRow(row);

        if (fire) {
          SelectorEvent.fire(this, State.INSERTED);
          ValueChangeEvent.fire(this, getValue());
          SummaryChangeEvent.maybeFire(this);
        }
      }

      DomUtils.scrollIntoView(getElement());
      setFocus(true);

      setActive(true);
    }
  }

  public void setSeparators(String separators) {
    this.separators = separators;
  }

  /**
   * Sets the internal representation.
   * This method is only provided for compatibility with the HasStringValue interface. Use setIds,
   * setValues, setChoices instead.
   */
  @Override
  public void setValue(String value) {
    this.value = value;
  }

  public void setValues(Collection<String> values) {
    if (!BeeUtils.isEmpty(values) && hasValueSource()) {
      List<Choice> choices = new ArrayList<>();

      for (String s : values) {
        if (!BeeUtils.isEmpty(s)) {
          choices.add(new Choice(new TextValue(s)));
        }
      }

      updateValues(buildValue(choices));
      renderChoices(choices);

    } else {
      updateValues(null);
      clearChoices();

      setExclusions(null);
      SummaryChangeEvent.maybeFire(this);
    }
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
      messages.add(Localized.dictionary().valueRequired());
    }
    return messages;
  }

  @Override
  protected boolean consumeCharacter(char ch) {
    if (getSeparators() != null && getSeparators().indexOf(ch) >= 0
        && !isStrict() && !BeeUtils.isEmpty(getDisplayValue())) {

      Value v = parse(getDisplayValue());
      if (v != null) {
        setSelection(null, v, true);
        return true;
      }
    }
    return super.consumeCharacter(ch);
  }

  @Override
  protected void exit(boolean hideSelector, State state, Integer keyCode, boolean hasModifiers) {
    State st = BeeUtils.same(getOldValue(), getValue()) ? state : State.CHANGED;
    super.exit(hideSelector, st, keyCode, hasModifiers);
  }

  @Override
  protected void init(final InputWidget inputWidget, boolean embed) {
    final Flow container = new Flow(STYLE_CONTAINER);

    inputWidget.addStyleName(STYLE_INPUT);

    container.add(inputWidget);

    if (isNewRowEnabled()) {
      FaLabel plusWidget = new FaLabel(FontAwesome.PLUS_CIRCLE, STYLE_PLUS);
      plusWidget.setTitle(BeeUtils.buildLines(Localized.dictionary().actionCreate(),
          BeeUtils.bracket(getLabel())));

      plusWidget.addMouseDownHandler(event -> {
        event.stopPropagation();
        RowFactory.createRelatedRow(MultiSelector.this, getDisplayValue());
      });

      container.add(plusWidget);
    }

    inputWidget.addFocusHandler(event -> container.addStyleName(STYLE_CONTAINER_ACTIVE));
    inputWidget.addBlurHandler(event -> {
      container.removeStyleName(STYLE_CONTAINER_ACTIVE);
      clearInput();
    });

    inputWidget.addKeyDownHandler(event -> {
      if (event.getNativeKeyCode() == KeyCodes.KEY_BACKSPACE) {
        onBackSpace();
      }
    });

    inputWidget.addInputHandler(event -> inputResizer.accept(inputWidget));

    StyleUtils.setWidth(inputWidget, MIN_INPUT_WIDTH);

    DomUtils.makeFocusable(container);

    Binder.addMouseDownHandler(container, event -> setEffectivelyActive(isActive()));

    container.addClickHandler(event -> {
      if (isEnabled()) {
        if (!isEditing()) {
          inputWidget.setFocus(true);
        }

        if (!isActive() && isEffectivelyActive()) {
          setActive(true);
        }
        inputWidget.onMouseClick();
      }
    });

    initWidget(container);
  }

  public String getOldValue() {
    return oldValue;
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

  private String buildValue(Collection<Choice> choices) {
    if (BeeUtils.isEmpty(choices)) {
      return null;

    } else if (hasValueSource()) {
      List<Object> values = new ArrayList<>();
      for (Choice choice : choices) {
        if (choice.getValue() != null) {
          values.add(choice.getRowId());
          values.add(choice.getValue());
        }
      }
      return Codec.beeSerialize(values);

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
      RowEditor.openForm(getEditForm(), getOracle().getDataInfo(), Filter.compareId(rowId), opener,
          rowCallback);

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
    RowEditor.openForm(getEditForm(), Data.getDataInfo(getEditViewName()),
        Filter.compareId(sourceId), opener, rowCallback);
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

  private String getSeparators() {
    return separators;
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

  private List<Choice> parseChoices(String input) {
    if (BeeUtils.isEmpty(input)) {
      return new ArrayList<>();

    } else if (hasValueSource()) {
      List<Choice> choices = new ArrayList<>();
      String[] arr = Codec.beeDeserializeCollection(input);

      if (arr != null) {
        for (int i = 0; i < arr.length - 1; i += 2) {
          Long id = BeeUtils.toLongOrNull(arr[i]);
          Value v = Value.restore(arr[i + 1]);

          if (v != null && !v.isNull()) {
            choices.add(new Choice(id, v));
          }
        }
      }

      return choices;

    } else {
      return getChoices(DataUtils.parseIdList(input));
    }
  }

  private void removeChoice(Choice choice) {
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

    DomUtils.scrollIntoView(getElement());
    setFocus(true);

    setActive(true);

    if (removed) {
      SelectorEvent.fire(this, State.REMOVED);
      ValueChangeEvent.fire(this, getValue());
      SummaryChangeEvent.maybeFire(this);
    }
  }

  private void renderCachedChoices(Collection<Choice> choices) {
    clearChoices();

    for (Choice choice : choices) {
      String label = cache.get(choice);

      if (!BeeUtils.isEmpty(label)) {
        addChoice(new ChoiceWidget(choice, label));
      }
    }

    setExclusions(choices);
    SummaryChangeEvent.maybeFire(this);
  }

  private void renderChoices(final Collection<Choice> choices) {
    if (choices.isEmpty()) {
      clearChoices();

      setExclusions(null);
      SummaryChangeEvent.maybeFire(this);

    } else if (cache.keySet().containsAll(choices)) {
      renderCachedChoices(choices);

    } else {
      Set<Long> notCached = new HashSet<>(getIds(choices));
      notCached.removeAll(getIds(cache.keySet()));

      if (hasValueSource()) {
        for (Choice choice : choices) {
          if (!choice.hasRowId() && choice.hasValue()) {
            cache.put(choice, choice.getValue().getString());
          }
        }
      }

      if (notCached.isEmpty()) {
        renderCachedChoices(choices);

      } else {
        getData(notCached, new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            if (result != null) {
              for (BeeRow row : result) {
                String label = getRenderer().render(row);

                if (!BeeUtils.isEmpty(label)) {
                  for (Choice choice : choices) {
                    if (Objects.equals(row.getId(), choice.getRowId())) {
                      cache.put(choice, label);
                      break;
                    }
                  }
                }
              }
            }

            renderCachedChoices(choices);
          }
        });
      }
    }
  }

  private boolean selectsIds() {
    return !hasValueSource();
  }

  private boolean isEffectivelyActive() {
    return effectivelyActive;
  }

  private void setEffectivelyActive(boolean effectivelyActive) {
    this.effectivelyActive = effectivelyActive;
  }

  private void setExclusions(Collection<Choice> choices) {
    Collection<Long> exclusions = getIds(choices);

    if (!BeeUtils.sameElements(exclusions, getOracle().getExclusions())) {
      SelectorEvent event = SelectorEvent.fireExclusions(this, exclusions);
      if (!event.isConsumed()) {
        getOracle().setExclusions(exclusions);
      }
    }
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  private void updateChoice(IsRow row) {
    Choice choice = (row == null) ? null : new Choice(row.getId());

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
    List<Choice> choices = new ArrayList<>();

    Flow panel = getContainer();
    for (Widget child : panel) {
      if (child instanceof ChoiceWidget) {
        choices.add(((ChoiceWidget) child).getChoice());
      }
    }

    setValue(buildValue(choices));
    setExclusions(choices);
  }

  private void updateValues(String input) {
    setOldValue(input);
    setValue(input);
  }
}
