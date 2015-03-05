package com.butent.bee.client.modules.documents;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.HasWidgets;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentTreeForm extends AbstractFormInterceptor
    implements SelectionHandler<IsRow> {

  protected static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Rights-";
  protected static final String STYLE_SUFFIX_CELL = "-cell";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_HOVER = STYLE_PREFIX + "hover";

  private static final String STYLE_ROLE_LABEL = STYLE_PREFIX + "role-label";
  private static final String STYLE_ROLE_LABEL_CELL = STYLE_ROLE_LABEL + STYLE_SUFFIX_CELL;
  private static final String STYLE_STATE_LABEL = STYLE_PREFIX + "state-label";
  private static final String STYLE_STATE_LABEL_CELL = STYLE_STATE_LABEL + STYLE_SUFFIX_CELL;

  private static final String STYLE_VALUE_ROW = STYLE_PREFIX + "value-row";
  private static final String STYLE_VALUE_TOGGLE = STYLE_PREFIX + "value-toggle";
  private static final String STYLE_VALUE_CELL = STYLE_PREFIX + "value-cell";

  private static final String DATA_KEY_ID = "rights-id";
  private static final String DATA_KEY_ROLE = "rights-role";
  private static final String DATA_KEY_STATE = "rights-state";

  private final boolean isManager = BeeKeeper.getUser()
      .isWidgetVisible(RegulatedWidget.DOCUMENT_TREE);

  private TreeView treeView;
  private HasWidgets rightsWidget;

  private List<RightsState> states;
  private Map<String, String> roles;

  private HtmlTable table;
  private int hoverColumn = BeeConst.UNDEF;

  @Override
  public boolean beforeCreateWidget(String name, com.google.gwt.xml.client.Element description) {
    if (BeeUtils.same(name, "Tree") && !isManager) {
      description.setAttribute("hideActions", "true");
    }
    return super.beforeCreateWidget(name, description);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof TreeView) {
      treeView = (TreeView) widget;
      treeView.addSelectionHandler(this);

    } else if (BeeUtils.same(name, AdministrationConstants.TBL_RIGHTS)
        && widget instanceof HasWidgets && isManager) {
      rightsWidget = (HasWidgets) widget;
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new DocumentTreeForm();
  }

  @Override
  public void onSelection(SelectionEvent<IsRow> event) {
    refresh(event.getSelectedItem());
  }

  private Toggle createValueToggle(long id, final String roleName, final RightsState state) {
    Toggle toggle = new Toggle(FontAwesome.EYE_SLASH, FontAwesome.EYE, STYLE_VALUE_TOGGLE, false);

    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_ID, id);
    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_ROLE, roles.get(roleName));
    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_STATE, state.ordinal());

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Toggle) {
          Toggle t = (Toggle) event.getSource();
          String recordId = DomUtils.getDataProperty(t.getElement(), DATA_KEY_ID);
          String roleId = DomUtils.getDataProperty(t.getElement(), DATA_KEY_ROLE);
          String stateIdx = DomUtils.getDataProperty(t.getElement(), DATA_KEY_STATE);

          ParameterList params = DocumentsHandler
              .createArgs(DocumentConstants.SVC_SET_CATEGORY_STATE);

          params.addDataItem("id", recordId);
          params.addDataItem(AdministrationConstants.COL_ROLE, roleId);
          params.addDataItem(AdministrationConstants.COL_STATE, stateIdx);
          params.addDataItem("on", Codec.pack(t.isChecked()));
          treeView.getSelectedItem().setProperty(getPropertyName(roleName, state),
              BeeUtils.toString(t.isChecked()));

          BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              response.notify(getFormView());
            }
          });
        }
      }
    });
    return toggle;
  }

  private void refresh(IsRow selectedItem) {
    if (rightsWidget != null) {
      rightsWidget.clear();

      if (selectedItem != null) {
        if (states == null) {
          states = new ArrayList<>();

          for (String state : Codec.beeDeserializeCollection(treeView.getTreePresenter()
              .getProperty(AdministrationConstants.TBL_RIGHTS))) {
            states.add(EnumUtils.getEnumByName(RightsState.class, state));
          }
        }
        if (roles == null) {
          roles = Codec.deserializeMap(treeView.getTreePresenter()
              .getProperty(AdministrationConstants.TBL_ROLES));
        }
        if (table == null) {
          table = new HtmlTable(STYLE_TABLE);
          table.addStyleName(STYLE_PREFIX + "multi-role");

          table.addMouseMoveHandler(new MouseMoveHandler() {
            @Override
            public void onMouseMove(MouseMoveEvent event) {
              Element target = EventUtils.getTargetElement(event.getNativeEvent().getEventTarget());

              for (Element el = target; el != null; el = el.getParentElement()) {
                if (TableCellElement.is(el)) {
                  int col = ((TableCellElement) el.cast()).getCellIndex();

                  if (getHoverColumn() != col) {
                    onColumnHover(col);
                  }
                } else if (table.getId().equals(el.getId())) {
                  break;
                }
              }
            }
          });
          table.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
              onColumnHover(BeeConst.UNDEF);
            }
          });
        } else if (!table.isEmpty()) {
          table.clear();
        }
        populateTable(selectedItem);
        rightsWidget.add(table);
      }
    }
  }

  private int getHoverColumn() {
    return hoverColumn;
  }

  private String getPropertyName(String roleName, RightsState state) {
    return BeeUtils.join("_", roles.get(roleName), state.ordinal());
  }

  private void onColumnHover(int col) {
    if (getHoverColumn() >= 1) {
      List<TableCellElement> cells = table.getColumnCells(getHoverColumn());
      for (TableCellElement cell : cells) {
        cell.removeClassName(STYLE_HOVER);
      }
    }
    setHoverColumn(col);

    if (col >= 1) {
      List<TableCellElement> cells = table.getColumnCells(col);
      for (TableCellElement cell : cells) {
        cell.addClassName(STYLE_HOVER);
      }
    }
  }

  private void populateTable(IsRow selectedItem) {
    int row = 0;

    for (String roleName : roles.keySet()) {
      if (row == 0) {
        int col = 1;

        for (RightsState state : states) {
          Label widget = new Label(state.getCaption());
          widget.addStyleName(STYLE_STATE_LABEL);
          table.setWidget(row, col++, widget, STYLE_STATE_LABEL_CELL);
        }
        row++;
      }
      int col = 0;
      Label widget = new Label(roleName);
      widget.addStyleName(STYLE_ROLE_LABEL);
      table.setWidget(row, col++, widget, STYLE_ROLE_LABEL_CELL);

      for (RightsState state : states) {
        Toggle toggle = createValueToggle(selectedItem.getId(), roleName, state);
        toggle.setChecked(BeeUtils.toBoolean(selectedItem.getProperty(getPropertyName(roleName,
            state))));
        table.setWidget(row, col++, toggle, STYLE_VALUE_CELL);
      }
      table.getRowFormatter().addStyleName(row++, STYLE_VALUE_ROW);
    }
  }

  private void setHoverColumn(int hoverColumn) {
    this.hoverColumn = hoverColumn;
  }
}
