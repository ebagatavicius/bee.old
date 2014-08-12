package com.butent.bee.client.communication;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Enables to see all processed requests and their information in one object.
 */
@SuppressWarnings("serial")
public class RpcList extends LinkedHashMap<Integer, RpcInfo> {

  private static final BeeLogger logger = LogUtils.getLogger(RpcList.class);

  public static final String[] DEFAULT_INFO_COLUMNS = new String[] {
      RpcInfo.COL_ID, RpcInfo.COL_SERVICE, RpcInfo.COL_METHOD,
      RpcInfo.COL_STATE, RpcInfo.COL_START, RpcInfo.COL_TIMEOUT,
      RpcInfo.COL_EXPIRES, RpcInfo.COL_END, RpcInfo.COL_COMPLETED,
      RpcInfo.COL_REQ_PARAMS, RpcInfo.COL_REQ_TYPE, RpcInfo.COL_REQ_DATA,
      RpcInfo.COL_REQ_ROWS, RpcInfo.COL_REQ_COLS, RpcInfo.COL_REQ_SIZE,
      RpcInfo.COL_RESP_TYPE, RpcInfo.COL_RESP_DATA, RpcInfo.COL_RESP_SIZE,
      RpcInfo.COL_RESP_MSG_CNT, RpcInfo.COL_RESP_MESSAGES,
      RpcInfo.COL_RESP_INFO, RpcInfo.COL_ERR_MSG};

  private static final int DEFAULT_MAX_SIZE = 100;

  private int maxSize = DEFAULT_MAX_SIZE;

  public RpcList() {
    super();
  }

  @Override
  public RpcInfo get(Object key) {
    RpcInfo info = super.get(key);
    if (info == null) {
      logger.warning("rpc id", key, "not found");
    }
    return info;
  }

  public List<String[]> getDefaultInfo() {
    return getInfo(null, DEFAULT_INFO_COLUMNS);
  }

  public List<String[]> getDefaultInfo(Collection<State> states) {
    return getInfo(states, DEFAULT_INFO_COLUMNS);
  }

  public List<String[]> getInfo(Collection<State> states, String... cols) {
    Assert.notNull(cols);
    int c = cols.length;
    Assert.parameterCount(c + 1, 2);

    List<String[]> result = Lists.newArrayList();
    if (isEmpty()) {
      return result;
    }
    String s;

    for (RpcInfo el : values()) {
      if (!el.filterStates(states)) {
        continue;
      }
      String[] arr = new String[c];

      for (int i = 0; i < c; i++) {
        if (BeeUtils.isEmpty(cols[i])) {
          arr[i] = BeeConst.STRING_EMPTY;
          continue;
        }

        if (BeeUtils.same(cols[i], RpcInfo.COL_ID)) {
          s = BeeUtils.toString(el.getId());
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_SERVICE)) {
          s = el.getService();
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_METHOD)) {
          s = el.getMethodString();
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_STATE)) {
          s = el.getStateString();

        } else if (BeeUtils.same(cols[i], RpcInfo.COL_START)) {
          s = el.getStartTime();
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_TIMEOUT)) {
          s = el.getTimeoutString();
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_EXPIRES)) {
          s = el.getExpireTime();
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_END)) {
          s = el.getEndTime();
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_COMPLETED)) {
          s = el.getCompletedTime();

        } else if (BeeUtils.same(cols[i], RpcInfo.COL_REQ_PARAMS)) {
          s = Codec.escapeUnicode(el.getReqParams().toString());
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_REQ_TYPE)) {
          s = BeeUtils.toString(el.getReqType());
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_REQ_DATA)) {
          s = Codec.escapeUnicode(Codec.escapeHtml(el.getReqData()));

        } else if (BeeUtils.same(cols[i], RpcInfo.COL_REQ_ROWS)) {
          s = el.getSizeString(el.getReqRows());
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_REQ_COLS)) {
          s = el.getSizeString(el.getReqCols());
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_REQ_SIZE)) {
          s = el.getSizeString(el.getReqSize());

        } else if (BeeUtils.same(cols[i], RpcInfo.COL_RESP_TYPE)) {
          s = BeeUtils.toString(el.getRespType());
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_RESP_DATA)) {
          s = Codec.escapeUnicode(Codec.escapeHtml(el.getRespData()));

        } else if (BeeUtils.same(cols[i], RpcInfo.COL_RESP_SIZE)) {
          s = el.getSizeString(el.getRespSize());

        } else if (BeeUtils.same(cols[i], RpcInfo.COL_RESP_MSG_CNT)) {
          s = el.getSizeString(el.getRespMsgCnt());
        } else if (BeeUtils.same(cols[i], RpcInfo.COL_RESP_MESSAGES)) {
          s = (el.getRespMessages() == null)
              ? null : Codec.escapeUnicode(el.getRespMessages().toString());

        } else if (BeeUtils.same(cols[i], RpcInfo.COL_RESP_INFO)) {
          s = el.getRespInfoString();

        } else if (BeeUtils.same(cols[i], RpcInfo.COL_ERR_MSG)) {
          s = el.getErrMsg();

        } else {
          s = BeeConst.STRING_EMPTY;
        }

        if (BeeUtils.isEmpty(s)) {
          arr[i] = BeeConst.STRING_EMPTY;
        } else {
          arr[i] = s;
        }
      }

      result.add(arr);
    }
    return result;
  }

  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  public boolean updateRequestInfo(int id, int rows, int cols, int size) {
    RpcInfo el = get(id);

    if (el == null) {
      return false;
    } else {
      if (!BeeConst.isUndef(rows)) {
        el.setReqRows(rows);
      }
      if (!BeeConst.isUndef(cols)) {
        el.setReqCols(cols);
      }
      if (!BeeConst.isUndef(size)) {
        el.setReqSize(size);
      }
    }
    return true;
  }

  @Override
  protected boolean removeEldestEntry(Entry<Integer, RpcInfo> eldest) {
    return getMaxSize() > 0 && size() > getMaxSize();
  }
}
