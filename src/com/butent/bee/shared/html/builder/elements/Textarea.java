package com.butent.bee.shared.html.builder.elements;

import com.butent.bee.shared.html.Autocomplete;
import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.builder.FertileElement;

public class Textarea extends FertileElement {

  private static final String WRAP_HARD = "hard";
  private static final String WRAP_SOFT = "soft";

  public Textarea() {
    super();
  }

  public Textarea addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Textarea autocomplete(Autocomplete autocomplete) {
    String value = (autocomplete == null) ? null : autocomplete.build();
    return autocomplete(value);
  }

  public Textarea autocomplete(String value) {
    setAttribute(Attributes.AUTOCOMPLETE, value);
    return this;
  }

  public Textarea autocompleteOff() {
    setAttribute(Attributes.AUTOCOMPLETE, Autocomplete.OFF);
    return this;
  }

  public Textarea autocompleteOn() {
    setAttribute(Attributes.AUTOCOMPLETE, Autocomplete.ON);
    return this;
  }

  public Textarea autofocus() {
    setAttribute(Attributes.AUTOFOCUS, true);
    return this;
  }

  public Textarea cols(int value) {
    setAttribute(Attributes.COLS, value);
    return this;
  }

  public Textarea dirName(String value) {
    setAttribute(Attributes.DIR_NAME, value);
    return this;
  }

  public Textarea disabled() {
    setAttribute(Attributes.DISABLED, true);
    return this;
  }

  public Textarea enabled() {
    setAttribute(Attributes.DISABLED, false);
    return this;
  }

  public Textarea form(String value) {
    setAttribute(Attributes.FORM, value);
    return this;
  }

  public Textarea id(String value) {
    setId(value);
    return this;
  }

  public Textarea inputModeEmail() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_EMAIL);
    return this;
  }

  public Textarea inputModeFullWidthLatin() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_FULL_WIDTH_LATIN);
    return this;
  }

  public Textarea inputModeKana() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_KANA);
    return this;
  }

  public Textarea inputModeKatakana() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_KATAKANA);
    return this;
  }

  public Textarea inputModeLatin() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_LATIN);
    return this;
  }

  public Textarea inputModeLatinName() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_LATIN_NAME);
    return this;
  }

  public Textarea inputModeLatinProse() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_LATIN_PROSE);
    return this;
  }

  public Textarea inputModeNumeric() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_NUMERIC);
    return this;
  }

  public Textarea inputModeTel() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_TEL);
    return this;
  }

  public Textarea inputModeUrl() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_URL);
    return this;
  }

  public Textarea inputModeVerbatim() {
    setAttribute(Attributes.INPUT_MODE, Keywords.INPUT_MODE_VERBATIM);
    return this;
  }

  public Textarea lang(String value) {
    setLang(value);
    return this;
  }

  public Textarea maxLength(int value) {
    setAttribute(Attributes.MAX_LENGTH, value);
    return this;
  }

  public Textarea minLength(int value) {
    setAttribute(Attributes.MIN_LENGTH, value);
    return this;
  }

  public Textarea name(String value) {
    setAttribute(Attributes.NAME, value);
    return this;
  }

  public Textarea placeholder(String value) {
    setAttribute(Attributes.PLACEHOLDER, value);
    return this;
  }

  public Textarea readOnly() {
    setAttribute(Attributes.READ_ONLY, true);
    return this;
  }

  public Textarea required() {
    setAttribute(Attributes.REQUIRED, true);
    return this;
  }

  public Textarea rows(int value) {
    setAttribute(Attributes.ROWS, value);
    return this;
  }

  public Textarea text(String text) {
    super.appendText(text);
    return this;
  }

  public Textarea title(String value) {
    setTitle(value);
    return this;
  }

  public Textarea wrapHard() {
    setAttribute(Attributes.WRAP, WRAP_HARD);
    return this;
  }

  public Textarea wrapSoft() {
    setAttribute(Attributes.WRAP, WRAP_SOFT);
    return this;
  }
}
