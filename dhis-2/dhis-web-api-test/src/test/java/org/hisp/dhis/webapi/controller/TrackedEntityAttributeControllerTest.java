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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntityAttributeControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    private OrganisationUnit orgUnit;

    private Program program;

    private TrackedEntityInstance tei;

    private Enrollment enrollment;

    private TrackedEntityAttribute teaA;

    private TrackedEntityAttribute teaB;

    private TrackedEntityAttribute teaC;

    private TrackedEntityAttribute teaD;

    private TrackedEntityAttribute teaE;

    ProgramTrackedEntityAttribute pteaA;

    ProgramTrackedEntityAttribute pteaB;

    TrackedEntityTypeAttribute tetaA;

    TrackedEntityTypeAttribute tetaB;

    TrackedEntityType trackedEntityType;

    @BeforeEach
    void setUp()
    {
        orgUnit = createOrganisationUnit( 'A' );
        manager.save( orgUnit );

        program = createProgram( 'A' );
        manager.save( program );

        teaA = createTrackedEntityAttribute( 'A' );
        teaB = createTrackedEntityAttribute( 'B' );
        teaC = createTrackedEntityAttribute( 'C' );
        teaD = createTrackedEntityAttribute( 'D' );
        teaE = createTrackedEntityAttribute( 'E' );
        manager.save( teaA );
        manager.save( teaB );
        manager.save( teaC );
        manager.save( teaD );
        manager.save( teaE );

        trackedEntityType = createTrackedEntityType( 'A' );
        manager.save( trackedEntityType );

        tetaA = new TrackedEntityTypeAttribute();
        tetaA.setSearchable( true );
        tetaA.setTrackedEntityType( trackedEntityType );
        tetaA.setTrackedEntityAttribute( teaA );
        manager.save( tetaA );
        trackedEntityType.getTrackedEntityTypeAttributes().add( tetaA );

        tetaB = new TrackedEntityTypeAttribute();
        tetaB.setSearchable( false );
        tetaB.setTrackedEntityType( trackedEntityType );
        tetaB.setTrackedEntityAttribute( teaD );
        manager.save( tetaB );
        trackedEntityType.getTrackedEntityTypeAttributes().add( tetaB );

        manager.update( trackedEntityType );

        pteaA = new ProgramTrackedEntityAttribute();
        pteaA.setSearchable( true );
        pteaA.setAttribute( teaB );
        pteaA.setProgram( program );
        manager.save( pteaA );

        pteaB = new ProgramTrackedEntityAttribute();
        pteaB.setSearchable( false );
        pteaB.setAttribute( teaE );
        pteaB.setProgram( program );
        manager.save( pteaB );

        program.getProgramAttributes().add( pteaA );
        program.getProgramAttributes().add( pteaB );
        manager.update( program );

        tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        manager.save( tei );

        enrollment = new Enrollment( program, tei, orgUnit );
        enrollment.setAutoFields();
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );
        enrollment.setStatus( ProgramStatus.COMPLETED );
        enrollment.setFollowup( true );
        manager.save( enrollment );
    }

    @Test
    void getIndexableOnlyAttributes()
    {
        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=true" ).content( HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeA", "AttributeB" ) );
    }

    @Test
    void getIndexableOnlyAttributesWithDateAndTextValueType()
    {
        teaA.setValueType( ValueType.DATE );
        manager.update( teaA );
        teaB.setValueType( ValueType.TEXT );
        manager.update( teaB );

        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=true" ).content( HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeB" ) );
    }

    @Test
    void getIndexableOnlyAttributesWithLongTextAndBooleanValueType()
    {
        teaA.setValueType( ValueType.LONG_TEXT );
        manager.update( teaA );
        teaB.setValueType( ValueType.BOOLEAN );
        manager.update( teaB );

        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=true" ).content( HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeA" ) );
    }

    @Test
    void getIndexableOnlyAttributesWithNumberAndPhoneNumberValueType()
    {
        teaA.setValueType( ValueType.NUMBER );
        manager.update( teaA );
        teaB.setValueType( ValueType.PHONE_NUMBER );
        manager.update( teaB );

        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=true" ).content( HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeB" ) );
    }

    @Test
    void getIndexableOnlyAttributesWithEmailAndPercentageValueType()
    {
        teaA.setValueType( ValueType.EMAIL );
        manager.update( teaA );
        teaB.setValueType( ValueType.PERCENTAGE );
        manager.update( teaB );

        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=true" ).content( HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeA" ) );
    }

    @Test
    void getIndexableOnlyAttributesWithTimeAndUserNameValueType()
    {
        teaA.setValueType( ValueType.TIME );
        manager.update( teaA );
        teaB.setValueType( ValueType.USERNAME );
        manager.update( teaB );

        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=true" ).content( HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeB" ) );
    }

    @Test
    void getIndexableOnlyAttributesWithUrlAndCoordinateValueType()
    {
        teaA.setValueType( ValueType.URL );
        manager.update( teaA );
        teaB.setValueType( ValueType.COORDINATE );
        manager.update( teaB );

        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=true" ).content( HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeA" ) );
    }

    @Test
    void getAllAttributes()
    {
        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=false" ).content( HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeA", "AttributeB", "AttributeC", "AttributeD", "AttributeE" ) );
    }

    @Test
    void getIndexableAttributesAndFilterByIdShouldThrowError()
    {
        assertEquals( "indexableOnly parameter cannot be set if a separate filter for id is specified",
            GET( "/trackedEntityAttributes?indexableOnly=true&filter=id:eq:ImspTQPwCqd" ).error(
                HttpStatus.BAD_REQUEST ).getMessage() );
    }

    @Test
    void getIndexableAttributesAndFilterByOtherParameters()
    {
        JsonObject json = GET(
            "/trackedEntityAttributes?indexableOnly=true&filter=name:in:[AttributeB,AttributeC]" ).content(
                HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeB" ) );
    }

    @Test
    void getAttributesWithNameFilter()
    {
        JsonObject json = GET(
            "/trackedEntityAttributes?indexableOnly=false&filter=name:in:[AttributeB,AttributeC]" ).content(
                HttpStatus.OK );

        assertAttributeList( json, Set.of( "AttributeB", "AttributeC" ) );
    }

    @Test
    void shouldNotFailIfNoIndexableAttributesAreConfigured()
    {
        tetaA.setSearchable( false );
        manager.update( tetaA );
        tetaB.setSearchable( false );
        manager.update( tetaB );
        pteaA.setSearchable( false );
        manager.update( pteaA );
        pteaB.setSearchable( false );
        manager.update( pteaB );

        JsonObject json = GET( "/trackedEntityAttributes?indexableOnly=true&filter=name:in:[AttributeB,AttributeC]" )
            .content( HttpStatus.OK );

        assertAttributeList( json, Set.of() );
    }

    private static void assertAttributeList( JsonObject actualJson, Set<String> expected )
    {
        assertFalse( actualJson.isEmpty() );
        assertEquals( expected.size(), actualJson.getArray( "trackedEntityAttributes" ).size() );
        assertEquals( expected, actualJson.getArray( "trackedEntityAttributes" )
            .viewAsList( e -> e.asObject().getString( "displayName" ) )
            .stream().map( JsonString::string )
            .collect( Collectors.toSet() ) );
    }
}