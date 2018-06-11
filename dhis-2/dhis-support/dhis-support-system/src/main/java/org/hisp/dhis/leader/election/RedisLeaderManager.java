package org.hisp.dhis.leader.election;

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

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

/**
 * Takes care of the leader election implementation backed by redis.
 * 
 * @author Ameen Mohamed
 */
public class RedisLeaderManager implements LeaderManager
{
    private static final String key = "dhis2:leader";

    private static final Log log = LogFactory.getLog( RedisLeaderManager.class );
    
    private static final String CLUSTER_LEADER_RENEWAL = "Cluster leader renewal";


    private String nodeId;

    private Long timeToLiveSeconds;

    private SchedulingManager schedulingManager;

    private RedisTemplate<String, ?> redisTemplate;

    public RedisLeaderManager( Long timeToLiveMinutes, RedisTemplate<String, ?> redisTemplate )
    {
        this.nodeId = UUID.randomUUID().toString();
        log.info( "Setting up redis based leader manager on NodeId:" + this.nodeId );
        this.timeToLiveSeconds = timeToLiveMinutes * 60;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void renewLeader()
    {
        if ( isLeader() )
        {
            log.debug( "Renewing leader with nodeId:" + this.nodeId );
            redisTemplate.getConnectionFactory().getConnection().expire( key.getBytes(), timeToLiveSeconds );
        }
    }

    @Override
    public void electLeader()
    {
        log.debug( "Election attempt by nodeId:" + this.nodeId );
        redisTemplate.getConnectionFactory().getConnection().set( key.getBytes(), nodeId.getBytes(),
            Expiration.from( timeToLiveSeconds, TimeUnit.SECONDS ), SetOption.SET_IF_ABSENT );
        if ( isLeader() )
        {
            renewLeader();
            Calendar calendar = Calendar.getInstance();
            calendar.add( Calendar.SECOND, (int) (this.timeToLiveSeconds / 2) );
            log.debug( "Next leader renewal job nodeId:" + this.nodeId + " set at " + calendar.getTime().toString() );
            JobConfiguration leaderRenewalJobConfiguration = new JobConfiguration( CLUSTER_LEADER_RENEWAL,
                JobType.LEADER_RENEWAL, null, null, false, true, true );
            leaderRenewalJobConfiguration.setLeaderOnlyJob( true );
            schedulingManager.scheduleJobWithStartTime( leaderRenewalJobConfiguration, calendar.getTime() );
        }
    }

    @Override
    public boolean isLeader()
    {
        byte[] leaderIdBytes = redisTemplate.getConnectionFactory().getConnection().get( key.getBytes() );
        String leaderId = null;
        if ( leaderIdBytes != null )
        {
            leaderId = new String( leaderIdBytes );
        }
        return nodeId.equals( leaderId );
    }

    @Override
    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

}