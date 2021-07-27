/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.feedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.Stats;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "importReport", namespace = DxfNamespaces.DXF_2_0 )
public class ImportReport
{
    private MetadataImportParams importParams;

    private Status status = Status.OK;

    private Map<Class<?>, TypeReport> typeReportMap = new HashMap<>();

    public ImportReport()
    {
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    public TypeReport addTypeReport( TypeReport typeReport )
    {
        if ( !typeReportMap.containsKey( typeReport.getKlass() ) )
            typeReportMap.put( typeReport.getKlass(), new TypeReport( typeReport.getKlass() ) );
        typeReportMap.get( typeReport.getKlass() ).merge( typeReport );

        return typeReport;
    }

    public void addTypeReports( List<TypeReport> typeReports )
    {
        typeReports.forEach( this::addTypeReport );
    }

    public void addTypeReports( Map<Class<?>, TypeReport> typeReportMap )
    {
        typeReportMap.values().forEach( this::addTypeReport );
    }

    public List<ErrorReport> getErrorReports()
    {
        List<ErrorReport> errorReports = new ArrayList<>();
        typeReportMap.values().forEach( typeReport -> errorReports.addAll( typeReport.getErrorReports() ) );

        return errorReports;
    }

    // -----------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MetadataImportParams getImportParams()
    {
        return importParams;
    }

    public void setImportParams( MetadataImportParams importParams )
    {
        this.importParams = importParams;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Status getStatus()
    {
        return status;
    }

    public void setStatus( Status status )
    {
        this.status = status;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Stats getStats()
    {
        Stats stats = new Stats();
        typeReportMap.values().forEach( typeReport -> stats.merge( typeReport.getStats() ) );

        return stats;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "typeReports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "typeReport", namespace = DxfNamespaces.DXF_2_0 )
    public List<TypeReport> getTypeReports()
    {
        return new ArrayList<>( typeReportMap.values() );
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "typeReports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "typeReport", namespace = DxfNamespaces.DXF_2_0 )
    public void setTypeReports( List<TypeReport> typeReports )
    {
        typeReportMap.clear();
        if ( typeReports != null )
        {
            typeReports.forEach( tr -> typeReportMap.put( tr.getKlass(), tr ) );
        }
    }

    public Map<Class<?>, TypeReport> getTypeReportMap()
    {
        return typeReportMap;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "stats", getStats() )
            .add( "typeReports", getTypeReports() )
            .toString();
    }
}
