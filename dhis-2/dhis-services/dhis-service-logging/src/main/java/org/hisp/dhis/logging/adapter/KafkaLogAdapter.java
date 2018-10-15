package org.hisp.dhis.logging.adapter;

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

import org.apache.kafka.common.serialization.StringSerializer;
import org.hisp.dhis.kafka.KafkaManager;
import org.hisp.dhis.logging.Log;
import org.hisp.dhis.logging.LogAdapter;
import org.hisp.dhis.logging.LogEvent;
import org.hisp.dhis.logging.LoggingConfig;
import org.hisp.dhis.logging.LoggingManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class KafkaLogAdapter implements LogAdapter, InitializingBean
{
    private final KafkaManager kafkaManager;
    private ProducerFactory<String, Log> pfLog;
    private KafkaTemplate<String, Log> ktLog;

    public KafkaLogAdapter( KafkaManager kafkaManager )
    {
        this.kafkaManager = kafkaManager;
    }

    @Override
    public void log( Log log, LoggingConfig config )
    {
        ktLog.send( config.getKafkaTopic(), log );
    }

    @Override
    public boolean isEnabled( LogEvent event )
    {
        if ( event == null || event.getLog() == null )
        {
            return false;
        }

        LoggingConfig config = event.getConfig();

        return kafkaManager.isEnabled() && config.isKafkaEnabled() &&
            config.getKafkaLevel().isEnabled( event.getLog().getLogLevel() );
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        if ( !kafkaManager.isEnabled() )
        {
            return;
        }

        this.pfLog = kafkaManager.getProducerFactory( new StringSerializer(), new JsonSerializer<>(
            LoggingManager.objectMapper
        ) );

        this.ktLog = kafkaManager.getTemplate( this.pfLog );
    }
}
