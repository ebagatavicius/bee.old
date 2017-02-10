package com.butent.bee.client.modules.administration;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.data.Queries;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class StageUtils {

  private static final Table<String, String, String> actions = HashBasedTable.create();
  private static final Table<String, String, String> triggers = HashBasedTable.create();

  private StageUtils() {
  }

  public static List<Stage> filterStages(List<Stage> stages, IsRow row) {
    if (BeeUtils.isEmpty(stages)) {
      return new ArrayList<>();
    }
    return stages.stream().filter(stage -> stage.applies(row)).collect(Collectors.toList());
  }

  public static Stage findStage(List<Stage> stages, Long id) {
    if (!BeeUtils.isEmpty(stages)) {
      for (Stage stage : stages) {
        if (Objects.equals(stage.getId(), id)) {
          return stage;
        }
      }
    }
    return null;
  }

  public static Map<String, String> getActions(String dataView) {
    return actions.row(dataView);
  }

  public static Map<String, String> getTriggers(String dataView) {
    return triggers.row(dataView);
  }

  public static void initStages(String viewName, Consumer<List<Stage>> stagesConsumer) {
    Queries.getRowSet(TBL_STAGES, null, Filter.equals(COL_STAGE_VIEW, viewName),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet stageInfo) {
            List<Stage> stages = new ArrayList<>();

            for (BeeRow row : stageInfo) {
              stages.add(new Stage(viewName, row.getId(),
                  row.getString(stageInfo.getColumnIndex(COL_STAGE_NAME)),
                  row.getString(stageInfo.getColumnIndex(COL_STAGE_CONFIRM))));
            }
            if (stages.isEmpty()) {
              stagesConsumer.accept(stages);
              return;
            }
            Filter filter = Filter.any(COL_STAGE, stages.stream().map(Stage::getId)
                .collect(Collectors.toSet()));

            Queries.getData(Arrays.asList(TBL_STAGE_CONDITIONS, TBL_STAGE_ACTIONS,
                TBL_STAGE_TRIGGERS), ImmutableMap.of(TBL_STAGE_CONDITIONS, filter,
                TBL_STAGE_ACTIONS, filter, TBL_STAGE_TRIGGERS, filter), null,
                new Queries.DataCallback() {
                  @Override
                  public void onSuccess(Collection<BeeRowSet> data) {
                    data.forEach(rs -> rs.getRows().forEach(row -> {
                      Stage stage = findStage(stages, row.getLong(rs.getColumnIndex(COL_STAGE)));

                      switch (rs.getViewName()) {
                        case TBL_STAGE_CONDITIONS:
                          int idx = rs.getColumnIndex(COL_STAGE_FROM);
                          stage.addCondition(COL_STAGE,
                              row.isEmpty(idx) ? Operator.IS_NULL : Operator.EQ,
                              row.getString(idx));
                          break;

                        case TBL_STAGE_ACTIONS:
                          stage.addAction(row.getString(rs.getColumnIndex(COL_STAGE_ACTION)));
                          break;

                        case TBL_STAGE_TRIGGERS:
                          stage.addTrigger(row.getString(rs.getColumnIndex(COL_STAGE_TRIGGER)));
                          break;

                      }
                    }));
                    stagesConsumer.accept(stages);
                  }
                });
          }
        });
  }

  public static void registerStageAction(String dataView, String action, String caption) {
    actions.put(dataView, action, caption);
  }

  public static void registerStageTrigger(String dataView, String trigger, String caption) {
    triggers.put(dataView, trigger, caption);
  }
}
