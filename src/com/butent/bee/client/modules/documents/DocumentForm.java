package com.butent.bee.client.modules.documents;

import com.google.common.base.Splitter;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.UserInfo;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.composite.Relations;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DocumentForm extends DocumentDataForm {

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    super.onSaveChanges(listener, event);

    if (getFormView() != null) {

      String oldValue = event.getOldRow().getString(Data.getColumnIndex(VIEW_DOCUMENTS,
          COL_DOCUMENT_COMPANY));
      String newValue = event.getNewRow().getString(Data.getColumnIndex(VIEW_DOCUMENTS,
          COL_DOCUMENT_COMPANY));

      if (!Objects.equals(oldValue, newValue)) {
        DocumentsHandler.insertCompanyInfo(event.getNewRow(), oldValue);
      }
    }
  }

  private class RelationsHandler implements SelectorEvent.Handler {

    @Override
    public void onDataSelector(SelectorEvent event) {
      if (event.isNewRow()) {
        final String viewName = event.getRelatedViewName();

        switch (viewName) {

          case TaskConstants.TBL_TASKS:
            if (event.isNewRow() && TaskConstants.VIEW_TASKS.equals(event.getRelatedViewName())) {
              createNewTaskRelation(event);
            }
            break;

          case ServiceConstants.TBL_SERVICE_OBJECTS:
            if (event.isNewRow()
                && ServiceConstants.VIEW_SERVICE_OBJECTS.equals(event.getRelatedViewName())) {
              createNewServiceObjectRelation(event);
            }
            break;
        }
      } else if (event.getCallback() == null && event.isRowCreated()) {
        final String viewName = event.getRelatedViewName();

        switch (viewName) {

          case TaskConstants.TBL_TASKS:
            if (rel != null) {
              rel.requery(null, getActiveRowId());
            }
            break;
        }
      }
    }
  }

  private final Button newTemplateButton = new Button(Localized.dictionary()
      .newDocumentTemplate(), new ClickHandler() {
    @Override
    public void onClick(ClickEvent event) {
      RowFactory.createRow(VIEW_DOCUMENT_TEMPLATES, Modality.ENABLED, new RowCallback() {
        @Override
        public void onSuccess(final BeeRow row) {
          DocumentsHandler.copyDocumentData(getLongValue(COL_DOCUMENT_DATA), new IdCallback() {
            @Override
            public void onSuccess(Long dataId) {
              Queries.update(VIEW_DOCUMENT_TEMPLATES, Filter.compareId(row.getId()),
                  COL_DOCUMENT_DATA, new LongValue(dataId), new IntCallback() {
                    @Override
                    public void onSuccess(Integer res) {
                      Long oldId = Data.getLong(VIEW_DOCUMENT_TEMPLATES, row, COL_DOCUMENT_DATA);

                      if (DataUtils.isId(oldId)) {
                        Queries.deleteRow(VIEW_DOCUMENT_DATA, oldId);
                      }
                      RowEditor.open(VIEW_DOCUMENT_TEMPLATES, row.getId(), Opener.MODAL);
                    }
                  });
            }
          });
        }
      });
    }
  });
  private ChildGrid itemsGrid;
  private Relations rel;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    super.afterCreateWidget(name, widget, callback);

    if (widget instanceof Relations) {
      this.rel = (Relations) widget;
      rel.setSelectorHandler(new RelationsHandler());

    } else if (BeeUtils.same(name, VIEW_DOCUMENT_ITEMS) && widget instanceof ChildGrid) {
      itemsGrid = (ChildGrid) widget;
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
    super.onStart(form);
  }

  @Override
  public FormInterceptor getInstance() {
    return new DocumentForm();
  }

  @Override
  protected Multimap<String, Pair<String, String>> getInsertObjects() {
    Multimap<String, Pair<String, String>> objects = super.getInsertObjects();

    if (objects != null) {
      objects = LinkedListMultimap.create(objects);
    } else {
      objects = LinkedListMultimap.create();
    }
    DataInfo dataInfo = Data.getDataInfo(getViewName());

    for (String col : dataInfo.getColumnNames(false)) {
      if (!BeeUtils.same(col, COL_DOCUMENT_CONTENT) && !dataInfo.hasRelation(col)) {
        objects.put(getFormView().getCaption(),
            Pair.of(Localized.getLabel(dataInfo.getColumn(col)), "{" + COL_DOCUMENT + col + "}"));
      }
    }
    for (RowChildren garden : rel.getRowChildren(true)) {
      String relation = Data.getColumnRelation(garden.getRepository(), garden.getChildColumn());
      dataInfo = Data.getDataInfo(relation);

      for (String col : dataInfo.getColumnNames(false)) {
        if (!dataInfo.hasRelation(col)) {
          objects.put(Data.getViewCaption(relation),
              Pair.of(Localized.getLabel(dataInfo.getColumn(col)), "{" + relation + col + "}"));
        }
      }
    }
    return objects;
  }

  @Override
  protected Map<String, String> getTemplates() {
    Map<String, String> templates = super.getTemplates();

    if (templates != null) {
      templates = new LinkedHashMap<>(templates);
    } else {
      templates = new LinkedHashMap<>();
    }
    Dictionary loc = Localized.dictionary();

    StringBuilder sb = new StringBuilder("<table style=\"border-collapse:collapse;")
        .append(" border:1px solid black; text-align:right;\">")
        .append("<tbody><tr style=\"text-align:center;\">");

    for (String cap : new String[] {
        loc.ordinal(), loc.description(), loc.quantity(), loc.price(),
        loc.amount(), loc.vat(), loc.total()}) {
      sb.append("<td style=\"border:1px solid black;\">" + cap + "</td>");
    }
    sb.append("</tr>")
        .append("<!--{DocumentItems}-->")
        .append("<tr>")
        .append("<td style=\"border:1px solid black;\">{Index}</td>")
        .append("<td style=\"border:1px solid black; text-align:left;\">{Description}</td>");

    for (String col : new String[] {
        "{Quantity}", "{Price}", "{Amount}", "{Vat}",
        "{VatPlusAmount}"}) {
      sb.append("<td style=\"border:1px solid black;\">" + col + "</td>");
    }
    sb.append("</tr>")
        .append("<!--{DocumentItems}-->")
        .append("<tr>")
        .append("<td style=\"text-align:left;\" colspan=\"2\">" + loc.totalOf() + "</td>")
        .append("<td>{QuantityAmount}</td>")
        .append("<td colspan=\"2\">{TotalAmount}</td>")
        .append("<td>{VatAmount}</td>")
        .append("<td>{Total}</td>")
        .append("</tr></tbody></table>");

    templates.put(loc.documentItems(), sb.toString());

    templates.put(loc.documentItems() + ": " + loc.content(),
        new StringBuilder("<table><tbody>")
            .append("<!--{DocumentItems}-->")
            .append("<tr><td>{Content}</td></tr>")
            .append("<!--{DocumentItems}-->")
            .append("</tbody></table>").toString());

    return templates;
  }

  @Override
  protected void parseContent(String content, Long dataId, final Consumer<String> consumer) {
    super.parseContent(content, dataId, new Consumer<String>() {
      @Override
      public void accept(String input) {
        final Map<String, BeeRow> relations = new HashMap<>();

        final List<String> parts = Lists.newArrayList(Splitter
            .on("<!--{" + VIEW_DOCUMENT_ITEMS + "}-->").split(input));

        final Map<String, Double> globals = new HashMap<>();
        final Holder<Integer> holder = Holder.of(rel.getRowChildren(true).size() + parts.size());

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
                    BeeUtils.nonZero(globals.get(global))
                        ? BeeUtils.toString(globals.get(global), 2) : "");
              }
              for (BeeColumn column : getFormView().getDataColumns()) {
                result = result.replace("{" + COL_DOCUMENT + column.getId() + "}",
                    BeeUtils.nvl(getParsedValue(getViewName(), getActiveRow(), column), ""));
              }
              for (RowChildren garden : rel.getRowChildren(true)) {
                String relation = Data.getColumnRelation(garden.getRepository(),
                    garden.getChildColumn());

                for (BeeColumn column : Data.getColumns(relation)) {
                  result = result.replace("{" + relation + column.getId() + "}",
                      BeeUtils.nvl(getParsedValue(relation, relations.get(relation), column), ""));
                }
              }
              consumer.accept(result);
            }
          }

          private String getParsedValue(String viewName, IsRow row, BeeColumn column) {
            String val = null;

            if (row != null) {
              switch (column.getType()) {
                case BOOLEAN:
                  val = BeeUtils.unbox(Data.getBoolean(viewName, row, column.getId()))
                      ? Localized.dictionary().yes() : Localized.dictionary().no();
                  break;

                case DATE:
                  JustDate date = Data.getDate(viewName, row, column.getId());
                  if (date != null) {
                    val = Format.renderDate(date);
                  }
                  break;

                case DATE_TIME:
                  DateTime time = Data.getDateTime(viewName, row, column.getId());
                  if (time != null) {
                    val = Format.renderDateTime(time);
                  }
                  break;

                default:
                  String enumKey = column.getEnumKey();

                  if (!BeeUtils.isEmpty(enumKey)) {
                    val = EnumUtils.getLocalizedCaption(enumKey,
                        Data.getInteger(viewName, row, column.getId()),
                        Localized.dictionary());
                  } else {
                    val = Data.getString(viewName, row, column.getId());
                  }
                  break;
              }
            }
            return val;
          }
        };
        for (RowChildren garden : rel.getRowChildren(true)) {
          final String relation = Data.getColumnRelation(garden.getRepository(),
              garden.getChildColumn());
          Long id = BeeUtils.peek(DataUtils.parseIdList(garden.getChildrenIds()));

          if (DataUtils.isId(id)) {
            Queries.getRow(relation, id, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
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
    Long company = null;

    for (RowChildren garden : rel.getRowChildren(true)) {
      if (Objects.equals(Data.getColumnRelation(garden.getRepository(), garden.getChildColumn()),
          VIEW_COMPANIES)) {
        company = BeeUtils.peek(DataUtils.parseIdList(garden.getChildrenIds()));
      }
    }

    if (DataUtils.isId(company)) {
      event.consume();
      final BeeRow row = event.getNewRow();
      final String formName = event.getNewRowFormName();
      final DataSelector selector = event.getSelector();

      Queries.getRow(TBL_COMPANIES, company, new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          RelationUtils.updateRow(Data.getDataInfo(ServiceConstants.VIEW_SERVICE_OBJECTS),
              ServiceConstants.COL_SERVICE_CUSTOMER, row,
              Data.getDataInfo(TBL_COMPANIES), result, true);

          RowFactory.createRelatedRow(formName, row, selector, null);
        }
      });
    }
  }

  private void createNewTaskRelation(final SelectorEvent event) {
    final BeeRow row = event.getNewRow();
    String summary = BeeUtils.notEmpty(event.getDefValue(), getStringValue(COL_DOCUMENT_NAME));
    row.setProperty(Relations.PFX_RELATED + VIEW_DOCUMENTS,
        DataUtils.buildIdList(getActiveRowId()));

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

    for (RowChildren garden : rel.getRowChildren(true)) {
      final List<Long> collection;

      switch (Data.getColumnRelation(garden.getRepository(), garden.getChildColumn())) {
        case VIEW_COMPANIES:
          collection = companies;
          break;
        case VIEW_PERSONS:
          collection = persons;
          break;
        default:
          continue;
      }
      collection.addAll(DataUtils.parseIdList(garden.getChildrenIds()));
    }

    if (!companies.isEmpty() || !persons.isEmpty()) {
      event.consume();

      final String formName = event.getNewRowFormName();
      final DataSelector selector = event.getSelector();

      int count = (companies.isEmpty() ? 0 : 1) + (persons.isEmpty() ? 0 : 1);
      final Holder<Integer> latch = Holder.of(count);

      if (!companies.isEmpty()) {
        if (companies.size() > 1) {
          row.setProperty(Relations.PFX_RELATED + VIEW_COMPANIES,
              DataUtils.buildIdList(companies.subList(1, companies.size())));
        }

        Queries.getRow(VIEW_COMPANIES, companies.get(0), new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            RelationUtils.updateRow(Data.getDataInfo(TaskConstants.VIEW_TASKS),
                COL_COMPANY, row, Data.getDataInfo(VIEW_COMPANIES), result, true);

            latch.set(latch.get() - 1);
            if (latch.get() <= 0) {
              RowFactory.createRelatedRow(formName, row, selector, null);
            }
          }
        });
      }

      if (!persons.isEmpty()) {
        Queries.getRowSet(VIEW_COMPANY_PERSONS, null, Filter.equals(COL_PERSON, persons.get(0)),
            new RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet result) {
                BeeRow contact = null;

                int size = result.getNumberOfRows();
                if (size == 1) {
                  contact = result.getRow(0);

                } else if (size > 1 && !companies.isEmpty()) {
                  Long company = companies.get(0);
                  int index = result.getColumnIndex(COL_COMPANY);

                  for (BeeRow r : result) {
                    if (company.equals(r.getLong(index))) {
                      contact = r;
                      break;
                    }
                  }
                }

                if (contact == null) {
                  row.setProperty(Relations.PFX_RELATED + VIEW_PERSONS,
                      DataUtils.buildIdList(persons));
                } else {
                  RelationUtils.updateRow(Data.getDataInfo(TaskConstants.VIEW_TASKS),
                      COL_CONTACT, row, Data.getDataInfo(VIEW_COMPANY_PERSONS), contact, true);

                  if (persons.size() > 1) {
                    row.setProperty(Relations.PFX_RELATED + VIEW_PERSONS,
                        DataUtils.buildIdList(persons.subList(1, persons.size())));
                  }
                }

                latch.set(latch.get() - 1);
                if (latch.get() <= 0) {
                  RowFactory.createRelatedRow(formName, row, selector, null);
                }
              }
            });
      }
    }
  }

  private void parseItems(final String content, final Integer idx,
      final BiConsumer<Integer, String> consumer, Map<String, Double> globals) {

    if (itemsGrid == null) {
      consumer.accept(idx, null);
      return;
    }
    GridView gridView = itemsGrid.getPresenter().getGridView();
    int ordIdx = gridView.getDataIndex(COL_TRADE_ITEM_ORDINAL);
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
      final String ordinal = row.getString(ordIdx);
      final String descr = row.getString(descrIdx);
      String itemContent = row.getString(contentIdx);
      final int index = i;

      Consumer<String> contentConsumer = new Consumer<String>() {
        @Override
        public void accept(String input) {
          executor.accept(index, content.replace("{" + COL_DOCUMENT_CONTENT + "}", input)
              .replace("{Index}", BeeUtils.nvl(ordinal, ""))
              .replace("{" + COL_DESCRIPTION + "}", descr)
              .replace("{" + COL_TRADE_ITEM_QUANTITY + "}",
                  BeeUtils.nonZero(qty) ? BeeUtils.toString(qty) : "")
              .replace("{" + COL_TRADE_ITEM_PRICE + "}",
                  BeeUtils.nonZero(sum.get()) ? BeeUtils.toString(sum.get() / qty, 3) : "")
              .replace("{" + COL_TRADE_AMOUNT + "}",
                  BeeUtils.nonZero(sum.get()) ? BeeUtils.toString(sum.get(), 2) : "")
              .replace("{" + COL_TRADE_VAT + "}",
                  BeeUtils.nonZero(vat.get()) ? BeeUtils.toString(vat.get(), 2) : "")
              .replace("{" + COL_TRADE_VAT_PLUS + COL_TRADE_AMOUNT + "}",
                  BeeUtils.nonZero(vat.get() + sum.get())
                      ? BeeUtils.toString(vat.get() + sum.get(), 2) : ""));
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
