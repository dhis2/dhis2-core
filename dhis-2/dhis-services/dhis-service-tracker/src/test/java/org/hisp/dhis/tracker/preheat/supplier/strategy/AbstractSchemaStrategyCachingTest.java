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
package org.hisp.dhis.tracker.preheat.supplier.strategy;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.tracker.TrackerIdentifierCollector.ID_WILDCARD;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.descriptors.ProgramSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.RelationshipTypeSchemaDescriptor;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.cache.PreheatCacheService;
import org.hisp.dhis.tracker.preheat.mappers.CopyMapper;
import org.hisp.dhis.tracker.preheat.mappers.ProgramMapper;
import org.hisp.dhis.tracker.preheat.mappers.RelationshipTypeMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class AbstractSchemaStrategyCachingTest
{
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private PreheatCacheService cache;

    @Mock
    private IdentifiableObjectManager manager;

    @Mock
    private QueryService queryService;

    @Mock
    private SchemaService schemaService;

    private TrackerPreheat preheat;

    private BeanRandomizer rnd;

    @Before
    public void setUp()
    {
        preheat = new TrackerPreheat();
        rnd = new BeanRandomizer();
    }

    @Test
    public void verifyFetchWildcardObjectsFromDbAndPutInCache()
    {
        // Given
        final Schema schema = new RelationshipTypeSchemaDescriptor().getSchema();

        when( manager.getAll( (Class<IdentifiableObject>) schema.getKlass() ) )
            .thenReturn( (List<IdentifiableObject>) rnd.randomObjects( schema.getKlass(), 5 ) );

        RelationshipTypeStrategy strategy = new RelationshipTypeStrategy( schemaService, queryService,
            manager, cache );

        // When
        strategy.queryForIdentifiableObjects( preheat, schema, TrackerIdentifier.UID,
            singletonList( singletonList( ID_WILDCARD ) ), RelationshipTypeMapper.class );

        // Then
        assertThat( preheat.getAll( RelationshipType.class ), hasSize( 5 ) );

        verify( cache, times( 1 ) ).hasKey( "RelationshipType" );

        verify( cache, times( 5 ) ).put( eq( "RelationshipType" ), anyString(), any(), eq( 10 ), eq( 10L ) );
    }

    @Test
    public void verifyObjectInCacheIsReturned()
    {
        // Given
        final Schema schema = new ProgramSchemaDescriptor().getSchema();

        String UID = CodeGenerator.generateUid();

        Program program = rnd.randomObject( Program.class );
        when( cache.get( Program.class.getSimpleName(), UID ) ).thenReturn( Optional.of( program ) );

        ProgramStrategy strategy = new ProgramStrategy( schemaService, queryService,
            manager, cache );

        // When
        strategy.queryForIdentifiableObjects( preheat, schema, TrackerIdentifier.UID,
            singletonList( singletonList( UID ) ), ProgramMapper.INSTANCE.getClass() );

        // Then
        assertThat( preheat.getAll( Program.class ), hasSize( 1 ) );
    }

    @Test
    public void verifyObjectNotInCacheIsFetchedFromDbAndPutInCache()
    {
        // Given
        final Schema schema = new ProgramSchemaDescriptor().getSchema();

        String UID = CodeGenerator.generateUid();

        Program program = rnd.randomObject( Program.class );

        when( cache.get( Program.class.getSimpleName(), UID ) ).thenReturn( Optional.empty() );

        doReturn( singletonList( program ) ).when( queryService ).query( any( Query.class ) );
        ProgramStrategy strategy = new ProgramStrategy( schemaService, queryService,
            manager, cache );

        // When
        strategy.queryForIdentifiableObjects( preheat, schema, TrackerIdentifier.UID,
            singletonList( singletonList( UID ) ), CopyMapper.class );

        // Then
        assertThat( preheat.getAll( Program.class ), hasSize( 1 ) );

        verify( cache, times( 1 ) ).put( eq( "Program" ), anyString(), any(), eq( 20 ), eq( 10L ) );
    }

}