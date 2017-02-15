package com.butent.bee.client.modules.administration;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.InputTimeOfDay;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ParametersGrid extends AbstractGridInterceptor {

  private final class ValueRenderer extends AbstractCellRenderer {

    private int nameIdx;
    private int valueIdx;

    private ValueRenderer(int nameIdx, int valueIdx, CellSource cellSource) {
      super(cellSource);
      this.nameIdx = nameIdx;
      this.valueIdx = valueIdx;
    }

    @Override
    public String render(IsRow row) {
      String prmName = row.getString(nameIdx);

      if (!params.containsKey(prmName)) {
        params.put(prmName, BeeParameter.restore(row.getString(valueIdx)));
      }
      BeeParameter prm = params.get(prmName);
      String value = null;
      row.setProperty("IsDefault", Objects.equals(prm.supportsUsers() ? prm.getValue(userId)
          : prm.getValue(), prm.getDefValue()) ? "1" : null);

      switch (prm.getType()) {
        case BOOLEAN:
          value = BeeUtils.unbox(prm.supportsUsers() ? prm.getBoolean(userId) : prm.getBoolean())
              ? Localized.dictionary().yes() : Localized.dictionary().no();
          break;

        case COLLECTION:
          Collection<String> collection = prm.supportsUsers()
              ? prm.getCollection(userId) : prm.getCollection();

          if (!BeeUtils.isEmpty(collection)) {
            value = "..." + BeeUtils.parenthesize(collection.size());
          }
          break;

        case DATE:
          JustDate date = prm.supportsUsers() ? prm.getDate(userId) : prm.getDate();

          if (date != null) {
            value = Format.renderDate(date);
          }
          break;

        case DATETIME:
          DateTime dateTime = prm.supportsUsers() ? prm.getDateTime(userId) : prm.getDateTime();

          if (dateTime != null) {
            value = Format.renderDateTime(dateTime);
          }
          break;

        case MAP:
          Map<String, String> map = prm.supportsUsers() ? prm.getMap(userId) : prm.getMap();

          if (!BeeUtils.isEmpty(map)) {
            value = "..." + BeeUtils.parenthesize(map.size());
          }
          break;

        case NUMBER:
          Number number = prm.supportsUsers() ? prm.getNumber(userId) : prm.getNumber();

          if (number != null) {
            value = number.toString();
          }
          break;

        case RELATION:
          value = row.getProperty(COL_RELATION);
          break;

        case TEXT:
          value = prm.supportsUsers() ? prm.getText(userId) : prm.getText();
          break;

        case TIME:
          Long time = prm.supportsUsers() ? prm.getTime(userId) : prm.getTime();

          if (time != null) {
            value = TimeUtils.renderTime(time, false);
          }
          break;
      }
      return value;
    }

  }

  public static void open(String module) {
    Assert.notEmpty(module);
    openGrid(module, ViewHelper.getPresenterCallback());
  }

  public static void open(String module, ViewCallback callback) {
    Assert.notEmpty(module);
    Assert.notNull(callback);
    openGrid(module, ViewFactory.getPresenterCallback(callback));
  }

  private static void openGrid(String module, PresenterCallback callback) {
    GridFactory.openGrid(TBL_PARAMETERS, new ParametersGrid(module), null, callback);
  }

  private final String module;
  private final Map<String, BeeParameter> params = new HashMap<>();
  private final Long userId;

  private ParametersGrid(String module) {
    this.module = Assert.notEmpty(module);
    this.userId = BeeKeeper.getUser().getUserId();
  }

  @Override
  public void beforeRefresh(GridPresenter presenter) {
    params.clear();
    super.beforeRefresh(presenter);
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
    return BeeUtils.joinWords(Localized.dictionary().parameters(),
        BeeUtils.parenthesize(message));
  }

  @Override
  public GridInterceptor getInstance() {
    return new ParametersGrid(module);
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {

    if (BeeUtils.same(columnName, COL_PARAMETER_VALUE)) {
      return new ValueRenderer(DataUtils.getColumnIndex(COL_PARAMETER_NAME, dataColumns),
          DataUtils.getColumnIndex(COL_PARAMETER_VALUE, dataColumns), cellSource);
    }
    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }

  @Override
  public String getSupplierKey() {
    return ViewFactory.SupplierKind.PARAMETERS.getKey(module);
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    gridDescription.setFilter(Filter.custom(VAR_PARAMETERS_MODULE, module));
    return true;
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    event.consume();
    final String column = event.getColumnId();
    final IsRow row = event.getRowValue();
    final BeeParameter prm = params.get(row.getString(getDataIndex(COL_PARAMETER_NAME)));

    if (BeeUtils.same(column, COL_PARAMETER_NAME) && prm.supportsUsers()
        && BeeKeeper.getUser().isAdministrator()) {

      prm.setUserSupport(false);
      row.removeProperty(COL_USER);
      row.setProperty(COL_RELATION, row.getProperty(VAR_PARAMETER_DEFAULT + COL_RELATION));

      getGridView().refreshBySource(COL_PARAMETER_NAME);
      getGridView().refreshBySource(COL_PARAMETER_VALUE);
      return;
    }
    if (!BeeUtils.same(column, COL_PARAMETER_VALUE) || event.isReadOnly()) {
      return;
    }
    if (event.isDelete()) {
      set(prm, prm.getDefValue(),
          prm.supportsUsers() ? row.getProperty(VAR_PARAMETER_DEFAULT + COL_RELATION) : null);
    } else {
      switch (prm.getType()) {
        case BOOLEAN:
          if (EditStartEvent.isClickOrEnter(event.getCharCode())) {
            set(prm, BeeUtils.isTrue(prm.supportsUsers() ? prm.getBoolean(userId)
                : prm.getBoolean()) ? null : BeeConst.STRING_TRUE, null);
          }
          break;

        case COLLECTION:
          Global.inputCollection(prm.getName(), Localized.dictionary().parameter(),
              BeeUtils.toBoolean(prm.getOptions()),
              prm.supportsUsers() ? prm.getCollection(userId) : prm.getCollection(),
              result -> set(prm, Codec.beeSerialize(result), null), null);
          break;

        case MAP:
          Global.inputMap(prm.getName(), Localized.dictionary().parameter(),
              Localized.dictionary().value(),
              prm.supportsUsers() ? prm.getMap(userId) : prm.getMap(),
              result -> set(prm, Codec.beeSerialize(result), null));
          break;

        default:
          final Popup popup = new Popup(OutsideClick.CLOSE, null);
          final Editor editor = getEditor(prm, event);

          final ScheduledCommand executor = () -> {
            if (popup.isShowing()) {
              popup.close();
            }
            List<String> errors = editor.validate(true);

            if (!BeeUtils.isEmpty(errors)) {
              Global.showError(errors);
            } else {
              String selection = null;

              if (editor instanceof UnboundSelector) {
                UnboundSelector selector = (UnboundSelector) editor;
                selector.render(selector.getRelatedRow());
                selection = selector.getRenderedValue();
              }
              set(prm, editor.getValue(), selection);
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

          editor.addEditStopHandler(e -> executor.execute());
          popup.setWidget(editor);

          popup.addCloseHandler(ev -> {
            if (ev.mouseOutside()) {
              executor.execute();
            }
            getGridView().getGrid().refocus();
          });
          final char character = (char) event.getCharCode();

          popup.addOpenHandler(ev -> {
            editor.setFocus(true);
            editor.startEdit(editor.getNormalizedValue(), character, EditorAction.REPLACE, null);
          });
          popup.showOnTop(event.getSourceElement());
          break;
      }
    }
  }

  private Editor getEditor(BeeParameter prm, EditStartEvent event) {
    Editor editor;

    switch (prm.getType()) {
      case DATE:
        editor = new InputDate();
        ((InputDate) editor).setDate(prm.supportsUsers() ? prm.getDate(userId) : prm.getDate());
        break;

      case DATETIME:
        editor = new InputDateTime();
        ((InputDateTime) editor).setDateTime(prm.supportsUsers()
            ? prm.getDateTime(userId) : prm.getDateTime());
        break;

      case NUMBER:
        editor = new InputNumber();
        Number n = prm.supportsUsers() ? prm.getNumber(userId) : prm.getNumber();
        Double d = null;

        if (n != null) {
          d = n.doubleValue();
        }
        ((InputNumber) editor).setValue(d);
        break;

      case RELATION:
        Pair<String, String> relData = Pair.restore(prm.getOptions());
        ArrayList<String> cols = Lists.newArrayList(relData.getB());
        Relation relation = Relation.create(relData.getA(), cols);
        relation.disableEdit();
        relation.disableNewRow();

        UnboundSelector selector = UnboundSelector.create(relation, cols);
        Long rel = prm.supportsUsers() ? prm.getRelation(userId) : prm.getRelation();
        selector.setValue(rel != null ? rel.toString() : null);
        selector.setDisplayValue(event.getRowValue().getProperty(COL_RELATION));
        editor = selector;
        break;

      case TEXT:
        editor = new InputText();
        editor.setValue(prm.supportsUsers() ? prm.getText(userId) : prm.getText());
        break;

      case TIME:
        editor = BeeUtils.toBoolean(prm.getOptions()) ? new InputTimeOfDay() : new InputTime();
        Long time = prm.supportsUsers() ? prm.getTime(userId) : prm.getTime();

        if (time != null) {
          editor.setValue(TimeUtils.renderTime(time, false));
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

  private void set(BeeParameter param, String value, String selection) {
    if (!BeeUtils.equalsTrimRight(param.supportsUsers()
        ? param.getValue(userId) : param.getValue(), value)) {

      if (param.supportsUsers()) {
        param.setValue(userId, value);
      } else {
        param.setValue(value);
      }
      Global.setParameter(param.getName(), value, !param.supportsUsers());

      getGridView().getActiveRow().setProperty(COL_RELATION, selection);
      getGridView().refreshBySource(COL_PARAMETER_VALUE);
    }
  }
}