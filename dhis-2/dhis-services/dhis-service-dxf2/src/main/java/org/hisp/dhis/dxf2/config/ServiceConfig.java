package org.hisp.dhis.dxf2.config;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE;
import static org.hisp.dhis.importexport.ImportStrategy.DELETE;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.insert.preprocess.EventGeometryPreProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.EventStoredByPreProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.ImportOptionsPreProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.preprocess.ProgramInstancePreProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.preprocess.ProgramStagePreProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.validation.AttributeOptionComboAclCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.AttributeOptionComboCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.AttributeOptionComboDateCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.EventBaseCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.EventCreationAclCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.EventDateCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.EventGeometryCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.OrgUnitCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.ProgramCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.ProgramInstanceCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.ProgramInstanceRepeatableStageCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.ProgramOrgUnitCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.ProgramStageCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.TrackedEntityInstanceCheck;
import org.hisp.dhis.dxf2.events.importer.update.postprocess.ProgramNotificationPostProcessor;
import org.hisp.dhis.dxf2.events.importer.update.postprocess.PublishEventPostProcessor;
import org.hisp.dhis.dxf2.events.importer.update.preprocess.ProgramInstanceGeometryPreProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.validation.*;
import org.hisp.dhis.dxf2.events.importer.update.preprocess.ProgramInstanceUpdatePreProcessor;
import org.hisp.dhis.dxf2.events.importer.update.validation.EventSimpleCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ProgramStageInstanceAclCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ProgramStageInstanceAuthCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ProgramStageInstanceBasicCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.DataValueCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.CreationCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DeletionCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DuplicateIdsCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.MandatoryAttributesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ReferencesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.SchemaCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.SecurityCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UniqueAttributesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UniquenessCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UpdateCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationHooksCheck;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.external.conf.ConfigurationPropertyFactoryBean;
import org.hisp.dhis.importexport.ImportStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
@Configuration( "dxf2ServiceConfig" )
public class ServiceConfig
{
    @Autowired
    @Qualifier( "initialInterval" )
    private ConfigurationPropertyFactoryBean initialInterval;

    @Autowired
    @Qualifier( "maxAttempts" )
    private ConfigurationPropertyFactoryBean maxAttempts;

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        return new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    @Bean( "retryTemplate" )
    public RetryTemplate retryTemplate()
    {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

        backOffPolicy.setInitialInterval( Long.parseLong( (String) initialInterval.getObject() ) );

        Map<Class<? extends Throwable>, Boolean> exceptionMap = new HashMap<>();
        exceptionMap.put( MetadataSyncServiceException.class, true );
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(
                Integer.parseInt( (String) maxAttempts.getObject() ), exceptionMap );

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy( backOffPolicy );
        retryTemplate.setRetryPolicy( simpleRetryPolicy );

        return retryTemplate;
    }

    private final static List<Class<? extends ValidationCheck>> CREATE_UPDATE_CHECKS = Lists.newArrayList(
            DuplicateIdsCheck.class, ValidationHooksCheck.class, SecurityCheck.class, SchemaCheck.class,
            UniquenessCheck.class, MandatoryAttributesCheck.class, UniqueAttributesCheck.class, ReferencesCheck.class );

    private final static List<Class<? extends ValidationCheck>> CREATE_CHECKS = Lists.newArrayList(
            DuplicateIdsCheck.class, ValidationHooksCheck.class, SecurityCheck.class, CreationCheck.class, SchemaCheck.class,
            UniquenessCheck.class, MandatoryAttributesCheck.class, UniqueAttributesCheck.class, ReferencesCheck.class );

    private final static List<Class<? extends ValidationCheck>> UPDATE_CHECKS = Lists.newArrayList(
            DuplicateIdsCheck.class, ValidationHooksCheck.class, SecurityCheck.class, UpdateCheck.class, SchemaCheck.class,
            UniquenessCheck.class, MandatoryAttributesCheck.class, UniqueAttributesCheck.class, ReferencesCheck.class );

    private final static List<Class<? extends ValidationCheck>> DELETE_CHECKS = Lists.newArrayList( SecurityCheck.class,
            DeletionCheck.class );

    
    @Bean( "validatorMap" )
    public Map<ImportStrategy, List<Class<? extends ValidationCheck>>> validatorMap()
    {
        // @formatter:off
        return ImmutableMap.of(
            ImportStrategy.CREATE_AND_UPDATE, CREATE_UPDATE_CHECKS,
            CREATE, CREATE_CHECKS,
            ImportStrategy.UPDATE, UPDATE_CHECKS,
            ImportStrategy.DELETE, DELETE_CHECKS );
        // @formatter:on
    }

    /*
     * TRACKER EVENT IMPORT VALIDATION
     */

    @Bean
    public Map<ImportStrategy, List<Class<? extends Checker>>> eventInsertValidatorMap()
    {
        // @formatter:off
        return ImmutableMap.of( CREATE, newArrayList(
            EventDateCheck.class,
            OrgUnitCheck.class,
            ProgramCheck.class,
            ProgramStageCheck.class,
            TrackedEntityInstanceCheck.class,
            ProgramInstanceCheck.class,
            ProgramInstanceRepeatableStageCheck.class,
            ProgramOrgUnitCheck.class,
            EventGeometryCheck.class,
            EventCreationAclCheck.class,
            EventBaseCheck.class,
            AttributeOptionComboCheck.class,
            AttributeOptionComboDateCheck.class,
            AttributeOptionComboAclCheck.class,
            DataValueCheck.class,
            DataValueAclCheck.class
            )
        );
        // @formatter:on
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Checker>>> eventUpdateValidatorMap()
    {
        // @formatter:off
        return ImmutableMap.of( UPDATE, newArrayList(
            EventSimpleCheck.class,
            EventBaseCheck.class,
            ProgramStageInstanceBasicCheck.class,
            ProgramStageInstanceAclCheck.class,
            ProgramCheck.class,
            ProgramInstanceCheck.class,
            ProgramStageInstanceAuthCheck.class,
            AttributeOptionComboCheck.class,
            AttributeOptionComboDateCheck.class,
            EventGeometryCheck.class,
            DataValueCheck.class
        ) );
        // @formatter:on
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Checker>>> eventDeleteValidatorMap()
    {
        // @formatter:off
        return ImmutableMap.of( DELETE, newArrayList(
                org.hisp.dhis.dxf2.events.importer.delete.validation.ProgramStageInstanceAclCheck.class
        ) );
        // @formatter:on
    }

    /*
     * TRACKER EVENT PRE/POST PROCESSING
     */

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventInsertPreProcessorsMap()
    {
        // @formatter:off
        return ImmutableMap.of( CREATE, newArrayList(
            ImportOptionsPreProcessor.class,
            EventStoredByPreProcessor.class,
            ProgramInstancePreProcessor.class,
            ProgramStagePreProcessor.class,
            EventGeometryPreProcessor.class
        ) );
        // @formatter:on
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventUpdatePreProcessorMap()
    {
        // @formatter:off
        return ImmutableMap.of( UPDATE, newArrayList(
            ImportOptionsPreProcessor.class,
            EventStoredByPreProcessor.class,
            ProgramInstanceUpdatePreProcessor.class,
            ProgramInstanceGeometryPreProcessor.class
        ) );
        // @formatter:on
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventUpdatePostProcessorMap()
    {
        // @formatter:off
        return ImmutableMap.of( UPDATE, newArrayList(
            PublishEventPostProcessor.class,
            ProgramNotificationPostProcessor.class
        ) );
        // @formatter:on
    }
}
