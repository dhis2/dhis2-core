package org.hisp.dhis.dxf2.events.aggregates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.TrackerTest;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityInstanceAttributesAggregateTest extends TrackerTest
{
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

    @Autowired
    private ProgramService programService;

    private Program programB;

    private final static int A = 65;
    private final static int C = 67;
    private final static int D = 68;
    private final static int F = 70;

    @Override
    protected void mockCurrentUserService()
    {
        User user = createUser( "testUser" );

        makeUserSuper( user );

        currentUserService = new MockCurrentUserService( user );

        ReflectionTestUtils.setField( trackedEntityInstanceAggregate, "currentUserService", currentUserService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "currentUserService", currentUserService );
        ReflectionTestUtils.setField( teiService, "currentUserService", currentUserService );

    }

    @Before
    public void setUp()
    {
        ReflectionTestUtils.setField( trackedEntityInstanceAggregate, "currentUserService", currentUserService );
    }

    @Test
    public void testTrackedEntityInstanceAttributes()
    {
        doInTransaction( () -> {

            ProgramStage programStageA = createProgramStage( programB );
            ProgramStage programStageB = createProgramStage( programB );

            // Create 5 Tracked Entity Attributes (named A .. E)
            IntStream.range( A, F ).mapToObj( i -> Character.toString( (char) i ) ).forEach( c -> attributeService
                    .addTrackedEntityAttribute( createTrackedEntityAttribute( c.charAt( 0 ), ValueType.TEXT ) ) );

            // Transform the Tracked Entity Attributes into a List of
            // TrackedEntityTypeAttribute
            List<TrackedEntityTypeAttribute> teatList = IntStream.range( A, C )
                    .mapToObj( i -> Character.toString( (char) i ) )
                    .map( s -> new TrackedEntityTypeAttribute( trackedEntityTypeA,
                            attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
                    .collect( Collectors.toList() );

            // Assign 2 (A, B) TrackedEntityTypeAttribute to Tracked Entity Type A
            trackedEntityTypeA.setTrackedEntityTypeAttributes( teatList );
            trackedEntityTypeService.updateTrackedEntityType( trackedEntityTypeA );

            programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
            programB.setProgramType( ProgramType.WITH_REGISTRATION );
            programB.setCategoryCombo( categoryComboA );
            programB.setUid( CodeGenerator.generateUid() );
            programB.setCode( RandomStringUtils.randomAlphanumeric( 10 ) );
            programB.setProgramStages(
                    Stream.of( programStageA, programStageB ).collect( Collectors.toCollection( HashSet::new ) ) );
            programService.addProgram( programB );

            // Assign ProgramTrackedEntityAttribute C, D, E to program B
            List<ProgramTrackedEntityAttribute> pteaList = IntStream.range( C,  D )
                    .mapToObj( i -> Character.toString( (char) i ) ).map( s -> new ProgramTrackedEntityAttribute( programB,
                            attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
                    .collect( Collectors.toList() );

            programB.setProgramAttributes( pteaList );
            programService.updateProgram( programB );

            // make sure the current user does not have access to program B

            // Create a TEI associated to program B
            final org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = persistTrackedEntityInstance(
                ImmutableMap.of( "program", programB ) );

            // Assign Attribute A,B,E to Tracked Entity Instance
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeService.getTrackedEntityAttributeByName("AttributeA"), trackedEntityInstance, "A" ) );
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeService.getTrackedEntityAttributeByName("AttributeB"), trackedEntityInstance, "B" ) );
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeService.getTrackedEntityAttributeByName("AttributeE"), trackedEntityInstance, "E" ) );
        } );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertThat( trackedEntityInstances.get( 0 ).getAttributes(), hasSize( 2 ) );

        final List<Attribute> attributes = trackedEntityInstances.get( 0 ).getAttributes();
        Attribute attributeA = findByValue( attributes, "A" );
        assertThat( attributeA.getAttribute(),
            is( attributeService.getTrackedEntityAttributeByName( "AttributeA" ).getUid() ) );
        Attribute attributeB = findByValue( attributes, "B" );

        assertThat( attributeB.getAttribute(),
                is( attributeService.getTrackedEntityAttributeByName( "AttributeB" ).getUid() ) );
    }

    private Attribute findByValue(List<Attribute> attributes, String val) {

        return attributes.stream().filter( a -> a.getValue().equals( val ) ).findFirst()
            .orElseThrow( () -> new NullPointerException( "Attribute not found!" ) );

    }
}
