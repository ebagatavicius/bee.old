package com.butent.bee.client.modules.discussions;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.*;


/**
 * Useful shared methods for Discussions module UI logic.
 */
final class  DiscussionHelper {
    /**
     * Renders formatted date time by locale.
     * @param time time value.
     * @return formatted date time by locale.
     */
    static String renderDateTime(DateTime time) {
        return Format.renderDateTime(time);
    }

    static boolean isDiscussionAdmin(String prmAdmin) {
        if (BeeUtils.isEmpty(prmAdmin)) {
            return false;
        }

        return BeeUtils.equalsTrim(BeeKeeper.getUser().getLogin(), prmAdmin);
    }

    /**
     * Checks is the row record an announcement or discussion of Discussion data source.
     * The main difference of announcement has not empty Topic field relation with AdsTopics data
     * source.
     * @param form View of Discussion/Announcement form
     * @param row row related with Discussions data source
     * @return true the row is announcement data record.
     */
    static boolean isAnnouncement(FormView form, IsRow row) {
        return !BeeUtils.isEmpty(row.getString(form.getDataIndex(COL_TOPIC)));
    }

    static DiscussionsConstants.DiscussionStatus getStatus(IsRow row) {
        if (row == null) {
            return null;
        }

        Integer status = Data.getInteger(VIEW_DISCUSSIONS, row, COL_STATUS);

        return EnumUtils.getEnumByIndex(DiscussionsConstants.DiscussionStatus.class, status);
    }

    static Widget renderAction(DiscussionsConstants.DiscussionEvent event, Flow container,
                             ClickHandler action, String ... styles) {
        if (event == null) {
            return null;
        }

        String label = event.getCommandLabel();
        FontAwesome icon = event.getCommandIcon();

        Widget widgetEvent;

        if (icon != null) {
            widgetEvent = new FaLabel(icon);
            widgetEvent.setTitle(label);
        } else {
            widgetEvent = new Button(label);
            widgetEvent.setTitle(label);
        }

        if (widgetEvent instanceof HasClickHandlers) {
            ((HasClickHandlers) widgetEvent).addClickHandler(action);
        }

        if (container != null) {
            container.add(widgetEvent);
        }

        if (ArrayUtils.isEmpty(styles)) {
            return widgetEvent;
        }

        for (String style : styles) {
            widgetEvent.addStyleName(style);
        }

        return widgetEvent;
    }

    static void setFormCaption(FormView form, IsRow row) {
        if (isAnnouncement(form, row)) {
            form.getViewPresenter().getHeader().setCaption(Localized.dictionary().announcement());
        } else {
            form.getViewPresenter().getHeader().setCaption(Localized.dictionary().discussion());
        }
    }

    static boolean isOwner(IsRow row) {
        return Objects.equals(BeeKeeper.getUser().getUserId(),
                Data.getLong(VIEW_DISCUSSIONS, row, COL_OWNER));
    }

    static boolean validateDates(Long from, Long to,
                                        final Callback callback) {
        long now = System.currentTimeMillis();

        if (from == null && to == null) {
            callback.onFailure(BeeUtils.joinWords(Localized.dictionary().displayInBoard(),
                    Localized.dictionary().enterDate()));
            return false;
        }

        if (from == null && to != null) {
            if (to >= now) {
                return true;
            }
        }

        if (from != null && to == null) {
            if (from <= now) {
                return true;
            }
        }

        if (from != null && to != null) {
            if (from <= to) {
                return true;
            } else {
                callback.onFailure(
                        BeeUtils.joinWords(Localized.dictionary().displayInBoard(),
                                Localized.dictionary().crmFinishDateMustBeGreaterThanStart()));
                return false;
            }
        }

        return true;
    }

    private DiscussionHelper() {

    }

}
