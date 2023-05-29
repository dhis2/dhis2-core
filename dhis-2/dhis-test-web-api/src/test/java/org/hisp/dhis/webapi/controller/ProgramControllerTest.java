/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.feedback.ErrorCode.E1005;
import static org.hisp.dhis.webapi.utils.TestUtils.getMatchingGroupFromPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.json.domain.JsonProgram;
import org.hisp.dhis.webapi.json.domain.JsonProgramStage;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramControllerTest extends DhisControllerConvenienceTest
{
    private OrganisationUnit orgUnit;

    @BeforeEach
    public void testSetup()
    {
        orgUnit = createOrganisationUnit( "Org 1" );
        manager.save( orgUnit );
        CategoryOption option = createCategoryOption( 'y' );
        manager.save( option );
        Category category = createCategory( 'x', option );
        manager.save( category );
        ProgramStage stage1 = createProgramStage( 'a', 30 );
        ProgramStage stage2 = createProgramStage( 'b', 30 );
        manager.save( List.of( stage1, stage2 ) );
        DataElement dataElement1 = createDataElement( 'p' );
        DataElement dataElement2 = createDataElement( 'q' );
        manager.save( List.of( dataElement1, dataElement2 ) );
        ProgramStageDataElement psde1 = createProgramStageDataElement( stage1, dataElement1, 1 );
        ProgramStageDataElement psde2 = createProgramStageDataElement( stage2, dataElement2, 2 );
        manager.save( List.of( psde1, psde2 ) );

        ProgramStageSection pss1 = createProgramStageSection( 'l', 3 );
        ProgramStageSection pss2 = createProgramStageSection( 'm', 4 );
        manager.save( List.of( pss1, pss2 ) );

        TrackedEntityAttribute tea = createTrackedEntityAttribute( 't' );
        manager.save( tea );
        CategoryCombo categoryCombo = createCategoryCombo( 'z', category );
        manager.save( categoryCombo );
        Program program = createProgram( 'c', null, Set.of( tea ), Set.of( orgUnit ),
            categoryCombo );
        manager.save( program );
        stage1.setProgramStageDataElements( Set.of( psde1, psde2 ) );
        stage2.setProgramStageDataElements( Set.of( psde1, psde2 ) );
        stage1.setProgramStageSections( Set.of( pss1, pss2 ) );
        stage2.setProgramStageSections( Set.of( pss1, pss2 ) );
        manager.save( stage1 );
        manager.save( stage2 );
        program.setProgramStages( Set.of( stage1, stage2 ) );
        manager.save( program );
        TrackedEntity tei1 = createTrackedEntity( 'd', orgUnit );
        TrackedEntity tei2 = createTrackedEntity( 'e', orgUnit );
        manager.save( List.of( tei1, tei2 ) );
        Enrollment enrollment1 = createEnrollment( program, tei1, orgUnit );
        Enrollment enrollment2 = createEnrollment( program, tei2, orgUnit );
        manager.save( List.of( enrollment1, enrollment2 ) );
    }

    @Test
    void testCopyProgramWith2ProgramStagesAnd2Enrollments()
    {
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( BASE_PR_UID + 'c' ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String copiedProgramUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram copiedProgram = GET( "/programs/{id}", copiedProgramUid ).content( HttpStatus.OK )
            .as( JsonProgram.class );
        JsonList<JsonProgramStage> copiedStages = copiedProgram.getProgramStages();

        assertEquals( copiedProgramUid, copiedProgram.getUid() );
        assertEquals( "ProgramShortc", copiedProgram.getShortName() );
        assertEquals( "Copy of Programc", copiedProgram.getName() );
        assertEquals( "Descriptionc", copiedProgram.getDescription() );
        assertEquals( 2, copiedProgram.getProgramStages().size() );

        // ensure that copied stages have new uids
        Set<String> copiedStageUids = copiedStages.stream().map( JsonProgramStage::getUid )
            .collect( Collectors.toSet() );
        copiedStageUids.removeAll( Set.of( BASE_PG_UID + 'a', BASE_PG_UID + 'b' ) );
        assertEquals( 2, copiedStageUids.size() );

        //check for copied enrollments with new program uid
        JsonObject enrollments = GET( "/tracker/enrollments?orgUnit={orgUnitId}", orgUnit.getUid() )
            .content( HttpStatus.OK )
            .as( JsonObject.class );

        JsonList<JsonEnrollment> instances = enrollments.getString( "instances" ).asList( JsonEnrollment.class );

        long enrollmentsForNewProgramCount = instances.stream().map( JsonEnrollment::getProgram )
            .filter( program -> program.equals( copiedProgramUid ) ).count();
        assertEquals( 2, enrollmentsForNewProgramCount );

        //check for new Program Stage Data Elements and Program Sections
        JsonProgramStage copiedStage = copiedStages.get( 0 );
        JsonProgramStage jsonCopiedProgramStage = GET( "/programStages/" + copiedStage.getUid() )
            .content( HttpStatus.OK )
            .as( JsonProgramStage.class );
        assertEquals( 2, jsonCopiedProgramStage.getProgramStageDataElements().size() );
        assertEquals( 2, jsonCopiedProgramStage.getProgramStageSections().size() );
    }

    @Test
    void testCopyProgramWithPrefixCopyOption()
    {
        String prefixCopyOption = "add prefix ";
        JsonWebMessage response = POST(
            "/programs/%s/copy?prefix=%s".formatted( BASE_PR_UID + 'c', prefixCopyOption ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String newUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram newProgram = GET( "/programs/{id}", newUid ).content( HttpStatus.OK ).as( JsonProgram.class );

        assertEquals( newUid, newProgram.getUid() );
        assertEquals( "ProgramShortc", newProgram.getShortName() );
        assertEquals( "add prefix Programc", newProgram.getName() );
        assertEquals( "Descriptionc", newProgram.getDescription() );
    }

    @Test
    void testCopyProgramWithInvalidUid()
    {
        String invalidProgramUid = "invalid";
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( invalidProgramUid ) )
            .content( HttpStatus.NOT_FOUND )
            .as( JsonWebMessage.class );
        assertEquals( "Not Found", response.getHttpStatus() );
        assertEquals( 404, response.getHttpStatusCode() );
        assertEquals( "ERROR", response.getStatus() );
        assertEquals( "Program with id invalid could not be found.", response.getMessage() );
        assertEquals( E1005, response.getErrorCode() );
    }
}
