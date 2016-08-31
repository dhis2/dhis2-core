package org.hisp.dhis.dataadmin.action.statistics;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.Objects;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.commons.collection.EnumMapWrapper;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.joda.time.DateTime;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class GetStatisticsAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
    private StatisticsProvider statisticsProvider;

    public void setStatisticsProvider( StatisticsProvider statisticsProvider )
    {
        this.statisticsProvider = statisticsProvider;
    }
    
    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }
    
    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    private ProgramStageInstanceService programStageInstanceService;

    public void setProgramStageInstanceService( ProgramStageInstanceService programStageInstanceService )
    {
        this.programStageInstanceService = programStageInstanceService;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------
    
    private EnumMapWrapper<Objects, Integer> objects;

    public EnumMapWrapper<Objects, Integer> getObjects()
    {
        return objects;
    }
    
    private Map<Integer, Integer> activeUsers = new HashMap<>();

    public Map<Integer, Integer> getActiveUsers()
    {
        return activeUsers;
    }

    private EnumMapWrapper<UserInvitationStatus, Integer> userInvitations;

    public EnumMapWrapper<UserInvitationStatus, Integer> getUserInvitations()
    {
        return userInvitations;
    }

    private Map<Integer, Integer> dataValueCount = new HashMap<>();

    public Map<Integer, Integer> getDataValueCount()
    {
        return dataValueCount;
    }

    private Map<Integer, Long> eventCount = new HashMap<>();

    public Map<Integer, Long> getEventCount()
    {
        return eventCount;
    }
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------
    
    @Override
    public String execute()
        throws Exception
    {
        Map<Objects, Integer> counts = statisticsProvider.getObjectCounts();
        
        Date lastHour = new DateTime().minusHours( 1 ).toDate();
        
        objects = new EnumMapWrapper<>( Objects.class, counts );
        
        activeUsers.put( 0, userService.getActiveUsersCount( lastHour ) );
        activeUsers.put( 1, userService.getActiveUsersCount( 0 ) );
        activeUsers.put( 2, userService.getActiveUsersCount( 1 ) );
        activeUsers.put( 7, userService.getActiveUsersCount( 7 ) );
        activeUsers.put( 30, userService.getActiveUsersCount( 30 ) );
        
        Map<UserInvitationStatus, Integer> invitations = new HashMap<>();
        
        UserQueryParams inviteAll = new UserQueryParams();
        inviteAll.setInvitationStatus( UserInvitationStatus.ALL );
        invitations.put( UserInvitationStatus.ALL, userService.getUserCount( inviteAll ) );

        UserQueryParams inviteExpired = new UserQueryParams();
        inviteExpired.setInvitationStatus( UserInvitationStatus.EXPIRED );
        invitations.put( UserInvitationStatus.EXPIRED, userService.getUserCount( inviteExpired ) );             
        
        userInvitations = new EnumMapWrapper<>( UserInvitationStatus.class, invitations );
        
        dataValueCount.put( 0, dataValueService.getDataValueCount( 0 ) );
        dataValueCount.put( 1, dataValueService.getDataValueCount( 1 ) );
        dataValueCount.put( 7, dataValueService.getDataValueCount( 7 ) );
        dataValueCount.put( 30, dataValueService.getDataValueCount( 30 ) );

        eventCount.put( 0, programStageInstanceService.getProgramStageInstanceCount( 0 ) );
        eventCount.put( 1, programStageInstanceService.getProgramStageInstanceCount( 1 ) );
        eventCount.put( 7, programStageInstanceService.getProgramStageInstanceCount( 7 ) );
        eventCount.put( 30, programStageInstanceService.getProgramStageInstanceCount( 30 ) );

        return SUCCESS;
    }
}
