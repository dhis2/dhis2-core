package org.hisp.dhis.tracker.report;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
import org.hisp.dhis.tracker.TrackerErrorCode;
import org.hisp.dhis.tracker.TrackerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "objectReport", namespace = DxfNamespaces.DXF_2_0 )
public class TrackerObjectReport
{
    /**
     * Type of object this @{@link TrackerObjectReport} represents.
     */
    private final TrackerType trackerType;

    /**
     * Index into list.
     */
    private Integer index;

    /**
     * UID of object (if object is id object).
     */
    private String uid;

    private Map<TrackerErrorCode, List<TrackerErrorReport>> errorReportsByCode = new HashMap<>();

    public TrackerObjectReport( TrackerType trackerType )
    {
        this.trackerType = trackerType;
    }

    public TrackerObjectReport( TrackerType trackerType, String uid, int index )
    {
        this.trackerType = trackerType;
        this.uid = uid;
        this.index = index;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    public boolean isEmpty()
    {
        return errorReportsByCode.isEmpty();
    }

    public int size()
    {
        return errorReportsByCode.size();
    }

    //-----------------------------------------------------------------------------------
    // Getters and Setters
    //-----------------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public TrackerType getTrackerType()
    {
        return trackerType;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Integer getIndex()
    {
        return index;
    }

    public void setIndex( Integer index )
    {
        this.index = index;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getUid()
    {
        return uid;
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "errorReports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "errorReport", namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackerErrorReport> getErrorReports()
    {
        List<TrackerErrorReport> errorReports = new ArrayList<>();
        errorReportsByCode.values().forEach( errorReports::addAll );

        return errorReports;
    }

    public List<TrackerErrorCode> getErrorCodes()
    {
        return new ArrayList<>( errorReportsByCode.keySet() );
    }

    public Map<TrackerErrorCode, List<TrackerErrorReport>> getErrorReportsByCode()
    {
        return errorReportsByCode;
    }
}
