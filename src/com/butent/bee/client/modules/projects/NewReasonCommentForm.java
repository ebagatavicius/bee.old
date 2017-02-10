package com.butent.bee.client.modules.projects;

import com.google.common.collect.Maps;
import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.projects.ProjectConstants.ProjectEvent;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;

class NewReasonCommentForm extends AbstractFormInterceptor {

  private static final String WIDGET_NAME_FROM = "From";
  private static final String WIDGET_NAME_TO = "To";
  private static final String WIDGET_NAME_REASON = "Reason";
  private static final String WIDGET_NAME_DOCUMENT_LABEL = "DocumentLabel";
  private static final String WIDGET_NAME_DOCUMENT = "Document";
  private static final String WIDGET_EDIT_INFO = "EditInfo";

  private final FormView projectForm;
  private final IsRow projectRow;
  private final CellValidateEvent projectValidator;

  private UnboundSelector reasonSelector;
  private UnboundSelector documentSelector;
  private Label editInfoLabel;
  private Label documentLabel;
  private Flow fromFlow;
  private Flow toFlow;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {
    switch (name) {
      case WIDGET_NAME_REASON:
        if (widget instanceof UnboundSelector) {
          reasonSelector = (UnboundSelector) widget;
          reasonSelector.addSelectorHandler(new SelectorEvent.Handler() {

            @Override
            public void onDataSelector(SelectorEvent event) {
              UnboundSelector docSelector = getDocumentSelector();
              Label docLabel = getDocumentLabel();
              if (docSelector != null) {
                docSelector.setNullable(!isRequiredDocument());
              }

              if (docLabel != null) {
                docLabel.setStyleName(StyleUtils.NAME_REQUIRED, isRequiredDocument());
              }

            }
          });
        }

        break;

      case WIDGET_NAME_DOCUMENT:
        if (widget instanceof UnboundSelector) {
          documentSelector = (UnboundSelector) widget;
        }
        break;
      case WIDGET_NAME_DOCUMENT_LABEL:
        if (widget instanceof Label) {
          documentLabel = (Label) widget;
        }
        break;

      case WIDGET_EDIT_INFO:
        if (widget instanceof Label) {
          editInfoLabel = (Label) widget;
        }
        break;

      case WIDGET_NAME_FROM:
        if (widget instanceof Flow) {
          fromFlow = (Flow) widget;
        }
        break;

      case WIDGET_NAME_TO:
        if (widget instanceof Flow) {
          toFlow = (Flow) widget;
        }
        break;
      default:
        break;
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new NewReasonCommentForm(projectForm, projectRow, projectValidator);
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, final ReadyForInsertEvent event) {
    event.consume();

    FormView form = getFormView();

    if (form == null) {
      event.getCallback().onFailure("FormView object not found");
      return;
    }

    if (projectForm == null) {
      event.getCallback().onFailure("projectForm FormView object not found");
      return;
    }

    if (projectRow == null) {
      event.getCallback().onFailure("projectRow object not found");
      return;
    }

    IsRow row = form.getActiveRow();

    if (row == null) {
      event.getCallback().onFailure("activeRow object not found");
      return;
    }

    final String viewName = form.getViewName();

    if (BeeUtils.isEmpty(viewName)) {
      event.getCallback().onFailure("Form not view name defined");
      return;
    }

    String comment = null;

    if (!BeeUtils.isNegative(form.getDataIndex(COL_COMMENT))) {
      comment = row.getString(form.getDataIndex(COL_COMMENT));
    }

    String changedView = projectForm.getViewName();
    DataInfo data = Data.getDataInfo(changedView);

    String changedColumn = null;
    String oldValue = null;
    String newValue = null;
    String reason = null;
    String document = null;
    String documentLink = null;

    if (projectValidator != null) {
      newValue = projectValidator.getNewValue();
      changedColumn = projectValidator.getColumnId();

      int idxOldVal = projectForm.getDataIndex(changedColumn);

      if (!BeeConst.isUndef(idxOldVal) && projectForm.getOldRow() != null) {
        oldValue = projectForm.getOldRow().getString(idxOldVal);

        if (data.hasRelation(changedColumn)) {
          oldValue =
              ProjectsHelper.getDisplayValue(changedView, changedColumn, oldValue, projectForm
                  .getOldRow());
          newValue =
              ProjectsHelper.getDisplayValue(changedView, changedColumn, newValue, projectForm
                  .getActiveRow());
        }
      }
    }

    if (reasonSelector != null) {
      reason = reasonSelector.getDisplayValue();
    }

    if (documentSelector != null) {
      document = documentSelector.getValue();
      documentLink = documentSelector.getDisplayValue();
    }

    Map<String, Map<String, String>> oldData = Maps.newHashMap();
    Map<String, Map<String, String>> newData = Maps.newHashMap();

    Map<String, String> oldProjectData = Maps.newHashMap();
    Map<String, String> newProjectData = Maps.newHashMap();
    Map<String, String> reasonData = Maps.newHashMap();

    oldProjectData.put(changedColumn, oldValue);
    newProjectData.put(changedColumn, newValue);
    oldData.put(changedView, oldProjectData);
    newData.put(changedView, newProjectData);

    reasonData.put(PROP_REASON, reason);
    reasonData.put(PROP_DOCUMENT, document);
    reasonData.put(PROP_DOCUMENT_LINK, documentLink);
    oldData.put(PROP_REASON_DATA, reasonData);
    newData.put(PROP_REASON_DATA, reasonData);

    ProjectsHelper.registerProjectEvent(viewName, ProjectEvent.EDIT, projectRow.getId(), comment,
        newData,
        oldData, new Callback<BeeRow>() {

          @Override
          public void onSuccess(BeeRow result) {
            event.getCallback().onSuccess(result);
            RowInsertEvent.fire(BeeKeeper.getBus(), viewName, result, event.getSourceId());
          }
        });

  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    if (editInfoLabel != null) {
      DateTime time = new DateTime();
      String user =
          BeeUtils.joinWords(BeeKeeper.getUser().getFirstName(), BeeKeeper.getUser().getLastName());
      editInfoLabel.setHtml(BeeUtils.joinWords(Format.renderDateTime(time), user));
    }

    if (fromFlow != null && projectForm != null && projectValidator != null) {
      int idx = projectForm.getDataIndex(projectValidator.getColumnId());

      if (!BeeConst.isUndef(idx) && projectForm.getOldRow() != null) {
        String value = projectForm.getOldRow().getString(idx);

        if (BeeUtils.isEmpty(value)) {
          value = Localized.dictionary().filterNullLabel();
        } else {
          value = ProjectsHelper.getDisplayValue(projectForm.getViewName(),
              projectValidator.getColumnId(), value, projectForm.getOldRow());
        }

        fromFlow.getElement().setInnerText(value);
      }
    }

    if (toFlow != null && projectValidator != null) {
      String value = projectValidator.getNewValue();

      if (BeeUtils.isEmpty(value)) {
        value = Localized.dictionary().filterNullLabel();
      } else {
        value =
            ProjectsHelper.getDisplayValue(projectForm.getViewName(),
                projectValidator.getColumnId(), value, projectForm.getActiveRow(),
                new Callback<String>() {

              @Override
              public void onSuccess(String result) {
                if (BeeUtils.isEmpty(result)) {
                  return;
                }

                Flow tf = getToFlow();

                if (tf == null) {
                  return;
                }

                tf.getElement().setInnerText(result);
              }
            });
      }

      toFlow.getElement().setInnerText(value);
    }
  }

  NewReasonCommentForm(FormView projectForm, IsRow projectRow, CellValidateEvent projectValidator) {
    this.projectForm = projectForm;
    this.projectRow = projectRow;
    this.projectValidator = projectValidator;
  }

  private UnboundSelector getDocumentSelector() {
    return documentSelector;
  }

  private Label getDocumentLabel() {
    return documentLabel;
  }

  private Flow getToFlow() {
    return toFlow;
  }

  private boolean isRequiredDocument() {
    if (reasonSelector == null) {
      return false;
    }

    long id = BeeUtils.toLong(reasonSelector.getValue());

    if (!DataUtils.isId(id)) {
      return false;
    }

    BeeRowSet rs = reasonSelector.getOracle().getViewData();
    int idxDocRequired = rs.getColumnIndex(COL_DOCUMENT_REQUIRED);

    if (BeeConst.isUndef(idxDocRequired)) {
      return false;
    }

    return BeeUtils.unbox(rs.getRowById(id).getBoolean(idxDocRequired));
  }

}
