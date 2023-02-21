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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityCheck;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.Test;

/**
 * Be default, metadata checks which are marked as "slow" should be excluded
 * from a default run of all checks. These "slow" checks may require significant
 * computational resources. Users should be able to trigger these checks
 * individually though as needed.
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDefaultChecksTest extends AbstractDataIntegrityIntegrationTest
{

    @Test
    void testNonSlowChecksNotRunByDefault()
    {
        final String check = "data_elements_aggregate_abandoned";
        JsonList<JsonDataIntegrityCheck> checks = GET( "/dataIntegrity?checks=" + check )
            .content()
            .asList( JsonDataIntegrityCheck.class );
        assertEquals( 1, checks.size() );
        JsonDataIntegrityCheck slowCheck = checks.get( 0 );
        assertTrue( slowCheck.getIsSlow() );

        // Be sure we start with a clean slate
        assertStatus( HttpStatus.NO_CONTENT, POST( "/maintenance?cacheClear=true" ) );

        // Trigger the default checks
        assertStatus( HttpStatus.OK, POST( "/dataIntegrity/summary" ) );

        // The slow check should not exist
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/" + check + "/summary" )
            .content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertFalse( summary.has( "count" ) );
        assertFalse( summary.has( "percentage" ) );
        assertFalse( summary.has( "finishedTime" ) );

        summary = GET( "/dataIntegrity/categories-no-options/summary" )
            .content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 0, summary.getCount() );
        assertFalse( summary.getIsSlow() );
        assertNotNull( summary.getFinishedTime() );
        assertEquals( 0, summary.getPercentage() );

        // Trigger the slow check
        assertStatus( HttpStatus.OK, POST( "/dataIntegrity/summary?checks=" + check ) );

        summary = GET( "/dataIntegrity/" + check + "/summary" )
            .content()
            .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 0, summary.getCount() );
        assertTrue( summary.getIsSlow() );
        assertNotNull( summary.getFinishedTime() );
        assertNull( summary.getPercentage() );

    }

}