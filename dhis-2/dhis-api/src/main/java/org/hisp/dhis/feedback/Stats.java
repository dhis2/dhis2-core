package org.hisp.dhis.feedback;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "stats", namespace = DxfNamespaces.DXF_2_0 )
public class Stats
{
    private int created;

    private int updated;

    private int deleted;

    private int ignored;

    public Stats()
    {
    }

    public void merge( Stats stats )
    {
        created += stats.getCreated();
        updated += stats.getUpdated();
        deleted += stats.getDeleted();
        ignored += stats.getIgnored();
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getTotal()
    {
        return created + updated + deleted + ignored;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getCreated()
    {
        return created;
    }

    public void incCreated()
    {
        created++;
    }

    public void incCreated( int n )
    {
        created += n;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getUpdated()
    {
        return updated;
    }

    public void incUpdated()
    {
        updated++;
    }

    public void incUpdated( int n )
    {
        updated += n;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getDeleted()
    {
        return deleted;
    }

    public void incDeleted()
    {
        deleted++;
    }

    public void incDeleted( int n )
    {
        deleted += n;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getIgnored()
    {
        return ignored;
    }

    public void incIgnored()
    {
        ignored++;
    }

    public void incIgnored( int n )
    {
        ignored += n;
    }

    public void ignored()
    {
        ignored += created;
        ignored += updated;
        ignored += deleted;

        created = 0;
        updated = 0;
        deleted = 0;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "created", created )
            .add( "updated", updated )
            .add( "deleted", deleted )
            .add( "ignored", ignored )
            .toString();
    }
}
