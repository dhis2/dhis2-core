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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditControllerTest extends DhisControllerConvenienceTest
{

    private static final String DATA_ELEMENT_VALUE = "value";

    @Autowired
    private IdentifiableObjectManager manager;

    private OrganisationUnit orgUnit;

    private OrganisationUnit anotherOrgUnit;

    private Program program;

    private ProgramStage programStage;

    private User owner;

    private User user;

    private TrackedEntityType trackedEntityType;

    private TrackedEntity te1;

    private TrackedEntity te2;

    private Enrollment enrollment;

    private Event event1;

    private Event event2;

    @BeforeEach
    void setUp()
    {
        owner = makeUser( "owner" );

        orgUnit = createOrganisationUnit( 'A' );
        orgUnit.getSharing().setOwner( owner );
        manager.save( orgUnit, false );

        anotherOrgUnit = createOrganisationUnit( 'B' );
        anotherOrgUnit.getSharing().setOwner( owner );
        manager.save( anotherOrgUnit, false );

        user = createUserWithId( "tester", CodeGenerator.generateUid() );
        user.addOrganisationUnit( orgUnit );
        user.setTeiSearchOrganisationUnits( Set.of( orgUnit ) );
        this.userService.updateUser( user );

        program = createProgram( 'A' );
        program.addOrganisationUnit( orgUnit );
        program.getSharing().setOwner( owner );
        program.getSharing().addUserAccess( userAccess() );
        manager.save( program, false );

        programStage = createProgramStage( 'A', program );
        programStage.getSharing().setOwner( owner );
        programStage.getSharing().addUserAccess( userAccess() );
        manager.save( programStage, false );

        trackedEntityType = trackedEntityTypeAccessible();

        te1 = createTrackedEntity( orgUnit );
        te1.setTrackedEntityType( trackedEntityType );
        te1.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        te1.getSharing().setOwner( owner );
        manager.save( te1, false );

        te2 = createTrackedEntity( orgUnit );
        te2.setTrackedEntityType( trackedEntityType );
        te2.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        te2.getSharing().setOwner( owner );
        manager.save( te2, false );

        enrollment = createEnrollment( program, te1, te1.getOrganisationUnit() );
        manager.save( enrollment );

        event1 = createEvent( programStage, enrollment, enrollment.getOrganisationUnit() );
        manager.save( event1 );

        event2 = createEvent( programStage, enrollment, enrollment.getOrganisationUnit() );
        manager.save( event2 );
    }

    @Test
    void shouldFailGetDataValueAuditsWhenGivenPsiAndEventsParameters()
    {
        assertEquals(
            "Only one parameter of 'psi' (deprecated; semicolon separated UIDs) and 'events' (comma separated UIDs) must be specified. Prefer 'events' as 'psi' will be removed.",
            GET( "/audits/trackedEntityDataValue?psi={deprecatedEvents}&events={events}",
                event1.getUid() + ";" + event2.getUid(),
                event1.getUid() + "," + event2.getUid() )
                .error( HttpStatus.BAD_REQUEST )
                .getMessage() );
    }

    @Test
    void shouldSuccessToGetDataValueAuditsWhenPassingOnlyPsiParameter()
    {
        assertStatus( HttpStatus.OK,
            GET( "/audits/trackedEntityDataValue?psi={events}", event1.getUid() + ";" + event2.getUid() ) );
    }

    @Test
    void shouldSuccessToGetDataValueAuditsWhenPassingOnlyEventsParameter()
    {
        assertStatus( HttpStatus.OK,
            GET( "/audits/trackedEntityDataValue?events={events}", event1.getUid() + "," + event2.getUid() ) );
    }

    @Test
    void shouldFailGetAttributeValueAuditsWhenGivenTeiAndTrackedEntitiesParameters()
    {
        assertEquals(
            "Only one parameter of 'tei' (deprecated; semicolon separated UIDs) and 'trackedEntities' (comma separated UIDs) must be specified. Prefer 'trackedEntities' as 'tei' will be removed.",
            GET( "/audits/trackedEntityAttributeValue?tei={te}&trackedEntities={te}",
                te1.getUid() + ";" + te2.getUid(),
                te1.getUid() + "," + te2.getUid() )
                .error( HttpStatus.BAD_REQUEST )
                .getMessage() );
    }

    @Test
    void shouldSuccessToGetAttributeValueAuditsWhenPassingOnlyTeiParameter()
    {
        assertStatus( HttpStatus.OK,
            GET( "/audits/trackedEntityAttributeValue?tei={te}", te1.getUid() + ";" + te2.getUid() ) );
    }

    @Test
    void shouldSuccessToGetAttributeValueAuditsWhenPassingOnlyTrackedEntitiesParameter()
    {
        assertStatus( HttpStatus.OK,
            GET( "/audits/trackedEntityAttributeValue?trackedEntities={te}", te1.getUid() + "," + te2.getUid() ) );
    }

    private TrackedEntityType trackedEntityTypeAccessible()
    {
        TrackedEntityType type = createTrackedEntityType( 'A' );
        type.getSharing().addUserAccess( userAccess() );
        manager.save( type, false );
        return type;
    }

    private UserAccess userAccess()
    {
        UserAccess a = new UserAccess();
        a.setUser( user );
        a.setAccess( AccessStringHelper.FULL );
        return a;
    }
}
