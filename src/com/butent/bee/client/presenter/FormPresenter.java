package com.butent.bee.client.presenter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.AsyncProvider;
import com.butent.bee.client.data.CachedProvider;
import com.butent.bee.client.data.HasActiveRow;
import com.butent.bee.client.data.HasDataProvider;
import com.butent.bee.client.data.HasDataTable;
import com.butent.bee.client.data.LocalProvider;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.view.FormContainerImpl;
import com.butent.bee.client.view.FormContainerView;
import com.butent.bee.client.view.HasSearch;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.ReadyForUpdateEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.search.FilterHandler;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.ProviderType;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FormPresenter extends AbstractPresenter implements ReadyForInsertEvent.Handler,
    ReadyForUpdateEvent.Handler, HasViewName, HasSearch, HasDataProvider, HasActiveRow {

  private final class DeleteCallback implements ConfirmationCallback {
    private final long rowId;
    private final long version;

    private DeleteCallback(long rowId, long version) {
      this.rowId = rowId;
      this.version = version;
    }

    @Override
    public void onConfirm() {
      Queries.deleteRow(getViewName(), rowId, version, new Queries.IntCallback() {
        @Override
        public void onFailure(String... reason) {
          showFailure(Localized.dictionary().deleteRowError(), reason);
        }

        @Override
        public void onSuccess(Integer result) {
          RowDeleteEvent.fire(BeeKeeper.getBus(), getViewName(), rowId);
        }
      });
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(FormPresenter.class);

  private final FormContainerView formContainer;
  private final Provider dataProvider;

  private final Set<HandlerRegistration> filterChangeHandlers = new HashSet<>();
  private Filter lastFilter;

  public FormPresenter(FormDescription formDescription, String viewName, int rowCount,
      BeeRowSet rowSet, ProviderType providerType, CachingPolicy cachingPolicy,
      FormInterceptor interceptor) {

    List<BeeColumn> columns = (rowSet == null) ? null : rowSet.getColumns();

    this.formContainer = createView(formDescription, columns, rowCount, interceptor);
    this.dataProvider = createProvider(formContainer, viewName, columns, rowSet, providerType,
        cachingPolicy);

    bind();
  }

  @Override
  public IsRow getActiveRow() {
    return getFormView().getActiveRow();
  }

  @Override
  public long getActiveRowId() {
    return DataUtils.getId(getActiveRow());
  }

  @Override
  public String getCaption() {
    return formContainer.getCaption();
  }

  @Override
  public Provider getDataProvider() {
    return dataProvider;
  }

  @Override
  public HeaderView getHeader() {
    return formContainer.getHeader();
  }

  public Filter getLastFilter() {
    return lastFilter;
  }

  @Override
  public View getMainView() {
    return formContainer;
  }

  public NotificationListener getNotificationListener() {
    return getFormView();
  }

  @Override
  public Collection<SearchView> getSearchers() {
    Collection<SearchView> searchers;

    if (getMainView() instanceof HasSearch) {
      searchers = ((HasSearch) getMainView()).getSearchers();
    } else {
      searchers = new HashSet<>();
    }
    return searchers;
  }

  @Override
  public String getViewKey() {
    return formContainer.getSupplierKey();
  }

  @Override
  public String getViewName() {
    if (getDataProvider() == null) {
      return null;
    }
    return getDataProvider().getViewName();
  }

  @Override
  public void handleAction(Action action) {
    Assert.notNull(action);

    if (getFormInterceptor() != null && !getFormInterceptor().beforeAction(action, this)) {
      return;
    }

    switch (action) {
      case CANCEL:
      case CLOSE:
        BeeKeeper.getScreen().closeWidget(getMainView());
        break;

      case CONFIGURE:
        Global.inputString("Options", new StringCallback() {
          @Override
          public void onSuccess(String value) {
            getFormView().applyOptions(value);
          }
        }, null);
        break;

      case DELETE:
        IsRow row = getFormView().getActiveRow();
        if (hasData() && getFormView().isRowEnabled(row)) {
          deleteRow(row.getId(), row.getVersion());
        }
        break;

      case REFRESH:
        if (hasData()) {
          getDataProvider().refresh(true);
        }
        break;

      case ADD:
        if (hasData()) {
          getFormView().startNewRow(false);
        }
        break;

      case PRINT:
        FormView form = getFormView();
        if (form.printHeader() || form.printFooter()) {
          Printer.print(formContainer);
        } else {
          Printer.print(form);
        }
        break;

      case BOOKMARK:
        getFormView().bookmark();
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }
  }

  @Override
  public void onReadyForInsert(final ReadyForInsertEvent event) {
    Queries.insert(getViewName(), event.getColumns(), event.getValues(), event.getChildren(),
        new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            if (event.getCallback() == null) {
              showFailure("Insert Row", reason);
            } else {
              event.getCallback().onFailure(reason);
            }
          }

          @Override
          public void onSuccess(BeeRow result) {
            RowInsertEvent.fire(BeeKeeper.getBus(), getViewName(), result, event.getSourceId());
            if (event.getCallback() != null) {
              event.getCallback().onSuccess(result);
            }
          }
        });
  }

  @Override
  public boolean onReadyForUpdate(final ReadyForUpdateEvent event) {
    BeeRowSet rowSet = event.getRowSet(getViewName(), getDataProvider().getColumns());
    event.getRowValue().reset();

    final boolean rowMode = event.isRowMode() || rowSet.getNumberOfColumns() > 1;

    RowCallback rowCallback = new RowCallback() {
      @Override
      public void onFailure(String... reason) {
        if (event.getCallback() != null) {
          event.getCallback().onFailure(reason);
        }
      }

      @Override
      public void onSuccess(BeeRow row) {
        if (event.getCallback() != null) {
          event.getCallback().onSuccess(row);
        }

        if (rowMode) {
          RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), row);
        } else {

          CellSource source = CellSource.forColumn(event.getColumn(),
              getDataProvider().getColumnIndex(event.getColumn().getId()));
          String value = row.getString(0);

          CellUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), row.getId(), row.getVersion(),
              source, value);
        }
      }
    };

    if (rowMode) {
      Queries.updateRow(rowSet, rowCallback);
    } else {
      Queries.updateCell(rowSet, rowCallback);
    }
    return true;
  }

  @Override
  public void onStateChange(State state) {
    getMainView().setStyleName(StyleUtils.NAME_LOADING,
        state == State.LOADING || state == State.PENDING);
  }

  @Override
  public void onViewUnload() {
    getMainView().setViewPresenter(null);

    EventUtils.clearRegistry(filterChangeHandlers);

    if (getDataProvider() != null) {
      getDataProvider().onUnload();
    }

    super.onViewUnload();
  }

  private void bind() {
    FormContainerView view = formContainer;
    view.setViewPresenter(this);
    view.bind();

    if (hasData()) {
      Collection<SearchView> searchers = getSearchers();

      if (!searchers.isEmpty()) {
        FilterHandler handler = new FilterHandler() {
          @Override
          public Filter getEffectiveFilter(ImmutableSet<String> exclusions) {
            return getDataProvider().getQueryFilter(ViewHelper.getFilter(FormPresenter.this,
                getDataProvider(), exclusions));
          }

          @Override
          public void onFilterChange() {
            FormPresenter.this.updateFilter();
          }
        };

        for (SearchView search : searchers) {
          search.setFilterHandler(handler);
        }
      }

      view.getForm().addReadyForUpdateHandler(this);
      view.getForm().addReadyForInsertHandler(this);
    }
  }

  private Provider createProvider(FormContainerView view, String viewName,
      List<BeeColumn> columns, BeeRowSet rowSet, ProviderType providerType,
      CachingPolicy cachingPolicy) {
    if (BeeUtils.isEmpty(viewName) || providerType == null) {
      return null;
    }

    HasDataTable display = view.getForm().getDisplay();
    NotificationListener notificationListener = view.getForm();
    Provider provider;

    switch (providerType) {
      case ASYNC:
        provider = new AsyncProvider(display, this, null, notificationListener, viewName, columns,
            null, null, null, cachingPolicy, null, null);
        break;
      case CACHED:
        provider = new CachedProvider(display, this, null, notificationListener, viewName, columns,
            rowSet);
        break;
      case LOCAL:
        provider = new LocalProvider(display, this, null, notificationListener, viewName, columns,
            rowSet);
        break;
      default:
        Assert.untouchable();
        provider = null;
    }
    return provider;
  }

  private static FormContainerView createView(FormDescription formDescription,
      List<BeeColumn> columns, int rowCount, FormInterceptor interceptor) {
    FormContainerView view = new FormContainerImpl();

    view.create(formDescription, columns, rowCount, interceptor);
    return view;
  }

  private void deleteRow(long rowId, long version) {
    Global.confirmDelete(getCaption(), Icon.WARNING,
        Collections.singletonList(Localized.dictionary().deleteRecordQuestion()),
        new DeleteCallback(rowId, version));
  }

  private FormInterceptor getFormInterceptor() {
    return getFormView().getFormInterceptor();
  }

  private FormView getFormView() {
    return formContainer.getForm();
  }

  private boolean hasData() {
    return getDataProvider() != null;
  }

  private void showFailure(String activity, String... reasons) {
    List<String> messages = Lists.newArrayList(activity);
    if (reasons != null) {
      messages.addAll(Lists.newArrayList(reasons));
    }
    getNotificationListener().notifySevere(ArrayUtils.toArray(messages));
  }

  private void updateFilter() {
    Filter filter = ViewHelper.getFilter(this, getDataProvider());
    if (Objects.equals(filter, getLastFilter())) {
      logger.info("filter not changed", filter);
    } else {
      lastFilter = filter;
      getDataProvider().tryFilter(filter, null, true);
    }
  }
}
