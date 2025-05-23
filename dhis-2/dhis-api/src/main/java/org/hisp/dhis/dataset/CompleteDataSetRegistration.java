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
package org.hisp.dhis.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Date;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "completeDataSetRegistration", namespace = DxfNamespaces.DXF_2_0)
public class CompleteDataSetRegistration implements Serializable {
  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = 334738541365949298L;

  private DataSet dataSet;

  private Period period;

  private OrganisationUnit source;

  private CategoryOptionCombo attributeOptionCombo;

  private Date date; // TO-DO rename to created

  private String storedBy; // TO-DO rename to createdBy

  private Date lastUpdated;

  private String lastUpdatedBy;

  private Boolean completed;

  private transient String periodName;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public CompleteDataSetRegistration() {}

  public CompleteDataSetRegistration(
      DataSet dataSet,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo attributeOptionCombo,
      Boolean completed) {
    this.dataSet = dataSet;
    this.period = period;
    this.source = source;
    this.attributeOptionCombo = attributeOptionCombo;
    this.completed = completed;
  }

  public CompleteDataSetRegistration(
      DataSet dataSet,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo attributeOptionCombo,
      Date date,
      String storedBy,
      Date lastUpdated,
      String lastUpdatedBy,
      Boolean completed) {
    this.dataSet = dataSet;
    this.period = period;
    this.source = source;
    this.attributeOptionCombo = attributeOptionCombo;
    this.date = date;
    this.storedBy = storedBy;
    this.lastUpdated = lastUpdated;
    this.lastUpdatedBy = lastUpdatedBy;
    this.completed = completed;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @JsonIgnore
  public boolean hasDate() {
    return date != null;
  }

  @JsonIgnore
  public boolean hasStoredBy() {
    return storedBy != null;
  }

  @JsonIgnore
  public boolean hasLastUpdated() {
    return lastUpdated != null;
  }

  @JsonIgnore
  public boolean hasLastUpdatedBy() {
    return lastUpdatedBy != null;
  }

  // -------------------------------------------------------------------------
  // HashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    final int prime = 31;

    int result = 1;

    result = prime * result + ((dataSet == null) ? 0 : dataSet.hashCode());
    result = prime * result + ((period == null) ? 0 : period.hashCode());
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    result =
        prime * result + ((attributeOptionCombo == null) ? 0 : attributeOptionCombo.hashCode());

    return result;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null) {
      return false;
    }

    if (getClass() != object.getClass()) {
      return false;
    }

    final CompleteDataSetRegistration other = (CompleteDataSetRegistration) object;

    if (dataSet == null) {
      if (other.dataSet != null) {
        return false;
      }
    } else if (!dataSet.equals(other.dataSet)) {
      return false;
    }

    if (period == null) {
      if (other.period != null) {
        return false;
      }
    } else if (!period.equals(other.period)) {
      return false;
    }

    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }

    if (attributeOptionCombo == null) {
      if (other.attributeOptionCombo != null) {
        return false;
      }
    } else if (!attributeOptionCombo.equals(other.attributeOptionCombo)) {
      return false;
    }

    return true;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataSet getDataSet() {
    return dataSet;
  }

  public void setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Period getPeriod() {
    return period;
  }

  public void setPeriod(Period period) {
    this.period = period;
  }

  @JsonProperty(value = "organisationUnit")
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0)
  public OrganisationUnit getSource() {
    return source;
  }

  public void setSource(OrganisationUnit source) {
    this.source = source;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public CategoryOptionCombo getAttributeOptionCombo() {
    return attributeOptionCombo;
  }

  public void setAttributeOptionCombo(CategoryOptionCombo attributeOptionCombo) {
    this.attributeOptionCombo = attributeOptionCombo;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getStoredBy() {
    return storedBy;
  }

  public void setStoredBy(String storedBy) {
    this.storedBy = storedBy;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public void setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getCompleted() {
    return completed;
  }

  public void setCompleted(Boolean isCompleted) {
    this.completed = isCompleted;
  }

  public String getPeriodName() {
    return periodName;
  }

  public void setPeriodName(String periodName) {
    this.periodName = periodName;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("dataSet", dataSet)
        .add("period", period)
        .add("source", source)
        .add("attributeOptionCombo", attributeOptionCombo)
        .add("date", date)
        .add("storedBy", storedBy)
        .add("lastUpdated", lastUpdated)
        .add("lastUpdatedBy", lastUpdatedBy)
        .add("isCompleted", completed)
        .toString();
  }

  /**
   * Creates a copy of the passed in CompleteDataSetRegistration, using all old values except for
   * attributeOptionCombo, which uses the param attributeOptionCombo passed in.
   *
   * @param old old CompleteDataSetRegistration to use values from
   * @param attributeOptionCombo attributeOptionCombo to use as new value in new
   *     CompleteDataSetRegistration
   * @return copy of old CompleteDataSetRegistration except with a new attributeOptionCombo
   */
  public static CompleteDataSetRegistration copyWithNewAttributeOptionCombo(
      @Nonnull CompleteDataSetRegistration old, @Nonnull CategoryOptionCombo attributeOptionCombo) {
    CompleteDataSetRegistration newCopy = new CompleteDataSetRegistration();
    newCopy.setDataSet(old.getDataSet());
    newCopy.setPeriod(old.getPeriod());
    newCopy.setSource(old.getSource());
    newCopy.setAttributeOptionCombo(attributeOptionCombo);
    newCopy.setDate(old.getDate());
    newCopy.setStoredBy(old.getStoredBy());
    newCopy.setLastUpdated(old.getLastUpdated());
    newCopy.setCompleted(old.getCompleted());
    newCopy.setPeriodName(old.getPeriodName());
    return newCopy;
  }
}
