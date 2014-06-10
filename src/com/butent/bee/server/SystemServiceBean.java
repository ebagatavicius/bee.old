package com.butent.bee.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.io.Filter;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.utils.ClassUtils;
import com.butent.bee.server.utils.JvmUtils;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Resource;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.io.FileNameUtils;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.io.StoredFile;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Manages system requests with <code>rpc_sys</code> tag, for example working with system's files
 * and other resources.
 */

@Stateless
public class SystemServiceBean {

  private static BeeLogger logger = LogUtils.getLogger(SystemServiceBean.class);

  @EJB
  FileStorageBean fs;
  
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    Assert.notEmpty(svc);
    ResponseObject response;

    if (BeeUtils.same(svc, Service.GET_CLASS_INFO)) {
      response = classInfo(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_RESOURCE)) {
      response = getResource(reqInfo);
    } else if (BeeUtils.same(svc, Service.SAVE_RESOURCE)) {
      response = saveResource(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_DIGEST)) {
      response = getDigest(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_FILES)) {
      response = getFiles();
    } else if (BeeUtils.same(svc, Service.GET_FLAGS)) {
      response = getFlags();
      
    } else {
      String msg = BeeUtils.joinWords(svc, "system service not recognized");
      logger.warning(msg);
      response = ResponseObject.warning(msg);
    }

    return response;
  }

  private static ResponseObject classInfo(RequestInfo reqInfo) {
    String cnm = reqInfo.getParameter(Service.VAR_CLASS_NAME);
    String pck = reqInfo.getParameter(Service.VAR_PACKAGE_LIST);

    if (BeeUtils.isEmpty(cnm)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_CLASS_NAME);
    }

    Set<Class<?>> classes = JvmUtils.findClass(cnm, NameUtils.toList(pck));
    if (BeeUtils.isEmpty(classes)) {
      return ResponseObject.warning("Class not found", cnm, pck);
    }
    int c = classes.size();
    
    List<ExtendedProperty> result = Lists.newArrayList();
    result.add(new ExtendedProperty(cnm, pck, BeeUtils.bracket(c)));

    int i = 0;
    if (c > 1) {
      for (Class<?> cls : classes) {
        i++;
        result.add(new ExtendedProperty(cls.getName(), null, BeeUtils.progress(i, c)));
      }
    }

    i = 0;
    for (Class<?> cls : classes) {
      i++;
      if (c > 1) {
        result.add(new ExtendedProperty(cls.getName(), null, BeeUtils.progress(i, c)));
      }
      result.addAll(ClassUtils.getClassInfo(cls));
    }
    
    return ResponseObject.collection(result, ExtendedProperty.class);
  }

  private static ResponseObject getDigest(RequestInfo reqInfo) {
    String src = reqInfo.getContent();

    if (BeeUtils.length(src) <= 0) {
      return ResponseObject.error(reqInfo.getService(), "source not found");
    }
    
    ResponseObject response = new ResponseObject();
    
    if (src.length() < 100) {
      response.addInfo(BeeConst.SERVER, src);
    }
    response.addInfo(BeeConst.SERVER, "source length", src.length());
    response.addInfo(BeeConst.SERVER, Codec.md5(src));
    
    return response;
  }
  
  private ResponseObject getFiles() {
    List<StoredFile> files = fs.getFiles();
    if (files.isEmpty()) {
      return ResponseObject.warning("file repository is empty");
    } else {
      return ResponseObject.response(files);
    }
  }
  
  private static ResponseObject getFlags() {
    Map<String, String> flags = Maps.newHashMap(); 

    File dir = new File(Config.IMAGE_DIR, Paths.FLAG_DIR);
    File[] files = dir.listFiles();
    
    if (files != null) {
      for (File file : files) {
        String name = file.getName();
        
        String key = BeeUtils.normalize(FileNameUtils.getBaseName(name));
        String ext = BeeUtils.normalize(FileNameUtils.getExtension(name));
        
        if (BeeUtils.anyEmpty(key, ext)) {
          logger.warning("invalid flag file:", file.getPath());
          continue;
        }
        
        byte[] bytes = FileUtils.getBytes(file);
        if (bytes == null) {
          logger.severe("error loading flag:", file.getPath());
          break;
        }
        
        String uri = "data:image/" + ext + ";base64," + Codec.toBase64(bytes);
        flags.put(key, uri);
      }
    }

    logger.info("loaded", flags.size(), "flags");
    return ResponseObject.response(flags);
  }

  private static ResponseObject getResource(RequestInfo reqInfo) {
    String mode = reqInfo.getParameter(0);

    if (BeeUtils.same(mode, "cs")) {
      return ResponseObject.collection(FileUtils.getCharsets(), ExtendedProperty.class);
    }
    
    if (BeeUtils.same(mode, "fs")) {
      List<Property> properties = PropertyUtils.createProperties(
          "Path Separator", File.pathSeparator, "Separator", File.separator);
      properties.addAll(FileUtils.getRootsInfo());

      return ResponseObject.collection(properties, Property.class);
    }

    String search = reqInfo.getParameter(1);
    if (BeeUtils.isEmpty(search)) {
      return ResponseObject.error(reqInfo.getService(), "resource name ( parameter",
          CommUtils.rpcParamName(1), ") not specified");
    }

    File resFile = null;
    if (FileUtils.isFile(search)) {
      resFile = new File(search);

    } else if (FileUtils.isFile(Config.WAR_DIR, search)) {
      resFile = new File(Config.WAR_DIR, search);

    } else {
      List<File> roots = null;
      if (BeeUtils.same(mode, "src")) {
        roots = Lists.newArrayList(Config.SOURCE_DIR);
      }

      List<Filter> filters = Lists.newArrayList();
      if (BeeUtils.same(mode, "dir")) {
        filters.add(FileUtils.DIRECTORY_FILTER);
      } else if (BeeUtils.same(mode, "file")) {
        filters.add(FileUtils.FILE_FILTER);
      } else {
        filters.add(FileUtils.INPUT_FILTER);
      }

      String defaultExtension = null;
      if (BeeUtils.same(mode, "src")) {
        defaultExtension = FileUtils.EXT_JAVA;
      } else if (BeeUtils.same(mode, "xml")) {
        defaultExtension = XmlUtils.DEFAULT_XML_EXTENSION;
      }

      List<File> files = FileUtils.findFiles(search, roots, filters, defaultExtension, true, false);
      if (BeeUtils.isEmpty(files)) {
        return ResponseObject.warning("resource", search, "not found");
      }

      if (files.size() > 1 && !FileNameUtils.hasSeparator(search)) {
        List<File> nameMatch = Lists.newArrayList();
        for (File fl : files) {
          if (BeeUtils.same(fl.getName(), search)) {
            nameMatch.add(fl);
          }
        }
        
        if (!nameMatch.isEmpty() && nameMatch.size() < files.size()) {
          files.clear();
          files.addAll(nameMatch);
        }
      }

      if (files.size() > 1) {
        Collections.sort(files);
        
        BeeRowSet rowSet = new BeeRowSet(new BeeColumn(ValueType.NUMBER, "Idx"),
            new BeeColumn(ValueType.TEXT, "Name"),
            new BeeColumn(ValueType.TEXT, "Path"),
            new BeeColumn(ValueType.NUMBER, "Size"),
            new BeeColumn(ValueType.DATE_TIME, "Modified"));

        long totSize = 0;
        long lastMod = 0;
        long x;
        long y;
        int idx = 0;
        
        int rc = 0;

        for (File fl : files) {
          x = fl.isFile() ? fl.length() : 0;
          y = fl.lastModified();
          
          List<String> values = Lists.newArrayList(String.valueOf(++idx),
              fl.getName(), fl.getPath(), String.valueOf(x), String.valueOf(y));
          
          rowSet.addRow(rc++, 0, values);
          
          if (x > 0) {
            totSize += x;
          }
          if (y != 0) {
            lastMod = Math.max(lastMod, y);
          }
        }

        List<String> totals = Lists.newArrayList(null, mode, search, String.valueOf(totSize),
            String.valueOf(lastMod));
        rowSet.addRow(rc++, 0, totals);
        
        ResponseObject response = ResponseObject.response(rowSet);
        response.addInfo(mode, search, "found", files.size(), "files");
        return response;
      }

      resFile = files.get(0);
    }

    Assert.notNull(resFile);
    String resPath = resFile.getPath();
    if (!resFile.exists()) {
      return ResponseObject.warning("file", resPath, "does not exist");
    }
    
    ResponseObject response = ResponseObject.info(mode, search, "found", resPath);

    if (BeeUtils.inListSame(mode, "get", "src", "xml")) {
      if (!FileUtils.isInputFile(resFile)) {
        response.addWarning(resPath, "is not readable");

      } else if (!Config.isText(resFile)) {
        response.addWarning(resPath, "is not a text resource");
      
      } else {
        Charset cs = FileUtils.normalizeCharset(reqInfo.getParameter(2));
        response.addInfo("charset", cs);
        String s = FileUtils.fileToString(resFile, cs);

        if (s == null || s.length() == 0) {
          response.addWarning(resPath, "no content found");

        } else if (BeeUtils.same(mode, "xml")) {
          return response.setResponse(s);

        } else {
          return response.setResponse(new Resource(resPath, s));
        }
      }
    }

    List<Property> result = FileUtils.getFileInfo(resFile);

    if (resFile.isDirectory()) {
      String[] arr = FileUtils.getFiles(resFile);
      int n = ArrayUtils.length(arr);

      if (n > 0) {
        result.addAll(PropertyUtils.createProperties("file", arr));
      } else {
        response.addWarning("no files found");
      }
    }
    
    return response.setCollection(result, Property.class);
  }

  private static ResponseObject saveResource(RequestInfo reqInfo) {
    long start = System.currentTimeMillis();

    String pUri = reqInfo.getParameter(Service.RPC_VAR_URI);
    if (BeeUtils.isEmpty(pUri)) {
      return ResponseObject.parameterNotFound(Service.SAVE_RESOURCE, Service.RPC_VAR_URI);
    }
    String uri = Codec.decodeBase64(pUri);

    String md5 = reqInfo.getParameter(Service.RPC_VAR_MD5);

    String content = reqInfo.getContent();
    if (BeeUtils.isEmpty(content)) {
      return ResponseObject.error("Content not found");
    }
    
    ResponseObject response = new ResponseObject();
    response.addInfo("uri", uri);

    if (BeeUtils.isEmpty(md5)) {
      response.addWarning("md5 not specified");
    } else {
      response.addInfo("md5", md5);
      String z = Codec.md5(content);
      if (!BeeUtils.same(md5, z)) {
        response.addError("md5 does not match");
        response.addError("received", z);
        return response;
      }
    }

    boolean ok = FileUtils.saveToFile(content, uri);
    if (ok) {
      response.addInfo("saved", content.length(), TimeUtils.elapsedSeconds(start));
    } else {
      response.addError("error saving to", uri);
    }
    
    return response;
  }
}
