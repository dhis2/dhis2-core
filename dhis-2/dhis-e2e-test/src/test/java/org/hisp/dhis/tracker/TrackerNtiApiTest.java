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

package org.hisp.dhis.tracker;

import com.google.gson.JsonObject;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.MaintenanceActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.helpers.file.JsonFileReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@Execution( ExecutionMode.SAME_THREAD )
public class TrackerNtiApiTest
    extends TrackerApiTest
{
    protected TrackerActions trackerActions;

    protected LoginActions loginActions;

    @BeforeAll
    public void beforeTrackerNti()
    {
        trackerActions = new TrackerActions();
        loginActions = new LoginActions();
    }

    protected String importEnrollment()
        throws Exception
    {
        JsonObject teiBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/teis/teiAndEnrollment.json" ) );

        return trackerActions.postAndGetJobReport( teiBody ).validateSuccessfulImport().extractImportedEnrollments().get( 0 );
    }

    protected String importTei()
        throws Exception
    {
        JsonObject teiBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/teis/tei.json" ) );

        return trackerActions.postAndGetJobReport( teiBody ).validateSuccessfulImport().extractImportedTeis().get( 0 );
    }

    protected List<String> importTeis()
    {
        List<String> teis = trackerActions.postAndGetJobReport( new File( "src/test/resources/tracker/importer/teis/teis.json" ) )
            .validateSuccessfulImport().extractImportedTeis();

        return teis;
    }

    protected List<String> importEvents()
        throws Exception
    {
        JsonObject object = new FileReaderUtils().read( new File( "src/test/resources/tracker/importer/events/events.json" ) )
            .replacePropertyValuesWithIds( "event" ).get( JsonObject.class );

        return trackerActions.postAndGetJobReport( object )
            .validateSuccessfulImport().extractImportedEvents();
    }

    protected TrackerApiResponse importTeiWithEnrollment( String programId, String programStageId )
        throws Exception
    {
        JsonObject teiWithEnrollment = new FileReaderUtils()
            .read( new File( "src/test/resources/tracker/importer/teis/teiWithEnrollments.json" ) )
            .replacePropertyValuesWith( "program", programId )
            .replacePropertyValuesWith( "programStage", programStageId )
            .get( JsonObject.class );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( teiWithEnrollment );

        response.validateSuccessfulImport();
        return response;
    }

    protected JsonObject buildTeiWithEnrollmentAndEvent()
        throws IOException
    {
        JsonObject object = new JsonFileReader(
            new File( "src/test/resources/tracker/importer/teis/teisWithEnrollmentsAndEvents.json" ) )
            .replaceStringsWithIds( "Kj6vYde4LHh", "Nav6inZRw1u", "MNWZ6hnuhSw", "ZwwuwNp6gVd", "PuBvJxDB73z", "olfXZzSGacW" )

            .get();

        return object;
    }

    protected TrackerApiResponse importTeiWithEnrollmentAndEvent()
        throws Exception
    {
        JsonObject object = buildTeiWithEnrollmentAndEvent();

        return trackerActions.postAndGetJobReport( object )
            .validateSuccessfulImport();
    }

    @AfterEach
    public void afterEachNTI()
    {
        new MaintenanceActions().removeSoftDeletedData();
    }
}
