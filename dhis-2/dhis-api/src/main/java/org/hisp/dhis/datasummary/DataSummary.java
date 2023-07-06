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
package org.hisp.dhis.datasummary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSummary object to transfer System Statistics
 *
 * @author Joao Antunes
 */
@JacksonXmlRootElement
public class DataSummary {
  private Map<String, Long> objectCounts = new HashMap<>();

  private Map<Integer, Integer> activeUsers = new HashMap<>();

  private Map<String, Integer> userInvitations = new HashMap<>();

  private Map<Integer, Integer> dataValueCount = new HashMap<>();

  private Map<Integer, Long> eventCount = new HashMap<>();

  public DataSummary() {}

  public DataSummary(
      Map<String, Long> objectCounts,
      Map<Integer, Integer> activeUsers,
      Map<String, Integer> userInvitations,
      Map<Integer, Integer> dataValueCount,
      Map<Integer, Long> eventCount) {
    this.objectCounts = objectCounts;
    this.activeUsers = activeUsers;
    this.userInvitations = userInvitations;
    this.dataValueCount = dataValueCount;
    this.eventCount = eventCount;
  }

  @JsonProperty
  public Map<String, Long> getObjectCounts() {
    return objectCounts;
  }

  public void setObjectCounts(Map<String, Long> objectCounts) {
    this.objectCounts = objectCounts;
  }

  @JsonProperty
  public Map<Integer, Integer> getActiveUsers() {
    return activeUsers;
  }

  public void setActiveUsers(Map<Integer, Integer> activeUsers) {
    this.activeUsers = activeUsers;
  }

  @JsonProperty
  public Map<String, Integer> getUserInvitations() {
    return userInvitations;
  }

  public void setUserInvitations(Map<String, Integer> userInvitations) {
    this.userInvitations = userInvitations;
  }

  @JsonProperty
  public Map<Integer, Integer> getDataValueCount() {
    return dataValueCount;
  }

  public void setDataValueCount(Map<Integer, Integer> dataValueCount) {
    this.dataValueCount = dataValueCount;
  }

  @JsonProperty
  public Map<Integer, Long> getEventCount() {
    return eventCount;
  }

  @JsonProperty
  public void setEventCount(Map<Integer, Long> eventCount) {
    this.eventCount = eventCount;
  }
}
