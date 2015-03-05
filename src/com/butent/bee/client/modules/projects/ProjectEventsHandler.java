package com.butent.bee.client.modules.projects;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.eventsboard.EventsBoard;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.html.builder.Factory;
import com.butent.bee.shared.html.builder.elements.B;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants.ProjectEvent;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Map;
import java.util.Set;

class ProjectEventsHandler extends EventsBoard {
  private static final LocalizableConstants LC = Localized.getConstants();
  private static final String STYLE_PREFIX = ProjectsKeeper.STYLE_PREFIX + "Events-";

  private final Set<Action> enabledActions = Sets.newHashSet(Action.REFRESH);

  @Override
  public String getCaption() {
    return LC.prjComments();
  }

  @Override
  protected IdentifiableWidget getAddEventActionWidget() {
    if (ProjectEvent.COMMENT.getCommandIcon() != null) {
      FaLabel label = new FaLabel(ProjectEvent.COMMENT.getCommandIcon());
      label.setTitle(ProjectEvent.COMMENT.getCommandLabel());
      return label;
    }
    return null;
  }

  @Override
  protected String getAddEventFromName() {
    return FORM_NEW_COMMENT;
  }

  @Override
  protected Set<Action> getEnabledActions() {
    return enabledActions;
  }

  @Override
  protected AbstractFormInterceptor getNewEventFormInterceptor() {
    return new NewProjectCommentForm(getRelatedId());
  }

  @Override
  protected Order getEventsDataOrder() {
    Order order = Order.ascending(COL_PUBLISH_TIME);
    return order;
  }

  @Override
  protected void afterCreateEventNoteCell(BeeRowSet rs, BeeRow row, Flow widget, Flow cell) {
    int idxProp = rs.getColumnIndex(COL_EVENT_PROPERTIES);

    if (BeeConst.isUndef(idxProp)) {
      return;
    }

    String prop = row.getString(idxProp);

    if (BeeUtils.isEmpty(prop)) {
      return;
    }

    List<String> pairedData = Lists.newArrayList(Codec.beeDeserializeCollection(prop));

    if (pairedData.size() < 2) {
      return;
    }

    Map<String, String> viewOldList = Codec.deserializeMap(pairedData.get(0));
    Map<String, String> viewNewList = Codec.deserializeMap(pairedData.get(1));

    String html = BeeConst.STRING_EMPTY;
    final List<Widget> links = Lists.newArrayList();

    for (String view : viewOldList.keySet()) {
      Map<String, String> newChanges = Codec.deserializeMap(viewNewList.get(view));
      final Map<String, String> oldChanges = Codec.deserializeMap(viewOldList.get(view));

      if (newChanges.isEmpty() && oldChanges.isEmpty()) {
        continue;
      }

      B viewCaption = Factory.b();
      if (!BeeUtils.same(view, PROP_REASON_DATA)) {
        viewCaption.appendText(Data.getViewCaption(view));
      }

      html += viewCaption.build() + Factory.br().build();

      for (final String col : oldChanges.keySet()) {
        String oldValue = oldChanges.get(col);
        String newValue = newChanges.get(col);

        String columnLabel = col;

        if (!BeeUtils.containsAnySame(view, PROP_REASON_DATA, VIEW_PROJECT_INVOICES)) {
          columnLabel = Data.getColumnLabel(view, col);
        } else {
          switch (col) {
            case PROP_REASON:
              columnLabel = LC.reason();
              break;

            case PROP_DOCUMENT:

              if (BeeUtils.isEmpty(oldChanges.get(col))) {
                continue;
              }

              columnLabel = LC.document();
              InternalLink link =
                  new InternalLink(BeeUtils.joinWords(columnLabel, oldChanges
                      .get(PROP_DOCUMENT_LINK)));
              link.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent arg0) {
                  RowEditor.open(DocumentConstants.VIEW_DOCUMENTS, BeeUtils.toLongOrNull(oldChanges
                      .get(col)),
                      Opener.NEW_TAB);
                }
              });

              links.add(link);
              continue;
            case PROP_DOCUMENT_LINK:
              continue;
            case TradeConstants.COL_SALE:

              if (BeeUtils.isEmpty(oldChanges.get(col))) {
                continue;
              }

              columnLabel = LC.trdInvoice();
              link =
                  new InternalLink(BeeUtils.joinWords(columnLabel, oldChanges
                      .get(col)));
              link.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent arg0) {
                  RowEditor.open(VIEW_PROJECT_INVOICES, BeeUtils.toLongOrNull(oldChanges
                      .get(col)),
                      Opener.NEW_TAB);
                }
              });

              links.add(link);
              continue;
            default:
              continue;
          }
        }

        if (BeeUtils.isEmpty(oldValue) && BeeUtils.isEmpty(newValue)) {
          continue;
        }

        String direction = "->";

        if (BeeUtils.isEmpty(oldValue)) {
          oldValue = LC.filterNullLabel();
        } else if (!BeeUtils.same(view, PROP_REASON_DATA)) {
          oldValue = ProjectsHelper.getDisplayValue(view, col, oldValue, null);
        }

        if (BeeUtils.isEmpty(newValue)) {
          newValue = LC.filterNullLabel();
        } else if (!BeeUtils.same(view, PROP_REASON_DATA)) {
          newValue = ProjectsHelper.getDisplayValue(view, col, newValue, null);
        }

        if (BeeUtils.same(oldValue, newValue)) {
          newValue = BeeConst.STRING_EMPTY;
          direction = BeeConst.STRING_EMPTY;
        }

        html +=
            BeeUtils.joinWords(columnLabel, BeeConst.STRING_COLON, oldValue,
                direction, newValue, Factory.br().build());
      }
    }

    if (BeeUtils.isEmpty(html) && links.isEmpty()) {
      return;
    }

    Flow rowCell = createEventRowCell(cell, COL_EVENT_PROPERTIES, null);
    rowCell.add(createCellHtmlItem(COL_EVENT_PROPERTIES, html));

    for (Widget w : links) {
      rowCell.add(w);
    }
  }

  @Override
  protected String getEventsDataViewName() {
    return VIEW_PROJECT_EVENTS;
  }

  @Override
  protected String getRelatedColumnName() {
    return COL_PROJECT;
  }

  @Override
  protected String getStylePrefix() {
    return STYLE_PREFIX;
  }

  @Override
  protected String getEventNoteColumnName() {
    return COL_COMMENT;
  }

  @Override
  protected String getEventTypeColumnName() {
    return COL_EVENT;
  }

  @Override
  protected String getPublisherPhotoColumnName() {
    return ClassifierConstants.COL_PHOTO;
  }

  @Override
  protected String getPublisherFirstNameColumnName() {
    return ALS_PUBLISHER_FIRST_NAME;
  }

  @Override
  protected String getPublisherLastNameColumnName() {
    return ALS_PUBLISHER_LAST_NAME;
  }

  @Override
  protected String getPublishTimeColumnName() {
    return COL_PUBLISH_TIME;
  }

}
