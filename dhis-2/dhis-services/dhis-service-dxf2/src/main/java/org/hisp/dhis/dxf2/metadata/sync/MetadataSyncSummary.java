package org.hisp.dhis.dxf2.metadata.sync;

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
import org.hisp.dhis.dxf2.common.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessageResponse;
import org.hisp.dhis.metadata.version.MetadataVersion;

/**
 * Defines the structure of metadata sync summary
 *
 * @author vanyas
 */
@JacksonXmlRootElement( localName = "metadataSyncSummary", namespace = DxfNamespaces.DXF_2_0 )
public class MetadataSyncSummary implements WebMessageResponse
{
    private ImportReport importReport;

    private ImportSummary importSummary;

    private MetadataVersion metadataVersion;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MetadataVersion getMetadataVersion()
    {
        return metadataVersion;
    }

    public void setMetadataVersion( MetadataVersion metadataVersion )
    {
        this.metadataVersion = metadataVersion;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportSummary getImportSummary()
    {
        return importSummary;
    }

    public void setImportSummary( ImportSummary importSummary )
    {
        this.importSummary = importSummary;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportReport getImportReport()
    {
        return importReport;
    }

    public void setImportReport( ImportReport importReport )
    {
        this.importReport = importReport;
    }

    @Override
    public String toString()
    {
        return "MetadataSyncSummary{" +
            "importReport=" + importReport +
            ", importSummary=" + importSummary +
            ", metadataVersion=" + metadataVersion +
            '}';
    }
}
