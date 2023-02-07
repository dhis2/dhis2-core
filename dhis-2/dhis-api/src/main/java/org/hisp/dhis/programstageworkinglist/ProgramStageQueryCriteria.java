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
package org.hisp.dhis.programstageworkinglist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Setter;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.EventDataFilter;
import org.hisp.dhis.trackedentityfilter.AttributeValueFilter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Represents the filtering/sorting criteria to be used when querying program
 * stage working lists.
 */
@Setter
public class ProgramStageQueryCriteria implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Property indicating which event status types to filter
     */
    private EventStatus status;

    /**
     * Property to filter events based on their created dates
     */
    private DateFilterPeriod eventCreatedAt;

    /**
     * Property to filter events based on their scheduled dates
     */
    private DateFilterPeriod scheduledAt;

    /**
     * Property indicating which enrollment status types to filter
     */
    private ProgramStatus enrollmentStatus;

    /**
     * Property to filter events based on their enrolment dates
     */
    private DateFilterPeriod enrolledAt;

    /**
     * Property to filter events based on enrollment incident dates
     */
    private DateFilterPeriod enrollmentOccurredAt;

    /**
     * Property which contains the required field ordering along with its
     * direction (asc/desc)
     */
    private String order;

    /**
     * Property which contains the order of output columns
     */
    private List<String> displayColumnOrder = new ArrayList<>();

    /**
     * Property indicating the OU for the filter.
     */
    private String orgUnit;

    /**
     * Property indicating the OU selection mode for the event filter
     */
    private OrganisationUnitSelectionMode ouMode;

    /**
     * Property indicating the assigned user selection mode for the event
     * filter.
     */
    private AssignedUserSelectionMode assignedUserMode;

    /**
     * Property which contains the required assigned user ids to be used in the
     * event filter.
     */
    private Set<String> assignedUsers;

    /**
     * Property which contains the filters to be used when querying events.
     */
    private List<EventDataFilter> dataFilters;

    /**
     * Property to filter tracked entity instances based on tracked entity
     * attribute values
     */
    private List<AttributeValueFilter> attributeValueFilters = new ArrayList<>();

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventStatus getStatus()
    {
        return status;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DateFilterPeriod getEventCreatedAt()
    {
        return eventCreatedAt;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DateFilterPeriod getScheduledAt()
    {
        return scheduledAt;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStatus getEnrollmentStatus()
    {
        return enrollmentStatus;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DateFilterPeriod getEnrolledAt()
    {
        return enrolledAt;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DateFilterPeriod getEnrollmentOccurredAt()
    {
        return enrollmentOccurredAt;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOrder()
    {
        return order;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getDisplayColumnOrder()
    {
        return displayColumnOrder;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOrgUnit()
    {
        return orgUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnitSelectionMode getOuMode()
    {
        return ouMode;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AssignedUserSelectionMode getAssignedUserMode()
    {
        return assignedUserMode;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getAssignedUsers()
    {
        return assignedUsers;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<EventDataFilter> getDataFilters()
    {
        return dataFilters;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<AttributeValueFilter> getAttributeValueFilters()
    {
        return attributeValueFilters;
    }

}
