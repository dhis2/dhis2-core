package org.hisp.dhis.amqp;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.amqp.stats.CpuStats;
import org.hisp.dhis.amqp.stats.DiskStats;
import org.hisp.dhis.amqp.stats.MemoryStats;
import org.hisp.dhis.amqp.stats.MemoryUsage;
import org.hisp.dhis.amqp.stats.Stats;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class StatsCollector
{
    private final Log log = LogFactory.getLog( StatsCollector.class );

    private final AmqpManager amqpManager;

    public StatsCollector( AmqpManager amqpManager )
    {
        this.amqpManager = amqpManager;
    }

    @PostConstruct
    public void init() throws Exception
    {
        AmqpClient client = amqpManager.getClient();
        client.createTopic( "dhis2.stats" );
        client.close();
    }

    @Scheduled( fixedRate = 5_000, initialDelay = 10_000 )
    public void runner()
    {
        CpuStats cpuStats = new CpuStats();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        cpuStats.setSystemLoadAverage( operatingSystemMXBean.getSystemLoadAverage() );

        MemoryStats memoryStats = new MemoryStats();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        Runtime runtime = Runtime.getRuntime();

        memoryStats.setHeapMemoryUsage( new MemoryUsage( memoryMXBean.getHeapMemoryUsage() ) );
        memoryStats.setNonHeapMemory( new MemoryUsage( memoryMXBean.getNonHeapMemoryUsage() ) );
        memoryStats.setFree( runtime.freeMemory() / (1024d * 1024d) );
        memoryStats.setMax( runtime.maxMemory() / (1024d * 1024d) );
        memoryStats.setTotal( runtime.totalMemory() / (1024d * 1024d) );

        Stats stats = new Stats();

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        stats.setUptime( runtimeMXBean.getUptime() / 1000d / 60d );
        stats.setStartDate( ZonedDateTime.ofInstant( Instant.ofEpochMilli( runtimeMXBean.getStartTime() ), ZoneId.systemDefault() ) );

        stats.setCpu( cpuStats );
        stats.setMemory( memoryStats );

        File[] roots = File.listRoots();

        for ( File root : roots )
        {
            DiskStats diskStats = new DiskStats();
            diskStats.setAbsolutePath( root.getAbsolutePath() );
            diskStats.setTotalSpace( root.getTotalSpace() / (1024d * 1024d) );
            diskStats.setFreeSpace( root.getFreeSpace() / (1024d * 1024d) );
            diskStats.setUsableSpace( root.getUsableSpace() / (1024d * 1024d) );

            stats.getDisks().add( diskStats );
        }

        log.info( AmqpClient.toJson( stats ) );

        AmqpClient client = amqpManager.getClient();
        client.sendTopic( "dhis2.stats", stats );
        client.close();
    }
}
