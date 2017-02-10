package com.butent.bee.server.modules.administration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

import com.butent.bee.server.Config;
import com.butent.bee.server.Invocation;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.HtmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.lowagie.text.DocumentException;

import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

@Singleton
@Lock(LockType.READ)
public class FileStorageBean {

  private static final BeeLogger logger = LogUtils.getLogger(FileStorageBean.class);

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  ConcurrencyBean cb;
  @EJB
  ParamHolderBean prm;

  private static final long idLimit = 10000000000L;
  private final AtomicLong idGenerator = new AtomicLong(Math.max(idLimit,
      System.currentTimeMillis()));

  private final LoadingCache<Long, FileInfo> cache = CacheBuilder.newBuilder()
      .recordStats()
      .expireAfterAccess(1, TimeUnit.DAYS)
      .removalListener(removalNotification -> ((FileInfo) removalNotification.getValue()).close())
      .build(new CacheLoader<Long, FileInfo>() {
        @Override
        public FileInfo load(Long fileId) throws IOException {
          return supplyFile(fileId);
        }
      });

  public Long commitFile(Long fileId) throws IOException {
    if (fileId < idLimit) {
      return fileId;
    }
    FileInfo storedFile;

    try {
      storedFile = cache.get(fileId);
    } catch (ExecutionException e) {
      throw e.getCause() instanceof IOException
          ? (IOException) e.getCause() : new IOException(e.getCause());
    }
    Holder<Long> id = Holder.absent();
    Holder<Boolean> exists = Holder.of(false);

    cb.synchronizedCall(TBL_FILES, () -> {
      QueryServiceBean queryBean = Invocation.locateRemoteBean(QueryServiceBean.class);

      id.set(queryBean.getLong(new SqlSelect()
          .addFields(TBL_FILES, sys.getIdName(TBL_FILES))
          .addFrom(TBL_FILES)
          .setWhere(SqlUtils.equals(TBL_FILES, COL_FILE_HASH, storedFile.getHash()))));

      if (id.isNull()) {
        id.set(queryBean.insertData(new SqlInsert(TBL_FILES)
            .addConstant(COL_FILE_HASH, storedFile.getHash())
            .addConstant(COL_FILE_NAME,
                sys.clampValue(TBL_FILES, COL_FILE_NAME, storedFile.getName()))
            .addConstant(COL_FILE_SIZE, storedFile.getSize())
            .addConstant(COL_FILE_TYPE,
                sys.clampValue(TBL_FILES, COL_FILE_TYPE, storedFile.getType()))));
      } else {
        exists.set(true);
      }
    });
    File repositoryDir = getRepositoryDir();

    if (Objects.nonNull(repositoryDir)) {
      String repo = qs.getValue(new SqlSelect()
          .addFields(TBL_FILES, COL_FILE_REPO)
          .addFrom(TBL_FILES)
          .setWhere(sys.idEquals(TBL_FILES, id.get())));

      if (BeeUtils.isEmpty(repo) || !FileUtils.isFile(repo)) {
        JustDate dt = new JustDate();

        repositoryDir = new File(repositoryDir,
            BeeUtils.join(File.separator, dt.getYear(), dt.getMonth(), dt.getDom()));
        repositoryDir.mkdirs();

        File target = new File(repositoryDir, storedFile.getHash());
        repo = target.getAbsolutePath();

        if (target.exists()) {
          logger.warning("File already existed:", repo);
          target.delete();
        }
        File tmp = storedFile.getFile();

        if (!tmp.renameTo(target)) {
          throw new BeeRuntimeException(BeeUtils.joinWords("Error renaming file:",
              tmp.getAbsolutePath(), "to:", repo));
        }
        qs.updateData(new SqlUpdate(TBL_FILES)
            .addConstant(COL_FILE_REPO, repo)
            .setWhere(sys.idEquals(TBL_FILES, id.get())));
      }
    } else if (!exists.get()) {
      File tmp = File.createTempFile("bee_", null);
      tmp.deleteOnExit();

      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmp));
      out.putNextEntry(new ZipEntry(storedFile.getName()));
      Files.copy(java.nio.file.Paths.get(storedFile.getPath()), out);
      out.closeEntry();
      out.flush();
      out.close();

      try (InputStream in = new FileInputStream(tmp)) {
        byte[] buffer = new byte[0x100000];
        int bytesRead = in.read(buffer);

        while (bytesRead > 0) {
          long recId = qs.insertData(new SqlInsert(TBL_FILE_PARTS).addConstant(COL_FILE, id.get()));
          qs.updateBlob(TBL_FILE_PARTS, recId, COL_FILE_PART, new ByteArrayInputStream(buffer));
          bytesRead = in.read(buffer);
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      } finally {
        tmp.delete();
      }
      storedFile.close();
    }
    return id.get();
  }

  public Long createPdf(String content, String... styleSheets) {
    StringBuilder sb = new StringBuilder();

    for (String name : new String[] {PRM_PRINT_HEADER, PRM_PRINT_FOOTER}) {
      Long id = prm.getRelation(name);

      if (DataUtils.isId(id)) {
        String nameContent = qs.getValue(new SqlSelect()
            .addFields(TBL_EDITOR_TEMPLATES, COL_EDITOR_TEMPLATE_CONTENT)
            .addFrom(TBL_EDITOR_TEMPLATES)
            .setWhere(sys.idEquals(TBL_EDITOR_TEMPLATES, id)));

        if (!BeeUtils.isEmpty(nameContent)) {
          sb.append("<div style=\"position:running(").append(name).append(")\">")
              .append(nameContent)
              .append("</div>");
        }
      }
    }
    String parsed = HtmlUtils.cleanXml(sb.append(content).toString());

    Map<Long, String> files = HtmlUtils.getFileReferences(parsed);
    Long file = null;

    try {
      for (Long fileId : files.keySet()) {
        FileInfo fileInfo = getFile(fileId);
        parsed = parsed.replace(files.get(fileId), fileInfo.getFile().toURI().toString());
      }
      parsed = parsed.replace("src=\"" + Paths.IMAGE_DIR, "src=\"" + Config.IMAGE_DIR.toURI());

      StringBuilder html = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
          .append("<!DOCTYPE html [<!ENTITY nbsp \"&#160;\">]>")
          .append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>")
          .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");

      List<String> styles = new ArrayList<>();
      styles.add("print");
      styles.add("commons");
      styles.add("trade");

      if (!ArrayUtils.isEmpty(styleSheets)) {
        styles.addAll(Arrays.asList(styleSheets));
      }
      for (String style : styles) {
        html.append("<link rel=\"stylesheet\" href=\"")
            .append(new File(Config.WAR_DIR, Paths.getStyleSheetPath(style)).toURI().toString())
            .append("\" />");
      }
      sb = new StringBuilder();

      for (Pair<String, String> pair : Arrays.asList(Pair.of(PRM_PRINT_MARGINS, "margin"),
          Pair.of(PRM_PRINT_SIZE, "size"))) {
        String prop = prm.getText(pair.getA());

        if (!BeeUtils.isEmpty(prop)) {
          sb.append(pair.getB()).append(":").append(prop).append(";");
        }
      }
      if (BeeUtils.isPositive(sb.length())) {
        html.append("<style>")
            .append("@page {").append(sb.toString()).append("}")
            .append("</style>");
      }
      html.append("</head><body>").append(parsed).append("</body></html>");

      ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(html.toString());
      renderer.layout();

      File tmp = File.createTempFile("bee_", ".pdf");
      tmp.deleteOnExit();
      FileOutputStream os = new FileOutputStream(tmp);

      renderer.createPDF(os);
      os.close();

      file = storeFile(new FileInputStream(tmp), tmp.getName(), null);
      tmp.delete();

    } catch (IOException | DocumentException e) {
      logger.error(e);
    }
    return file;
  }

  public String getCacheStats() {
    return BeeUtils.joinWords(cache.stats().toString(), "size", cache.size());
  }

  public FileInfo getFile(Long fileId) throws IOException {
    try {
      return cache.get(fileId);
    } catch (ExecutionException e) {
      throw e.getCause() instanceof IOException
          ? (IOException) e.getCause() : new IOException(e.getCause());
    }
  }

  public List<FileInfo> getFileInfos(List<Long> fileIds) {
    List<FileInfo> files = new ArrayList<>();

    String idName = sys.getIdName(TBL_FILES);
    String versionName = sys.getVersionName(TBL_FILES);

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_FILES, idName, versionName,
            COL_FILE_HASH, COL_FILE_REPO, COL_FILE_NAME, COL_FILE_SIZE, COL_FILE_TYPE)
        .addFrom(TBL_FILES)
        .setWhere(sys.idInList(TBL_FILES, fileIds))
        .addOrder(TBL_FILES, versionName));

    for (SimpleRow row : data) {
      FileInfo sf = new FileInfo(row.getLong(idName),
          row.getValue(COL_FILE_NAME), row.getLong(COL_FILE_SIZE), row.getValue(COL_FILE_TYPE));

      sf.setPath(row.getValue(COL_FILE_REPO));
      sf.setHash(row.getValue(COL_FILE_HASH));
      sf.setFileDate(DateTime.restore(row.getValue(versionName)));
      sf.setIcon(ExtensionIcons.getIcon(sf.getName()));
      files.add(sf);
    }
    return files;
  }

  public Long storeFile(InputStream is, String fileName, String mimeType) throws IOException {
    MessageDigest md;

    try {
      md = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException e) {
      throw new BeeRuntimeException(e);
    }
    InputStream in = new DigestInputStream(is, md);
    File tmp = File.createTempFile("bee_", null);
    tmp.deleteOnExit();
    long size = Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
    in.close();
    String hash = Codec.toHex(md.digest());

    Long id = qs.getLong(new SqlSelect()
        .addField(TBL_FILES, sys.getIdName(TBL_FILES), COL_FILE)
        .addFrom(TBL_FILES)
        .setWhere(SqlUtils.equals(TBL_FILES, COL_FILE_HASH, hash)));

    if (DataUtils.isId(id)) {
      tmp.delete();
    } else {
      byte[] buffer = new byte[16];
      in = new FileInputStream(tmp);
      int bytesRead = in.read(buffer);
      InputStream header = new ByteArrayInputStream(buffer, 0, bytesRead);
      in.close();

      String name = BeeUtils.notEmpty(fileName, tmp.getName());
      String type = BeeUtils.notEmpty(URLConnection.guessContentTypeFromStream(header), mimeType,
          URLConnection.guessContentTypeFromName(name));

      id = idGenerator.incrementAndGet();
      FileInfo fileInfo = new FileInfo(id, name, size, type);
      fileInfo.setPath(tmp.getAbsolutePath());
      fileInfo.setHash(hash);
      fileInfo.setTemporary(true);

      cache.put(id, fileInfo);
    }
    return id;
  }

  private static File getRepositoryDir() {
    String repo = Config.getProperty("RepositoryDir");

    if (!BeeUtils.isEmpty(repo)) {
      File repository = new File(repo);

      if (FileUtils.isDirectory(repository)) {
        return repository;
      } else {
        logger.warning("Wrong repository directory:", repo);
      }
    }
    return null;
  }

  private FileInfo supplyFile(Long fileId) throws IOException {
    Assert.notNull(fileId);
    FileInfo storedFile = BeeUtils.peek(getFileInfos(Collections.singletonList(fileId)));

    if (Objects.isNull(storedFile)) {
      throw new FileNotFoundException("File not found: id=" + fileId);
    }
    if (BeeUtils.isEmpty(storedFile.getPath())) {
      SqlSelect query = new SqlSelect().setLimit(10)
          .addFields(TBL_FILE_PARTS, COL_FILE_PART)
          .addFrom(TBL_FILE_PARTS)
          .setWhere(SqlUtils.equals(TBL_FILE_PARTS, COL_FILE, fileId))
          .addOrder(TBL_FILE_PARTS, sys.getIdName(TBL_FILE_PARTS));

      File tmp = File.createTempFile("bee_", null);
      tmp.deleteOnExit();
      OutputStream out = new FileOutputStream(tmp);
      List<byte[]> rs = qs.getBytesColumn(query);

      while (rs.size() > 0) {
        for (byte[] bytes : rs) {
          if (bytes != null) {
            out.write(bytes, 0, bytes.length);
          }
        }
        if (rs.size() < query.getLimit()) {
          break;
        }
        rs = qs.getBytesColumn(query.setOffset(query.getOffset() + query.getLimit()));
      }
      out.flush();
      out.close();

      File res = File.createTempFile("bee_", null);
      res.deleteOnExit();
      storedFile.setPath(res.getAbsolutePath());
      storedFile.setTemporary(true);

      ZipInputStream in = new ZipInputStream(new FileInputStream(tmp));

      if (Objects.nonNull(in.getNextEntry())) {
        Files.copy(in, res.toPath(), StandardCopyOption.REPLACE_EXISTING);
        in.closeEntry();
        in.close();
      }
      tmp.delete();
    }
    return FileInfo.restore(storedFile.serialize());
  }
}
