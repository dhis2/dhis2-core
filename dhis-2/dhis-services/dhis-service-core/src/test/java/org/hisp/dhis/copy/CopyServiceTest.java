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
package org.hisp.dhis.copy;

import static org.hisp.dhis.program.notification.NotificationTrigger.ENROLLMENT;
import static org.hisp.dhis.program.notification.ProgramNotificationRecipient.WEB_HOOK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramSectionService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith( MockitoExtension.class )
class CopyServiceTest extends DhisConvenienceTest
{

    @Mock
    private ProgramService programService;

    @Mock
    private ProgramStageService programStageService;

    @Mock
    private ProgramSectionService programSectionService;

    @Mock
    private ProgramStageSectionService programStageSectionService;

    @Mock
    private ProgramStageDataElementService programStageDataElementService;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private ProgramRuleVariableService programRuleVariableService;

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private CopyService copyService;

    private static final String VALID_PROGRAM_UID = "abcdefghijk";

    private static final String INVALID_PROGRAM_UID = "123456789";

    @Test
    void testCopyProgramFromUidWithValidProgram()
        throws NotFoundException
    {
        Program original = createProgram();
        OrganisationUnit orgUnit = createOrganisationUnit( "New Org 1" );
        List<Enrollment> originalEnrollments = List
            .of( createEnrollment( original, createTrackedEntity( orgUnit ), orgUnit ) );
        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );
        when( enrollmentService.getEnrollments( original ) ).thenReturn( originalEnrollments );

        Program programCopy = copyService.copyProgram( VALID_PROGRAM_UID, Map.of() );

        assertNotEquals( original.getUid(), programCopy.getUid() );
        assertTrue( CodeGenerator.isValidUid( programCopy.getUid() ) );
        verify( programService, times( 2 ) ).addProgram( programCopy );
        verify( programStageService, times( 2 ) ).saveProgramStage( any( ProgramStage.class ) );
        verify( programStageDataElementService, times( 2 ) )
            .addProgramStageDataElement( any( ProgramStageDataElement.class ) );
        verify( programStageSectionService, times( 2 ) )
            .saveProgramStageSection( any( ProgramStageSection.class ) );
        verify( programIndicatorService, times( 1 ) ).addProgramIndicator( any( ProgramIndicator.class ) );
        verify( programRuleVariableService, times( 1 ) ).addProgramRuleVariable( any( ProgramRuleVariable.class ) );
        verify( programSectionService, times( 1 ) ).addProgramSection( any( ProgramSection.class ) );
        verify( enrollmentService, times( 1 ) ).addEnrollment( any( Enrollment.class ) );
    }

    @Test
    void testCopyProgramWithCorrectlyMappedStageForRuleVariable()
        throws NotFoundException
    {
        Program original = createProgram();
        ProgramStage stage1 = createProgramStage( original, "stage 1" );
        ProgramStage stage2 = createProgramStage( original, "stage 2" );
        original.setProgramStages( Set.of( stage1, stage2 ) );

        ProgramRuleVariable ruleVariable1 = createProgramRuleVariable( original, stage1 );
        ProgramRuleVariable ruleVariable2 = createProgramRuleVariable( original, stage2 );
        original.setProgramRuleVariables( Set.of( ruleVariable1, ruleVariable2 ) );

        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );

        Program programCopy = copyService.copyProgram( VALID_PROGRAM_UID, Map.of() );

        Set<String> stageUidsLinkedToRuleVariables = programCopy.getProgramRuleVariables().stream()
            .map( prv -> prv.getProgramStage().getUid() ).collect( Collectors.toSet() );
        Set<String> originalStageUids = Set.of( stage1.getUid(), stage2.getUid() );

        stageUidsLinkedToRuleVariables.retainAll( originalStageUids );
        assertTrue( stageUidsLinkedToRuleVariables.isEmpty() );
    }

    @Test
    void testCopyProgramFromUidCheckProgramAttributes()
        throws NotFoundException
    {
        Program original = createProgram();
        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );

        Program programCopy = copyService.copyProgram( VALID_PROGRAM_UID, Map.of() );

        assertEquals( 1, programCopy.getProgramAttributes().size() );
    }

    @Test
    void testCopyProgramFromUidCheckProgramRuleVariables()
        throws NotFoundException
    {
        Program original = createProgram();
        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );

        Program programCopy = copyService.copyProgram( VALID_PROGRAM_UID, Map.of() );

        assertEquals( 1, programCopy.getProgramRuleVariables().size() );
    }

    @Test
    void testCopyProgramFromUidCheckProgramIndicators()
        throws NotFoundException
    {
        Program original = createProgram();
        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );

        Program programCopy = copyService.copyProgram( VALID_PROGRAM_UID, Map.of() );

        assertEquals( 1, programCopy.getProgramIndicators().size() );
    }

    @Test
    void testCopyProgramFromUidWithValidProgramAndNullEnrollments()
        throws NotFoundException
    {
        Program original = createProgram();
        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );
        when( enrollmentService.getEnrollments( original ) ).thenReturn( null );

        Program programCopy = copyService.copyProgram( VALID_PROGRAM_UID, Map.of() );

        assertNotEquals( original.getUid(), programCopy.getUid() );
        assertTrue( CodeGenerator.isValidUid( programCopy.getUid() ) );
        verify( enrollmentService, never() ).addEnrollment( any( Enrollment.class ) );
    }

    @Test
    void testCopyProgramFromUidWithNullProgram() {
        when(programService.getProgram(INVALID_PROGRAM_UID)).thenReturn(null);
        assertThrows(NotFoundException.class, () -> copyService.copyProgram(INVALID_PROGRAM_UID, Map.of()));
        verify(programService, never()).addProgram(any(Program.class));
    }

    @Test
    void testCopyProgramFromUidWithDbException()
    {
        Program original = createProgram();
        Map<String, String> options = Map.of();
        DataIntegrityViolationException error = new DataIntegrityViolationException( "DB ERROR",
            new Throwable( "DB ERROR" ) );
        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );
        when( programService.addProgram( any( Program.class ) ) )
            .thenThrow( error );

        assertThrows( DataIntegrityViolationException.class,
            () -> copyService.copyProgram( VALID_PROGRAM_UID, options ) );
    }

    Program createProgram()
    {
        Program p = new Program();
        p.setAutoFields();
        p.setAccessLevel( AccessLevel.OPEN );
        p.setCategoryCombo( createCategoryCombo( 'c' ) );
        p.setCompleteEventsExpiryDays( 22 );
        p.setDataEntryForm( createDataEntryForm( 'd' ) );
        p.setDescription( "Program description" );
        p.setDisplayIncidentDate( true );
        p.setDisplayFrontPageList( true );
        p.setEnrollmentDateLabel( "enroll date" );
        p.setExpiryDays( 33 );
        p.setExpiryPeriodType( PeriodType.getPeriodType( PeriodTypeEnum.QUARTERLY ) );
        p.setFeatureType( FeatureType.NONE );
        p.setFormName( "Form name" );
        p.setIgnoreOverdueEvents( true );
        p.setIncidentDateLabel( "incident date" );
        p.setMaxTeiCountToReturn( 2 );
        p.setMinAttributesRequiredToSearch( 3 );
        p.setName( "Program Name" );
        p.setNotificationTemplates( Set.of( createProgramNotificationTemplate( "not1", 20, ENROLLMENT, WEB_HOOK ) ) );
        p.setOnlyEnrollOnce( true );
        p.setOpenDaysAfterCoEndDate( 20 );
        p.setOrganisationUnits( Set.of( createOrganisationUnit( "Org 1" ) ) );
        p.setProgramAttributes( List.of(
            createProgramTrackedEntityAttribute( p, createTrackedEntityAttribute( 't' ) ) ) );
        p.setProgramIndicators( Set.of( createProgramIndicator( 'i', p, "exp", "ind" ) ) );
        p.setProgramRuleVariables( Set.of( createProgramRuleVariable( 'v', p ) ) );
        p.setProgramSections( Set.of( createProgramSection( 'x', p ) ) );
        p.setProgramStages( createProgramStages( p ) );
        p.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        p.setRelatedProgram( createProgram( 'P' ) );
        p.setSharing( Sharing.builder().publicAccess( "yes" ).owner( "admin" ).build() );
        p.setShortName( "short name" );
        p.setSelectEnrollmentDatesInFuture( true );
        p.setSelectIncidentDatesInFuture( false );
        p.setSkipOffline( true );
        p.setStyle( new ObjectStyle() );
        p.setTrackedEntityType( createTrackedEntityType( 'A' ) );
        p.setUseFirstStageDuringRegistration( false );
        p.setUserRoles( Set.of( createUserRole( "tester", "d" ) ) );
        return p;
    }

    private Set<ProgramStage> createProgramStages( Program program )
    {
        ProgramStage stage = createProgramStage( program, "stage name" );
        return Set.of( stage );
    }

    private ProgramStage createProgramStage( Program program, String name )
    {
        ProgramStage ps = new ProgramStage();
        ps.setAutoFields();
        ps.setDataEntryForm( new DataEntryForm( "entry form" ) );
        ps.setDescription( "Program description" );
        ps.setDueDateLabel( "due label" );
        ps.setExecutionDateLabel( "label" );
        ps.setFeatureType( FeatureType.NONE );
        ps.setFormName( "Form name" );
        ps.setName( "Stage Name" );
        ps.setNextScheduleDate( new DataElement( "element" ) );
        ps.setNotificationTemplates( Collections.emptySet() );
        ps.setPeriodType( PeriodType.getPeriodType( PeriodTypeEnum.DAILY ) );
        ps.setProgram( program );
        ps.setReportDateToUse( "report date" );
        ps.setSharing( Sharing.builder().publicAccess( "yes" ).owner( "admin" ).build() );
        ps.setShortName( "short name" );
        ps.setProgramStageSections( createProgramStageSections( ps ) );
        ps.setProgramStageDataElements( createProgramStageDataElements( ps ) );
        ps.setSortOrder( 2 );
        ps.setStyle( new ObjectStyle() );
        ps.setStandardInterval( 11 );
        return ps;
    }

    private Set<ProgramStageSection> createProgramStageSections( ProgramStage programStage )
    {
        ProgramStageSection pss1 = createProgramStageSection( 'w', 7 );
        pss1.setProgramStage( programStage );
        ProgramStageSection pss2 = createProgramStageSection( 'q', 6 );
        pss2.setProgramStage( programStage );
        return Set.of( pss1, pss2 );
    }

    private Set<ProgramStageDataElement> createProgramStageDataElements( ProgramStage programStage )
    {
        ProgramStageDataElement psde1 = createProgramStageDataElement( programStage, createDataElement( 'k' ), 3 );
        ProgramStageDataElement psde2 = createProgramStageDataElement( programStage, createDataElement( 'y' ), 3 );
        return Set.of( psde1, psde2 );
    }

    private ProgramRuleVariable createProgramRuleVariable( Program program, ProgramStage programStage )
    {
        ProgramRuleVariable ruleVariable = createProgramRuleVariable( 'a', program );
        ruleVariable.setProgramStage( programStage );
        return ruleVariable;
    }
}
