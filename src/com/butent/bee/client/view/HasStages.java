package com.butent.bee.client.view;

import com.google.gwt.user.client.ui.HasWidgets;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.administration.Stage;
import com.butent.bee.client.modules.administration.StageUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Objects;

public interface HasStages {

  FormView getFormView();

  HasWidgets getStageContainer();

  List<Stage> getStages();

  default void refreshStages() {
    List<Stage> stages = getStages();

    if (Objects.isNull(stages)) {
      StageUtils.initStages(getFormView().getViewName(), newStages -> {
        setStages(newStages);
        refreshStages();
      });
      return;
    }
    HasWidgets container = getStageContainer();

    if (Objects.isNull(container)) {
      return;
    }
    container.clear();

    StageUtils.filterStages(stages, getFormView().getOldRow())
        .forEach(stage -> {
          Flow button = new Flow("bee-stage");
          button.getElement().setInnerText(stage.getName());

          if (stage.active(getFormView().getActiveRow())) {
            button.addStyleName("bee-stage-active");
          } else {
            button.addClickHandler(ev -> {
              if (BeeUtils.isEmpty(stage.getConfirm())) {
                updateStage(stage);
              } else {
                Global.confirm(stage.getConfirm(), () -> updateStage(stage));
              }
            });
          }
          container.add(button);
        });
  }

  void setStages(List<Stage> stages);

  default void triggerStage(String trigger) {
    StageUtils.filterStages(getStages(), getFormView().getOldRow()).stream()
        .filter(stage -> stage.hasTrigger(trigger)).findFirst()
        .ifPresent(this::updateStage);
  }

  default void updateStage(Stage stage) {
    FormView form = getFormView();

    int stageIdx = form.getDataIndex(COL_STAGE);
    int nameIdx = form.getDataIndex(COL_STAGE_NAME);
    IsRow row = form.getActiveRow();

    row.setValue(stageIdx, stage.getId());
    row.setValue(nameIdx, stage.getName());

    if (DataUtils.isNewRow(row)) {
      form.refresh();
    } else {
      IsRow oldRow = form.getOldRow();

      form.saveChanges(new RowUpdateCallback(form.getViewName()) {
        @Override
        public void onCancel() {
          row.setValue(stageIdx, oldRow.getLong(stageIdx));
          row.setValue(nameIdx, oldRow.getString(nameIdx));
          super.onCancel();
        }

        @Override
        public void onFailure(String... reason) {
          onCancel();
          super.onFailure(reason);
        }
      });
    }
  }
}
