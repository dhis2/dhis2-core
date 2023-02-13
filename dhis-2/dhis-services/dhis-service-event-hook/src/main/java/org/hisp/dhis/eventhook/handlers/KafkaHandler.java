/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.eventhook.handlers;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hisp.dhis.eventhook.Event;
import org.hisp.dhis.eventhook.EventHook;
import org.hisp.dhis.eventhook.Handler;
import org.hisp.dhis.eventhook.targets.KafkaTarget;

/**
 * @author Morten Olav Hansen
 */
@Slf4j
public class KafkaHandler implements Handler
{
    private final KafkaTarget target;

    private KafkaProducer<String, String> producer;

    public KafkaHandler( KafkaTarget target )
    {
        this.target = target;
        configure( target );
    }

    /**
     * These are mostly defaults taken from {@see KafkaProducer} documentation.
     * Some of these are configurable today and we might want to expose more
     * config props in the future, for now we are using the defaults.
     *
     * @param target A ready configured KafkaTarget
     */
    private void configure( KafkaTarget target )
    {
        Map<String, Object> properties = Map.of(
            "client.id", target.getClientId(),
            "bootstrap.servers", target.getBootstrapServers(),
            "delivery.timeout.ms", 120000,
            "request.timeout.ms", 30000,
            "max.request.size", 1048576,
            "security.protocol", "PLAINTEXT", // PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL
            "acks", "all", // all (full replica), -1 (leader ack), 0 (no wait), 1 (full replica)
            "key.serializer", StringSerializer.class,
            "value.serializer", StringSerializer.class );

        this.producer = new KafkaProducer<>( properties );
    }

    @Override
    public void run( EventHook eventHook, Event event, String payload )
    {
        if ( producer == null )
        {
            log.error( "Kafka is not properly configured. Please check Event Hook '" + eventHook.getName()
                + "' with ID '" + eventHook.getUid() + "'" );
            return;
        }

        producer.send( new ProducerRecord<>( target.getTopic(), null, payload ) );
    }

    @Override
    public void close()
    {
        if ( producer != null )
        {
            producer.close();
        }
    }
}
