/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.shared.component.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.shared.component.element.Element;
import org.hisp.dhis.analytics.shared.component.element.EnrollmentDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.EventDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.ExecutionDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.ProgramEnrollmentFlagElement;
import org.hisp.dhis.analytics.shared.component.element.SimpleColumnElement;
import org.hisp.dhis.analytics.shared.component.element.TeavValueElement;
import org.hisp.dhis.analytics.shared.visitor.SelectElementVisitor;
import org.hisp.dhis.analytics.tei.TeiParams;

public class SelectClauseComponentBuilder
{
    private TeiParams teiParams;

    public static SelectClauseComponentBuilder builder()
    {
        return new SelectClauseComponentBuilder();
    }

    public SelectClauseComponentBuilder withTeiParams( TeiParams teiParams )
    {
        this.teiParams = teiParams;

        return this;
    }

    public List<Element<SelectElementVisitor>> build()
    {
        List<Element<SelectElementVisitor>> elements = new ArrayList<>(
            List.of( new SimpleColumnElement( "t.trackedentityinstanceid" ),
                new SimpleColumnElement( "t.uid" ) ) );

        Map<String, String> inputUidMap = new HashMap<>();
        inputUidMap.put( "w75KJ2mc4zz", "First Name" );
        inputUidMap.put( "zDhUuAYrxNC", "LastName" );
        inputUidMap.put( "iESIqZ0R0R0", "DOB" );
        elements.addAll( inputUidMap
            .keySet()
            .stream()
            .map( k -> new TeavValueElement( k, inputUidMap.get( k ) ) ).collect( Collectors.toList() ) );

        inputUidMap.clear();
        inputUidMap.put( "IpHINAT79UW", "Child Program Enrollment Date" );
        inputUidMap.put( "ur1Edk5Oe2n", "TB Program Enrollment Date" );
        elements.addAll( inputUidMap
            .keySet()
            .stream()
            .map( k -> new ProgramEnrollmentFlagElement( k, inputUidMap.get( k ) ) ).collect( Collectors.toList() ) );

        inputUidMap.clear();
        inputUidMap.put( "IpHINAT79UW", "is enrolled in Child Program" );
        inputUidMap.put( "ur1Edk5Oe2n", "is enrolled in TB Program" );
        elements.addAll( inputUidMap
            .keySet()
            .stream()
            .map( k -> new EnrollmentDateValueElement( k, inputUidMap.get( k ) ) ).collect( Collectors.toList() ) );

        Map<Pair<String, String>, String> pJsonNodeWithPUidMap = new HashMap<>();
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "ZzYYXq4fJie", "IpHINAT79UW" ), "Report Date" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "jdRD35YwbRH", null ), "Stage Sputum Test Report Date" );
        elements.addAll( pJsonNodeWithPUidMap
            .keySet()
            .stream()
            .map( k -> new ExecutionDateValueElement( getJsonNodeUid( k ), getProgramUid( k ),
                pJsonNodeWithPUidMap.get( k ) ) )
            .collect( Collectors.toList() ) );

        pJsonNodeWithPUidMap.clear();
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "cYGaxwK615G", "IpHINAT79UW" ), "Infant HIV test Result" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "sj3j9Hwc7so", "IpHINAT79UW" ), "Child ARV's" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "zocHNQIQBIN", null ), "TB  smear microscopy test outcome" );
        elements.addAll( pJsonNodeWithPUidMap
            .keySet()
            .stream()
            .map( k -> new EventDateValueElement( getProgramStageUid( k ), getProgramUid( k ),
                pJsonNodeWithPUidMap.get( k ) ) )
            .collect( Collectors.toList() ) );

        return elements;
    }

    private String getProgramUid( Pair<String, String> pUidWithPsUid )
    {
        return pUidWithPsUid.getRight();
    }

    private String getProgramStageUid( Pair<String, String> pUidWithPsUid )
    {
        return pUidWithPsUid.getLeft();
    }

    private String getJsonNodeUid( Pair<String, String> pUidWithPsUid )
    {
        return pUidWithPsUid.getLeft();
    }

}
