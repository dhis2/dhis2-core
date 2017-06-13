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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.DxfNamespaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "objectReport", namespace = DxfNamespaces.DXF_2_0 )
public class ObjectReport
{
    private final Class<?> klass;

    private Integer index;

    /**
     * UID of object (if object is id object).
     */
    private String uid;

    /**
     * Name to be used if ImportReportMode is DEBUG
     */
    private String displayName;

    private Map<ErrorCode, List<ErrorReport>> errorReportsByCode = new HashMap<>();

    public ObjectReport( Class<?> klass, Integer index )
    {
        this.klass = klass;
        this.index = index;
    }

    public ObjectReport( Class<?> klass, Integer index, String uid )
    {
        this.klass = klass;
        this.index = index;
        this.uid = uid;
    }

    public ObjectReport( Class<?> klass, Integer index, String uid, String displayName )
    {
        this.klass = klass;
        this.index = index;
        this.uid = uid;
        this.displayName = displayName;
    }

    public ObjectReport( ObjectReport objectReport )
    {
        this.klass = objectReport.getKlass();
        this.index = objectReport.getIndex();
        this.uid = objectReport.getUid();
        this.displayName = objectReport.getDisplayName();
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    public void merge( ObjectReport objectReport )
    {
        addErrorReports( objectReport.getErrorReports() );
    }

    public void addErrorReports( List<? extends ErrorReport> errorReports )
    {
        errorReports.forEach( this::addErrorReport );
    }

    public void addErrorReport( ErrorReport errorReport )
    {
        if ( !errorReportsByCode.containsKey( errorReport.getErrorCode() ) )
        {
            errorReportsByCode.put( errorReport.getErrorCode(), new ArrayList<>() );
        }

        errorReportsByCode.get( errorReport.getErrorCode() ).add( errorReport );
    }

    //-----------------------------------------------------------------------------------
    // Getters and Setters
    //-----------------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Class<?> getKlass()
    {
        return klass;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Integer getIndex()
    {
        return index;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getUid()
    {
        return uid;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "errorReports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "errorReport", namespace = DxfNamespaces.DXF_2_0 )
    public List<ErrorReport> getErrorReports()
    {
        List<ErrorReport> errorReports = new ArrayList<>();
        errorReportsByCode.values().forEach( errorReports::addAll );

        return errorReports;
    }

    public List<ErrorCode> getErrorCodes()
    {
        return new ArrayList<>( errorReportsByCode.keySet() );
    }

    public Map<ErrorCode, List<ErrorReport>> getErrorReportsByCode()
    {
        return errorReportsByCode;
    }

    public boolean isEmpty()
    {
        return errorReportsByCode.isEmpty();
    }

    public int size()
    {
        return errorReportsByCode.size();
    }


    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "klass", klass )
            .add( "index", index )
            .add( "uid", uid )
            .add( "errorReports", getErrorReports() )
            .toString();
    }
}
