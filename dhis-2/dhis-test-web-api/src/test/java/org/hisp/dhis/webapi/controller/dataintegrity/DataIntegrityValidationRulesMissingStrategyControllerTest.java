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

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Test for visualizations which have not been viewed in the past year.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/analytical_objects/visualizations_not_used_1year.yaml
 * }
 *
 * @implNote The API and service layer will coerce a missing value strategy to a
 *           non-null type, thus it is not possible to create a test for this
 *           situation.
 * @author Jason P. Pickering
 */
class DataIntegrityValidationRulesMissingStrategyControllerTest extends AbstractDataIntegrityIntegrationTest
{

    private static final String check = "validation_rules_missing_value_strategy_null";

    private static final String detailsIdType = "validationRules";

    @Test
    void testValidationRulesWithNoStrategyExist()
    {

        assertStatus( HttpStatus.CREATED,
            POST( "/validationRules",
                "{'importance':'MEDIUM','operator':'not_equal_to','leftSide':{'missingValueStrategy':'NEVER_SKIP', "
                    + "" +
                    "'description':'Test','expression':'#{FTRrcoaog83.qk6n4eMAdtK}'}," +
                    "'rightSide':{'missingValueStrategy': 'NEVER_SKIP', 'description':'Test2'," +
                    "'expression':'#{FTRrcoaog83.sqGRzCziswD}'},'periodType':'Monthly','name':'Test rule'}" ) );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );
    }

    @Test
    void testValidationRulesMissingStrategyRuns()
    {
        assertHasNoDataIntegrityIssues( detailsIdType, check, false );
    }

}
