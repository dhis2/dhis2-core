package org.hisp.dhis.dxf2.events.report;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.Pager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */

@JacksonXmlRootElement( localName = "eventRows", namespace = DxfNamespaces.DXF_2_0 )
public class EventRows
{
    private List<EventRow> eventRows = new ArrayList<>();
    
    private Pager pager;

    public EventRows()
    {
    }

    @JsonProperty( "eventRows" )
    @JacksonXmlElementWrapper( localName = "eventRows", useWrapping = false, namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "eventRows", namespace = DxfNamespaces.DXF_2_0 )
    public List<EventRow> getEventRows()
    {
        return eventRows;
    }

    public void setEventRows( List<EventRow> eventRows )
    {
        this.eventRows = eventRows;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Pager getPager()
    {
        return pager;
    }

    public void setPager( Pager pager )
    {
        this.pager = pager;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        EventRows that = (EventRows) o;

        if ( eventRows != null ? !eventRows.equals( that.eventRows ) : that.eventRows != null ) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        return eventRows != null ? eventRows.hashCode() : 0;
    }

    @Override
    public String toString()
    {
        return "EventRows{" +
            "eventRows=" + eventRows +
            '}';
    }
}
