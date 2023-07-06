/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
import java.util.Date;
import java.util.regex.Pattern;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
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

  @AuditAttribute private DataElement dataElement;

  @AuditAttribute private Period period;

  @AuditAttribute private OrganisationUnit source;

  @AuditAttribute private CategoryOptionCombo categoryOptionCombo;

  @AuditAttribute private CategoryOptionCombo attributeOptionCombo;

  @AuditAttribute private String value;

  private String storedBy;

  private Date created;

  private Date lastUpdated;

  private String comment;

  private Boolean followup;

  private boolean deleted;

  // -------------------------------------------------------------------------
  // Transient properties
  // -------------------------------------------------------------------------

  private transient boolean auditValueIsSet = false;

  private transient boolean valueIsSet = false;

  private transient String auditValue;

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

  /** Indicates whether the value is null. */
  public boolean isNullValue() {
    return StringUtils.trimToNull(value) == null && StringUtils.trimToNull(comment) == null;
  }

  public boolean isFollowup() {
    return followup != null && followup;
  }

  public boolean hasComment() {
    return comment != null && !comment.isEmpty();
  }

  public void toggleFollowUp() {
    if (this.followup == null) {
      this.followup = true;
    } else {
      this.followup = !this.followup;
    }
  }

  public void mergeWith(DataValue other) {
    this.value = other.getValue();
    this.storedBy = other.getStoredBy();
    this.created = other.getCreated();
    this.lastUpdated = other.getLastUpdated();
    this.comment = other.getComment();
    this.followup = other.isFollowup();
    this.deleted = other.isDeleted();
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null) {
      return false;
    }

    if (!getClass().isAssignableFrom(o.getClass())) {
      return false;
    }

    final DataValue other = (DataValue) o;

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

  public void setDataElement(DataElement dataElement) {
    this.dataElement = dataElement;
  }

  @JsonProperty
  public Period getPeriod() {
    return period;
  }

  public void setPeriod(Period period) {
    this.period = period;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  public OrganisationUnit getSource() {
    return source;
  }

  public void setSource(OrganisationUnit source) {
    this.source = source;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  public CategoryOptionCombo getCategoryOptionCombo() {
    return categoryOptionCombo;
  }

  public void setCategoryOptionCombo(CategoryOptionCombo categoryOptionCombo) {
    this.categoryOptionCombo = categoryOptionCombo;
  }

  @JsonProperty
  public String getValue() {
    return value;
  }

  public CategoryOptionCombo getAttributeOptionCombo() {
    return attributeOptionCombo;
  }

  public void setAttributeOptionCombo(CategoryOptionCombo attributeOptionCombo) {
    this.attributeOptionCombo = attributeOptionCombo;
  }

  public void setValue(String value) {
    if (!auditValueIsSet) {
      this.auditValue = valueIsSet ? this.value : value;
      auditValueIsSet = true;
    }

    valueIsSet = true;

    this.value = value;
  }

  public String getStoredBy() {
    return storedBy;
  }

  public void setStoredBy(String storedBy) {
    this.storedBy = storedBy;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setFollowup(Boolean followup) {
    this.followup = followup;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public String getAuditValue() {
    return auditValue;
  }
}
