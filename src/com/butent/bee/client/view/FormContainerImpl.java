package com.butent.bee.client.view;

import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.ElementSize;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.view.add.AddEndEvent;
import com.butent.bee.client.view.add.AddStartEvent;
import com.butent.bee.client.view.form.FormImpl;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.navigation.PagerView;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Handles such visible components of forms as header and footer.
 */

public class FormContainerImpl extends Split implements FormContainerView, HasNavigation,
    HasSearch, ActiveRowChangeEvent.Handler, AddStartEvent.Handler, AddEndEvent.Handler {

  private Presenter viewPresenter;

  private String headerId;
  private String footerId;
  private String commandId;

  private int commandHeight = 36;

  private boolean hasSearch;

  private Evaluator rowMessage;

  private boolean enabled = true;

  private String currentCaption;

  private boolean hasData;
  private int initialRowCount = BeeConst.UNDEF;

  private boolean started;

  public FormContainerImpl() {
    super(-1);
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    if (ReadyEvent.maybeDelegate(this, getForm())) {
      return addHandler(handler, ReadyEvent.getType());
    } else {
      return null;
    }
  }

  @Override
  public void bind() {
    if (hasFooter()) {
      getForm().getDisplay().addSelectionCountChangeHandler(getFooter());
    }

    if (getRowMessage() != null) {
      getForm().getDisplay().addActiveRowChangeHandler(this);
    }

    getForm().addAddStartHandler(this);
    getForm().addAddEndHandler(this);
  }

  @Override
  public void create(FormDescription formDescription, List<BeeColumn> dataColumns, int rowCount,
      FormInterceptor interceptor) {
    Assert.notNull(formDescription);

    setHasData(!BeeUtils.isEmpty(dataColumns));
    setInitialRowCount(rowCount);

    setHasSearch(hasData());

    Set<Action> enabledActions = formDescription.getEnabledActions();
    Set<Action> disabledActions = formDescription.getDisabledActions();

    if (!disabledActions.contains(Action.PRINT)) {
      enabledActions.add(Action.PRINT);
    }

    HeaderView header = new HeaderImpl();
    header.create(formDescription.getCaption(), hasData(), formDescription.isReadOnly(),
        formDescription.getViewName(), EnumSet.of(UiOption.ROOT),
        enabledActions, disabledActions, Action.NO_ACTIONS);

    FormView content = new FormImpl(formDescription.getName());
    content.create(formDescription, null, dataColumns, true, interceptor);

    FooterView footer;
    if (hasData() && (interceptor == null || interceptor.hasFooter(rowCount))) {
      footer = new FooterImpl();
      footer.create(rowCount, true, false, hasSearch());
    } else {
      footer = null;
    }

    addNorth(header, header.getHeight());
    setHeaderId(header.getWidgetId());

    if (footer != null) {
      addSouth(footer, footer.getHeight());
      setFooterId(footer.getWidgetId());
    }

    if (hasData()) {
      Image confirm = new Image(Global.getImages().ok(), new Command() {
        @Override
        public void execute() {
          getForm().prepareForInsert();
        }
      });

      Image cancel = new Image(Global.getImages().cancel(), new Command() {
        @Override
        public void execute() {
          getForm().finishNewRow(null);
        }
      });

      Absolute panel = new Absolute();
      panel.add(confirm);
      panel.add(cancel);

      StyleUtils.setLeft(confirm, 10);
      StyleUtils.setRight(cancel, 10);
      StyleUtils.makeAbsolute(confirm);
      StyleUtils.makeAbsolute(cancel);

      addSouth(panel, 0);
      setCommandId(panel.getId());
      panel.setVisible(false);
    }

    add(content);

    if (hasData()) {
      Calculation rmc = formDescription.getRowMessage();
      if (rmc != null) {
        setRowMessage(Evaluator.create(rmc, null, dataColumns));
      }
    }
  }

  @Override
  public String getCaption() {
    return hasHeader() ? getHeader().getCaption() : null;
  }

  public int getCommandHeight() {
    return commandHeight;
  }

  public Widget getCommandPanel() {
    if (BeeUtils.isEmpty(getCommandId())) {
      return null;
    }
    for (Widget widget : getChildren()) {
      if (BeeUtils.same(DomUtils.getId(widget), getCommandId())) {
        return widget;
      }
    }
    return null;
  }

  @Override
  public Domain getDomain() {
    FormInterceptor interceptor = getForm().getFormInterceptor();
    return (interceptor == null) ? null : interceptor.getDomain();
  }

  public FooterView getFooter() {
    if (BeeUtils.isEmpty(getFooterId())) {
      return null;
    }
    for (Widget widget : getChildren()) {
      if (widget instanceof FooterView) {
        return (FooterView) widget;
      }
    }
    return null;
  }

  @Override
  public FormView getForm() {
    if (getCenter() instanceof FormView) {
      return (FormView) getCenter();
    } else {
      return null;
    }
  }

  @Override
  public HeaderView getHeader() {
    if (BeeUtils.isEmpty(getHeaderId())) {
      return null;
    }
    for (Widget widget : getChildren()) {
      if (widget instanceof HeaderView) {
        return (HeaderView) widget;
      }
    }
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "form-container";
  }

  @Override
  public Collection<PagerView> getPagers() {
    if (hasData()) {
      return ViewHelper.getPagers(this);
    } else {
      return Sets.newHashSet();
    }
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public Collection<SearchView> getSearchers() {
    if (hasSearch()) {
      return ViewHelper.getSearchers(this);
    } else {
      return Sets.newHashSet();
    }
  }

  @Override
  public String getSupplierKey() {
    FormInterceptor interceptor = getForm().getFormInterceptor();
    String key = (interceptor == null) ? null : interceptor.getSupplierKey();

    if (BeeUtils.isEmpty(key)) {
      return FormFactory.getSupplierKey(getForm().getFormName());
    } else {
      return key;
    }
  }

  @Override
  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  public boolean hasFooter() {
    return !BeeUtils.isEmpty(getFooterId());
  }

  public boolean hasHeader() {
    return !BeeUtils.isEmpty(getHeaderId());
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onActiveRowChange(ActiveRowChangeEvent event) {
    if (event == null || event.getRowValue() == null || getRowMessage() == null) {
      return;
    }
    getRowMessage().update(event.getRowValue());
    String message = getRowMessage().evaluate();

    if (!BeeUtils.isEmpty(message)) {
      getHeader().setMessage(message);
    }
  }

  @Override
  public void onAddEnd(AddEndEvent event) {
    if (event.isPopup()) {
      return;
    }

    if (hasHeader()) {
      getHeader().setCaption(getCurrentCaption());
      getHeader().setEnabled(true);
    }
    if (hasFooter()) {
      setWidgetSize(getFooter().asWidget(), getFooter().getHeight());
      getFooter().asWidget().setVisible(true);
      getFooter().setEnabled(true);
    }

    showNewRowCommands(false);
  }

  @Override
  public void onAddStart(AddStartEvent event) {
    if (event.isPopup()) {
      return;
    }

    if (hasHeader() && !BeeUtils.isEmpty(event.getCaption())) {
      getHeader().setEnabled(false);
      setCurrentCaption(getHeader().getCaption());
      getHeader().setCaption(event.getCaption());
    }
    if (hasFooter()) {
      getFooter().setEnabled(false);
      getFooter().asWidget().setVisible(false);
      setWidgetSize(getFooter().asWidget(), 0);
    }

    showNewRowCommands(true);
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    boolean ok;

    if (getId().equals(source.getId())) {
      ElementSize.copyWithAdjustment(source, target, getForm().getPrintElement());
      ok = true;

    } else if (hasHeader() && getHeader().asWidget().getElement().isOrHasChild(source)) {
      ok = getForm().printHeader() && getHeader().onPrint(source, target);

    } else if (hasFooter() && getFooter().asWidget().getElement().isOrHasChild(source)) {
      ok = getForm().printFooter() && getFooter().onPrint(source, target);

    } else {
      ok = true;
    }
    return ok;
  }

  @Override
  public void onStateChange(State state) {
    FormInterceptor interceptor = getForm().getFormInterceptor();

    if (interceptor != null) {
      interceptor.onStateChange(state);
    }
  }

  @Override
  public boolean reactsTo(Action action) {
    FormView form = getForm();
    return form != null && form.reactsTo(action);
  }

  public void setCommandHeight(int commandHeight) {
    this.commandHeight = commandHeight;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled != isEnabled()) {
      this.enabled = enabled;
      UiHelper.enableChildren(this, enabled);
    }
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;

    for (Widget child : this) {
      if (child instanceof View && ((View) child).getViewPresenter() == null) {
        ((View) child).setViewPresenter(viewPresenter);
      }
    }
  }

  public void start(int rowCount) {
    if (getForm() != null && hasData() && rowCount >= 0) {
      Collection<PagerView> pagers = getPagers();
      if (pagers != null) {
        for (PagerView pager : pagers) {
          pager.start(getForm().getDisplay());
        }
      }
      getForm().start(rowCount);

    } else {
      getForm().start(null);
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (!started) {
      start(getInitialRowCount());
      started = true;
    }
  }

  @Override
  protected void onUnload() {
    if (getViewPresenter() != null) {
      getViewPresenter().onViewUnload();
    }
    super.onUnload();
  }

  private String getCommandId() {
    return commandId;
  }

  private String getCurrentCaption() {
    return currentCaption;
  }

  private String getFooterId() {
    return footerId;
  }

  private String getHeaderId() {
    return headerId;
  }

  private int getInitialRowCount() {
    return initialRowCount;
  }

  private Evaluator getRowMessage() {
    return rowMessage;
  }

  private boolean hasData() {
    return hasData;
  }

  private boolean hasSearch() {
    return hasSearch;
  }

  private void setCommandId(String commandId) {
    this.commandId = commandId;
  }

  private void setCurrentCaption(String currentCaption) {
    this.currentCaption = currentCaption;
  }

  private void setFooterId(String footerId) {
    this.footerId = footerId;
  }

  private void setHasData(boolean hasData) {
    this.hasData = hasData;
  }

  private void setHasSearch(boolean hasSearch) {
    this.hasSearch = hasSearch;
  }

  private void setHeaderId(String headerId) {
    this.headerId = headerId;
  }

  private void setInitialRowCount(int initialRowCount) {
    this.initialRowCount = initialRowCount;
  }

  private void setRowMessage(Evaluator rowMessage) {
    this.rowMessage = rowMessage;
  }

  private void showNewRowCommands(boolean show) {
    Widget widget = getCommandPanel();
    if (widget != null) {
      setWidgetSize(widget, show ? getCommandHeight() : 0);
      widget.setVisible(show);
    }
  }
}
