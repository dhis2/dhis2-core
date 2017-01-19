package org.hisp.dhis.dxf2.common;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.importsummary.ImportCount;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "importSummary", namespace = DxfNamespaces.DXF_2_0 )
public class ImportSummary
{
    private ImportCount importCount = new ImportCount();

    private List<ImportTypeSummary> importTypeSummaries = new ArrayList<>();

    public ImportSummary()
    {

    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportCount getImportCount()
    {
        return importCount;
    }

    public void setImportCount( ImportCount importCount )
    {
        this.importCount = importCount;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "typeSummaries", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "typeSummary", namespace = DxfNamespaces.DXF_2_0 )
    public List<ImportTypeSummary> getImportTypeSummaries()
    {
        return importTypeSummaries;
    }

    public void setImportTypeSummaries( List<ImportTypeSummary> importTypeSummaries )
    {
        this.importTypeSummaries = importTypeSummaries;
    }

    //-------------------------------------------------------------------------
    // Helpers
    //-------------------------------------------------------------------------

    public void incrementImportCount( ImportCount importCount )
    {
        this.importCount.incrementImported( importCount.getImported() );
        this.importCount.incrementUpdated( importCount.getUpdated() );
        this.importCount.incrementIgnored( importCount.getIgnored() );
        this.importCount.incrementDeleted( importCount.getDeleted() );
    }

    @Override
    public String toString()
    {
        return "ImportSummary{" +
            "importCount=" + importCount +
            ", importTypeSummaries=" + importTypeSummaries +
            '}';
    }
}
