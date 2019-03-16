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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.DxfNamespaces;
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
     * Property indicating the fields that needs to be part of the response. The
     * projections of the query.
     */
    private Set<String> fields;

    /**
     * Property indicating the sort order of fields.
     */
    private Set<String> sortOrders;

    /**
     * Property indicating explicit event uids.
     */
    private Set<String> events;

    /**
     * Property indicating which event status types to filter
     */
    private EventStatus eventStatus;

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

    /**
     * Property to filter events based on data values
     */
    private List<EventDataValueFilter> eventDataValueFilters = new ArrayList<>();

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventStatus getEventStatus()
    {
        return eventStatus;
    }

    public void setEventStatus( EventStatus eventStatus )
    {
        this.eventStatus = eventStatus;
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
    public List<EventDataValueFilter> getEventDataValueFilters()
    {
        return eventDataValueFilters;
    }

    public void setEventDataValueFilters( List<EventDataValueFilter> eventDataValueFilters )
    {
        this.eventDataValueFilters = eventDataValueFilters;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getFields()
    {
        return fields;
    }

    public void setFields( Set<String> fields )
    {
        this.fields = fields;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getSortOrders()
    {
        return sortOrders;
    }

    public void setSortOrders( Set<String> sortOrders )
    {
        this.sortOrders = sortOrders;
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

}
