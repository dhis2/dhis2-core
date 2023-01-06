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
package org.hisp.dhis.tracker.programrule.implementers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.EventActionRule;
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
    private final static String FIRST_EVENT_ID = "EventUid";

    private final static String SECOND_EVENT_ID = "CompletedEventUid";

    private final static String DATA_ELEMENT_ID = "DataElementId";

    private final static String ATTRIBUTE_ID = "AttributeId";

    private final static String ATTRIBUTE_CODE = "AttributeCode";

    private final static String DATA_ELEMENT_VALUE = "1.0";

    private static ProgramStage firstProgramStage;

    private static ProgramStage secondProgramStage;

    private static DataElement dataElementA;

    private static DataElement dataElementB;

    private TrackedEntityAttribute attribute;

    private final SetMandatoryFieldValidator implementerToTest = new SetMandatoryFieldValidator();

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
    void testValidateOkMandatoryFieldsForEvents()
    {
        when( preheat.getIdSchemes() ).thenReturn( TrackerIdSchemeParams.builder().build() );
        when( preheat.getDataElement( DATA_ELEMENT_ID ) ).thenReturn( dataElementA );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( firstProgramStage ) ) )
            .thenReturn( firstProgramStage );
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet() ) );

        List<ProgramRuleIssue> errors = implementerToTest.validateEvent( bundle, getRuleEventEffects(),
            getEventWithMandatoryValueSet() );

        assertTrue( errors.isEmpty() );
    }

    @Test
    void testValidateOkMandatoryFieldsForEventsUsingIdSchemeCode()
    {
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .dataElementIdScheme( TrackerIdSchemeParam.CODE )
            .build();
        when( preheat.getIdSchemes() ).thenReturn( idSchemes );
        when( preheat.getDataElement( DATA_ELEMENT_ID ) ).thenReturn( dataElementA );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( firstProgramStage ) ) )
            .thenReturn( firstProgramStage );
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet( idSchemes ) ) );

        List<ProgramRuleIssue> errors = implementerToTest.validateEvent( bundle, getRuleEventEffects(),
            getEventWithMandatoryValueSet( idSchemes ) );

        assertTrue( errors.isEmpty() );
    }

    @Test
    void testValidateWithErrorMandatoryFieldsForEvents()
    {
        when( preheat.getIdSchemes() ).thenReturn( TrackerIdSchemeParams.builder().build() );
        when( preheat.getDataElement( DATA_ELEMENT_ID ) ).thenReturn( dataElementA );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( firstProgramStage ) ) )
            .thenReturn( firstProgramStage );
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet(), getEventWithMandatoryValueNOTSet() ) );

        List<ProgramRuleIssue> errors = implementerToTest.validateEvent( bundle, getRuleEventEffects(),
            getEventWithMandatoryValueSet() );
        assertTrue( errors.isEmpty() );

        errors = implementerToTest.validateEvent( bundle, getRuleEventEffects(), getEventWithMandatoryValueNOTSet() );

        assertFalse( errors.isEmpty() );
        errors.forEach( e -> {
            assertEquals( "RULE_DATA_VALUE", e.getRuleUid() );
            assertEquals( ValidationCode.E1301, e.getIssueCode() );
            assertEquals( IssueType.ERROR, e.getIssueType() );
            assertEquals( Lists.newArrayList( dataElementA.getUid() ), e.getArgs() );
        } );
    }

    @Test
    void testValidateOkMandatoryFieldsForValidEventAndNotValidEventInDifferentProgramStage()
    {
        when( preheat.getIdSchemes() ).thenReturn( TrackerIdSchemeParams.builder().build() );
        when( preheat.getDataElement( DATA_ELEMENT_ID ) ).thenReturn( dataElementA );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( firstProgramStage ) ) )
            .thenReturn( firstProgramStage );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( secondProgramStage ) ) )
            .thenReturn( secondProgramStage );
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet(),
            getEventWithMandatoryValueNOTSetInDifferentProgramStage() ) );

        List<ProgramRuleIssue> errors = implementerToTest.validateEvent( bundle,
            getRuleEventEffects( getEventWithMandatoryValueSet().getDataValues() ),
            getEventWithMandatoryValueSet() );

        assertTrue( errors.isEmpty() );

        errors = implementerToTest.validateEvent( bundle, Collections.emptyList(),
            getEventWithMandatoryValueNOTSetInDifferentProgramStage() );

        assertTrue( errors.isEmpty() );
    }

    private Event getEventWithMandatoryValueSet( TrackerIdSchemeParams idSchemes )
    {
        return Event.builder()
            .event( FIRST_EVENT_ID )
            .status( EventStatus.ACTIVE )
            .programStage( idSchemes.toMetadataIdentifier( firstProgramStage ) )
            .dataValues( getActiveEventDataValues( idSchemes ) )
            .build();
    }

    private Event getEventWithMandatoryValueSet()
    {
        return Event.builder()
            .event( FIRST_EVENT_ID )
            .status( EventStatus.ACTIVE )
            .programStage( MetadataIdentifier.ofUid( firstProgramStage ) )
            .dataValues( getActiveEventDataValues() )
            .build();
    }

    private Event getEventWithMandatoryValueNOTSet()
    {
        Event event = new Event();
        event.setEvent( SECOND_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( MetadataIdentifier.ofUid( firstProgramStage ) );
        return event;
    }

    private Event getEventWithMandatoryValueNOTSetInDifferentProgramStage()
    {
        Event event = new Event();
        event.setEvent( SECOND_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( MetadataIdentifier.ofUid( secondProgramStage ) );
        return event;
    }

    private Set<DataValue> getActiveEventDataValues( TrackerIdSchemeParams idSchemes )
    {
        DataValue dataValue = DataValue.builder()
            .value( DATA_ELEMENT_VALUE )
            .dataElement( idSchemes.toMetadataIdentifier( dataElementA ) )
            .build();
        return Sets.newHashSet( dataValue );
    }

    private Set<DataValue> getActiveEventDataValues()
    {
        DataValue dataValue = DataValue.builder()
            .value( DATA_ELEMENT_VALUE )
            .dataElement( MetadataIdentifier.ofUid( DATA_ELEMENT_ID ) )
            .build();
        return Sets.newHashSet( dataValue );
    }

    private List<EventActionRule> getRuleEventEffects( Set<DataValue> dataValues )
    {
        return Lists.newArrayList( new EventActionRule( "RULE_DATA_VALUE", null, DATA_ELEMENT_ID, null,
            RuleActionType.MANDATORY_VALUE, dataValues ) );
    }

    private List<EventActionRule> getRuleEventEffects()
    {
        return Lists.newArrayList( new EventActionRule( "RULE_DATA_VALUE", null, DATA_ELEMENT_ID, null,
            RuleActionType.MANDATORY_VALUE, Collections.emptySet() ) );
    }
}
