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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.kafka.KafkaManager;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

/**
 * Tracker specific KafkaManager, uses JsonSerializer/JsonDeserializer to automatically serialize deserialize Jackson objects.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class DefaultTrackerKafkaManager
    implements TrackerKafkaManager, InitializingBean
{
    private static final Log log = LogFactory.getLog( DefaultTrackerKafkaManager.class );

    private final EventService eventService;
    private final EnrollmentService enrollmentService;
    private final TrackedEntityInstanceService trackedEntityInstanceService;
    private final KafkaManager kafkaManager;
    private final UserService userService;
    private final Notifier notifier;

    private ConsumerFactory<String, KafkaEvent> cfEvent;
    private ConsumerFactory<String, KafkaEnrollment> cfEnrollment;
    private ConsumerFactory<String, KafkaTrackedEntity> cfTrackedEntity;

    private ProducerFactory<String, KafkaEvent> pfEvent;
    private ProducerFactory<String, KafkaEnrollment> pfEnrollment;
    private ProducerFactory<String, KafkaTrackedEntity> pfTrackedEntity;

    private KafkaTemplate<String, KafkaEvent> ktEvent;
    private KafkaTemplate<String, KafkaEnrollment> ktEnrollment;
    private KafkaTemplate<String, KafkaTrackedEntity> ktTrackedEntity;

    private Consumer<String, KafkaEvent> cEvent;
    private Consumer<String, KafkaEnrollment> cEnrollment;
    private Consumer<String, KafkaTrackedEntity> cTrackedEntity;

    public DefaultTrackerKafkaManager( EventService eventService, EnrollmentService enrollmentService,
        TrackedEntityInstanceService trackedEntityInstanceService, KafkaManager kafkaManager, UserService userService, Notifier notifier )
    {
        this.eventService = eventService;
        this.enrollmentService = enrollmentService;
        this.trackedEntityInstanceService = trackedEntityInstanceService;
        this.kafkaManager = kafkaManager;
        this.userService = userService;
        this.notifier = notifier;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        if ( !kafkaManager.isEnabled() )
        {
            return;
        }

        ObjectMapper jsonMapper = DefaultRenderService.getJsonMapper();

        this.pfEvent = kafkaManager.getProducerFactory( new StringSerializer(), new JsonSerializer<>( jsonMapper ) );
        this.pfEnrollment = kafkaManager.getProducerFactory( new StringSerializer(), new JsonSerializer<>( jsonMapper ) );
        this.pfTrackedEntity = kafkaManager.getProducerFactory( new StringSerializer(), new JsonSerializer<>( jsonMapper ) );

        this.cfEvent = kafkaManager.getConsumerFactory(
            new StringDeserializer(), new JsonDeserializer<>( KafkaEvent.class, jsonMapper ), GROUP_BULK_EVENTS );

        this.cfEnrollment = kafkaManager.getConsumerFactory(
            new StringDeserializer(), new JsonDeserializer<>( KafkaEnrollment.class, jsonMapper ), GROUP_BULK_ENROLLMENTS );
        this.cfTrackedEntity = kafkaManager.getConsumerFactory(
            new StringDeserializer(), new JsonDeserializer<>( KafkaTrackedEntity.class, jsonMapper ), GROUP_BULK_TRACKED_ENTITIES );

        this.ktEvent = kafkaManager.getTemplate( this.pfEvent );
        this.ktEnrollment = kafkaManager.getTemplate( this.pfEnrollment );
        this.ktTrackedEntity = kafkaManager.getTemplate( this.pfTrackedEntity );

        // UUID.randomUUID() is required as of now since we are starting up in at least 3 different servlet contexts, and the consumer
        // MBean (JMX) needs a unique name to register with. Should be replaced at a later stage.
        this.cEvent = getCfEvent().createConsumer( "events-" + UUID.randomUUID().toString() );
        this.cEnrollment = getCfEnrollment().createConsumer( "enrollments-" + UUID.randomUUID().toString() );
        this.cTrackedEntity = getCfTrackedEntity().createConsumer( "tracked-entities-" + UUID.randomUUID().toString() );

        this.cTrackedEntity.subscribe( Collections.singletonList( TOPIC_BULK_TRACKED_ENTITIES ) );
        this.cEvent.subscribe( Collections.singleton( TOPIC_BULK_EVENTS ) );
        this.cEnrollment.subscribe( Collections.singleton( TOPIC_BULK_ENROLLMENTS ) );
    }

    @Override
    public boolean isEnabled()
    {
        return kafkaManager.isEnabled();
    }

    @Override
    public ConsumerFactory<String, KafkaEvent> getCfEvent()
    {
        return cfEvent;
    }

    @Override
    public ConsumerFactory<String, KafkaEnrollment> getCfEnrollment()
    {
        return cfEnrollment;
    }

    @Override
    public ConsumerFactory<String, KafkaTrackedEntity> getCfTrackedEntity()
    {
        return cfTrackedEntity;
    }

    @Override
    public ProducerFactory<String, KafkaEvent> getPfEvent()
    {
        return pfEvent;
    }

    @Override
    public ProducerFactory<String, KafkaEnrollment> getPfEnrollment()
    {
        return pfEnrollment;
    }

    @Override
    public ProducerFactory<String, KafkaTrackedEntity> getPfTrackedEntity()
    {
        return pfTrackedEntity;
    }

    @Override
    public KafkaTemplate<String, KafkaEvent> getKtEvent()
    {
        return ktEvent;
    }

    @Override
    public KafkaTemplate<String, KafkaEnrollment> getKtEnrollment()
    {
        return ktEnrollment;
    }

    @Override
    public KafkaTemplate<String, KafkaTrackedEntity> getKtTrackedEntity()
    {
        return ktTrackedEntity;
    }

    @Override
    public JobConfiguration dispatchEvents( User user, ImportOptions importOptions, List<Event> events )
    {
        // generate a common job id for this entire job (useful for grouping when consuming topics)
        String jobId = CodeGenerator.generateUid();

        for ( Event event : events )
        {
            if ( event.getEvent() == null )
            {
                event.setEvent( CodeGenerator.generateUid() );
            }

            ktEvent.send( TOPIC_BULK_EVENTS,
                new KafkaEvent( CodeGenerator.generateUid(), jobId, user.getUid(), importOptions, event ) );
        }

        JobConfiguration job = new JobConfiguration( "kafka-event-import", JobType.KAFKA_TRACKER,
            user.getUid(), true );
        job.setUid( jobId );

        notifier.notify( job, "Kafka Event job was queued." );
        notifier.addJobSummary( job, new ImportSummaries() );

        return job;
    }

    @Override
    public JobConfiguration dispatchEnrollments( User user, ImportOptions importOptions, List<Enrollment> enrollments )
    {
        // generate a common job id for this entire job (useful for grouping when consuming topics)
        String jobId = CodeGenerator.generateUid();

        for ( Enrollment enrollment : enrollments )
        {
            if ( enrollment.getEnrollment() == null )
            {
                enrollment.setEnrollment( CodeGenerator.generateUid() );
            }

            ktEnrollment.send( TOPIC_BULK_ENROLLMENTS,
                new KafkaEnrollment( CodeGenerator.generateUid(), jobId, user.getUid(), importOptions, enrollment ) );
        }

        JobConfiguration job = new JobConfiguration( "kafka-enrollment-import", JobType.KAFKA_TRACKER,
            user.getUid(), true );
        job.setUid( jobId );

        notifier.notify( job, "Kafka Enrollment job was queued." );
        notifier.addJobSummary( job, new ImportSummaries() );

        return job;
    }

    @Override
    public JobConfiguration dispatchTrackedEntities( User user, ImportOptions importOptions, List<TrackedEntityInstance> trackedEntities )
    {
        // generate a common job id for this entire job (useful for grouping when consuming topics)
        String jobId = CodeGenerator.generateUid();

        for ( TrackedEntityInstance trackedEntity : trackedEntities )
        {
            if ( trackedEntity.getTrackedEntityInstance() == null )
            {
                trackedEntity.setTrackedEntityInstance( CodeGenerator.generateUid() );
            }

            ktTrackedEntity.send( TOPIC_BULK_TRACKED_ENTITIES,
                new KafkaTrackedEntity( CodeGenerator.generateUid(), jobId, user.getUid(), importOptions, trackedEntity ) );
        }

        JobConfiguration job = new JobConfiguration( "kafka-tracked-entity-import", JobType.KAFKA_TRACKER,
            user.getUid(), true );
        job.setUid( jobId );

        notifier.notify( job, "Kafka Tracked Entity job was queued." );
        notifier.addJobSummary( job, new ImportSummaries() );

        return job;
    }

    @Override
    public void consumeEvents( JobConfiguration jobConfiguration )
    {
        ConsumerRecords<String, KafkaEvent> consumerRecords = cEvent.poll( 1000 );

        StreamSupport.stream( consumerRecords.spliterator(), false )
            .collect( groupingBy( record -> record.value().getJobId() ) )
            .forEach( ( key, records ) -> {
                KafkaEvent kafkaEvent = records.get( 0 ).value();
                User user = userService.getUser( kafkaEvent.getUser() );

                if ( user == null )
                {
                    log.warn( "User with ID " + kafkaEvent.getUser() + " not found." );
                    return;
                }

                JobConfiguration job = new JobConfiguration( "kafka-event-import", JobType.KAFKA_TRACKER,
                    user.getUid(), true );
                job.setUid( kafkaEvent.getJobId() );

                ImportSummaries importSummaries = (ImportSummaries) notifier.getJobSummaryByJobId( JobType.KAFKA_TRACKER, job.getUid() );

                if ( importSummaries == null )
                {
                    importSummaries = new ImportSummaries();
                }

                ImportOptions importOptions = kafkaEvent.getImportOptions().setUser( user );

                List<Event> events = records.stream().map( x -> x.value().getPayload() )
                    .collect( Collectors.toList() );

                String msg = "Importing " + events.size() + " events for user " + user.getUsername();

                log.info( msg );
                notifier.notify( job, msg );

                importSummaries.addImportSummaries( eventService.addEvents( events, importOptions, true ) );
                notifier.addJobSummary( job, importSummaries );
            } );

        cEvent.commitSync();
    }

    @Override
    public void consumeEnrollments( JobConfiguration jobConfiguration )
    {
        ConsumerRecords<String, KafkaEnrollment> consumerRecords = cEnrollment.poll( 1000 );

        StreamSupport.stream( consumerRecords.spliterator(), false )
            .collect( groupingBy( record -> record.value().getJobId() ) )
            .forEach( ( key, records ) -> {
                KafkaEnrollment kafkaEnrollment = records.get( 0 ).value();
                User user = userService.getUser( kafkaEnrollment.getUser() );

                if ( user == null )
                {
                    log.warn( "User with ID " + kafkaEnrollment.getUser() + " not found." );
                    return;
                }

                JobConfiguration job = new JobConfiguration( "kafka-enrollment-import", JobType.KAFKA_TRACKER,
                    user.getUid(), true );
                job.setUid( kafkaEnrollment.getJobId() );

                ImportSummaries importSummaries = (ImportSummaries) notifier.getJobSummaryByJobId( JobType.KAFKA_TRACKER, job.getUid() );

                if ( importSummaries == null )
                {
                    importSummaries = new ImportSummaries();
                }

                ImportOptions importOptions = kafkaEnrollment.getImportOptions().setUser( user );

                List<Enrollment> enrollments = records.stream().map( x -> x.value().getPayload() )
                    .collect( Collectors.toList() );

                String msg = "Importing " + enrollments.size() + " enrollments for user " + user.getUsername();

                log.info( msg );
                notifier.notify( job, msg );

                importSummaries.addImportSummaries( enrollmentService.addEnrollments( enrollments, importOptions, false ) );
                notifier.addJobSummary( job, importSummaries );
            } );

        cEnrollment.commitSync();
    }

    @Override
    public void consumeTrackedEntities( JobConfiguration jobConfiguration )
    {
        ConsumerRecords<String, KafkaTrackedEntity> consumerRecords = cTrackedEntity.poll( 1000 );

        StreamSupport.stream( consumerRecords.spliterator(), false )
            .collect( groupingBy( record -> record.value().getJobId() ) )
            .forEach( ( key, records ) -> {
                KafkaTrackedEntity kafkaTrackedEntity = records.get( 0 ).value();
                User user = userService.getUser( kafkaTrackedEntity.getUser() );

                if ( user == null )
                {
                    log.warn( "User with ID " + kafkaTrackedEntity.getUser() + " not found." );
                    return;
                }

                JobConfiguration job = new JobConfiguration( "kafka-tracked-entity-import", JobType.KAFKA_TRACKER,
                    user.getUid(), true );
                job.setUid( kafkaTrackedEntity.getJobId() );

                ImportSummaries importSummaries = (ImportSummaries) notifier.getJobSummaryByJobId( JobType.KAFKA_TRACKER, job.getUid() );

                if ( importSummaries == null )
                {
                    importSummaries = new ImportSummaries();
                }

                ImportOptions importOptions = kafkaTrackedEntity.getImportOptions().setUser( user );

                List<TrackedEntityInstance> trackedEntityInstances = records.stream().map( x -> x.value().getPayload() )
                    .collect( Collectors.toList() );

                String msg = "Importing " + trackedEntityInstances.size() + " tracked entities for user " + user.getUsername();

                log.info( msg );
                notifier.notify( job, msg );

                importSummaries.addImportSummaries( trackedEntityInstanceService.addTrackedEntityInstances( trackedEntityInstances, importOptions ) );
                notifier.addJobSummary( job, importSummaries );
            } );

        cTrackedEntity.commitSync();
    }
}
