/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.Objects;

public final class EventChangeLog {

  @JsonProperty private final User createdBy;
  @JsonProperty private final Date createdAt;
  @JsonProperty private final String type;
  @JsonProperty private final Change change;

  public EventChangeLog(
      @JsonProperty User createdBy,
      @JsonProperty Date createdAt,
      @JsonProperty String type,
      @JsonProperty Change change) {
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.type = type;
    this.change = change;
  }

  @JsonProperty
  public User createdBy() {
    return createdBy;
  }

  @JsonProperty
  public Date createdAt() {
    return createdAt;
  }

  @JsonProperty
  public String type() {
    return type;
  }

  @JsonProperty
  public Change change() {
    return change;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (EventChangeLog) obj;
    return Objects.equals(this.createdBy, that.createdBy)
        && Objects.equals(this.createdAt, that.createdAt)
        && Objects.equals(this.type, that.type)
        && Objects.equals(this.change, that.change);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdAt, type, change);
  }

  @Override
  public String toString() {
    return "EventChangeLog["
        + "createdBy="
        + createdBy
        + ", "
        + "createdAt="
        + createdAt
        + ", "
        + "type="
        + type
        + ", "
        + "change="
        + change
        + ']';
  }

  public record Change(@JsonProperty DataValueChange dataValue) {}

  public record DataValueChange(
      @JsonProperty String dataElement,
      @JsonProperty String previousValue,
      @JsonProperty String currentValue) {}
}
