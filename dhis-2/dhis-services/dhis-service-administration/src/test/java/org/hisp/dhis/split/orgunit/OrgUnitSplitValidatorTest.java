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
package org.hisp.dhis.split.orgunit;

import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class OrgUnitSplitValidatorTest
{
    private OrgUnitSplitValidator validator;

    @Before
    public void before()
    {
        this.validator = new OrgUnitSplitValidator();
    }

    @Test
    public void testValidateMissingSource()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );

        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder()
            .addTarget( ouA )
            .addTarget( ouB )
            .withPrimaryTarget( ouA )
            .build();

        assertEquals( ErrorCode.E1510, validator.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void testValidateMissingTargets()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );

        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder()
            .withSource( ouB )
            .addTarget( ouA )
            .withPrimaryTarget( ouA )
            .build();

        assertEquals( ErrorCode.E1511, validator.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void testValidateTargetIsSource()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );

        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder()
            .withSource( ouA )
            .addTarget( ouA )
            .addTarget( ouB )
            .withPrimaryTarget( ouA )
            .build();

        assertEquals( ErrorCode.E1512, validator.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void validateUndefinedPrimaryTarget()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        OrganisationUnit ouC = createOrganisationUnit( 'C' );

        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder()
            .withSource( ouA )
            .addTarget( ouB )
            .addTarget( ouC )
            .build();

        assertEquals( ErrorCode.E1513, validator.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void validatePrimaryTargetNotTarget()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        OrganisationUnit ouC = createOrganisationUnit( 'C' );
        OrganisationUnit ouD = createOrganisationUnit( 'D' );

        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder()
            .withSource( ouA )
            .addTarget( ouB )
            .addTarget( ouC )
            .withPrimaryTarget( ouD )
            .build();

        assertEquals( ErrorCode.E1514, validator.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void validateTargetIsDescendantOfSource()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        OrganisationUnit ouC = createOrganisationUnit( 'C', ouA );

        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder()
            .withSource( ouA )
            .addTarget( ouB )
            .addTarget( ouC )
            .withPrimaryTarget( ouB )
            .build();

        assertEquals( ErrorCode.E1516, validator.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void validateSuccess()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        OrganisationUnit ouC = createOrganisationUnit( 'C' );

        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder()
            .withSource( ouA )
            .addTarget( ouB )
            .addTarget( ouC )
            .withPrimaryTarget( ouB )
            .build();

        assertNull( validator.validateForErrorMessage( request ) );
    }
}
