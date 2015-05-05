package com.butent.bee.client.render;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class TokenRenderer extends AbstractCellRenderer {

  public static final String DEFAULT_SEPARATOR = BeeConst.STRING_SPACE;

  private final List<ColumnToken> tokens = new ArrayList<>();

  public TokenRenderer(List<ColumnToken> tokens) {
    super(null);
    if (!BeeUtils.isEmpty(tokens)) {
      this.tokens.addAll(tokens);
    }
  }

  public void addToken(ColumnToken token) {
    if (token != null) {
      tokens.add(token);
    }
  }

  public void addTokens(List<ColumnToken> cts) {
    Assert.notNull(cts);
    for (ColumnToken token : cts) {
      addToken(token);
    }
  }

  @Override
  public ValueType getExportType() {
    return ValueType.TEXT;
  }

  public int getSize() {
    return tokens.size();
  }

  public List<ColumnToken> getTokens() {
    return tokens;
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    boolean wasSuffix = true;

    for (ColumnToken token : tokens) {
      String value = token.render(row);
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (!wasSuffix && !token.hasPrefix()) {
        sb.append(DEFAULT_SEPARATOR);
      }
      sb.append(value);
      wasSuffix = token.hasSuffix();
    }
    return sb.toString().trim();
  }

  public void setTokens(List<ColumnToken> tokens) {
    BeeUtils.overwrite(this.tokens, tokens);
  }
}
