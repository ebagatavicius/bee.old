package com.butent.bee.shared.modules.discussions;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.EnumUtils;

public final class DiscussionsConstants {

  public enum DiscussionEvent implements HasCaption {
    CREATE(Localized.dictionary().discussEventCreated(), null, null),
    CREATE_MAIL(Localized.dictionary().mailNotify(), null, null),
    VISIT(Localized.dictionary().discussEventVisited(), null, null),
    ACTIVATE(Localized.dictionary().discussEventActivated(), Localized.dictionary()
        .discussActionActivate(), FontAwesome.ARROW_CIRCLE_RIGHT),
    DEACTIVATE(Localized.dictionary().discussEventDeactivated(), Localized.dictionary()
        .discussActionDeactivate(), null),
    CLOSE(Localized.dictionary().discussEventClosed(), Localized.dictionary()
        .discussActionClose(), FontAwesome.CHECK_CIRCLE_O),
    COMMENT(Localized.dictionary().discussEventCommented(), Localized.dictionary()
        .discussActionComment(), FontAwesome.COMMENT_O),
    COMMENT_DELETE(Localized.dictionary().discussEventCommentDeleted(), Localized.dictionary()
        .actionDelete(), FontAwesome.TRASH),
    REPLY(Localized.dictionary().discussEventReplied(), Localized.dictionary()
        .discussActionReply(), FontAwesome.REPLY),
    MARK(Localized.dictionary().discussEventMarked(), Localized.dictionary()
        .discussActionMark(), FontAwesome.TAG),
    MODIFY(Localized.dictionary().discussEventModified(), Localized.dictionary().actionEdit(),
        FontAwesome.PENCIL),
    REFRESH(Localized.dictionary().actionRefresh(), Localized.dictionary().actionRefresh(),
        FontAwesome.REFRESH);

    private final String caption;
    private final String commandLabel;
    private final FontAwesome commandIcon;

    DiscussionEvent(String caption, String commandLabel, FontAwesome commandIcon) {
      this.caption = caption;
      this.commandLabel = commandLabel;
      this.commandIcon = commandIcon;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    public FontAwesome getCommandIcon() {
      return commandIcon;
    }

    public static boolean in(int event, DiscussionEvent... events) {
      for (DiscussionEvent ts : events) {
        if (ts.ordinal() == event) {
          return true;
        }
      }
      return false;
    }

    public String getCommandLabel() {
      return commandLabel;
    }
  }

  public enum DiscussionStatus implements HasCaption {
    ACTIVE(Localized.dictionary().discussStatusActive()),
    CLOSED(Localized.dictionary().discussStatusClosed()),
    INACTIVE(Localized.dictionary().discussStatusInactive());

    private final String caption;

    public static boolean in(int status, DiscussionStatus... statuses) {
      for (DiscussionStatus ts : statuses) {
        if (ts.ordinal() == status) {
          return true;
        }
      }
      return false;
    }

    public static boolean in(DiscussionStatus status, DiscussionStatus... statuses) {
      for (DiscussionStatus ts : statuses) {
        if (ts == status) {
          return true;
        }
      }
      return false;
    }

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }

    DiscussionStatus(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public static void register() {
    EnumUtils.register(DiscussionStatus.class);
  }

  public static final String DISCUSSIONS_PREFIX = "discuss_";
  public static final String DISCUSSIONS_STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "discuss-";

  public static final String ALS_BIRTHDAY = "Birthday";
  public static final String ALS_OWNER_FIRST_NAME = "OwnerFirstName";
  public static final String ALS_OWNER_LAST_NAME = "OwnerLastName";
  public static final String ALS_OWNER_PHOTO = "OwnerPhoto";
  public static final String ALS_LAST_COMMET = "LastComment";
  public static final String ALS_LAST_COMMENT_PUBLISH_TIME = "LastCommentPublishTime";
  public static final String ALS_TOPIC_NAME = "TopicName";
  public static final String ALS_NEW_ANNOUCEMENT = "NewAnnouncement";

  public static final String COL_ACCESSIBILITY = "Accessibility";
  public static final String COL_CAPTION = "Caption";
  public static final String COL_COMMENT = "Comment";
  public static final String COL_COMMENT_TEXT = "CommentText";
  public static final String COL_CREATED = "Created";
  public static final String COL_DISCUSSION = "Discussion";
  public static final String COL_DISCUSSION_COMMENT_ID = "DiscussionCommentID";
  public static final String COL_DISCUSSION_ID = "DiscussionID";
  public static final String COL_DESCRIPTION = "Description";
  public static final String COL_SUMMARY = "Summary";
  public static final String COL_DELETED = "Deleted";
  public static final String COL_IMAGE_RESOURCE_NAME = "ImageResourceName";
  public static final String COL_IMPORTANT = "Important";
  public static final String COL_MARK_NAME = "Name";
  public static final String COL_MARK_RESOURCE = "ImageResourceName";
  public static final String COL_LAST_ACCESS = "LastAccess";

  public static final String COL_MAIL_NEW_ANNOUNCEMENTS = "MailNewAnnouncements";
  public static final String COL_MAIL_NEW_DISCUSSIONS = "MailNewDiscussions";

  public static final String COL_MARK = "Mark";
  public static final String COL_MEMBER = "Member";
  public static final String COL_NAME = "Name";
  public static final String COL_ORDINAL = "Ordinal";
  public static final String COL_BACKGROUND_COLOR = "BackgroundColor";
  public static final String COL_TEXT_COLOR = "Color";
  public static final String COL_OWNER = "Owner";
  public static final String COL_PERMIT_COMMENT = "PermitComment";
  public static final String COL_PUBLISHER = "Publisher";
  public static final String COL_PUBLISHER_FIRST_NAME = "PublisherFirstName";
  public static final String COL_PUBLISHER_LAST_NAME = "PublisherLastName";
  public static final String COL_PUBLISH_TIME = "PublishTime";
  public static final String ALS_MAX_PUBLISH_TIME = "MaxPublishTime";
  public static final String COL_PARENT_COMMENT = "Parent";
  public static final String COL_REASON = "Reason";
  public static final String COL_STAR = "Star";
  public static final String COL_STATUS = "Status";
  public static final String COL_USER = "User";
  public static final String COL_SUBJECT = "Subject";
  public static final String COL_TOPIC = "Topic";
  public static final String COL_VISIBLE = "Visible";
  public static final String COL_VISIBLE_FROM = "VisibleFrom";
  public static final String COL_VISIBLE_TO = "VisibleTo";

  public static final String FORM_NEW_DISCUSSION = "NewDiscussion";
  public static final String FORM_DISCUSSION = "Discussion";
  public static final String FORM_ANNOUNCEMENTS_BOARD = "AnnouncementsBoard";
  public static final String FORM_NEW_ANNOUNCEMENT = "NewAnnouncement";

  public static final String GRID_DISCUSSIONS = "Discussions";
  public static final String GRID_DISCUSSION_FILES = "DiscussionFiles";

  public static final String PROP_COMMENTS = "Comments";
  public static final String PROP_FILES = "Files";
  public static final String PROP_LAST_ACCESS = "LastAccess";
  public static final String PROP_LAST_COMMENT = "LastComment";
  public static final String PROP_LAST_COMMENT_DATA = "LastCommentData";
  public static final String PROP_COMMENT_COUNT = "CommentCount";
  public static final String PROP_MEMBERS = "Members";
  public static final String PROP_MEMBER_GROUP = "MemberGroup";
  public static final String PROP_STAR = "Star";
  public static final String PROP_USER = "User";
  public static final String PROP_MARKS = "Marks";
  public static final String PROP_MAIL = "Mail";

  public static final String PROP_ANNOUNCMENT = "Announcement";
  public static final String PROP_MARK_COUNT = "MarkCount";
  public static final String PROP_MARK_TYPES = "MarkTypes";
  public static final String PROP_MARK_DATA = "MarkData";
  public static final String PROP_RELATIONS_COUNT = "RelationsCount";
  public static final String PROP_FILES_COUNT = "FilesCount";
  public static final String PROP_PREVIEW_IMAGE = "PreviewImage";

  public static final String PROP_PARAMETERS = "Parameters";

  public static final String PRM_DISCUSS_ADMIN = "DiscussionsAdmin";
  public static final String PRM_FORBIDDEN_FILES_EXTENTIONS = "ForbiddenFilesExtentions";
  public static final String PRM_MAX_UPLOAD_FILE_SIZE = "MaxUploadFileSize";
  public static final String PRM_ALLOW_DELETE_OWN_COMMENTS = "AllowDeleteOwnComments";
  public static final String PRM_DISCUSS_INACTIVE_TIME_IN_DAYS = "DiscussInactiveTimeInDays";
  public static final String PRM_DISCUSS_BIRTHDAYS = "DiscussBirthdays";

  public static final String SVC_GET_DISCUSSION_DATA = "get_discuss_data";
  public static final String SVC_GET_ANNOUNCEMENTS_DATA = "get_ads_data";
  public static final String SVC_GET_BIRTHDAYS = "get_birthdays";

  public static final String TBL_ADS_TOPICS = "AdsTopics";
  public static final String TBL_DISCUSSIONS = "Discussions";
  public static final String TBL_DISCUSSIONS_COMMENTS = "DiscussionsComments";
  public static final String TBL_DISCUSSIONS_FILES = "DiscussionsFiles";
  public static final String TBL_DISCUSSIONS_USERS = "DiscussionsUsers";
  public static final String TBL_DISCUSSIONS_COMMENTS_MARKS = "DiscussCommentsMarks";
  public static final String TBL_COMMENTS_MARK_TYPES = "CommentsMarksTypes";
  public static final String TBL_DISCUSSIONS_USAGE = "DiscussionsUsage";

  public static final long VALUE_MEMBER = 1;

  public static final String VAR_DISCUSSION_COMMENT = Service.RPC_VAR_PREFIX + "discuss_comment";
  public static final String VAR_DISCUSSION_PARENT_COMMENT = Service.RPC_VAR_PREFIX
      + "discuss_parent_comment";
  public static final String VAR_DISCUSSION_DELETED_COMMENT = Service.RPC_VAR_PREFIX
      + "discuss_deleted_comment";
  public static final String VAR_DISCUSSION_DATA = Service.RPC_VAR_PREFIX + "discuss_data";
  public static final String VAR_DISCUSSION_ID = Service.RPC_VAR_PREFIX + "discuss_id";
  public static final String VAR_DISCUSSION_USERS = Service.RPC_VAR_PREFIX + "discuss_users";
  public static final String VAR_DISCUSSION_MARK = Service.RPC_VAR_PREFIX + "discuss_mark";
  public static final String VAR_DISCUSSION_MARKED_COMMENT = Service.RPC_VAR_PREFIX
      + "discuss_marked_comment";

  public static final String VIEW_DISCUSSIONS = "Discussions";
  public static final String VIEW_DISCUSSIONS_COMMENTS = "DiscussionsComments";
  public static final String VIEW_DISCUSSIONS_FILES = "DiscussionsFiles";
  public static final String VIEW_DISCUSSIONS_USERS = "DiscussionsUsers";
  public static final String VIEW_DISCUSSIONS_MARK_TYPES = "DiscussMarkTypes";
  public static final String VIEW_ADS_TOPICS = "AdsTopics";

  public static final int DEFAULT_BIRTHDAYS_DAYS_RANGE = 6;

  private DiscussionsConstants() {

  }
}
