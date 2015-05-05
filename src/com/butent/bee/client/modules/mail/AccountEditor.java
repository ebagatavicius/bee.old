package com.butent.bee.client.modules.mail;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.WidgetInitializer;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.mail.MailConstants.Protocol;
import com.butent.bee.shared.modules.mail.MailConstants.RuleAction;
import com.butent.bee.shared.modules.mail.MailConstants.RuleCondition;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccountEditor extends AbstractFormInterceptor implements SelectorEvent.Handler {

  private class RuleRenderer extends AbstractCellRenderer {
    private final int conditionIdx;
    private final int conditionOptionsIdx;
    private final int actionIdx;
    private final int actionOptionsIdx;

    public RuleRenderer(List<? extends IsColumn> dataColumns) {
      super(null);

      conditionIdx = DataUtils.getColumnIndex(COL_RULE_CONDITION, dataColumns);
      conditionOptionsIdx = DataUtils.getColumnIndex(COL_RULE_CONDITION_OPTIONS, dataColumns);
      actionIdx = DataUtils.getColumnIndex(COL_RULE_ACTION, dataColumns);
      actionOptionsIdx = DataUtils.getColumnIndex(COL_RULE_ACTION_OPTIONS, dataColumns);
    }

    @Override
    public String render(IsRow row) {
      String conditionOptions = row.getString(conditionOptionsIdx);

      if (!BeeUtils.isEmpty(conditionOptions)) {
        conditionOptions = "<strong>" + Codec.escapeHtml(conditionOptions) + "</strong>,";
      }
      RuleAction action = EnumUtils.getEnumByIndex(RuleAction.class, row.getInteger(actionIdx));
      String actionOptions = row.getString(actionOptionsIdx);

      if (!BeeUtils.isEmpty(actionOptions)) {
        if (EnumSet.of(RuleAction.COPY, RuleAction.MOVE).contains(action)) {
          actionOptions = folders.get(BeeUtils.toLongOrNull(actionOptions));
        }
        actionOptions = "<strong>" + Codec.escapeHtml(actionOptions) + "</strong>";
      }
      return BeeUtils.joinWords(EnumUtils.getEnumByIndex(RuleCondition.class,
          row.getInteger(conditionIdx)).getCaption(), conditionOptions, action.getCaption(),
          actionOptions);
    }
  }

  private final Map<Long, String> folders = new HashMap<>();

  @Override
  public void afterCreateWidget(final String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof DataSelector
        && BeeUtils.inListSame(name,
            SystemFolder.Sent.name() + COL_FOLDER,
            SystemFolder.Drafts.name() + COL_FOLDER,
            SystemFolder.Trash.name() + COL_FOLDER)) {

      ((DataSelector) widget).addSelectorHandler(this);

    } else if (widget instanceof HasClickHandlers) {
      if (BeeUtils.inListSame(name, COL_STORE_PASSWORD, COL_TRANSPORT_PASSWORD)) {
        ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Global.inputString(Localized.getConstants().mailNewAccountPassword(), null,
                new StringCallback(false) {
                  @Override
                  public void onSuccess(String value) {
                    getFormView().getActiveRow().setValue(getFormView().getDataIndex(name),
                        BeeUtils.isEmpty(value) ? null : Codec.encodeBase64(value));
                  }
                }, null, null, BeeConst.UNDEF, null, BeeConst.DOUBLE_UNDEF, null, BeeConst.UNDEF,
                Localized.getConstants().ok(), Localized.getConstants().cancel(),
                new WidgetInitializer() {
                  @Override
                  public Widget initialize(Widget inputWidget, String widgetName) {
                    if (BeeUtils.same(widgetName, DialogConstants.WIDGET_INPUT)) {
                      inputWidget.getElement().setPropertyString("type", "password");
                    }
                    return inputWidget;
                  }
                });
          }
        });
      } else if (BeeUtils.inListSame(name, COL_STORE_PROPERTIES, COL_TRANSPORT_PROPERTIES)) {
        ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            final IsRow row = getActiveRow();
            final int index = getDataIndex(name);
            if (index == BeeConst.UNDEF) {
              return;
            }
            Global.inputMap(BeeUtils.joinWords(Localized.getConstants().properties(),
                BeeUtils.parenthesize(BeeUtils.same(name, COL_TRANSPORT_PROPERTIES)
                    ? Protocol.SMTP.name() : row.getString(getDataIndex(COL_STORE_TYPE)))),
                Localized.getConstants().property(), Localized.getConstants().value(),
                Codec.deserializeMap(row.getString(index)),
                new Consumer<Map<String, String>>() {
                  @Override
                  public void accept(Map<String, String> input) {
                    row.setValue(index, Codec.beeSerialize(input));
                  }
                });
          }
        });
      }
    } else if (widget instanceof ChildGrid && BeeUtils.same(name, TBL_RULES)) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public GridInterceptor getInstance() {
          return null;
        }

        @Override
        public AbstractCellRenderer getRenderer(String columnName,
            List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
            CellSource cellSource) {

          if (BeeUtils.same(columnName, COL_RULE)) {
            return new RuleRenderer(dataColumns);
          }
          return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
        }
      });
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    Queries.getRow(getViewName(), result.getId(), new RowUpdateCallback(getViewName()));
    super.afterInsertRow(result, forced);
  }

  @Override
  public FormInterceptor getInstance() {
    return new AccountEditor();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isOpened()) {
      Set<Long> exclusions = new HashSet<>();

      for (SystemFolder folder : SystemFolder.values()) {
        exclusions.add(getLongValue(folder + COL_FOLDER));
      }
      event.consume();
      event.getSelector().setAdditionalFilter(Filter.equals(COL_ACCOUNT, getActiveRowId()));
      event.getSelector().getOracle().setExclusions(exclusions);
    }
  }

  @Override
  public void onSetActiveRow(IsRow row) {
    folders.clear();

    Queries.getRowSet(TBL_FOLDERS, null, Filter.equals(COL_ACCOUNT, row.getId()),
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            int nameIdx = result.getColumnIndex(COL_FOLDER_NAME);
            int parentIdx = result.getColumnIndex(COL_FOLDER_PARENT + COL_FOLDER_NAME);

            for (BeeRow beeRow : result) {
              folders.put(beeRow.getId(),
                  BeeUtils.join("/", beeRow.getString(parentIdx), beeRow.getString(nameIdx)));
            }
          }
        });
    super.onSetActiveRow(row);
  }
}
