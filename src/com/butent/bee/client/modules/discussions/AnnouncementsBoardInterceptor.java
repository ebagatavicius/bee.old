package com.butent.bee.client.modules.discussions;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.DateTimeFormat.PredefinedFormat;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.Display;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.HandlesUpdateEvents;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

class AnnouncementsBoardInterceptor extends AbstractFormInterceptor implements
    RowInsertEvent.Handler, HandlesUpdateEvents, DataChangeEvent.Handler {
  private static final String WIDGET_ADS_CONTENT = "AdsContent";
  private static final String STYLE_PREFIX = "bee-discuss-adsFormContent-";
  private static final String STYLE_HAPPY_DAY = "-happyDay";
  private static final String STYLE_BIRTH_LIST = "-birthList";
  private static final String STYLE_ACTION = "action";
  private static final String STYLE_CHAT_BALLOON = "chatBalloon";

  private final Collection<HandlerRegistration> registry = Lists.newArrayList();

  @Override
  public FormInterceptor getInstance() {
    return new AnnouncementsBoardInterceptor();
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action.compareTo(Action.REFRESH) == 0) {
      renderContent(getFormView());
      return false;
    }

    return super.beforeAction(action, presenter);
  }

  @Override
  public void onLoad(FormView form) {
    registry.add(BeeKeeper.getBus().registerRowInsertHandler(this, false));
    registry.addAll(BeeKeeper.getBus().registerUpdateHandler(this, false));
    registry.add(BeeKeeper.getBus().registerDataChangeHandler(this, false));
    super.onLoad(form);
  }

  @Override
  public void onStart(FormView form) {
    renderContent(form);
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS)) {
      renderContent(getFormView());
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS)) {
      renderContent(getFormView());
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS)) {
      renderContent(getFormView());
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (event.hasView(VIEW_DISCUSSIONS)) {
      renderContent(getFormView());
    }
  }

  @Override
  public void onUnload(FormView form) {
    for (HandlerRegistration entry : registry) {
      if (entry != null) {
        entry.removeHandler();
      }
    }
    super.onUnload(form);
  }

  private static void renderContent(FormView form) {
    Widget w = Assert.notNull(form.getWidgetByName(WIDGET_ADS_CONTENT));

    if (!(w instanceof HtmlTable)) {
      Assert.notImplemented();
    }

    final HtmlTable adsTable = (HtmlTable) w;
    adsTable.clear();
    ParameterList params = DiscussionsKeeper.createArgs(SVC_GET_ANNOUNCEMENTS_DATA);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        Assert.notNull(response);
        adsTable.clear();
        if (response.isEmpty()) {
          renderWelcomeSection(adsTable);
          return;
        }

        if (!response.hasResponse(SimpleRowSet.class)) {
          renderWelcomeSection(adsTable);
          return;
        }

        SimpleRowSet rs = SimpleRowSet.restore(response.getResponseAsString());
        for (String[] rsRow : rs.getRows()) {

          if (rs.hasColumn(ALS_BIRTHDAY)) {
            if (!BeeUtils.isEmpty(rsRow[rs.getColumnIndex(ALS_BIRTHDAY)])) {
              renderBirthdaySection(rsRow, rs, adsTable);
            } else {
              renderAnnoucementsSection(rsRow, rs, adsTable);
            }
          } else {
            renderAnnoucementsSection(rsRow, rs, adsTable);
          }

        }

      }
    });
  }

  protected static void renderAnnoucementsSection(String[] rsRow, SimpleRowSet rs,
      HtmlTable adsTable) {
    int row = adsTable.getRowCount();
    adsTable.setHtml(row, 0, rsRow[rs.getColumnIndex(ALS_TOPIC_NAME)]);
    adsTable.getCellFormatter().setColSpan(row, 0, 2);
    adsTable.getRow(row).addClassName(STYLE_PREFIX + ALS_TOPIC_NAME);

    if (!BeeUtils.isEmpty(rsRow[rs.getColumnIndex(COL_IMPORTANT)])) {
      adsTable.getRow(row).addClassName(STYLE_PREFIX + COL_IMPORTANT);
    }
    row++;
    adsTable.setHtml(row, 0, renderDateTime(rsRow[rs.getColumnIndex(COL_CREATED)]),
        STYLE_PREFIX + COL_CREATED);

    String attachment = "";

    if (rs.hasColumn(AdministrationConstants.COL_FILE)) {
      if (!BeeUtils.isEmpty(rsRow[rs.getColumnIndex(AdministrationConstants.COL_FILE)])) {
        int fileCount = BeeUtils.toInt(rsRow[rs.getColumnIndex(AdministrationConstants.COL_FILE)]);

        if (BeeUtils.isPositive(fileCount)) {
          attachment = (new Image(Global.getImages().attachment())).toString();
        }
      }
    }

    CustomDiv div = new CustomDiv(STYLE_PREFIX + STYLE_CHAT_BALLOON);

    int subjectRow = row;
    adsTable.setHtml(row, 1, div.toString() + attachment + rsRow[rs.getColumnIndex(COL_SUBJECT)],
        STYLE_PREFIX + COL_SUBJECT);

    row++;
    adsTable.setHtml(row, 0, renderPhotoAndAuthor(rsRow, rs, STYLE_PREFIX + COL_OWNER
        + BeeConst.STRING_MINUS),
        STYLE_PREFIX
            + COL_OWNER);

    adsTable.setHtml(row, 1, rsRow[rs.getColumnIndex(COL_DESCRIPTION)], STYLE_PREFIX
        + COL_DESCRIPTION);

    if (rs.hasColumn(ALS_NEW_ANNOUCEMENT)) {
      boolean isNew = BeeUtils.toBoolean(rsRow[rs.getColumnIndex(ALS_NEW_ANNOUCEMENT)]);
      if (isNew) {
        adsTable.getCellFormatter().addStyleName(row, 1, STYLE_PREFIX + ALS_NEW_ANNOUCEMENT);
        adsTable.getCellFormatter().addStyleName(subjectRow, 1, STYLE_PREFIX + ALS_NEW_ANNOUCEMENT);
      }
    }

    row++;

    final Long rowId = BeeUtils.toLongOrNull(rsRow[rs.getColumnIndex(COL_DISCUSSION)]);

    if (DataUtils.isId(rowId)) {
      ScheduledCommand command = new ScheduledCommand() {

        @Override
        public void execute() {
          RowEditor.openRow(VIEW_DISCUSSIONS, rowId, false, null);
        }
      };
      String btnCaption = BeeUtils.joinWords(
          new FaLabel(FontAwesome.SQUARE_O).toString(),
          new FaLabel(FontAwesome.SQUARE_O).toString(),
          new FaLabel(FontAwesome.SQUARE_O).toString());

      Button moreButton = new Button(btnCaption, command);
      moreButton.setTitle(Localized.getConstants().more());
      moreButton.addStyleName(STYLE_PREFIX + STYLE_ACTION + COL_DISCUSSION);
      adsTable.setText(row, 0, BeeConst.STRING_EMPTY);
      adsTable.setWidget(row, 1, moreButton);

      adsTable.getRow(row).addClassName(STYLE_PREFIX + STYLE_ACTION);

      row++;
    }
  }

  protected static void renderBirthdaySection(String[] rsRow, SimpleRowSet rs, HtmlTable adsTable) {
    int row = adsTable.getRowCount();
    int startRow = row;
    adsTable.setHtml(row, 0, rsRow[rs.getColumnIndex(ALS_TOPIC_NAME)]);
    adsTable.getCellFormatter().setColSpan(row, 0, 2);
    adsTable.getRow(row).addClassName(STYLE_PREFIX + ALS_TOPIC_NAME);
    adsTable.getRow(row).addClassName(STYLE_PREFIX + ALS_TOPIC_NAME + ALS_BIRTHDAY);
    StyleUtils.setDisplay(adsTable.getRow(row), Display.NONE);

    row++;
    Image img = new Image(Global.getImages().cake());
    img.addStyleName(STYLE_PREFIX + COL_OWNER + ALS_BIRTHDAY);
    adsTable.setWidget(row, 0, img, STYLE_PREFIX + COL_OWNER);
    adsTable.getCellFormatter().setRowSpan(row, 0, 2);

    adsTable.setHtml(row, 1, Localized.getConstants().birthdaysParties(), STYLE_PREFIX
        + COL_SUBJECT);
    adsTable.getRow(row).addClassName(STYLE_PREFIX + ALS_BIRTHDAY);
    StyleUtils.setDisplay(adsTable.getRow(row), Display.NONE);

    row++;
    int contentRow = row;
    adsTable.setHtml(row, 0, BeeConst.STRING_EMPTY, STYLE_PREFIX
        + COL_DESCRIPTION);

    adsTable.getRow(row).addClassName(STYLE_PREFIX + ALS_BIRTHDAY);
    StyleUtils.setDisplay(adsTable.getRow(row), Display.NONE);

    row++;
    int finishRow = row;

    renderBirthdaysList(startRow, contentRow, finishRow, adsTable);
  }

  protected static void renderWelcomeSection(HtmlTable adsTable) {

    int row = adsTable.getRowCount();
    adsTable.setHtml(row, 0, Localized.getConstants().welcome());
    adsTable.getCellFormatter().setColSpan(row, 0, 2);
    adsTable.getRow(row).addClassName(STYLE_PREFIX + ALS_TOPIC_NAME);


    row++;
    adsTable.setHtml(row, 0, BeeConst.STRING_EMPTY,
        STYLE_PREFIX + COL_CREATED);

    adsTable.setHtml(row, 1, BeeConst.STRING_EMPTY,
        STYLE_PREFIX + COL_SUBJECT);

    row++;
    Image img = new Image("images/logo.png");
    StyleUtils.setMaxHeight(img, 90);
    adsTable.setWidget(row, 0, img, STYLE_PREFIX + COL_OWNER);

    adsTable.setHtml(row, 1, Localized.getConstants().welcomeMessage(), STYLE_PREFIX
        + COL_DESCRIPTION);

    row++;

    ScheduledCommand command = new ScheduledCommand() {

        @Override
        public void execute() {
        // RowEditor.openRow(VIEW_DISCUSSIONS, rowId, false, null);
        FormFactory.openForm(FORM_NEW_DISCUSSION, new CreateDiscussionInterceptor());
        }
      };

    String btnCaption = BeeUtils.joinWords(
        new FaLabel(FontAwesome.SQUARE_O).toString(),
        new FaLabel(FontAwesome.SQUARE_O).toString(),
        new FaLabel(FontAwesome.SQUARE_O).toString());

    Button moreButton = new Button(btnCaption, command);
    moreButton.setTitle(Localized.getConstants().more());
    moreButton.addStyleName(STYLE_PREFIX + STYLE_ACTION + COL_DISCUSSION);
    adsTable.setText(row, 0, BeeConst.STRING_EMPTY);
    // adsTable.setWidget(row, 1, moreButton);
    adsTable.setHtml(row, 1, BeeConst.STRING_EMPTY);

    adsTable.getRow(row).addClassName(STYLE_PREFIX + STYLE_ACTION);

    row++;

  }

  private static void renderBirthdaysList(final int startRow, final int contentRow,
      final int finishRow, final HtmlTable adsTable) {
    ParameterList params = DiscussionsKeeper.createArgs(SVC_GET_BIRTHDAYS);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        if (response.isEmpty()) {
          adsTable.setHtml(contentRow, 0, Localized.getConstants().noData());
          return;
        }

        if (!response.hasResponse(SimpleRowSet.class)) {
          adsTable.setHtml(contentRow, 0, Localized.getConstants().actionCanNotBeExecuted());
          return;
        }

        SimpleRowSet rs = SimpleRowSet.restore(response.getResponseAsString());

        HtmlTable listTbl = new HtmlTable();
        int row = listTbl.getRowCount();

        for (String[] birthListData : rs.getRows()) {
          listTbl.setHtml(row, 0, birthListData[rs.getColumnIndex(COL_NAME)]);

          listTbl.setHtml(row, 1, DateTimeFormat.getFormat(PredefinedFormat.MONTH_DAY)
              .format(new JustDate(BeeUtils.toLong(birthListData[rs
                  .getColumnIndex(COL_DATE_OF_BIRTH)]))));
          listTbl.getRow(row).addClassName(STYLE_PREFIX + COL_DESCRIPTION
              + ALS_BIRTHDAY + STYLE_BIRTH_LIST);

          JustDate now = new JustDate();

          if (now.getDoy() == (new JustDate(BeeUtils.toLong(birthListData[rs
              .getColumnIndex(COL_DATE_OF_BIRTH)]))).getDoy()) {
            listTbl.getRow(row).addClassName(STYLE_PREFIX + COL_DESCRIPTION
                + ALS_BIRTHDAY + STYLE_HAPPY_DAY);
          }
          row++;
        }

        adsTable.setHtml(contentRow, 0, listTbl.toString());

        for (int i = startRow; i < finishRow; i++) {
          StyleUtils.setDisplay(adsTable.getRow(i), Display.TABLE_ROW);
        }
      }
    });
  }

  private static String renderDateTime(String timestamp) {
    String result;
    DateTime dt = new DateTime(BeeUtils.toLong(timestamp));
    result = dt.toCompactString();
    return result;
  }

  private static String renderPhotoAndAuthor(String[] rsRow, SimpleRowSet rs, String stylePref) {
    String fullName = BeeUtils.joinWords(rsRow[rs.getColumnIndex(COL_FIRST_NAME)],
        rsRow[rs.getColumnIndex(COL_LAST_NAME)]);
    Flow container = new Flow();

    Flow colPublisher = new Flow();
    colPublisher.addStyleName(stylePref + COL_PUBLISHER);

    if (!BeeUtils.isEmpty(fullName)) {
      colPublisher.add(createCell(stylePref + COL_PUBLISHER, fullName));
    }

    container.add(colPublisher);

    Flow colPhoto = new Flow();
    colPhoto.addStyleName(stylePref + COL_PHOTO);

    if (!BeeUtils.isEmpty(rsRow[rs.getColumnIndex(COL_PHOTO)])) {
      renderPhoto(rsRow, rs, stylePref, colPhoto);
    }
    container.add(colPhoto);

    return container.toString();
  }

  private static void renderPhoto(String[] rsRow, SimpleRowSet rs, String stylePref,
      Flow container) {
    String photo =
        rsRow[rs.getColumnIndex(COL_PHOTO)];
    if (!BeeUtils.isEmpty(photo)) {
      Image image = new Image(PhotoRenderer.getUrl(photo));
      image.addStyleName(stylePref + COL_PHOTO);
      container.add(image);
    }
  }

  private static Widget createCell(String style, String value) {
    Widget widget = new CustomDiv(style);
    if (!BeeUtils.isEmpty(value)) {
      widget.getElement().setInnerHTML(value);
    }

    return widget;
  }

}
