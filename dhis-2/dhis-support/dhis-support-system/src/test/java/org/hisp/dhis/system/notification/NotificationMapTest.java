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
package org.hisp.dhis.system.notification;

import static org.hisp.dhis.scheduling.JobType.DATAVALUE_IMPORT;
import static org.hisp.dhis.system.notification.NotificationMap.MAX_POOL_TYPE_SIZE;

import java.util.Optional;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class NotificationMapTest
{
    private final NotificationMap mapToTest = new NotificationMap();

    @Test
    public void testFirstSummaryToBeCreatedIsTheFirstOneToBeRemoved()
    {
        // Fill the map with jobs
        JobConfiguration jobConfiguration = new JobConfiguration( null, DATAVALUE_IMPORT, "userId", false );
        for ( int i = 0; i < MAX_POOL_TYPE_SIZE; i++ )
        {
            jobConfiguration.setUid( String.valueOf( i ) );
            mapToTest.addSummary( jobConfiguration, i );
        }

        // Add one more
        jobConfiguration.setUid( String.valueOf( MAX_POOL_TYPE_SIZE ) );
        mapToTest.addSummary( jobConfiguration, MAX_POOL_TYPE_SIZE );

        // Check that oldest job is not in the map anymore
        Optional<String> notPresentSummary = mapToTest.getJobSummariesForJobType( DATAVALUE_IMPORT ).keySet()
            .stream()
            .filter( object -> object.equals( "0" ) )
            .findAny();

        Assert.assertFalse( notPresentSummary.isPresent() );

        // Add one more
        jobConfiguration.setUid( String.valueOf( MAX_POOL_TYPE_SIZE + 1 ) );
        mapToTest.addSummary( jobConfiguration, MAX_POOL_TYPE_SIZE + 1 );

        // Check that oldest job is not in the map anymore
        notPresentSummary = mapToTest.getJobSummariesForJobType( DATAVALUE_IMPORT ).keySet()
            .stream()
            .filter( object -> object.equals( "1" ) )
            .findAny();

        Assert.assertFalse( notPresentSummary.isPresent() );

    }
}
