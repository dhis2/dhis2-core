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
package org.hisp.dhis.analytics.common;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamType.DIMENSIONS;
import static org.hisp.dhis.analytics.common.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.hisp.dhis.analytics.common.query.Field.ofUnquoted;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.rowset.RowSetMetaDataImpl;

import org.apache.commons.collections4.MapUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.processing.HeaderParamsHandler;
import org.hisp.dhis.analytics.common.processing.MetadataParamsHandler;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Tests for {@link GridAdaptor}.
 *
 * @author maikel arabori
 */
@ExtendWith( MockitoExtension.class )
class GridAdaptorTest extends DhisConvenienceTest
{
    private GridAdaptor gridAdaptor;

    private HeaderParamsHandler headerParamsHandler;

    private MetadataParamsHandler metadataDetailsHandler;

    private User user;

    @Mock
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp()
    {
        headerParamsHandler = new HeaderParamsHandler();
        metadataDetailsHandler = new MetadataParamsHandler();
        gridAdaptor = new GridAdaptor( headerParamsHandler, metadataDetailsHandler, currentUserService );
        user = makeUser( ADMIN_USER_UID );
    }

    @Test
    void testCreateGridWithField()
        throws SQLException
    {
        // Given
        ResultSet resultSet = mock( ResultSet.class );

        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount( 2 );
        metaData.setColumnName( 1, "anyFakeCol-1" );
        metaData.setColumnName( 2, "anyFakeCol-2" );

        TeiQueryParams teiQueryParams = TeiQueryParams.builder().trackedEntityType( stubTrackedEntityType() )
            .commonParams( stubCommonParams() ).build();

        List<Field> fields = List.of( ofUnquoted( "ev", null, "oucode" ) );

        when( resultSet.next() ).thenReturn( true ).thenReturn( true ).thenReturn( true ).thenReturn( false );
        when( resultSet.getMetaData() ).thenReturn( metaData );
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        SqlRowSet sqlRowSet = new ResultSetWrappingSqlRowSet( resultSet );
        SqlQueryResult mockSqlResult = new SqlQueryResult( sqlRowSet );
        long anyCount = 0;

        // When
        Grid grid = gridAdaptor.createGrid( Optional.of( mockSqlResult ), anyCount, teiQueryParams, fields );

        // Then
        assertNotNull( grid, "Should not be null: grid" );
        assertFalse( grid.getHeaders().isEmpty(), "Should not be empty: headers" );
        assertFalse( grid.getRows().isEmpty(), "Should not be empty: rows" );
        assertEquals( 1, grid.getHeaders().size(), "Should have size of 1: headers" );
        assertEquals( 3, grid.getRows().size(), "Should have size of 3: rows" );
    }

    @Test
    void testCreateGridWithEmptyField()
        throws SQLException
    {
        // Given
        ResultSet resultSet = mock( ResultSet.class );

        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount( 2 );
        metaData.setColumnName( 1, "anyFakeCol-1" );
        metaData.setColumnName( 2, "anyFakeCol-2" );

        TeiQueryParams teiQueryParams = TeiQueryParams.builder().trackedEntityType( stubTrackedEntityType() )
            .commonParams( stubCommonParams() ).build();

        List<Field> fields = emptyList();

        when( resultSet.next() ).thenReturn( true ).thenReturn( true ).thenReturn( true ).thenReturn( false );
        when( resultSet.getMetaData() ).thenReturn( metaData );
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        SqlRowSet sqlRowSet = new ResultSetWrappingSqlRowSet( resultSet );
        SqlQueryResult mockSqlResult = new SqlQueryResult( sqlRowSet );
        long anyCount = 0;

        // When
        Grid grid = gridAdaptor.createGrid( Optional.of( mockSqlResult ), anyCount, teiQueryParams, fields );

        // Then
        assertNotNull( grid, "Should not be null: grid" );
        assertTrue( grid.getHeaders().isEmpty(), "Should be empty: headers" );
        assertFalse( grid.getRows().isEmpty(), "Should not be empty: rows" );
        assertEquals( 3, grid.getRows().size(), "Should have size of 3: rows" );
        assertTrue( MapUtils.isNotEmpty( grid.getMetaData() ) );
    }

    @Test
    void testCreateGridWithEmptySqlResult()
    {
        // Given
        Optional<SqlQueryResult> emptySqlResult = Optional.empty();

        TeiQueryParams teiQueryParams = TeiQueryParams.builder().trackedEntityType( stubTrackedEntityType() )
            .commonParams( stubCommonParams() ).build();

        List<Field> fields = List.of( ofUnquoted( "ev", null, "oucode" ) );

        long anyCount = 0;

        when( currentUserService.getCurrentUser() ).thenReturn( user );

        // When
        Grid grid = gridAdaptor.createGrid( emptySqlResult, anyCount, teiQueryParams, fields );

        // Then
        assertTrue( isNotEmpty( grid.getHeaders() ) );
        assertTrue( MapUtils.isNotEmpty( grid.getMetaData() ) );
        assertTrue( isEmpty( grid.getRows() ) );
    }

    @Test
    void testCreateGridWithNullTeiQueryParams()
    {
        // Given
        Optional<SqlQueryResult> anySqlResult = Optional.empty();
        TeiQueryParams nullTeiQueryParams = null;
        long anyCount = 0;

        // When
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> gridAdaptor.createGrid( anySqlResult, anyCount, nullTeiQueryParams, null ),
            "Expected exception not thrown: createGrid()" );

        // Then
        assertTrue( ex.getMessage().contains( "The 'teiQueryParams' must not be null" ) );
    }

    private TrackedEntityType stubTrackedEntityType()
    {
        TrackedEntityTypeAttribute tetaA = createTrackedEntityTypeAttribute( 'A', TEXT );
        tetaA.setUid( "tetaA-uid" );
        tetaA.getTrackedEntityAttribute().setUid( "teaA-uid" );

        TrackedEntityTypeAttribute tetaB = createTrackedEntityTypeAttribute( 'B', TEXT );
        tetaB.setUid( "tetaB-uid" );
        tetaB.getTrackedEntityAttribute().setUid( "teaB-uid" );

        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( "tet-uid" );
        trackedEntityType.setTrackedEntityTypeAttributes( List.of( tetaA, tetaB ) );

        return trackedEntityType;
    }

    private CommonParams stubCommonParams()
    {
        List<String> ous = List.of( "ou1-uid", "ou2-uid" );

        DimensionIdentifier<DimensionParam> dimensionIdentifierA = stubDimensionIdentifier(
            ous, "Z8z5uu61HAb", "tO8L1aBitDm", "teaA-uid" );

        DimensionIdentifier<DimensionParam> dimensionIdentifierB = stubDimensionIdentifier(
            ous, "Z8z5uu61HAb", "tO8L1aBitDm", "teaB-uid" );

        List<DimensionIdentifier<DimensionParam>> dimIdentifiers = new ArrayList<>();
        dimIdentifiers.add( dimensionIdentifierA );
        dimIdentifiers.add( dimensionIdentifierB );

        return CommonParams.builder().programs( List.of( createProgram( 'A' ) ) )
            .dimensionIdentifiers( dimIdentifiers )
            .build();
    }

    private DimensionIdentifier<DimensionParam> stubDimensionIdentifier( List<String> ous,
        String programUid, String programStageUid, String dimensionUid )
    {
        BaseDimensionalObject tea = new BaseDimensionalObject( dimensionUid, DATA_X,
            ous.stream()
                .map( item -> new BaseDimensionalItemObject( item ) )
                .collect( Collectors.toList() ),
            TEXT );

        DimensionParam dimensionParam = DimensionParam.ofObject( tea, DIMENSIONS, ous );

        ElementWithOffset program = emptyElementWithOffset();
        ElementWithOffset programStage = emptyElementWithOffset();

        if ( isNotBlank( programUid ) )
        {
            Program p = new Program();
            p.setUid( programUid );
            program = ElementWithOffset.of( p, null );
        }

        if ( isNotBlank( programStageUid ) )
        {
            ProgramStage ps = new ProgramStage();
            ps.setUid( programStageUid );
            programStage = ElementWithOffset.of( ps, null );
        }

        return DimensionIdentifier.of( program, programStage, dimensionParam );
    }
}
