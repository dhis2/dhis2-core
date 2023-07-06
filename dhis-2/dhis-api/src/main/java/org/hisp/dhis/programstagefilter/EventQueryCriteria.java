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
package org.hisp.dhis.programstagefilter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;

/**
 * Represents the filtering/sorting criteria to be used when querying events.
 *
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
public class EventQueryCriteria implements Serializable {
  private static final long serialVersionUID = 1L;

  /** Property indicating the followUp status of the enrollment. */
  private Boolean followUp;

  /** Property indication the OU for the filter. */
  private String organisationUnit;

  /** Property indicating the OU selection mode for the event filter */
  private OrganisationUnitSelectionMode ouMode;

  /** Property indicating the assigned user selection mode for the event filter. */
  private AssignedUserSelectionMode assignedUserMode;

  /** Property which contains the required assigned user ids to be used in the event filter. */
  private Set<String> assignedUsers;

  /** Property which contains the required field ordering along with its direction (asc/desc) */
  private String order;

  /** Property which contains the order of output columns */
  private List<String> displayColumnOrder = new ArrayList<>();

  /** Property which contains the filters to be used when querying events. */
  private List<EventDataFilter> dataFilters;

  /** Property indicating explicit event uids to be used when listing events. */
  private Set<String> events;

  /** Property indicating which event status types to filter */
  private EventStatus status;

  /** Property to filter events based on event created dates */
  private DateFilterPeriod eventDate;

  /** Property to filter events based on event dates */
  private DateFilterPeriod dueDate;

  /** Property to filter events based on event dates */
  private DateFilterPeriod lastUpdatedDate;

  /** Property to filter events based on event dates */
  private DateFilterPeriod completedDate;

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public EventStatus getStatus() {
    return status;
  }

  public void setStatus(EventStatus status) {
    this.status = status;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DateFilterPeriod getEventDate() {
    return eventDate;
  }

  public void setEventDate(DateFilterPeriod createdDate) {
    this.eventDate = createdDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DateFilterPeriod getDueDate() {
    return dueDate;
  }

  public void setDueDate(DateFilterPeriod dueDate) {
    this.dueDate = dueDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DateFilterPeriod getCompletedDate() {
    return completedDate;
  }

  public void setCompletedDate(DateFilterPeriod completedDate) {
    this.completedDate = completedDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DateFilterPeriod getLastUpdatedDate() {
    return lastUpdatedDate;
  }

  public void setLastUpdatedDate(DateFilterPeriod lastUpdatedDate) {
    this.lastUpdatedDate = lastUpdatedDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<String> getEvents() {
    return events;
  }

  public void setEvents(Set<String> events) {
    this.events = events;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getFollowUp() {
    return followUp;
  }

  public void setFollowUp(Boolean followUp) {
    this.followUp = followUp;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OrganisationUnitSelectionMode getOuMode() {
    return ouMode;
  }

  public void setOuMode(OrganisationUnitSelectionMode ouMode) {
    this.ouMode = ouMode;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getOrder() {
    return order;
  }

  public void setOrder(String order) {
    this.order = order;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public List<String> getDisplayColumnOrder() {
    return displayColumnOrder;
  }

  public void setDisplayColumnOrder(List<String> filters) {
    this.displayColumnOrder = filters;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public List<EventDataFilter> getDataFilters() {
    return dataFilters;
  }

  public void setDataFilters(List<EventDataFilter> dataElements) {
    this.dataFilters = dataElements;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AssignedUserSelectionMode getAssignedUserMode() {
    return assignedUserMode;
  }

  public void setAssignedUserMode(AssignedUserSelectionMode assignedUserMode) {
    this.assignedUserMode = assignedUserMode;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<String> getAssignedUsers() {
    return assignedUsers;
  }

  public void setAssignedUsers(Set<String> assignedUsers) {
    this.assignedUsers = assignedUsers;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getOrganisationUnit() {
    return organisationUnit;
  }

  public void setOrganisationUnit(String organisationUnit) {
    this.organisationUnit = organisationUnit;
  }
}
