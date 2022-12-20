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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.hibernate.HibernateTrackedEntityInstanceStore;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class HibernateTrackedEntityInstanceStoreTest extends TrackerTest
{

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private HibernateTrackedEntityInstanceStore teiStore;

    private TrackedEntityInstanceQueryParams params;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        User userA = userService.getUser( "M5zQapPyTZI" );
        assertNoErrors(
            trackerImportService
                .importTracker( fromJson( "tracker/tracked_entity_basic_data_for_ordering.json", userA.getUid() ) ) );
        manager.flush();

        params = new TrackedEntityInstanceQueryParams();
    }

    @ParameterizedTest
    @MethodSource( "provideOrderParamsAndExpectedResult" )
    void whenOrderingByBasicStaticFieldsThenEntitiesAreSortedAccordingly( String input, String expected )
    {
        params.setOrders( List.of( new OrderParam( input, OrderParam.SortDirection.ASC ) ) );

        List<Long> list = teiStore.getTrackedEntityInstanceIds( params );
        list = reduceListValuesToStartCountingFromOne( list );

        assertEquals( expected, list.toString() );
    }

    private static Stream<Arguments> provideOrderParamsAndExpectedResult()
    {
        return Stream.of(
            Arguments.of( "createdAt", "[1, 2, 3, 4]" ),
            Arguments.of( "enrolledAt", "[3, 2, 1, 4]" ),
            Arguments.of( "trackedEntity", "[1, 4, 2, 3]" ) );
    }

    @Test
    void whenOrderingByMultipleStaticFieldsThenEntitiesAreSortedAndOrderOfParamsIsKept()
    {
        params.setOrders( List.of( new OrderParam( "inactive", OrderParam.SortDirection.DESC ),
            new OrderParam( "enrolledAt", OrderParam.SortDirection.DESC ) ) );

        List<Long> list = teiStore.getTrackedEntityInstanceIds( params );
        list = reduceListValuesToStartCountingFromOne( list );

        assertEquals( "[2, 3, 4, 1]", list.toString() );
    }

    @Test
    void whenNoOrderParamsProvidedThenEntitiesSortedById()
    {
        params.setOrders( Collections.emptyList() );

        List<Long> list = teiStore.getTrackedEntityInstanceIds( params );
        list = reduceListValuesToStartCountingFromOne( list );

        assertEquals( "[1, 2, 3, 4]", list.toString() );
    }

    @Test
    void whenOrderingByNonStaticFieldThenEntitiesAreSortedByProvidedNonStaticField()
    {
        params.setAttributes( List.of( queryItem( "toUpdate000" ) ) );
        params.setOrders( List.of( new OrderParam( "toUpdate000", OrderParam.SortDirection.ASC ) ) );

        List<Long> list = teiStore.getTrackedEntityInstanceIds( params );
        list = reduceListValuesToStartCountingFromOne( list );

        assertEquals( "[1, 2, 3, 4]", list.toString() );
    }

    private static QueryItem queryItem( String teaUid )
    {
        return queryItem( teaUid, ValueType.TEXT );
    }

    private static QueryItem queryItem( String teaUid, ValueType valueType )
    {
        TrackedEntityAttribute at = new TrackedEntityAttribute();
        at.setUid( teaUid );
        at.setValueType( valueType );
        at.setAggregationType( AggregationType.NONE );
        return new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(),
            at.isUnique() );
    }

    /**
     * When running integration tests, we will not be aware of the tei id used,
     * this method receives the list of id's created and reduces them to start
     * counting at one
     *
     * @param list
     * @return list of ids starting from one
     */
    private List<Long> reduceListValuesToStartCountingFromOne( List<Long> list )
    {
        long minValue = list.stream().min( Comparator.naturalOrder() ).orElse( 0L );
        return list.stream().map( n -> n - minValue + 1 ).collect( Collectors.toList() );
    }
}
