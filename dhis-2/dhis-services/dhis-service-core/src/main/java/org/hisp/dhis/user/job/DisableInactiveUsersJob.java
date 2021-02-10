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
package org.hisp.dhis.user.job;

import static java.time.ZoneId.systemDefault;

import java.time.LocalDate;
import java.util.Date;

import lombok.AllArgsConstructor;

import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.DisableInactiveUsersJobParameters;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * @author Jan Bernitt
 */
@AllArgsConstructor
@Component( "disableInactiveUsersJob" )
public class DisableInactiveUsersJob extends AbstractJob
{
    private final UserService userService;

    private final Notifier notifier;

    @Override
    public JobType getJobType()
    {
        return JobType.DISABLE_INACTIVE_USERS;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        DisableInactiveUsersJobParameters parameters = (DisableInactiveUsersJobParameters) jobConfiguration
            .getJobParameters();
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusMonths( parameters.getInactiveMonths() );
        Date nMonthsAgo = Date.from( since.atStartOfDay( systemDefault() ).toInstant() );
        int disabledUserCount = userService.disableUsersInactiveSince( nMonthsAgo );
        notifier.notify( jobConfiguration, String.format( "Disabled %d users with %d months of inactivity",
            disabledUserCount, parameters.getInactiveMonths() ) );
    }
}
