package com.butent.bee.client.modules.crm;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.crm.CrmConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.Autocomplete;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.AutocompleteEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.layout.TabbedPages.SelectionOrigin;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.AbstractFormInterceptor;
import com.butent.bee.client.ui.FormFactory.FormInterceptor;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class DocumentDataForm extends AbstractFormInterceptor
    implements ClickHandler, SelectionHandler<Pair<Integer, SelectionOrigin>> {

  private class TinyEditor {

    private JavaScriptObject tiny;
    private String deferedContent;

    public void doDefered() {
      setContent(tiny, BeeUtils.nvl(deferedContent, ""));
    }

    public String getContent() {
      if (isActive()) {
        return getContent(tiny);
      }
      return null;
    }

    public void init(String editorId) {
      Assert.state(!isActive());
      Assert.notEmpty(editorId);

      JavaScriptObject jso = JavaScriptObject.createObject();
      JsUtils.setProperty(jso, "mode", "exact");
      JsUtils.setProperty(jso, "elements", editorId);
      JsUtils.setProperty(jso, "language", Localized.getConstants().languageTag());
      JsUtils.setProperty(jso, "plugins", "advlist lists image charmap hr pagebreak searchreplace "
          + "visualblocks visualchars code fullscreen table template paste textcolor");
      JsUtils.setProperty(jso, "toolbar", "fullscreen | undo redo | styleselect "
          + "| bold italic underline | alignleft aligncenter alignright alignjustify "
          + "| forecolor backcolor | bullist numlist outdent indent | fontselect fontsizeselect");
      JsUtils.setProperty(jso, "image_advtab", true);
      JsUtils.setProperty(jso, "paste_data_images", true);
      JsUtils.setProperty(jso, "pagebreak_separator",
          "<div style=\"page-break-before:always;\"></div>");

      JsArrayMixed templateArray = JavaScriptObject.createArray().cast();

      JavaScriptObject template = JavaScriptObject.createObject();
      JsUtils.setProperty(template, "title", Localized.getConstants().group());
      JsUtils.setProperty(template, "content",
          "<p><table style=\"border-collapse:collapse;\"><tbody>"
              + "<!--{CriteriaGroups}-->"
              + "<tr><td colspan=\"3\" style=\"border:1px solid black;\">{Name}</td></tr>"
              + "<!--{Criteria}--><tr><td style=\"border:1px solid black;\">{Criterion}</td>"
              + "<td style=\"border:1px solid black;\">{Value}</td></tr>"
              + "<!--{Criteria}--><!--{CriteriaGroups}--></tbody></table></p>");

      templateArray.push(template);

      JsUtils.setProperty(jso, "templates", templateArray);

      initEditor(jso, this);
    }

    public boolean isActive() {
      return tiny != null;
    }

    public boolean isDirty() {
      if (isActive()) {
        return isDirty(tiny);
      }
      return false;
    }

    public void setContent(String content) {
      if (isActive()) {
        setContent(tiny, BeeUtils.nvl(content, ""));
      } else {
        deferedContent = content;
      }
    }

    private native String getContent(JavaScriptObject editor) /*-{
      return editor.getContent();
    }-*/;

    private native void initEditor(JavaScriptObject object, TinyEditor ed) /*-{
      object.init_instance_callback = function(editor) {
        ed.@com.butent.bee.client.modules.crm.DocumentDataForm.TinyEditor::tiny = editor;
        ed.@com.butent.bee.client.modules.crm.DocumentDataForm.TinyEditor::doDefered()();
      };
      $wnd.tinymce.init(object);
    }-*/;

    private native boolean isDirty(JavaScriptObject editor) /*-{
      return editor.isDirty();
    }-*/;

    private native void setContent(JavaScriptObject editor, String content) /*-{
      editor.setContent(content);
      editor.isNotDirty = 1;
    }-*/;
  }

  private final class AutocompleteFilter implements AutocompleteEvent.Handler {

    private final String source;
    private final String criterion;

    private AutocompleteFilter(String source, String criterion) {
      this.source = source;
      this.criterion = criterion;
    }

    @Override
    public void onDataSelector(AutocompleteEvent event) {
      if (event.getState() == State.OPEN) {
        CompoundFilter flt = Filter.and();

        for (String name : new String[] {COL_DOCUMENT_CATEGORY, COL_DOCUMENT_DATA}) {
          Long id = getLongValue(name);

          if (DataUtils.isId(id)) {
            if (BeeUtils.same(name, COL_DOCUMENT_CATEGORY)) {
              flt.add(Filter.isEqual(name, Value.getValue(id)));
            } else {
              flt.add(Filter.isNotEqual(name, Value.getValue(id)));
            }
          }
        }
        if (BeeUtils.isEmpty(source)) {
          flt.add(Filter.isNull(COL_CRITERIA_GROUP_NAME));

          if (!BeeUtils.isEmpty(criterion)) {
            flt.add(Filter.isEqual(COL_CRITERION_NAME, Value.getValue(criterion)));
          }
        } else if (!BeeUtils.same(source, COL_CRITERIA_GROUP_NAME)) {
          if (groupsGrid != null) {
            flt.add(Filter.isEqual(COL_CRITERIA_GROUP_NAME,
                groupsGrid.getPresenter().getActiveRow().getValue(groupsGrid.getPresenter()
                    .getGridView().getDataIndex(COL_CRITERIA_GROUP_NAME))));
          }
          if (BeeUtils.same(source, COL_CRITERION_VALUE) && criteriaGrid != null) {
            flt.add(Filter.isEqual(COL_CRITERION_NAME,
                criteriaGrid.getPresenter().getActiveRow().getValue(criteriaGrid.getPresenter()
                    .getGridView().getDataIndex(COL_CRITERION_NAME))));
          }
        }
        event.getSelector().setAdditionalFilter(flt);
      }
    }
  }

  private HasWidgets panel;
  private Long groupId;
  private final Map<String, String> criteriaHistory = Maps.newLinkedHashMap();
  private final Map<String, Editor> criteria = Maps.newLinkedHashMap();
  private final Map<String, Long> ids = Maps.newHashMap();

  private ChildGrid groupsGrid;
  private ChildGrid criteriaGrid;

  private final TinyEditor tinyEditor = new TinyEditor();

  private final GridInterceptor childInterceptor = new AbstractGridInterceptor() {
    @Override
    public void afterCreateEditor(String source, Editor editor, boolean embedded) {
      if (editor instanceof Autocomplete) {
        ((Autocomplete) editor).addAutocompleteHandler(new AutocompleteFilter(source, null));
      }
    }

    @Override
    public boolean ensureRelId(final IdCallback callback) {
      ensureDataId(null, callback);
      return true;
    }
  };

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, "MainCriteriaEditor")) {
      widget.asWidget().addDomHandler(this, ClickEvent.getType());

    } else if (widget instanceof HasWidgets && BeeUtils.same(name, "MainCriteriaContainer")) {
      panel = (HasWidgets) widget;

    } else if (widget instanceof ChildGrid) {
      ChildGrid grid = (ChildGrid) widget;

      if (BeeUtils.same(name, TBL_CRITERIA_GROUPS)) {
        groupsGrid = grid;
        grid.setGridInterceptor(childInterceptor);

      } else if (BeeUtils.same(name, TBL_CRITERIA)) {
        criteriaGrid = grid;
        grid.setGridInterceptor(childInterceptor);
      }
    } else if (widget instanceof TabbedPages) {
      ((TabbedPages) widget).addSelectionHandler(this);
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    if (!forced) {
      save(result);
    }
  }

  @Override
  public void afterUpdateRow(IsRow result) {
    save(result);
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.PRINT) {
      String content = tinyEditor.isActive()
          ? tinyEditor.getContent() : getStringValue(COL_DOCUMENT_CONTENT);

      if (BeeUtils.isEmpty(content)) {
        getFormView().notifyWarning(Localized.getConstants().documentContentIsEmpty());
      } else {
        parseContent(content, new Consumer<String>() {
          @Override
          public void accept(String input) {
            Printer.print(input, null);
          }
        });
      }
      return false;
    }
    return true;
  }

  @Override
  public FormInterceptor getInstance() {
    return new DocumentDataForm();
  }

  @Override
  public void onClick(ClickEvent event) {
    LocalizableConstants loc = Localized.getConstants();

    Global.inputCollection(loc.mainCriteria(), loc.name(), true,
        criteria.keySet(), new Consumer<Collection<String>>() {
          @Override
          public void accept(Collection<String> collection) {
            Map<String, Editor> oldCriteria = Maps.newHashMap(criteria);
            criteria.clear();

            for (String crit : collection) {
              Editor input = oldCriteria.get(crit);

              if (input == null) {
                input = createAutocomplete("DistinctCriterionValues", COL_CRITERION_VALUE, crit);
              }
              criteria.put(crit, input);
            }
            render();
          }
        }, new Supplier<Editor>() {
          @Override
          public Editor get() {
            return createAutocomplete("DistinctCriteria", COL_CRITERION_NAME, null);
          }
        });
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
    LocalizableConstants loc = Localized.getConstants();
    List<String> warnings = Lists.newArrayList();

    if (save(null)) {
      warnings.add(loc.mainCriteria());
    }
    if (tinyEditor.isDirty()) {
      warnings.add(loc.content());
    }
    if (!BeeUtils.isEmpty(warnings)) {
      messages.add(BeeUtils.joinWords(loc.changedValues(), warnings));
    }
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    includeContent(event.getColumns(), null, null, event.getValues());
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    includeContent(event.getColumns(),
        event.getOldRow().getString(getDataIndex(COL_DOCUMENT_CONTENT)),
        event.getOldValues(), event.getNewValues());

    if (BeeUtils.isEmpty(event.getColumns())) {
      save(getActiveRow());
    }
  }

  @Override
  public void onSelection(SelectionEvent<Pair<Integer, SelectionOrigin>> event) {
    if (!tinyEditor.isActive() && event.getSource() instanceof TabbedPages) {
      Widget content = getFormView().getWidgetByName(COL_DOCUMENT_CONTENT);

      if (Objects.equals(((TabbedPages) event.getSource()).getSelectedWidget(), content)) {
        if (content instanceof HasOneWidget) {
          tinyEditor.init(DomUtils.getId(((HasOneWidget) content).getWidget()));
        }
      }
    }
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    requery(row);
    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    requery(newRow);
  }

  protected void parseContent(String content, final Consumer<String> consumer) {
    final List<String> parts = Lists.newArrayList(Splitter
        .on("<!--{" + TBL_CRITERIA_GROUPS + "}-->").split(content));

    final Holder<Integer> holder = Holder.of(parts.size());

    BiConsumer<Integer, String> executor = new BiConsumer<Integer, String>() {
      @Override
      public void accept(Integer index, String value) {
        parts.set(index, value);
        holder.set(holder.get() - 1);

        if (!BeeUtils.isPositive(holder.get())) {
          StringBuilder sb = new StringBuilder();

          for (String part : parts) {
            sb.append(part);
          }
          String result = sb.toString();

          for (Entry<String, Editor> entry : criteria.entrySet()) {
            result = BeeUtils.replace(result.replace("&Scaron;", "Š").replace("&scaron;", "š"),
                "{" + entry.getKey() + "}", entry.getValue().getNormalizedValue());
          }
          consumer.accept(result);
        }
      }
    };
    for (int i = 0; i < parts.size(); i++) {
      String part = parts.get(i);

      if (i % 2 > 0 && i < parts.size() - 1) {
        parseGroup(part, i, executor);
      } else {
        executor.accept(i, part);
      }
    }
  }

  private Autocomplete createAutocomplete(String viewName, String column, String value) {
    Autocomplete input = Autocomplete.create(Relation.create(viewName,
        Lists.newArrayList(column)), true);

    input.addAutocompleteHandler(new AutocompleteFilter(null, value));
    return input;
  }

  private void ensureDataId(IsRow row, final IdCallback callback) {
    final FormView form = getFormView();
    final BeeRow newRow = DataUtils.cloneRow(row == null ? form.getActiveRow() : row);
    final int idx = form.getDataIndex(COL_DOCUMENT_DATA);
    Long dataId = newRow.getLong(idx);

    if (DataUtils.isId(dataId)) {
      callback.onSuccess(dataId);
    } else {
      Queries.insert(TBL_DOCUMENT_DATA, Data.getColumns(TBL_DOCUMENT_DATA,
          Lists.newArrayList(COL_DOCUMENT_CONTENT)), Lists.newArrayList((String) null), null,
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              long id = result.getId();
              newRow.setValue(idx, id);

              RowUpdateEvent.fire(BeeKeeper.getBus(), form.getViewName(), newRow);
              callback.onSuccess(id);

              Queries.update(form.getViewName(), newRow.getId(), COL_DOCUMENT_DATA,
                  Value.getValue(id));
            }
          });
    }
  }

  private void includeContent(List<BeeColumn> columns, String oldValue, List<String> oldValues,
      List<String> newValues) {

    if (tinyEditor.isDirty()) {
      columns.add(DataUtils.getColumn(COL_DOCUMENT_CONTENT, getFormView().getDataColumns()));
      newValues.add(tinyEditor.getContent());

      if (oldValues != null) {
        oldValues.add(oldValue);
      }
    }
  }

  private static String parseCriteria(String content, String group,
      Collection<Pair<String, String>> crit) {

    if (BeeUtils.isEmpty(crit)) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    List<String> parts = Splitter.on("<!--{" + TBL_CRITERIA + "}-->").splitToList(content);

    for (int i = 0; i < parts.size(); i++) {
      String part = parts.get(i).replace("{" + COL_CRITERIA_GROUP_NAME + "}", group);

      if (i % 2 > 0 && i < parts.size() - 1) {
        for (Pair<String, String> pair : crit) {
          sb.append(part.replace("{" + COL_CRITERION_NAME + "}", pair.getA())
              .replace("{" + COL_CRITERION_VALUE + "}", BeeUtils.nvl(pair.getB(), "")));
        }
      } else {
        sb.append(part);
      }
    }
    return sb.toString();
  }

  private void parseGroup(final String content, final Integer idx,
      final BiConsumer<Integer, String> executor) {
    if (groupsGrid == null) {
      executor.accept(idx, null);
      return;
    }
    GridView gridView = groupsGrid.getPresenter().getGridView();
    int nameIdx = gridView.getDataIndex(COL_CRITERIA_GROUP_NAME);
    final Map<Long, String> groups = Maps.newLinkedHashMap();

    for (IsRow row : gridView.getRowData()) {
      groups.put(row.getId(), row.getString(nameIdx));
    }
    if (BeeUtils.isEmpty(groups)) {
      executor.accept(idx, null);
      return;
    }
    Queries.getRowSet(TBL_CRITERIA, null, Filter.any(COL_CRITERIA_GROUP, groups.keySet()),
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            Multimap<Long, Pair<String, String>> crit = LinkedHashMultimap.create();
            int grpIdx = result.getColumnIndex(COL_CRITERIA_GROUP);
            int crtIdx = result.getColumnIndex(COL_CRITERION_NAME);
            int valIdx = result.getColumnIndex(COL_CRITERION_VALUE);

            for (BeeRow row : result.getRows()) {
              crit.put(row.getLong(grpIdx), Pair.of(row.getString(crtIdx), row.getString(valIdx)));
            }
            StringBuilder sb = new StringBuilder();

            for (Long group : groups.keySet()) {
              sb.append(parseCriteria(content, groups.get(group), crit.get(group)));
            }
            executor.accept(idx, sb.toString());
          }
        });
  }

  private void render() {
    if (panel == null) {
      getHeaderView().clearCommandPanel();
      return;
    }
    panel.clear();

    if (criteria.size() > 0) {
      HtmlTable table = new HtmlTable();
      table.setColumnCellStyles(0, "text-align:right");
      int c = 0;

      for (Entry<String, Editor> entry : criteria.entrySet()) {
        table.setText(c, 0, entry.getKey());
        table.setWidget(c++, 1, entry.getValue().asWidget());
      }
      panel.add(table);
    }
  }

  private void requery(IsRow row) {
    tinyEditor.setContent(row.getString(getDataIndex(COL_DOCUMENT_CONTENT)));
    criteriaHistory.clear();
    criteria.clear();
    ids.clear();
    groupId = null;
    render();
    Long dataId = row.getLong(getDataIndex(COL_DOCUMENT_DATA));

    if (!DataUtils.isId(dataId)) {
      return;
    }
    Queries.getRowSet(VIEW_MAIN_CRITERIA, null,
        Filter.isEqual(COL_DOCUMENT_DATA, Value.getValue(dataId)),
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            if (result.getNumberOfRows() > 0) {
              groupId = result.getRow(0).getId();

              for (BeeRow crit : result.getRows()) {
                String name = Data.getString(VIEW_MAIN_CRITERIA, crit, COL_CRITERION_NAME);

                if (!BeeUtils.isEmpty(name)) {
                  String value = Data.getString(VIEW_MAIN_CRITERIA, crit, COL_CRITERION_VALUE);

                  Autocomplete box = createAutocomplete("DistinctCriterionValues",
                      COL_CRITERION_VALUE, name);

                  box.setValue(value);

                  criteriaHistory.put(name, value);
                  criteria.put(name, box);
                  ids.put(name, Data.getLong(VIEW_MAIN_CRITERIA, crit, "ID"));
                }
              }
              render();
            }
          }
        });
  }

  private boolean save(final IsRow row) {
    final Map<String, String> newValues = Maps.newLinkedHashMap();
    Map<Long, String> changedValues = Maps.newHashMap();
    CompoundFilter flt = Filter.or();
    final Holder<Integer> holder = Holder.of(0);

    for (String crit : criteria.keySet()) {
      String value = criteria.get(crit).getValue();
      value = BeeUtils.isEmpty(value) ? null : value;
      Long id = ids.get(crit);

      if (!criteriaHistory.containsKey(crit) || !Objects.equals(value, criteriaHistory.get(crit))) {
        if (DataUtils.isId(id)) {
          changedValues.put(id, value);
        } else {
          newValues.put(crit, value);
        }
        holder.set(holder.get() + 1);
      }
    }
    for (String crit : ids.keySet()) {
      if (!criteria.containsKey(crit)) {
        flt.add(Filter.compareId(ids.get(crit)));
      }
    }
    if (!flt.isEmpty()) {
      holder.set(holder.get() + 1);
    }
    if (row == null) {
      return BeeUtils.isPositive(holder.get());
    }
    final ScheduledCommand scheduler = new ScheduledCommand() {
      @Override
      public void execute() {
        holder.set(holder.get() - 1);

        if (!BeeUtils.isPositive(holder.get())) {
          Queries.getRow(getViewName(), row.getId(), new RowUpdateCallback(getViewName()) {
            @Override
            public void onSuccess(BeeRow result) {
              super.onSuccess(result);
              getGridView().getGrid().refresh();
            }
          });
        }
      }
    };
    if (!BeeUtils.isEmpty(newValues)) {
      final Consumer<Long> consumer = new Consumer<Long>() {
        @Override
        public void accept(Long id) {
          for (Entry<String, String> entry : newValues.entrySet()) {
            Queries.insert(TBL_CRITERIA, Data.getColumns(TBL_CRITERIA,
                Lists.newArrayList(COL_CRITERIA_GROUP, COL_CRITERION_NAME, COL_CRITERION_VALUE)),
                Lists.newArrayList(BeeUtils.toString(id), entry.getKey(), entry.getValue()), null,
                new RowCallback() {
                  @Override
                  public void onSuccess(BeeRow result) {
                    scheduler.execute();
                  }
                });
          }
        }
      };
      if (!DataUtils.isId(groupId)) {
        ensureDataId(row, new IdCallback() {
          @Override
          public void onSuccess(Long dataId) {
            Queries.insert(TBL_CRITERIA_GROUPS,
                Data.getColumns(TBL_CRITERIA_GROUPS, Lists.newArrayList(COL_DOCUMENT_DATA)),
                Lists.newArrayList(BeeUtils.toString(dataId)), null, new RowCallback() {
                  @Override
                  public void onSuccess(BeeRow result) {
                    consumer.accept(result.getId());
                  }
                });
          }
        });
      } else {
        consumer.accept(groupId);
      }
    }
    if (!BeeUtils.isEmpty(changedValues)) {
      for (Entry<Long, String> entry : changedValues.entrySet()) {
        Queries.update(TBL_CRITERIA, Filter.compareId(entry.getKey()),
            COL_CRITERION_VALUE, new TextValue(entry.getValue()), new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                scheduler.execute();
              }
            });
      }
    }
    if (!flt.isEmpty()) {
      Queries.delete(TBL_CRITERIA, flt, new IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          scheduler.execute();
        }
      });
    }
    return true;
  }
}
