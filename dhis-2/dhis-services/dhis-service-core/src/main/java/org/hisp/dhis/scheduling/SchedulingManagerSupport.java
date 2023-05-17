/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.scheduling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.eventhook.EventHookPublisher;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.AuthenticationService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class SchedulingManagerSupport
{
    private final UserService userService;

    private final AuthenticationService authenticationService;

    private final JobService jobService;

    private final JobConfigurationService jobConfigurationService;

    private final MessageService messageService;

    private final LeaderManager leaderManager;

    private final Notifier notifier;

    private final EventHookPublisher eventHookPublisher;

    private final CacheProvider cacheProvider;

    private final AsyncTaskExecutor taskExecutor;

    @Qualifier( "taskScheduler" )
    private final TaskScheduler jobScheduler;
}
