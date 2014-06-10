package com.butent.bee.client.modules.documents;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

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
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.layout.TabbedPages.SelectionOrigin;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BiFunction;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.State;
import com.butent.bee.shared.css.values.TextAlign;
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
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
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

    public void destroy(boolean automatic) {
      if (isActive()) {
        destroy(tiny, automatic);
        tiny = null;
      }
    }

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

      LocalizableConstants loc = Localized.getConstants();

      JavaScriptObject jso = JavaScriptObject.createObject();
      JsUtils.setProperty(jso, "mode", "exact");
      JsUtils.setProperty(jso, "elements", editorId);
      JsUtils.setProperty(jso, "language", loc.languageTag());
      JsUtils.setProperty(jso, "plugins", "advlist lists image charmap hr pagebreak searchreplace "
          + "visualblocks visualchars code fullscreen table template paste textcolor");
      JsUtils.setProperty(jso, "toolbar", "fullscreen | undo redo | styleselect "
          + "| bold italic underline | alignleft aligncenter alignright alignjustify "
          + "| forecolor backcolor | bullist numlist outdent indent | fontselect fontsizeselect");
      JsUtils.setProperty(jso, "image_advtab", true);
      JsUtils.setProperty(jso, "paste_data_images", true);
      JsUtils.setProperty(jso, "pagebreak_separator",
          "<div style=\"page-break-before:always;\"></div>");

      BiFunction<String, String, String> td = new BiFunction<String, String, String>() {
        @Override
        public String apply(String input, String attributes) {
          String stylePrefix = "style=\"";

          return "<td style=\"border:1px solid black;"
              + (BeeUtils.startsWith(attributes, stylePrefix)
                  ? attributes.substring(stylePrefix.length())
                  : "\" " + BeeUtils.nvl(attributes, "")) + ">" + input + "</td>";
        }
      };
      JsArrayMixed templateArray = JavaScriptObject.createArray().cast();

      JavaScriptObject template = JavaScriptObject.createObject();
      JsUtils.setProperty(template, "title", loc.criteriaGroups());
      JsUtils.setProperty(template, "content",
          new StringBuilder("<p/><div><table style=\"border-collapse:collapse;\"><tbody>")
              .append("<!--{CriteriaGroups}--><tr>")
              .append(td.apply("{Name}", "colspan=\"3\""))
              .append("</tr><!--{Criteria}--><tr>")
              .append(td.apply("{Criterion}", null))
              .append(td.apply("{Value}", null))
              .append("</tr><!--{Criteria}-->")
              .append("<!--{CriteriaGroups}--></tbody></table></div><p/>").toString());

      templateArray.push(template);

      template = JavaScriptObject.createObject();
      JsUtils.setProperty(template, "title", loc.documentItems());
      JsUtils.setProperty(template, "content",
          new StringBuilder("<p/><div><table style=\"border-collapse:collapse;"
              + " border:1px solid black;\"><tbody>")
              .append("<tr style=\"text-align:center;\">")
              .append(td.apply(loc.ordinal(), null))
              .append(td.apply(loc.description(), null))
              .append(td.apply(loc.quantity(), null))
              .append(td.apply(loc.price(), null))
              .append(td.apply(loc.amount(), null))
              .append(td.apply(loc.vat(), null))
              .append(td.apply(loc.total(), null))
              .append("</tr><!--{DocumentItems}--><tr style=\"text-align:right;\">")
              .append(td.apply("{Index}", null))
              .append(td.apply("{Description}", "style=\"text-align:left;\""))
              .append(td.apply("{Quantity}", null))
              .append(td.apply("{Price}", null))
              .append(td.apply("{Amount}", null))
              .append(td.apply("{Vat}", null))
              .append(td.apply("{VatPlusAmount}", null))
              .append("</tr><!--{DocumentItems}--><tr style=\"text-align:right;\">")
              .append("<td style=\"text-align:left;\" colspan=\"2\">" + loc.totalOf() + "</td>")
              .append("<td>{QuantityAmount}</td>")
              .append("<td colspan=\"2\">{TotalAmount}</td>")
              .append("<td>{VatAmount}</td>")
              .append("<td>{Total}</td>")
              .append("</tr></tbody></table></div><p/>").toString());

      templateArray.push(template);

      template = JavaScriptObject.createObject();
      JsUtils.setProperty(template, "title", loc.content());
      JsUtils.setProperty(template, "content",
          new StringBuilder("<p/><div><!--{DocumentItems}-->")
              .append("<p><strong>{Index}. {Description}</strong></p>")
              .append("<p>{Content}</p>")
              .append("<!--{DocumentItems}--></div><p/>").toString());

      templateArray.push(template);

      template = JavaScriptObject.createObject();
      JsUtils.setProperty(template, "title", loc.company());
      List<String> descr = Lists.newArrayList();

      for (BeeColumn col : Data.getColumns(ClassifierConstants.TBL_COMPANIES)) {
        descr.add("{" + ClassifierConstants.COL_COMPANY + col.getId() + "}");
      }
      JsUtils.setProperty(template, "description", BeeUtils.joinItems(descr));
      JsUtils.setProperty(template, "content",
          new StringBuilder("<p/>")
              .append("<p><strong>{CompanyName}</strong></p>")
              .append("<p>").append(loc.companyCode()).append(": {CompanyCode}</p>")
              .append("<p>").append(loc.address()).append(": {CompanyAddress}</p>")
              .append("<p>").append(loc.phone()).append(": {CompanyPhone}</p>")
              .append("<p>").append(loc.email()).append(": {CompanyEmail}</p>")
              .append("<p>").append("{CompanyWebsite}</p>")
              .append("<p/>").toString());

      templateArray.push(template);

      template = JavaScriptObject.createObject();
      JsUtils.setProperty(template, "title", loc.person());
      descr.clear();

      for (BeeColumn col : Data.getColumns(ClassifierConstants.TBL_PERSONS)) {
        descr.add("{" + ClassifierConstants.COL_PERSON + col.getId() + "}");
      }
      JsUtils.setProperty(template, "description", BeeUtils.joinItems(descr));
      JsUtils.setProperty(template, "content",
          new StringBuilder("<p/>")
              .append("<p><strong>{PersonFirstName} {PersonLastName}</strong></p>")
              .append("<p>").append(loc.phone()).append(": {PersonPhone}</p>")
              .append("<p>").append(loc.mobile()).append(": {PersonMobile}</p>")
              .append("<p>").append(loc.email()).append(": {PersonEmail}</p>")
              .append("<p/>").toString());

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

    private native void destroy(JavaScriptObject editor, boolean automatic) /*-{
      editor.destroy(automatic);
    }-*/;

    private native String getContent(JavaScriptObject editor) /*-{
      return editor.getContent();
    }-*/;

    private native void initEditor(JavaScriptObject object, TinyEditor ed) /*-{
      object.init_instance_callback = function(editor) {
        ed.@com.butent.bee.client.modules.documents.DocumentDataForm.TinyEditor::tiny = editor;
        ed.@com.butent.bee.client.modules.documents.DocumentDataForm.TinyEditor::doDefered()();
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

  private TabbedPages tabbedPages;

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

    @Override
    public GridInterceptor getInstance() {
      return null;
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

      if (BeeUtils.same(name, VIEW_CRITERIA_GROUPS)) {
        groupsGrid = grid;
        grid.setGridInterceptor(childInterceptor);

      } else if (BeeUtils.same(name, VIEW_CRITERIA)) {
        criteriaGrid = grid;
        grid.setGridInterceptor(childInterceptor);
      }
    } else if (widget instanceof TabbedPages) {
      tabbedPages = (TabbedPages) widget;
      tabbedPages.addSelectionHandler(this);
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
        parseContent(content, getLongValue(COL_DOCUMENT_DATA), new Consumer<String>() {
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
  public void beforeStateChange(State state, boolean modal) {
    if (state == State.CLOSED && modal && tinyEditor.isActive()) {
      tinyEditor.destroy(false);
    } else if (state == State.OPEN && modal && tabbedPages != null) {
      tabbedPages.selectPage(0, SelectionOrigin.SCRIPT);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new DocumentDataForm();
  }

  @Override
  public void onClick(ClickEvent event) {
    if (!getActiveRow().isEditable()) {
      return;
    }
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
        }, new Function<String, Editor>() {
          @Override
          public Editor apply(String value) {
            Editor editor = createAutocomplete("DistinctCriteria", COL_CRITERION_NAME, null);
            editor.setValue(value);
            return editor;
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
    if (tinyEditor.isDirty() && newRow.isEditable()) {
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
    if (!tinyEditor.isActive() && tabbedPages != null) {
      Widget content = getFormView().getWidgetByName(COL_DOCUMENT_CONTENT);

      if (Objects.equals(tabbedPages.getSelectedWidget(), content)) {
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

  @Override
  public void onUnload(FormView form) {
    tinyEditor.destroy(false);
    super.onUnload(form);
  }

  protected void parseContent(final String content, Long dataId, final Consumer<String> consumer) {
    Queries.getRowSet(VIEW_DATA_CRITERIA, null,
        Filter.equals(COL_DOCUMENT_DATA, dataId), new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            Multimap<String, Pair<String, String>> data = LinkedListMultimap.create();
            int grpIdx = result.getColumnIndex(COL_CRITERIA_GROUP_NAME);
            int crtIdx = result.getColumnIndex(COL_CRITERION_NAME);
            int valIdx = result.getColumnIndex(COL_CRITERION_VALUE);

            for (BeeRow row : result.getRows()) {
              data.put(row.getString(grpIdx), Pair.of(row.getString(crtIdx),
                  row.getString(valIdx)));
            }
            final List<String> groupBlocks = Lists.newArrayList(Splitter
                .on("<!--{" + VIEW_CRITERIA_GROUPS + "}-->").split(content));

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < groupBlocks.size(); i++) {
              String groupBlock = groupBlocks.get(i);

              if (i % 2 > 0 && i < groupBlocks.size() - 1) {
                List<String> criteriaBlocks = Splitter.on("<!--{" + VIEW_CRITERIA + "}-->")
                    .splitToList(groupBlock);

                for (String group : data.keySet()) {
                  if (BeeUtils.isEmpty(group)) {
                    continue;
                  }
                  for (int j = 0; j < criteriaBlocks.size(); j++) {
                    String criteriaBlock = criteriaBlocks.get(j)
                        .replace("{" + COL_CRITERIA_GROUP_NAME + "}", group);

                    if (j % 2 > 0 && j < criteriaBlocks.size() - 1) {
                      for (Pair<String, String> pair : data.get(group)) {
                        if (!BeeUtils.isEmpty(pair.getA())) {
                          sb.append(criteriaBlock.replace("{" + COL_CRITERION_NAME + "}",
                              pair.getA())
                              .replace("{" + COL_CRITERION_VALUE + "}",
                                  BeeUtils.nvl(pair.getB(), "")));
                        }
                      }
                    } else {
                      sb.append(criteriaBlock);
                    }
                  }
                }
              } else {
                sb.append(groupBlock);
              }
            }
            String parsed = sb.toString().replace("&Scaron;", "Š").replace("&scaron;", "š");
            Collection<Pair<String, String>> mainCriteria = data.get(null);

            if (!BeeUtils.isEmpty(mainCriteria)) {
              for (Pair<String, String> entry : mainCriteria) {
                parsed = parsed.replace("{" + entry.getA() + "}", BeeUtils.nvl(entry.getB(), ""));
              }
            }
            consumer.accept(parsed);
          }
        });
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
      Queries.insert(VIEW_DOCUMENT_DATA, Data.getColumns(VIEW_DOCUMENT_DATA,
          Lists.newArrayList(COL_DOCUMENT_CONTENT)), Lists.newArrayList((String) null), null,
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              long id = result.getId();
              newRow.setValue(idx, id);

              RowUpdateEvent.fire(BeeKeeper.getBus(), form.getViewName(), newRow);
              form.refreshChildWidgets(newRow);

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

  private void render() {
    if (panel == null) {
      getHeaderView().clearCommandPanel();
      return;
    }
    panel.clear();

    if (criteria.size() > 0) {
      int h = 0;
      FlowPanel labelContainer = new FlowPanel();
      labelContainer.getElement().getStyle().setMarginRight(5, Unit.PX);
      panel.add(labelContainer);

      FlowPanel inputContainer = new FlowPanel();
      inputContainer.addStyleName(StyleUtils.NAME_FLEXIBLE);
      panel.add(inputContainer);

      for (Entry<String, Editor> entry : criteria.entrySet()) {
        Label label = new Label(entry.getKey());
        StyleUtils.setTextAlign(label.getElement(), TextAlign.RIGHT);
        SimplePanel labelDiv = new SimplePanel(label);
        labelContainer.add(labelDiv);

        Widget editor = entry.getValue().asWidget();
        editor.setWidth("100%");
        SimplePanel editorDiv = new SimplePanel(editor);
        inputContainer.add(editorDiv);

        if (!BeeUtils.isPositive(h)) {
          h = BeeUtils.max(labelDiv.getElement().getClientHeight(),
              editorDiv.getElement().getClientHeight());

          if (!BeeUtils.isPositive(h)) {
            h = 20;
          }
          h += 5;
        }
        StyleUtils.setHeight(labelDiv, h);
        StyleUtils.setHeight(editorDiv, h);
      }
    }
  }

  private void requery(final IsRow row) {
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
    Queries.getRowSet(VIEW_DATA_CRITERIA, null,
        Filter.and(Filter.isEqual(COL_DOCUMENT_DATA, Value.getValue(dataId)),
            Filter.isNull(COL_CRITERIA_GROUP_NAME)),
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            if (result.getNumberOfRows() > 0) {
              groupId = result.getRow(0).getId();

              for (BeeRow crit : result.getRows()) {
                String name = Data.getString(VIEW_DATA_CRITERIA, crit, COL_CRITERION_NAME);

                if (!BeeUtils.isEmpty(name)) {
                  String value = Data.getString(VIEW_DATA_CRITERIA, crit, COL_CRITERION_VALUE);

                  Autocomplete box = createAutocomplete("DistinctCriterionValues",
                      COL_CRITERION_VALUE, name);

                  box.setEnabled(row.isEditable());
                  box.setStyleName("bee-disabled", !row.isEditable());
                  box.setValue(value);

                  criteriaHistory.put(name, value);
                  criteria.put(name, box);
                  ids.put(name, Data.getLong(VIEW_DATA_CRITERIA, crit, "ID"));
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
              GridView gridView = getGridView();
              if (gridView != null) {
                gridView.getGrid().refresh();
              }
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
            Queries.insert(VIEW_CRITERIA, Data.getColumns(VIEW_CRITERIA,
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
            Queries.insert(VIEW_CRITERIA_GROUPS,
                Data.getColumns(VIEW_CRITERIA_GROUPS, Lists.newArrayList(COL_DOCUMENT_DATA)),
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
        Queries.update(VIEW_CRITERIA, Filter.compareId(entry.getKey()),
            COL_CRITERION_VALUE, new TextValue(entry.getValue()), new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                scheduler.execute();
              }
            });
      }
    }
    if (!flt.isEmpty()) {
      Queries.delete(VIEW_CRITERIA, flt, new IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          scheduler.execute();
        }
      });
    }
    return true;
  }
}
