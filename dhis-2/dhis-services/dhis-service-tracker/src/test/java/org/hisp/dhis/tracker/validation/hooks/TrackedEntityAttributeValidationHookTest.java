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
package org.hisp.dhis.tracker.validation.hooks;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.util.Constant;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
public class TrackedEntityAttributeValidationHookTest
{
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private TrackedEntityAttributeValidationHook trackedEntityAttributeValidationHook;

    @Mock
    private TrackerImportValidationContext validationContext;

    @Mock
    private ReservedValueService reservedValueService;

    @Mock
    private DhisConfigurationProvider dhisConfigurationProvider;

    @Mock
    private TrackedEntityAttribute trackedEntityAttribute;

    @Before
    public void setUp()
    {
        TrackerBundle bundle = TrackerBundle.builder().build();
        when( validationContext.getBundle() ).thenReturn( bundle );
        when( dhisConfigurationProvider.getEncryptionStatus() ).thenReturn( EncryptionStatus.OK );
    }

    @Test
    public void shouldPassValidation()
    {
        TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
        trackedEntityAttribute.setUid( "uid" );
        trackedEntityAttribute.setValueType( ValueType.TEXT );

        when( validationContext.getTrackedEntityAttribute( anyString() ) ).thenReturn( trackedEntityAttribute );
        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( new TrackedEntityType() );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes(
                Collections.singletonList( Attribute.builder().attribute( "uid" ).value( "value" ).build() ) )
            .trackedEntityType( "trackedEntityType" )
            .build();

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( validationErrorReporter,
            trackedEntity );

        assertFalse( validationErrorReporter.hasErrors() );
        assertEquals( 0, validationErrorReporter.getReportList().size() );
    }

    @Test
    public void shouldFailValidationMandatoryFields()
    {
        String tet = "tet";

        TrackedEntityType trackedEntityType = new TrackedEntityType();
        TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();
        trackedEntityTypeAttribute.setMandatory( true );

        TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
        trackedEntityAttribute.setUid( "c" );
        trackedEntityTypeAttribute.setTrackedEntityAttribute( trackedEntityAttribute );

        trackedEntityType.setTrackedEntityTypeAttributes( Collections.singletonList( trackedEntityTypeAttribute ) );

        when( validationContext.getTrackedEntityType( tet ) ).thenReturn( trackedEntityType );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntityType( tet )
            .attributes( Arrays.asList( Attribute.builder().attribute( "a" ).value( "value" ).build(),
                Attribute.builder().attribute( "b" ).value( "value" ).build() ) )
            .build();

        TrackedEntityAttribute contextAttribute = new TrackedEntityAttribute();
        contextAttribute.setUid( "uid" );
        contextAttribute.setValueType( ValueType.TEXT );

        when( validationContext.getTrackedEntityAttribute( anyString() ) ).thenReturn( contextAttribute );

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( validationErrorReporter,
            trackedEntity );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 1, validationErrorReporter.getReportList().size() );
        assertEquals( 1, validationErrorReporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1090 ).count() );
    }

    @Test
    public void shouldFailValidationMissingTea()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes( Arrays.asList( Attribute.builder().attribute( "aaaaa" ).build(),
                Attribute.builder().attribute( "bbbbb" ).build() ) )
            .trackedEntityType( "tet" )
            .build();

        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( new TrackedEntityType() );

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( validationErrorReporter,
            trackedEntity );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 2, validationErrorReporter.getReportList().size() );
        assertEquals( 2, validationErrorReporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1006 ).count() );
    }

    @Test
    public void shouldFailMissingAttributeValue()
    {
        String tea = "tea";
        String tet = "tet";

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes( Collections.singletonList( Attribute.builder().attribute( tea ).build() ) )
            .trackedEntityType( tet )
            .build();

        TrackedEntityType trackedEntityType = new TrackedEntityType();
        TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();
        trackedEntityTypeAttribute.setMandatory( true );

        TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
        trackedEntityAttribute.setUid( tea );
        trackedEntityTypeAttribute.setTrackedEntityAttribute( trackedEntityAttribute );

        trackedEntityType.setTrackedEntityTypeAttributes( Collections.singletonList( trackedEntityTypeAttribute ) );

        when( validationContext.getTrackedEntityType( tet ) ).thenReturn( trackedEntityType );
        when( validationContext.getTrackedEntityAttribute( tea ) ).thenReturn( trackedEntityAttribute );

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( validationErrorReporter,
            trackedEntity );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 1, validationErrorReporter.getReportList().size() );
        assertEquals( 1, validationErrorReporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1076 ).count() );
    }

    @Test
    public void shouldFailValueTooLong()
    {
        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );

        when( trackedEntityAttribute.getValueType() ).thenReturn( ValueType.TEXT );

        StringBuilder sbString = new StringBuilder();

        for ( int i = 0; i < Constant.MAX_ATTR_VALUE_LENGTH + 1; i++ )
        {
            sbString.append( "a" );
        }

        trackedEntityAttributeValidationHook.validateAttributeValue( validationErrorReporter,
            trackedEntityAttribute, sbString.toString() );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 1, validationErrorReporter.getReportList().size() );
        assertEquals( 1, validationErrorReporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1077 ).count() );
    }

    @Test
    public void shouldFailDataValueIsValid()
    {
        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );

        when( trackedEntityAttribute.getValueType() ).thenReturn( ValueType.NUMBER );

        trackedEntityAttributeValidationHook.validateAttributeValue( validationErrorReporter,
            trackedEntityAttribute, "value" );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 1, validationErrorReporter.getReportList().size() );
        assertEquals( 1, validationErrorReporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1085 ).count() );
    }

    @Test
    public void shouldFailEncryptionStatus()
    {
        when( trackedEntityAttribute.isConfidentialBool() ).thenReturn( true );
        when( trackedEntityAttribute.getValueType() ).thenReturn( ValueType.AGE );

        when( dhisConfigurationProvider.getEncryptionStatus() )
            .thenReturn( EncryptionStatus.ENCRYPTION_PASSWORD_TOO_SHORT );
        when( dhisConfigurationProvider.getProperty( any() ) ).thenReturn( "property" );
        when( trackedEntityAttribute.getValueType() ).thenReturn( ValueType.TEXT );

        when( validationContext.getTrackedEntityAttribute( anyString() ) ).thenReturn( trackedEntityAttribute );

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateAttributeValue( validationErrorReporter,
            trackedEntityAttribute, "value" );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 1, validationErrorReporter.getReportList().size() );
        assertEquals( 1, validationErrorReporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1112 ).count() );
    }

    @Test
    public void shouldFailOptionSetNotValid()
    {
        TrackedEntityAttribute trackedEntityAttribute = getTrackedEntityAttributeWithOptionSet();

        when( validationContext.getTrackedEntityAttribute( anyString() ) ).thenReturn( trackedEntityAttribute );
        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( new TrackedEntityType() );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes(
                Collections.singletonList( Attribute.builder().attribute( "uid" ).value( "wrongCode" ).build() ) )
            .trackedEntityType( "trackedEntityType" )
            .build();

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( validationErrorReporter,
            trackedEntity );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 1, validationErrorReporter.getReportList().size() );
        assertEquals( 1, validationErrorReporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1125 ).count() );
    }

    @Test
    public void shouldPassValidationValueInOptionSet()
    {
        TrackedEntityAttribute trackedEntityAttribute = getTrackedEntityAttributeWithOptionSet();

        when( validationContext.getTrackedEntityAttribute( anyString() ) ).thenReturn( trackedEntityAttribute );
        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( new TrackedEntityType() );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes(
                Collections.singletonList( Attribute.builder().attribute( "trackedEntity" ).value( "code" ).build() ) )
            .trackedEntityType( "trackedEntityType" )
            .build();

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( validationErrorReporter,
            trackedEntity );

        assertFalse( validationErrorReporter.hasErrors() );
        assertEquals( 0, validationErrorReporter.getReportList().size() );
    }

    @Test
    public void shouldPassValidationWhenValueIsNullAndAttributeIsNotMandatory()
    {
        TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();

        TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
        trackedEntityAttribute.setUid( "trackedEntityAttribute" );
        trackedEntityAttribute.setValueType( ValueType.TEXT );
        trackedEntityTypeAttribute.setTrackedEntityAttribute( trackedEntityAttribute );

        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setTrackedEntityTypeAttributes( Collections.singletonList( trackedEntityTypeAttribute ) );

        when( validationContext.getTrackedEntityAttribute( "trackedEntityAttribute" ) )
            .thenReturn( trackedEntityAttribute );
        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( trackedEntityType );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes(
                Collections.singletonList( Attribute.builder().attribute( "trackedEntityAttribute" ).build() ) )
            .trackedEntityType( "trackedEntityType" )
            .build();

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( validationErrorReporter,
            trackedEntity );

        assertFalse( validationErrorReporter.hasErrors() );
        assertEquals( 0, validationErrorReporter.getReportList().size() );
    }

    @Test
    public void shouldFailValidationWhenValueIsNullAndAttributeIsMandatory()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes(
                Collections.singletonList( Attribute.builder().attribute( "trackedEntityAttribute" ).build() ) )
            .trackedEntityType( "trackedEntityType" )
            .build();

        TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();

        TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
        trackedEntityAttribute.setUid( "trackedEntityAttribute" );
        trackedEntityAttribute.setValueType( ValueType.TEXT );
        trackedEntityTypeAttribute.setTrackedEntityAttribute( trackedEntityAttribute );
        trackedEntityTypeAttribute.setMandatory( true );

        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setTrackedEntityTypeAttributes( Collections.singletonList( trackedEntityTypeAttribute ) );

        when( validationContext.getTrackedEntityAttribute( "trackedEntityAttribute" ) )
            .thenReturn( trackedEntityAttribute );

        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( trackedEntityType );

        ValidationErrorReporter validationErrorReporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( validationErrorReporter,
            trackedEntity );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 1, validationErrorReporter.getReportList().size() );
        assertEquals( TrackerErrorCode.E1076, validationErrorReporter.getReportList().get( 0 ).getErrorCode() );
    }

    private TrackedEntityAttribute getTrackedEntityAttributeWithOptionSet()
    {
        TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
        trackedEntityAttribute.setUid( "uid" );
        trackedEntityAttribute.setValueType( ValueType.TEXT );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );

        Option option1 = new Option();
        option1.setCode( "CODE1" );

        optionSet.setOptions( Arrays.asList( option, option1 ) );

        trackedEntityAttribute.setOptionSet( optionSet );
        return trackedEntityAttribute;
    }
}
