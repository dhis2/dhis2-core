package org.hisp.dhis.dxf2.metadata.objectbundle.feedback;

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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleCommitReport
{
    private Map<Class<?>, TypeReport> typeReportMap = new HashMap<>();

    public ObjectBundleCommitReport()
    {
    }

    public ObjectBundleCommitReport( Map<Class<?>, TypeReport> typeReportMap )
    {
        this.typeReportMap = typeReportMap;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    public List<ErrorReport> getErrorReportsByCode( Class<?> klass, ErrorCode errorCode )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( !typeReportMap.containsKey( klass ) )
        {
            return errorReports;
        }

        List<ObjectReport> objectReports = typeReportMap.get( klass ).getObjectReports();

        for ( ObjectReport objectReport : objectReports )
        {
            List<ErrorReport> byCode = objectReport.getErrorReportsByCode().get( errorCode );

            if ( byCode != null )
            {
                errorReports.addAll( byCode );
            }
        }

        return errorReports;
    }

    public void addTypeReport( TypeReport report )
    {
        if ( report == null )
        {
            return;
        }

        if ( !typeReportMap.containsKey( report.getKlass() ) )
        {
            typeReportMap.put( report.getKlass(), report );
            return;
        }

        TypeReport typeReport = typeReportMap.get( report.getKlass() );
        typeReport.merge( typeReport );
    }

    //-----------------------------------------------------------------------------------
    // Getters and Setters
    //-----------------------------------------------------------------------------------

    public boolean isEmpty()
    {
        return typeReportMap.isEmpty();
    }

    public boolean isEmpty( Class<?> klass )
    {
        return typeReportMap.containsKey( klass ) && typeReportMap.get( klass ).getObjectReportMap().isEmpty();
    }

    public Map<Class<?>, TypeReport> getTypeReportMap()
    {
        return typeReportMap;
    }

    public TypeReport getTypeReportMap( Class<?> klass )
    {
        return typeReportMap.get( klass );
    }

    public List<ObjectReport> getObjectReports( Class<?> klass )
    {
        if ( !typeReportMap.containsKey( klass ) )
        {
            return Lists.newArrayList();
        }
        
        return typeReportMap.get( klass ).getObjectReports();
    }

    public List<ErrorReport> getErrorReports( Class<?> klass )
    {
        if ( !typeReportMap.containsKey( klass ) )
        {
            return Lists.newArrayList();
        }
        
        return typeReportMap.get( klass ).getErrorReports().stream().collect( Collectors.toList() );
    }

    public List<ErrorReport> getErrorReports()
    {
        List<ErrorReport> errorReports = new ArrayList<>();
        
        typeReportMap.values().forEach( typeReport -> errorReports.addAll( typeReport.getErrorReports() ) );

        return errorReports;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "typeReportMap", typeReportMap )
            .toString();
    }
}
