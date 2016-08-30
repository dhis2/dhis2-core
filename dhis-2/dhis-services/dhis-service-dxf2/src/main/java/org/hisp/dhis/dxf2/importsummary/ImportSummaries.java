package org.hisp.dhis.dxf2.importsummary;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.webmessage.AbstractWebMessageResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "importSummaries", namespace = DxfNamespaces.DXF_2_0 )
public class ImportSummaries extends AbstractWebMessageResponse
{
    private int imported;

    private int updated;

    private int deleted;

    private int ignored;

    private List<ImportSummary> importSummaries = new ArrayList<>();

    public ImportSummaries()
    {

    }

    public void addImportSummaries( ImportSummaries importSummaries )
    {
        importSummaries.getImportSummaries().forEach( this::addImportSummary );
    }

    public ImportSummaries addImportSummary( ImportSummary importSummary )
    {
        if ( importSummary.getImportCount() != null )
        {
            imported += importSummary.getImportCount().getImported();
            updated += importSummary.getImportCount().getUpdated();
            deleted += importSummary.getImportCount().getDeleted();
            ignored += importSummary.getImportCount().getIgnored();
        }

        importSummaries.add( importSummary );

        return this;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "importSummaryList", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "importSummary", namespace = DxfNamespaces.DXF_2_0 )
    public List<ImportSummary> getImportSummaries()
    {
        return importSummaries;
    }

    public void setImportSummaries( List<ImportSummary> importSummaries )
    {
        this.importSummaries = importSummaries;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getImported()
    {
        return imported;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getUpdated()
    {
        return updated;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getDeleted()
    {
        return deleted;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getIgnored()
    {
        return ignored;
    }

    @Override
    public String toString()
    {
        return "ImportSummaries{" +
            "importSummaries=" + importSummaries +
            '}';
    }
}
