/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.datavalue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UsageTestOnly;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Kristian Nordal
 */
@Auditable(scope = AuditScope.AGGREGATE)
public class DataValue implements Serializable {
  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = 6269303850789110610L;

  private static final Pattern ZERO_PATTERN = Pattern.compile("^0(\\.0*)?$");

  public static final String TRUE = "true";

  public static final String FALSE = "false";

  // -------------------------------------------------------------------------
  // Persistent properties
  // -------------------------------------------------------------------------

  @Setter @AuditAttribute private DataElement dataElement;

  @Setter @AuditAttribute private Period period;

  @Setter @AuditAttribute private OrganisationUnit source;

  @Setter @AuditAttribute private CategoryOptionCombo categoryOptionCombo;

  @Setter @Getter @AuditAttribute private CategoryOptionCombo attributeOptionCombo;

  @AuditAttribute private String value;

  @Setter @Getter private String storedBy;

  @Setter @Getter private Date created;

  @Setter @Getter private Date lastUpdated;

  @Setter @Getter private String comment;

  @Setter private Boolean followup;

  @Setter @Getter private boolean deleted;

  // -------------------------------------------------------------------------
  // Transient properties
  // -------------------------------------------------------------------------

  private transient boolean auditValueIsSet = false;

  private transient boolean valueIsSet = false;

  @Getter private transient String auditValue;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataValue() {
    this.created = new Date();
    this.lastUpdated = new Date();
  }

  /**
   * @param dataElement the data element.
   * @param period the period.
   * @param source the organisation unit.
   * @param categoryOptionCombo the category option combo.
   * @param attributeOptionCombo the attribute option combo.
   */
  public DataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo) {
    this.dataElement = dataElement;
    this.period = period;
    this.source = source;
    this.categoryOptionCombo = categoryOptionCombo;
    this.attributeOptionCombo = attributeOptionCombo;
    this.created = new Date();
    this.lastUpdated = new Date();
  }

  /**
   * @param dataElement the data element.
   * @param period the period.
   * @param source the organisation unit.
   * @param categoryOptionCombo the category option combo.
   * @param attributeOptionCombo the attribute option combo.
   * @param value the value.
   */
  public DataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      String value) {
    this.dataElement = dataElement;
    this.period = period;
    this.source = source;
    this.categoryOptionCombo = categoryOptionCombo;
    this.attributeOptionCombo = attributeOptionCombo;
    this.value = value;
    this.created = new Date();
    this.lastUpdated = new Date();
  }

  /**
   * @param dataElement the data element.
   * @param period the period.
   * @param source the organisation unit.
   * @param categoryOptionCombo the category option combo.
   * @param attributeOptionCombo the attribute option combo.
   * @param value the value.
   * @param storedBy the user that stored this data value.
   * @param lastUpdated the time of the last update to this data value.
   * @param comment the comment.
   */
  public DataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      String value,
      String storedBy,
      Date lastUpdated,
      String comment) {
    this.dataElement = dataElement;
    this.period = period;
    this.source = source;
    this.categoryOptionCombo = categoryOptionCombo;
    this.attributeOptionCombo = attributeOptionCombo;
    this.value = value;
    this.storedBy = storedBy;
    this.created = new Date();
    this.lastUpdated = lastUpdated;
    this.comment = comment;
  }

  /**
   * @param dataElement the data element.
   * @param period the period.
   * @param source the organisation unit.
   * @param categoryOptionCombo the category option combo.
   * @param attributeOptionCombo the attribute option combo.
   * @param value the value.
   * @param storedBy the user that stored this data value.
   * @param lastUpdated the time of the last update to this data value.
   * @param comment the comment.
   * @param followup whether followup is set.
   * @param deleted whether the value is deleted.
   */
  @Builder(toBuilder = true)
  public DataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      String value,
      String storedBy,
      Date lastUpdated,
      String comment,
      Boolean followup,
      boolean deleted) {
    this.dataElement = dataElement;
    this.period = period;
    this.source = source;
    this.categoryOptionCombo = categoryOptionCombo;
    this.attributeOptionCombo = attributeOptionCombo;
    this.value = value;
    this.storedBy = storedBy;
    this.created = new Date();
    this.lastUpdated = lastUpdated;
    this.comment = comment;
    this.followup = followup;
    this.deleted = deleted;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Alias for getCategoryOptionCombo(). TODO remove. */
  public CategoryOptionCombo getOptionCombo() {
    return getCategoryOptionCombo();
  }

  /** Indicates whether the value is a zero. */
  public boolean isZero() {
    return dataElement != null
        && dataElement.getValueType().isNumeric()
        && value != null
        && ZERO_PATTERN.matcher(value).find();
  }

  public boolean isFollowup() {
    return followup != null && followup;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof DataValue other)) {
      return false;
    }

    return dataElement.equals(other.getDataElement())
        && period.equals(other.getPeriod())
        && source.equals(other.getSource())
        && categoryOptionCombo.equals(other.getCategoryOptionCombo())
        && attributeOptionCombo.equals(other.getAttributeOptionCombo());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;

    result = result * prime + dataElement.hashCode();
    result = result * prime + period.hashCode();
    result = result * prime + source.hashCode();
    result = result * prime + categoryOptionCombo.hashCode();
    result = result * prime + attributeOptionCombo.hashCode();

    return result;
  }

  @Override
  public String toString() {
    return "[Data element: "
        + dataElement.getUid()
        + ", period: "
        + period.getUid()
        + ", source: "
        + source.getUid()
        + ", category option combo: "
        + categoryOptionCombo.getUid()
        + ", attribute option combo: "
        + attributeOptionCombo.getUid()
        + ", value: "
        + value
        + ", deleted: "
        + deleted
        + "]";
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  public DataElement getDataElement() {
    return dataElement;
  }

  @JsonProperty
  public Period getPeriod() {
    return period;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  public OrganisationUnit getSource() {
    return source;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  public CategoryOptionCombo getCategoryOptionCombo() {
    return categoryOptionCombo;
  }

  @JsonProperty
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    if (!auditValueIsSet) {
      this.auditValue = valueIsSet ? this.value : value;
      auditValueIsSet = true;
    }

    valueIsSet = true;

    this.value = value;
  }

  @UsageTestOnly
  public DataEntryKey toKey() {
    return new DataEntryKey(
        UID.of(dataElement),
        UID.of(source),
        UID.of(categoryOptionCombo),
        UID.of(attributeOptionCombo),
        period.getIsoDate());
  }

  @UsageTestOnly
  public DataValueEntry toEntry() {
    return new DataValueEntry(
        UID.of(dataElement),
        period.getIsoDate(),
        UID.of(source),
        UID.of(categoryOptionCombo),
        UID.of(attributeOptionCombo),
        value,
        comment,
        followup,
        storedBy,
        created,
        lastUpdated,
        deleted);
  }

  public DataEntryValue toDataEntryValue(int index) {
    return new DataEntryValue(
        index,
        UID.of(getDataElement()),
        UID.of(getSource()),
        UID.of(getCategoryOptionCombo()),
        UID.of(getAttributeOptionCombo()),
        getPeriod().getIsoDate(),
        value,
        comment,
        followup,
        deleted);
  }

  public static List<DataEntryValue> toDataEntryValues(Collection<DataValue> values) {
    List<DataEntryValue> res = new ArrayList<>(values.size());
    int i = 0;
    for (DataValue dv : values) res.add(dv.toDataEntryValue(i++));
    return res;
  }
}
