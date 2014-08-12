package com.butent.bee.client.screen;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class Spaces {

  private static final class Item {
    private final long id;

    private int ordinal;
    private String label;
    private boolean startup;

    private final String workspace;

    private Item(long id, int ordinal, String label, boolean startup, String workspace) {
      this.id = id;
      this.ordinal = ordinal;
      this.label = label;
      this.startup = startup;
      this.workspace = workspace;
    }

    private long getId() {
      return id;
    }

    private String getLabel() {
      return label;
    }

    private int getOrdinal() {
      return ordinal;
    }

    private String getWorkspace() {
      return workspace;
    }

    private boolean isStartup() {
      return startup;
    }

    private void open(boolean append) {
      BeeKeeper.getScreen().restore(Lists.newArrayList(workspace), append);
    }

    private void setLabel(String label) {
      this.label = label;
    }

    private void setStartup(boolean startup) {
      this.startup = startup;
    }
  }

  private static final class ItemWidget extends Flow {
    private final Item item;

    private ItemWidget(Item item) {
      super(STYLE_ITEM);
      this.item = item;

      Toggle startup = new Toggle(FontAwesome.SQUARE_O, FontAwesome.HOME, STYLE_STARTUP,
          item.isStartup());
      startup.setTitle(Localized.getConstants().workspaceStartup());

      startup.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (event.getSource() instanceof Toggle) {
            boolean value = ((Toggle) event.getSource()).isChecked();
            getItem().setStartup(value);

            Queries.update(VIEW_WORKSPACES, getItem().getId(), COL_STARTUP,
                BooleanValue.getInstance(value));
          }
        }
      });

      add(startup);

      CustomDiv label = new CustomDiv(STYLE_LABEL);
      label.setText(item.getLabel());

      if (Global.isDebug()) {
        label.setTitle(item.getWorkspace().replace(BeeConst.CHAR_COMMA, BeeConst.CHAR_EOL));
      }

      DomUtils.preventSelection(label);

      label.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          getItem().open(EventUtils.hasModifierKey(event.getNativeEvent()));
        }
      });

      add(label);

      FaLabel edit = new FaLabel(FontAwesome.EDIT, STYLE_EDIT);
      edit.setTitle(Localized.getConstants().actionRename());

      edit.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          int maxLength = Data.getColumnPrecision(VIEW_WORKSPACES, COL_LABEL);

          Global.inputString(Localized.getConstants().bookmarkName(), null, new StringCallback() {
            @Override
            public void onSuccess(String value) {
              setLabel(value);
            }
          }, getItem().getLabel(), maxLength, getElement(), LABEL_INPUT_WIDTH,
              LABEL_INPUT_WIDTH_UNIT);
        }
      });

      add(edit);

      FaLabel delete = new FaLabel(FontAwesome.TRASH_O, STYLE_DELETE);
      delete.setTitle(Localized.getConstants().actionRemove());

      delete.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Global.confirmRemove(Domain.WORKSPACES.getCaption(), getItem().getLabel(),
              new ConfirmationCallback() {
                @Override
                public void onConfirm() {
                  Queries.deleteRow(VIEW_WORKSPACES, getItem().getId());
                  removeFromParent();
                }
              }, getElement());
        }
      });

      add(delete);
    }

    private Item getItem() {
      return item;
    }

    private void setLabel(String label) {
      if (!BeeUtils.isEmpty(label) && !BeeUtils.equalsTrim(label, item.getLabel())) {
        Queries.update(VIEW_WORKSPACES, item.getId(), COL_LABEL, new TextValue(label.trim()));
        getItem().setLabel(label.trim());

        for (Widget widget : this) {
          if (widget.getElement().hasClassName(STYLE_LABEL)) {
            widget.getElement().setInnerText(label.trim());
            break;
          }
        }
      }
    }
  }

  private static final String STYLE_PREFIX = StyleUtils.CLASS_NAME_PREFIX + "spaces-";

  private static final String STYLE_ITEM = STYLE_PREFIX + "item";

  private static final String STYLE_STARTUP = STYLE_PREFIX + "startup";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_EDIT = STYLE_PREFIX + "edit";
  private static final String STYLE_DELETE = STYLE_PREFIX + "delete";

  private static final String COL_ORDINAL = "Ordinal";
  private static final String COL_LABEL = "Label";
  private static final String COL_STARTUP = "Startup";
  private static final String COL_WORKSPACE = "Workspace";

  private static final double LABEL_INPUT_WIDTH = 320;
  private static final CssUnit LABEL_INPUT_WIDTH_UNIT = CssUnit.PX;

  private final Flow panel = new Flow(STYLE_PREFIX + "panel");

  public Spaces() {
    super();
  }

  public void bookmark(String label, final String workspace) {
    Assert.notEmpty(workspace);

    if (!DataUtils.isId(BeeKeeper.getUser().getUserId())) {
      return;
    }

    final ItemWidget itemWidget = find(workspace);

    String defValue = (itemWidget == null) ? label : itemWidget.getItem().getLabel();
    int maxLength = Data.getColumnPrecision(VIEW_WORKSPACES, COL_LABEL);

    Global.inputString(Localized.getConstants().bookmarkName(), null, new StringCallback() {
      @Override
      public void onSuccess(String value) {
        if (itemWidget == null) {
          addItem(BeeUtils.trim(value), workspace);

        } else {
          itemWidget.setLabel(value);
          activate();
        }
      }
    }, defValue, maxLength, null, LABEL_INPUT_WIDTH, LABEL_INPUT_WIDTH_UNIT);
  }

  public IdentifiableWidget getPanel() {
    return panel;
  }

  public List<String> getStartup() {
    List<String> result = new ArrayList<>();

    if (!panel.isEmpty()) {
      for (Widget widget : panel) {
        if (widget instanceof ItemWidget) {
          Item item = ((ItemWidget) widget).getItem();

          if (item.isStartup()) {
            result.add(item.getWorkspace());
          }
        }
      }
    }

    return result;
  }

  public boolean isEmpty() {
    return panel.isEmpty();
  }

  public void load(String serialized) {
    if (!panel.isEmpty()) {
      panel.clear();
    }

    if (BeeUtils.isEmpty(serialized)) {
      return;
    }

    BeeRowSet rowSet = BeeRowSet.restore(serialized);
    if (DataUtils.isEmpty(rowSet)) {
      return;
    }

    int ordinalIndex = rowSet.getColumnIndex(COL_ORDINAL);
    Assert.nonNegative(ordinalIndex, COL_ORDINAL);

    int labelIndex = rowSet.getColumnIndex(COL_LABEL);
    Assert.nonNegative(labelIndex, COL_LABEL);

    int startupIndex = rowSet.getColumnIndex(COL_STARTUP);
    Assert.nonNegative(startupIndex, COL_STARTUP);

    int workspaceIndex = rowSet.getColumnIndex(COL_WORKSPACE);
    Assert.nonNegative(workspaceIndex, COL_WORKSPACE);

    for (BeeRow row : rowSet) {
      int ordinal = BeeUtils.unbox(row.getInteger(ordinalIndex));
      String label = BeeUtils.trim(row.getString(labelIndex));
      boolean startup = BeeUtils.unbox(row.getBoolean(startupIndex));
      String workspace = BeeUtils.trim(row.getString(workspaceIndex));

      Item item = new Item(row.getId(), ordinal, label, startup, workspace);
      add(item);
    }
  }

  private void activate() {
    if (!BeeKeeper.getScreen().containsDomainEntry(Domain.WORKSPACES, null)) {
      BeeKeeper.getScreen().addDomainEntry(Domain.WORKSPACES, getPanel(), null, null);
    }
    BeeKeeper.getScreen().activateDomainEntry(Domain.WORKSPACES, null);
  }

  private void add(Item item) {
    panel.add(new ItemWidget(item));
  }

  private void addItem(final String label, final String workspace) {
    final int ordinal = getNextOrdinal();

    List<BeeColumn> columns = Data.getColumns(VIEW_WORKSPACES,
        Lists.newArrayList(COL_USER, COL_ORDINAL, COL_LABEL, COL_WORKSPACE));
    List<String> values = Lists.newArrayList(BeeUtils.toString(BeeKeeper.getUser().getUserId()),
        BeeUtils.toString(ordinal), label, workspace);

    Queries.insert(VIEW_WORKSPACES, columns, values, null, new RowCallback() {
      @Override
      public void onSuccess(BeeRow row) {
        Item item = new Item(row.getId(), ordinal, label, false, workspace);
        add(item);

        activate();

        if (panel.getWidgetCount() > 1) {
          DomUtils.scrollToBottom(panel);
        }
      }
    });
  }

  private ItemWidget find(String workspace) {
    if (panel.isEmpty()) {
      return null;
    }

    for (Widget widget : panel) {
      if (widget instanceof ItemWidget) {
        Item item = ((ItemWidget) widget).getItem();

        if (BeeUtils.equalsTrim(item.getWorkspace(), workspace)) {
          return (ItemWidget) widget;
        }
      }
    }
    return null;
  }

  private int getNextOrdinal() {
    int ordinal = -1;

    if (!panel.isEmpty()) {
      for (Widget widget : panel) {
        if (widget instanceof ItemWidget) {
          Item item = ((ItemWidget) widget).getItem();
          ordinal = Math.max(ordinal, item.getOrdinal());
        }
      }
    }

    return ++ordinal;
  }
}
