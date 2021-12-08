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

package org.hisp.dhis.tracker.teis;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.tracker.TEIActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TeiExportTests extends ApiTest
{
    private TEIActions teiActions;
    private TrackerActions trackerActions;
   private HashMap<String, String> teis;

    @BeforeAll
    public void beforeAll() {
        teiActions = new TEIActions();
        trackerActions = new TrackerActions();

        setupData();
        new LoginActions().loginAsAdmin();
    }


    @Test
    public void shouldReturnSpecificTeis() {
        teiActions.get( "?trackedEntityInstance=" + teis.values() )
            .validateStatus( 200 )
            .validate()
            .body( "trackedEntityInstances", hasSize(greaterThanOrEqualTo( 1 )) )
            .body( "trackedEntityInstances.enrollments.events", hasSize( greaterThanOrEqualTo( 1 ) ) );
    }

    public void setupData() {
        for ( String orgUnitId : Constants.ORG_UNIT_IDS )
        {
            TrackerApiResponse response = trackerActions.postAndGetJobReport( new TeiDataBuilder().buildWithEnrollmentAndEvent( Constants.TRACKED_ENTITY_TYPE, orgUnitId, Constants.TRACKER_PROGRAM_ID, Constants.TRACKER_PROGRAM_STAGE_IDS[0] ), new QueryParamsBuilder().add( "async=false" ) );

            teis.put( orgUnitId, response.validateSuccessfulImport()
                .extractImportedTeis().get( 0 ));
        }
    }
}
