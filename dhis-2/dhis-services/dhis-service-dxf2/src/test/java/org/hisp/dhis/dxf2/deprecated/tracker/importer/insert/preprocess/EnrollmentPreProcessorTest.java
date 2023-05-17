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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.insert.preprocess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.BasePreProcessTest;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EnrollmentPreProcessorTest extends BasePreProcessTest
{

    private EnrollmentPreProcessor subject;

    private final Map<String, Enrollment> programInstanceMap = new HashMap<>();

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Captor
    protected ArgumentCaptor<String> sql;

    @Mock
    protected ResultSet mockResultSet;

    @BeforeEach
    void setUp()
    {
        this.subject = new EnrollmentPreProcessor();
        //
        // empty Enrollment Map
        //
        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );
    }

    @Test
    void verifyExitOnNullProgram()
    {
        when( workContext.getProgramsMap() ).thenReturn( new HashMap<>() );
        subject.process( event, workContext );
        MatcherAssert.assertThat( event.getEnrollment(), is( nullValue() ) );
    }

    @Test
    void verifyEnrollmentIsSetOnEventWhenOneProgramInstanceIsFound()
    {
        //
        // Tracked Entity Instance
        //
        TrackedEntity tei = createTrackedEntity( createOrganisationUnit( 'A' ) );
        when( workContext.getTrackedEntityInstance( event.getUid() ) ).thenReturn( Optional.of( tei ) );
        Enrollment enrollment = new Enrollment();
        enrollment.setUid( CodeGenerator.generateUid() );
        when( enrollmentStore.get( tei, program, ProgramStatus.ACTIVE ) )
            .thenReturn( Lists.newArrayList( enrollment ) );
        event.setProgram( program.getUid() );
        //
        // Method under test
        //
        subject.process( event, workContext );
        MatcherAssert.assertThat( event.getEnrollment(), is( enrollment.getUid() ) );
        assertThat( programInstanceMap.get( event.getUid() ), is( enrollment ) );
    }

    @Test
    void verifyEnrollmentIsNotSetOnEventWhenMultipleProgramInstanceAreFound()
    {
        //
        // Tracked Entity Instance
        //
        TrackedEntity tei = createTrackedEntity( createOrganisationUnit( 'A' ) );
        Map<String, Pair<TrackedEntity, Boolean>> teiMap = new HashMap<>();
        teiMap.put( event.getUid(), Pair.of( tei, true ) );
        when( workContext.getTrackedEntityInstanceMap() ).thenReturn( teiMap );

        event.setProgram( program.getUid() );
        subject.process( event, workContext );
        MatcherAssert.assertThat( event.getEnrollment(), is( nullValue() ) );
        assertThat( programInstanceMap.get( event.getUid() ), is( nullValue() ) );
    }

    @Test
    void verifyEnrollmentIsSetWithProgramWithoutRegistrationAndOneProgramStageInstance()
        throws SQLException
    {
        // crete a Program "without registration"
        Program programWithoutReg = createProgram( 'W' );
        programWithoutReg.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        // add the program to the work context map
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( programWithoutReg.getUid(), programWithoutReg );
        // make sure tha map is returned when invoking the mock work context
        when( workContext.getProgramsMap() ).thenReturn( programMap );
        Enrollment enrollment = new Enrollment();
        enrollment.setUid( CodeGenerator.generateUid() );
        enrollment.setId( 100L );
        when( workContext.getServiceDelegator().getJdbcTemplate() ).thenReturn( jdbcTemplate );
        //
        // simulate one record returned from query
        //
        when( mockResultSet.next() ).thenReturn( true ).thenReturn( false );
        when( mockResultSet.getLong( "programinstanceid" ) ).thenReturn( enrollment.getId() );
        when( mockResultSet.getString( "uid" ) ).thenReturn( enrollment.getUid() );
        // Mock jdbc call
        mockResultSetExtractor( mockResultSet );
        event.setProgram( programWithoutReg.getUid() );
        // method under test
        subject.process( event, workContext );
        MatcherAssert.assertThat( event.getEnrollment(), is( enrollment.getUid() ) );
        assertThat( programInstanceMap.get( event.getUid() ).getUid(), is( enrollment.getUid() ) );
        assertThat( programInstanceMap.get( event.getUid() ).getProgram().getUid(), is( programWithoutReg.getUid() ) );
        assertThat( sql.getValue(), is(
            "select pi.programinstanceid, pi.programid, pi.uid from programinstance pi where pi.programid = ? and pi.status = ?" ) );
    }

    @Test
    void verifyEnrollmentIsNotSetWithProgramWithoutRegistrationAndMultipleProgramStageInstances()
        throws SQLException
    {
        // crete a Program "without registration"
        Program programWithoutReg = createProgram( 'W' );
        programWithoutReg.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        // add the program to the work context map
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( programWithoutReg.getUid(), programWithoutReg );
        // make sure tha map is returned when invoking the mock work context
        when( workContext.getProgramsMap() ).thenReturn( programMap );
        Enrollment enrollment1 = new Enrollment();
        enrollment1.setUid( CodeGenerator.generateUid() );
        enrollment1.setId( 100L );
        Enrollment enrollment2 = new Enrollment();
        enrollment2.setUid( CodeGenerator.generateUid() );
        enrollment2.setId( 100L );
        when( workContext.getServiceDelegator().getJdbcTemplate() ).thenReturn( jdbcTemplate );
        //
        // simulate 2 records returned from query
        //
        when( mockResultSet.next() ).thenReturn( true ).thenReturn( true ).thenReturn( false );
        when( mockResultSet.getLong( "programinstanceid" ) ).thenReturn( enrollment1.getId(),
            enrollment2.getId() );
        when( mockResultSet.getString( "uid" ) ).thenReturn( enrollment1.getUid(), enrollment2.getUid() );
        // Mock jdbc call
        mockResultSetExtractor( mockResultSet );
        event.setProgram( programWithoutReg.getUid() );
        // method under test
        subject.process( event, workContext );
        MatcherAssert.assertThat( event.getEnrollment(), is( nullValue() ) );
        assertThat( sql.getValue(), is(
            "select pi.programinstanceid, pi.programid, pi.uid from programinstance pi where pi.programid = ? and pi.status = ?" ) );
    }

    public void mockResultSetExtractor( ResultSet resultSetMock )
    {
        when( jdbcTemplate.query( sql.capture(), (Object[]) any( Object.class ), any( ResultSetExtractor.class ) ) )
            .thenAnswer( (Answer<List<Enrollment>>) invocationOnMock -> {
                Object[] args = invocationOnMock.getArguments();
                ResultSetExtractor<List<Enrollment>> rm = (ResultSetExtractor<List<Enrollment>>) args[2];
                return rm.extractData( resultSetMock );
            } );
    }
}
