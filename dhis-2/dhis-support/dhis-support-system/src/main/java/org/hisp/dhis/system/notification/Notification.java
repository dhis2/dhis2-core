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
package org.hisp.dhis.system.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Date;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobType;

/**
 * @author Lars Helge Overland
 */
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@JacksonXmlRootElement(localName = "notification", namespace = DxfNamespaces.DXF_2_0)
public class Notification implements Comparable<Notification> {

  @ToString.Include private NotificationLevel level;
  @ToString.Include private JobType category;
  @ToString.Include @Getter private long timestamp;
  @ToString.Include private String message;
  private boolean completed;
  private NotificationDataType dataType;
  private JsonValue data;

  public Notification() {
    this.timestamp = System.currentTimeMillis();
  }

  public Notification(
      NotificationLevel level,
      JobType category,
      long timestamp,
      String message,
      boolean completed,
      NotificationDataType dataType,
      JsonValue data) {
    this.level = level;
    this.category = category;
    this.timestamp = timestamp;
    this.message = message;
    this.completed = completed;
    this.dataType = dataType;
    this.data = data;
  }

  @JsonProperty
  public NotificationLevel getLevel() {
    return level;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getId() {
    return CodeGenerator.generateUid(timestamp);
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getUid() {
    return getId();
  }

  @JsonProperty
  public JobType getCategory() {
    return category;
  }

  @Nonnull
  @JsonProperty
  public Date getTime() {
    return new Date(timestamp);
  }

  @JsonProperty
  public String getMessage() {
    return message;
  }

  @JsonProperty
  public boolean isCompleted() {
    return completed;
  }

  @JsonProperty
  public NotificationDataType getDataType() {
    return dataType;
  }

  @JsonProperty
  public JsonValue getData() {
    return data;
  }

  /**
   * When comparing {@link Notification}s with the same time & {@link JobType}, we want the {@link
   * Notification} marked as completed to be seen as the latest {@link Notification}. This ensures
   * the UI doesn't 'hang' if the order is not as expected. This is an edge case but has been seen
   * locally.
   *
   * @param other the object to be compared.
   * @return comparison result
   */
  @Override
  public int compareTo(@Nonnull Notification other) {
    if (category != other.category) {
      return category.compareTo(other.category);
    }
    // flip this/other => newest first
    int result = Long.compare(other.timestamp, timestamp);

    if (result != 0) return result;

    if (completed) return -1;
    return result;
  }
}
