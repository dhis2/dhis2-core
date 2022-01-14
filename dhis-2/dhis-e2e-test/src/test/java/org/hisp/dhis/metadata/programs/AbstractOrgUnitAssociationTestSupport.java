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
package org.hisp.dhis.metadata.programs;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;

import java.util.List;
import java.util.function.Function;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.dto.ApiResponse;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
class AbstractOrgUnitAssociationTestSupport extends ApiTest
{

    private final LoginActions loginActions = new LoginActions();

    protected AbstractOrgUnitAssociationTestSupport()
    {
    };

    private ApiResponse validateResponseHasKey( Function<String, ApiResponse> apiResponseProvider, String key )
    {
        ApiResponse apiResponse = apiResponseProvider.apply( key );
        apiResponse.validate()
            .body( "$", hasKey( key ) );
        return apiResponse;
    }

    public void testOrgUnitsConnections( Function<String, ApiResponse> apiResponseProvider, String uid )
    {
        loginActions.loginAsSuperUser();

        ApiResponse associatedOrgUnitsAsSuperUserApiResponse = validateResponseHasKey( apiResponseProvider, uid );

        loginActions.loginAsDefaultUser();

        List<String> associatedOrgUnitsAsTrackerUids = validateResponseHasKey( apiResponseProvider, uid )
            .extractList( uid );

        associatedOrgUnitsAsSuperUserApiResponse.validate()
            .assertThat()
            .body( uid, hasItems( associatedOrgUnitsAsTrackerUids.toArray() ) )
            .and()
            .body( uid + ".size()", greaterThanOrEqualTo( associatedOrgUnitsAsTrackerUids.size() ) );
    }

}
