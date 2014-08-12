package com.butent.bee.client.modules.administration;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.LocalProvider;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.InputTimeOfDay;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ParametersGrid extends AbstractGridInterceptor {

  private static final String GRID_NAME = "Parameters";

  private static final String NAME = "Name";
  private static final String VALUE = "Value";
  private static final String DESCRIPTION = "Description";

  private static final List<BeeColumn> columns = Lists.newArrayList(
      new BeeColumn(ValueType.TEXT, NAME, false),
      new BeeColumn(ValueType.TEXT, VALUE, true),
      new BeeColumn(ValueType.TEXT, DESCRIPTION, true));

  public static void open(String module) {
    Assert.notEmpty(module);
    openGrid(module, ViewHelper.getPresenterCallback());
  }

  public static void open(String module, ViewCallback callback) {
    Assert.notEmpty(module);
    Assert.notNull(callback);
    openGrid(module, ViewFactory.getPresenterCallback(callback));
  }

  private static int indexOf(String colName) {
    return DataUtils.getColumnIndex(colName, columns);
  }

  private static void openGrid(String module, PresenterCallback callback) {
    GridFactory.openGrid(GRID_NAME, new ParametersGrid(module), null, callback);
  }

  private final String module;
  private LocalProvider provider;
  private final List<BeeParameter> params = Lists.newArrayList();
  private final Long userId;

  private ParametersGrid(String module) {
    this.module = module;
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (presenter != null && presenter.getDataProvider() instanceof LocalProvider) {
      provider = (LocalProvider) presenter.getDataProvider();
      requery();
    }
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    if (action != Action.REFRESH) {
      return super.beforeAction(action, presenter);
    }
    requery();
    return false;
  }

  @Override
  public String getCaption() {
    ModuleAndSub ms = ModuleAndSub.parse(module);

    String message;
    if (ms == null) {
      message = module;
    } else {
      message = BeeUtils.joinWords(ms.getModule().getCaption(),
          (ms.getSubModule() == null) ? null : ms.getSubModule().getCaption());
    }

    return BeeUtils.joinWords(Localized.getConstants().parameters(),
        BeeUtils.parenthesize(message));
  }

  @Override
  public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
    return new BeeRowSet(columns);
  }

  @Override
  public GridInterceptor getInstance() {
    return new ParametersGrid(module);
  }

  @Override
  public String getSupplierKey() {
    return ViewFactory.SupplierKind.PARAMETERS.getKey(module);
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    event.consume();
    final String column = event.getColumnId();

    if (!BeeUtils.same(column, VALUE) || event.isReadOnly()) {
      return;
    }
    final CellGrid grid = getGridView().getGrid();
    final IsRow row = event.getRowValue();
    final BeeParameter param = params.get(BeeUtils.toInt(row.getId()));

    final BiConsumer<String, String> consumer = new BiConsumer<String, String>() {
      @Override
      public void accept(String value, String displayValue) {
        if (set(param, value)) {
          if (!BeeUtils.isEmpty(displayValue)) {
            if (param.supportsUsers()) {
              param.setDisplayValue(userId, displayValue);
            } else {
              param.setDisplayValue(displayValue);
            }
          }
          row.setValue(getGridView().getDataIndex(column), render(param));
          grid.refreshCellContent(row.getId(), column);
        }
      }
    };
    if (event.isDelete()) {
      consumer.accept(null, null);
    } else {
      switch (param.getType()) {
        case BOOLEAN:
          if (EditStartEvent.isClickOrEnter(event.getCharCode())) {
            consumer.accept(BeeUtils.toString(!BeeUtils.unbox(param.getBoolean(userId))), null);
          }
          break;

        case COLLECTION:
          Global.inputCollection(param.getName(), Localized.getConstants().parameter(),
              BeeUtils.toBoolean(param.getOptions()), param.getCollection(userId),
              new Consumer<Collection<String>>() {
                @Override
                public void accept(Collection<String> result) {
                  consumer.accept(Codec.beeSerialize(result), null);
                }
              }, null);
          break;

        case MAP:
          Global.inputMap(param.getName(), Localized.getConstants().parameter(),
              Localized.getConstants().value(), param.getMap(userId),
              new Consumer<Map<String, String>>() {
                @Override
                public void accept(Map<String, String> result) {
                  consumer.accept(Codec.beeSerialize(result), null);
                }
              });
          break;

        default:
          final Popup popup = new Popup(OutsideClick.CLOSE, null);
          final Editor editor = getEditor(param, event);

          final ScheduledCommand executor = new ScheduledCommand() {
            @Override
            public void execute() {
              if (popup.isShowing()) {
                popup.close();
              }
              List<String> errors = editor.validate(true);

              if (!BeeUtils.isEmpty(errors)) {
                Global.showError(errors);
              } else {
                String displayValue;
                if (editor instanceof UnboundSelector) {
                  UnboundSelector selector = (UnboundSelector) editor;
                  selector.render(selector.getRelatedRow());
                  displayValue = selector.getRenderedValue();
                } else {
                  displayValue = null;
                }
                consumer.accept(editor.getValue(), displayValue);
              }
            }
          };
          editor.addEditChangeHandler(new EditChangeHandler() {
            @Override
            public void onKeyDown(KeyDownEvent e) {
              int keyCode = e.getNativeKeyCode();

              if (editor.handlesKey(keyCode)) {
                return;
              }
              switch (keyCode) {
                case KeyCodes.KEY_ESCAPE:
                  e.preventDefault();
                  popup.close();
                  break;

                case KeyCodes.KEY_ENTER:
                  e.preventDefault();
                  executor.execute();
                  break;
              }
            }

            @Override
            public void onValueChange(ValueChangeEvent<String> e) {
              executor.execute();
            }
          });

          editor.addEditStopHandler(new EditStopEvent.Handler() {
            @Override
            public void onEditStop(EditStopEvent e) {
              executor.execute();
            }
          });
          popup.setWidget(editor);

          popup.addCloseHandler(new CloseEvent.Handler() {
            @Override
            public void onClose(CloseEvent ev) {
              if (ev.mouseOutside()) {
                executor.execute();
              }
              grid.refocus();
            }
          });
          popup.showOnTop(event.getSourceElement());
          editor.setFocus(true);
          editor.startEdit(editor.getNormalizedValue(), (char) event.getCharCode(),
              EditorAction.REPLACE, null);
          break;
      }
    }
  }

  private Editor getEditor(BeeParameter param, EditStartEvent event) {
    Editor editor;

    switch (param.getType()) {
      case DATE:
        editor = new InputDate();
        ((InputDate) editor).setDate(param.getDate(userId));
        break;

      case DATETIME:
        editor = new InputDateTime();
        ((InputDateTime) editor).setDateTime(param.getDateTime(userId));
        break;

      case NUMBER:
        editor = new InputNumber();
        Number n = param.getNumber(userId);
        Double d = null;

        if (n != null) {
          d = n.doubleValue();
        }
        ((InputNumber) editor).setValue(d);
        break;

      case RELATION:
        Pair<String, String> relData = Pair.restore(param.getOptions());
        ArrayList<String> cols = Lists.newArrayList(relData.getB());
        Relation relation = Relation.create(relData.getA(), cols);
        relation.disableEdit();
        relation.disableNewRow();

        UnboundSelector selector = UnboundSelector.create(relation, cols);
        Long rel = param.getRelation(userId);
        selector.setValue(rel != null ? rel.toString() : null);
        selector.setDisplayValue(param.getDisplayValue(userId));
        editor = selector;
        break;

      case TEXT:
        editor = new InputText();
        editor.setValue(param.getText(userId));
        break;

      case TIME:
        editor = BeeUtils.toBoolean(param.getOptions()) ? new InputTimeOfDay() : new InputTime();
        Long time = param.getTime(userId);

        if (time != null) {
          ((InputTime) editor).setValue(TimeUtils.renderTime(time, false));
        }
        break;

      default:
        editor = null;
        break;
    }
    if (editor != null) {
      StyleUtils.copyBox(event.getSourceElement(), editor.getElement());
      StyleUtils.setTop(editor.getElement(), 0);
      StyleUtils.setLeft(editor.getElement(), 0);
      editor.addStyleName(EditableColumn.STYLE_EDITOR);
    }
    return editor;
  }

  private String render(BeeParameter param) {
    String value = null;

    switch (param.getType()) {
      case BOOLEAN:
        value = BeeUtils.unbox(param.getBoolean(userId))
            ? Localized.getConstants().yes() : Localized.getConstants().no();
        break;

      case COLLECTION:
        Collection<String> collection = param.getCollection(userId);

        if (!BeeUtils.isEmpty(collection)) {
          value = "..." + BeeUtils.parenthesize(collection.size());
        }
        break;

      case DATE:
        JustDate date = param.getDate(userId);

        if (date != null) {
          value = date.toString();
        }
        break;

      case DATETIME:
        DateTime dateTime = param.getDateTime(userId);

        if (dateTime != null) {
          value = dateTime.toCompactString();
        }
        break;

      case MAP:
        Map<String, String> map = param.getMap(userId);

        if (!BeeUtils.isEmpty(map)) {
          value = "..." + BeeUtils.parenthesize(map.size());
        }
        break;

      case NUMBER:
        Number number = param.getNumber(userId);

        if (number != null) {
          value = number.toString();
        }
        break;

      case RELATION:
        value = param.getDisplayValue(userId);
        break;

      case TEXT:
        value = param.getText(userId);
        break;

      case TIME:
        Long time = param.getTime(userId);

        if (time != null) {
          value = TimeUtils.renderTime(time, false);
        }
        break;
    }
    return value;
  }

  private void requery() {
    if (provider == null) {
      return;
    }
    provider.clear();
    params.clear();
    ParameterList args = AdministrationKeeper.createArgs(SVC_GET_PARAMETERS);
    args.addDataItem(VAR_PARAMETERS_MODULE, module);

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getGridView());

        if (response.hasErrors()) {
          return;
        }
        for (String prm : Codec.beeDeserializeCollection(response.getResponseAsString())) {
          BeeParameter p = BeeParameter.restore(prm);
          params.add(p);

          String[] values = new String[columns.size()];
          values[indexOf(NAME)] = p.getName();
          values[indexOf(VALUE)] = render(p);
          values[indexOf(DESCRIPTION)] = Localized.translate("prm" + p.getName());

          provider.addRow(new BeeRow(params.size() - 1, values));
        }
        provider.refresh(false);
      }
    });
  }

  @SuppressWarnings("unused")
  private void reset(BeeParameter param) {
    ParameterList args = AdministrationKeeper.createArgs(SVC_RESET_PARAMETER);
    args.addDataItem(VAR_PARAMETER, param.getName());

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getGridView());
      }
    });
  }

  private boolean set(BeeParameter param, String value) {
    if (BeeUtils.equalsTrimRight(param.getValue(userId), value)) {
      return false;
    }
    if (param.supportsUsers()) {
      param.setValue(userId, value);
    } else {
      param.setValue(value);
    }
    Global.setParameter(param.getName(), value);
    return true;
  }
}