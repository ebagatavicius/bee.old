package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.GridDescription;

class ProjectsGrid extends AllProjectsGrid {

  private static final Long USER_ID = BeeKeeper.getUser().getUserId();

  @Override
  public GridInterceptor getInstance() {
    return new ProjectsGrid();
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    gridDescription.setFilter(getGridFilter());
    return true;
  }


  private static Filter getGridFilter() {
    Filter isOwner = Filter.equals(COL_PROJECT_OWNER, USER_ID);
    Filter isProjectUser =
        Filter.in(Data.getIdColumn(VIEW_PROJECTS), VIEW_PROJECT_USERS, COL_PROJECT, Filter.equals(
            AdministrationConstants.COL_USER, USER_ID));
    return Filter.or(isOwner, isProjectUser);
  }

}
