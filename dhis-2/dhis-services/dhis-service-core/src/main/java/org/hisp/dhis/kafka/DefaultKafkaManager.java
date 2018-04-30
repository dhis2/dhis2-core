package org.hisp.dhis.kafka;

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

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hisp.dhis.system.SystemService;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * These methods should be considered utility methods, don't call these for every single kafka call. Get the template or factory you need
 * and reuse it for your requests.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultKafkaManager implements KafkaManager
{
    private final SystemService systemService;

    public DefaultKafkaManager( SystemService systemService )
    {
        this.systemService = systemService;
    }

    @Override
    public boolean isEnabled()
    {
        Kafka kafka = systemService.getSystemInfo().getKafka();
        return kafka != null && kafka.isValid();
    }

    @Override
    public KafkaAdmin getAdmin()
    {
        Kafka kafka = systemService.getSystemInfo().getKafka();

        Map<String, Object> props = new HashMap<>();
        props.put( AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers() );

        return new KafkaAdmin( props );
    }

    @Override
    public KafkaTemplate<String, String> getTemplate()
    {
        return getTemplate( new StringSerializer(), new StringSerializer() );
    }

    @Override
    public ConsumerFactory<String, String> getConsumerFactory( String group )
    {
        return getConsumerFactory( new StringDeserializer(), new StringDeserializer(), group );
    }

    @Override
    public ProducerFactory<String, String> getProducerFactory()
    {
        return getProducerFactory( new StringSerializer(), new StringSerializer() );
    }

    @Override
    public <K, V> KafkaTemplate<K, V> getTemplate( ProducerFactory<K, V> producerFactory )
    {
        return new KafkaTemplate<>( producerFactory );
    }

    @Override
    public <K, V> KafkaTemplate<K, V> getTemplate( Serializer<K> keySerializer, Serializer<V> serializer )
    {
        return getTemplate( getProducerFactory( keySerializer, serializer ) );
    }

    /**
     * Build a kafka consumer factory for a given group-id.
     */
    @Override
    public <K, V> ConsumerFactory<K, V> getConsumerFactory( Deserializer<K> keyDeserializer, Deserializer<V> deserializer, String group )
    {
        Kafka kafka = systemService.getSystemInfo().getKafka();

        Map<String, Object> props = new HashMap<>();
        props.put( ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers() );
        props.put( ConsumerConfig.GROUP_ID_CONFIG, group );
        props.put( ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true );
        props.put( ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100" );
        props.put( ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000" );

        return new DefaultKafkaConsumerFactory<>(
            props, keyDeserializer, deserializer
        );
    }

    @Override
    public <K, V> ProducerFactory<K, V> getProducerFactory( Serializer<K> keySerializer, Serializer<V> serializer )
    {
        Kafka kafka = systemService.getSystemInfo().getKafka();

        Map<String, Object> props = new HashMap<>();
        props.put( ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers() );
        props.put( ProducerConfig.RETRIES_CONFIG, kafka.getRetries() );
        props.put( ProducerConfig.BATCH_SIZE_CONFIG, 16384 );
        props.put( ProducerConfig.LINGER_MS_CONFIG, 10 );
        props.put( ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432 );
        props.put( ProducerConfig.ACKS_CONFIG, 1 );

        return new DefaultKafkaProducerFactory<>(
            props, keySerializer, serializer
        );
    }

    @Override
    public <K, V> ConcurrentKafkaListenerContainerFactory<K, V> getListenerContainerFactory(
        Deserializer<K> keyDeserializer, Deserializer<V> deserializer, String group )
    {
        ConcurrentKafkaListenerContainerFactory<K, V> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory( getConsumerFactory( keyDeserializer, deserializer, group ) );

        return factory;
    }
}
