package org.hisp.dhis.programrule.engine;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.*;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;

import java.util.*;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
public class ProgramRuleEngineTest extends DhisSpringTest
{
    private Program programA;

    private Program programB;

    private Program programC;

    private ProgramStage programStageA;

    private ProgramStage programStageB;

    private ProgramRule programRuleA;

    private ProgramRuleAction programRuleActionA;

    private ProgramRuleAction programRuleActionB;

    private ProgramRuleVariable programRuleVariableA;

    private ProgramRuleVariable programRuleVariableB;

    private ProgramRule programRuleB;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramInstance programInstanceA;

    private ProgramStageInstance programStageInstanceA;

    private ProgramStageInstance programStageInstanceB;

    private TrackedEntityInstance entityInstanceA;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private ProgramStageDataElement programStageDataElementA;

    private ProgramStageDataElement programStageDataElementB;

    private TrackedEntityAttribute attributeA;

    private Collection<Integer> orgunitIds;

    private Date incidenDate;

    private Date enrollmentDate;

    private String expressionA = "#{ProgramRuleVariableA}=='malaria'";

    private String expressionB = "#{ProgramRuleVariableB}=='bcgdoze'";

    @Autowired
    ProgramRuleEngine programRuleEngine;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private TrackedEntityDataValueService trackedEntityDataValueService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    TrackedEntityAttributeService attributeService;

    @Override
    public void setUpTest()
    {
        dataElementA = createDataElement( 'A', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementB = createDataElement( 'B', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        attributeA = createTrackedEntityAttribute('B', ValueType.TEXT );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        attributeService.addTrackedEntityAttribute(attributeA);

        setupEnrollment();
        setupEvent();
        setupProgramRuleEngine();
    }

    @Test
    public void testEnrollment() throws Exception
    {
        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS1" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

        System.out.println("size" + ruleEffects.size());
    }

    @Test
    public void testEvent() throws Exception
    {
    }

    private void setupEnrollment()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        int idA = organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        int idB = organisationUnitService.addOrganisationUnit( organisationUnitB );

        orgunitIds = new HashSet<>();
        orgunitIds.add( idA );
        orgunitIds.add( idB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );

        programService.addProgram( programA );

        programStageA = createProgramStage( 'A', programA );
        programStageB = createProgramStage( 'B', programA );

        programStageDataElementA = createProgramStageDataElement( programStageA, dataElementA, 0 );
        programStageDataElementB = createProgramStageDataElement( programStageA, dataElementB, 1 );

        programStageDataElementService.addProgramStageDataElement( programStageDataElementA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );

        programStageA.setSortOrder( 1 );
        programStageB.setSortOrder( 2 );
        programStageA.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );
        programStageA.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB ) );

        programStageB.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );
        programStageB.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB ) );

        programStageService.saveProgramStage( programStageA );
        programStageService.saveProgramStage( programStageB );

        programA.setProgramStages( Sets.newHashSet( programStageA, programStageB ) );
        programService.updateProgram( programA );

        programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
        programService.addProgram( programB );

        programC = createProgram( 'C', new HashSet<>(), organisationUnitA );
        programService.addProgram( programC );

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        TrackedEntityInstance entityInstanceB = createTrackedEntityInstance( 'B', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidenDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();

        programInstanceA = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceA, programA );
        programInstanceA.setUid( "UID-P" );
        programInstanceA.setOrganisationUnit( organisationUnitA );

        programInstanceService.addProgramInstance( programInstanceA );

        programStageInstanceA = new ProgramStageInstance( programInstanceA, programStageA );
        programStageInstanceA.setDueDate( enrollmentDate );
        programStageInstanceA.setExecutionDate( new Date() );
        programStageInstanceA.setUid( "UID-PS1" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        programStageInstanceB = new ProgramStageInstance( programInstanceA, programStageB );
        programStageInstanceB.setDueDate( enrollmentDate );
        programStageInstanceB.setExecutionDate( new Date() );
        programStageInstanceB.setUid( "UID-PS2" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        ProgramStageInstance programStageInstanceTempA = programStageInstanceService.getProgramStageInstance( "UID-PS1" );
        ProgramStageInstance programStageInstanceTempB = programStageInstanceService.getProgramStageInstance( "UID-PS2" );

        TrackedEntityDataValue diagnosis = new TrackedEntityDataValue( programStageInstanceTempA, dataElementA, "malaria" );
        TrackedEntityDataValue bcgDoze = new TrackedEntityDataValue( programStageInstanceTempA, dataElementB, "bcgdoze" );

        programStageInstanceTempA.setDataValues( Sets.newHashSet( diagnosis, bcgDoze ) );
        programStageInstanceTempB.setDataValues( Sets.newHashSet( diagnosis ) );

        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempA );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempB );

        programInstanceA.getProgramStageInstances().addAll( Sets.newHashSet( programStageInstanceA, programStageInstanceB ) );
        programInstanceService.updateProgramInstance( programInstanceA );

        trackedEntityDataValueService.saveTrackedEntityDataValue( diagnosis );
        trackedEntityDataValueService.saveTrackedEntityDataValue( bcgDoze );

    }

    private void setupEvent()
    {

    }

    private void setupProgramRuleEngine()
    {
        programRuleA = createProgramRule( 'A', programA );
        programRuleA.setCondition( expressionA );
        programRuleService.addProgramRule( programRuleA );

        programRuleB = createProgramRule( 'B', programA );
        programRuleB.setCondition( expressionB );
        programRuleService.addProgramRule( programRuleB );

        programRuleActionA = createProgramRuleAction( 'A', programRuleA );
        programRuleActionA.setProgramRuleActionType( ProgramRuleActionType.DISPLAYTEXT );
        programRuleActionA.setContent("DUMMY-CONTENT");
        programRuleActionA.setData("NO-EXPRESSION");
        programRuleActionService.addProgramRuleAction( programRuleActionA );

        programRuleActionB = createProgramRuleAction( 'A', programRuleB );
        programRuleActionB.setProgramRuleActionType( ProgramRuleActionType.ASSIGN );
        programRuleActionB.setContent("DUMMY-CONTENT2");
        programRuleActionB.setData("NO-EXPRESSION2");
        programRuleActionService.addProgramRuleAction( programRuleActionB );

        programRuleVariableA = createProgramRuleVariable( 'A', programA );
        programRuleVariableA.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableA.setDataElement( dataElementA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableA );

        programRuleVariableB = createProgramRuleVariable( 'B', programA );
        programRuleVariableB.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableB.setDataElement( dataElementB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableB );

        programRuleA.setProgramRuleActions( Sets.newHashSet( programRuleActionA ) );
        programRuleB.setProgramRuleActions( Sets.newHashSet( programRuleActionB ) );
        programRuleService.updateProgramRule( programRuleA );
        programRuleService.updateProgramRule( programRuleB );

    }
}