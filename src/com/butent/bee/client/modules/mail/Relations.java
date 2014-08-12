package com.butent.bee.client.modules.mail;

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
import com.google.gwt.xml.client.Document;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HandlesValueChange;
import com.butent.bee.client.ui.HasFosterParent;
import com.butent.bee.client.ui.HasRowChildren;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.filter.Filter;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Relations extends Flow implements Editor, ClickHandler, SelectorEvent.Handler,
    ParentRowEvent.Handler, HasFosterParent, HasRowChildren, HandlesValueChange {

  private static final String STORAGE = TBL_RELATIONS;

  private final String column;
  private final String view;
  private final boolean inline;

  private final HtmlTable table = new HtmlTable();

  private boolean enabled = true;
  private String options;
  private boolean handlesTabulation;

  private com.google.web.bindery.event.shared.HandlerRegistration parentRowReg;
  private String parentId;

  final Multimap<String, Long> ids = HashMultimap.create();
  final Map<String, MultiSelector> widgetMap = new HashMap<>();
  final Map<MultiSelector, HandlerRegistration> registry = new HashMap<>();

  private Long id;
  private SelectorEvent.Handler handler;

  public Relations(String column, boolean inline, Collection<Relation> relations,
      List<String> defaultRelations, List<String> blockedRelations) {

    DataInfo info = Data.getDataInfo(STORAGE);

    this.column = Assert.notEmpty(column);
    this.view = Assert.notEmpty(info.getRelation(column));
    this.inline = inline;

    if (inline) {
      FaLabel add = new FaLabel(FontAwesome.PLUS);

      add.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent ev) {
          addRelations();
        }
      });
      table.setWidget(0, 0, add);
      add(table);

    } else {
      FaLabel face = new FaLabel(FontAwesome.CHAIN);
      face.addClickHandler(this);
      add(face);
      table.setWidth("600px");
    }
    table.setColumnCellStyles(0, "text-align:right; white-space:nowrap;");
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
  public void clearValue() {
  }

  @Override
  public Collection<RowChildren> getChildrenForInsert() {
    if (inline) {
      return getRowChildren();
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

  public Collection<RowChildren> getRowChildren() {
    List<RowChildren> relations = new ArrayList<>();

    for (Entry<String, MultiSelector> entry : widgetMap.entrySet()) {
      MultiSelector multi = entry.getValue();

      if (multi != null && multi.isValueChanged()) {
        relations.add(RowChildren.create(STORAGE, column, id, entry.getKey(),
            DataUtils.buildIdList(multi.getIds())));
      }
    }
    return relations;
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

    Global.inputWidget(Localized.getConstants().relations(), table, new InputCallback() {
      @Override
      public void onAdd() {
        addRelations();
      }

      @Override
      public void onClose(final CloseCallback closeCallback) {
        if (isValueChanged()) {
          Global.decide(Localized.getConstants().relations(),
              Lists.newArrayList(Localized.getConstants().changedValues() + BeeConst.CHAR_SPACE
                  + getLabel(), Localized.getConstants().saveChanges()),
              new DecisionCallback() {
                @Override
                public void onConfirm() {
                  closeCallback.onSave();
                }

                @Override
                public void onDeny() {
                  closeCallback.onClose();
                }
              }, DialogConstants.DECISION_YES);
        } else {
          super.onClose(closeCallback);
        }
      }

      @Override
      public void onSuccess() {
        Collection<RowChildren> relations = getRowChildren();

        if (!BeeUtils.isEmpty(relations)) {
          Queries.updateChildren(view, id, relations, new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              requery(id);
            }
          });
        }
      }
    }, null, null, EnumSet.of(Action.ADD));
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    requery(event.getRowId());
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
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  public void requery(final Long parent) {
    reset();

    if (!DataUtils.isId(parent)) {
      return;
    }
    Queries.getRowSet(STORAGE, null, Filter.equals(column, parent), new RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
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
        if (ids.containsKey(COL_RELATION)) {
          Queries.getRowSet(STORAGE, Lists.newArrayList(column),
              Filter.idIn(ids.get(COL_RELATION)), new RowSetCallback() {
                @Override
                public void onSuccess(BeeRowSet res) {
                  for (BeeRow beeRow : res) {
                    Long relId = beeRow.getLong(0);

                    if (DataUtils.isId(relId)) {
                      ids.put(column, relId);
                    }
                  }
                  if (inline) {
                    refresh();
                  }
                  id = parent;
                }
              });
        } else {
          if (inline) {
            refresh();
          }
          id = parent;
        }
      }
    });
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
            && !BeeUtils.same(multi.getOracle().getViewName(), MailConstants.TBL_MESSAGES));
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
      Global.showInfo(Localized.getConstants().noData());
      return;
    } else if (listBox.getItemCount() > 30) {
      listBox.setVisibleItemCount(30);
    } else {
      listBox.setAllVisible();
    }
    Global.inputWidget(Localized.getConstants().newRelation(), listBox, new InputCallback() {
      @Override
      public void onSuccess() {
        for (int i = 0; i < listBox.getItemCount(); i++) {
          OptionElement optionElement = listBox.getOptionElement(i);

          if (optionElement.isSelected()) {
            createMultiSelector(optionElement.getValue(), null);
          }
        }
      }
    });
  }

  private MultiSelector createMultiSelector(String col, Relation rel) {
    String viewName = Data.getColumnRelation(STORAGE, col);
    Relation relation = rel;

    if (relation == null) {
      String relationInfo = Data.getDataInfo(viewName).getRelationInfo();

      if (!BeeUtils.isEmpty(relationInfo)) {
        Document doc = XmlUtils.parse(relationInfo);

        if (doc != null) {
          Map<String, String> attributes = XmlUtils.getAttributes(doc.getDocumentElement());
          attributes.put(UiConstants.ATTR_VIEW_NAME, viewName);
          relation = FormWidget.createRelation(null, attributes,
              XmlUtils.getChildrenElements(doc.getDocumentElement()), Relation.RenderMode.SOURCE);
        }
      }
      if (relation == null) {
        LogUtils.getRootLogger().severe("Missing relation info:", viewName);
        return null;
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

    int c = inline ? table.insertRow(table.getRowCount() - 1) : table.getRowCount();

    table.setText(c, 0, BeeUtils.notEmpty(relation.getLabel(), Data.getViewCaption(viewName)));
    table.setWidget(c, 1, multi);

    widgetMap.put(col, multi);
    setEnabled(isEnabled());

    return multi;
  }

  private com.google.web.bindery.event.shared.HandlerRegistration getParentRowReg() {
    return parentRowReg;
  }

  private void register() {
    unregister();
    if (!BeeUtils.isEmpty(getParentId())) {
      setParentRowReg(BeeKeeper.getBus().registerParentRowHandler(getParentId(), this, false));
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

  private void setParentRowReg(com.google.web.bindery.event.shared.HandlerRegistration
      handlerRegistration) {
    this.parentRowReg = handlerRegistration;
  }

  private void unregister() {
    if (getParentRowReg() != null) {
      getParentRowReg().removeHandler();
      setParentRowReg(null);
    }
  }
}
