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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.association.jdbc.JdbcOrgUnitAssociationsStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This Integration test using Postgres is necessary as the H2 DB doesn't work
 * with {@link JdbcOrgUnitAssociationsStore#checkOrganisationUnitsAssociations}.
 * A ClassCastException is thrown. H2 uses a different object type for its
 * result set which doesn't allow the cast to String[] when creating the orgUnit
 * <-> program relationship map.
 *
 * @author David Mackessy
 */

class ProgramControllerIntegrationTest extends DhisControllerIntegrationTest
{

    @Autowired
    private ObjectMapper jsonMapper;

    public static final String PROGRAM_UID = "PrZMWi7rBga";

    public static final String ORG_UNIT_UID = "Orgunit1000";

    @BeforeEach
    public void testSetup()
        throws JsonProcessingException
    {
        DataElement dataElement1 = createDataElement( 'a' );
        DataElement dataElement2 = createDataElement( 'b' );
        dataElement1.setUid( "deabcdefgha" );
        dataElement2.setUid( "deabcdefghb" );
        TrackedEntityAttribute tea1 = createTrackedEntityAttribute( 'a' );
        TrackedEntityAttribute tea2 = createTrackedEntityAttribute( 'b' );
        tea1.setUid( "TEA1nnnnnaa" );
        tea2.setUid( "TEA1nnnnnab" );
        POST( "/dataElements", jsonMapper.writeValueAsString( dataElement1 ) )
            .content( HttpStatus.CREATED );
        POST( "/dataElements", jsonMapper.writeValueAsString( dataElement2 ) )
            .content( HttpStatus.CREATED );
        POST( "/trackedEntityAttributes", jsonMapper.writeValueAsString( tea1 ) )
            .content( HttpStatus.CREATED );
        POST( "/trackedEntityAttributes", jsonMapper.writeValueAsString( tea2 ) )
            .content( HttpStatus.CREATED );

        POST( "/metadata", WebClient.Body( "program/create_program.json" ) )
            .content( HttpStatus.OK );
    }

    @Test
    void testCopyProgramEnrollments()
    {
        assertStatus( HttpStatus.CREATED, POST( "/trackedEntityTypes",
            "{'description': 'add TET for Enrollment test','id':'TEType10000','name':'Tracked Entity Type 1'}" ) );

        assertStatus( HttpStatus.CREATED, POST( "/trackedEntityAttributes/",
            "{'name':'attrA', 'id':'TEAttr10000','shortName':'attrA', 'valueType':'TEXT', 'aggregationType':'NONE'}" ) );

        String teiId = assertStatus( HttpStatus.OK, POST( "/trackedEntityInstances", "{\n" +
            "  'trackedEntityType': 'TEType10000',\n" +
            "  'program': 'PrZMWi7rBga',\n" +
            "  'status': 'ACTIVE',\n" +
            "  'orgUnit': '" + ORG_UNIT_UID + "',\n" +
            "  'enrollmentDate': '2023-06-16',\n" +
            "  'incidentDate': '2023-06-16'\n" +
            "}" ) );

        POST( "/enrollments", "{\n" +
            "  'trackedEntityInstance': '" + teiId + "',\n" +
            "  'program': 'PrZMWi7rBga',\n" +
            "  'status': 'ACTIVE',\n" +
            "  'orgUnit': '" + ORG_UNIT_UID + "',\n" +
            "  'enrollmentDate': '2023-06-16',\n" +
            "  'incidentDate': '2023-06-16'\n" +
            "}" ).content( HttpStatus.CREATED );

        POST(
            "/programs/%s/copy".formatted( PROGRAM_UID ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        JsonWebMessage enrollmentsForOrgUnit = GET( "/tracker/enrollments?orgUnit=%s".formatted( ORG_UNIT_UID ) )
            .content( HttpStatus.OK )
            .as( JsonWebMessage.class );

        JsonList<JsonEnrollment> enrollments = enrollmentsForOrgUnit.getList( "instances", JsonEnrollment.class );
        Set<JsonEnrollment> originalProgramEnrollments = enrollments.stream()
            .filter( enrollment -> enrollment.getProgram().equals( PROGRAM_UID ) )
            .collect( Collectors.toSet() );

        assertEquals( 2, enrollments.size() );
        assertEquals( 1, originalProgramEnrollments.size() );
    }
}
