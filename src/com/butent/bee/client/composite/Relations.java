package com.butent.bee.client.composite;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.grid.CellKind;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.grid.TableKind;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HandlesValueChange;
import com.butent.bee.client.ui.HasFosterParent;
import com.butent.bee.client.ui.HasRowChildren;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class Relations extends Flow implements Editor, ClickHandler, SelectorEvent.Handler,
    ParentRowEvent.Handler, HasFosterParent, HasRowChildren, HandlesValueChange,
    SummaryChangeEvent.Handler, RowInsertEvent.Handler {

  public interface InputRelationsInterceptor {
    void onClose();

    void onOpen();

    void onSave();
  }

  public static final String PFX_RELATED = "Rel";

  private static final String STORAGE = TBL_RELATIONS;

  private final String column;
  private final String view;
  private final boolean inline;

  private final HtmlTable table = new HtmlTable();

  private boolean enabled = true;
  private String options;
  private boolean handlesTabulation;

  private boolean summarize;

  private final List<com.google.web.bindery.event.shared.HandlerRegistration> eventRegistry =
      new ArrayList<>();
  private String parentId;

  private final Multimap<String, Long> ids = HashMultimap.create();
  private final Map<String, MultiSelector> widgetMap = new LinkedHashMap<>();
  private final Map<MultiSelector, HandlerRegistration> registry = new HashMap<>();
  private final Set<String> blockedRelations = new HashSet<>();

  private final Map<String, String> rowProperties = new HashMap<>();

  private static final String RELATIONS_PLUS_ADD_RELATION = "bee-Relations-newRel";

  private Long id;
  private SelectorEvent.Handler handler;
  private InputRelationsInterceptor inputRelationsInterceptor;

  public Relations(String column, boolean inline, Collection<Relation> relations,
      List<String> defaultRelations, List<String> blockedRelations) {

    DataInfo info = Data.getDataInfo(STORAGE);

    this.column = Assert.notEmpty(column);
    this.view = Assert.notEmpty(info.getRelation(column));
    this.inline = inline;

    table.setKind(TableKind.CONTROLS);

    if (inline) {
      FaLabel add = new FaLabel(FontAwesome.PLUS_CIRCLE, RELATIONS_PLUS_ADD_RELATION);

      add.addClickHandler(ev -> addRelations());
      table.setWidget(0, 0, add);
      table.getCellFormatter().setHorizontalAlignment(0, 0, TextAlign.CENTER);
      add(table);

    } else {
      FaLabel face = new FaLabel(FontAwesome.CHAIN);
      face.setTitle(Localized.dictionary().relations());
      face.addClickHandler(this);
      add(face);
      table.setWidth("600px");
    }

    table.setColumnCellKind(0, CellKind.LABEL);
    table.setColumnCellStyles(0, "white-space:nowrap; vertical-align:middle; padding-right:1em;");
    table.setColumnCellStyles(1, "width:100%");

    Map<String, String> viewMap = new HashMap<>();

    for (String col : info.getColumnNames(false)) {
      String viewName = info.getRelation(col);

      if (!BeeKeeper.getUser().isDataVisible(viewName) || BeeUtils.same(col, COL_RELATION)
          || BeeUtils.containsSame(blockedRelations, viewName)) {
        continue;
      }
      widgetMap.put(col, null);
      viewMap.put(viewName, col);
    }
    if (!BeeUtils.isEmpty(relations)) {
      for (Relation relation : relations) {
        String col = viewMap.get(relation.getViewName());

        if (!BeeUtils.isEmpty(col)) {
          createMultiSelector(col, relation);
        }
      }
    }
    if (!BeeUtils.isEmpty(defaultRelations)) {
      for (String relation : defaultRelations) {
        String col = viewMap.get(relation);

        if (!BeeUtils.isEmpty(col) && widgetMap.get(col) == null) {
          createMultiSelector(col, null);
        }
      }
    }
    blockRelation(MailConstants.TBL_MESSAGES, true);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler arg0) {
    return addDomHandler(arg0, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler arg0) {
    return addHandler(arg0, ValueChangeEvent.getType());
  }

  @Override
  public HandlerRegistration addEditStopHandler(EditStopEvent.Handler arg0) {
    return addHandler(arg0, EditStopEvent.getType());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler arg0) {
    return addDomHandler(arg0, FocusEvent.getType());
  }

  @Override
  public HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler eh) {
    return addHandler(eh, SummaryChangeEvent.getType());
  }

  public void blockRelation(String viewName, boolean block) {
    if (block) {
      blockedRelations.add(viewName);
    } else {
      blockedRelations.remove(viewName);
    }
    setEnabled(isEnabled());
  }

  @Override
  public void clearValue() {
  }

  @Override
  public Collection<RowChildren> getChildrenForInsert() {
    if (inline) {
      return getRowChildren(false);
    }
    return null;
  }

  @Override
  public Collection<RowChildren> getChildrenForUpdate() {
    return getChildrenForInsert();
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getLabel() {
    List<String> messages = new ArrayList<>();

    for (Entry<String, MultiSelector> entry : widgetMap.entrySet()) {
      MultiSelector multi = entry.getValue();

      if (multi != null && multi.isValueChanged()) {
        messages.add(Data.getViewCaption(Data.getColumnRelation(STORAGE, entry.getKey())));
      }
    }
    return BeeUtils.joinItems(messages);
  }

  @Override
  public String getNormalizedValue() {
    return null;
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  public Collection<RowChildren> getRowChildren(boolean all) {
    List<RowChildren> relations = new ArrayList<>();

    for (Entry<String, MultiSelector> entry : widgetMap.entrySet()) {
      MultiSelector multi = entry.getValue();

      if (multi != null && (all || multi.isValueChanged())) {
        relations.add(RowChildren.create(STORAGE, column, id, entry.getKey(),
            DataUtils.buildIdList(multi.getIds())));
      }
    }
    return relations;
  }

  public Collection<RowChildren> getOldRowChildren() {
    List<RowChildren> relations = new ArrayList<>();

    widgetMap.forEach((key, multi) -> {
      if (multi != null) {
        relations.add(RowChildren.create(STORAGE, column, id, key, multi.getOldValue()));
      }
    });
    return relations;
  }

  public String getSelectorRowLabel(String viewName, long rowId) {
    for (MultiSelector multi : widgetMap.values()) {
      if (multi != null && BeeUtils.same(multi.getOracle().getViewName(), viewName)) {
        return multi.getRowLabel(rowId);
      }
    }
    return BeeConst.STRING_EMPTY;
  }

  @Override
  public Value getSummary() {
    int count = 0;

    for (MultiSelector multi : widgetMap.values()) {
      if (multi != null) {
        count += multi.getChoices().size();
      }
    }

    return new IntegerValue(count);
  }

  @Override
  public int getTabIndex() {
    return getElement().getTabIndex();
  }

  @Override
  public String getValue() {
    return null;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.RELATIONS;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return false;
  }

  @Override
  public boolean handlesTabulation() {
    return handlesTabulation;
  }

  @Override
  public boolean isEditing() {
    return false;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isNullable() {
    return true;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return node != null && getElement().isOrHasChild(node);
  }

  @Override
  public boolean isValueChanged() {
    for (MultiSelector multi : widgetMap.values()) {
      if (multi != null && multi.isValueChanged()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onClick(ClickEvent event) {
    event.stopPropagation();

    if (!DataUtils.isId(id)) {
      return;
    }
    refresh();

    if (Objects.nonNull(inputRelationsInterceptor)) {
      inputRelationsInterceptor.onOpen();
    }
    Global.inputWidget(Localized.dictionary().relations(), table, new InputCallback() {
      @Override
      public void onAdd() {
        addRelations();
      }

      @Override
      public void onClose(CloseCallback closeCallback) {
        if (isValueChanged()) {
          Global.decide(Localized.dictionary().relations(),
              Lists.newArrayList(Localized.dictionary().changedValues() + BeeConst.CHAR_SPACE
                  + getLabel(), Localized.dictionary().saveChanges()),
              new DecisionCallback() {
                @Override
                public void onConfirm() {
                  closeCallback.onSave();
                }

                @Override
                public void onDeny() {
                  if (Objects.nonNull(inputRelationsInterceptor)) {
                    inputRelationsInterceptor.onClose();
                  }
                  closeCallback.onClose();
                }
              }, DialogConstants.DECISION_YES);
        } else {
          if (Objects.nonNull(inputRelationsInterceptor)) {
            inputRelationsInterceptor.onClose();
          }
          InputCallback.super.onClose(closeCallback);
        }
      }

      @Override
      public void onSuccess() {
        Collection<RowChildren> relations = getRowChildren(false);

        if (!BeeUtils.isEmpty(relations)) {
          Queries.updateChildren(view, id, relations, result -> requery(result, id));
        }
        if (Objects.nonNull(inputRelationsInterceptor)) {
          inputRelationsInterceptor.onSave();
        }
      }
    }, null, null, EnumSet.of(Action.ADD));
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    requery(event.getRow(), event.getRowId());
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (!event.hasView(STORAGE) || !DataUtils.isId(id)) {
      return;
    }
    if (event.getRow() == null) {
      return;
    }
    if (event.hasSourceId(getId())) {
      return;
    }
    if (!Objects.equals(id, Data.getLong(STORAGE, event.getRow(), column))) {
      return;
    }
    requery(null, id);
  }

  @Override
  public void onSummaryChange(SummaryChangeEvent event) {
    SummaryChangeEvent.maybeFire(this);
  }

  public void refresh() {
    for (String col : widgetMap.keySet()) {
      MultiSelector multi = widgetMap.get(col);

      if (ids.containsKey(col)) {
        if (multi == null) {
          multi = createMultiSelector(col, null);
        }
        if (multi != null) {
          multi.setIds(ids.get(col));
        }
      } else if (multi != null) {
        multi.clearValue();
      }
    }
    setParentExclusion();
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  public void requery(IsRow row, final Long parent) {
    reset();

    if (DataUtils.isId(parent)) {
      Queries.getRowSet(STORAGE, null, Filter.equals(column, parent), result -> {
        for (int i = 0; i < result.getNumberOfColumns(); i++) {
          String col = result.getColumnId(i);

          if (BeeUtils.same(col, column)) {
            continue;
          }
          for (BeeRow beeRow : result) {
            Long relId = beeRow.getLong(i);

            if (DataUtils.isId(relId)) {
              ids.put(col, relId);
            }
          }
        }
        Runnable exec = () -> {
          id = parent;

          if (inline) {
            refresh();
          }
        };
        if (ids.containsKey(COL_RELATION)) {
          Queries.getRowSet(STORAGE, Collections.singletonList(column),
              Filter.idIn(ids.get(COL_RELATION)), res -> {
                for (BeeRow beeRow : res) {
                  Long relId = beeRow.getLong(0);

                  if (DataUtils.isId(relId)) {
                    ids.put(column, relId);
                  }
                }
                exec.run();
              });
        } else {
          exec.run();
        }
      });
    } else if (!rowProperties.isEmpty() && DataUtils.isNewRow(row)) {
      for (Map.Entry<String, String> entry : rowProperties.entrySet()) {
        String col = entry.getKey();
        String value = row.getProperty(entry.getValue());

        if (!BeeUtils.isEmpty(value)) {
          MultiSelector selector = widgetMap.get(col);
          if (selector != null) {
            selector.setIds(value);
            selector.setOldValue(null);
          }
        }
      }
    }
  }

  public void reset() {
    for (MultiSelector multi : widgetMap.values()) {
      if (multi != null) {
        multi.clearValue();
      }
    }
    ids.clear();
    id = null;
  }

  @Override
  public void setAccessKey(char key) {
  }

  @Override
  public void setEditing(boolean editing) {
  }

  @Override
  public void setEnabled(boolean enabled) {
    for (MultiSelector multi : widgetMap.values()) {
      if (multi != null) {
        UiHelper.enableAndStyle(multi, enabled
            && !blockedRelations.contains(multi.getOracle().getViewName()));
      }
    }
    this.enabled = enabled;
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      getElement().focus();
    } else {
      getElement().blur();
    }
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
    this.handlesTabulation = handlesTabulation;
  }

  public void setInputRelationsInterceptor(InputRelationsInterceptor interceptor) {
    this.inputRelationsInterceptor = interceptor;
  }

  @Override
  public void setNullable(boolean nullable) {
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;

    if (isAttached()) {
      register();
    }
  }

  public void setSelectorHandler(SelectorEvent.Handler selectorHandler) {
    this.handler = selectorHandler;

    for (MultiSelector multi : widgetMap.values()) {
      if (multi != null) {
        registerSelectorHandler(multi);
      }
    }
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;

    for (MultiSelector multi : widgetMap.values()) {
      if (multi != null) {
        multi.setSummarize(summarize);
      }
    }
  }

  @Override
  public void setTabIndex(int index) {
    getElement().setTabIndex(index);
  }

  @Override
  public void setValue(String value) {
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry, Element source) {
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return null;
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return validate(checkForNull);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    register();
  }

  @Override
  protected void onUnload() {
    unregister();
    super.onUnload();
  }

  private void addRelations() {
    if (!isEnabled()) {
      return;
    }
    final ListBox listBox = new ListBox(true);

    for (String col : widgetMap.keySet()) {
      if (widgetMap.get(col) == null) {
        listBox.addItem(Data.getViewCaption(Data.getColumnRelation(STORAGE, col)), col);
      }
    }
    if (listBox.isEmpty()) {
      Global.showInfo(Localized.dictionary().noData());
      return;
    } else if (listBox.getItemCount() > 30) {
      listBox.setVisibleItemCount(30);
    } else {
      listBox.setAllVisible();
    }
    Global.inputWidget(Localized.dictionary().newRelation(), listBox, () -> {
      for (int i = 0; i < listBox.getItemCount(); i++) {
        OptionElement optionElement = listBox.getOptionElement(i);

        if (optionElement.isSelected()) {
          createMultiSelector(optionElement.getValue(), null);
        }
      }
    });
  }

  private MultiSelector createMultiSelector(String col, Relation rel) {
    String viewName = Data.getColumnRelation(STORAGE, col);
    Relation relation = rel;

    if (relation == null) {
      relation = Data.getRelation(viewName);

      if (relation == null) {
        LogUtils.getRootLogger().severe("Missing relation info:", viewName);
        return null;
      }
      if (!relation.getAttributes().containsKey(UiConstants.ATTR_PROPERTY)) {
        relation.getAttributes().put(UiConstants.ATTR_PROPERTY,
            PFX_RELATED + relation.getViewName());
      }
    }
    List<String> cols = relation.getOriginalRenderColumns();

    if (BeeUtils.isEmpty(cols)) {
      cols = relation.getChoiceColumns();
    }
    MultiSelector multi = MultiSelector.autonomous(relation,
        RendererFactory.createRenderer(viewName, cols));

    multi.addSelectorHandler(this);
    registerSelectorHandler(multi);
    multi.setWidth("100%");

    multi.setSummarize(summarize());
    multi.addSummaryChangeHandler(this);

    int c = inline ? table.insertRow(table.getRowCount() - 1) : table.getRowCount();

    table.setText(c, 0, BeeUtils.notEmpty(relation.getLabel(), Data.getViewCaption(viewName)));
    table.setWidget(c, 1, multi);

    widgetMap.put(col, multi);

    String property = relation.getAttributes().get(UiConstants.ATTR_PROPERTY);
    if (!BeeUtils.isEmpty(property)) {
      rowProperties.put(col, property);
    }
    setParentExclusion();
    setEnabled(isEnabled());

    return multi;
  }

  private void register() {
    unregister();

    if (!BeeUtils.isEmpty(getParentId())) {
      eventRegistry.add(BeeKeeper.getBus().registerRowInsertHandler(this, false));
      eventRegistry.add(BeeKeeper.getBus().registerParentRowHandler(getParentId(), this, false));
    }
  }

  private void registerSelectorHandler(MultiSelector multi) {
    HandlerRegistration registration = registry.get(multi);

    if (registration != null) {
      registration.removeHandler();
    }
    if (handler != null) {
      registry.put(multi, multi.addSelectorHandler(handler));
    }
  }

  private void setParentExclusion() {
    MultiSelector self = widgetMap.get(column);

    if (Objects.nonNull(self)) {
      self.setAdditionalFilter(DataUtils.isId(id) ? Filter.idNotIn(Collections.singleton(id))
          : null);
    }
  }

  private void unregister() {
    EventUtils.clearRegistry(eventRegistry);
  }
}
