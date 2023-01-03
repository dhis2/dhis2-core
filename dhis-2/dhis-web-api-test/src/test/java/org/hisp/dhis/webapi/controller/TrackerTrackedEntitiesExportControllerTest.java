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

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertHasOnlyMembers;
import static org.hisp.dhis.webapi.controller.TrackerControllerAssertions.assertRelationship;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackerTrackedEntitiesExportControllerTest extends DhisControllerConvenienceTest
{
    private static final String TEA_UID = "TvjwTPToKHO";

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ProgramInstanceService programInstanceService;

    private OrganisationUnit orgUnit;

    private OrganisationUnit anotherOrgUnit;

    private Program program;

    private TrackedEntityType trackedEntityType;

    private User owner;

    private User user;

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

        TrackedEntityAttribute attr = createTrackedEntityAttribute( 'A' );
        attr.setUid( TEA_UID );
        manager.save( attr, false );

        ProgramStage programStage = createProgramStage( 'A', program );
        programStage.getSharing().setOwner( owner );
        programStage.getSharing().addUserAccess( userAccess() );
        manager.save( programStage, false );

        trackedEntityType = trackedEntityTypeAccessible();
    }

    @Test
    void getTrackedEntitiesNeedsProgramOrType()
    {
        assertEquals( "Either Program or Tracked entity type should be specified",
            GET( "/tracker/trackedEntities" )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    void getTrackedEntitiesNeedsProgramOrTrackedEntityType()
    {
        this.switchContextToUser( user );

        assertEquals( "Either Program or Tracked entity type should be specified",
            GET( "/tracker/trackedEntities?orgUnit={ou}", orgUnit.getUid() )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    void getTrackedEntitiesNeedsAtLeastOneOrgUnit()
    {
        assertEquals( "At least one organisation unit must be specified",
            GET( "/tracker/trackedEntities?program={program}", program.getUid() )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    void getTrackedEntitiesCannotHaveRepeatedAttributes()
    {
        assertContains( "Filter for attribute " + TEA_UID + " was specified more than once.",
            GET( "/tracker/trackedEntities?filter=" + TEA_UID + ":eq:test," + TEA_UID + ":gt:test2" )
                .error( HttpStatus.CONFLICT )
                .getMessage() );
    }

    @Test
    void getTrackedEntityById()
    {
        TrackedEntityInstance tei = trackedEntityInstance();
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities/{id}", tei.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertEquals( tei.getUid(), json.getString( "trackedEntity" ).string() );
        assertEquals( tei.getTrackedEntityType().getUid(), json.getString( "trackedEntityType" ).string() );
        assertEquals( tei.getOrganisationUnit().getUid(), json.getString( "orgUnit" ).string() );
        assertHasMember( json, "createdAt" );
        assertHasMember( json, "createdAtClient" );
        assertHasMember( json, "updatedAtClient" );
        assertHasNoMember( json, "relationships" );
        assertHasNoMember( json, "enrollments" );
        assertHasNoMember( json, "events" );
        assertHasNoMember( json, "programOwners" );
    }

    @Test
    void getTrackedEntityByIdWithFields()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstance();
        relationship( from, to );
        this.switchContextToUser( user );

        JsonObject json = GET(
            "/tracker/trackedEntities/{id}?fields=trackedEntity,orgUnit,relationships[relationship,relationshipType]",
            from.getUid() )
                .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertHasOnlyMembers( json, "trackedEntity", "orgUnit", "relationships" );
        assertHasOnlyMembers( json.getArray( "relationships" ).getObject( 0 ), "relationship", "relationshipType" );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationships()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstance();
        Relationship r = relationship( from, to );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK );

        JsonArray rels = json.getArray( "relationships" );
        assertFalse( rels.isEmpty(), "relationships are returned if `fields` contains relationships" );
        assertEquals( 1, rels.size() );
        assertRelationship( r, rels.getObject( 0 ) );
        assertTrackedEntityWithinRelationship( from, rels.getObject( 0 ).getObject( "from" ) );
        assertTrackedEntityWithinRelationship( to, rels.getObject( 0 ).getObject( "to" ) );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipType()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstance();
        relationship( relationshipTypeNotAccessible(), from, to );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertTrue( json.getArray( "relationships" ).isEmpty(),
            "user needs access to relationship type to access the relationship" );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemTo()
    {
        TrackedEntityInstance from = trackedEntityInstance();
        TrackedEntityInstance to = trackedEntityInstanceNotInSearchScope();
        relationship( from, to );
        this.switchContextToUser( user );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .content( HttpStatus.OK );

        assertFalse( json.isEmpty() );
        assertTrue( json.getArray( "relationships" ).isEmpty(),
            "user needs access to from and to items to access the relationship" );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToBothRelationshipItems()
    {
        TrackedEntityInstance from = trackedEntityInstanceNotInSearchScope();
        TrackedEntityInstance to = trackedEntityInstanceNotInSearchScope();
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.CONFLICT ).getMessage()
            .contains( "User has no read access to organisation unit" ) );
    }

    @Test
    void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemFrom()
    {
        TrackedEntityInstance from = trackedEntityInstanceNotInSearchScope();
        TrackedEntityInstance to = trackedEntityInstance();
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.CONFLICT ).getMessage()
            .contains( "User has no read access to organisation unit" ) );
    }

    @Test
    void getTrackedEntityByIdyWithFieldsRelationshipsNoAccessToTrackedEntityType()
    {
        TrackedEntityType type = trackedEntityTypeNotAccessible();
        TrackedEntityInstance from = trackedEntityInstance( type );
        TrackedEntityInstance to = trackedEntityInstance( type );
        relationship( from, to );
        this.switchContextToUser( user );

        assertTrue( GET( "/tracker/trackedEntities/{id}?fields=relationships", from.getUid() )
            .error( HttpStatus.CONFLICT ).getMessage()
            .contains( "User has no data read access to tracked entity" ) );
    }

    @Test
    void getTrackedEntityByIdNotFound()
    {
        assertEquals( "TrackedEntityInstance not found for uid: Hq3Kc6HK4OZ",
            GET( "/tracker/trackedEntities/Hq3Kc6HK4OZ" )
                .error( HttpStatus.NOT_FOUND )
                .getMessage() );
    }

    @Test
    void getTrackedEntityReturnsCsvFormat()
    {
        WebClient.HttpResponse response = GET( "/tracker/trackedEntities.csv?program={programId}&orgUnit={orgUnitId}",
            program.getUid(), orgUnit.getUid() );

        assertEquals( HttpStatus.OK, response.status() );

        assertAll( () -> response.header( "content-type" ).contains( ContextUtils.CONTENT_TYPE_TEXT_CSV ),
            () -> response.header( "content-disposition" ).contains( "filename=\"trackedEntities.csv\"" ),
            () -> response.content().toString().contains( "trackedEntity,trackedEntityType" ) );
    }

    @Test
    void getTrackedEntityReturnsCsvZipFormat()
    {
        WebClient.HttpResponse response = GET(
            "/tracker/trackedEntities.csv.zip?program={programId}&orgUnit={orgUnitId}",
            program.getUid(), orgUnit.getUid() );

        assertEquals( HttpStatus.OK, response.status() );

        assertAll( () -> response.header( "content-type" ).contains( ContextUtils.CONTENT_TYPE_CSV_ZIP ),
            () -> response.header( "content-disposition" ).contains( "filename=\"trackedEntities.csv.zip\"" ) );
    }

    @Test
    void getTrackedEntityReturnsCsvGZipFormat()
    {
        WebClient.HttpResponse response = GET(
            "/tracker/trackedEntities.csv.gz?program={programId}&orgUnit={orgUnitId}",
            program.getUid(), orgUnit.getUid() );

        assertEquals( HttpStatus.OK, response.status() );

        assertAll( () -> response.header( "content-type" ).contains( ContextUtils.CONTENT_TYPE_CSV_GZIP ),
            () -> response.header( "content-disposition" ).contains( "filename=\"trackedEntities.csv.gz\"" ) );
    }

    @Test
    void getTrackedEntityByIdWithEnrollments()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstance();

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( trackedEntityInstance,
            program, new Date(), new Date(), orgUnit );

        JsonObject json = GET( "/tracker/trackedEntities/{id}?fields=enrollments", trackedEntityInstance.getUid() )
            .content( HttpStatus.OK );

        assertWithEnrollmentResponse( json, programInstance );
    }

    private void assertWithEnrollmentResponse( JsonObject json, ProgramInstance programInstance )
    {
        assertTrue( json.isObject() );
        assertFalse( json.isEmpty() );
        assertHasOnlyMembers( json, "enrollments" );

        JsonObject enrollment = json.getArray( "enrollments" ).get( 0 ).asObject();

        assertHasMember( enrollment, "enrollment" );

        assertEquals( programInstance.getUid(), enrollment.getString( "enrollment" ).string() );
        assertEquals( programInstance.getUid(), enrollment.getString( "enrollment" ).string() );
        assertEquals( programInstance.getEntityInstance().getUid(), enrollment.getString( "trackedEntity" ).string() );
        assertEquals( program.getUid(), enrollment.getString( "program" ).string() );
        assertEquals( "ACTIVE", enrollment.getString( "status" ).string() );
        assertEquals( orgUnit.getUid(), enrollment.getString( "orgUnit" ).string() );
        assertEquals( orgUnit.getName(), enrollment.getString( "orgUnitName" ).string() );
        assertFalse( enrollment.getBoolean( "deleted" ).booleanValue() );
        assertHasMember( enrollment, "enrolledAt" );
        assertHasMember( enrollment, "occurredAt" );
        assertHasMember( enrollment, "createdAt" );
        assertHasMember( enrollment, "createdAtClient" );
        assertHasMember( enrollment, "updatedAt" );
        assertHasMember( enrollment, "notes" );
        assertHasMember( enrollment, "attributes" );
        assertHasMember( enrollment, "relationships" );
        assertHasMember( enrollment, "events" );
        assertHasMember( enrollment, "followUp" );
    }

    private TrackedEntityType trackedEntityTypeAccessible()
    {
        TrackedEntityType type = trackedEntityType( 'A' );
        type.getSharing().addUserAccess( userAccess() );
        manager.save( type, false );
        return type;
    }

    private TrackedEntityType trackedEntityTypeNotAccessible()
    {
        TrackedEntityType type = trackedEntityType( 'B' );
        manager.save( type, false );
        return type;
    }

    private TrackedEntityType trackedEntityType( char uniqueChar )
    {
        TrackedEntityType type = createTrackedEntityType( uniqueChar );
        type.getSharing().setOwner( owner );
        type.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        return type;
    }

    private TrackedEntityInstance trackedEntityInstance()
    {
        TrackedEntityInstance tei = trackedEntityInstance( orgUnit );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntityInstance trackedEntityInstanceNotInSearchScope()
    {
        TrackedEntityInstance tei = trackedEntityInstance( anotherOrgUnit );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntityInstance trackedEntityInstance( TrackedEntityType trackedEntityType )
    {
        TrackedEntityInstance tei = trackedEntityInstance( orgUnit, trackedEntityType );
        manager.save( tei, false );
        return tei;
    }

    private TrackedEntityInstance trackedEntityInstance( OrganisationUnit orgUnit )
    {
        return trackedEntityInstance( orgUnit, trackedEntityType );
    }

    private TrackedEntityInstance trackedEntityInstance( OrganisationUnit orgUnit, TrackedEntityType trackedEntityType )
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        tei.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        tei.getSharing().setOwner( owner );
        return tei;
    }

    private UserAccess userAccess()
    {
        UserAccess a = new UserAccess();
        a.setUser( user );
        a.setAccess( AccessStringHelper.FULL );
        return a;
    }

    private RelationshipType relationshipTypeAccessible( RelationshipEntity from,
        RelationshipEntity to )
    {
        RelationshipType type = relationshipType( from, to );
        type.getSharing().addUserAccess( userAccess() );
        manager.save( type, false );
        return type;
    }

    private RelationshipType relationshipTypeNotAccessible()
    {
        return relationshipType( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
    }

    private RelationshipType relationshipType( RelationshipEntity from, RelationshipEntity to )
    {
        RelationshipType type = createRelationshipType( 'A' );
        type.getFromConstraint().setRelationshipEntity( from );
        type.getToConstraint().setRelationshipEntity( to );
        type.getSharing().setOwner( owner );
        type.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( type, false );
        return type;
    }

    private Relationship relationship( TrackedEntityInstance from, TrackedEntityInstance to )
    {
        RelationshipType type = relationshipTypeAccessible( RelationshipEntity.TRACKED_ENTITY_INSTANCE,
            RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        return relationship( type, from, to );
    }

    private Relationship relationship( RelationshipType type, TrackedEntityInstance from, TrackedEntityInstance to )
    {
        Relationship r = new Relationship();

        RelationshipItem fromItem = new RelationshipItem();
        fromItem.setTrackedEntityInstance( from );
        from.getRelationshipItems().add( fromItem );
        r.setFrom( fromItem );
        fromItem.setRelationship( r );

        RelationshipItem toItem = new RelationshipItem();
        toItem.setTrackedEntityInstance( to );
        to.getRelationshipItems().add( toItem );
        r.setTo( toItem );
        toItem.setRelationship( r );

        r.setRelationshipType( type );
        r.setKey( type.getUid() );
        r.setInvertedKey( type.getUid() );
        r.setAutoFields();
        r.getSharing().setOwner( owner );
        manager.save( r, false );
        return r;
    }

    private void assertTrackedEntityWithinRelationship( TrackedEntityInstance expected, JsonObject json )
    {
        JsonObject jsonTEI = json.getObject( "trackedEntity" );
        assertFalse( jsonTEI.isEmpty(), "trackedEntity should not be empty" );
        assertEquals( expected.getUid(), jsonTEI.getString( "trackedEntity" ).string(), "trackedEntity UID" );
        assertHasNoMember( json, "trackedEntityType" );
        assertHasNoMember( json, "orgUnit" );
        assertHasNoMember( json, "relationships" ); // relationships are not
                                                   // returned within
                                                   // relationships
        assertTrue( jsonTEI.getArray( "attributes" ).isEmpty() );
    }

}