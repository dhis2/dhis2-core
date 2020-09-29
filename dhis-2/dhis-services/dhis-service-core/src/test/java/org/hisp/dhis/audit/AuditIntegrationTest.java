package org.hisp.dhis.audit;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ActiveProfiles( profiles = { "test-audit" } )
public class AuditIntegrationTest
    extends IntegrationTestBase
{
    private static final int TIMEOUT = 5;

    @Autowired
    private AuditService auditService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Override
    protected void setUpTest()
        throws Exception
    {
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Test
    public void testSaveMetadata()
    {
        DataElement dataElement = createDataElement( 'A' );
        dataElementService.addDataElement( dataElement );

        AuditQuery query = AuditQuery.builder()
            .uid( Sets.newHashSet( dataElement.getUid() ) )
            .build();

        await().atMost( TIMEOUT, TimeUnit.SECONDS ).until( () -> auditService.countAudits( query ) > 0 );

        List<Audit> audits = auditService.getAudits( query );

        assertEquals( 1, audits.size() );
        Audit audit = audits.get( 0 );
        assertEquals( DataElement.class.getName(), audit.getKlass() );
        assertEquals( dataElement.getUid(), audit.getAttributes().get( "uid" ) );
        assertNotNull( audit.getData() );
    }

    @Test
    public void testSaveTrackedEntityInstance()
    {
        OrganisationUnit ou = createOrganisationUnit( 'A' );
        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        manager.save( ou );
        manager.save( attribute );

        TrackedEntityInstance tei = createTrackedEntityInstance( 'A', ou, attribute );
        trackedEntityInstanceService.addTrackedEntityInstance( tei );

        AuditQuery query = AuditQuery.builder()
            .uid( Sets.newHashSet( tei.getUid() ) )
            .build();
        await().atMost( TIMEOUT, TimeUnit.SECONDS ).until( () -> auditService.countAudits( query ) > 0 );

        List<Audit> audits = auditService.getAudits( query );

        assertEquals( 1, audits.size() );
        Audit audit = audits.get( 0 );
        assertEquals( TrackedEntityInstance.class.getName(), audit.getKlass() );
        assertEquals( tei.getUid(), audit.getUid() );
        assertEquals( tei.getUid(), audit.getAttributes().get( "uid" ) );
        assertNotNull( audit.getData() );
    }

    @Test
    public void testSaveTrackedAttributeValue()
    {
        OrganisationUnit ou = createOrganisationUnit( 'A' );
        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        manager.save( ou );
        manager.save( attribute );

        TrackedEntityInstance tei = createTrackedEntityInstance( 'A', ou, attribute );
        trackedEntityInstanceService.addTrackedEntityInstance( tei );

        TrackedEntityAttributeValue dataValue = createTrackedEntityAttributeValue(
            'A', tei, attribute );

        attributeValueService.addTrackedEntityAttributeValue( dataValue );

        AuditAttributes attributes = new AuditAttributes();
        attributes.put( "attribute", attribute.getUid() );
        attributes.put( "entityInstance", tei.getUid() );

        AuditQuery query = AuditQuery.builder()
            .auditAttributes( attributes )
            .build();
        await().atMost( TIMEOUT, TimeUnit.SECONDS ).until( () -> auditService.countAudits( query ) > 0 );

        List<Audit> audits = auditService.getAudits( query );

        assertEquals( 1, audits.size() );
        Audit audit = audits.get( 0 );
        assertEquals( TrackedEntityAttributeValue.class.getName(), audit.getKlass() );
        assertEquals( attribute.getUid(), audit.getAttributes().get( "attribute" ) );
        assertEquals( tei.getUid(), audit.getAttributes().get( "entityInstance" ) );
        assertNotNull( audit.getData() );
    }

    @Test
    public void testSaveAggregateDataValue()
    {
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );


        Period periodA = createPeriod( getDay( 5 ), getDay( 6 ) );
        Period periodB = createPeriod( getDay( 6 ), getDay( 7 ) );
        Period periodC = createPeriod( getDay( 7 ), getDay( 8 ) );
        Period periodD = createPeriod( getDay( 8 ), getDay( 9 ) );

        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        periodService.addPeriod( periodD );

        OrganisationUnit orgUnitA = createOrganisationUnit( 'A' );
        OrganisationUnit orgUnitB = createOrganisationUnit( 'B' );
        OrganisationUnit orgUnitC = createOrganisationUnit( 'C' );
        OrganisationUnit orgUnitD = createOrganisationUnit( 'D' );

        manager.save( orgUnitA );
        manager.save( orgUnitB );
        manager.save( orgUnitC );
        manager.save( orgUnitD );

        CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo();
        categoryService.addCategoryOptionCombo( optionCombo );

        DataValue dataValueA = createDataValue( dataElementA, periodA, orgUnitA, "1",
            optionCombo );
        DataValue dataValueB = createDataValue( dataElementB, periodB, orgUnitB, "2", optionCombo );
        DataValue dataValueC = createDataValue( dataElementC, periodC, orgUnitC, "3", optionCombo );
        DataValue dataValueD = createDataValue( dataElementD, periodD, orgUnitD, "4", optionCombo );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );

        AuditAttributes attributes = new AuditAttributes();
        attributes.put( "dataElement", dataElementA.getUid() );

        AuditQuery query = AuditQuery.builder()
            .auditAttributes( attributes )
            .build();
        await().atMost( TIMEOUT, TimeUnit.SECONDS ).until( () -> auditService.countAudits( query ) > 0 );

        List<Audit> audits = auditService.getAudits( query );

        assertEquals( 1, audits.size() );
        Audit audit = audits.get( 0 );
        assertEquals( DataValue.class.getName(), audit.getKlass() );
        assertEquals( dataElementA.getUid(), audit.getAttributes().get( "dataElement" ) );
        assertNotNull( audit.getData() );
    }
}
