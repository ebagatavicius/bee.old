package com.butent.bee.client.render;

import com.google.common.base.Splitter;
import com.google.gwt.json.client.JSONObject;

import com.butent.bee.client.utils.JsonUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BranchRenderer extends AbstractCellRenderer {

  private static final String DEFAULT_INPUT_SEPARATOR = BeeConst.STRING_EOL;
  private static final String DEFAULT_OUTPUT_SEPARATOR = BeeConst.STRING_EOL;
  private static final String DEFAULT_INDENTATION = "   ";

  private static String getParameter(Collection<JSONObject> input, String pfx) {
    for (JSONObject json : input) {
      for (String key : json.keySet()) {
        if (BeeUtils.same(key, pfx) || BeeUtils.isPrefix(key, pfx)) {
          return JsonUtils.getString(json, key);
        }
      }
    }
    return null;
  }

  private final String inputSeparator;
  private final Splitter splitter;

  private final String outputSeparator;
  private final String indentation;

  public BranchRenderer(CellSource cellSource, String separator, String options) {
    super(cellSource);

    String inpSep = null;
    String outpSep = null;
    String indent = null;

    List<JSONObject> params = new ArrayList<>();

    if (JsonUtils.isJson(separator)) {
      JSONObject json = JsonUtils.parse(separator);
      if (json != null) {
        params.add(json);
      }

    } else if (BeeUtils.hasLength(separator)) {
      inpSep = separator;
    }

    if (JsonUtils.isJson(options)) {
      JSONObject json = JsonUtils.parse(options);
      if (json != null) {
        params.add(json);
      }

    } else if (BeeUtils.hasLength(options)) {
      indent = options;
    }

    if (!params.isEmpty()) {
      if (inpSep == null) {
        inpSep = getParameter(params, "inp");
      }

      outpSep = getParameter(params, "out");

      if (indent == null) {
        indent = getParameter(params, "ind");
      }
    }

    this.inputSeparator = BeeUtils.nvl(inpSep, DEFAULT_INPUT_SEPARATOR);
    this.splitter = Splitter.on(inputSeparator).trimResults().omitEmptyStrings();

    this.outputSeparator = BeeUtils.nvl(outpSep, DEFAULT_OUTPUT_SEPARATOR);
    this.indentation = BeeUtils.nvl(indent, DEFAULT_INDENTATION);
  }

  @Override
  public String render(IsRow row) {
    String value = getString(row);

    if (BeeUtils.isEmpty(value)) {
      return null;

    } else if (value.contains(inputSeparator)) {
      StringBuilder sb = new StringBuilder();
      int level = 0;

      for (String node : splitter.split(value)) {
        if (level > 0) {
          sb.append(outputSeparator);
          for (int i = 0; i < level; i++) {
            sb.append(indentation);
          }
        }

        sb.append(node);
        level++;
      }

      return sb.toString();

    } else {
      return value;
    }
  }
}
