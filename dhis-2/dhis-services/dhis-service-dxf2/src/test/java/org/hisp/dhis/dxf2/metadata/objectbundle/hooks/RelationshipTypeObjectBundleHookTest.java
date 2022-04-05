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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackerdataview.TrackerDataView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Zubair Asghar
 */
@ExtendWith( MockitoExtension.class )
public class RelationshipTypeObjectBundleHookTest extends DhisConvenienceTest
{
    @InjectMocks
    private RelationshipTypeObjectBundleHook subject;

    @Mock
    private TrackedEntityTypeService trackedEntityTypeService;

    @Mock
    private ProgramService programService;

    @Mock
    private ProgramStageService programStageService;

    private Program program;

    private ProgramStage programStage;

    private RelationshipType teiToTeiRelationshipType;

    private RelationshipType teiToEnrollmentRelationshipType;

    private RelationshipType teiToEventRelationshipType;

    private RelationshipConstraint personConstraint;

    private RelationshipConstraint personConstraintWithNoAttribute;

    private RelationshipConstraint personConstraintWithMultipleAttribute;

    private RelationshipConstraint enrollmentConstraint;

    private RelationshipConstraint eventConstraint;

    private TrackedEntityType personTrackedEntityType;

    private TrackedEntityAttribute trackedEntityAttribute;

    private TrackedEntityAttribute teaNotPartOfProgram;

    private TrackedEntityTypeAttribute trackedEntityTypeAttribute;

    private ProgramTrackedEntityAttribute programTrackedEntityAttribute;

    private ProgramStageDataElement programStageDataElement;

    private DataElement dataElement;

    @BeforeEach
    public void setUp()
    {
        personTrackedEntityType = createTrackedEntityType( 'P' );

        trackedEntityAttribute = createTrackedEntityAttribute( 'T', ValueType.TEXT );
        teaNotPartOfProgram = createTrackedEntityAttribute( 'V', ValueType.TEXT );
        trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();
        trackedEntityTypeAttribute.setTrackedEntityType( personTrackedEntityType );
        trackedEntityTypeAttribute.setTrackedEntityAttribute( trackedEntityAttribute );

        personTrackedEntityType.setTrackedEntityTypeAttributes( Lists.newArrayList( trackedEntityTypeAttribute ) );

        dataElement = createDataElement( 'D' );
        program = createProgram( 'P' );
        programTrackedEntityAttribute = createProgramTrackedEntityAttribute( program, trackedEntityAttribute );
        program.setProgramAttributes( Lists.newArrayList( programTrackedEntityAttribute ) );

        programStage = createProgramStage( 'S', program );
        programStageDataElement = createProgramStageDataElement( programStage, dataElement, 1, false );
        programStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElement ) );

        personConstraint = new RelationshipConstraint();
        personConstraint.setTrackedEntityType( personTrackedEntityType );
        personConstraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        personConstraint.setTrackerDataView(
            TrackerDataView.builder().attributes( Sets.newHashSet( trackedEntityAttribute.getUid() ) ).build() );

        personConstraintWithNoAttribute = new RelationshipConstraint();
        personConstraintWithNoAttribute.setTrackedEntityType( personTrackedEntityType );
        personConstraintWithNoAttribute.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        personConstraintWithNoAttribute
            .setTrackerDataView( TrackerDataView.builder().attributes( Sets.newHashSet() ).build() );

        personConstraintWithMultipleAttribute = new RelationshipConstraint();
        personConstraintWithMultipleAttribute.setTrackedEntityType( personTrackedEntityType );
        personConstraintWithMultipleAttribute.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        personConstraintWithMultipleAttribute.setTrackerDataView( TrackerDataView.builder()
            .attributes( Sets.newHashSet( trackedEntityAttribute.getUid(), teaNotPartOfProgram.getUid() ) )
            .build() );

        enrollmentConstraint = new RelationshipConstraint();
        enrollmentConstraint.setProgram( program );
        enrollmentConstraint.setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        enrollmentConstraint.setTrackerDataView(
            TrackerDataView.builder().attributes( Sets.newHashSet( trackedEntityAttribute.getUid() ) ).build() );

        eventConstraint = new RelationshipConstraint();
        eventConstraint.setProgramStage( programStage );
        eventConstraint.setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        eventConstraint
            .setTrackerDataView(
                TrackerDataView.builder().dataElements( Sets.newHashSet( dataElement.getUid() ) ).build() );

        teiToTeiRelationshipType = createPersonToPersonRelationshipType( 'A', program, personTrackedEntityType, false );
        teiToTeiRelationshipType.setToConstraint( personConstraint );
        teiToTeiRelationshipType.setFromConstraint( personConstraint );

        teiToEnrollmentRelationshipType = createPersonToPersonRelationshipType( 'B', program, personTrackedEntityType,
            false );
        teiToEnrollmentRelationshipType.setToConstraint( enrollmentConstraint );
        teiToEnrollmentRelationshipType.setFromConstraint( enrollmentConstraint );

        teiToEventRelationshipType = createPersonToPersonRelationshipType( 'D', program, personTrackedEntityType,
            false );
        teiToEventRelationshipType.setToConstraint( eventConstraint );
        teiToEventRelationshipType.setFromConstraint( eventConstraint );

    }

    @Test
    public void test_successful_TrackerDataView_For_TrackedEntityType_Relationship()
    {
        when( trackedEntityTypeService.getTrackedEntityType( anyString() ) ).thenReturn( personTrackedEntityType );

        List<ErrorReport> errorReportList = subject.validate( teiToTeiRelationshipType, null );

        assertNotNull( errorReportList );
        assertTrue( errorReportList.isEmpty() );
    }

    @Test
    public void test_successful_TrackerDataView_For_TrackedEnrollment_Relationship()
    {
        when( programService.getProgram( anyString() ) ).thenReturn( program );

        List<ErrorReport> errorReportList = subject.validate( teiToEnrollmentRelationshipType, null );

        assertNotNull( errorReportList );
        assertTrue( errorReportList.isEmpty() );
    }

    @Test
    public void test_successful_TrackerDataView_For_Event_Relationship()
    {
        when( programStageService.getProgramStage( anyString() ) ).thenReturn( programStage );

        List<ErrorReport> errorReportList = subject.validate( teiToEventRelationshipType, null );

        assertNotNull( errorReportList );
        assertTrue( errorReportList.isEmpty() );
    }

    @Test
    public void test_empty_TrackerDataView_For_TrackedEntityType_Relationship()
    {
        when( trackedEntityTypeService.getTrackedEntityType( anyString() ) ).thenReturn( personTrackedEntityType );

        teiToTeiRelationshipType.setToConstraint( personConstraintWithNoAttribute );
        List<ErrorReport> errorReportList = subject.validate( teiToTeiRelationshipType, null );

        assertNotNull( errorReportList );
        assertTrue( errorReportList.isEmpty() );
    }

    @Test
    public void test_error_report_TrackerDataView_For_TrackedEntityType_Relationship()
    {
        when( trackedEntityTypeService.getTrackedEntityType( anyString() ) ).thenReturn( personTrackedEntityType );

        teiToTeiRelationshipType.setToConstraint( personConstraintWithMultipleAttribute );
        List<ErrorReport> errorReportList = subject.validate( teiToTeiRelationshipType, null );

        assertNotNull( errorReportList );
        assertFalse( errorReportList.isEmpty() );

        List<ErrorCode> errorCodes = errorReportList.stream().map( ErrorReport::getErrorCode )
            .collect( Collectors.toList() );

        assertTrue( errorCodes.contains( ErrorCode.E4314 ) );
        assertTrue( errorReportList.get( 0 ).getMessage().contains( teaNotPartOfProgram.getUid() ) );

    }
}
