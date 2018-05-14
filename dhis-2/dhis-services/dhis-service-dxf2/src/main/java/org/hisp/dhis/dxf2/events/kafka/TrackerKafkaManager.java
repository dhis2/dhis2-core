package org.hisp.dhis.dxf2.events.kafka;

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

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface TrackerKafkaManager
{
    String TOPIC_BULK_EVENTS = "bulk-events";
    String TOPIC_BULK_ENROLLMENTS = "bulk-enrollments";
    String TOPIC_BULK_TRACKED_ENTITIES = "bulk-tracked-entities";

    String GROUP_BULK_EVENTS = "bulk-events-1";
    String GROUP_BULK_ENROLLMENTS = "bulk-enrollments-1";
    String GROUP_BULK_TRACKED_ENTITIES = "bulk-tracked-entities-1";

    @EventListener void init( ContextRefreshedEvent event );

    boolean isEnabled();

    ConsumerFactory<String, KafkaEvent> getCfEvent();

    ConsumerFactory<String, KafkaEnrollment> getCfEnrollment();

    ConsumerFactory<String, KafkaTrackedEntity> getCfTrackedEntity();

    ProducerFactory<String, KafkaEvent> getPfEvent();

    ProducerFactory<String, KafkaEnrollment> getPfEnrollment();

    ProducerFactory<String, KafkaTrackedEntity> getPfTrackedEntity();

    KafkaTemplate<String, KafkaEvent> getKtEvent();

    KafkaTemplate<String, KafkaEnrollment> getKtEnrollment();

    KafkaTemplate<String, KafkaTrackedEntity> getKtTrackedEntity();

    void dispatchEvents( User user, ImportOptions importOptions, List<Event> events );

    void dispatchEnrollments( User user, ImportOptions importOptions, List<Enrollment> enrollments );

    void dispatchTrackedEntities( User user, ImportOptions importOptions, List<TrackedEntityInstance> trackedEntities );

    void consumeEvents();

    void consumeEnrollments();

    void consumeTrackedEntities();
}
