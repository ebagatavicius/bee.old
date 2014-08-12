package com.butent.bee.server;

import com.butent.bee.server.http.HttpUtils;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.ejb.EJB;
import javax.mail.internet.MimeUtility;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Manages file transfers between client and server sides.
 */

@WebServlet(urlPatterns = "/file/*")
@SuppressWarnings("serial")
public class FileServlet extends LoginServlet {

  private static final int DEFAULT_BUFFER_SIZE = 10240;

  private static BeeLogger logger = LogUtils.getLogger(FileServlet.class);

  @EJB
  FileStorageBean fs;

  @Override
  protected void doService(HttpServletRequest req, HttpServletResponse resp) {
    Map<String, String> parameters = HttpUtils.getHeaders(req, false);
    parameters.putAll(HttpUtils.getParameters(req, false));

    Long fileId = BeeUtils.isEmpty(req.getPathInfo())
        ? null : BeeUtils.toLongOrNull(req.getPathInfo().substring(1));

    String fileName = null;
    String path = null;
    String mimeType = null;
    boolean isTemporary = false;

    if (!DataUtils.isId(fileId)) {
      String prm = parameters.get(Service.VAR_FILE_ID);

      if (!BeeUtils.isEmpty(prm)) {
        fileId = BeeUtils.toLongOrNull(Codec.decodeBase64(prm));
      }
      prm = parameters.get(Service.VAR_FILE_NAME);

      if (!BeeUtils.isEmpty(prm)) {
        fileName = Codec.decodeBase64(prm);
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
    } else if (!BeeUtils.isEmpty(fileName)) {
      path = Config.getPath(fileName, false);
      fileName = new File(fileName).getName();
    }
    if (path == null) {
      HttpUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          BeeUtils.joinWords("File not found:", fileName));
      return;
    }
    File file = new File(path);

    if (!FileUtils.isInputFile(file)) {
      HttpUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          BeeUtils.joinWords("File was removed:", fileName));
      return;
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
