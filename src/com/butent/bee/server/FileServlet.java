package com.butent.bee.server;

import com.google.common.net.MediaType;

import com.butent.bee.server.http.HttpUtils;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.EJB;
import javax.mail.internet.MimeUtility;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Manages file transfers between client and server sides.
 */

@WebServlet(urlPatterns = "/" + AdministrationConstants.FILE_URL + "/*")
@SuppressWarnings("serial")
public class FileServlet extends LoginServlet {

  private static final int DEFAULT_BUFFER_SIZE = 10240;

  private static BeeLogger logger = LogUtils.getLogger(FileServlet.class);

  @EJB
  FileStorageBean fs;

  @Override
  protected void doService(HttpServletRequest req, HttpServletResponse resp) {
    Map<String, String> parameters = HttpUtils.getParameters(req, true);

    Long fileId = null;
    String fileName = null;
    String path = null;
    String mimeType = null;
    boolean isTemporary = false;

    if (!BeeUtils.isEmpty(req.getPathInfo())) {
      String[] args = BeeUtils.split(req.getPathInfo(), '/');

      if (args.length > 0) {
        if (DataUtils.isId(args[0])) {
          fileId = BeeUtils.toLong(args[0]);

          if (args.length > 1) {
            fileName = args[1];
          }
        } else {
          fileName = args[0];
        }
      }
    }
    if (DataUtils.isId(fileId)) {
      try {
        FileInfo sf = fs.getFile(fileId);
        path = sf.getPath();
        fileName = BeeUtils.notEmpty(fileName, sf.getName());
        mimeType = sf.getType();
        isTemporary = sf.isTemporary();

      } catch (IOException e) {
        HttpUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        return;
      }
    } else if (parameters.containsKey(Service.VAR_FILES)) {
      if (FileUtils.isInputFile(parameters.get(Service.VAR_FILES))) {
        path = parameters.get(Service.VAR_FILES);
      } else {
        Map<String, String> files = Codec.deserializeMap(parameters.get(Service.VAR_FILES));

        try {
          File tmp = File.createTempFile("bee_", ".zip");
          tmp.deleteOnExit();
          path = tmp.getAbsolutePath();

          ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmp));

          for (Entry<String, String> entry : files.entrySet()) {
            FileInfo fileInfo = fs.getFile(BeeUtils.toLong(entry.getKey()));

            ZipEntry ze = new ZipEntry(BeeUtils.notEmpty(entry.getValue(), fileInfo.getName()));
            zos.putNextEntry(ze);
            String filePath = fileInfo.getPath();
            FileInputStream in = new FileInputStream(filePath);

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int len;

            while ((len = in.read(buffer)) > 0) {
              zos.write(buffer, 0, len);
            }
            in.close();
            zos.closeEntry();

            if (fileInfo.isTemporary()) {
              logger.debug("File deleted:", filePath, new File(filePath).delete());
            }
          }
          zos.close();

        } catch (IOException e) {
          HttpUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
          return;
        }
        mimeType = MediaType.ZIP.toString();
      }
      isTemporary = true;
    }
    if (path == null) {
      HttpUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          BeeUtils.joinWords("File not found:", fileName));
      return;
    }
    File file = new File(path);

    if (!FileUtils.isInputFile(file)) {
      HttpUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          BeeUtils.joinWords("File is not accessible:", fileName));
      return;
    }

    if (BeeUtils.isEmpty(fileName)) {
      fileName = file.getName();
    }
    if (mimeType == null) {
      mimeType = getServletContext().getMimeType(fileName);
    }
    if (mimeType == null) {
      mimeType = "application/octet-stream";
    }
    try {
      fileName = MimeUtility.encodeText(fileName, BeeConst.CHARSET_UTF8, null);
    } catch (UnsupportedEncodingException ex) {
      logger.warning(ex);
    }
    mimeType = BeeUtils.join("; ", mimeType, "name=\"" + fileName + "\"");

    resp.reset();
    resp.setBufferSize(DEFAULT_BUFFER_SIZE);
    resp.setContentType(mimeType);
    resp.setHeader("Content-Length", String.valueOf(file.length()));
    resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");

    BufferedInputStream input = null;

    try {
      input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
      OutputStream output = resp.getOutputStream();

      byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
      int length;
      while ((length = input.read(buffer)) > 0) {
        output.write(buffer, 0, length);
      }
      output.flush();

    } catch (IOException e) {
      logger.error(e);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (isTemporary) {
        logger.debug("File deleted:", file.getAbsolutePath(), file.delete());
      }
    }
  }
}
