package com.butent.bee.server;

import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.I18nUtils;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.utils.Checksum;
import com.butent.bee.server.utils.JvmUtils;
import com.butent.bee.server.utils.MxUtils;
import com.butent.bee.server.utils.SystemInfo;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Manages services with <code>invoke</code> tag (reflection invocation).
 */

@Stateless
public class Invocation {

  private static BeeLogger logger = LogUtils.getLogger(Invocation.class);

  @SuppressWarnings("unchecked")
  public static <T> T locateRemoteBean(Class<T> beanClass) {
    String beanName = beanClass.getSimpleName();
    T bean = null;

    try {
      bean = (T) InitialContext.doLookup("java:module/" + beanName);

    } catch (NamingException ex) {
      logger.severe("Remote bean not found:", BeeUtils.bracket(beanName));
    } catch (ClassCastException ex) {
      logger.severe("Remote bean cannot be cast to ", BeeUtils.bracket(beanClass.getName()));
    }

    return bean;
  }

  public ResponseObject configInfo() {
    return ResponseObject.collection(Config.getInfo(), ExtendedProperty.class);
  }

  public ResponseObject connectionInfo(RequestInfo reqInfo) {
    Assert.notNull(reqInfo);
    return ResponseObject.collection(reqInfo.getExtendedInfo(), ExtendedProperty.class);
  }

  public ResponseObject loaderInfo() {
    if (JvmUtils.getCvfFailure() == null) {
      return ResponseObject.collection(JvmUtils.getLoadedClasses(), Property.class);
    } else {
      return ResponseObject.error(JvmUtils.getCvfFailure());
    }
  }

  public ResponseObject localeInfo(RequestInfo reqInfo) {
    String mode = reqInfo.getContent();

    if (BeeUtils.length(mode) >= 2) {
      List<Property> lst = new ArrayList<>();

      Map<String, String> constants = Localizations.getDictionary(Localizations.getDefaultLocale());
      for (String key : BeeUtils.split(mode, BeeConst.CHAR_SPACE)) {
        String value = constants.get(key);
        PropertyUtils.addProperty(lst, key, BeeUtils.notEmpty(value, BeeConst.NULL));
      }

      Map<Locale, File> avail = Localizations.getAvailableConstants();
      int idx = 0;
      if (avail != null) {
        PropertyUtils.addProperty(lst, "Available Constants", avail.size());
        for (Map.Entry<Locale, File> entry : avail.entrySet()) {
          PropertyUtils.addProperty(lst,
              BeeUtils.joinWords(++idx, I18nUtils.toString(entry.getKey())), entry.getValue());
        }
      }

      avail = Localizations.getAvailableMessages();
      idx = 0;
      if (avail != null) {
        PropertyUtils.addProperty(lst, "Available Messages", avail.size());
        for (Map.Entry<Locale, File> entry : avail.entrySet()) {
          PropertyUtils.addProperty(lst,
              BeeUtils.joinWords(++idx, I18nUtils.toString(entry.getKey())), entry.getValue());
        }
      }

      Collection<Locale> locales = Localizations.getCachedConstantLocales();
      PropertyUtils.addProperty(lst, "Loaded Constants", locales.size());
      idx = 0;
      for (Locale z : locales) {
        PropertyUtils.addProperty(lst,
            BeeUtils.progress(++idx, locales.size()), I18nUtils.toString(z));
      }

      locales = Localizations.getCachedMessageLocales();
      PropertyUtils.addProperty(lst, "Loaded Messages", locales.size());
      idx = 0;
      for (Locale z : locales) {
        PropertyUtils.addProperty(lst,
            BeeUtils.progress(++idx, locales.size()), I18nUtils.toString(z));
      }

      return ResponseObject.collection(lst, Property.class);

    } else if (BeeUtils.containsSame(mode, "x")) {
      return ResponseObject.collection(I18nUtils.getExtendedInfo(), ExtendedProperty.class);

    } else {
      return ResponseObject.collection(I18nUtils.getInfo(), Property.class);
    }
  }

  public ResponseObject sleep(RequestInfo reqInfo) {
    String millis = reqInfo.getContent();

    if (BeeUtils.isPositiveInt(millis)) {
      logger.debug("sleep", millis);

      try {
        Thread.sleep(BeeUtils.toLong(millis));
        logger.debug("awake", millis);
      } catch (InterruptedException ex) {
        logger.warning(ex);
      }

      return ResponseObject.response(millis);

    } else {
      return ResponseObject.emptyResponse();
    }
  }

  public ResponseObject stringInfo(RequestInfo reqInfo) {
    String data = reqInfo.getContent();
    if (BeeUtils.length(data) <= 0) {
      return ResponseObject.error(reqInfo.getService(), "Request data not found");
    }

    byte[] arr = Codec.toBytes(data);

    Map<String, String> result = new HashMap<>();

    result.put("input", data);

    result.put("adler32.z", Checksum.adler32(arr));
    result.put("crc32.z", Checksum.crc32(arr));
    result.put("adler32", Codec.adler32(arr));
    result.put("crc16", Codec.crc16(arr));
    result.put("crc32", Codec.crc32(arr));
    result.put("crc32d", Codec.crc32Direct(arr));

    return ResponseObject.response(result);
  }

  public ResponseObject systemInfo() {
    List<ExtendedProperty> lst = new ArrayList<>();

    lst.addAll(SystemInfo.getSysInfo());
    PropertyUtils.appendChildrenToExtended(lst, "Runtime", SystemInfo.getRuntimeInfo());

    lst.addAll(SystemInfo.getPackagesInfo());

    PropertyUtils.appendChildrenToExtended(lst, "Thread Static", SystemInfo.getThreadStaticInfo());

    Thread ct = Thread.currentThread();
    String root = "Current Thread";

    PropertyUtils.appendChildrenToExtended(lst, root, SystemInfo.getThreadInfo(ct));
    PropertyUtils.appendChildrenToExtended(lst, BeeUtils.joinWords(root, "Stack"),
        SystemInfo.getThreadStackInfo(ct));

    lst.addAll(SystemInfo.getThreadGroupInfo(ct.getThreadGroup(), true, true));

    PropertyUtils.appendChildrenToExtended(lst, "[xml] Document Builder",
        XmlUtils.getDomBuilderInfo());

    PropertyUtils.appendChildrenToExtended(lst, "[xslt] Transformer Factory",
        XmlUtils.getXsltFactoryInfo());
    PropertyUtils.appendChildrenToExtended(lst, "[xslt] Output Keys",
        XmlUtils.getOutputKeysInfo());

    return ResponseObject.collection(lst, ExtendedProperty.class);
  }

  public ResponseObject vmInfo() {
    List<ExtendedProperty> lst = new ArrayList<>();

    PropertyUtils.appendChildrenToExtended(lst, "Class Loading", MxUtils.getClassLoadingInfo());
    PropertyUtils.appendChildrenToExtended(lst, "Compilation", MxUtils.getCompilationInfo());

    lst.addAll(MxUtils.getGarbageCollectorInfo());

    lst.addAll(MxUtils.getMemoryInfo());
    lst.addAll(MxUtils.getMemoryManagerInfo());
    lst.addAll(MxUtils.getMemoryPoolInfo());

    PropertyUtils.appendChildrenToExtended(lst, "Operating System",
        MxUtils.getOperatingSystemInfo());
    lst.addAll(MxUtils.getRuntimeInfo());

    lst.addAll(MxUtils.getThreadsInfo());

    return ResponseObject.collection(lst, ExtendedProperty.class);
  }
}
