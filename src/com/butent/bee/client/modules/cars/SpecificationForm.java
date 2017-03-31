package com.butent.bee.client.modules.cars;

import com.google.gwt.event.dom.client.HasClickHandlers;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.richtext.RichTextEditor;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.cars.Specification;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class SpecificationForm extends PrintFormInterceptor implements Consumer<Specification> {

  private Flow objectContainer;
  private Specification objectSpecification;

  @Override
  public void accept(Specification specification) {
    Specification oldSpec = getSpecification();

    if (Objects.nonNull(specification)) {
      commit(specification);

    } else if (Objects.nonNull(oldSpec)) {
      Global.confirmRemove(Localized.dictionary().specification(), oldSpec.getBranchName(),
          () -> commit(null));
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, NameUtils.getClassName(SpecificationBuilder.class))
        && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(clickEvent -> {
        FormView form = getFormView();

        if (!form.isEnabled()) {
          return;
        }
        if (DataUtils.isId(getActiveRowId())) {
          buildSpecification();
        } else if (form.getViewPresenter() instanceof ParentRowCreator) {
          ((ParentRowCreator) form.getViewPresenter()).createParentRow(form,
              result -> buildSpecification());
        }
      });
    } else if (BeeUtils.same(name, COL_OBJECT) && widget instanceof Flow) {
      objectContainer = (Flow) widget;
    }
    super.afterCreateWidget(name, widget, callback);
  }

  public static void commit(FormView form, Map<String, String> updates, Consumer<IsRow> consumer) {
    String viewName = form.getViewName();
    IsRow row = form.getOldRow();

    BeeRowSet rs = DataUtils.getUpdated(viewName, form.getDataColumns(), row, form.getActiveRow(),
        form.getChildrenForUpdate());

    if (DataUtils.isEmpty(rs)) {
      rs = new BeeRowSet(viewName, new ArrayList<>());
      rs.addRow(row.getId(), row.getVersion(), new ArrayList<>());
    }
    for (String col : updates.keySet()) {
      int idx = DataUtils.getColumnIndex(col, rs.getColumns());

      if (BeeConst.isUndef(idx)) {
        idx = rs.getNumberOfColumns();
        rs.addColumn(Data.getColumn(viewName, col));
        rs.getRow(0).setValue(idx, row.getString(form.getDataIndex(col)));
      }
      rs.getRow(0).preliminaryUpdate(idx, updates.get(col));
    }
    Queries.updateRow(rs, new RowCallback() {
      @Override
      public void onSuccess(BeeRow updatedRow) {
        if (Objects.nonNull(consumer)) {
          consumer.accept(updatedRow);
        }
        RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, updatedRow, true);
      }
    });
  }

  @Override
  public FormInterceptor getInstance() {
    return new SpecificationForm();
  }

  public Specification getSpecification() {
    return objectSpecification;
  }

  @Override
  public void onSetActiveRow(IsRow row) {
    Long objectId = getObjectId(row);

    if (DataUtils.isId(objectId)) {
      ParameterList args = CarsKeeper.createSvcArgs(SVC_GET_OBJECT);
      args.addDataItem(COL_OBJECT, objectId);

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(getFormView());

          if (!response.hasErrors()) {
            refreshObject(Specification.restore(response.getResponseAsString()));
          }
        }
      });
    } else {
      refreshObject(null);
    }
    super.onSetActiveRow(row);
  }

  protected void buildSpecification() {
    new SpecificationBuilder(objectSpecification, this);
  }

  protected void commit(Specification specification) {
    Map<String, String> updates = new HashMap<>();
    updates.put(COL_OBJECT,
        Objects.nonNull(specification) ? BeeUtils.toString(specification.getId()) : null);

    Long oldObject = getLongValue(COL_OBJECT);

    commit(getFormView(), updates, updatedRow -> {
      if (DataUtils.isId(oldObject)) {
        Queries.deleteRow(TBL_CONF_OBJECTS, oldObject);
      }
    });
  }

  protected Long getObjectId(IsRow row) {
    if (Objects.nonNull(row)) {
      return row.getLong(getDataIndex(COL_OBJECT));
    }
    return null;
  }

  private void refreshObject(Specification specification) {
    objectSpecification = specification;

    if (objectContainer != null) {
      objectContainer.clear();

      if (objectSpecification != null) {
        Flow flow = new Flow(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);
        objectContainer.add(flow);
        flow.add(objectSpecification.renderSummary(false));

        objectSpecification.getPhotos().values().forEach(val -> {
          Flow thumbnail = new Flow(SpecificationBuilder.STYLE_THUMBNAIL);
          thumbnail.addStyleName(StyleUtils.NAME_FLEXIBLE);
          thumbnail.add(new Image(FileUtils.getUrl(val)));
          flow.insert(thumbnail, flow.getWidgetCount() - 1);
        });
        Flow descriptionBox = new Flow(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);
        Flow descr = new Flow(SpecificationBuilder.STYLE_DESCRIPTION);
        Label descrCap = new Label(Localized.dictionary().description());
        descrCap.setStyleName(StyleUtils.NAME_LINK);
        descr.add(descrCap);
        CustomDiv descrHtml = new CustomDiv();
        descrHtml.setHtml(objectSpecification.getDescription());
        descr.add(descrHtml);

        if (getFormView().isEnabled()) {
          descrCap.addClickHandler(clickEvent -> {
            RichTextEditor area = new RichTextEditor(true);
            area.setValue(objectSpecification.getDescription());
            StyleUtils.setSize(area, BeeUtils.toInt(BeeKeeper.getScreen().getWidth() * 0.6),
                BeeUtils.toInt(BeeKeeper.getScreen().getHeight() * 0.6));

            Global.inputWidget(Localized.dictionary().description(), area, () -> {
              String description = area.getValue();
              ParameterList args = CarsKeeper.createSvcArgs(SVC_SAVE_OBJECT_INFO);
              args.addDataItem(COL_OBJECT, objectSpecification.getId());
              args.addDataItem(COL_KEY, COL_DESCRIPTION);
              args.addNotEmptyData(COL_DESCRIPTION, description);

              BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  response.notify(getFormView());

                  if (!response.hasErrors()) {
                    objectSpecification.setDescription(description);
                    descrHtml.setHtml(description);
                  }
                }
              });
            });
          });
        }
        descriptionBox.add(descr);

        Flow crit = new Flow();
        Label critCap = new Label(Localized.dictionary().criteria());
        critCap.setStyleName(StyleUtils.NAME_LINK);
        crit.add(critCap);
        CustomDiv critHtml = new CustomDiv();
        critHtml.setHtml(SpecificationBuilder.renderCriteria(objectSpecification.getCriteria()));
        crit.add(critHtml);

        if (getFormView().isEnabled()) {
          critCap.addClickHandler(clickEvent -> Global.inputMap(Localized.dictionary().criteria(),
              Localized.dictionary().criterionName(), Localized.dictionary().criterionValue(),
              objectSpecification.getCriteria(), map -> {
                ParameterList args = CarsKeeper.createSvcArgs(SVC_SAVE_OBJECT_INFO);
                args.addDataItem(COL_OBJECT, objectSpecification.getId());
                args.addDataItem(COL_KEY, COL_CRITERIA);
                args.addDataItem(COL_CRITERIA, Codec.beeSerialize(map));

                BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                  @Override
                  public void onResponse(ResponseObject response) {
                    response.notify(getFormView());

                    if (!response.hasErrors()) {
                      objectSpecification.setCriteria(map);
                      critHtml.setHtml(SpecificationBuilder.renderCriteria(map));
                    }
                  }
                });
              }));
        }
        descriptionBox.add(crit);

        objectContainer.add(descriptionBox);
      }
    }
  }
}
