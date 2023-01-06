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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith( MockitoExtension.class )
class SetMandatoryFieldValidatorTest extends DhisConvenienceTest
{

    private final static String ACTIVE_ENROLLMENT_ID = "ActiveEnrollmentUid";

    private final static String COMPLETED_ENROLLMENT_ID = "CompletedEnrollmentUid";

    private final static String DATA_ELEMENT_ID = "DataElementId";

    private final static String ATTRIBUTE_ID = "AttributeId";

    private final static String ATTRIBUTE_CODE = "AttributeCode";

    private final static String TEI_ID = "TeiId";

    private final static String ATTRIBUTE_VALUE = "23.0";

    private static ProgramStage firstProgramStage;

    private static ProgramStage secondProgramStage;

    private static DataElement dataElementA;

    private static DataElement dataElementB;

    private TrackedEntityAttribute attribute;

    private final SetMandatoryFieldValidator enrollmentValidatorToTest = new SetMandatoryFieldValidator();

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @BeforeEach
    void setUpTest()
    {
        firstProgramStage = createProgramStage( 'A', 0 );
        firstProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        dataElementA = createDataElement( 'A' );
        dataElementA.setUid( DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( firstProgramStage,
            dataElementA, 0 );
        firstProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );
        secondProgramStage = createProgramStage( 'B', 0 );
        secondProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        dataElementB = createDataElement( 'B' );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( secondProgramStage,
            dataElementB, 0 );
        secondProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB ) );

        attribute = createTrackedEntityAttribute( 'A' );
        attribute.setUid( ATTRIBUTE_ID );
        attribute.setCode( ATTRIBUTE_CODE );

        bundle = TrackerBundle.builder().build();
        bundle.setPreheat( preheat );
    }

    @Test
    void testValidateOkMandatoryFieldsForEnrollment()
    {
        when( preheat.getIdSchemes() ).thenReturn( TrackerIdSchemeParams.builder().build() );
        when( preheat.getTrackedEntityAttribute( ATTRIBUTE_ID ) ).thenReturn( attribute );
        bundle.setEnrollments( Lists.newArrayList( getEnrollmentWithMandatoryAttributeSet() ) );

        List<ProgramRuleIssue> errors = enrollmentValidatorToTest.validateEnrollment( bundle,
            getRuleEnrollmentEffects(),
            getEnrollmentWithMandatoryAttributeSet() );

        assertTrue( errors.isEmpty() );
    }

    @Test
    void testValidateOkMandatoryFieldsForEnrollmentUsingIdSchemeCode()
    {
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.CODE )
            .build();
        when( preheat.getIdSchemes() ).thenReturn( idSchemes );
        when( preheat.getTrackedEntityAttribute( ATTRIBUTE_ID ) ).thenReturn( attribute );
        bundle.setEnrollments( Lists.newArrayList( getEnrollmentWithMandatoryAttributeSet( idSchemes ) ) );

        List<ProgramRuleIssue> errors = enrollmentValidatorToTest.validateEnrollment( bundle,
            getRuleEnrollmentEffects(),
            getEnrollmentWithMandatoryAttributeSet( idSchemes ) );

        assertTrue( errors.isEmpty() );
    }

    @Test
    void testValidateWithErrorMandatoryFieldsForEnrollments()
    {
        when( preheat.getIdSchemes() ).thenReturn( TrackerIdSchemeParams.builder().build() );
        when( preheat.getTrackedEntityAttribute( ATTRIBUTE_ID ) ).thenReturn( attribute );
        bundle.setEnrollments( Lists.newArrayList( getEnrollmentWithMandatoryAttributeSet(),
            getEnrollmentWithMandatoryAttributeNOTSet() ) );

        List<ProgramRuleIssue> errors = enrollmentValidatorToTest.validateEnrollment( bundle,
            getRuleEnrollmentEffects(),
            getEnrollmentWithMandatoryAttributeSet() );

        assertTrue( errors.isEmpty() );

        errors = enrollmentValidatorToTest.validateEnrollment( bundle, getRuleEnrollmentEffects(),
            getEnrollmentWithMandatoryAttributeNOTSet() );

        errors.forEach( e -> {
            assertEquals( "RULE_ATTRIBUTE", e.getRuleUid() );
            assertEquals( ValidationCode.E1306, e.getIssueCode() );
            assertEquals( IssueType.ERROR, e.getIssueType() );
            assertEquals( Lists.newArrayList( ATTRIBUTE_ID ), e.getArgs() );
        } );
    }

    private Enrollment getEnrollmentWithMandatoryAttributeSet( TrackerIdSchemeParams idSchemes )
    {
        return Enrollment.builder()
            .enrollment( ACTIVE_ENROLLMENT_ID )
            .trackedEntity( TEI_ID )
            .status( EnrollmentStatus.ACTIVE )
            .attributes( getAttributes( idSchemes ) )
            .build();
    }

    private Enrollment getEnrollmentWithMandatoryAttributeSet()
    {
        return Enrollment.builder()
            .enrollment( ACTIVE_ENROLLMENT_ID )
            .trackedEntity( TEI_ID )
            .status( EnrollmentStatus.ACTIVE )
            .attributes( getAttributes() )
            .build();
    }

    private Enrollment getEnrollmentWithMandatoryAttributeNOTSet()
    {
        return Enrollment.builder()
            .enrollment( COMPLETED_ENROLLMENT_ID )
            .trackedEntity( TEI_ID )
            .status( EnrollmentStatus.COMPLETED )
            .build();
    }

    private List<Attribute> getAttributes( TrackerIdSchemeParams idSchemes )
    {
        return Lists.newArrayList( getAttribute( idSchemes ) );
    }

    private List<Attribute> getAttributes()
    {
        return Lists.newArrayList( getAttribute() );
    }

    private Attribute getAttribute( TrackerIdSchemeParams idSchemes )
    {
        return Attribute.builder()
            .attribute( idSchemes.toMetadataIdentifier( attribute ) )
            .value( ATTRIBUTE_VALUE )
            .build();
    }

    private Attribute getAttribute()
    {
        return Attribute.builder()
            .attribute( MetadataIdentifier.ofUid( ATTRIBUTE_ID ) )
            .value( ATTRIBUTE_VALUE )
            .build();
    }

    private List<MandatoryActionRule> getRuleEnrollmentEffects()
    {
        return Lists.newArrayList(
            new MandatoryActionRule( "RULE_ATTRIBUTE", null, ATTRIBUTE_ID, null, Collections.emptyList() ) );
    }
}
