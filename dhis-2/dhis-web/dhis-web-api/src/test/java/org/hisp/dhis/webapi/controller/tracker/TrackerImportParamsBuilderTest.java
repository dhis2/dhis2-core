package org.hisp.dhis.webapi.controller.tracker;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.ATOMIC_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.FLUSH_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.IMPORT_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.IMPORT_STRATEGY_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.VALIDATION_MODE_KEY;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;
import org.junit.Test;

/**
 * @author Luciano Fiandesio
 */
public class TrackerImportParamsBuilderTest
{
    private Map<String, List<String>> paramMap = new HashMap<>();

    @Test
    public void testDefaultParams()
    {
        final TrackerImportParams params = TrackerImportParamsBuilder.build( null );
        assertDefaultParams( params );
    }

    @Test
    public void testValidationMode()
    {
        Arrays.stream( ValidationMode.values() ).forEach( e -> {
            paramMap.put( VALIDATION_MODE_KEY.getKey(), Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getValidationMode(), is( e ) );
        } );
    }

    @Test
    public void testImportMode()
    {
        Arrays.stream( TrackerBundleMode.values() ).forEach( e -> {
            paramMap.put( IMPORT_MODE_KEY.getKey(), Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getImportMode(), is( e ) );
        } );
    }

    @Test
    public void testAtomicMode()
    {
        Arrays.stream( AtomicMode.values() ).forEach( e -> {
            paramMap.put( ATOMIC_MODE_KEY.getKey(), Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getAtomicMode(), is( e ) );
        } );
    }

    @Test
    public void testFlushMode()
    {
        Arrays.stream( FlushMode.values() ).forEach( e -> {
            paramMap.put( FLUSH_MODE_KEY.getKey(), Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getFlushMode(), is( e ) );
        } );
    }

    @Test
    public void testImportStrategy()
    {
        Arrays.stream( TrackerImportStrategy.values() ).forEach( e -> {
            paramMap.put( IMPORT_STRATEGY_KEY.getKey(), Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getImportStrategy(), is( e ) );
        } );
    }

    @Test
    public void testOrgUnitIdentifier()
    {
        Arrays.stream( TrackerIdScheme.values() ).forEach( e -> {
            paramMap.put( "orgUnitIdScheme", Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getIdentifiers().getOrgUnitIdScheme().getIdScheme(), is( e ) );
        } );
    }

    @Test
    public void testProgramIdentifier()
    {
        Arrays.stream( TrackerIdScheme.values() ).forEach( e -> {
            paramMap.put( "programIdScheme", Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getIdentifiers().getProgramIdScheme().getIdScheme(), is( e ) );
        } );
    }

    @Test
    public void testProgramStageIdentifier()
    {
        Arrays.stream( TrackerIdScheme.values() ).forEach( e -> {
            paramMap.put( "programStageIdScheme", Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getIdentifiers().getProgramStageIdScheme().getIdScheme(), is( e ) );
        } );
    }

    @Test
    public void testDataElementIdentifier()
    {
        Arrays.stream( TrackerIdScheme.values() ).forEach( e -> {
            paramMap.put( "dataElementIdScheme", Collections.singletonList( e.name() ) );
            TrackerImportParams params = TrackerImportParamsBuilder.build( paramMap );
            assertThat( params.getIdentifiers().getDataElementIdScheme().getIdScheme(), is( e ) );
        } );
    }

    private void assertDefaultParams( TrackerImportParams params )
    {
        assertThat( params.getValidationMode(), is( ValidationMode.FULL ) );
        assertThat( params.getImportMode(), is( TrackerBundleMode.COMMIT ) );
        assertThat( params.getImportStrategy(), is( TrackerImportStrategy.CREATE_AND_UPDATE ) );
        assertThat( params.getAtomicMode(), is( AtomicMode.ALL ) );
        assertThat( params.getFlushMode(), is( FlushMode.AUTO ) );

        TrackerIdentifierParams identifiers = params.getIdentifiers();
        assertThat( identifiers.getOrgUnitIdScheme(), is( TrackerIdentifier.UID ) );
        assertThat( identifiers.getProgramIdScheme(), is( TrackerIdentifier.UID ) );
        assertThat( identifiers.getCategoryOptionComboIdScheme(), is( TrackerIdentifier.UID ) );
        assertThat( identifiers.getCategoryOptionIdScheme(), is( TrackerIdentifier.UID ) );
        assertThat( identifiers.getDataElementIdScheme(), is( TrackerIdentifier.UID ) );
        assertThat( identifiers.getProgramStageIdScheme(), is( TrackerIdentifier.UID ) );
        assertThat( identifiers.getIdScheme(), is( TrackerIdentifier.UID ) );
    }

}