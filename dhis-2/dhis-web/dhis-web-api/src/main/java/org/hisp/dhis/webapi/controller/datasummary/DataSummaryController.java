package org.hisp.dhis.webapi.controller.datasummary;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import datasummary.DataSummary;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping( value = DataSummaryController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataSummaryController
{
    public static final String RESOURCE_PATH = "/dataSummary";

    @Autowired
    private StatisticsProvider statisticsProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @GetMapping
    public @ResponseBody
    DataSummary getStatistics()
    {
        DataSummary statistics = new DataSummary();

        /* database object counts */
        Map<String, Integer> objectCounts = new HashMap<>(  );
        statisticsProvider.getObjectCounts().forEach( (object, count) -> objectCounts.put( object.getValue(), count ));
        statistics.setObjectCounts( objectCounts );

        /* active users count */
        Date lastHour = new DateTime().minusHours( 1 ).toDate();

        Map<Integer, Integer> activeUsers = new HashMap<>(  );

        activeUsers.put( 0,  userService.getActiveUsersCount( lastHour ));
        activeUsers.put( 1,  userService.getActiveUsersCount( 0 ));
        activeUsers.put( 2,  userService.getActiveUsersCount( 1 ));
        activeUsers.put( 7,  userService.getActiveUsersCount( 7 ));
        activeUsers.put( 30,  userService.getActiveUsersCount( 30 ));

        statistics.setActiveUsers( activeUsers );

        /* user invitations count */
        Map<String, Integer> userInvitations = new HashMap<>(  );

        UserQueryParams inviteAll = new UserQueryParams();
        inviteAll.setInvitationStatus( UserInvitationStatus.ALL );
        userInvitations.put( UserInvitationStatus.ALL.getValue(),  userService.getUserCount( inviteAll ) );

        UserQueryParams inviteExpired = new UserQueryParams();
        inviteExpired.setInvitationStatus( UserInvitationStatus.EXPIRED );
        userInvitations.put( UserInvitationStatus.EXPIRED.getValue(),  userService.getUserCount( inviteExpired ) );

        statistics.setUserInvitations( userInvitations );

        /* data value count */
        Map<Integer, Integer> dataValueCount = new HashMap<>(  );

        dataValueCount.put( 0, dataValueService.getDataValueCount( 0 ));
        dataValueCount.put( 1, dataValueService.getDataValueCount( 1 ));
        dataValueCount.put( 7, dataValueService.getDataValueCount( 7 ));
        dataValueCount.put( 30, dataValueService.getDataValueCount( 30 ));

        statistics.setDataValueCount( dataValueCount );

        /* event count */
        Map<Integer, Long> eventCount = new HashMap<>(  );

        eventCount.put( 0, programStageInstanceService.getProgramStageInstanceCount( 0 ) );
        eventCount.put( 1, programStageInstanceService.getProgramStageInstanceCount( 1 ) );
        eventCount.put( 7, programStageInstanceService.getProgramStageInstanceCount( 7 ) );
        eventCount.put( 30, programStageInstanceService.getProgramStageInstanceCount( 30 ) );

        statistics.setEventCount( eventCount );

        return statistics;
    }
}
