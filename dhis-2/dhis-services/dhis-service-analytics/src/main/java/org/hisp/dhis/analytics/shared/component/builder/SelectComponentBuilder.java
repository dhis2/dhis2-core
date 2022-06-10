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
import org.hisp.dhis.analytics.shared.component.SelectComponent;
import org.hisp.dhis.analytics.shared.component.element.Element;
import org.hisp.dhis.analytics.shared.component.element.select.EnrollmentDateValueSelectElement;
import org.hisp.dhis.analytics.shared.component.element.select.EventDataValueElement;
import org.hisp.dhis.analytics.shared.component.element.select.ExecutionDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.select.ProgramEnrollmentFlagElement;
import org.hisp.dhis.analytics.shared.component.element.select.SimpleSelectElement;
import org.hisp.dhis.analytics.shared.visitor.select.SelectVisitor;
import org.hisp.dhis.analytics.tei.TeiQueryParams;

/**
 * SelectComponentBuilder is responsible for building the select section of sql
 * query
 *
 * @author dusan bernat
 */
public class SelectComponentBuilder
{
    private TeiQueryParams teiQueryParams;

    /**
     * Instance
     *
     * @return
     */
    public static SelectComponentBuilder builder()
    {
        return new SelectComponentBuilder();
    }

    /**
     * with method of builder
     *
     * @param teiQueryParams
     * @return
     */
    public SelectComponentBuilder withTeiQueryParams( TeiQueryParams teiQueryParams )
    {
        this.teiQueryParams = teiQueryParams;

        return this;
    }

    /**
     * all elements have to be included here
     *
     * @return
     */
    public SelectComponent build()
    {
        String trackedEntityUid = teiQueryParams.getTrackedEntityType().getUid();

        List<Element<SelectVisitor>> elements = new ArrayList<>(
            List.of(
                new SimpleSelectElement( trackedEntityUid, "atei.trackedentityinstanceid",
                    "tracked entity instance id" ),
                new SimpleSelectElement( trackedEntityUid, "atei.trackedentityinstanceuid",
                    "tracked entity instance uid" ),
                new SimpleSelectElement( trackedEntityUid, "atei.\"zDhUuAYrxNC\"", "last name" ),
                new SimpleSelectElement( trackedEntityUid, "atei.\"w75KJ2mc4zz\"", "first name" ),
                new SimpleSelectElement( trackedEntityUid, "atei.\"lZGmxYbs97q\"", "unique id" ) ) );

        Map<String, String> inputUidMap = new HashMap<>();

        inputUidMap.put( "IpHINAT79UW", "Child Program Enrollment Date" );
        inputUidMap.put( "ur1Edk5Oe2n", "TB Program Enrollment Date" );
        elements.addAll( inputUidMap
            .keySet()
            .stream()
            .map( k -> new ProgramEnrollmentFlagElement( trackedEntityUid, k, inputUidMap.get( k ) ) )
            .collect( Collectors.toList() ) );

        inputUidMap.clear();
        inputUidMap.put( "IpHINAT79UW", "is enrolled in Child Program" );
        inputUidMap.put( "ur1Edk5Oe2n", "is enrolled in TB Program" );
        elements.addAll( inputUidMap
            .keySet()
            .stream()
            .map( k -> new EnrollmentDateValueSelectElement( trackedEntityUid, k, inputUidMap.get( k ) ) )
            .collect( Collectors.toList() ) );

        Map<Pair<String, String>, String> pJsonNodeWithPUidMap = new HashMap<>();
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "ZzYYXq4fJie", "IpHINAT79UW" ), "Report Date" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "jdRD35YwbRH", null ), "Stage Sputum Test Report Date" );
        elements.addAll( pJsonNodeWithPUidMap
            .keySet()
            .stream()
            .map( k -> new ExecutionDateValueElement( trackedEntityUid, getJsonNodeUid( k ), getProgramUid( k ),
                pJsonNodeWithPUidMap.get( k ) ) )
            .collect( Collectors.toList() ) );

        pJsonNodeWithPUidMap.clear();
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "cYGaxwK615G", "IpHINAT79UW" ), "Infant HIV test Result" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "zocHNQIQBIN", null ), "TB  smear microscopy test outcome" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "sj3j9Hwc7so", "IpHINAT79UW" ), "Child ARVs" );

        elements.addAll( pJsonNodeWithPUidMap
            .keySet()
            .stream()
            .map( k -> new EventDataValueElement( trackedEntityUid, getProgramStageUid( k ), getProgramUid( k ),
                pJsonNodeWithPUidMap.get( k ) ) )
            .collect( Collectors.toList() ) );

        return new SelectComponent( elements );
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
