package com.butent.bee.client.data;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.XMLParser;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.dialog.ModalForm;
import com.butent.bee.client.event.Previewer.PreviewConsumer;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.presenter.NewRowPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.List;

public final class RowFactory {

  public static final String DIALOG_STYLE = "bee-NewRow";

  public static final String STYLE_NEW_ROW_TABLE = "bee-NewRow-table";
  public static final String STYLE_NEW_ROW_LABEL_CELL = "bee-NewRow-labelCell";
  public static final String STYLE_NEW_ROW_LABEL = "bee-NewRow-label";
  public static final String STYLE_NEW_ROW_INPUT_CELL = "bee-NewRow-inputCell";
  public static final String STYLE_NEW_ROW_INPUT = "bee-NewRow-input";

  public static final int GENERATED_FORM_WIDTH = 360;
  public static final int GENERATED_HEADER_HEIGHT = 30;
  public static final int GENERATED_ROW_HEIGHT = 32;
  public static final int GENERATED_HEIGHT_MARGIN = 20;

  private static final int GENERATED_AREA_HEIGHT = 60;

  private static final String DEFAULT_CAPTION = Localized.getConstants().actionNew();

  private static final BeeLogger logger = LogUtils.getLogger(RowFactory.class);

  public static BeeRow createEmptyRow(DataInfo dataInfo, boolean defaults) {
    BeeRow row = DataUtils.createEmptyRow(dataInfo.getColumnCount());
    if (defaults) {
      setDefaults(row, dataInfo);
    }
    return row;
  }

  public static void createRelatedRow(final DataSelector selector, String defValue) {
    Assert.notNull(selector);

    DataInfo dataInfo = selector.getOracle().getDataInfo();

    String formName = selector.getNewRowForm();

    if (BeeUtils.isEmpty(formName)) {
      List<BeeColumn> columns = getColumns(dataInfo, selector.getNewRowColumns(),
          selector.getChoiceColumns());
      if (columns.isEmpty()) {
        logger.warning(dataInfo.getViewName(), "columns not available for create row");
        return;
      }

      formName = generateFormName(dataInfo, columns);

      FormDescription formDescription = FormFactory.getFormDescription(formName);
      if (formDescription == null) {
        formDescription = createFormDescription(formName, dataInfo, columns);
        FormFactory.putFormDescription(formName, formDescription);
      }
    }

    BeeRow row = createEmptyRow(dataInfo, true);

    SelectorEvent event = SelectorEvent.fireNewRow(selector, row, formName, defValue);

    if (!event.isConsumed()) {
      if (!BeeUtils.isEmpty(event.getDefValue())) {
        for (String colName : selector.getChoiceColumns()) {
          BeeColumn column = dataInfo.getColumn(colName);

          if (column != null && column.isEditable() && ValueType.isString(column.getType())) {
            Data.squeezeValue(dataInfo.getViewName(), row, column.getId(),
                event.getDefValue().trim());
            break;
          }
        }
      }

      createRelatedRow(formName, row, selector);
    }
  }

  public static void createRelatedRow(String formName, BeeRow row, final DataSelector selector) {
    Assert.notEmpty(formName);
    Assert.notNull(row);
    Assert.notNull(selector);

    selector.setAdding(true);

    createRow(formName, selector.getNewRowCaption(), selector.getOracle().getDataInfo(), row,
        selector, null, new RowCallback() {
          @Override
          public void onCancel() {
            selector.setAdding(false);
            selector.setFocus(true);
          }

          @Override
          public void onSuccess(BeeRow result) {
            SelectorEvent.fireRowCreated(selector, result);

            selector.setAdding(false);
            selector.setSelection(result, null, true);
          }
        });
  }

  public static void createRow(String viewName) {
    createRow(viewName, null, null);
  }

  public static void createRow(String viewName, RowCallback rowCallback) {
    createRow(viewName, null, rowCallback);
  }

  public static void createRow(String viewName, String caption, RowCallback rowCallback) {
    Assert.notEmpty(viewName);

    DataInfo dataInfo = Data.getDataInfo(viewName);
    if (dataInfo == null) {
      return;
    }

    BeeRow row = createEmptyRow(dataInfo, true);
    createRow(dataInfo.getNewRowForm(), BeeUtils.notEmpty(caption, dataInfo.getNewRowCaption()),
        dataInfo, row, null, null, rowCallback);
  }

  public static void createRow(DataInfo dataInfo, BeeRow row) {
    createRow(dataInfo, row, null);
  }

  public static void createRow(DataInfo dataInfo, BeeRow row, RowCallback rowCallback) {
    Assert.notNull(dataInfo);
    createRow(dataInfo.getNewRowForm(), dataInfo.getNewRowCaption(), dataInfo, row, null, null,
        rowCallback);
  }

  public static void createRow(String formName, String caption, DataInfo dataInfo, BeeRow row,
      UIObject target, FormInterceptor formInterceptor, RowCallback rowCallback) {
    Assert.notEmpty(formName);

    Assert.notNull(dataInfo);
    Assert.notNull(row);

    getForm(formName, caption, formInterceptor, dataInfo, row, target, rowCallback);
  }

  public static void createRow(String formName, String caption, DataInfo dataInfo, BeeRow row,
      RowCallback rowCallback) {
    createRow(formName, caption, dataInfo, row, null, null, rowCallback);
  }

  public static int setDefaults(BeeRow row, DataInfo dataInfo) {
    if (row == null || dataInfo == null) {
      return BeeConst.UNDEF;
    }

    List<String> colNames = Lists.newArrayList();
    for (BeeColumn column : dataInfo.getColumns()) {
      if (column.hasDefaults()) {
        colNames.add(column.getId());
      }
    }
    if (colNames.isEmpty()) {
      return 0;
    }

    return DataUtils.setDefaults(row, colNames, dataInfo.getColumns(), Global.getDefaults())
        + RelationUtils.setDefaults(dataInfo, row, colNames, dataInfo.getColumns(),
            BeeKeeper.getUser().getUserData());
  }

  private static FormDescription createFormDescription(String formName, DataInfo dataInfo,
      List<BeeColumn> columns) {
    Document doc = XMLParser.createDocument();
    Element form = doc.createElement(FormFactory.TAG_FORM);

    form.setAttribute(UiConstants.ATTR_NAME, formName);
    form.setAttribute(UiConstants.ATTR_VIEW_NAME, dataInfo.getViewName());

    form.setAttribute(HasDimensions.ATTR_WIDTH, BeeUtils.toString(GENERATED_FORM_WIDTH));

    Element table = doc.createElement(FormWidget.TABLE.getTagName());
    table.setAttribute(UiConstants.ATTR_CLASS, STYLE_NEW_ROW_TABLE);

    int height = GENERATED_HEADER_HEIGHT + GENERATED_HEIGHT_MARGIN;
    for (BeeColumn column : columns) {
      Element row = doc.createElement(UiConstants.TAG_ROW);

      Element labelCell = doc.createElement(UiConstants.TAG_CELL);
      labelCell.setAttribute(UiConstants.ATTR_CLASS, STYLE_NEW_ROW_LABEL_CELL);

      Element label = doc.createElement(FormWidget.LABEL.getTagName());
      label.setAttribute(UiConstants.ATTR_HTML,
          BeeUtils.notEmpty(Localized.getLabel(column), column.getId()));

      String labelClass;
      if (column.hasDefaults()) {
        labelClass = StyleUtils.NAME_HAS_DEFAULTS;
      } else if (!column.isNullable()) {
        labelClass = StyleUtils.NAME_REQUIRED;
      } else {
        labelClass = null;
      }
      label.setAttribute(UiConstants.ATTR_CLASS,
          StyleUtils.buildClasses(STYLE_NEW_ROW_LABEL, labelClass));

      labelCell.appendChild(label);
      row.appendChild(labelCell);

      Element inputCell = doc.createElement(UiConstants.TAG_CELL);
      inputCell.setAttribute(UiConstants.ATTR_CLASS, STYLE_NEW_ROW_INPUT_CELL);

      FormWidget widgetType = dataInfo.hasRelation(column.getId())
          ? FormWidget.DATA_SELECTOR : FormFactory.getWidgetType(column);
      Element input = doc.createElement(widgetType.getTagName());
      input.setAttribute(UiConstants.ATTR_SOURCE, column.getId());
      input.setAttribute(UiConstants.ATTR_CLASS, STYLE_NEW_ROW_INPUT);

      inputCell.appendChild(input);
      row.appendChild(inputCell);

      table.appendChild(row);

      if (column.isText()) {
        height += GENERATED_AREA_HEIGHT;
      } else {
        height += GENERATED_ROW_HEIGHT;
      }
    }

    form.setAttribute(HasDimensions.ATTR_HEIGHT, BeeUtils.toString(height));

    form.appendChild(table);
    return new FormDescription(form);
  }

  private static String generateFormName(DataInfo dataInfo, List<BeeColumn> columns) {
    int hash = 0;
    for (int i = 0; i < columns.size(); i++) {
      hash += columns.get(i).getId().hashCode() * (i + 1);
    }
    return dataInfo.getViewName().toLowerCase() + "-new-row-" + BeeUtils.toString(Math.abs(hash));
  }

  private static List<BeeColumn> getColumns(DataInfo dataInfo, String specified,
      List<String> preferred) {
    List<BeeColumn> result = Lists.newArrayList();

    List<String> colNames = Lists.newArrayList();
    if (!BeeUtils.isEmpty(specified)) {
      List<String> list = DataUtils.parseColumns(specified, dataInfo.getColumns());
      if (list != null) {
        colNames.addAll(list);
      }
    }

    if (colNames.isEmpty() && !BeeUtils.isEmpty(preferred)) {
      List<String> list = DataUtils.parseColumns(preferred, dataInfo.getColumns());
      if (!list.isEmpty()) {
        colNames.addAll(list);
      }
    }

    if (!colNames.isEmpty()) {
      for (String colName : colNames) {
        BeeColumn column = dataInfo.getColumn(colName);
        if (column.isEditable()) {
          result.add(dataInfo.getColumn(colName));
        }
      }
    }

    for (BeeColumn column : dataInfo.getColumns()) {
      if (column.isEditable() && (colNames.isEmpty() || !colNames.contains(column.getId())
          && !column.isNullable() && !column.hasDefaults())) {
        result.add(column);
      }
    }
    return result;
  }

  private static void getForm(String formName, final String caption,
      FormInterceptor formInterceptor, final DataInfo dataInfo, final BeeRow row,
      final UIObject target, final RowCallback rowCallback) {

    FormInterceptor fcb =
        (formInterceptor == null) ? FormFactory.getFormInterceptor(formName) : formInterceptor;

    FormFactory.createFormView(formName, dataInfo.getViewName(), dataInfo.getColumns(), true, fcb,
        new FormFactory.FormViewCallback() {
          @Override
          public void onSuccess(FormDescription formDescription, FormView result) {
            if (result != null) {
              result.setEditing(true);
              result.start(null);

              openForm(result, caption, dataInfo, row, target, rowCallback);
            }
          }
        });
  }

  private static void openForm(final FormView formView, String caption, final DataInfo dataInfo,
      final BeeRow row, UIObject target, final RowCallback callback) {

    String cap = BeeUtils.notEmpty(caption, formView.getCaption(), DEFAULT_CAPTION);

    final NewRowPresenter presenter = new NewRowPresenter(formView, dataInfo, cap);
    final ModalForm dialog = new ModalForm(presenter, formView, false);

    final RowCallback closer = new RowCallback() {
      @Override
      public void onCancel() {
        dialog.close();
        if (callback != null) {
          callback.onCancel();
        }
      }

      @Override
      public void onSuccess(BeeRow result) {
        dialog.close();
        if (callback != null) {
          callback.onSuccess(result);
        }
      }
    };

    presenter.setActionDelegate(new HandlesActions() {
      @Override
      public void handleAction(Action action) {
        FormInterceptor interceptor = formView.getFormInterceptor();
        if (interceptor != null && !interceptor.beforeAction(action, presenter)) {
          return;
        }

        switch (action) {
          case CANCEL:
            closer.onCancel();
            break;

          case CLOSE:
            formView.onClose(new CloseCallback() {
              @Override
              public void onClose() {
                closer.onCancel();
              }

              @Override
              public void onSave() {
                handleAction(Action.SAVE);
              }
            });
            break;

          case SAVE:
            presenter.save(closer);
            break;

          default:
            logger.warning(NameUtils.getName(this), action, "not implemented");
        }

        if (interceptor != null) {
          interceptor.afterAction(action, presenter);
        }
      }
    });

    dialog.setOnSave(new PreviewConsumer() {
      @Override
      public void accept(NativePreviewEvent input) {
        if (formView.checkOnSave(input)) {
          presenter.handleAction(Action.SAVE);
        }
      }
    });

    dialog.setOnEscape(new PreviewConsumer() {
      @Override
      public void accept(NativePreviewEvent input) {
        if (formView.checkOnClose(input)) {
          presenter.handleAction(Action.CLOSE);
        }
      }
    });

    dialog.addOpenHandler(new OpenEvent.Handler() {
      @Override
      public void onOpen(OpenEvent event) {
        if (formView.getFormInterceptor() != null) {
          formView.getFormInterceptor().onStartNewRow(formView, null, row);
        }
        formView.updateRow(row, true);
      }
    });

    if (target == null) {
      dialog.center();
    } else {
      dialog.showRelativeTo(target.getElement());
    }

    UiHelper.focus(formView.getRootWidget().asWidget());
  }

  private RowFactory() {
  }
}
