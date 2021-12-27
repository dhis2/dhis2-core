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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class TrackedEntityAttributeValidationHookTest
{

    @InjectMocks
    private TrackedEntityAttributeValidationHook trackedEntityAttributeValidationHook;

    @Mock
    private TrackerImportValidationContext validationContext;

    @Mock
    private DhisConfigurationProvider dhisConfigurationProvider;

    @Mock
    private TrackedEntityAttribute trackedEntityAttribute;

    @BeforeEach
    public void setUp()
    {
        TrackerBundle bundle = TrackerBundle.builder().build();
        when( validationContext.getBundle() ).thenReturn( bundle );
        when( dhisConfigurationProvider.getEncryptionStatus() ).thenReturn( EncryptionStatus.OK );
    }

    @Test
    void shouldPassValidation()
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

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( reporter,
            trackedEntity );

        assertFalse( reporter.hasErrors() );
        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    void shouldFailValidationMandatoryFields()
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

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( reporter,
            trackedEntity );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        assertEquals( 1, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1090 ).count() );
    }

    @Test
    void shouldFailValidationMissingTea()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes( Arrays.asList( Attribute.builder().attribute( "aaaaa" ).build(),
                Attribute.builder().attribute( "bbbbb" ).build() ) )
            .trackedEntityType( "tet" )
            .build();

        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( new TrackedEntityType() );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( reporter,
            trackedEntity );

        assertTrue( reporter.hasErrors() );
        assertEquals( 2, reporter.getReportList().size() );
        assertEquals( 2, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1006 ).count() );
    }

    @Test
    void shouldFailMissingAttributeValue()
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

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( reporter,
            trackedEntity );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        assertEquals( 1, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1076 ).count() );
    }

    @Test
    void shouldFailValueTooLong()
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );

        when( trackedEntityAttribute.getValueType() ).thenReturn( ValueType.TEXT );

        StringBuilder sbString = new StringBuilder();

        for ( int i = 0; i < Constant.MAX_ATTR_VALUE_LENGTH + 1; i++ )
        {
            sbString.append( "a" );
        }

        trackedEntityAttributeValidationHook.validateAttributeValue( reporter,
            trackedEntityAttribute, sbString.toString() );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        assertEquals( 1, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1077 ).count() );
    }

    @Test
    void shouldFailDataValueIsValid()
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );

        when( trackedEntityAttribute.getValueType() ).thenReturn( ValueType.NUMBER );

        trackedEntityAttributeValidationHook.validateAttributeValue( reporter,
            trackedEntityAttribute, "value" );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        assertEquals( 1, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1085 ).count() );
    }

    @Test
    void shouldFailEncryptionStatus()
    {
        when( trackedEntityAttribute.isConfidentialBool() ).thenReturn( true );
        when( trackedEntityAttribute.getValueType() ).thenReturn( ValueType.AGE );

        when( dhisConfigurationProvider.getEncryptionStatus() )
            .thenReturn( EncryptionStatus.ENCRYPTION_PASSWORD_TOO_SHORT );
        when( dhisConfigurationProvider.getProperty( any() ) ).thenReturn( "property" );
        when( trackedEntityAttribute.getValueType() ).thenReturn( ValueType.TEXT );

        when( validationContext.getTrackedEntityAttribute( anyString() ) ).thenReturn( trackedEntityAttribute );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateAttributeValue( reporter,
            trackedEntityAttribute, "value" );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        assertEquals( 1, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1112 ).count() );
    }

    @Test
    void shouldFailOptionSetNotValid()
    {
        TrackedEntityAttribute trackedEntityAttribute = getTrackedEntityAttributeWithOptionSet();

        when( validationContext.getTrackedEntityAttribute( anyString() ) ).thenReturn( trackedEntityAttribute );
        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( new TrackedEntityType() );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes(
                Collections.singletonList( Attribute.builder().attribute( "uid" ).value( "wrongCode" ).build() ) )
            .trackedEntityType( "trackedEntityType" )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( reporter,
            trackedEntity );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        assertEquals( 1, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1125 ).count() );
    }

    @Test
    void shouldPassValidationValueInOptionSet()
    {
        TrackedEntityAttribute trackedEntityAttribute = getTrackedEntityAttributeWithOptionSet();

        when( validationContext.getTrackedEntityAttribute( anyString() ) ).thenReturn( trackedEntityAttribute );
        when( validationContext.getTrackedEntityType( anyString() ) ).thenReturn( new TrackedEntityType() );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .attributes(
                Collections.singletonList( Attribute.builder().attribute( "trackedEntity" ).value( "code" ).build() ) )
            .trackedEntityType( "trackedEntityType" )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( reporter,
            trackedEntity );

        assertFalse( reporter.hasErrors() );
        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    void shouldPassValidationWhenValueIsNullAndAttributeIsNotMandatory()
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

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( reporter,
            trackedEntity );

        assertFalse( reporter.hasErrors() );
        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    void shouldFailValidationWhenValueIsNullAndAttributeIsMandatory()
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

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext );
        trackedEntityAttributeValidationHook.validateTrackedEntity( reporter,
            trackedEntity );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );
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
