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

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface KafkaManager
{
    boolean isEnabled();

    KafkaAdmin getAdmin();

    //--------------------------------------------------------------------------
    // String based kafka serializer/deserializer
    //--------------------------------------------------------------------------

    KafkaTemplate<String, String> getTemplate();

    ConsumerFactory<String, String> getConsumerFactory( String group );

    ProducerFactory<String, String> getProducerFactory();

    //--------------------------------------------------------------------------
    // Generic implementations, requires serializer/deserializer instances
    //--------------------------------------------------------------------------

    <K, V> KafkaTemplate<K, V> getTemplate( ProducerFactory<K, V> producerFactory );

    <K, V> KafkaTemplate<K, V> getTemplate( Serializer<K> keySerializer, Serializer<V> serializer );

    <K, V> ConsumerFactory<K, V> getConsumerFactory( Deserializer<K> keyDeserializer, Deserializer<V> deserializer, String group );

    <K, V> ProducerFactory<K, V> getProducerFactory( Serializer<K> keySerializer, Serializer<V> serializer );
}
