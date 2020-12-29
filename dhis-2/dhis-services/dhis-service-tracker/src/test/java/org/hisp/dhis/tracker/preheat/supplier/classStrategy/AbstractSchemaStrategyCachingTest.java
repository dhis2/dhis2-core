package org.hisp.dhis.tracker.preheat.supplier.classStrategy;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.tracker.TrackerIdentifierCollector.ID_WILDCARD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            singletonList( singletonList( ID_WILDCARD ) ), RelationshipTypeMapper.INSTANCE.getClass() );

        // Then
        assertThat( preheat.getAll( RelationshipType.class ), hasSize( 5 ) );

        verify( cache, times( 1 ) ).hasKey( "RelationshipType" );

        verify( cache, times( 5 ) ).put( eq( "RelationshipType" ), anyString(), any(), eq( 10 ), eq( Long.MAX_VALUE ) );
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

        verify( cache, times( 1 ) ).put( eq( "Program" ), anyString(), any(), eq( 20 ), eq( Long.MAX_VALUE ) );
    }

}