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
package org.hisp.dhis.tracker.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Enrico Colasante
 */
class RelationshipTrackerConverterServiceTest extends DhisSpringTest
{

    private final static String TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE = "xLmPUYJX8Ks";

    private final static String TEI_TO_EVENT_RELATIONSHIP_TYPE = "TV9oB9LT3sh";

    private final static String TEI = "IOR1AXXl24H";

    private final static String ENROLLMENT = "TvctPPhpD8u";

    private final static String EVENT = "D9PbzJY8bJO";

    @Autowired
    @Qualifier( "relationshipTrackerConverterService" )
    private TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipConverterService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    private TrackerBundle trackerBundle;

    @Override
    protected void setUpTest()
        throws IOException
    {
        userService = _userService;
        preCreateInjectAdminUser();
        dbmsManager.clearSession();

        renderService = _renderService;
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );
        Program program = createProgram( 'A' );
        programService.addProgram( program );
        ProgramStage programStage = createProgramStage( 'A', program );
        programStageService.saveProgramStage( programStage );
        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( 'A', organisationUnit,
            trackedEntityAttribute );
        trackedEntityInstance.setUid( TEI );
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setProgram( program );
        programInstance.setUid( ENROLLMENT );
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( EVENT );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setProgramStage( programStage );
        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstance );
        programInstanceService.addProgramInstance( programInstance );
        programStageInstanceService.addProgramStageInstance( programStageInstance );
        RelationshipType relationshipTypeA = createTeiToEnrollmentRelationshipType( 'A', program, trackedEntityType,
            false );
        relationshipTypeA.setUid( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE );
        RelationshipType relationshipTypeB = createTeiToEventRelationshipType( 'B', program, trackedEntityType, false );
        relationshipTypeB.setUid( TEI_TO_EVENT_RELATIONSHIP_TYPE );
        relationshipTypeService.addRelationshipType( relationshipTypeA );
        relationshipTypeService.addRelationshipType( relationshipTypeB );
        TrackerImportParams trackerImportParams = renderService.fromJson(
            new ClassPathResource( "tracker/relationships.json" ).getInputStream(), TrackerImportParams.class );
        User adminUser = createAndInjectAdminUser();
        trackerImportParams.setUser( adminUser );
        trackerBundle = trackerBundleService.create( trackerImportParams );
    }

    @Test
    void testConverterFromRelationships()
    {
        List<org.hisp.dhis.relationship.Relationship> from = relationshipConverterService
            .from( trackerBundle.getPreheat(), trackerBundle.getRelationships() );
        assertNotNull( from );
        assertEquals( 2, from.size() );
        from.forEach( relationship -> {
            if ( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getUid() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntityInstance().getUid() );
                assertEquals( ENROLLMENT, relationship.getTo().getProgramInstance().getUid() );
            }
            else if ( TEI_TO_EVENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getUid() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntityInstance().getUid() );
                assertEquals( EVENT, relationship.getTo().getProgramStageInstance().getUid() );
            }
            else
            {
                fail( "Unexpected relationshipType found." );
            }
            assertNotNull( relationship.getFrom() );
            assertNotNull( relationship.getTo() );
        } );
    }

    @Test
    void testConverterToRelationships()
    {
        List<org.hisp.dhis.relationship.Relationship> from = relationshipConverterService
            .from( trackerBundle.getPreheat(), trackerBundle.getRelationships() );
        List<Relationship> to = relationshipConverterService.to( from );
        assertNotNull( to );
        assertEquals( 2, to.size() );
        from.forEach( relationship -> {
            if ( TEI_TO_ENROLLMENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getUid() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntityInstance().getUid() );
                assertEquals( ENROLLMENT, relationship.getTo().getProgramInstance().getUid() );
            }
            else if ( TEI_TO_EVENT_RELATIONSHIP_TYPE.equals( relationship.getRelationshipType().getUid() ) )
            {
                assertEquals( TEI, relationship.getFrom().getTrackedEntityInstance().getUid() );
                assertEquals( EVENT, relationship.getTo().getProgramStageInstance().getUid() );
            }
            else
            {
                fail( "Unexpected relationshipType found." );
            }
            assertNotNull( relationship.getFrom() );
            assertNotNull( relationship.getTo() );
        } );
    }
}
