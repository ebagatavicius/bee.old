package com.butent.bee.client.modules.documents;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.UserInfo;
import com.butent.bee.client.composite.Autocomplete;
import com.butent.bee.client.composite.ChildSelector;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.event.logical.AutocompleteEvent;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.Relation.Caching;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentForm extends DocumentDataForm implements SelectorEvent.Handler {

  private final Button newTemplateButton = new Button(Localized.getConstants()
      .newDocumentTemplate(), new ClickHandler() {
    @Override
    public void onClick(ClickEvent event) {
      createTemplate();
    }
  });
  private ChildGrid itemsGrid;

  private final Map<String, ChildSelector> childSelectors = new HashMap<>();

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    super.afterCreateWidget(name, widget, callback);

    if (widget instanceof ChildSelector) {
      if (!BeeUtils.isEmpty(name)) {
        childSelectors.put(name, (ChildSelector) widget);
      }
      if (((ChildSelector) widget).hasRelatedView(TaskConstants.VIEW_TASKS)) {
        ((ChildSelector) widget).addSelectorHandler(this);
      } else if (((ChildSelector) widget).hasRelatedView(ServiceConstants.VIEW_SERVICE_OBJECTS)) {
        ((ChildSelector) widget).addSelectorHandler(this);
      }

    } else if (BeeUtils.same(name, VIEW_DOCUMENT_ITEMS) && widget instanceof ChildGrid) {
      itemsGrid = (ChildGrid) widget;

      itemsGrid.setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public void afterCreateEditor(String source, Editor editor, boolean embedded) {
          if (editor instanceof Autocomplete) {
            ((Autocomplete) editor).addAutocompleteHandler(new AutocompleteEvent.Handler() {
              @Override
              public void onDataSelector(AutocompleteEvent event) {
                if (event.getState() == State.OPEN) {
                  event.getSelector()
                      .setAdditionalFilter(Filter.and(Filter.isEqual(COL_DOCUMENT_CATEGORY,
                          Value.getValue(getLongValue(COL_DOCUMENT_CATEGORY))),
                          Filter.isNotEqual(COL_DOCUMENT, Value.getValue(getActiveRowId()))));
                }
              }
            });
          }
        }

        @Override
        public GridInterceptor getInstance() {
          return null;
        }

        @Override
        public void onEditStart(final EditStartEvent event) {
          if (!BeeUtils.same(event.getColumnId(), COL_DOCUMENT_DATA)) {
            if (event.isReadOnly()) {
              event.consume();
            }
            return;
          }
          Long dataId = DataUtils.getLong(getDataColumns(), event.getRowValue(), COL_DOCUMENT_DATA);

          if (!DataUtils.isId(dataId)) {
            event.consume();

            Relation relation = Relation.create(VIEW_DOCUMENT_TEMPLATES,
                Lists.newArrayList(ALS_CATEGORY_NAME, COL_DOCUMENT_TEMPLATE_NAME));
            relation.disableNewRow();
            relation.setCaching(Caching.QUERY);

            final UnboundSelector selector = UnboundSelector.create(relation);

            HtmlTable table = new HtmlTable();
            table.setText(0, 0, Localized.getConstants().documentTemplateName());
            table.setWidget(0, 1, selector);

            Global.inputWidget(Localized.getConstants().selectDocumentTemplate(), table,
                new InputCallback() {
                  @Override
                  public void onSuccess() {
                    final Consumer<Long> executor = new Consumer<Long>() {
                      @Override
                      public void accept(Long newDataId) {
                        String viewName = itemsGrid.getPresenter().getViewName();

                        Queries.update(viewName, event.getRowValue().getId(),
                            event.getRowValue().getVersion(),
                            Data.getColumns(viewName, Lists.newArrayList(COL_DOCUMENT_DATA)),
                            Lists.newArrayList((String) null),
                            Lists.newArrayList(BeeUtils.toString(newDataId)),
                            null, new RowUpdateCallback(viewName) {
                              @Override
                              public void onSuccess(BeeRow result) {
                                super.onSuccess(result);

                                itemsGrid.getPresenter().getGridView()
                                    .onEditStart(new EditStartEvent(result, event.getColumnId(),
                                        event.getSourceElement(), event.getCharCode(),
                                        event.isReadOnly()));
                              }
                            });
                      }
                    };
                    Long data = null;

                    if (selector.getRelatedRow() != null) {
                      data = Data.getLong(VIEW_DOCUMENT_TEMPLATES, selector.getRelatedRow(),
                          COL_DOCUMENT_DATA);
                    }
                    if (DataUtils.isId(data)) {
                      DocumentsHandler.copyDocumentData(data, new IdCallback() {
                        @Override
                        public void onSuccess(Long result) {
                          executor.accept(result);
                        }
                      });
                    } else {
                      Queries.insert(VIEW_DOCUMENT_DATA, Data.getColumns(VIEW_DOCUMENT_DATA,
                          Lists.newArrayList(COL_DOCUMENT_CONTENT)),
                          Lists.newArrayList((String) null), null, new RowCallback() {
                            @Override
                            public void onSuccess(BeeRow result) {
                              executor.accept(result.getId());
                            }
                          });
                    }
                  }
                });
          }
        }
      });
    }

  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    UserInfo user = BeeKeeper.getUser();
    boolean newRow = DataUtils.isNewRow(row);

    if (!user.isAdministrator()) {
      Widget category = form.getWidgetBySource(COL_DOCUMENT_CATEGORY);

      if (category instanceof HasEnabled) {
        ((HasEnabled) category).setEnabled(newRow);
      }
    }
    if (getHeaderView() == null) {
      return;
    }
    getHeaderView().clearCommandPanel();

    if (!newRow && user.isModuleVisible(ModuleAndSub.of(Module.DOCUMENTS, SubModule.TEMPLATES))) {
      getHeaderView().addCommandItem(newTemplateButton);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new DocumentForm();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isNewRow() && TaskConstants.VIEW_TASKS.equals(event.getRelatedViewName())) {
      createNewTaskRelation(event);
    } else if (event.isNewRow()
        && ServiceConstants.VIEW_SERVICE_OBJECTS.equals(event.getRelatedViewName())) {
      createNewServiceObjectRelation(event);
    }
  }

  @Override
  protected void parseContent(String content, Long dataId, final Consumer<String> consumer) {
    super.parseContent(content, dataId, new Consumer<String>() {
      @Override
      public void accept(String input) {
        final Map<String, BeeRowSet> relations = new HashMap<>();

        final List<String> parts = Lists.newArrayList(Splitter
            .on("<!--{" + VIEW_DOCUMENT_ITEMS + "}-->").split(input));

        final Map<String, Double> globals = new HashMap<>();
        final Holder<Integer> holder = Holder.of(childSelectors.size() + parts.size());

        final BiConsumer<Integer, String> executor = new BiConsumer<Integer, String>() {
          @Override
          public void accept(Integer index, String value) {
            if (index != null) {
              parts.set(index, value);
            }
            holder.set(holder.get() - 1);

            if (!BeeUtils.isPositive(holder.get())) {
              StringBuilder sb = new StringBuilder();

              for (String part : parts) {
                sb.append(part);
              }
              String result = sb.toString();

              for (String global : globals.keySet()) {
                result = result.replace("{" + global + "}",
                    BeeUtils.toString(globals.get(global), 2));
              }
              for (final String relation : childSelectors.keySet()) {
                ChildSelector selector = childSelectors.get(relation);
                BeeRowSet rs = relations.get(relation);

                for (BeeColumn col : Data.getColumns(selector.getOracle().getViewName())) {
                  String val = DataUtils.isEmpty(rs)
                      ? "" : BeeUtils.nvl(rs.getString(0, col.getId()), "");

                  result = result.replace("{" + relation + col.getId() + "}", val);
                }
              }
              consumer.accept(result);
            }
          }
        };
        for (final String relation : childSelectors.keySet()) {
          ChildSelector selector = childSelectors.get(relation);
          Long id = BeeUtils.peek(DataUtils.parseIdList(selector.getValue()));

          if (DataUtils.isId(id)) {
            Queries.getRowSet(selector.getOracle().getViewName(), null, Filter.compareId(id),
                new RowSetCallback() {
                  @Override
                  public void onSuccess(BeeRowSet result) {
                    relations.put(relation, result);
                    executor.accept(null, null);
                  }
                });
          } else {
            executor.accept(null, null);
          }
        }
        for (int i = 0; i < parts.size(); i++) {
          String part = parts.get(i);

          if (i % 2 > 0 && i < parts.size() - 1) {
            parseItems(part, i, executor, globals);
          } else {
            executor.accept(i, part);
          }
        }
      }
    });
  }

  private void createNewServiceObjectRelation(SelectorEvent event) {
    final BeeRow row = event.getNewRow();
    final List<Long> companies = new ArrayList<>();

    for (ChildSelector selector : childSelectors.values()) {
      if (selector.hasRelatedView(ClassifierConstants.VIEW_COMPANIES)) {
        if (!BeeUtils.isEmpty(selector.getValue())) {
          companies.addAll(DataUtils.parseIdList(selector.getValue()));
        }

      }
    }

    if (!companies.isEmpty()) {
      event.consume();

      final String formName = event.getNewRowFormName();
      final DataSelector selector = event.getSelector();

      Queries.getRow(ClassifierConstants.VIEW_COMPANIES, companies.get(0), new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          RelationUtils.updateRow(Data.getDataInfo(ServiceConstants.VIEW_SERVICE_OBJECTS),
              ServiceConstants.COL_SERVICE_CUSTOMER, row,
              Data.getDataInfo(ClassifierConstants.VIEW_COMPANIES), result, true);

          RowFactory.createRelatedRow(formName, row, selector);
        }
      });
    }
  }

  private void createNewTaskRelation(final SelectorEvent event) {
    final BeeRow row = event.getNewRow();

    String summary = BeeUtils.notEmpty(event.getDefValue(), getStringValue(COL_DOCUMENT_NAME));
    if (!BeeUtils.isEmpty(summary)) {
      Data.squeezeValue(TaskConstants.VIEW_TASKS, row, TaskConstants.COL_SUMMARY,
          BeeUtils.trim(summary));
    }

    event.setDefValue(null);

    String description = getStringValue(COL_DESCRIPTION);
    if (!BeeUtils.isEmpty(description)) {
      Data.setValue(TaskConstants.VIEW_TASKS, row, TaskConstants.COL_DESCRIPTION,
          BeeUtils.trim(description));
    }

    final List<Long> companies = new ArrayList<>();
    final List<Long> persons = new ArrayList<>();

    for (ChildSelector selector : childSelectors.values()) {
      if (selector.hasRelatedView(ClassifierConstants.VIEW_COMPANIES)) {
        if (!BeeUtils.isEmpty(selector.getValue())) {
          companies.addAll(DataUtils.parseIdList(selector.getValue()));
        }

      } else if (selector.hasRelatedView(ClassifierConstants.VIEW_PERSONS)) {
        if (!BeeUtils.isEmpty(selector.getValue())) {
          persons.addAll(DataUtils.parseIdList(selector.getValue()));
        }
      }
    }

    if (!companies.isEmpty() || !persons.isEmpty()) {
      event.consume();

      final String formName = event.getNewRowFormName();
      final DataSelector selector = event.getSelector();

      int count = (companies.isEmpty() ? 0 : 1) + (persons.isEmpty() ? 0 : 1);
      final Holder<Integer> latch = Holder.of(count);

      if (!companies.isEmpty()) {
        if (companies.size() > 1) {
          row.setProperty(TaskConstants.PROP_COMPANIES,
              DataUtils.buildIdList(companies.subList(1, companies.size())));
        }

        Queries.getRow(ClassifierConstants.VIEW_COMPANIES, companies.get(0), new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            RelationUtils.updateRow(Data.getDataInfo(TaskConstants.VIEW_TASKS),
                ClassifierConstants.COL_COMPANY, row,
                Data.getDataInfo(ClassifierConstants.VIEW_COMPANIES), result, true);

            latch.set(latch.get() - 1);
            if (latch.get() <= 0) {
              RowFactory.createRelatedRow(formName, row, selector);
            }
          }
        });
      }

      if (!persons.isEmpty()) {
        Queries.getRowSet(ClassifierConstants.VIEW_COMPANY_PERSONS, null,
            Filter.equals(ClassifierConstants.COL_PERSON, persons.get(0)), new RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet result) {
                BeeRow contact = null;

                int size = result.getNumberOfRows();
                if (size == 1) {
                  contact = result.getRow(0);

                } else if (size > 1 && !companies.isEmpty()) {
                  Long company = companies.get(0);
                  int index = result.getColumnIndex(ClassifierConstants.COL_COMPANY);

                  for (BeeRow r : result) {
                    if (company.equals(r.getLong(index))) {
                      contact = r;
                      break;
                    }
                  }
                }

                if (contact == null) {
                  row.setProperty(TaskConstants.PROP_PERSONS, DataUtils.buildIdList(persons));
                } else {
                  RelationUtils.updateRow(Data.getDataInfo(TaskConstants.VIEW_TASKS),
                      ClassifierConstants.COL_CONTACT, row,
                      Data.getDataInfo(ClassifierConstants.VIEW_COMPANY_PERSONS), contact, true);

                  if (persons.size() > 1) {
                    row.setProperty(TaskConstants.PROP_PERSONS,
                        DataUtils.buildIdList(persons.subList(1, persons.size())));
                  }
                }

                latch.set(latch.get() - 1);
                if (latch.get() <= 0) {
                  RowFactory.createRelatedRow(formName, row, selector);
                }
              }
            });
      }
    }
  }



  private void createTemplate() {
    LocalizableConstants loc = Localized.getConstants();

    Global.inputString(loc.newDocumentTemplate(), loc.documentTemplateName(),
        new StringCallback() {
          @Override
          public void onSuccess(final String value) {
            DocumentsHandler.copyDocumentData(getLongValue(COL_DOCUMENT_DATA),
                new IdCallback() {
                  @Override
                  public void onSuccess(Long dataId) {
                    Queries.insert(VIEW_DOCUMENT_TEMPLATES,
                        Data.getColumns(VIEW_DOCUMENT_TEMPLATES,
                            Lists.newArrayList(COL_DOCUMENT_CATEGORY, COL_DOCUMENT_TEMPLATE_NAME,
                                COL_DOCUMENT_DATA)),
                        Lists.newArrayList(getStringValue(COL_DOCUMENT_CATEGORY), value,
                            DataUtils.isId(dataId) ? BeeUtils.toString(dataId) : null),
                        null, new RowInsertCallback(VIEW_DOCUMENT_TEMPLATES, null) {
                          @Override
                          public void onSuccess(BeeRow result) {
                            super.onSuccess(result);
                            RowEditor.open(VIEW_DOCUMENT_TEMPLATES, result, Opener.MODAL);
                          }
                        });
                  }
                });
          }
        });
  }

  private void parseItems(final String content, final Integer idx,
      final BiConsumer<Integer, String> consumer, Map<String, Double> globals) {

    if (itemsGrid == null) {
      consumer.accept(idx, null);
      return;
    }
    GridView gridView = itemsGrid.getPresenter().getGridView();
    int descrIdx = gridView.getDataIndex(COL_DESCRIPTION);
    int qtyIdx = gridView.getDataIndex(COL_TRADE_ITEM_QUANTITY);
    int prcIdx = gridView.getDataIndex(COL_TRADE_ITEM_PRICE);
    int vatPlusIdx = gridView.getDataIndex(COL_TRADE_VAT_PLUS);
    int vatIdx = gridView.getDataIndex(COL_TRADE_VAT);
    int vatPrcIdx = gridView.getDataIndex(COL_TRADE_VAT_PERC);
    int dataIdx = gridView.getDataIndex(COL_DOCUMENT_DATA);
    int contentIdx = gridView.getDataIndex(COL_DOCUMENT_CONTENT);

    int c = gridView.getRowData().size();
    final Holder<Integer> holder = Holder.of(c);
    final String[] parts = new String[c];

    final BiConsumer<Integer, String> executor = new BiConsumer<Integer, String>() {
      @Override
      public void accept(Integer index, String value) {
        parts[index] = value;
        holder.set(holder.get() - 1);

        if (!BeeUtils.isPositive(holder.get())) {
          StringBuilder sb = new StringBuilder();

          for (String part : parts) {
            sb.append(part);
          }
          consumer.accept(idx, sb.toString());
        }
      }
    };
    double qtyAmount = 0;
    double vatAmount = 0;
    double sumAmount = 0;

    for (int i = 0; i < c; i++) {
      IsRow row = gridView.getRowData().get(i);

      final double qty = BeeUtils.unbox(row.getDouble(qtyIdx));
      final Holder<Double> sum = Holder.of(qty * BeeUtils.unbox(row.getDouble(prcIdx)));
      final Holder<Double> vat = Holder.of(BeeUtils.unbox(row.getDouble(vatIdx)));

      boolean vatInPercents = BeeUtils.unbox(row.getBoolean(vatPrcIdx));

      if (BeeUtils.unbox(row.getBoolean(vatPlusIdx))) {
        if (vatInPercents) {
          vat.set(sum.get() / 100 * vat.get());
        }
      } else {
        if (vatInPercents) {
          vat.set(sum.get() - sum.get() / (1 + vat.get() / 100));
        }
        sum.set(sum.get() - vat.get());
      }
      final String descr = row.getString(descrIdx);
      String itemContent = row.getString(contentIdx);
      final int index = i;

      Consumer<String> contentConsumer = new Consumer<String>() {
        @Override
        public void accept(String input) {
          executor.accept(index, content.replace("{" + COL_DOCUMENT_CONTENT + "}", input)
              .replace("{Index}", BeeUtils.toString(index + 1))
              .replace("{" + COL_DESCRIPTION + "}", descr)
              .replace("{" + COL_TRADE_ITEM_QUANTITY + "}", BeeUtils.toString(qty))
              .replace("{" + COL_TRADE_ITEM_PRICE + "}", BeeUtils.toString(sum.get() / qty, 3))
              .replace("{" + COL_TRADE_AMOUNT + "}", BeeUtils.toString(sum.get(), 2))
              .replace("{" + COL_TRADE_VAT + "}", BeeUtils.toString(vat.get(), 2))
              .replace("{" + COL_TRADE_VAT_PLUS + COL_TRADE_AMOUNT + "}",
                  BeeUtils.toString(vat.get() + sum.get(), 2)));
        }
      };
      if (!BeeUtils.isEmpty(itemContent)) {
        super.parseContent(itemContent, row.getLong(dataIdx), contentConsumer);
      } else {
        contentConsumer.accept("");
      }
      qtyAmount += qty;
      sumAmount += sum.get();
      vatAmount += vat.get();
    }
    if (!globals.containsKey(COL_TRADE_ITEM_QUANTITY + COL_TRADE_AMOUNT)) {
      globals.put(COL_TRADE_ITEM_QUANTITY + COL_TRADE_AMOUNT, qtyAmount);
    }
    if (!globals.containsKey(VAR_TOTAL + COL_TRADE_AMOUNT)) {
      globals.put(VAR_TOTAL + COL_TRADE_AMOUNT, sumAmount);
    }
    if (!globals.containsKey(COL_TRADE_VAT + COL_TRADE_AMOUNT)) {
      globals.put(COL_TRADE_VAT + COL_TRADE_AMOUNT, vatAmount);
    }
    if (!globals.containsKey(VAR_TOTAL)) {
      globals.put(VAR_TOTAL, sumAmount + vatAmount);
    }
  }
}
