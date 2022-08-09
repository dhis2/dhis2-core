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
package org.hisp.dhis.dxf2.metadata.attribute;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.MetadataAttributeCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationContext;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.attribute.DefaultAttributeValidator;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;

@ExtendWith( MockitoExtension.class )
class MetadataAttributeCheckTest
{
    @Mock
    private IdentifiableObjectManager manager;

    @Mock
    private UserService userService;

    private OrganisationUnit organisationUnit;

    private Attribute attribute;

    private ValidationContext validationContext;

    private ObjectBundle objectBundle;

    private Preheat preheat;

    private MetadataAttributeCheck metadataAttributeCheck;

    private DefaultAttributeValidator attributeValidator;

    @BeforeEach
    void setUpTest()
    {
        organisationUnit = new OrganisationUnit();
        organisationUnit.setName( "A" );
        attribute = new Attribute();
        attribute.setUid( "attributeID" );
        attribute.setName( "attributeA" );
        attribute.setOrganisationUnitAttribute( true );
        attribute.setValueType( ValueType.INTEGER );

        validationContext = Mockito.mock( ValidationContext.class );
        Schema schema = new Schema( OrganisationUnit.class, "organisationUnit", "organisationUnits" );
        Property property = new Property();
        property.setPersisted( true );
        schema.getPropertyMap().put( BaseIdentifiableObject_.ATTRIBUTE_VALUES, property );
        SchemaService schemaService = Mockito.mock( SchemaService.class );

        when( schemaService.getDynamicSchema( OrganisationUnit.class ) ).thenReturn( schema );
        when( validationContext.getSchemaService() ).thenReturn( schemaService );

        preheat = Mockito.mock( Preheat.class );
        when( preheat.getAttributesByClass( OrganisationUnit.class ) )
            .thenReturn( Map.of( attribute.getUid(), attribute ) );

        objectBundle = Mockito.mock( ObjectBundle.class );
        when( objectBundle.getPreheat() ).thenReturn( preheat );

        attributeValidator = new DefaultAttributeValidator( manager, userService );
        metadataAttributeCheck = new MetadataAttributeCheck( attributeValidator );
    }

    @Test
    void testAttributeAssigned()
    {
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "10" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotAssigned()
    {
        attribute.setOrganisationUnitAttribute( false );

        // OrganisationUnit doesn't have any attribute assigned
        when( preheat.getAttributesByClass( OrganisationUnit.class ) ).thenReturn( Map.of() );

        // Import OrganisationUnit with an AttributeValue
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "10" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();
        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6012, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeNotInteger()
    {
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "10.1" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6006, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeInteger()
    {
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "11" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotPositiveInteger()
    {
        attribute.setValueType( ValueType.INTEGER_POSITIVE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "-10" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6007, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributePositiveInteger()
    {
        attribute.setValueType( ValueType.INTEGER_POSITIVE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "10" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotZeroPositiveInteger()
    {
        attribute.setValueType( ValueType.INTEGER_ZERO_OR_POSITIVE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "-10" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6009, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeZeroPositiveInteger()
    {
        attribute.setValueType( ValueType.INTEGER_ZERO_OR_POSITIVE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "0" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotNumber()
    {
        attribute.setValueType( ValueType.NUMBER );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "Aaa" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6008, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeNumber()
    {
        attribute.setValueType( ValueType.NUMBER );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "123" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotNegativeInteger()
    {
        attribute.setValueType( ValueType.INTEGER_NEGATIVE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "10" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6013, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeNegativeInteger()
    {
        attribute.setValueType( ValueType.INTEGER_NEGATIVE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "-10" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotPercentage()
    {
        attribute.setValueType( ValueType.PERCENTAGE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "101" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6010, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributePercentage()
    {
        attribute.setValueType( ValueType.PERCENTAGE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "100" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotUnitInterval()
    {
        attribute.setValueType( ValueType.UNIT_INTERVAL );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "2" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6011, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeUnitInterval()
    {
        attribute.setValueType( ValueType.UNIT_INTERVAL );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "1" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotOrganisationUnit()
    {
        attribute.setValueType( ValueType.ORGANISATION_UNIT );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "Invalid-OU-ID" ) );

        // OrganisationUnit doesn't exist
        when( manager.get( OrganisationUnit.class, "Invalid-OU-ID" ) ).thenReturn( null );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6019, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeOrganisationUnit()
    {
        attribute.setValueType( ValueType.ORGANISATION_UNIT );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "OU-ID" ) );

        // OrganisationUnit exists
        when( manager.get( OrganisationUnit.class, "OU-ID" ) ).thenReturn( new OrganisationUnit() );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testAttributeNotFileResource()
    {
        attribute.setValueType( ValueType.FILE_RESOURCE );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "FileResourceID" ) );

        // FileResource doesn't exist
        when( manager.get( FileResource.class, "FileResourceID" ) ).thenReturn( null );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6019, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeUserNameNotExist()
    {
        attribute.setValueType( ValueType.USERNAME );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "userNameA" ) );

        // User doesn't exist
        when( userService.getUserByUsername( "userNameA" ) ).thenReturn( null );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6020, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testAttributeUserNameExist()
    {
        attribute.setValueType( ValueType.USERNAME );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "userNameA" ) );

        // User exists
        when( userService.getUserByUsername( "userNameA" ) ).thenReturn( new User() );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testIsPhoneNumber()
    {
        attribute.setValueType( ValueType.PHONE_NUMBER );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "+84938938928" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertTrue( CollectionUtils.isEmpty( objectReportList ) );
    }

    @Test
    void testNotPhoneNumber()
    {
        attribute.setValueType( ValueType.PHONE_NUMBER );
        organisationUnit.getAttributeValues().add( new AttributeValue( attribute, "VN 84938938928" ) );

        List<ObjectReport> objectReportList = new ArrayList<>();

        metadataAttributeCheck.check( objectBundle, OrganisationUnit.class, Lists.newArrayList( organisationUnit ),
            Collections
                .emptyList(),
            ImportStrategy.CREATE_AND_UPDATE,
            validationContext, objectReport -> objectReportList.add( objectReport ) );

        assertFalse( CollectionUtils.isEmpty( objectReportList ) );
        assertEquals( ErrorCode.E6021, objectReportList.get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }
}
