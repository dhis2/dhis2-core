package org.hisp.dhis.dxf2.monitoring;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

/**
 * @author Lars Helge Overland
 */
public class DefaultMonitoringService
    implements MonitoringService
{
    private static final Log log = LogFactory.getLog( DefaultMonitoringService.class );
    
    private static final int PUSH_INTERVAL = DateTimeConstants.MILLIS_PER_SECOND * 5;
    
    @Autowired
    private SystemService systemService;
    
    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private TaskScheduler scheduler;

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent event )
    {
        Date date = new DateTime().plus( PUSH_INTERVAL ).toDate();
        
        scheduler.scheduleWithFixedDelay( () -> pushMonitoringInfo(), date, PUSH_INTERVAL );
        
        log.info( "Scheduled monitoring push service" );
    }
    
    @Override
    public void pushMonitoringInfo()
    {
        String url = (String) systemSettingManager.getSystemSetting( SettingKey.MONITORING_SERVICE_URL );
        
        if ( StringUtils.trimToNull( url ) == null )
        {
            log.warn( "Monitoring service URL not configured, aborting monitoring request" );
            return;
        }
        
        SystemInfo systemInfo = systemService.getSystemInfo();
        
        if ( systemInfo == null )
        {
            log.warn( "System info not available, aborting monitoring request" );
            return;
        }

        systemInfo.clearSensitiveInfo();
        
        ResponseEntity<String> response = null;
        HttpStatus sc = null;
        
        try
        {
            response = restTemplate.postForEntity( url, systemInfo, String.class );
            sc = response.getStatusCode();
        }
        catch ( HttpClientErrorException | HttpServerErrorException ex )
        {
            log.warn( "Monitoring request failed, status code: " + sc, ex );
            return;
        }
        catch ( ResourceAccessException ex )
        {
            log.warn( "Monitoring request failed, network is unreachable" );
            return;
        }
        
        if ( response != null && sc != null && sc.is2xxSuccessful() )
        {
            log.info( "Monitoring request successfully sent" );
        }
        else
        {
            log.warn( "Monitoring request was unsuccessful, status code: " + sc );
        }
    }
}
