package org.hisp.dhis.programstagefilter;
/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.io.Serializable;
import java.util.Set;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
public class EventQueryCriteria implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    /**
     * Property indicating the followUp status of the enrollment.
     */
    private Boolean followUp;
    
    /**
     * Property indicating the OU selection mode for the event filter
     */
    private OrganisationUnitSelectionMode ouMode;
    
    /**
     * Property indicating the assigned user selection mode for the event filter.
     */
    private AssignedUserSelectionMode assignedUserMode;
    
    /**
     * Property which contains the required assigned user ids to be used in the event filter.
     */
    private Set<String> assignedUsers;
    /**
     * Property indicating a specific tei to be used in the event filter.
     */
    private String trackedEntityInstance;
    
    /**
     * Property which contains the required field ordering along with its direction (asc/desc)
     */
    private String order;
    
    /**
     * Property which contains the required filters to be used when filtering events.
     */
    private Set<String> filters;
    
    /**
     * Property which contains the dataElements to be added to the output grid (only for query api)
     */
    private Set<String> dataElements;
    
    /**
     * Property indicating the fields that needs to be part of the response. (only for event list api)
     */
    private String fields;

    /**
     * Property indicating explicit event uids to be used when listing events.
     */
    private Set<String> events;

    /**
     * Property indicating which event status types to filter
     */
    private EventStatus status;

    /**
     * Property to filter events based on event created dates
     */
    private DatePeriod createdDate;

    /**
     * Property to filter events based on event dates
     */
    private DatePeriod dueDate;

    /**
     * Property to filter events based on event dates
     */
    private DatePeriod lastUpdatedDate;


    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventStatus getStatus()
    {
        return status;
    }

    public void setStatus( EventStatus status )
    {
        this.status = status;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DatePeriod getCreatedDate()
    {
        return createdDate;
    }

    public void setCreatedDate( DatePeriod createdDate )
    {
        this.createdDate = createdDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DatePeriod getDueDate()
    {
        return dueDate;
    }

    public void setDueDate( DatePeriod dueDate )
    {
        this.dueDate = dueDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DatePeriod getLastUpdatedDate()
    {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate( DatePeriod lastUpdatedDate )
    {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getEvents()
    {
        return events;
    }

    public void setEvents( Set<String> events )
    {
        this.events = events;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getFollowUp()
    {
        return followUp;
    }

    public void setFollowUp( Boolean followUp )
    {
        this.followUp = followUp;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnitSelectionMode getOuMode()
    {
        return ouMode;
    }

    public void setOuMode( OrganisationUnitSelectionMode ouMode )
    {
        this.ouMode = ouMode;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTrackedEntityInstance()
    {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance( String trackedEntityInstance )
    {
        this.trackedEntityInstance = trackedEntityInstance;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOrder()
    {
        return order;
    }

    public void setOrder( String order )
    {
        this.order = order;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getFilters()
    {
        return filters;
    }

    public void setFilters( Set<String> filters )
    {
        this.filters = filters;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getDataElements()
    {
        return dataElements;
    }

    public void setDataElements( Set<String> dataElements )
    {
        this.dataElements = dataElements;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFields()
    {
        return fields;
    }

    public void setFields( String fields )
    {
        this.fields = fields;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AssignedUserSelectionMode getAssignedUserMode()
    {
        return assignedUserMode;
    }

    public void setAssignedUserMode( AssignedUserSelectionMode assignedUserMode )
    {
        this.assignedUserMode = assignedUserMode;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getAssignedUsers()
    {
        return assignedUsers;
    }

    public void setAssignedUsers( Set<String> assignedUsers )
    {
        this.assignedUsers = assignedUsers;
    }
    
    

}
