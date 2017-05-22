package com.butent.bee.client.modules.ec;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.view.SearchByCar;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcCarType;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ArticleCarsGridInterceptor extends AbstractGridInterceptor implements
    BeforeSelectionHandler<EcCarType> {

  private static final String STYLE_PREFIX = EcStyles.name("add-article-cars-");

  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_SAVE = STYLE_PREFIX + "save";
  private static final String STYLE_CLOSE = STYLE_PREFIX + "close";

  private static final String STYLE_VIEW = STYLE_PREFIX + "view";

  private static final String STYLE_SELECTION_PREFIX = STYLE_PREFIX + "selection-";
  private static final String STYLE_SELECTION_PANEL = STYLE_SELECTION_PREFIX + "panel";
  private static final String STYLE_SELECTION_TABLE = STYLE_SELECTION_PREFIX + "table";
  private static final String STYLE_SELECTION_REMOVE = STYLE_SELECTION_PREFIX + "remove";

  private static String getDialogCaption(GridView grid) {
    FormView form = ViewHelper.getForm(grid.asWidget());
    IsRow itemRow = (form == null) ? null : form.getActiveRow();

    if (itemRow == null) {
      return Localized.dictionary().ecItemDetailsCarTypes();
    } else {
      return BeeUtils.joinWords(form.getStringValue(COL_TCD_ARTICLE_NAME),
          form.getStringValue(COL_TCD_ARTICLE_NR),
          Localized.dictionary().ecItemDetailsCarTypes());
    }
  }

  private final List<Long> selectedCarTypes = new ArrayList<>();

  private Flow selectionPanel;

  ArticleCarsGridInterceptor() {
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    EcKeeper.ensureClientStyleSheet();

    if (!selectedCarTypes.isEmpty()) {
      selectedCarTypes.clear();
    }

    Set<Long> exclusions = new HashSet<>();

    List<? extends IsRow> data = presenter.getGridView().getRowData();
    if (!BeeUtils.isEmpty(data)) {
      int index = getDataIndex(COL_TCD_TYPE);
      for (IsRow row : data) {
        exclusions.add(row.getLong(index));
      }
    }

    SearchByCar widget = new SearchByCar(exclusions);
    widget.addStyleName(STYLE_VIEW);

    setSelectionPanel(new Flow(STYLE_SELECTION_PANEL));
    widget.getMainPanel().add(getSelectionPanel());

    widget.addBeforeSelectionHandler(this);

    final DialogBox dialog = DialogBox.withoutCloseBox(getDialogCaption(presenter.getGridView()),
        STYLE_DIALOG);

    FaLabel save = new FaLabel(FontAwesome.SAVE);
    save.addStyleName(STYLE_SAVE);

    save.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        dialog.close();
        saveSelectedCars();
      }
    });

    dialog.addAction(Action.SAVE, save);

    FaLabel close = new FaLabel(FontAwesome.CLOSE);
    close.addStyleName(STYLE_CLOSE);

    close.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (selectedCarTypes.isEmpty()) {
          dialog.close();

        } else {
          Global.decide(Localized.dictionary().tcdTypes(),
              Lists.newArrayList(Localized.dictionary().saveChanges()), new DecisionCallback() {
                @Override
                public void onConfirm() {
                  saveSelectedCars();
                  dialog.close();
                }

                @Override
                public void onDeny() {
                  dialog.close();
                }
              }, DialogConstants.DECISION_YES);
        }
      }
    });

    dialog.addAction(Action.CLOSE, close);

    dialog.setWidget(widget);
    dialog.center();

    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new ArticleCarsGridInterceptor();
  }

  @Override
  public void onBeforeSelection(BeforeSelectionEvent<EcCarType> event) {
    if (event.getItem() != null) {
      addSelection(event.getItem());

      if (getSelectionPanel() != null) {
        DomUtils.scrollToBottom(getSelectionPanel());
      }
    }

    event.cancel();
  }

  private void addSelection(EcCarType carType) {
    final long typeId = carType.getTypeId();
    if (selectedCarTypes.contains(typeId) || getSelectionPanel() == null) {
      return;
    }

    selectedCarTypes.add(typeId);

    final HtmlTable table = ensureSelectionTable();

    int row = table.getRowCount();
    int col = 0;

    table.setText(row, col++, carType.getManufacturer());
    table.setText(row, col++, carType.getModelName());

    table.setText(row, col++,
        EcUtils.formatProduced(carType.getProducedFrom(), carType.getProducedTo()));
    table.setText(row, col++, carType.getTypeName());
    table.setText(row, col++, carType.getPower());

    table.setText(row, col++, EcUtils.format(carType.getCcm()));
    table.setText(row, col++, EcUtils.format(carType.getCylinders()));
    table.setText(row, col++, EcUtils.format(carType.getMaxWeight()));

    table.setText(row, col++, carType.getEngine());
    table.setText(row, col++, carType.getFuel());
    table.setText(row, col++, carType.getBody());
    table.setText(row, col++, carType.getAxle());

    FaLabel remove = new FaLabel(FontAwesome.TRASH_O, STYLE_SELECTION_REMOVE);
    remove.setTitle(Localized.dictionary().actionRemove());

    remove.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectedCarTypes.remove(typeId);

        Integer index = table.getEventRow(event, false);
        if (index != null) {
          table.removeRow(index);
        }

        Widget view = getSearchByCar();
        if (view instanceof SearchByCar) {
          ((SearchByCar) view).includeType(typeId);
        }
      }
    });

    table.setWidget(row, col++, remove);
  }

  private SearchByCar getSearchByCar() {
    for (Widget parent = selectionPanel.getParent(); parent != null; parent = parent.getParent()) {
      if (parent instanceof SearchByCar) {
        return (SearchByCar) parent;
      }
    }
    return null;
  }

  private HtmlTable ensureSelectionTable() {
    HtmlTable table = null;

    if (!getSelectionPanel().isEmpty()) {
      for (Widget widget : getSelectionPanel()) {
        if (widget instanceof HtmlTable) {
          table = (HtmlTable) widget;
          break;
        }
      }
    }

    if (table == null) {
      table = new HtmlTable(STYLE_SELECTION_TABLE);
      getSelectionPanel().add(table);
    }

    return table;
  }

  private Flow getSelectionPanel() {
    return selectionPanel;
  }

  private void saveSelectedCars() {
    if (selectedCarTypes.isEmpty()) {
      return;
    }

    getGridView().ensureRelId(new IdCallback() {
      @Override
      public void onSuccess(Long result) {
        if (DataUtils.isId(result) && !selectedCarTypes.isEmpty()) {
          ParameterList params = EcKeeper.createArgs(SVC_ADD_ARTICLE_CAR_TYPES);
          params.addDataItem(COL_TCD_ARTICLE, result);
          params.addDataItem(COL_TCD_TYPE, DataUtils.buildIdList(selectedCarTypes));

          BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              response.notify(BeeKeeper.getScreen());

              if (response.hasResponse()) {
                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getViewName(), result);
              }
            }
          });
        }
      }
    });
  }

  private void setSelectionPanel(Flow selectionPanel) {
    this.selectionPanel = selectionPanel;
  }
}
