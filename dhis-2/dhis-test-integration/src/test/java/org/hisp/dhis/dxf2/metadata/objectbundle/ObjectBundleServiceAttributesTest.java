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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class ObjectBundleServiceAttributesTest extends IntegrationTestBase
{

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private AttributeService attributeService;

    @Override
    public void setUpTest()
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    void testCreateSimpleMetadataAttributeValuesUID()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/simple_metadata_with_av.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertFalse( validationReport.hasErrorReports() );
        transactionTemplate.execute( status -> {
            objectBundleService.commit( bundle );
            return null;
        } );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<Option> options = manager.getAll( Option.class );
        List<OptionSet> optionSets = manager.getAll( OptionSet.class );
        List<Attribute> attributes = manager.getAll( Attribute.class );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( dataSets.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 2, attributes.size() );
        assertEquals( 2, options.size() );
        assertEquals( 1, optionSets.size() );
        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = manager.getDefaults();
        DataSet dataSet = dataSets.get( 0 );
        User user = users.get( 0 );
        OptionSet optionSet = optionSets.get( 0 );
        for ( DataElement dataElement : dataElements )
        {
            assertNotNull( dataElement.getCategoryCombo() );
            assertEquals( defaults.get( CategoryCombo.class ), dataElement.getCategoryCombo() );
        }
        assertFalse( dataSet.getSources().isEmpty() );
        assertFalse( dataSet.getDataSetElements().isEmpty() );
        assertEquals( 1, dataSet.getSources().size() );
        assertEquals( 2, dataSet.getDataSetElements().size() );
        assertEquals( PeriodType.getPeriodTypeByName( "Monthly" ), dataSet.getPeriodType() );
        assertNotNull( user );
        assertEquals( "admin", user.getUsername() );
        assertFalse( user.getUserRoles().isEmpty() );
        assertFalse( user.getOrganisationUnits().isEmpty() );
        assertEquals( "PdWlltZnVZe", user.getOrganisationUnit().getUid() );
        assertEquals( 2, optionSet.getOptions().size() );
        // attribute value check
        DataElement dataElementA = manager.get( DataElement.class, "SG4HuKlNEFH" );
        DataElement dataElementB = manager.get( DataElement.class, "CCwk5Yx440o" );
        DataElement dataElementC = manager.get( DataElement.class, "j5PneRdU7WT" );
        DataElement dataElementD = manager.get( DataElement.class, "k90AVpBahO4" );
        assertNotNull( dataElementA );
        assertNotNull( dataElementB );
        assertNotNull( dataElementC );
        assertNotNull( dataElementD );
        assertTrue( dataElementA.getAttributeValues().isEmpty() );
        assertTrue( dataElementB.getAttributeValues().isEmpty() );
        assertFalse( dataElementC.getAttributeValues().isEmpty() );
        assertFalse( dataElementD.getAttributeValues().isEmpty() );
    }

    @Test
    void testValidateMetadataAttributeValuesMandatory()
        throws IOException
    {
        defaultSetupWithAttributes();
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_mandatory_attributes.json" ).getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        TypeReport typeReport = validationReport.getTypeReport( DataElement.class );
        assertNotNull( typeReport );
        assertEquals( 4, typeReport.getObjectReportsCount() );
        typeReport.forEachObjectReport( objectReport -> assertEquals( 2, objectReport.getErrorReportsCount() ) );
    }

    @Test
    void testValidateMetadataAttributeValuesMandatoryFromPayload()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_mandatory_attributes_from_payload_only.json" )
                .getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        TypeReport typeReport = validationReport.getTypeReport( DataElement.class );
        assertNotNull( typeReport );
        assertEquals( 4, typeReport.getObjectReportsCount() );
        typeReport.forEachObjectReport( objectReport -> assertEquals( 1, objectReport.getErrorReportsCount() ) );
    }

    @Test
    void testValidateMetadataAttributeValuesUnique()
        throws IOException
    {
        defaultSetupWithAttributes();
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_unique_attributes.json" ).getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        TypeReport typeReport = validationReport.getTypeReport( DataElement.class );
        assertNotNull( typeReport );
        assertEquals( 2, validationReport.getErrorReportsCountByCode( DataElement.class, ErrorCode.E4009 ) );
    }

    @Test
    void testValidateMetadataAttributeValuesUniqueFromPayload()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_unique_attributes_from_payload.json" ).getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertNotNull( validationReport.getTypeReport( DataElement.class ) );
        assertEquals( 2, validationReport.getErrorReportsCountByCode( DataElement.class, ErrorCode.E4009 ) );
    }

    private void defaultSetupWithAttributes()
    {
        Attribute attribute = new Attribute( "AttributeA", ValueType.TEXT );
        attribute.setUid( "d9vw7V9Mw8W" );
        attribute.setUnique( true );
        attribute.setMandatory( true );
        attribute.setDataElementAttribute( true );
        manager.save( attribute );
        AttributeValue attributeValue1 = new AttributeValue( "Value1", attribute );
        AttributeValue attributeValue2 = new AttributeValue( "Value2", attribute );
        AttributeValue attributeValue3 = new AttributeValue( "Value3", attribute );
        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );
        transactionTemplate.execute( status -> {
            manager.save( de1 );
            manager.save( de2 );
            manager.save( de3 );
            attributeService.addAttributeValue( de1, attributeValue1 );
            attributeService.addAttributeValue( de2, attributeValue2 );
            attributeService.addAttributeValue( de3, attributeValue3 );
            manager.clear();
            return null;
        } );
        User user = makeUser( "A" );
        manager.save( user );
    }
}
