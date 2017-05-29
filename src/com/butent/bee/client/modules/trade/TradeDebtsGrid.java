package com.butent.bee.client.modules.trade;

import com.google.gwt.dom.client.Element;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Storage;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.RowCountChangeEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.trade.DebtKind;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class TradeDebtsGrid extends AbstractGridInterceptor {

  private static final String STYLE_COMMAND_DISCHARGE =
      TradeKeeper.STYLE_PREFIX + "debt-command-discharge";
  private static final String STYLE_PHASE_TOGGLE = TradeKeeper.STYLE_PREFIX + "debts-phase-toggle";

  private static final String PARENT_FILTER_KEY = "parent";
  private static final String PHASE_FILTER_KEY = "phase";

  private final DebtKind debtKind;

  private String dischargerCaption;
  private Consumer<GridView> discharger;

  private EnumSet<TradeDocumentPhase> phases = EnumSet.noneOf(TradeDocumentPhase.class);
  private String phasesStorageKey;

  TradeDebtsGrid(DebtKind debtKind) {
    this.debtKind = debtKind;
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeDebtsGrid(debtKind);
  }

  public DebtKind getDebtKind() {
    return debtKind;
  }

  @Override
  public Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions) {
    Map<String, Filter> filters = new HashMap<>();
    filters.put(PARENT_FILTER_KEY, Filter.isFalse());

    return filters;
  }

  public void initDischarger(String caption, Consumer<GridView> consumer) {
    this.dischargerCaption = caption;
    this.discharger = consumer;
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    event.consume();

    IsRow row = event.getRowValue();
    if (row != null) {
      RowEditor.open(getViewName(), row, Opener.NEW_TAB);
    }
  }

  @Override
  public void onLoad(GridView gridView) {
    GridPresenter presenter = getGridPresenter();
    HeaderView header = (presenter == null) ? null : presenter.getHeader();

    if (header != null) {
      header.clearCommandPanel();

      initPhases(gridView);
      presenter.getDataProvider().setParentFilter(PHASE_FILTER_KEY, buildPhaseFilter());

      for (TradeDocumentPhase phase : TradeDocumentPhase.values()) {
        CheckBox checkBox = new CheckBox(phase.getCaption());
        checkBox.setChecked(phases.contains(phase));
        checkBox.addStyleName(STYLE_PHASE_TOGGLE);

        DomUtils.setDataIndex(checkBox.getElement(), phase.ordinal());

        checkBox.addClickHandler(event -> {
          Element target = EventUtils.getEventTargetElement(event);
          Integer index = DomUtils.getDataIndexInt(DomUtils.getParentByClassName(target,
              STYLE_PHASE_TOGGLE, true));

          TradeDocumentPhase ph = EnumUtils.getEnumByIndex(TradeDocumentPhase.class, index);

          if (ph != null) {
            if (phases.contains(ph)) {
              phases.remove(ph);
            } else {
              phases.add(ph);
            }

            onPhaseChange();
          }
        });

        header.addCommandItem(checkBox);
      }

      if (!BeeUtils.isEmpty(dischargerCaption)) {
        Button discharge = new Button(dischargerCaption);
        discharge.addStyleName(STYLE_COMMAND_DISCHARGE);

        discharge.addClickHandler(event -> {
          if (discharger != null && !gridView.isEmpty()) {
            discharger.accept(gridView);
          }
        });

        header.addCommandItem(discharge);
      }
    }

    gridView.getGrid().addMutationHandler(e -> SummaryChangeEvent.maybeFire(gridView));
    gridView.getGrid().addSelectionCountChangeHandler(e -> SummaryChangeEvent.maybeFire(gridView));

    super.onLoad(gridView);
  }

  @Override
  public boolean onRowCountChange(GridView gridView, RowCountChangeEvent event) {
    return false;
  }

  void onParentChange(Long company, Long currency, DateTime dateTo, DateTime termTo) {
    Filter filter = buildParentFilter(company, currency, dateTo, termTo);
    maybeRefresh(PARENT_FILTER_KEY, filter);
  }

  private void onPhaseChange() {
    storePhases();
    maybeRefresh(PHASE_FILTER_KEY, buildPhaseFilter());
  }

  private Filter buildParentFilter(Long company, Long currency, DateTime dateTo, DateTime termTo) {
    if (DataUtils.isId(company)) {
      List<String> args = new ArrayList<>();
      args.add(Codec.pack(debtKind));

      args.add(BeeUtils.toStringOrNull(company));
      args.add(BeeUtils.toStringOrNull(currency));

      args.add((dateTo == null) ? null : BeeUtils.toString(dateTo.getTime()));
      args.add((termTo == null) ? null : BeeUtils.toString(termTo.getTime()));

      return Filter.custom(FILTER_HAS_TRADE_DEBT, args);

    } else {
      return Filter.isFalse();
    }
  }

  private Filter buildPhaseFilter() {
    if (phases.isEmpty() || phases.containsAll(EnumSet.allOf(TradeDocumentPhase.class))) {
      return null;
    } else {
      return Filter.any(COL_TRADE_DOCUMENT_PHASE, phases);
    }
  }

  private String getPhasesStorageKey() {
    return phasesStorageKey;
  }

  private void setPhasesStorageKey(String phasesStorageKey) {
    this.phasesStorageKey = phasesStorageKey;
  }

  private void initPhases(GridView gridView) {
    FormView form = ViewHelper.getForm(gridView);
    String value = null;

    if (form != null) {
      String key = Storage.getUserKey(form.getFormName() + gridView.getGridName(), "phases");
      setPhasesStorageKey(key);

      value = BeeKeeper.getStorage().get(key);
    }

    phases.clear();

    if (BeeUtils.isEmpty(value)) {
      phases.addAll(Arrays.stream(TradeDocumentPhase.values())
          .filter(TradeDocumentPhase::modifyStock)
          .collect(Collectors.toSet()));

    } else if (!BeeConst.STRING_MINUS.equals(value)) {
      phases.addAll(EnumUtils.parseIndexSet(TradeDocumentPhase.class, value));
    }
  }

  private void storePhases() {
    if (!BeeUtils.isEmpty(getPhasesStorageKey())) {
      String value = phases.isEmpty() ? BeeConst.STRING_MINUS : EnumUtils.joinIndexes(phases);
      BeeKeeper.getStorage().set(getPhasesStorageKey(), value);
    }
  }

  private void maybeRefresh(String key, Filter filter) {
    GridPresenter presenter = getGridPresenter();

    if (presenter != null && presenter.getDataProvider().setParentFilter(key, filter)) {
      getGridView().getGrid().clearSelection();
      presenter.handleAction(Action.REFRESH);
    }
  }
}
