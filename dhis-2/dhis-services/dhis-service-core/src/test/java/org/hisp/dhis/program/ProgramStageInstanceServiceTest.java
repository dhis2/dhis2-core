/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.program;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.audit.UserInfoTestHelper;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.TestCache;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.mock.MockI18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ProgramStageInstanceServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private TrackedEntityDataValueAuditService dataValueAuditService;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramStage stageA;

    private ProgramStage stageB;

    private ProgramStage stageC;

    private ProgramStage stageD;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private ProgramStageDataElement stageDataElementA;

    private ProgramStageDataElement stageDataElementB;

    private ProgramStageDataElement stageDataElementC;

    private ProgramStageDataElement stageDataElementD;

    private Date incidenDate;

    private Date enrollmentDate;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramStageInstance programStageInstanceA;

    private ProgramStageInstance programStageInstanceB;

    private ProgramStageInstance programStageInstanceC;

    private ProgramStageInstance programStageInstanceD1;

    private ProgramStageInstance programStageInstanceD2;

    private TrackedEntityInstance entityInstanceA;

    private TrackedEntityInstance entityInstanceB;

    private Program programA;

    private MockI18nFormat mockFormat;

    private EventDataValue eventDataValueA;

    private EventDataValue eventDataValueB;

    private EventDataValue eventDataValueC;

    private EventDataValue eventDataValueD;

    private Cache<DataElement> dataElementMap = new TestCache<>();

    private List<DataElement> dataElements;

    @Override
    public void setUpTest()
    {
        mockFormat = new MockI18nFormat();

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );

        entityInstanceA = createTrackedEntityInstance( organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        entityInstanceB = createTrackedEntityInstance( organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        attribute.setValueType( ValueType.PHONE_NUMBER );
        attributeService.addTrackedEntityAttribute( attribute );

        TrackedEntityAttributeValue attributeValue = createTrackedEntityAttributeValue( 'A', entityInstanceA,
            attribute );
        attributeValue.setValue( "123456789" );
        attributeValueService.addTrackedEntityAttributeValue( attributeValue );

        entityInstanceA.getTrackedEntityAttributeValues().add( attributeValue );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA );

        /**
         * Program A
         */
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programService.addProgram( programA );

        stageA = createProgramStage( 'A', 0 );
        stageA.setProgram( programA );
        stageA.setSortOrder( 1 );

        programStageService.saveProgramStage( stageA );

        stageB = new ProgramStage( "B", programA );
        stageB.setSortOrder( 2 );

        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        stageDataElementA = new ProgramStageDataElement( stageA, dataElementA, false, 1 );
        stageDataElementB = new ProgramStageDataElement( stageA, dataElementB, false, 2 );
        stageDataElementC = new ProgramStageDataElement( stageB, dataElementA, false, 1 );
        stageDataElementD = new ProgramStageDataElement( stageB, dataElementB, false, 2 );

        programStageDataElementService.addProgramStageDataElement( stageDataElementA );
        programStageDataElementService.addProgramStageDataElement( stageDataElementB );
        programStageDataElementService.addProgramStageDataElement( stageDataElementC );
        programStageDataElementService.addProgramStageDataElement( stageDataElementD );

        /*
         * Program B
         */

        Program programB = createProgram( 'B', new HashSet<>(), organisationUnitB );
        programService.addProgram( programB );

        stageC = new ProgramStage( "C", programB );
        stageC.setSortOrder( 1 );
        programStageService.saveProgramStage( stageC );

        stageD = new ProgramStage( "D", programB );
        stageB.setSortOrder( 2 );
        stageC.setRepeatable( true );
        programStageService.saveProgramStage( stageD );

        programStages = new HashSet<>();
        programStages.add( stageC );
        programStages.add( stageD );
        programB.setProgramStages( programStages );
        programService.updateProgram( programB );

        /**
         * Program Instance and Program Stage Instance
         */

        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidenDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();

        programInstanceA = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceA, programA );
        programInstanceA.setUid( "UID-PIA" );
        programInstanceService.addProgramInstance( programInstanceA );

        programInstanceB = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceB, programB );
        programInstanceService.addProgramInstance( programInstanceB );

        programStageInstanceA = new ProgramStageInstance( programInstanceA, stageA );
        programStageInstanceA.setDueDate( enrollmentDate );
        programStageInstanceA.setUid( "UID-A" );

        programStageInstanceB = new ProgramStageInstance( programInstanceA, stageB );
        programStageInstanceB.setDueDate( enrollmentDate );
        programStageInstanceB.setUid( "UID-B" );

        programStageInstanceC = new ProgramStageInstance( programInstanceB, stageC );
        programStageInstanceC.setDueDate( enrollmentDate );
        programStageInstanceC.setUid( "UID-C" );

        programStageInstanceD1 = new ProgramStageInstance( programInstanceB, stageD );
        programStageInstanceD1.setDueDate( enrollmentDate );
        programStageInstanceD1.setUid( "UID-D1" );

        programStageInstanceD2 = new ProgramStageInstance( programInstanceB, stageD );
        programStageInstanceD2.setDueDate( enrollmentDate );
        programStageInstanceD2.setUid( "UID-D2" );

        /*
         * Prepare data for EventDataValues manipulation tests
         */

        programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        // Check that there are no EventDataValues assigned to PSI
        ProgramStageInstance tempPsiA = programStageInstanceService
            .getProgramStageInstance( programStageInstanceA.getUid() );
        assertEquals( 0, tempPsiA.getEventDataValues().size() );

        // Prepare EventDataValues to manipulate with
        String storedBy = "test-user";
        eventDataValueA = new EventDataValue( dataElementA.getUid(), "1", UserInfoTestHelper.testUserInfo( storedBy ) );
        eventDataValueB = new EventDataValue( dataElementB.getUid(), "2", UserInfoTestHelper.testUserInfo( storedBy ) );
        eventDataValueC = new EventDataValue( dataElementC.getUid(), "3", UserInfoTestHelper.testUserInfo( storedBy ) );
        eventDataValueD = new EventDataValue( dataElementD.getUid(), "4", UserInfoTestHelper.testUserInfo( storedBy ) );

        dataElementMap.put( dataElementA.getUid(), dataElementA );
        dataElementMap.put( dataElementB.getUid(), dataElementB );
        dataElementMap.put( dataElementC.getUid(), dataElementC );
        dataElementMap.put( dataElementD.getUid(), dataElementD );

        dataElements = dataElementMap.getAll().collect( toList() );
    }

    @Test
    public void testAddProgramStageInstance()
    {
        long idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        long idB = programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        assertNotNull( programStageInstanceService.getProgramStageInstance( idA ) );
        assertNotNull( programStageInstanceService.getProgramStageInstance( idB ) );
    }

    @Test
    public void testDeleteProgramStageInstance()
    {
        long idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        long idB = programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        assertNotNull( programStageInstanceService.getProgramStageInstance( idA ) );
        assertNotNull( programStageInstanceService.getProgramStageInstance( idB ) );

        programStageInstanceService.deleteProgramStageInstance( programStageInstanceA );

        assertNull( programStageInstanceService.getProgramStageInstance( idA ) );
        assertNotNull( programStageInstanceService.getProgramStageInstance( idB ) );

        programStageInstanceService.deleteProgramStageInstance( programStageInstanceB );

        assertNull( programStageInstanceService.getProgramStageInstance( idA ) );
        assertNull( programStageInstanceService.getProgramStageInstance( idB ) );
    }

    @Test
    public void testUpdateProgramStageInstance()
    {
        long idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        assertNotNull( programStageInstanceService.getProgramStageInstance( idA ) );

        programStageInstanceA.setName( "B" );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceA );

        assertEquals( "B", programStageInstanceService.getProgramStageInstance( idA ).getName() );
    }

    @Test
    public void testGetProgramStageInstanceById()
    {
        long idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        long idB = programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        assertEquals( programStageInstanceA, programStageInstanceService.getProgramStageInstance( idA ) );
        assertEquals( programStageInstanceB, programStageInstanceService.getProgramStageInstance( idB ) );
    }

    @Test
    public void testGetProgramStageInstanceByUid()
    {
        long idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        long idB = programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        assertEquals( programStageInstanceA, programStageInstanceService.getProgramStageInstance( idA ) );
        assertEquals( programStageInstanceB, programStageInstanceService.getProgramStageInstance( idB ) );

        assertEquals( programStageInstanceA, programStageInstanceService.getProgramStageInstance( "UID-A" ) );
        assertEquals( programStageInstanceB, programStageInstanceService.getProgramStageInstance( "UID-B" ) );
    }

    @Test
    public void testGetProgramStageInstanceByProgramInstanceStage()
    {
        programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance(
            programInstanceA, stageA );
        assertEquals( programStageInstanceA, programStageInstance );

        programStageInstance = programStageInstanceService.getProgramStageInstance( programInstanceA, stageB );
        assertEquals( programStageInstanceB, programStageInstance );
    }

    @Test
    public void testCompleteProgramStageInstance()
    {
        long idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        programStageInstanceService.completeProgramStageInstance( programStageInstanceA, true, mockFormat, null );

        assertEquals( true, programStageInstanceService.getProgramStageInstance( idA ).isCompleted() );
    }

    @Test
    public void testEventDataValuesSave()
    {
        addInitialEventDataValues();

        // Check that there are 4 EventDataValues
        ProgramStageInstance tempPsiA = programStageInstanceService
            .getProgramStageInstance( programStageInstanceA.getUid() );
        assertEquals( 4, tempPsiA.getEventDataValues().size() );

        // Check that there are 4 audits of CREATE type
        long auditCreateCount = dataValueAuditService.countTrackedEntityDataValueAudits( dataElements,
            Collections.singletonList( programStageInstanceA ), AuditType.CREATE );
        assertEquals( 4, auditCreateCount );

        // Fetch value of the EventDataValueB and compare that it is correct
        String eventDataValueBValue = tempPsiA.getEventDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementB.getUid() ) )
            .findFirst()
            .get()
            .getValue();

        assertEquals( "2", eventDataValueBValue );

        // Fetch value of the EventDataValueC and compare that it is correct
        String eventDataValueCValue = tempPsiA.getEventDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementC.getUid() ) )
            .findFirst()
            .get()
            .getValue();

        assertEquals( "3", eventDataValueCValue );
    }

    @Test
    public void testEventDataValuesUpdateAndDeletion()
    {
        addInitialEventDataValues();

        // Prepare data for update and delete. eventDataValueA has to be put
        // into updated because I am accessing DAO layer directly, so I am
        // skipping check from DTO layer
        eventDataValueB.setValue( "12" );
        eventDataValueC.setValue( "13" );
        eventDataValueD.setValue( "14" );

        Set<EventDataValue> updatedEventDataValues = new HashSet<>(
            Arrays.asList( eventDataValueA, eventDataValueB, eventDataValueC ) );

        // Update PSI: create 0, update 3, delete 1
        programStageInstanceService.auditDataValuesChangesAndHandleFileDataValues( Collections.emptySet(),
            updatedEventDataValues, Collections.singleton( eventDataValueD ),
            convertToMap( dataElementMap ), programStageInstanceA, false );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceA );

        // Check that there are 3 EventDataValues
        ProgramStageInstance tempPsiA = programStageInstanceService
            .getProgramStageInstance( programStageInstanceA.getUid() );
        assertEquals( 3, tempPsiA.getEventDataValues().size() );

        // Check that there are 4 audits of CREATE type, 3 of UPDATE type and 1
        // of DELETE type
        long auditCreateCount = dataValueAuditService.countTrackedEntityDataValueAudits( dataElements,
            Collections.singletonList( programStageInstanceA ), AuditType.CREATE );
        assertEquals( 4, auditCreateCount );

        long auditUpdateCount = dataValueAuditService.countTrackedEntityDataValueAudits( dataElements,
            Collections.singletonList( programStageInstanceA ), AuditType.UPDATE );
        assertEquals( 3, auditUpdateCount );

        long auditDeleteCount = dataValueAuditService.countTrackedEntityDataValueAudits( dataElements,
            Collections.singletonList( programStageInstanceA ), AuditType.DELETE );
        assertEquals( 1, auditDeleteCount );

        // Fetch value of the EventDataValueB and compare that it is correct
        String eventDataValueBValue = tempPsiA.getEventDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementB.getUid() ) )
            .findFirst()
            .get()
            .getValue();

        assertEquals( "12", eventDataValueBValue );

        // Fetch value of the EventDataValueC and compare that it is correct
        String eventDataValueCValue = tempPsiA.getEventDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementC.getUid() ) )
            .findFirst()
            .get()
            .getValue();

        assertEquals( "13", eventDataValueCValue );
    }

    @Test
    public void testEventDataValuesSingleValueUpdate()
    {
        addInitialEventDataValues();

        // Update 1 EventDataVaue and run a "SingleValue" update and check that
        // others, not mentioned, EventDataValues are not touched
        eventDataValueB.setValue( "22" );

        programStageInstanceService.auditDataValuesChangesAndHandleFileDataValues( Collections.emptySet(),
            Collections.singleton( eventDataValueB ), Collections.emptySet(),
            convertToMap( dataElementMap ), programStageInstanceA, true );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceA );

        // Check that there are 4 EventDataValues
        ProgramStageInstance tempPsiA = programStageInstanceService
            .getProgramStageInstance( programStageInstanceA.getUid() );
        assertEquals( 4, tempPsiA.getEventDataValues().size() );

        // Check that there are 4 audits of CREATE type, 3 of UPDATE type and 1
        // of DELETE type
        long auditCreateCount = dataValueAuditService.countTrackedEntityDataValueAudits( dataElements,
            Collections.singletonList( programStageInstanceA ), AuditType.CREATE );
        assertEquals( 4, auditCreateCount );

        long auditUpdateCount = dataValueAuditService.countTrackedEntityDataValueAudits( dataElements,
            Collections.singletonList( programStageInstanceA ), AuditType.UPDATE );
        assertEquals( 1, auditUpdateCount );

        long auditDeleteCount = dataValueAuditService.countTrackedEntityDataValueAudits( dataElements,
            Collections.singletonList( programStageInstanceA ), AuditType.DELETE );
        assertEquals( 0, auditDeleteCount );

        // Fetch value of the EventDataValueB and compare that it is correct
        String eventDataValueBValue = tempPsiA.getEventDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementB.getUid() ) )
            .findFirst()
            .get()
            .getValue();

        assertEquals( "22", eventDataValueBValue );

        // Fetch value of the EventDataValueC and compare that it is correct
        String eventDataValueCValue = tempPsiA.getEventDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementC.getUid() ) )
            .findFirst()
            .get()
            .getValue();

        assertEquals( "3", eventDataValueCValue );
    }

    private Map<String, DataElement> convertToMap( Cache<DataElement> dataElementMap )
    {
        return dataElementMap.getAll()
            .collect( Collectors.toMap( DataElement::getUid, d -> d ) );
    }

    private void addInitialEventDataValues()
    {
        // Check that there are no EventDataValues assigned to PSI
        ProgramStageInstance tempPsiA = programStageInstanceService
            .getProgramStageInstance( programStageInstanceA.getUid() );
        assertEquals( 0, tempPsiA.getEventDataValues().size() );

        // Prepare EventDataValues -> save 4 of them to PSI
        Set<EventDataValue> newEventDataValues = new HashSet<>(
            Arrays.asList( eventDataValueA, eventDataValueB, eventDataValueC, eventDataValueD ) );
        programStageInstanceService.auditDataValuesChangesAndHandleFileDataValues( newEventDataValues,
            Collections.emptySet(), Collections.emptySet(),
            convertToMap( dataElementMap ), programStageInstanceA, false );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceA );
    }
}
