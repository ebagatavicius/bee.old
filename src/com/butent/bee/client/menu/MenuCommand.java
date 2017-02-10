package com.butent.bee.client.menu;

import com.google.gwt.core.client.Scheduler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.FORM_STAGES;

import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.administration.ParametersGrid;
import com.butent.bee.client.modules.administration.StageEditorForm;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuService;

public class MenuCommand implements Scheduler.ScheduledCommand {

  private static final BeeLogger logger = LogUtils.getLogger(MenuCommand.class);

  private final MenuService service;
  private final String parameters;

  public MenuCommand(MenuService service, String parameters) {
    this.service = service;
    this.parameters = parameters;
  }

  @Override
  public void execute() {
    switch (service) {
      case FORM:
        FormFactory.openForm(parameters);
        break;

      case GRID:
        GridFactory.openGrid(parameters);
        break;

      case NEW:
        RowFactory.createRow(parameters);
        break;

      case PARAMETERS:
        ParametersGrid.open(parameters);
        break;

      case REPORT:
        Report.open(parameters);
        break;

      case STAGES:
        FormFactory.openForm(FORM_STAGES, new StageEditorForm(parameters));
        break;

      default:
        if (service.getHandler() == null) {
          logger.warning("menu handler not available for", service);
        } else {
          service.getHandler().onSelection(parameters);
        }
    }
  }

  public String getParameters() {
    return parameters;
  }

  public MenuService getService() {
    return service;
  }
}
