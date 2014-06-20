package com.butent.bee.client;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.ModuleManager;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.CustomWidget;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.HandlesDeleteEvents;
import com.butent.bee.shared.data.event.HandlesUpdateEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.EnumSet;
import java.util.List;

public class Search {

  private static final class ResultPanel extends Flow implements HandlesDeleteEvents,
      HandlesUpdateEvents, Presenter, View, Printable {

    private static final String STYLE_RESULT_CONTAINER = "bee-SearchResultContainer";

    private static final String STYLE_RESULT_CONTENT = "bee-SearchResultContent";
    private static final String STYLE_RESULT_VIEW = "bee-SearchResultView";

    private final String query;

    private final HeaderView header;
    private int size;

    private final List<HandlerRegistration> handlerRegistry = Lists.newArrayList();

    private boolean enabled = true;

    private ResultPanel(String query, List<SearchResult> results) {
      super();
      this.query = query;
      this.size = results.size();

      this.addStyleName(STYLE_RESULT_CONTAINER);

      this.header = new HeaderImpl();
      header.create(query, false, true, null, EnumSet.of(UiOption.ROOT),
          EnumSet.of(Action.PRINT, Action.CLOSE), Action.NO_ACTIONS, Action.NO_ACTIONS);

      header.setViewPresenter(this);
      header.setMessage(getMessage());

      this.add(header);

      Flow content = new Flow();
      content.addStyleName(STYLE_RESULT_CONTENT);

      String viewName = null;
      DataInfo dataInfo = null;

      for (SearchResult result : results) {
        if (!result.getViewName().equals(viewName)) {
          viewName = result.getViewName();
          dataInfo = Data.getDataInfo(viewName);

          String viewCaption = Localized.maybeTranslate(dataInfo.getCaption());
          Label label = new Label(BeeUtils.notEmpty(viewCaption, viewName));
          label.addStyleName(STYLE_RESULT_VIEW);
          content.add(label);
        }

        ResultWidget widget = new ResultWidget(viewName, result.getRow());
        widget.render(query, dataInfo);

        content.add(widget);
      }
      this.add(content);

      this.handlerRegistry.addAll(BeeKeeper.getBus().registerDeleteHandler(this, false));
      this.handlerRegistry.addAll(BeeKeeper.getBus().registerUpdateHandler(this, false));
    }

    @Override
    public String getCaption() {
      return header.getCaption();
    }

    @Override
    public String getEventSource() {
      return null;
    }

    @Override
    public HeaderView getHeader() {
      return header;
    }

    @Override
    public String getIdPrefix() {
      return "search-results";
    }

    @Override
    public View getMainView() {
      return this;
    }

    @Override
    public Element getPrintElement() {
      return getElement();
    }

    @Override
    public Presenter getViewPresenter() {
      return this;
    }

    @Override
    public IdentifiableWidget getWidget() {
      return this;
    }

    @Override
    public String getWidgetId() {
      return getId();
    }

    @Override
    public void handleAction(Action action) {
      switch (action) {
        case CANCEL:
        case CLOSE:
          BeeKeeper.getScreen().closeWidget(this);
          break;

        case PRINT:
          Printer.print(this);
          break;

        default:
          Assert.untouchable(action.getCaption() + " not implemented");
      }
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void onCellUpdate(CellUpdateEvent event) {
      ResultWidget widget = findWidget(this, event.getViewName(), event.getRowId());
      if (widget != null) {
        event.applyTo(widget.getRow());

        DataInfo dataInfo = Data.getDataInfo(event.getViewName());
        if (dataInfo != null) {
          widget.render(query, dataInfo);
        }
      }
    }

    @Override
    public void onMultiDelete(MultiDeleteEvent event) {
      int count = 0;
      for (RowInfo rowInfo : event.getRows()) {
        ResultWidget widget = findWidget(this, event.getViewName(), rowInfo.getId());
        if (widget != null && removeResultWidget(widget)) {
          count++;
        }
      }

      if (count > 0) {
        setSize(getSize() - count);
        updateMessage();
      }
    }

    @Override
    public boolean onPrint(Element source, Element target) {
      if (getId().equals(source.getId())) {
        int height = 0;
        for (Widget child : this) {
          height += child.getElement().getScrollHeight();
        }
        StyleUtils.setHeight(target, height);
        return true;

      } else if (header.asWidget().getElement().isOrHasChild(source)) {
        return header.onPrint(source, target);

      } else {
        return true;
      }
    }

    @Override
    public void onRowDelete(RowDeleteEvent event) {
      ResultWidget widget = findWidget(this, event.getViewName(), event.getRowId());
      if (widget != null && removeResultWidget(widget)) {
        setSize(getSize() - 1);
        updateMessage();
      }
    }

    @Override
    public void onRowUpdate(RowUpdateEvent event) {
      ResultWidget widget = findWidget(this, event.getViewName(), event.getRowId());
      if (widget != null) {
        BeeRow row = DataUtils.cloneRow(event.getRow());
        widget.setRow(row);

        DataInfo dataInfo = Data.getDataInfo(event.getViewName());
        if (dataInfo != null) {
          widget.render(query, dataInfo);
        }
      }
    }

    @Override
    public void onViewUnload() {
    }

    @Override
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    @Override
    public void setEventSource(String eventSource) {
    }

    @Override
    public void setViewPresenter(Presenter viewPresenter) {
    }

    @Override
    protected void onUnload() {
      for (HandlerRegistration entry : handlerRegistry) {
        if (entry != null) {
          entry.removeHandler();
        }
      }
      super.onUnload();
    }

    private ResultWidget findWidget(HasWidgets container, String viewName, long rowId) {
      for (Widget child : container) {
        if (child instanceof ResultWidget) {
          ResultWidget widget = (ResultWidget) child;
          if (widget.getRow().getId() == rowId && BeeUtils.same(widget.getViewName(), viewName)) {
            return widget;
          }

        } else if (child instanceof HasWidgets) {
          ResultWidget widget = findWidget((HasWidgets) child, viewName, rowId);
          if (widget != null) {
            return widget;
          }
        }
      }
      return null;
    }

    private String getMessage() {
      return BeeUtils.bracket(getSize());
    }

    private int getSize() {
      return size;
    }

    private static boolean removeResultWidget(ResultWidget widget) {
      Widget container = widget.getParent();
      if (container instanceof HasWidgets) {
        return ((HasWidgets) container).remove(widget);
      } else {
        return false;
      }
    }

    private void setSize(int size) {
      this.size = size;
    }

    private void updateMessage() {
      header.setMessage(getMessage());
    }
  }

  private static final class ResultWidget extends CustomWidget implements HasViewName {

    private static final String STYLE_RESULT_ROW = "bee-SearchResultRow";
    private static final String STYLE_RESULT_MATCH = "bee-SearchResultMatch";

    private final String viewName;
    private BeeRow row;

    private ResultWidget(String viewName, BeeRow row) {
      super(Document.get().createPElement(), STYLE_RESULT_ROW);

      this.viewName = viewName;
      this.row = row;

      sinkEvents(Event.ONCLICK);
    }

    @Override
    public String getIdPrefix() {
      return "found";
    }

    @Override
    public String getViewName() {
      return viewName;
    }

    @Override
    public void onBrowserEvent(Event event) {
      if (EventUtils.isClick(event)) {
        RowEditor.open(getViewName(), getRow(), Opener.NEW_TAB);
      }
      super.onBrowserEvent(event);
    }

    private BeeRow getRow() {
      return row;
    }

    private void render(String query, DataInfo dataInfo) {
      if (isAttached()) {
        DomUtils.clear(getElement());
      }

      String text = transformRow(dataInfo);

      String nt = text.toLowerCase();
      String nq = query.toLowerCase();

      int ql = query.length();

      int start = 0;
      int end = nt.indexOf(nq);

      while (end >= 0) {
        if (end > start) {
          Element span = Document.get().createSpanElement();
          span.setInnerText(text.substring(start, end));
          getElement().appendChild(span);
        }

        Element match = Document.get().createSpanElement();
        match.setClassName(STYLE_RESULT_MATCH);
        match.setInnerText(text.substring(end, end + ql));
        getElement().appendChild(match);

        start = end + ql;
        end = nt.indexOf(nq, start);
      }

      if (start < text.length()) {
        Element span = Document.get().createSpanElement();
        span.setInnerText(text.substring(start));
        getElement().appendChild(span);
      }
    }

    private void setRow(BeeRow row) {
      this.row = row;
    }

    private String transformRow(DataInfo dataInfo) {
      RowTransformEvent event = new RowTransformEvent(getViewName(), getRow());
      BeeKeeper.getBus().fireEvent(event);

      if (BeeUtils.isEmpty(event.getResult())) {
        return DataUtils.join(dataInfo, getRow(), BeeConst.STRING_SPACE);
      } else {
        return event.getResult();
      }
    }
  }

  private static final String STYLE_SEARCH_PANEL = "bee-MainSearchContainer";
  private static final String STYLE_INPUT = "bee-MainSearchBox";

  private static final String STYLE_OPTIONS_CONTAINER = "bee-MainSearchOptionsContainer";
  private static final String STYLE_OPTIONS = "bee-MainSearchOptions";
  private static final String STYLE_SUBMIT_CONTAINER = "bee-MainSearchSubmitContainer";
  private static final String STYLE_SUBMIT = "bee-MainSearchSubmit";

  private static final String KEY_INPUT = "main-search";
  
  private static final int MIN_SEARCH_PHRASE_LENGTH = 3;

  private Panel searchPanel;
  private InputText input;

  Search() {
    super();
  }

  public void focus() {
    if (getInput() != null) {
      getInput().setFocus(true);
    }
  }

  Widget ensureSearchWidget() {
    if (getSearchPanel() == null) {
      createSearchPanel();
    }
    return getSearchPanel();
  }

  private void createSearchPanel() {
    setSearchPanel(new Flow());
    getSearchPanel().addStyleName(STYLE_SEARCH_PANEL);

    setInput(new InputText());
    DomUtils.setSearch(getInput());
    AutocompleteProvider.enableAutocomplete(getInput(), KEY_INPUT);

    getInput().addStyleName(STYLE_INPUT);

    getInput().addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          event.preventDefault();
          submit();
        }
      }
    });

    getSearchPanel().add(getInput());

    Simple optionsContainer = new Simple();
    optionsContainer.addStyleName(STYLE_OPTIONS_CONTAINER);

    Image options = new Image(Global.getImages().searchOptions());
    options.addStyleName(STYLE_OPTIONS);

    optionsContainer.setWidget(options);
    getSearchPanel().add(optionsContainer);

    Simple submitContainer = new Simple();
    submitContainer.addStyleName(STYLE_SUBMIT_CONTAINER);

    Image submit = new Image(Global.getImages().search());
    submit.addStyleName(STYLE_SUBMIT);

    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        submit();
      }
    });

    submitContainer.setWidget(submit);
    getSearchPanel().add(submitContainer);
  }

  private InputText getInput() {
    return input;
  }

  private Panel getSearchPanel() {
    return searchPanel;
  }

  private static void processResults(String query, List<SearchResult> results) {
    if (results.size() == 1) {
      RowEditor.open(results.get(0).getViewName(), results.get(0).getRow(), Opener.modeless());
    } else {
      showResults(query, results);
    }
  }

  private void setInput(InputText input) {
    this.input = input;
  }

  private void setSearchPanel(Panel panel) {
    this.searchPanel = panel;
  }

  private static void showResults(String query, List<SearchResult> results) {
    ResultPanel resultPanel = new ResultPanel(query, results);
    BeeKeeper.getScreen().showWidget(resultPanel);
  }

  private void submit() {
    final String value = getInput().getValue();
    if (!BeeUtils.isEmpty(value)) {

      if (value.trim().length() < MIN_SEARCH_PHRASE_LENGTH) {
        BeeKeeper.getScreen().notifyWarning(
            Localized.getMessages().minSearchQueryLength(MIN_SEARCH_PHRASE_LENGTH));
        return;
      }

      ParameterList params = BeeKeeper.getRpc().createParameters(Service.SEARCH);
      params.addPositionalData(value.trim());

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          String[] arr = Codec.beeDeserializeCollection((String) response.getResponse());
          final List<SearchResult> results = Lists.newArrayList();

          if (arr != null) {
            for (String s : arr) {
              SearchResult result = SearchResult.restore(s);
              if (result != null) {
                results.add(result);
              }
            }
          }

          if (results.isEmpty()) {
            BeeKeeper.getScreen().notifyWarning(value, Localized.getConstants().nothingFound());

          } else {
            AutocompleteProvider.retainValue(getInput());
            
            ModuleManager.maybeInitialize(new Command() {
              @Override
              public void execute() {
                processResults(value, results);
              }
            });
          }
        }
      });
    }
  }
}
