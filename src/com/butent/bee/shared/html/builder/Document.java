package com.butent.bee.shared.html.builder;

import com.butent.bee.shared.html.builder.elements.Body;
import com.butent.bee.shared.html.builder.elements.Head;
import com.butent.bee.shared.html.builder.elements.Html;

import java.util.List;

public class Document extends Node {

  private static final String DOC_TYPE = "<!doctype html>";

  private static final int DEFAULT_INDENTATION = 2;

  private final Html html;

  private final Head head;
  private final Body body;

  public Document() {
    super();
    this.html = new Html();

    this.head = new Head();
    this.body = new Body();

    html.appendChild(head);
    html.appendChild(body);
  }

  @Override
  public String build(int indentStart, int indentStep) {
    StringBuilder sb = new StringBuilder();

    if (indentStart > 0) {
      sb.append(Node.indent(indentStart, DOC_TYPE));
    } else {
      sb.append(DOC_TYPE);
    }

    sb.append(html.build(indentStart, indentStep));

    return sb.toString();
  }

  public String buildLines() {
    return build(0, DEFAULT_INDENTATION);
  }

  public Body getBody() {
    return body;
  }

  public Head getHead() {
    return head;
  }

  public List<Element> queryTag(String tagName) {
    return html.queryTag(tagName);
  }
}
