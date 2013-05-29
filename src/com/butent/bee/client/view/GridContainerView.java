package com.butent.bee.client.view;

import com.google.gwt.event.dom.client.HasAllDragAndDropHandlers;

import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.ui.HandlesHistory;
import com.butent.bee.client.ui.HasWidgetSupplier;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.GridInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.HasCaption;

import java.util.Collection;
import java.util.List;

public interface GridContainerView extends View, HasAllDragAndDropHandlers, HasGridView, Printable,
    HasCaption, HandlesHistory, HasWidgetSupplier {

  void bind();

  void create(GridDescription gridDescription, GridView gridView, int rowCount, Filter userFilter,
      GridInterceptor gridInterceptor, Collection<UiOption> uiOptions,
      GridFactory.GridOptions gridOptions);

  List<String> getFavorite();

  FooterView getFooter();
  
  HeaderView getHeader();
  
  boolean hasSearch();
}
