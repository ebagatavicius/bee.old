package com.butent.bee.shared.html;

import com.google.common.base.Strings;

import com.butent.bee.shared.utils.BeeUtils;

public class Autocomplete {

  public static final String OFF = "off";
  public static final String ON = "on";

  public static final String FIELD_NAME = "name";
  public static final String FIELD_HONORIFIC_PREFIX = "honorific-prefix";
  public static final String FIELD_GIVEN_NAME = "given-name";
  public static final String FIELD_ADDITIONAL_NAME = "additional-name";
  public static final String FIELD_FAMILY_NAME = "family-name";
  public static final String FIELD_HONORIFIC_SUFFIX = "honorific-suffix";
  public static final String FIELD_NICKNAME = "nickname";
  public static final String FIELD_ORGANIZATION_TITLE = "organization-title";
  public static final String FIELD_ORGANIZATION = "organization";
  public static final String FIELD_STREET_ADDRESS = "street-address";
  public static final String FIELD_ADDRESS_LINE1 = "address-line1";
  public static final String FIELD_ADDRESS_LINE2 = "address-line2";
  public static final String FIELD_ADDRESS_LINE3 = "address-line3";
  public static final String FIELD_LOCALITY = "locality";
  public static final String FIELD_REGION = "region";
  public static final String FIELD_COUNTRY = "country";
  public static final String FIELD_COUNTRY_NAME = "country-name";
  public static final String FIELD_POSTAL_CODE = "postal-code";
  public static final String FIELD_CC_NAME = "cc-name";
  public static final String FIELD_CC_GIVEN_NAME = "cc-given-name";
  public static final String FIELD_CC_ADDITIONAL_NAME = "cc-additional-name";
  public static final String FIELD_CC_FAMILY_NAME = "cc-family-name";
  public static final String FIELD_CC_NUMBER = "cc-number";
  public static final String FIELD_CC_EXP = "cc-exp";
  public static final String FIELD_CC_EXP_MONTH = "cc-exp-month";
  public static final String FIELD_CC_EXP_YEAR = "cc-exp-year";
  public static final String FIELD_CC_CSC = "cc-csc";
  public static final String FIELD_CC_TYPE = "cc-type";
  public static final String FIELD_LANGUAGE = "language";
  public static final String FIELD_BDAY = "bday";
  public static final String FIELD_BDAY_DAY = "bday-day";
  public static final String FIELD_BDAY_MONTH = "bday-month";
  public static final String FIELD_BDAY_YEAR = "bday-year";
  public static final String FIELD_SEX = "sex";
  public static final String FIELD_URL = "url";
  public static final String FIELD_PHOTO = "photo";

  public static final String CONTACT_HOME = "home";
  public static final String CONTACT_WORK = "work";
  public static final String CONTACT_MOBILE = "mobile";
  public static final String CONTACT_FAX = "fax";
  public static final String CONTACT_PAGER = "pager";

  public static final String CONTACT_FIELD_TEL = "tel";
  public static final String CONTACT_FIELD_TEL_COUNTRY_CODE = "tel-country-code";
  public static final String CONTACT_FIELD_TEL_NATIONAL = "tel-national";
  public static final String CONTACT_FIELD_TEL_AREA_CODE = "tel-area-code";
  public static final String CONTACT_FIELD_TEL_LOCAL = "tel-local";
  public static final String CONTACT_FIELD_TEL_LOCAL_PREFIX = "tel-local-prefix";
  public static final String CONTACT_FIELD_TEL_LOCAL_SUFFIX = "tel-local-suffix";
  public static final String CONTACT_FIELD_TEL_EXTENSION = "tel-extension";
  public static final String CONTACT_FIELD_EMAIL = "email";
  public static final String CONTACT_FIELD_IMPP = "impp";

  private static final char SEPARATOR = ' ';

  private static final String SECTION_PREFIX = "section-";

  private static final String SHIPPING = "shipping";
  private static final String BILLING = "billing";

  private String section;
  private String hint;
  private String contact;
  private String field;

  public Autocomplete() {
    super();
  }

  public Autocomplete billing() {
    return hint(BILLING);
  }

  public String build() {
    StringBuilder sb = new StringBuilder();

    if (!BeeUtils.isEmpty(getSection())) {
      sb.append(SECTION_PREFIX + section.trim());
    }

    if (!BeeUtils.isEmpty(getHint())) {
      if (sb.length() > 0) {
        sb.append(SEPARATOR);
      }
      sb.append(getHint().trim());
    }

    if (!BeeUtils.isEmpty(getContact())) {
      if (sb.length() > 0) {
        sb.append(SEPARATOR);
      }
      sb.append(getContact().trim());
    }

    if (!BeeUtils.isEmpty(getField())) {
      if (sb.length() > 0) {
        sb.append(SEPARATOR);
      }
      sb.append(getField().trim());
    }

    return (sb.length() > 0) ? sb.toString() : null;
  }

  public Autocomplete contact(String c, String fld) {
    setContact(c);
    setField(fld);
    return this;
  }

  public Autocomplete field(String fld) {
    setField(fld);
    return this;
  }

  public String getContact() {
    return contact;
  }

  public String getField() {
    return field;
  }

  public String getHint() {
    return hint;
  }

  public String getSection() {
    return section;
  }

  public Autocomplete hint(String h) {
    setHint(h);
    return this;
  }

  public Autocomplete section(String s) {
    setSection(s);
    return this;
  }

  public void setContact(String contact) {
    this.contact = contact;
  }

  public void setField(String field) {
    this.field = field;
  }

  public void setHint(String hint) {
    this.hint = hint;
  }

  public void setSection(String section) {
    this.section = section;
  }

  public Autocomplete shipping() {
    return hint(SHIPPING);
  }

  @Override
  public String toString() {
    return Strings.nullToEmpty(build());
  }
}
