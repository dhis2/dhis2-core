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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith( MockitoExtension.class )
class CopyServiceTest
{

    @Mock
    private ProgramService programService;

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private CopyService copyService;

    private static final String VALID_PROGRAM_UID = "abcdefghijk";

    private static final String INVALID_PROGRAM_UID = "123456789";

    @BeforeEach
    void setup()
    {
        copyService = new CopyService( programService, enrollmentService );
    }

    @Test
    void testCopyProgramFromUidWithValidProgram()
        throws ConflictException,
        NotFoundException
    {
        Program original = getValidProgram();
        List<Enrollment> originalEnrollments = List.of();
        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );
        when( programService.addProgram( any( Program.class ) ) ).thenReturn( 12344L );
        when( enrollmentService.getEnrollments( original ) ).thenReturn( originalEnrollments );

        String newProgramUid = copyService.copyProgramFromUid( VALID_PROGRAM_UID, Map.of() );

        assertNotEquals( original.getUid(), newProgramUid );
        assertTrue( CodeGenerator.isValidUid( newProgramUid ) );
    }

    @Test
    void testCopyProgramFromUidWithNullProgram()
    {
        when( programService.getProgram( INVALID_PROGRAM_UID ) ).thenReturn( null );
        assertThrows(NotFoundException.class, () -> copyService.copyProgramFromUid( INVALID_PROGRAM_UID, Map.of() ));
    }

    @Test
    void testCopyProgramFromUidWithDuplicateName()
    {
        Program original = getValidProgram();
        DataIntegrityViolationException error = new DataIntegrityViolationException( "DB ERROR",
            new Throwable( "DB ERROR" ) );
        when( programService.getProgram( VALID_PROGRAM_UID ) ).thenReturn( original );
        when( programService.addProgram( any( Program.class ) ) )
            .thenThrow( error );

        assertThrows( ConflictException.class, () -> copyService.copyProgramFromUid( VALID_PROGRAM_UID, Map.of() ) );
    }

    Program getValidProgram()
    {
        Program program = new Program();
        program.setAutoFields();
        program.setAccessLevel( AccessLevel.OPEN );
        program.setCode( CodeGenerator.generateCode( CodeGenerator.UID_CODE_SIZE ) );
        program.setCompleteEventsExpiryDays( 22 );
        program.setDescription( "Program description" );
        program.setDisplayIncidentDate( true );
        program.setDisplayFrontPageList( true );
        program.setEnrollmentDateLabel( "enroll date" );
        program.setExpiryDays( 33 );
        program.setFeatureType( FeatureType.NONE );
        program.setFormName( "Form name" );
        program.setIgnoreOverdueEvents( true );
        program.setIncidentDateLabel( "incident date" );
        program.setMaxTeiCountToReturn( 2 );
        program.setMinAttributesRequiredToSearch( 3 );
        program.setName( "Name" + CodeGenerator.generateUid() );
        program.setOnlyEnrollOnce( true );
        program.setOpenDaysAfterCoEndDate( 20 );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        program.setSharing( Sharing.builder().publicAccess( "yes" ).owner( "admin" ).build() );
        program.setShortName( "short name" );
        program.setSelectEnrollmentDatesInFuture( true );
        program.setSelectIncidentDatesInFuture( false );
        program.setSkipOffline( true );
        program.setUseFirstStageDuringRegistration( false );
        program.setCategoryCombo( new CategoryCombo( "cat combo", DataDimensionType.ATTRIBUTE ) );
        program.setDataEntryForm( new DataEntryForm( "entry form" ) );
        program.setExpiryPeriodType( PeriodType.getPeriodType( PeriodTypeEnum.QUARTERLY ) );
        program.setNotificationTemplates( Collections.emptySet() );
        program.setOrganisationUnits( Set.of( new OrganisationUnit( "Org One" ) ) );
        program.setProgramAttributes( Collections.emptyList() );
        program.setProgramIndicators( Collections.emptySet() );
        program.setProgramRuleVariables( Collections.emptySet() );
        program.setProgramSections( Collections.emptySet() );
        program.setProgramStages( getProgramStages( program ) );
        program.setRelatedProgram( new Program( "Related Program" ) );
        program.setStyle( new ObjectStyle() );
        program.setTrackedEntityType( new TrackedEntityType( "TET", "description" ) );
        program.setUserRoles( Collections.emptySet() );
        return program;
    }

    private Set<ProgramStage> getProgramStages( Program program )
    {
        ProgramStage stage = getNewProgramStageWithNoNulls( program );
        return Set.of( stage );
    }

    private ProgramStage getNewProgramStageWithNoNulls( Program program )
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setAutoFields();
        programStage.setCode( CodeGenerator.generateCode( CodeGenerator.UID_CODE_SIZE ) );
        programStage.setDataEntryForm( new DataEntryForm( "entry form" ) );
        programStage.setDescription( "Program description" );
        programStage.setDueDateLabel( "due label" );
        programStage.setExecutionDateLabel( "label" );
        programStage.setFeatureType( FeatureType.NONE );
        programStage.setFormName( "Form name" );
        programStage.setName( "Name" + CodeGenerator.generateUid() );
        programStage.setNextScheduleDate( new DataElement( "element" ) );
        programStage.setNotificationTemplates( Collections.emptySet() );
        programStage.setPeriodType( PeriodType.getPeriodType( PeriodTypeEnum.DAILY ) );
        programStage.setProgram( program );
        programStage.setReportDateToUse( "report date" );
        programStage.setSharing( Sharing.builder().publicAccess( "yes" ).owner( "admin" ).build() );
        programStage.setShortName( "short name" );
        programStage.setProgramStageSections( getProgramStageSections( programStage ) );
        programStage.setProgramStageDataElements( getProgramStageDataElements( programStage ) );
        programStage.setSortOrder( 2 );
        programStage.setStyle( new ObjectStyle() );
        programStage.setStandardInterval( 11 );
        return programStage;
    }

    private Set<ProgramStageSection> getProgramStageSections( ProgramStage programStage )
    {
        ProgramStageSection pss1 = getNewProgramStageSection( programStage );
        ProgramStageSection pss2 = getNewProgramStageSection( programStage );
        return Set.of( pss1, pss2 );
    }

    private Set<ProgramStageDataElement> getProgramStageDataElements( ProgramStage programStage )
    {
        ProgramStageDataElement psde1 = getNewProgramStageDataElement( programStage, "data el1" );
        ProgramStageDataElement psde2 = getNewProgramStageDataElement( programStage, "data el2" );
        return Set.of( psde1, psde2 );
    }

    private ProgramStageSection getNewProgramStageSection( ProgramStage original )
    {
        ProgramStageSection pss = new ProgramStageSection();
        pss.setAutoFields();
        pss.setDataElements( List.of( new DataElement( "DE1" ), new DataElement( "DE2" ) ) );
        pss.setDescription( "PSS Description" );
        pss.setFormName( "PSS form name" );
        pss.setProgramIndicators( List.of( new ProgramIndicator() ) );
        pss.setProgramStage( original );
        pss.setRenderType( new DeviceRenderTypeMap<>() );
        pss.setSortOrder( 1 );
        pss.setShortName( "PSS short name" );
        pss.setSharing( new Sharing() );
        pss.setStyle( new ObjectStyle() );
        return pss;
    }

    private ProgramStageDataElement getNewProgramStageDataElement( ProgramStage original, String dataElementName )
    {
        ProgramStageDataElement psde = new ProgramStageDataElement();
        psde.setProgramStage( original );
        psde.setAutoFields();
        psde.setDataElement( new DataElement( dataElementName ) );
        psde.setRenderType( new DeviceRenderTypeMap<>() );
        psde.setSortOrder( 1 );
        psde.setCompulsory( true );
        psde.setAllowProvidedElsewhere( true );
        psde.setDisplayInReports( true );
        psde.setAllowFutureDate( true );
        psde.setRenderOptionsAsRadio( true );
        psde.setSkipAnalytics( true );
        return psde;
    }
}
