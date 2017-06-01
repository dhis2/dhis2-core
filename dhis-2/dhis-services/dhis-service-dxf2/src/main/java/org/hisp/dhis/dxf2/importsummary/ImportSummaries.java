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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.webmessage.AbstractWebMessageResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "importSummaries", namespace = DxfNamespaces.DXF_2_0 )
public class ImportSummaries extends AbstractWebMessageResponse
{
    private ImportStatus status = ImportStatus.SUCCESS;
    
    private int imported;

    private int updated;

    private int deleted;

    private int ignored;

    private ImportOptions importOptions;

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
        if ( importSummary == null )
        {
            return this;
        }

        if ( importSummary.getImportCount() != null )
        {
            imported += importSummary.getImportCount().getImported();
            updated += importSummary.getImportCount().getUpdated();
            deleted += importSummary.getImportCount().getDeleted();
            ignored += importSummary.getImportCount().getIgnored();
        }

        importSummaries.add( importSummary );
        
        status = getHighestOrderImportStatus();

        return this;
    }
    
    public String toCountString()
    {
        return String.format( "Imported %d, updated %d, deleted %d, ignored %d", imported, updated, deleted, ignored );
    }

    public boolean isStatus( ImportStatus status )
    {
        ImportStatus st = getStatus();
        
        return st != null && st.equals( status );
    }

    /**
     * Returns the {@link ImportStatus} with the highest order from the list
     * of import summaries, where {@link ImportStatus#ERROR} is the highest.
     * If no import summaries are present, {@link ImportStatus#SUCCESS} is
     * returned.
     * 
     * @return import status with highest order.
     */
    public ImportStatus getHighestOrderImportStatus()
    {
        return importSummaries.stream()
            .map( ImportSummary::getStatus )
            .max( ( s1, s2 ) -> s1.getOrder() - s2.getOrder() )
            .orElse( ImportStatus.SUCCESS );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportStatus getStatus()
    {
        return status;
    }

    public void setStatus( ImportStatus status )
    {
        this.status = status;
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportOptions getImportOptions()
    {
        return importOptions;
    }

    public void setImportOptions( ImportOptions importOptions )
    {
        this.importOptions = importOptions;
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

    public String toMinimalString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "imported", imported )
            .add( "updated", updated )
            .add( "deleted", deleted )
            .add( "ignored", ignored ).toString();
    }
    
    @Override
    public String toString()
    {
        return "ImportSummaries{" +
            "importSummaries=" + importSummaries +
            '}';
    }
}
