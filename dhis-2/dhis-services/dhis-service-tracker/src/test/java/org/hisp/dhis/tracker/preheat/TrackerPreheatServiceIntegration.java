package org.hisp.dhis.tracker.preheat;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

public class TrackerPreheatServiceIntegration
    extends IntegrationTestBase
{
    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Autowired
    private TrackerPreheatService trackerPreheatService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private AttributeService attributeService;

    private final String TET_UID = "TET12345678";

    private final String TE_UID = "TE123456789";

    private final String ATTRIBUTE_UID = "ATTR1234567";

    @Autowired
    private UserService _userService;

    @Override
    public void setUpTest()
        throws Exception
    {
        userService = _userService;

        // Set up placeholder OU; We add Code for testing idScheme.
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        ouA.setCode( "OUA" );
        organisationUnitService.addOrganisationUnit( ouA );

        // Set up placeholder TET
        TrackedEntityType tetA = createTrackedEntityType( 'A' );
        tetA.setUid( TET_UID );
        trackedEntityTypeService.addTrackedEntityType( tetA );

        // Set up attribute for program, to be used for testing idScheme.
        Attribute attributeA = createAttribute( 'A' );
        attributeA.setUid( ATTRIBUTE_UID );
        attributeA.setUnique( true );
        attributeA.setProgramAttribute( true );
        attributeService.addAttribute( attributeA );

        // Set up placeholder Program, with attributeValue
        Program programA = createProgram( 'A' );
        programA.addOrganisationUnit( ouA );
        programA.setTrackedEntityType( tetA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        programA.setAttributeValues( Sets.newHashSet( new AttributeValue( "PROGRAM1", attributeA ) ) );
        programService.addProgram( programA );
    }

    @Test
    public void testPreheatWithDifferentIdSchemes()
    {
        TrackedEntity teA = TrackedEntity.builder()
            .orgUnit( "OUA" )
            .trackedEntityType( TET_UID )
            .build();

        Enrollment enrollmentA = Enrollment.builder()
            .orgUnit( "OUA" )
            .program( "PROGRAM1" )
            .trackedEntity( TE_UID )
            .build();

        TrackerPreheatParams trackerPreheatParams = TrackerPreheatParams.builder()
            .trackedEntities( Lists.newArrayList( teA ) )
            .enrollments( Lists.newArrayList( enrollmentA ) )
            .identifiers( TrackerIdentifierParams.builder()
                .idScheme( TrackerIdentifier.UID )
                .orgUnitIdScheme( TrackerIdentifier.CODE )
                .programIdScheme(
                    TrackerIdentifier.builder().idScheme( TrackerIdScheme.ATTRIBUTE ).value( ATTRIBUTE_UID ).build() )
                .build() )
            .build();

        trackerPreheatService.validate( trackerPreheatParams );

        TrackerPreheat preheat = trackerPreheatService.preheat( trackerPreheatParams );

        assertNotNull( preheat );
        assertNotNull( preheat.getMap() );
        assertNotNull( preheat.getMap().get( TrackerIdScheme.UID ) );
        assertNotNull( preheat.getMap().get( TrackerIdScheme.CODE ).get( OrganisationUnit.class ) );
        assertNotNull( preheat.getMap().get( TrackerIdScheme.ATTRIBUTE ).get( Program.class ) );
    }
}
