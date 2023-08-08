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
package org.hisp.dhis.system.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Date;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.scheduling.JobType;

/**
 * @author Lars Helge Overland
 */
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@JacksonXmlRootElement(localName = "notification", namespace = DxfNamespaces.DXF_2_0)
public class Notification implements Comparable<Notification> {
  @EqualsAndHashCode.Include private String uid;

  @ToString.Include private NotificationLevel level;

  @ToString.Include private JobType category;

  @ToString.Include private Date time;

  @ToString.Include private String message;

  private boolean completed;

  private NotificationDataType dataType;

  private JsonNode data;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Notification() {
    this.uid = CodeGenerator.generateUid();
  }

  public Notification(
      NotificationLevel level,
      JobType category,
      Date time,
      String message,
      boolean completed,
      NotificationDataType dataType,
      JsonNode data) {
    this.uid = CodeGenerator.generateUid();
    this.level = level;
    this.category = category;
    this.time = time;
    this.message = message;
    this.completed = completed;
    this.dataType = dataType;
    this.data = data;
  }

  // -------------------------------------------------------------------------
  // Get and set
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public NotificationLevel getLevel() {
    return level;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getId() {
    return uid;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getUid() {
    return uid;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public JobType getCategory() {
    return category;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getTime() {
    return time;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getMessage() {
    return message;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isCompleted() {
    return completed;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public NotificationDataType getDataType() {
    return dataType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public JsonNode getData() {
    return data;
  }

  @Override
  public int compareTo(@Nonnull Notification other) {
    if (category != other.category) {
      return category.compareTo(other.category);
    }
    // flip this/other => newest first
    return other.time.compareTo(time);
  }
}
