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
package org.hisp.dhis.tracker.programrule;

import static org.hisp.dhis.programrule.ProgramRuleActionType.ASSIGN;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyWarnings;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1307;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1308;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.ImportReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramRuleAssignActionTest extends TrackerTest
{
    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    private Program program;

    private DataElement dataElement1;

    private DataElement dataElementWithOptionSet;

    private DataElement dataElementToAssignValueTo;

    private ProgramRuleVariable programRuleVariableWithOptionSet;

    @Override
    public void initTest()
        throws IOException
    {
        ObjectBundle bundle = setUpMetadata( "tracker/simple_metadata.json" );
        program = bundle.getPreheat().get( PreheatIdentifier.UID, Program.class, "BFcipDERJnf" );
        dataElement1 = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00001" );
        DataElement dataElement2 = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00002" );
        dataElementWithOptionSet = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00012" );
        dataElementToAssignValueTo = bundle.getPreheat().get( PreheatIdentifier.UID, DataElement.class, "DATAEL00013" );

        ProgramRuleVariable programRuleVariable = createProgramRuleVariableWithDataElement( 'A', program,
            dataElement2 );
        programRuleVariableWithOptionSet = createProgramRuleVariableWithDataElement( 'A', program,
            dataElementWithOptionSet );
        programRuleVariableWithOptionSet.setName( "test-de-optionset" );
        programRuleVariableWithOptionSet.setUseCodeForOptionSet( false );

        programRuleVariableService.addProgramRuleVariable( programRuleVariableWithOptionSet );
        programRuleVariableService.addProgramRuleVariable( programRuleVariable );

        injectAdminUser();

        assignProgramRule();
        trackerImportService.importTracker( fromJson( "tracker/programrule/tei_enrollment_completed_event.json" ) );
    }

    @Test
    void shouldImportWithWarningWhenDataElementWithSameValueIsAssignedByAssignRule()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/programrule/event_update_datavalue_same_value.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport importReport = trackerImportService.importTracker( params );

        assertHasOnlyWarnings( importReport, E1308, E1308 );
    }

    @Test
    void shouldAssignOptionNameToDataElement()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/programrule/event_update_datavalue_same_value.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        trackerImportService.importTracker( params );

        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( "D9PbzJY8bJO" );
        String dataValue = psi.getEventDataValues().stream().filter( dv -> dv.getDataElement().equals( "DATAEL00013" ) )
            .map( EventDataValue::getValue ).findAny().get();

        assertEquals( "option3-name", dataValue, "Option name is not assigned to dataElement" );
    }

    @Test
    void shouldNotImportWhenDataElementWithDifferentValueIsAssignedByAssignRule()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/programrule/event_update_datavalue_different_value.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport importReport = trackerImportService.importTracker( params );

        assertHasOnlyErrors( importReport, E1307 );
    }

    @Test
    void shouldImportWithWarningWhenDataElementWithDifferentValueIsAssignedByAssignRuleAndOverwriteKeyIsTrue()
        throws IOException
    {
        systemSettingManager.saveSystemSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE, true );
        TrackerImportParams params = fromJson( "tracker/programrule/event_update_datavalue_different_value.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport importReport = trackerImportService.importTracker( params );

        assertHasOnlyWarnings( importReport, E1308 );
    }

    private void assignProgramRule()
    {
        ProgramRule programRule = createProgramRule( 'F', program, null,
            "d2:daysBetween('2019-01-28', d2:lastEventDate('ProgramRuleVariableA')) < 5" );
        programRuleService.addProgramRule( programRule );
        ProgramRuleAction programRuleAction = createProgramRuleAction( programRule, ASSIGN, dataElement1,
            "#{ProgramRuleVariableA}" );
        programRuleActionService.addProgramRuleAction( programRuleAction );
        programRule.getProgramRuleActions().add( programRuleAction );
        programRuleService.updateProgramRule( programRule );

        ProgramRule programRuleForOptionSet = createProgramRule( 'P', program, null,
            "d2:hasValue(#{test-de-optionset})" );
        programRuleService.addProgramRule( programRuleForOptionSet );
        ProgramRuleAction programRuleActionForOptionSet = createProgramRuleAction( programRuleForOptionSet, ASSIGN,
            dataElementToAssignValueTo,
            "#{test-de-optionset}" );
        programRuleActionService.addProgramRuleAction( programRuleActionForOptionSet );
        programRuleForOptionSet.getProgramRuleActions().add( programRuleActionForOptionSet );
        programRuleService.updateProgramRule( programRuleForOptionSet );
    }

    private ProgramRule createProgramRule( char uniqueCharacter, Program program, ProgramStage programStage,
        String condition )
    {
        ProgramRule programRule = createProgramRule( uniqueCharacter, program );
        programRule.setUid( "ProgramRul" + uniqueCharacter );
        programRule.setProgramStage( programStage );
        programRule.setCondition( condition );
        return programRule;
    }

    private ProgramRuleAction createProgramRuleAction( ProgramRule programRule, ProgramRuleActionType actionType,
        DataElement dataElement, String data )
    {
        ProgramRuleAction programRuleAction = createProgramRuleAction( 'A', programRule );
        programRuleAction.setProgramRuleActionType( actionType );
        programRuleAction.setContent( "CONTENT" );
        programRuleAction.setDataElement( dataElement );
        programRuleAction.setData( data );

        return programRuleAction;
    }
}
