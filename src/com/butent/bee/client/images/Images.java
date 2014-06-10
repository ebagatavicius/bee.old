package com.butent.bee.client.images;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Handles a list of images used in the system.
 */

public final class Images {

  public interface Resources extends ClientBundle {

    @Source("accept.png")
    ImageResource accept();

    @Source("add.png")
    ImageResource add();

    @Source("alarm.png")
    ImageResource alarm();

    @Source("arrow_down.png")
    ImageResource arrowDown();

    @Source("arrow_down_double.png")
    ImageResource arrowDownDouble();

    @Source("arrow_in.png")
    ImageResource arrowIn();

    @Source("arrow_left.png")
    ImageResource arrowLeft();

    @Source("arrow_left_double.png")
    ImageResource arrowLeftDouble();

    @Source("arrow_out.png")
    ImageResource arrowOut();

    @Source("arrow_right.png")
    ImageResource arrowRight();

    @Source("arrow_right_double.png")
    ImageResource arrowRightDouble();

    @Source("arrow_up.png")
    ImageResource arrowUp();

    @Source("arrow_up_double.png")
    ImageResource arrowUpDouble();

    @Source("ascending.gif")
    ImageResource ascending();

    @Source("attachment.png")
    ImageResource attachment();

    @Source("bookmark.png")
    ImageResource bookmark();

    @Source("bookmark_add.png")
    ImageResource bookmarkAdd();

    @Source("cake_230x265.jpg")
    ImageResource cake();

    @Source("calendar.png")
    ImageResource calendar();

    @Source("cancel.png")
    ImageResource cancel();

    @Source("close.png")
    ImageResource close();

    @Source("closeSmall.png")
    ImageResource closeSmall();

    @Source("closeSmallRed.png")
    ImageResource closeSmallRed();

    @Source("comments.png")
    ImageResource comments();

    @Source("configure.png")
    ImageResource configure();

    @Source("delete.png")
    ImageResource delete();

    @Source("descending.gif")
    ImageResource descending();

    @Source("disclosureClosed.png")
    ImageResource disclosureClosed();

    @Source("disclosureOpen.png")
    ImageResource disclosureOpen();

    @Source("edit.png")
    ImageResource edit();

    @Source("edit_add.png")
    ImageResource editAdd();

    @Source("edit_delete.png")
    ImageResource editDelete();

    @Source("error.png")
    ImageResource error();

    @Source("silver/excel_17x18.png")
    ImageResource excel();

    @Source("feed.png")
    ImageResource feed();

    @Source("filter_delete.png")
    ImageResource filterDelete();

    @Source("first.png")
    ImageResource first();

    @Source("forward.png")
    ImageResource forward();

    @Source("forward_to.png")
    ImageResource forwardTo();

    @Source("green.gif")
    ImageResource green();

    @Source("greenSmall.gif")
    ImageResource greenSmall();

    @Source("html.png")
    ImageResource html();

    @Source("information.png")
    ImageResource information();

    @Source("last.png")
    ImageResource last();

    @Source("link.gif")
    ImageResource link();

    @Source("loading.gif")
    ImageResource loading();

    @Source("next.png")
    ImageResource next();

    @Source("noes.png")
    ImageResource noes();

    @Source("ok.png")
    ImageResource ok();

    @Source("plane.png")
    ImageResource plane();

    @Source("play.png")
    ImageResource play();

    @Source("previous.png")
    ImageResource previous();

    @Source("question.png")
    ImageResource question();

    @Source("red.gif")
    ImageResource red();

    @Source("redo.png")
    ImageResource redo();

    @Source("redSmall.gif")
    ImageResource redSmall();

    @Source("refresh.png")
    ImageResource refresh();

    @Source("reload.png")
    ImageResource reload();

    @Source("reply_to.png")
    ImageResource replyTo();

    @Source("reply_to_all.png")
    ImageResource replyToAll();

    @Source("report.png")
    ImageResource report();

    @Source("rewind.png")
    ImageResource rewind();

    @Source("save.png")
    ImageResource save();

    @Source("search.png")
    ImageResource search();

    @Source("searchOptions.gif")
    ImageResource searchOptions();

    @Source("silver/plus_gray_17x18.png")
    ImageResource silverAdd();

    @Source("silver/bar_chart_24x24.png")
    ImageResource silverBarChart();

    @Source("silver/bookmark_24x24.png")
    ImageResource silverBookmark();

    @Source("silver/bookmark_add_21x19.png")
    ImageResource silverBookmarkAdd();

    @Source("silver/calendar_24x24.png")
    ImageResource silverCalendar();

    @Source("silver/chat_icon_16x21.gif")
    ImageResource silverChatIcon();

    @Source("silver/close_17x18.png")
    ImageResource silverClose();

    @Source("silver/comments_24x24.png")
    ImageResource silverComments();

    @Source("silver/configure_17x18.png")
    ImageResource silverConfigure();

    @Source("silver/configure_24x24.png")
    ImageResource silverConfigure24();

    @Source("silver/delete_17x18.png")
    ImageResource silverDelete();

    @Source("silver/discuss_activate_17x18.png")
    ImageResource silverDiscussActivate();

    @Source("silver/discuss_close_17x18.png")
    ImageResource silverDiscussClose();

    @Source("silver/comment_17x18.png")
    ImageResource silverDiscussComment();

    @Source("silver/edit_17x18.png")
    ImageResource silverEdit();

    @Source("silver/feed_24x24.png")
    ImageResource silverFeed();

    @Source("silver/filter_17x14.png")
    ImageResource silverFilter();

    @Source("silver/filter_remove_25x14.png")
    ImageResource silverFilterRemove();

    @Source("silver/invoice_17x18.png")
    ImageResource silverInvoice();

    @Source("silver/mail_17x18.png")
    ImageResource silverMail();

    @Source("silver/mail_24x24.png")
    ImageResource silverMail24();

    @Source("silver/minus_button_17x18.png")
    ImageResource silverMinus();

    @Source("silver/plus_button_17x18.png")
    ImageResource silverPlus();

    @Source("silver/print_17x18.png")
    ImageResource silverPrint();

    @Source("silver/profit_17x18.png")
    ImageResource silverProfit();

    @Source("silver/reload_17x18.png")
    ImageResource silverReload();

    @Source("silver/sad_24x24.png")
    ImageResource silverSad();

    @Source("silver/save_17x18.png")
    ImageResource silverSave();

    @Source("silver/save_24x24.png")
    ImageResource silverSave24();

    @Source("silver/smile_24x24.png")
    ImageResource silverSmile();

    @Source("silver/tringle_down_11x9.png")
    ImageResource silverTringleDown();

    @Source("silver/tringle_up_11x9.png")
    ImageResource silverTringleUp();

    @Source("silver/truck_17x18.png")
    ImageResource silverTruck();

    @Source("silver/user_24x24.png")
    ImageResource silverUser();

    @Source("slider.gif")
    ImageResource slider();

    @Source("sliderDisabled.gif")
    ImageResource sliderDisabled();

    @Source("sliderSliding.gif")
    ImageResource sliderSliding();

    @Source("undo.png")
    ImageResource undo();

    @Source("user.png")
    ImageResource user();

    @Source("warning.png")
    ImageResource warning();

    @Source("workspace.png")
    ImageResource workspace();

    @Source("yellow.gif")
    ImageResource yellow();

    @Source("yellowSmall.gif")
    ImageResource yellowSmall();
  }

  public static final long MAX_SIZE_FOR_DATA_URL = 1258292L; /* ~1.2 MB */

  private static final Map<String, ImageResource> map = Maps.newHashMap();

  private static final ImageElement imageElement = Document.get().createImageElement();

  public static String asString(ImageResource imageResource) {
    if (imageResource == null) {
      return null;
    }

    imageElement.setSrc(imageResource.getSafeUri().asString());
    imageElement.setAlt(imageResource.getName());

    return imageElement.getString();
  }

  public static Resources createResources() {
    return GWT.create(Resources.class);
  }

  public static ImageResource get(String name) {
    Assert.notEmpty(name);
    return map.get(key(name));
  }

  public static String getHtml(String name) {
    return asString(get(name));
  }

  public static Map<String, ImageResource> getMap() {
    return map;
  }

  public static void init(Resources resources) {
    Assert.notNull(resources);
    if (!map.isEmpty()) {
      map.clear();
    }

    map.put(key("accept"), resources.accept());

    map.put(key("add"), resources.add());

    map.put(key("alarm"), resources.alarm());

    map.put(key("arrowDown"), resources.arrowDown());
    map.put(key("arrowDownDouble"), resources.arrowDownDouble());
    map.put(key("arrowIn"), resources.arrowIn());
    map.put(key("arrowLeft"), resources.arrowLeft());
    map.put(key("arrowLeftDouble"), resources.arrowLeftDouble());
    map.put(key("arrowOut"), resources.arrowOut());
    map.put(key("arrowRight"), resources.arrowRight());
    map.put(key("arrowRightDouble"), resources.arrowRightDouble());
    map.put(key("arrowUp"), resources.arrowUp());
    map.put(key("arrowUpDouble"), resources.arrowUpDouble());

    map.put(key("ascending"), resources.ascending());

    map.put(key("attachment"), resources.attachment());

    map.put(key("bookmark"), resources.bookmark());
    map.put(key("bookmarkAdd"), resources.bookmarkAdd());

    map.put(key("calendar"), resources.calendar());

    map.put(key("cancel"), resources.cancel());
    map.put(key("close"), resources.close());
    map.put(key("closeSmall"), resources.closeSmall());
    map.put(key("closeSmallRed"), resources.closeSmallRed());

    map.put(key("comments"), resources.comments());

    map.put(key("configure"), resources.configure());

    map.put(key("delete"), resources.delete());

    map.put(key("descending"), resources.descending());

    map.put(key("disclosureClosed"), resources.disclosureClosed());
    map.put(key("disclosureOpen"), resources.disclosureOpen());

    map.put(key("edit"), resources.edit());

    map.put(key("editAdd"), resources.editAdd());
    map.put(key("editDelete"), resources.editDelete());

    map.put(key("error"), resources.error());

    map.put(key("excel"), resources.excel());

    map.put(key("feed"), resources.feed());

    map.put(key("filterDelete"), resources.filterDelete());

    map.put(key("first"), resources.first());

    map.put(key("forward"), resources.forward());

    map.put(key("forwardTo"), resources.forwardTo());

    map.put(key("green"), resources.green());
    map.put(key("greenSmall"), resources.greenSmall());

    map.put(key("html"), resources.html());

    map.put(key("information"), resources.information());

    map.put(key("last"), resources.last());

    map.put(key("link"), resources.link());

    map.put(key("loading"), resources.loading());

    map.put(key("next"), resources.next());

    map.put(key("noes"), resources.noes());

    map.put(key("ok"), resources.ok());

    map.put(key("plane"), resources.plane());

    map.put(key("play"), resources.play());

    map.put(key("previous"), resources.previous());

    map.put(key("question"), resources.question());

    map.put(key("red"), resources.red());
    map.put(key("redSmall"), resources.redSmall());

    map.put(key("redo"), resources.redo());

    map.put(key("refresh"), resources.refresh());
    map.put(key("reload"), resources.reload());

    map.put(key("replyTo"), resources.replyTo());
    map.put(key("replyToAll"), resources.replyToAll());

    map.put(key("report"), resources.report());

    map.put(key("rewind"), resources.rewind());

    map.put(key("save"), resources.save());

    map.put(key("search"), resources.search());

    map.put(key("slider"), resources.slider());
    map.put(key("sliderDisabled"), resources.sliderDisabled());
    map.put(key("sliderSliding"), resources.sliderSliding());

    map.put(key("undo"), resources.undo());

    map.put(key("user"), resources.user());

    map.put(key("warning"), resources.warning());

    map.put(key("workspace"), resources.workspace());

    map.put(key("yellow"), resources.yellow());
    map.put(key("yellowSmall"), resources.yellowSmall());

    map.put(key("silverAdd"), resources.silverAdd());
    map.put(key("silverBookmarkAdd"), resources.silverBookmarkAdd());
    map.put(key("silverBarChart"), resources.silverBarChart());
    map.put(key("silverChatIcon"), resources.silverChatIcon());
    map.put(key("silverClose"), resources.silverClose());
    map.put(key("silverConfigure"), resources.silverConfigure());
    map.put(key("silverDelete"), resources.silverDelete());
    map.put(key("silverDiscussActivate"), resources.silverDiscussActivate());
    map.put(key("silverDiscussClose"), resources.silverDiscussClose());
    map.put(key("silverDiscussComment"), resources.silverDiscussComment());
    map.put(key("silverEdit"), resources.silverEdit());
    map.put(key("silverFilter"), resources.silverFilter());
    map.put(key("silverFilterRemove"), resources.silverFilterRemove());
    map.put(key("silverInvoice"), resources.silverInvoice());
    map.put(key("silverMail"), resources.silverMail());
    map.put(key("silverMinus"), resources.silverMinus());
    map.put(key("silverPlus"), resources.silverPlus());
    map.put(key("silverPrint"), resources.silverPrint());
    map.put(key("silverProfit"), resources.silverProfit());
    map.put(key("silverReload"), resources.silverReload());
    map.put(key("silverSave"), resources.silverSave());
    map.put(key("silverSmile"), resources.silverSmile());
    map.put(key("silverSad"), resources.silverSad());
    map.put(key("silverTringleDown"), resources.silverTringleDown());
    map.put(key("silverTringleUp"), resources.silverTringleUp());
    map.put(key("silverTruck"), resources.silverTruck());
  }

  public static List<NewFileInfo> sanitizeInput(Collection<NewFileInfo> input,
      NotificationListener notificationListener) {

    List<NewFileInfo> result = Lists.newArrayList();
    if (BeeUtils.isEmpty(input)) {
      return result;
    }

    List<String> errors = Lists.newArrayList();

    for (NewFileInfo nfi : input) {
      long size = nfi.getSize();

      if (size > MAX_SIZE_FOR_DATA_URL) {
        errors.add(BeeUtils.join(BeeConst.STRING_COLON + BeeConst.STRING_SPACE, nfi.getName(),
            Localized.getMessages().fileSizeExceeded(size, MAX_SIZE_FOR_DATA_URL)));
      } else {
        result.add(nfi);
      }
    }

    if (!errors.isEmpty() && notificationListener != null) {
      result.clear();
      notificationListener.notifyWarning(ArrayUtils.toArray(errors));
    }

    return result;
  }

  private static String key(String name) {
    return name.trim().toLowerCase();
  }

  private Images() {
  }
}
