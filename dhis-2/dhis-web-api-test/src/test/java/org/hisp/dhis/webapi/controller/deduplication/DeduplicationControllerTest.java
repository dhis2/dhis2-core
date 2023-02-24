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
package org.hisp.dhis.webapi.controller.deduplication;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author luca@dhis2.org
 */
class DeduplicationControllerTest extends DhisControllerConvenienceTest
{
    private final static String ENDPOINT = "/" + "potentialDuplicates/";

    @Autowired
    private IdentifiableObjectManager dbmsManager;

    @Autowired
    private ObjectMapper objectMapper;

    private TrackedEntityInstance origin;

    private TrackedEntityInstance duplicate;

    @BeforeEach
    public void setUp()
    {
        OrganisationUnit orgUnit = createOrganisationUnit( CodeGenerator.generateUid() );
        dbmsManager.save( orgUnit );

        origin = createTrackedEntityInstance( orgUnit );
        duplicate = createTrackedEntityInstance( orgUnit );

        dbmsManager.save( origin );
        dbmsManager.save( duplicate );
    }

    @Test
    void shouldPostPotentialDuplicateWhenTrackedEntitiesExist()
        throws Exception
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( origin.getUid(), duplicate.getUid() );
        assertStatus( HttpStatus.OK, POST( ENDPOINT, objectMapper.writeValueAsString( potentialDuplicate ) ) );
    }

    @Test
    void shouldThrowPostPotentialDuplicateWhenMissingDuplicateTeiInPayload()
        throws Exception
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( origin.getUid(), null );
        assertStatus( HttpStatus.BAD_REQUEST, POST( ENDPOINT, objectMapper.writeValueAsString( potentialDuplicate ) ) );
    }

    @Test
    void shouldThrowPostPotentialDuplicateWhenMissingOriginTeiInPayload()
        throws Exception
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( null, duplicate.getUid() );
        assertStatus( HttpStatus.BAD_REQUEST, POST( ENDPOINT, objectMapper.writeValueAsString( potentialDuplicate ) ) );
    }

    @Test
    void shouldThrowBadRequestWhenPutPotentialDuplicateAlreadyMerged()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( origin.getUid(), duplicate.getUid() );
        potentialDuplicate.setStatus( DeduplicationStatus.MERGED );
        save( potentialDuplicate );

        assertStatus( HttpStatus.BAD_REQUEST,
            PUT( ENDPOINT + potentialDuplicate.getUid() + "?status=" + DeduplicationStatus.INVALID.name() ) );
    }

    @Test
    void shouldThrowBadRequestWhenPutPotentialDuplicateToMergedStatus()
    {
        PotentialDuplicate potentialDuplicate = potentialDuplicate( origin.getUid(), duplicate.getUid() );
        assertStatus( HttpStatus.BAD_REQUEST,
            PUT( ENDPOINT + potentialDuplicate.getUid() + "?status=" + DeduplicationStatus.MERGED.name() ) );
    }

    @Test
    void shouldUpdatePotentialDuplicateWhenPotentialDuplicateExistsAndCorrectStatus()
    {
        PotentialDuplicate potentialDuplicate = potentialDuplicate( origin.getUid(), duplicate.getUid() );
        assertStatus( HttpStatus.OK,
            PUT( ENDPOINT + potentialDuplicate.getUid() + "?status=" + DeduplicationStatus.INVALID.name() ) );
    }

    @Test
    void shouldGetPotentialDuplicateByIdWhenPotentialDuplicateExists()
    {
        PotentialDuplicate potentialDuplicate = potentialDuplicate( origin.getUid(), duplicate.getUid() );
        assertStatus( HttpStatus.OK,
            GET( ENDPOINT + potentialDuplicate.getUid() ) );
    }

    @Test
    void shouldThrowNotFoundWhenPotentialDuplicateDoNotExists()
    {
        assertStatus( HttpStatus.NOT_FOUND,
            GET( ENDPOINT + "uid" ) );
    }

    private PotentialDuplicate potentialDuplicate( String original, String duplicate )
    {
        return save( new PotentialDuplicate( original, duplicate ) );
    }

    private PotentialDuplicate save( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicate.setLastUpdatedByUserName( "user" );
        potentialDuplicate.setCreatedByUserName( "user" );
        dbmsManager.save( potentialDuplicate );
        dbmsManager.flush();
        return potentialDuplicate;
    }
}
