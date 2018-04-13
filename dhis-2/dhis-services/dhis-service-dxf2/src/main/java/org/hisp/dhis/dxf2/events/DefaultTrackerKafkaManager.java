package org.hisp.dhis.dxf2.events;

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

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.kafka.KafkaManager;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Tracker specific KafkaManager, uses JsonSerializer/JsonDeserializer to automatically serialize deserialize Jackson objects.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultTrackerKafkaManager
    implements TrackerKafkaManager
{
    private final KafkaManager kafkaManager;
    private final ConsumerFactory<String, Event> cfEvent;
    private final ConsumerFactory<String, Enrollment> cfEnrollment;
    private final ConsumerFactory<String, TrackedEntityInstance> cfTrackedEntity;

    private final ProducerFactory<String, Event> pfEvent;
    private final ProducerFactory<String, Enrollment> pfEnrollment;
    private final ProducerFactory<String, TrackedEntityInstance> pfTrackedEntity;

    private final KafkaTemplate<String, Event> ktEvent;
    private final KafkaTemplate<String, Enrollment> ktEnrollment;
    private final KafkaTemplate<String, TrackedEntityInstance> ktTrackedEntity;

    public DefaultTrackerKafkaManager( KafkaManager kafkaManager )
    {
        this.kafkaManager = kafkaManager;

        this.pfEvent = kafkaManager.getProducerFactory( new StringSerializer(), new JsonSerializer<>() );
        this.pfEnrollment = kafkaManager.getProducerFactory( new StringSerializer(), new JsonSerializer<>() );
        this.pfTrackedEntity = kafkaManager.getProducerFactory( new StringSerializer(), new JsonSerializer<>() );

        this.cfEvent = kafkaManager.getConsumerFactory( new StringDeserializer(), new JsonDeserializer<>( Event.class ), "bulk-events" );
        this.cfEnrollment = kafkaManager.getConsumerFactory( new StringDeserializer(), new JsonDeserializer<>( Enrollment.class ), "bulk-enrollments" );
        this.cfTrackedEntity = kafkaManager.getConsumerFactory( new StringDeserializer(), new JsonDeserializer<>( TrackedEntityInstance.class ), "bulk-tracked-entities" );

        this.ktEvent = kafkaManager.getKafkaTemplate( this.pfEvent );
        this.ktEnrollment = kafkaManager.getKafkaTemplate( this.pfEnrollment );
        this.ktTrackedEntity = kafkaManager.getKafkaTemplate( this.pfTrackedEntity );
    }

    @Override
    public ConsumerFactory<String, Event> getCfEvent()
    {
        return cfEvent;
    }

    @Override
    public ConsumerFactory<String, Enrollment> getCfEnrollment()
    {
        return cfEnrollment;
    }

    @Override
    public ConsumerFactory<String, TrackedEntityInstance> getCfTrackedEntity()
    {
        return cfTrackedEntity;
    }

    @Override
    public ProducerFactory<String, Event> getPfEvent()
    {
        return pfEvent;
    }

    @Override
    public ProducerFactory<String, Enrollment> getPfEnrollment()
    {
        return pfEnrollment;
    }

    @Override
    public ProducerFactory<String, TrackedEntityInstance> getPfTrackedEntity()
    {
        return pfTrackedEntity;
    }

    @Override
    public KafkaTemplate<String, Event> getKtEvent()
    {
        return ktEvent;
    }

    @Override
    public KafkaTemplate<String, Enrollment> getKtEnrollment()
    {
        return ktEnrollment;
    }

    @Override
    public KafkaTemplate<String, TrackedEntityInstance> getKtTrackedEntity()
    {
        return ktTrackedEntity;
    }
}
