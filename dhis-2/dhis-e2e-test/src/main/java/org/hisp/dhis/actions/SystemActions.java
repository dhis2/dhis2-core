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
package org.hisp.dhis.actions;

import java.util.List;
import java.util.logging.Logger;

import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ImportSummary;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class SystemActions
    extends RestApiActions
{
    private Logger logger = Logger.getLogger( SystemActions.class.getName() );

    public SystemActions()
    {
        super( "/system" );
    }

    public ApiResponse waitUntilTaskCompleted( String taskType, String taskId )
    {
        logger.info( "Waiting until task " + taskType + " with id " + taskId + "is completed" );
        ApiResponse response = null;
        boolean completed = false;
        while ( !completed )
        {
            response = get( "/tasks/" + taskType + "/" + taskId );
            response.validate().statusCode( 200 );
            completed = response.extractList( "completed" ).contains( true );
        }

        logger.info( "Task completed. Message: " + response.extract( "message" ) );
        return response;
    }

    public List<ImportSummary> getTaskSummaries( String taskType, String taskId )
    {
        return getTaskSummariesResponse( taskType, taskId ).validateStatus( 200 ).getImportSummaries();
    }

    public ApiResponse getTaskSummariesResponse( String taskType, String taskId )
    {
        return get( "/taskSummaries/" + taskType + "/" + taskId );
    }

}
