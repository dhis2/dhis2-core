package org.hisp.dhis.tracker.validation;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.hooks.RelationshipsValidationHook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.validation.RelationshipStubs.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Enrico Colasante
 */
@RunWith( MockitoJUnitRunner.class )
public class RelationshipImportValidationTest
{
    @Mock
    private TrackerBundle trackerBundle;

    @Mock
    private TrackerImportValidationContext context;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private TrackedEntityAttributeService teAttrService;

    private ValidationErrorReporter reporter;

    private RelationshipsValidationHook validatorToTest;

    @Before
    public void setUpTest()
    {
        when( context.getBundle() ).thenReturn( trackerBundle );
        when( trackerBundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( trackerBundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) ).thenReturn( getRelationshipTypes() );
        when( trackerBundle.getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE );

        validatorToTest = new RelationshipsValidationHook( teAttrService );
        reporter = new ValidationErrorReporter( context, Relationship.class );
    }

    @Test
    public void validateRelationshipShouldSucceed()
    {
        validatorToTest.validateRelationship( reporter, getValidRelationship() );

        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    public void validateAutoRelationshipShouldFail()
    {
        validatorToTest.validateRelationship( reporter, getAutoRelationship() );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4000 ) );
    }

    @Test
    public void validateRelationshipShouldFailForMissingRelationshipType()
    {
        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) ).thenReturn( Lists.newArrayList() );

        validatorToTest.validateRelationship( reporter, getValidRelationship() );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4004 ) );
    }

    @Test
    public void validateRelationshipShouldFailForMissingFromRelationshipItem()
    {
        validatorToTest.validateRelationship( reporter, getMissingFromRelationshipItemRelationship() );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4004 ) );
    }

    @Test
    public void validateRelationshipShouldFailForMissingToRelationshipItem()
    {
        validatorToTest.validateRelationship( reporter, getMissingToRelationshipItemRelationship() );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4004 ) );
    }

    @Test
    public void validateRelationshipShouldFailForBadTEIRelationshipItem()
    {
        validatorToTest.validateRelationship( reporter, getBadTEIRelationshipItemRelationship() );

        assertEquals( 3, reporter.getReportList().size() );
        assertThat( reporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E4001 ) ) ) );
        assertThat( reporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E4002 ) ) ) );
    }

    @Test
    public void validateRelationshipShouldFailForBadEnrollmentRelationshipItem()
    {
        validatorToTest.validateRelationship( reporter, getBadEnrollmentRelationshipItemRelationship() );

        assertEquals( 3, reporter.getReportList().size() );
        assertThat( reporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E4001 ) ) ) );
        assertThat( reporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E4002 ) ) ) );
    }

    @Test
    public void validateRelationshipShouldFailForBadEventRelationshipItem()
    {
        validatorToTest.validateRelationship( reporter, getBadEventRelationshipItemRelationship() );

        assertEquals( 3, reporter.getReportList().size() );
        assertThat( reporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E4001 ) ) ) );
        assertThat( reporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E4002 ) ) ) );
    }

    @Test
    public void validateDuplicatedRelationshipShouldFail()
    {
        when( trackerBundle.getRelationships() ).thenReturn( getDuplicatedRelationships() );

        validatorToTest.validateRelationship( reporter, getDuplicatedRelationships().get( 0 ) );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList(), hasItem( hasProperty( "errorCode", equalTo(
            TrackerErrorCode.E4003 ) ) ) );
    }

    @Test
    public void validateValidRelationshipInDuplicatedRelationshipsShoulNotdFail()
    {
        when( trackerBundle.getRelationships() ).thenReturn( getDuplicatedAndValidRelationships() );

        validatorToTest.validateRelationship( reporter, getValidRelationship() );

        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    public void validateBidirectionalDuplicatedRelationshipShouldFail()
    {
        when( trackerBundle.getRelationships() ).thenReturn( getBidirectionalDuplicatedRelationships() );

        validatorToTest.validateRelationship( reporter, getBidirectionalDuplicatedRelationships().get( 0 ) );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E4003 ) ) ) );
    }

    @Test
    public void validateValidRelationshipInBidirectionalDuplicatedRelationshipShouldNotFail()
    {
        when( trackerBundle.getRelationships() ).thenReturn( getBidirectionalDuplicatedAndValidRelationships() );

        validatorToTest.validateRelationship( reporter, getValidRelationship() );

        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    public void validateOnlyOneBidirectionalDuplicatedRelationshipShouldFail()
    {
        when( trackerBundle.getRelationships() ).thenReturn( getOnlyOneBidirectionalDuplicatedRelationships() );

        validatorToTest.validateRelationship( reporter, getValidRelationship() );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList(), hasItem( hasProperty( "errorCode", equalTo(
            TrackerErrorCode.E4003 ) ) ) );
    }
}
