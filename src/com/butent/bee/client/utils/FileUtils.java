package com.butent.bee.client.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.RpcUtils;
import com.butent.bee.client.composite.Thermometer;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.widget.Link;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import elemental.client.Browser;
import elemental.events.ProgressEvent;
import elemental.html.File;
import elemental.html.FileList;
import elemental.html.FileReader;
import elemental.js.dom.JsClipboard;
import elemental.xml.XMLHttpRequest;

public final class FileUtils {

  private static final BeeLogger logger = LogUtils.getLogger(FileUtils.class);

  private static final long MIN_FILE_SIZE_FOR_PROGRESS = 100000;

  public static void commitFile(FileInfo fileInfo, Callback<FileInfo> callback) {
    upload(fileInfo, true, callback);
  }

  public static void commitFiles(Collection<? extends FileInfo> files, final String viewName,
      final String parentColumn, final Long parentId, final String fileColumn,
      final String captionColumn) {

    Assert.notEmpty(files);
    Assert.notEmpty(viewName);
    Assert.notEmpty(parentColumn);
    Assert.isTrue(DataUtils.isId(parentId));
    Assert.notEmpty(fileColumn);

    final List<BeeColumn> columns = Data.getColumns(viewName);

    final Holder<Integer> latch = Holder.of(files.size());

    for (final FileInfo fileInfo : files) {
      uploadFile(fileInfo, result -> {
        BeeRow row = DataUtils.createEmptyRow(columns.size());

        Data.setValue(viewName, row, parentColumn, parentId);
        Data.setValue(viewName, row, fileColumn, result.getId());

        if (!BeeUtils.isEmpty(captionColumn)) {
          String caption = BeeUtils.notEmpty(fileInfo.getCaption(), fileInfo.getName());

          Integer precision = Data.getColumnPrecision(viewName, captionColumn);
          if (BeeUtils.isPositive(precision) && BeeUtils.hasLength(caption, precision)) {
            caption = BeeUtils.left(caption, precision);
          }

          Data.setValue(viewName, row, captionColumn, caption);
        }

        Queries.insert(viewName, columns, row, new RowCallback() {
          @Override
          public void onSuccess(BeeRow br) {
            latch.set(latch.get() - 1);
            if (!BeeUtils.isPositive(latch.get())) {
              DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewName);
            }
          }
        });
      });
    }
  }

  public static FileList getFiles(NativeEvent event) {
    Assert.notNull(event);

    DataTransfer dataTransfer = event.getDataTransfer();
    if (dataTransfer == null) {
      return null;
    }

    JsClipboard clipboard = dataTransfer.cast();
    return clipboard.getFiles();
  }

  public static Widget getLink(FileInfo fileInfo, String... caption) {
    Assert.notNull(fileInfo);

    Simple simple = new Simple();
    String name = BeeUtils.notEmpty(fileInfo.getCaption(), fileInfo.getName());

    simple.setWidget(new Link(BeeUtils.notEmpty(ArrayUtils.joinWords(caption), name),
        getUrl(fileInfo.getHash(), name)));

    DndHelper.makeSource(simple, NameUtils.getClassName(FileInfo.class), fileInfo, null);

    return simple;
  }

  public static List<NewFileInfo> getNewFileInfos(FileList fileList) {
    List<NewFileInfo> result = new ArrayList<>();
    if (fileList == null) {
      return result;
    }

    for (int i = 0; i < fileList.length(); i++) {
      result.add(new NewFileInfo(fileList.item(i)));
    }
    return result;
  }

  public static String getUrl(String hash) {
    return getUrl() + "/" + URL.encodePathSegment(Assert.notEmpty(hash));
  }

  public static String getUrl(String hash, String fileName) {
    return getUrl(hash) + "/" + URL.encodePathSegment(Assert.notEmpty(fileName));
  }

  public static String getUrl(String fileName, Map<String, String> files) {
    return CommUtils.getPath(getUrl("zip", fileName),
        Collections.singletonMap(Service.VAR_FILES, Codec.beeSerialize(Assert.notEmpty(files))),
        true);
  }

  public static void readAsDataURL(File file, final Consumer<String> consumer) {
    Assert.notNull(file);
    Assert.notNull(consumer);

    final FileReader reader = Browser.getWindow().newFileReader();

    reader.setOnload(evt -> consumer.accept((String) reader.getResult()));

    reader.readAsDataURL(file);
  }

  public static void readLines(File file, final Consumer<List<String>> consumer) {
    CharMatcher cm = CharMatcher.is(BeeConst.CHAR_EOL).or(CharMatcher.is(BeeConst.CHAR_CR));
    Splitter splitter = Splitter.on(cm).omitEmptyStrings().trimResults();

    readLines(file, splitter, consumer);
  }

  public static void readLines(File file, final Splitter lineSplitter,
      final Consumer<List<String>> consumer) {

    Assert.notNull(file);
    Assert.notNull(lineSplitter);
    Assert.notNull(consumer);

    final FileReader reader = Browser.getWindow().newFileReader();

    reader.setOnload(evt -> {
      String text = (String) reader.getResult();

      if (BeeUtils.isEmpty(text)) {
        consumer.accept(BeeConst.EMPTY_IMMUTABLE_STRING_LIST);
      } else {
        consumer.accept(lineSplitter.splitToList(text));
      }
    });

    reader.readAsText(file);
  }

  public static String sizeToText(Long size) {
    long sz = BeeUtils.unbox(size);
    String prfx = "MB";
    long c = 1;

    for (int i = 1; i < 3; i++) {
      if (sz < c * 1024) {
        prfx = (i == 1) ? "B" : "KB";
        break;
      }
      c *= 1024;
    }
    if (c == 1) {
      return sz + prfx;
    }
    return Math.round(sz * 10d / c) / 10d + prfx;
  }

  public static void uploadFile(FileInfo fileInfo, Callback<FileInfo> callback) {
    upload(fileInfo, false, callback);
  }

  public static <T extends FileInfo> List<T> validateFileSize(Collection<T> input,
      long maxSize, NotificationListener notificationListener) {

    List<T> result = new ArrayList<>();
    if (BeeUtils.isEmpty(input)) {
      return result;
    }

    List<String> errors = new ArrayList<>();

    for (T fileInfo : input) {
      long size = fileInfo.getSize();

      if (size > maxSize) {
        errors.add(BeeUtils.join(BeeConst.STRING_COLON + BeeConst.STRING_SPACE, fileInfo.getName(),
            Localized.dictionary().fileSizeExceeded(size, maxSize)));
      } else {
        result.add(fileInfo);
      }
    }

    if (!errors.isEmpty() && notificationListener != null) {
      result.clear();
      notificationListener.notifyWarning(ArrayUtils.toArray(errors));
    }

    return result;
  }

  //@formatter:off
  static native double getLastModifiedMillis(File file) /*-{
    return file.lastModifiedDate ? file.lastModifiedDate.getTime() : file.lastModified;
  }-*/;
//@formatter:on

  private static void addProgressListener(XMLHttpRequest xhr, final String progressId) {
    if (progressId != null) {
      xhr.getUpload().setOnprogress(evt -> BeeKeeper.getScreen().updateProgress(progressId, null,
          ((ProgressEvent) evt).getLoaded()));
    }
  }

  private static String getUrl() {
    return GWT.getHostPageBaseURL() + AdministrationConstants.FILE_URL;
  }

  private static String maybeCreateProgress(String caption, long size) {
    if (size < MIN_FILE_SIZE_FOR_PROGRESS) {
      return null;
    } else {
      Thermometer widget = new Thermometer(caption, (double) size);
      return BeeKeeper.getScreen().addProgress(widget);
    }
  }

  private static void upload(FileInfo fileInfo, boolean commit, Callback<FileInfo> callback) {
    Assert.noNulls(fileInfo, callback);

    if (DataUtils.isId(fileInfo.getId())) {
      callback.onSuccess(fileInfo);

    } else if (!(fileInfo instanceof NewFileInfo)) {
      callback.onFailure("File is not an instance of " + NameUtils.getClassName(NewFileInfo.class));

    } else {
      String fileName = BeeUtils.notEmpty(fileInfo.getCaption(), fileInfo.getName());
      String fileType = fileInfo.getType();
      long fileSize = fileInfo.getSize();

      long start = System.currentTimeMillis();

      String progressId = maybeCreateProgress(fileName, fileInfo.getSize());

      XMLHttpRequest xhr = RpcUtils.createXhr();
      xhr.open(RequestBuilder.POST.toString(), getUrl(fileName), true);

      if (commit) {
        xhr.setRequestHeader(AdministrationConstants.FILE_COMMIT, "true");
      }
      RpcUtils.addSessionId(xhr);

      xhr.setOnload(evt -> {
        if (progressId != null) {
          BeeKeeper.getScreen().removeProgress(progressId);
        }
        String msg = BeeUtils.joinWords("upload", fileName, "response status:");

        if (xhr.getStatus() == Response.SC_OK) {
          String response = xhr.getResponseText();

          if (JsonUtils.isJson(response)) {
            JSONObject json = JsonUtils.parseObject(response);
            JSONObject status = (JSONObject) json.get("Status");

            if (BeeUtils.toBoolean(JsonUtils.toString(status.get("Success")))) {
              fileInfo.setId(BeeUtils.toLong(JsonUtils.toString(json.get("Result"))));
              fileInfo.setHash(JsonUtils.toString(json.get("Hash")));

              logger.info(TimeUtils.elapsedSeconds(start), "uploaded", fileName);
              logger.info("type:", fileType, "size:", fileSize, "id:", fileInfo.getId(),
                  "hash:", fileInfo.getHash());
              logger.addSeparator();

              callback.onSuccess(fileInfo);
              return;
            }
            msg = BeeUtils.joinWords(msg, status.toString());
          } else {
            msg = BeeUtils.joinWords("Not a json response:", response);
          }
        } else {
          msg = BeeUtils.joinWords(msg, BeeUtils.bracket(xhr.getStatus()), xhr.getStatusText());
        }
        logger.severe(msg);
        callback.onFailure(msg);
      });
      addProgressListener(xhr, progressId);
      xhr.send(((NewFileInfo) fileInfo).getNewFile());
    }
  }

  private FileUtils() {
  }
}
