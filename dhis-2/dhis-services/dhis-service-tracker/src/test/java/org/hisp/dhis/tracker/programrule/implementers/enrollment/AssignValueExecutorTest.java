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
package org.hisp.dhis.tracker.programrule.implementers.enrollment;

import static org.hisp.dhis.tracker.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.programrule.IssueType.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class AssignValueExecutorTest extends DhisConvenienceTest
{

    private final static String TRACKED_ENTITY_ID = "TrackedEntityUid";

    private final static String FIRST_ENROLLMENT_ID = "ActiveEnrollmentUid";

    private final static String SECOND_ENROLLMENT_ID = "CompletedEnrollmentUid";

    private final static String DATA_ELEMENT_ID = "DataElementId";

    private final static String DATA_ELEMENT_CODE = "DataElementCode";

    private final static String ANOTHER_DATA_ELEMENT_ID = "AnotherDataElementId";

    private final static String ATTRIBUTE_ID = "AttributeId";

    private final static String ATTRIBUTE_CODE = "AttributeCode";

    private final static String TEI_ATTRIBUTE_OLD_VALUE = "10.0";

    private final static String TEI_ATTRIBUTE_NEW_VALUE = "24.0";

    private static ProgramStage firstProgramStage;

    private static ProgramStage secondProgramStage;

    private static DataElement dataElementA;

    private static DataElement dataElementB;

    private static TrackedEntityAttribute attributeA;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private SystemSettingManager systemSettingManager;

    @BeforeEach
    void setUpTest()
    {
        firstProgramStage = createProgramStage( 'A', 0 );
        firstProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        attributeA = createTrackedEntityAttribute( 'A' );
        attributeA.setUid( ATTRIBUTE_ID );
        attributeA.setCode( ATTRIBUTE_CODE );
        attributeA.setValueType( ValueType.NUMBER );
        dataElementA = createDataElement( 'A' );
        dataElementA.setUid( DATA_ELEMENT_ID );
        dataElementA.setCode( DATA_ELEMENT_CODE );
        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( firstProgramStage,
            dataElementA, 0 );
        firstProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );
        secondProgramStage = createProgramStage( 'B', 0 );
        secondProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        dataElementB = createDataElement( 'B' );
        dataElementB.setUid( ANOTHER_DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( secondProgramStage,
            dataElementB, 0 );
        secondProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( firstProgramStage ) ) )
            .thenReturn( firstProgramStage );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( secondProgramStage ) ) )
            .thenReturn( secondProgramStage );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementA.getUid() ) ) ).thenReturn( dataElementA );
        when( preheat.getTrackedEntityAttribute( attributeA.getUid() ) )
            .thenReturn( attributeA );
        when( preheat.getTrackedEntityAttribute( MetadataIdentifier.ofUid( attributeA.getUid() ) ) )
            .thenReturn( attributeA );
        bundle = TrackerBundle.builder().build();
        bundle.setPreheat( preheat );
        when( systemSettingManager.getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE ) )
            .thenReturn( Boolean.FALSE );
    }

    @Test
    void testAssignAttributeValueForEnrollmentsWhenAttributeIsEmpty()
    {
        when( preheat.getIdSchemes() ).thenReturn( TrackerIdSchemeParams.builder().build() );
        List<TrackedEntity> trackedEntities = Lists.newArrayList( getTrackedEntitiesWithAttributeNOTSet() );
        Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
        List<Enrollment> enrollments = Lists.newArrayList( enrollmentWithAttributeNOTSet );
        bundle.setTrackedEntities( trackedEntities );
        bundle.setEnrollments( enrollments );

        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager,
            "", TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, enrollmentWithAttributeNOTSet.getAttributes() );

        Optional<ProgramRuleIssue> warning = executor.validateEnrollment( bundle, enrollmentWithAttributeNOTSet );

        Enrollment enrollment = bundle.getEnrollments().stream()
            .filter( e -> e.getEnrollment().equals( SECOND_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) ) ).findAny();
        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue() );
        assertTrue( warning.isPresent() );
        warning.ifPresent( w -> assertEquals( WARNING, w.getIssueType() ) );
    }

    @Test
    void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresent()
    {
        Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet();
        List<Enrollment> enrollments = Lists.newArrayList( enrollmentWithAttributeSet );
        bundle.setEnrollments( enrollments );

        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager,
            "", TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, enrollmentWithAttributeSet.getAttributes() );

        Optional<ProgramRuleIssue> error = executor.validateEnrollment( bundle, enrollmentWithAttributeSet );

        Enrollment enrollment = bundle.getEnrollments().stream()
            .filter( e -> e.getEnrollment().equals( FIRST_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) ) ).findAny();
        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_OLD_VALUE, attribute.get().getValue() );
        assertTrue( error.isPresent() );
        error.ifPresent( w -> assertEquals( ERROR, w.getIssueType() ) );
    }

    @Test
    void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentUsingIdSchemeCode()
    {

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.CODE )
            .build();
        when( preheat.getIdSchemes() ).thenReturn( idSchemes );
        when( preheat.getTrackedEntityAttribute( ATTRIBUTE_ID ) ).thenReturn( attributeA );
        Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet( idSchemes );
        List<Enrollment> enrollments = Lists.newArrayList( enrollmentWithAttributeSet );
        bundle.setEnrollments( enrollments );

        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager,
            "", TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, enrollmentWithAttributeSet.getAttributes() );

        Optional<ProgramRuleIssue> error = executor.validateEnrollment( bundle, enrollmentWithAttributeSet );

        Enrollment enrollment = bundle.getEnrollments().stream()
            .filter( e -> e.getEnrollment().equals( FIRST_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofCode( ATTRIBUTE_CODE ) ) ).findAny();
        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_OLD_VALUE, attribute.get().getValue() );
        assertTrue( error.isPresent() );
        error.ifPresent( w -> assertEquals( ERROR, w.getIssueType() ) );
    }

    @Test
    void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentInTei()
    {
        Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
        List<Enrollment> enrollments = Lists.newArrayList( enrollmentWithAttributeNOTSet );
        List<TrackedEntity> trackedEntities = Lists.newArrayList( getTrackedEntitiesWithAttributeSet() );
        bundle.setEnrollments( enrollments );
        bundle.setTrackedEntities( trackedEntities );

        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager,
            "", TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, getTrackedEntitiesWithAttributeSet().getAttributes() );

        Optional<ProgramRuleIssue> error = executor.validateEnrollment( bundle, enrollmentWithAttributeNOTSet );

        Enrollment enrollment = bundle.getEnrollments().stream()
            .filter( e -> e.getEnrollment().equals( SECOND_ENROLLMENT_ID ) ).findAny().get();
        TrackedEntity trackedEntity = bundle.getTrackedEntities().stream()
            .filter( e -> e.getTrackedEntity().equals( TRACKED_ENTITY_ID ) ).findAny().get();
        Optional<Attribute> enrollmentAttribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) ) ).findAny();
        Optional<Attribute> teiAttribute = trackedEntity.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) ) ).findAny();
        assertFalse( enrollmentAttribute.isPresent() );
        assertTrue( teiAttribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_OLD_VALUE, teiAttribute.get().getValue() );
        assertTrue( error.isPresent() );
        error.ifPresent( w -> assertEquals( ERROR, w.getIssueType() ) );
    }

    @Test
    void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentInTeiAndCanBeOverwritten()
    {
        when( systemSettingManager.getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE ) )
            .thenReturn( Boolean.TRUE );
        Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
        List<Enrollment> enrollments = Lists.newArrayList( enrollmentWithAttributeNOTSet );
        List<TrackedEntity> trackedEntities = Lists.newArrayList( getTrackedEntitiesWithAttributeSet() );
        bundle.setEnrollments( enrollments );
        bundle.setTrackedEntities( trackedEntities );

        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager,
            "", TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, enrollmentWithAttributeNOTSet.getAttributes() );

        Optional<ProgramRuleIssue> warning = executor.validateEnrollment( bundle, enrollmentWithAttributeNOTSet );

        Enrollment enrollment = bundle.getEnrollments().stream()
            .filter( e -> e.getEnrollment().equals( SECOND_ENROLLMENT_ID ) ).findAny().get();
        TrackedEntity trackedEntity = bundle.getTrackedEntities().stream()
            .filter( e -> e.getTrackedEntity().equals( TRACKED_ENTITY_ID ) ).findAny().get();
        Optional<Attribute> enrollmentAttribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) ) ).findAny();
        Optional<Attribute> teiAttribute = trackedEntity.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) ) ).findAny();
        assertFalse( enrollmentAttribute.isPresent() );
        assertTrue( teiAttribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_NEW_VALUE, teiAttribute.get().getValue() );
        assertTrue( warning.isPresent() );
        warning.ifPresent( w -> assertEquals( WARNING, w.getIssueType() ) );
    }

    @Test
    void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndHasTheSameValue()
    {
        Enrollment enrollmentWithAttributeSetSameValue = getEnrollmentWithAttributeSetSameValue();
        List<Enrollment> enrollments = Lists.newArrayList( enrollmentWithAttributeSetSameValue );
        bundle.setEnrollments( enrollments );

        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager,
            "", TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, enrollmentWithAttributeSetSameValue.getAttributes() );

        Optional<ProgramRuleIssue> warning = executor.validateEnrollment( bundle, enrollmentWithAttributeSetSameValue );

        Enrollment enrollment = bundle.getEnrollments().stream()
            .filter( e -> e.getEnrollment().equals( FIRST_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) ) ).findAny();
        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue() );
        assertTrue( warning.isPresent() );
        warning.ifPresent( w -> assertEquals( WARNING, w.getIssueType() ) );
    }

    @Test
    void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndSystemSettingToOverwriteIsTrue()
    {
        Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet();
        List<Enrollment> enrollments = Lists.newArrayList( enrollmentWithAttributeSet );
        bundle.setEnrollments( enrollments );
        when( systemSettingManager.getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE ) )
            .thenReturn( Boolean.TRUE );

        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager,
            "", TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, enrollmentWithAttributeSet.getAttributes() );

        Optional<ProgramRuleIssue> warning = executor.validateEnrollment( bundle, enrollmentWithAttributeSet );

        Enrollment enrollment = bundle.getEnrollments().stream()
            .filter( e -> e.getEnrollment().equals( FIRST_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) ) ).findAny();
        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue() );
        assertTrue( warning.isPresent() );
        warning.ifPresent( w -> assertEquals( WARNING, w.getIssueType() ) );
    }

    @Test
    void testIsEqual()
    {
        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager, null, null, null, null );

        assertTrue( executor.isEqual( "first_dose", "first_dose", ValueType.TEXT ) );
        assertTrue( executor.isEqual( "2020-01-01", "2020-01-01", ValueType.DATE ) );
        assertTrue( executor.isEqual( "true", "true", ValueType.BOOLEAN ) );
        assertTrue( executor.isEqual( "26.4", "26.4", ValueType.TEXT ) );
        assertTrue( executor.isEqual( "24.8", "24.8", ValueType.NUMBER ) );
        assertTrue( executor.isEqual( "32", "32", ValueType.INTEGER ) );

        assertFalse( executor.isEqual( "first_dose", "second_dose", ValueType.TEXT ) );
        assertFalse( executor.isEqual( "2020-01-01", "2020-01-02", ValueType.DATE ) );
        assertFalse( executor.isEqual( "true", "false", ValueType.BOOLEAN ) );
        assertFalse( executor.isEqual( "26.4", "26.5", ValueType.TEXT ) );
        assertFalse( executor.isEqual( "24.8", "24.9", ValueType.NUMBER ) );
        assertFalse( executor.isEqual( "32", "33", ValueType.INTEGER ) );
    }

    @Test
    void testIsEqualDataTypeIntegrity()
    {
        AssignValueExecutor executor = new AssignValueExecutor( systemSettingManager, null, null, null, null );

        assertFalse( executor.isEqual( "first_dose", "46.2", ValueType.NUMBER ) );
        assertFalse( executor.isEqual( "24", "second_dose", ValueType.NUMBER ) );
        assertFalse( executor.isEqual( null, "46.2", ValueType.NUMBER ) );
        assertFalse( executor.isEqual( "26.4", null, ValueType.NUMBER ) );
        assertFalse( executor.isEqual( "first_dose", null, ValueType.TEXT ) );
        assertFalse( executor.isEqual( null, "second_dose", ValueType.TEXT ) );
    }

    private Enrollment getEnrollmentWithAttributeSet()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( FIRST_ENROLLMENT_ID );
        enrollment.setStatus( EnrollmentStatus.ACTIVE );
        enrollment.setAttributes( getAttributes() );
        return enrollment;
    }

    private Enrollment getEnrollmentWithAttributeSet( TrackerIdSchemeParams idSchemes )
    {
        return Enrollment.builder()
            .enrollment( FIRST_ENROLLMENT_ID )
            .status( EnrollmentStatus.ACTIVE )
            .attributes( getAttributes( idSchemes ) )
            .build();
    }

    private Enrollment getEnrollmentWithAttributeSetSameValue()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( FIRST_ENROLLMENT_ID );
        enrollment.setStatus( EnrollmentStatus.ACTIVE );
        enrollment.setAttributes( getAttributesSameValue() );
        return enrollment;
    }

    private TrackedEntity getTrackedEntitiesWithAttributeSet()
    {
        TrackedEntity trackedEntity = new TrackedEntity();
        trackedEntity.setTrackedEntity( TRACKED_ENTITY_ID );
        trackedEntity.setAttributes( getAttributes() );
        return trackedEntity;
    }

    private TrackedEntity getTrackedEntitiesWithAttributeNOTSet()
    {
        TrackedEntity trackedEntity = new TrackedEntity();
        trackedEntity.setTrackedEntity( TRACKED_ENTITY_ID );
        return trackedEntity;
    }

    private Enrollment getEnrollmentWithAttributeNOTSet()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( SECOND_ENROLLMENT_ID );
        enrollment.setStatus( EnrollmentStatus.COMPLETED );
        enrollment.setTrackedEntity( TRACKED_ENTITY_ID );
        return enrollment;
    }

    private List<Attribute> getAttributes( TrackerIdSchemeParams idSchemes )
    {
        Attribute attribute = Attribute.builder()
            .attribute( idSchemes.toMetadataIdentifier( attributeA ) )
            .value( TEI_ATTRIBUTE_OLD_VALUE )
            .build();
        return Lists.newArrayList( attribute );
    }

    private List<Attribute> getAttributes()
    {
        Attribute attribute = Attribute.builder()
            .attribute( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) )
            .value( TEI_ATTRIBUTE_OLD_VALUE )
            .build();
        return Lists.newArrayList( attribute );
    }

    private List<Attribute> getAttributesSameValue()
    {
        Attribute attribute = Attribute.builder()
            .attribute( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) )
            .value( TEI_ATTRIBUTE_NEW_VALUE )
            .build();
        return Lists.newArrayList( attribute );
    }
}
