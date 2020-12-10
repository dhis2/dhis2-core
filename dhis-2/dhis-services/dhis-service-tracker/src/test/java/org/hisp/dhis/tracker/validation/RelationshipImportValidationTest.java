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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.validation.RelationshipStubs.getAutoRelationship;
import static org.hisp.dhis.tracker.validation.RelationshipStubs.getMissingFromRelationshipItemRelationship;
import static org.hisp.dhis.tracker.validation.RelationshipStubs.getMissingToRelationshipItemRelationship;
import static org.hisp.dhis.tracker.validation.RelationshipStubs.getRelationshipTypes;
import static org.hisp.dhis.tracker.validation.RelationshipStubs.getValidRelationship;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.hisp.dhis.relationship.RelationshipType;
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

import com.google.common.collect.Lists;

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

    private ValidationErrorReporter reporter;

    private RelationshipsValidationHook validatorToTest;

    @Before
    public void setUpTest()
    {
        when( context.getBundle() ).thenReturn( trackerBundle );
        when( trackerBundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( trackerBundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getAll( RelationshipType.class ) ).thenReturn( getRelationshipTypes() );
        when( trackerBundle.getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE );

        validatorToTest = new RelationshipsValidationHook();
    }

    @Test
    public void validateRelationshipShouldSucceed()
    {
        final Relationship rel = getValidRelationship();
        reporter = new ValidationErrorReporter( context, rel );
        validatorToTest.validateRelationship( reporter, rel );

        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    public void validateAutoRelationshipShouldFail()
    {
        Relationship rel = getAutoRelationship();
        reporter = new ValidationErrorReporter( context, rel );
        validatorToTest.validateRelationship( reporter, getAutoRelationship() );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4000 ) );
    }

    @Test
    public void validateRelationshipShouldFailForMissingRelationshipType()
    {
        when( preheat.getAll( RelationshipType.class ) ).thenReturn( Lists.newArrayList() );

        Relationship rel = getValidRelationship();
        reporter = new ValidationErrorReporter( context, rel );
        validatorToTest.validateRelationship( reporter, rel );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4009 ) );
    }

    @Test
    public void validateRelationshipShouldFailForMissingFromRelationshipItem()
    {
        Relationship rel = getMissingFromRelationshipItemRelationship();

        reporter = new ValidationErrorReporter( context, rel );
        validatorToTest.validateRelationship( reporter, rel );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4007 ) );
    }

    @Test
    public void validateRelationshipShouldFailForMissingToRelationshipItem()
    {
        Relationship rel = getMissingToRelationshipItemRelationship();
        reporter = new ValidationErrorReporter( context, rel );

        validatorToTest.validateRelationship( reporter, rel );

        assertEquals( 1, reporter.getReportList().size() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4008 ) );
    }
}
