package org.hisp.dhis.analytics.event.data.grid.postprocessor;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class OrgUnitColumnProcessorTest
{
    private OrgUnitColumnProcessor processor;

    @Mock
    private OrganisationUnitStore organisationUnitStore;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {

        processor = new OrgUnitColumnProcessor( organisationUnitStore );
    }

    @Test
    public void verifyOrgUnitUidIsReplacedWithName()
    {
        // Given
        List<GridHeader> headers = createGridHeaders( ValueType.INTEGER, ValueType.DATETIME, ValueType.BOOLEAN,
            ValueType.NUMBER, ValueType.ORGANISATION_UNIT );
        List<List<Object>> grid = generateRandomGridValues( headers, 10 );

        // When
        List<String> orgUnitUids = grid.stream().map( l -> (String) l.get( 4 ) ).collect( Collectors.toList() );

        final Map<String, String> uidToNameMap = generateUidOrgUnitNameMap( orgUnitUids );
        when( organisationUnitStore.getOrganisationUnitUidNameMap( argThat( t -> t.containsAll( orgUnitUids ) ) ) )
            .thenReturn( uidToNameMap );

        final List<List<Object>> aGrid = processor.process( headers, grid );

        // Then
        for ( int i = 0; i < aGrid.size(); i++ )
        {
            assertThat( aGrid.get( i ).get( 4 ), is( uidToNameMap.get( orgUnitUids.get( i ) ) ) );
        }
    }

    @Test
    public void verifyMultipleOrgUnitUidIsReplacedWithName()
    {
        // Given
        List<GridHeader> headers = createGridHeaders( ValueType.ORGANISATION_UNIT, ValueType.DATETIME,
            ValueType.BOOLEAN, ValueType.NUMBER, ValueType.ORGANISATION_UNIT, ValueType.INTEGER );

        List<List<Object>> grid = generateRandomGridValues( headers, 10 );

        // When
        List<String> orgUnitUids1 = grid.stream().map( l -> (String) l.get( 0 ) ).collect( Collectors.toList() );
        List<String> orgUnitUids2 = grid.stream().map( l -> (String) l.get( 4 ) ).collect( Collectors.toList() );
        List<String> merged = Lists.newArrayList( Iterables.concat( orgUnitUids1, orgUnitUids2 ) );
        final Map<String, String> uidToNameMap = generateUidOrgUnitNameMap( merged );

        when( organisationUnitStore.getOrganisationUnitUidNameMap( argThat( t -> t.containsAll( merged ) ) ) )
            .thenReturn( uidToNameMap );

        final List<List<Object>> aGrid = processor.process( headers, grid );

        // Then
        for ( int i = 0; i < aGrid.size(); i++ )
        {
            assertThat( aGrid.get( i ).get( 0 ), is( uidToNameMap.get( orgUnitUids1.get( i ) ) ) );
            assertThat( aGrid.get( i ).get( 4 ), is( uidToNameMap.get( orgUnitUids2.get( i ) ) ) );
        }
    }

    @Test
    public void verifyMultipleOrgUnitUidWithNullValuesIsReplacedWithName()
    {
        // Given
        List<GridHeader> headers = createGridHeaders( ValueType.ORGANISATION_UNIT, ValueType.DATETIME,
            ValueType.BOOLEAN, ValueType.NUMBER, ValueType.ORGANISATION_UNIT, ValueType.INTEGER );

        List<List<Object>> grid = generateRandomGridValues( headers, 10 );

        // make sure some values are null
        // set the index 0 to null every two rows
        for ( int i = 0; i < grid.size(); i += 2 )
        {
            grid.get( i ).set( 0, null );
        }

        // When
        List<String> orgUnitUids1 = grid.stream().map( l -> (String) l.get( 0 ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
        List<String> orgUnitUids2 = grid.stream().map( l -> (String) l.get( 4 ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
        List<String> merged = Lists.newArrayList( Iterables.concat( orgUnitUids1, orgUnitUids2 ) );
        final Map<String, String> uidToNameMap = generateUidOrgUnitNameMap( merged );

        when( organisationUnitStore.getOrganisationUnitUidNameMap( argThat( t -> t.containsAll( merged ) ) ) )
            .thenReturn( uidToNameMap );

        final List<List<Object>> aGrid = processor.process( headers, grid );

        // Then
        for ( int i = 0; i < aGrid.size(); i++ )
        {

            if ( i % 2 != 0 )
            {
                assertTrue( aGrid.get( i ).get( 0 ).toString().startsWith( "NAME_" ) );
            }
            else
            {
                assertThat( aGrid.get( i ).get( 0 ), is( nullValue() ) );
            }
            assertThat( aGrid.get( i ).get( 4 ), is( uidToNameMap.get( orgUnitUids2.get( i ) ) ) );
        }
    }

    @Test
    public void verifyGridDoesNotContainOrgUnitType()
    {
        // Given
        List<GridHeader> headers = createGridHeaders( ValueType.INTEGER, ValueType.DATETIME, ValueType.BOOLEAN,
            ValueType.NUMBER, ValueType.NUMBER );
        List<List<Object>> grid = generateRandomGridValues( headers, 10 );

        // When
        processor.process( headers, grid );

        verifyNoMoreInteractions( organisationUnitStore );
    }

    private GridHeader createGridHeader( ValueType valueType )
    {
        return new GridHeader( randomAlphabetic( 5 ), randomAlphabetic( 5 ), valueType, randomAlphabetic( 5 ),
            false,
            false );
    }

    private List<GridHeader> createGridHeaders( ValueType... valueTypes )
    {
        return Lists.newArrayList( valueTypes ).stream().map( this::createGridHeader ).collect( Collectors.toList() );
    }

    private List<List<Object>> generateRandomGridValues( List<GridHeader> headers, int size )
    {
        List<List<Object>> grid = new ArrayList<>();

        for ( int i = 0; i < size; i++ )
        {
            List<Object> row = new ArrayList<>( headers.size() );

            for ( GridHeader header : headers )
            {
                switch ( header.getValueType() )
                {
                case INTEGER:
                    row.add( RandomUtils.nextInt() );
                    break;
                case BOOLEAN:
                    row.add( true );
                    break;
                case NUMBER:
                    row.add( RandomUtils.nextDouble() );
                    break;
                case DATETIME:
                    row.add( randomTimestamp() );
                    break;
                case ORGANISATION_UNIT:
                    row.add( CodeGenerator.generateUid() );
                    break;
                }

            }
            grid.add( row );

        }

        return grid;
    }

    private long randomTimestamp()
    {
        long offset = Timestamp.valueOf( "2012-01-01 00:00:00" ).getTime();
        long end = Timestamp.valueOf( "2020-12-31 00:00:00" ).getTime();
        long diff = end - offset + 1;
        return (offset + (long) (Math.random() * diff));
    }

    private Map<String, String> generateUidOrgUnitNameMap( List<String> uids )
    {
        return uids.stream()
            .collect(
                Collectors.toMap( Function.identity(),
                    s -> "NAME_" + RandomStringUtils.randomAlphanumeric( 10 ) ) );
    }

}