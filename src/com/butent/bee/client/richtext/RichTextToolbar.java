package com.butent.bee.client.richtext;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.richtext.RichTextArea.Formatter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorFactory;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.css.CssUnit;

/**
 * Handles a rich text editor toolbar with all the buttons for formatting the text.
 */

public class RichTextToolbar extends Flow implements EnablableWidget {

  /**
   * Contains a list of necessary methods for text editing functions (bold, italic, justify,
   * hyperlink etc.
   */

  public interface Images extends ClientBundle {

    ImageResource bold();

    ImageResource createLink();

    ImageResource hr();

    ImageResource indent();

    ImageResource insertImage();

    ImageResource italic();

    ImageResource justifyCenter();

    ImageResource justifyLeft();

    ImageResource justifyRight();

    ImageResource ol();

    ImageResource outdent();

    ImageResource removeFormat();

    ImageResource removeLink();

    ImageResource strikeThrough();

    ImageResource subscript();

    ImageResource superscript();

    ImageResource ul();

    ImageResource underline();
  }

  private class EventHandler implements ClickHandler, ChangeHandler, KeyDownHandler, KeyUpHandler {

    @Override
    public void onChange(ChangeEvent event) {
      Widget sender = (Widget) event.getSource();

      if (sender == backColors) {
        formatter.setBackColor(backColors.getValue());
        area.setFocus(true);
      } else if (sender == foreColors) {
        formatter.setForeColor(foreColors.getValue());
        area.setFocus(true);
      } else if (sender == fonts) {
        formatter.setFontName(fonts.getValue());
        area.setFocus(true);
      } else if (sender == fontSizes) {
        if (fontSizes.getSelectedIndex() > 0) {
          formatter.setFontSize(fontSizesConstants[fontSizes.getSelectedIndex() - 1]);
          area.setFocus(true);
        }
      }
    }

    @Override
    public void onClick(ClickEvent event) {
      Widget sender = (Widget) event.getSource();

      if (sender == bold) {
        formatter.toggleBold();
        area.setFocus(true);
      } else if (sender == italic) {
        formatter.toggleItalic();
        area.setFocus(true);
      } else if (sender == underline) {
        formatter.toggleUnderline();
        area.setFocus(true);
      } else if (sender == subscript) {
        formatter.toggleSubscript();
        area.setFocus(true);
      } else if (sender == superscript) {
        formatter.toggleSuperscript();
        area.setFocus(true);
      } else if (sender == strikethrough) {
        formatter.toggleStrikethrough();
        area.setFocus(true);

      } else if (sender == indent) {
        formatter.rightIndent();
        area.setFocus(true);
      } else if (sender == outdent) {
        formatter.leftIndent();
        area.setFocus(true);
      } else if (sender == justifyLeft) {
        formatter.setJustification(RichTextArea.Justification.LEFT);
        area.setFocus(true);
      } else if (sender == justifyCenter) {
        formatter.setJustification(RichTextArea.Justification.CENTER);
        area.setFocus(true);
      } else if (sender == justifyRight) {
        formatter.setJustification(RichTextArea.Justification.RIGHT);
        area.setFocus(true);

      } else if (sender == insertImage) {
        getInput("Image URL", "http://", new Consumer<String>() {
          @Override
          public void accept(String parameter) {
            formatter.insertImage(parameter);
            area.setFocus(true);
          }
        });

      } else if (sender == createLink) {
        getInput("Link URL", "http://", new Consumer<String>() {
          @Override
          public void accept(String parameter) {
            formatter.createLink(parameter);
            area.setFocus(true);
          }
        });

      } else if (sender == hr) {
        formatter.insertHorizontalRule();
        area.setFocus(true);
      } else if (sender == ol) {
        formatter.insertOrderedList();
        area.setFocus(true);
      } else if (sender == ul) {
        formatter.insertUnorderedList();
        area.setFocus(true);

      } else if (sender == removeFormat) {
        formatter.removeFormat();
        area.setFocus(true);
      } else if (sender == area) {
        updateStatus();

      } else if (sender == insertHtml) {
        getInput("Html", null, new Consumer<String>() {
          @Override
          public void accept(String parameter) {
            formatter.insertHtml(parameter);
            area.setFocus(true);
          }
        });

      } else if (sender == undo) {
        formatter.undo();
        area.setFocus(true);
      } else if (sender == redo) {
        formatter.redo();
        area.setFocus(true);
      }
    }

    @Override
    public void onKeyDown(KeyDownEvent event) {
      if (accept != null && UiHelper.isSave(event.getNativeEvent())) {
        event.preventDefault();
        accept.execute();
      }
    }

    @Override
    public void onKeyUp(KeyUpEvent event) {
      Widget sender = (Widget) event.getSource();
      if (sender == area) {
        updateStatus();
      }
    }
  }

  private static final String STYLE_ROW = BeeConst.CSS_CLASS_PREFIX + "RichTextToolbar-row";

  private static final RichTextArea.FontSize[] fontSizesConstants = new RichTextArea.FontSize[] {
      RichTextArea.FontSize.XX_SMALL, RichTextArea.FontSize.X_SMALL,
      RichTextArea.FontSize.SMALL, RichTextArea.FontSize.MEDIUM,
      RichTextArea.FontSize.LARGE, RichTextArea.FontSize.X_LARGE,
      RichTextArea.FontSize.XX_LARGE};

  private final Images images = (Images) GWT.create(Images.class);
  private final EventHandler handler = new EventHandler();

  private final RichTextArea area;
  private final Formatter formatter;

  private final Flow firstRow = new Flow();
  private final Flow secondRow = new Flow();

  private final Toggle bold;
  private final Toggle italic;
  private final Toggle underline;
  private final Toggle subscript;
  private final Toggle superscript;
  private final Toggle strikethrough;

  private final Image indent;
  private final Image outdent;
  private final Image justifyLeft;
  private final Image justifyCenter;
  private final Image justifyRight;
  private final Image hr;
  private final Image ol;
  private final Image ul;
  private final Image insertImage;
  private final Image createLink;
  private final Image removeFormat;
  private final Image insertHtml;
  private final Image undo;
  private final Image redo;

  private final ListBox backColors;
  private final ListBox foreColors;
  private final ListBox fonts;
  private final ListBox fontSizes;

  private final Command accept;

  private boolean waiting;

  public RichTextToolbar(Editor editor, RichTextArea richText, boolean embedded) {
    this.area = richText;
    this.formatter = richText.getFormatter();

    if (embedded) {
      this.accept = null;
    } else {
      this.accept = new EditorFactory.Accept(editor);
      firstRow.add(new Image(Global.getImages().save(), this.accept));
      firstRow.add(createSpacer(1.0, CssUnit.EM));
    }

    this.undo = createButton(Global.getImages().undo(), "Undo");
    firstRow.add(undo);

    this.redo = createButton(Global.getImages().redo(), "Redo");
    firstRow.add(redo);

    this.removeFormat = createButton(images.removeFormat(), "Remove Formatting");
    firstRow.add(removeFormat);

    firstRow.add(createSpacer());

    this.bold = createToggle(images.bold(), "Toggle Bold");
    firstRow.add(bold);

    this.italic = createToggle(images.italic(), "Toggle Italic");
    firstRow.add(italic);

    this.underline = createToggle(images.underline(), "Toggle Underline");
    firstRow.add(underline);

    this.subscript = createToggle(images.subscript(), "Toggle Subscript");
    firstRow.add(subscript);

    this.superscript = createToggle(images.superscript(), "Toggle Superscript");
    firstRow.add(superscript);

    this.strikethrough = createToggle(images.strikeThrough(), "Toggle Strikethrough");
    firstRow.add(strikethrough);

    firstRow.add(createSpacer());

    this.justifyLeft = createButton(images.justifyLeft(), "Left Justify");
    firstRow.add(justifyLeft);

    this.justifyCenter = createButton(images.justifyCenter(), "Center");
    firstRow.add(justifyCenter);

    this.justifyRight = createButton(images.justifyRight(), "Right Justify");
    firstRow.add(justifyRight);

    this.indent = createButton(images.indent(), "Indent Right");
    firstRow.add(indent);

    this.outdent = createButton(images.outdent(), "Indent Left");
    firstRow.add(outdent);

    firstRow.add(createSpacer());

    if (formatter.queryCommandSupported("InsertHTML")) {
      this.insertHtml = createButton(Global.getImages().html(), "Insert HTML");
      firstRow.add(insertHtml);
    } else {
      insertHtml = null;
    }

    this.hr = createButton(images.hr(), "Insert Horizontal Rule");
    firstRow.add(hr);

    this.ol = createButton(images.ol(), "Insert Ordered List");
    firstRow.add(ol);

    this.ul = createButton(images.ul(), "Insert Unordered List");
    firstRow.add(ul);

    firstRow.add(createSpacer());

    this.insertImage = createButton(images.insertImage(), "Insert Image");
    firstRow.add(insertImage);

    this.createLink = createButton(images.createLink(), "Create Link");
    firstRow.add(createLink);

    if (!embedded) {
      firstRow.add(createSpacer(1.0, CssUnit.EM));
      firstRow.add(new Image(Global.getImages().close(), new EditorFactory.Cancel(editor)));
    }

    this.backColors = createColorList("Background");
    secondRow.add(backColors);

    this.foreColors = createColorList("Foreground");
    secondRow.add(foreColors);

    this.fonts = createFontList();
    secondRow.add(fonts);

    this.fontSizes = createFontSizes();
    secondRow.add(fontSizes);

    firstRow.addStyleName(STYLE_ROW);
    secondRow.addStyleName(STYLE_ROW);

    add(firstRow);
    add(secondRow);

    richText.addKeyDownHandler(handler);
    richText.addKeyUpHandler(handler);
    richText.addClickHandler(handler);
  }

  @Override
  public boolean isEnabled() {
    for (Widget child : this) {
      if (child instanceof HasEnabled) {
        return ((HasEnabled) child).isEnabled();
      }
    }
    return true;
  }

  public boolean isWaiting() {
    return waiting;
  }

  @Override
  public void setEnabled(boolean enabled) {
    UiHelper.enableChildren(this, enabled);
  }

  public void updateStatus() {
    bold.setChecked(formatter.isBold());
    italic.setChecked(formatter.isItalic());
    underline.setChecked(formatter.isUnderlined());
    subscript.setChecked(formatter.isSubscript());
    superscript.setChecked(formatter.isSuperscript());
    strikethrough.setChecked(formatter.isStrikethrough());
  }

  private Image createButton(ImageResource img, String tip) {
    Image ib = new Image(img);
    ib.addClickHandler(handler);
    ib.setTitle(tip);
    return ib;
  }

  private ListBox createColorList(String caption) {
    ListBox lb = new ListBox();
    lb.addChangeHandler(handler);
    lb.setVisibleItemCount(1);
    lb.setTabIndex(BeeConst.UNDEF);

    lb.addItem(caption, BeeConst.STRING_EMPTY);
    lb.addItem("White", "white");
    lb.addItem("Black", "black");
    lb.addItem("Red", "red");
    lb.addItem("Green", "green");
    lb.addItem("Yellow", "yellow");
    lb.addItem("Blue", "blue");

    return lb;
  }

  private ListBox createFontList() {
    ListBox lb = new ListBox();
    lb.addChangeHandler(handler);
    lb.setVisibleItemCount(1);
    lb.setTabIndex(BeeConst.UNDEF);

    lb.addItem("Font Name", BeeConst.STRING_EMPTY);
    lb.addItem("Normal", BeeConst.STRING_EMPTY);
    lb.addItem("Times New Roman", "Times New Roman");
    lb.addItem("Arial", "Arial");
    lb.addItem("Courier New", "Courier New");
    lb.addItem("Georgia", "Georgia");
    lb.addItem("Trebuchet", "Trebuchet");
    lb.addItem("Verdana", "Verdana");
    return lb;
  }

  private ListBox createFontSizes() {
    ListBox lb = new ListBox();
    lb.addChangeHandler(handler);
    lb.setVisibleItemCount(1);
    lb.setTabIndex(BeeConst.UNDEF);

    lb.addItem("Font Size", BeeConst.STRING_EMPTY);
    lb.addItem("XX-Small");
    lb.addItem("X-Small");
    lb.addItem("Small");
    lb.addItem("Medium");
    lb.addItem("Large");
    lb.addItem("X-Large");
    lb.addItem("XX-Large");
    return lb;
  }

  private static Widget createSpacer() {
    return createSpacer(5.0, CssUnit.PX);
  }

  private static Widget createSpacer(Double width, CssUnit unit) {
    CustomDiv spacer = new CustomDiv();
    spacer.setWidth(StyleUtils.toCssLength(width, unit));
    return spacer;
  }

  private Toggle createToggle(ImageResource ir, String tip) {
    Image image = new Image(ir);
    String html = DomUtils.getOuterHtml(image.getElement());

    Toggle toggle = new Toggle(html, html);
    toggle.addClickHandler(handler);
    toggle.setTitle(tip);

    return toggle;
  }

  private void getInput(String caption, String defaultValue, final Consumer<String> procedure) {
    setWaiting(true);

    Global.inputString(caption, null, new StringCallback() {
      @Override
      public void onCancel() {
        setWaiting(false);
        super.onCancel();
      }

      @Override
      public void onSuccess(String value) {
        setWaiting(false);
        procedure.accept(value);
      }
    }, defaultValue);
  }

  private void setWaiting(boolean waiting) {
    this.waiting = waiting;
  }
}
