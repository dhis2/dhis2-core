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
package org.hisp.dhis.eventdatavalue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.program.UserInfoSnapshot;

/**
 * @author David Katuscak
 */
@JacksonXmlRootElement(localName = "eventDataValue", namespace = DxfNamespaces.DXF_2_0)
public class EventDataValue implements Serializable {
  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = 2738519623273453182L;

  private String dataElement = "";

  private Date created = new Date();

  private UserInfoSnapshot createdByUserInfo;

  private Date lastUpdated = new Date();

  private UserInfoSnapshot lastUpdatedByUserInfo;

  private String value;

  private Boolean providedElsewhere = false;

  private String storedBy;

  // -------------------------------------------------------------------------
  // Transient properties
  // -------------------------------------------------------------------------

  private transient boolean auditValueIsSet = false;

  private transient boolean valueIsSet = false;

  private transient String auditValue;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public EventDataValue() {}

  public EventDataValue(String dataElement, String value) {
    this.dataElement = dataElement;
    setValue(value);
  }

  public EventDataValue(String dataElement, String value, UserInfoSnapshot userInfo) {
    this.dataElement = dataElement;
    this.storedBy = userInfo.getUsername();
    this.createdByUserInfo = userInfo;
    this.lastUpdatedByUserInfo = userInfo;
    setValue(value);
  }

  public void setAutoFields() {
    Date date = new Date();

    if (created == null) {
      created = date;
    }

    setLastUpdated(date);
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    return Objects.hash(dataElement);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    return dataElement.equals(((EventDataValue) object).dataElement);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------
  @JsonProperty
  public Boolean getProvidedElsewhere() {
    return providedElsewhere;
  }

  public void setProvidedElsewhere(Boolean providedElsewhere) {
    this.providedElsewhere = providedElsewhere;
  }

  public void setDataElement(String dataElement) {
    this.dataElement = dataElement;
  }

  @JsonIgnore
  public String getDataElement() {
    return dataElement;
  }

  @JsonProperty
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @JsonProperty
  public UserInfoSnapshot getCreatedByUserInfo() {
    return createdByUserInfo;
  }

  public void setCreatedByUserInfo(UserInfoSnapshot createdByUserInfo) {
    this.createdByUserInfo = createdByUserInfo;
  }

  @JsonProperty
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @JsonProperty
  public UserInfoSnapshot getLastUpdatedByUserInfo() {
    return lastUpdatedByUserInfo;
  }

  public void setLastUpdatedByUserInfo(UserInfoSnapshot lastUpdatedByUserInfo) {
    this.lastUpdatedByUserInfo = lastUpdatedByUserInfo;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public void setValue(String value) {
    if (!auditValueIsSet) {
      auditValue = valueIsSet ? this.value : value;
      auditValueIsSet = true;
    }

    valueIsSet = true;

    this.value = value;
  }

  @JsonProperty
  public String getValue() {
    return value;
  }

  @JsonProperty
  public String getStoredBy() {
    return storedBy;
  }

  public void setStoredBy(String storedBy) {
    this.storedBy = storedBy;
  }

  @JsonIgnore
  public String getAuditValue() {
    return auditValue;
  }

  @Override
  public String toString() {
    return "EventDataValue{"
        + "dataElement="
        + dataElement
        + ", created="
        + created
        + ", lastUpdated="
        + lastUpdated
        + ", value='"
        + value
        + '\''
        + ", providedElsewhere="
        + providedElsewhere
        + ", storedBy='"
        + storedBy
        + '\''
        + '}';
  }
}
