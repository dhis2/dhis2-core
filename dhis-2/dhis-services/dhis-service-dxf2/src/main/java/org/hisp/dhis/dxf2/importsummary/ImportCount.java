package org.hisp.dhis.dxf2.importsummary;

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
import org.hisp.dhis.common.DxfNamespaces;

@JacksonXmlRootElement( localName = "count", namespace = DxfNamespaces.DXF_2_0 )
public class ImportCount
{
    private int imported;

    private int updated;

    private int ignored;

    private int deleted;

    public ImportCount()
    {
    }

    public ImportCount( int imported, int updated, int ignored, int deleted )
    {
        this.imported = imported;
        this.updated = updated;
        this.ignored = ignored;
        this.deleted = deleted;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public int getImported()
    {
        return imported;
    }

    public void setImported( int imported )
    {
        this.imported = imported;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public int getUpdated()
    {
        return updated;
    }

    public void setUpdated( int updated )
    {
        this.updated = updated;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public int getIgnored()
    {
        return ignored;
    }

    public void setIgnored( int ignored )
    {
        this.ignored = ignored;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public int getDeleted()
    {
        return deleted;
    }

    public void setDeleted( int deleted )
    {
        this.deleted = deleted;
    }

    @Override
    public String toString()
    {
        return "[imports=" + imported + ", updates=" + updated + ", ignores=" + ignored + "]";
    }

    public void incrementImported()
    {
        imported++;
    }

    public void incrementUpdated()
    {
        updated++;
    }

    public void incrementIgnored()
    {
        ignored++;
    }

    public void incrementDeleted()
    {
        deleted++;
    }

    public void incrementImported( int n )
    {
        imported += n;
    }

    public void incrementUpdated( int n )
    {
        updated += n;
    }

    public void incrementIgnored( int n )
    {
        ignored += n;
    }

    public void incrementDeleted( int n )
    {
        deleted += n;
    }
}
