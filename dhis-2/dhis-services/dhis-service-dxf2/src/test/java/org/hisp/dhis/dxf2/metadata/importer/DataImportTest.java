package org.hisp.dhis.dxf2.metadata.importer;
/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DataImportTest extends DhisSpringTest
{
    @Autowired
    private org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService teiDXF2Service;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService teiDBModelService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityAttributeService teaService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private DataElementService dataElementService;

    //terminology note: "enrollment" = "programInstance"
    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private IdentifiableObjectManager manager;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance teiA;

    private List<ProgramTrackedEntityAttribute> createProgramTrackedEntityAttributes()
    {
        TrackedEntityAttribute teAttributeA = createTrackedEntityAttribute( 'A' );
        teAttributeA.setUid( "w75KJ2mc4zz" );
        teAttributeA.setDimensionItemType( DimensionItemType.PROGRAM_ATTRIBUTE );

        ProgramTrackedEntityAttribute pteAttributeA = createProgramTrackedEntityAttribute( 'A' );
        pteAttributeA.setMandatory( true );
        pteAttributeA.setUid( "dbzpQYr5G2g" );
        pteAttributeA.setAttribute( teAttributeA );

        TrackedEntityAttribute teAttributeB = createTrackedEntityAttribute( 'B' );
        teAttributeB.setUid( "zDhUuAYrxNC" );
        teAttributeB.setDimensionItemType( DimensionItemType.PROGRAM_ATTRIBUTE );

        ProgramTrackedEntityAttribute pteAttributeB = createProgramTrackedEntityAttribute( 'B' );
        pteAttributeB.setMandatory( true );
        pteAttributeB.setUid( "aQQTyeGoWm4" );
        pteAttributeB.setAttribute( teAttributeB );

        TrackedEntityAttribute teAttributeC = createTrackedEntityAttribute( 'C' );
        teAttributeC.setUid( "cejWyOfXge6" );
        teAttributeC.setDimensionItemType( DimensionItemType.PROGRAM_ATTRIBUTE );

        ProgramTrackedEntityAttribute pteAttributeC = createProgramTrackedEntityAttribute( 'C' );
        pteAttributeC.setMandatory( true );
        pteAttributeC.setUid( "lxo3ITSkcpt" );
        pteAttributeC.setAttribute( teAttributeC );

        teaService.addTrackedEntityAttribute( teAttributeA );
        teaService.addTrackedEntityAttribute( teAttributeB );
        teaService.addTrackedEntityAttribute( teAttributeC );
        manager.save( pteAttributeA );
        manager.save( pteAttributeB );
        manager.save( pteAttributeC );

        return new ArrayList<>( Arrays.asList( pteAttributeA, pteAttributeB, pteAttributeC ) );
    }

    private Set<TrackedEntityAttributeValue> createTrackedEntityAttributeValues( org.hisp.dhis.trackedentity.TrackedEntityInstance tei )
    {

        TrackedEntityAttribute attrA = teaService.getTrackedEntityAttribute( "zDhUuAYrxNC" );
        TrackedEntityAttributeValue valA = createTrackedEntityAttributeValue( 'A', tei, attrA );
        valA.setValue( "Rufus" );

        TrackedEntityAttribute attrB = teaService.getTrackedEntityAttribute( "w75KJ2mc4zz" );
        TrackedEntityAttributeValue valB = createTrackedEntityAttributeValue( 'B', tei, attrB );
        valB.setValue( "Joe" );

        TrackedEntityAttribute attrC = teaService.getTrackedEntityAttribute( "cejWyOfXge6" );
        TrackedEntityAttributeValue valC = createTrackedEntityAttributeValue( 'C', tei, attrC );
        valC.setValue( "Male" );

        return Sets.newHashSet( valA, valB, valC );
    }

    @Override
    protected void setUpTest() throws Exception
    {
        OrganisationUnit organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitA.setUid( "DiszpKrYNg8" );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityType.setUid( "nEenWmSyUEp" );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );

        Program programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setUid( "VBqh0ynB2wv" );
        programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        programA.setTrackedEntityType( trackedEntityType );

        Program programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
        programB.setUid( "kla3mAPgvCH" );
        programB.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        programB.setTrackedEntityType( trackedEntityType );

        List<ProgramTrackedEntityAttribute> programAttributes = createProgramTrackedEntityAttributes();

        Program programC = createProgram( 'C', new HashSet<>(), organisationUnitA );
        programC.setUid( "ur1Edk5Oe2n" );
        programC.setProgramType( ProgramType.WITH_REGISTRATION );
        programC.setTrackedEntityType( trackedEntityType );
        programC.setProgramAttributes( programAttributes );

        teiA = createTrackedEntityInstance( 'A', organisationUnitA );
        teiA.setTrackedEntityType( trackedEntityType );
        Set<TrackedEntityAttributeValue> attributeValues = createTrackedEntityAttributeValues( teiA );
        teiA.setTrackedEntityAttributeValues( attributeValues );
        teiA.setUid( "epkxNvNLgjk" );

        organisationUnitService.addOrganisationUnit( organisationUnitA );
        teiDBModelService.addTrackedEntityInstance( teiA );
        programService.addProgram( programA );
        programService.addProgram( programB );
        programService.addProgram( programC );

        ProgramStage programStageA = createProgramStage( 'A', programC );
        programStageA.setUid( "EPEcjy3FWmI" );
        programStageA.setRepeatable( true );

        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setUid( "qrur9Dvnyt5" );
        dataElementA.setValueType( ValueType.INTEGER );
        dataElementService.addDataElement( dataElementA );

        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setUid( "oZg33kd9taw" );
        dataElementB.setValueType( ValueType.TEXT );
        dataElementService.addDataElement( dataElementB );

        programStageA.addDataElement( dataElementA, 1 );
        programStageA.addDataElement( dataElementB, 2 );

        programStageService.saveProgramStage( programStageA );

        programC.setProgramStages( Sets.newHashSet( programStageA ) );
        programService.updateProgram( programC );
    }


    //Test creation of 2 events -> Check whether there is a link to TEI and enrollment

    @Test
    public void testCreateSimpleTEI() throws IOException
    {
        InputStream is = new ClassPathResource( "dxf2/import/create_simple_tei.json" ).getInputStream();
        ImportOptions io = new ImportOptions();
        io.setStrategy( ImportStrategy.CREATE_AND_UPDATE );

        ImportSummaries summaries = teiDXF2Service.addTrackedEntityInstanceJson( is, io );

        assertEquals( ImportStatus.SUCCESS, summaries.getStatus() );

        for ( ImportSummary summary : summaries.getImportSummaries() )
        {
            assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
        }

        String teiUIDFromResponse = summaries.getImportSummaries().get( 0 ).getReference();
        org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance tei = teiDXF2Service.getTrackedEntityInstance( teiUIDFromResponse );

        assertEquals( tei.getTrackedEntityInstance(), teiUIDFromResponse );

//        checkResultsInTEISummaries( summaries );
    }


    //Test creation of simple TEI -> take the tei UID and create 3 enrollments -> take returned UID of each enrollment and check whether enrollments have a link to TEI
    @Test
    public void testCreateFirstSimpleTEIThen3EnrollmentsAndTestThatEnrollmentsHaveLinkToTEI() throws IOException
    {
        InputStream is =
            new ClassPathResource( "dxf2/import/create_3_enrollments_1_to_program_with_registration_2_to_program_without_registration.json" )
                .getInputStream();
        ImportOptions io = new ImportOptions();
        io.setStrategy( ImportStrategy.CREATE_AND_UPDATE );

        ImportSummaries summaries = enrollmentService.addEnrollmentsJson( is, io );

        checkResultsInEnrollments( summaries, teiA.getUid() );
    }

    //Test creation of TEI and 2-3 enrollments in 1 request -> take UIDs of all enrollments and check whether enrollments have a link to TEI
    //TODO: This test should actually fail (and should be fixed) when the validation logic for enrollments into programs without registration is fixed
    @Test
    public void testCreateTEIAnd3EnrollmentsAndTestThatEnrollmentsHaveLinkToTEI() throws IOException
    {
        InputStream is =
            new ClassPathResource( "dxf2/import/create_tei_and_3_enrollments_1_with_2_without_registration.json" )
                .getInputStream();
        ImportOptions io = new ImportOptions();
        io.setStrategy( ImportStrategy.CREATE_AND_UPDATE );

        ImportSummaries summaries = teiDXF2Service.addTrackedEntityInstanceJson( is, io );

        assertEquals( ImportStatus.SUCCESS, summaries.getStatus() );

        for ( ImportSummary summary : summaries.getImportSummaries() )
        {

            assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

            ImportSummaries enrollmentsSummaries = summary.getEnrollments();
            checkResultsInEnrollments( enrollmentsSummaries, summary.getReference() );
        }

        String teiUIDFromResponse = summaries.getImportSummaries().get( 0 ).getReference();
        org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance tei = teiDXF2Service.getTrackedEntityInstance( teiUIDFromResponse );

        assertEquals( tei.getTrackedEntityInstance(), teiUIDFromResponse );
    }

    //Test creation of TEI, 2-3 enrollments and 2 event in 1 request -> take UIDs of all enrollments and check whether they have a link to TEI, -> check whether events have a link to TEI and enrollment
    //TODO: This test should actually fail (and should be fixed) when the validation logic for enrollments into programs without registration is fixed
    @Test
    public void testCreateTEIAnd3EnrollmentsAnd2EventsAndTestThatEnrollmentsHaveLinkToTEIAndEventsHaveLinkToTEiAndEnrollment() throws IOException
    {
        InputStream is =
            new ClassPathResource( "dxf2/import/create_tei_3_enrollments_and_2_events.json" )
                .getInputStream();
        ImportOptions io = new ImportOptions();
        io.setStrategy( ImportStrategy.CREATE_AND_UPDATE );

        ImportSummaries summaries = teiDXF2Service.addTrackedEntityInstanceJson( is, io );

        assertEquals( ImportStatus.SUCCESS, summaries.getStatus() );

        for ( ImportSummary summary : summaries.getImportSummaries() )
        {

            assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

            ImportSummaries enrollmentsSummaries = summary.getEnrollments();
            checkResultsInEnrollments( enrollmentsSummaries, summary.getReference() );
        }

        String teiUIDFromResponse = summaries.getImportSummaries().get( 0 ).getReference();
        org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance tei = teiDXF2Service.getTrackedEntityInstance( teiUIDFromResponse );

        assertEquals( tei.getTrackedEntityInstance(), teiUIDFromResponse );
    }

    private void checkResultsInEnrollments( ImportSummaries summaries, String expectedTeiUid )
    {
        assertEquals( ImportStatus.SUCCESS, summaries.getStatus() );

        for ( ImportSummary summary : summaries.getImportSummaries() )
        {
            String enrollmentReference = summary.getReference();

            ProgramInstance pi = programInstanceService.getProgramInstance( enrollmentReference );

            assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
            assertEquals( expectedTeiUid, pi.getEntityInstance().getUid() );

            checkResultsInEvents( summary.getEvents(), expectedTeiUid, pi.getUid() );
        }
    }

    private void checkResultsInEvents( ImportSummaries summaries, String expectedTeiUid, String expectedEnrollmentUid )
    {
        assertEquals( ImportStatus.SUCCESS, summaries.getStatus() );

        for ( ImportSummary summary : summaries.getImportSummaries() )
        {
            String eventReference = summary.getReference();

            ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( eventReference );

            assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
            assertEquals( expectedEnrollmentUid, psi.getProgramInstance().getUid() );
            assertEquals( expectedTeiUid, psi.getProgramInstance().getEntityInstance().getUid() );
        }
    }

    //TODO: More tests can be added. For example tests that test that import fails when we want to make an enrollment with TEI UID specified into program without registration
}
