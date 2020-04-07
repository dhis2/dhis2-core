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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.dxf2.events.event.preProcess.PreProcessor;
import org.hisp.dhis.dxf2.events.event.preProcess.ProgramInstancePreProcessor;
import org.hisp.dhis.dxf2.events.event.preProcess.ProgramStagePreProcessor;
import org.hisp.dhis.dxf2.events.event.validation.AttributeOptionComboAclCheck;
import org.hisp.dhis.dxf2.events.event.validation.AttributeOptionComboCheck;
import org.hisp.dhis.dxf2.events.event.validation.EventCreationAclCheck;
import org.hisp.dhis.dxf2.events.event.validation.EventBaseCheck;
import org.hisp.dhis.dxf2.events.event.validation.EventDateCheck;
import org.hisp.dhis.dxf2.events.event.validation.EventGeometryCheck;
import org.hisp.dhis.dxf2.events.event.validation.OrgUnitCheck;
import org.hisp.dhis.dxf2.events.event.validation.ProgramCheck;
import org.hisp.dhis.dxf2.events.event.validation.ProgramInstanceCheck;
import org.hisp.dhis.dxf2.events.event.validation.ProgramOrgUnitCheck;
import org.hisp.dhis.dxf2.events.event.validation.ProgramStageCheck;
import org.hisp.dhis.dxf2.events.event.validation.TrackedEntityInstanceCheck;
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
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
@Configuration( "dxf2ServiceConfig" )
@SuppressWarnings( "unchecked" )
public class ServiceConfig
{
    @Autowired
    @Qualifier( "initialInterval" )
    private ConfigurationPropertyFactoryBean initialInterval;

    @Autowired
    @Qualifier( "maxAttempts" )
    private ConfigurationPropertyFactoryBean maxAttempts;

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

    /*
     *
     * Default validation chains for each Metadata Import Strategy
     *
     */

    private final static List<Class<? extends ValidationCheck>> CREATE_UPDATE_CHECKS = Lists.newArrayList(
    // @formatter:off
        DuplicateIdsCheck.class,
        ValidationHooksCheck.class,
        SecurityCheck.class,
        SchemaCheck.class,
        UniquenessCheck.class,
        MandatoryAttributesCheck.class,
        UniqueAttributesCheck.class,
        ReferencesCheck.class
    // @formatter:on
    );

    private final static List<Class<? extends ValidationCheck>> CREATE_CHECKS = Lists.newArrayList(
    // @formatter:off
        DuplicateIdsCheck.class,
        ValidationHooksCheck.class,
        SecurityCheck.class,
        CreationCheck.class,
        SchemaCheck.class,
        UniquenessCheck.class,
        MandatoryAttributesCheck.class,
        UniqueAttributesCheck.class,
        ReferencesCheck.class
    // @formatter:on
    );

    private final static List<Class<? extends ValidationCheck>> UPDATE_CHECKS = Lists.newArrayList(
    // @formatter:off
        DuplicateIdsCheck.class,
        ValidationHooksCheck.class,
        SecurityCheck.class,
        UpdateCheck.class,
        SchemaCheck.class,
        UniquenessCheck.class,
        MandatoryAttributesCheck.class,
        UniqueAttributesCheck.class,
        ReferencesCheck.class
    // @formatter:on
    );

    private final static List<Class<? extends ValidationCheck>> DELETE_CHECKS = Lists.newArrayList(
    // @formatter:off
        SecurityCheck.class,
        DeletionCheck.class
    // @formatter:on
    );

    @Bean( "validatorMap" )
    public Map<ImportStrategy, List<Class<? extends ValidationCheck>>> validatorMap()
    {
        // @formatter:off
        return ImmutableMap.of(
            ImportStrategy.CREATE_AND_UPDATE, CREATE_UPDATE_CHECKS,
            ImportStrategy.CREATE, CREATE_CHECKS,
            ImportStrategy.UPDATE, UPDATE_CHECKS,
            ImportStrategy.DELETE, DELETE_CHECKS );
        // @formatter:on
    }

    /*
     *
     * Default validation chains for each Tracker Import Strategy
     *
     */

    private final static List<Class<? extends org.hisp.dhis.dxf2.events.event.validation.ValidationCheck>> CREATE_EVENTS_CHECKS = Lists
        .newArrayList(
        // @formatter:off
            EventDateCheck.class,
            OrgUnitCheck.class,
            ProgramCheck.class,
            ProgramStageCheck.class,
            TrackedEntityInstanceCheck.class,
            ProgramInstanceCheck.class,
            ProgramOrgUnitCheck.class,
            EventGeometryCheck.class,
            EventCreationAclCheck.class,
            EventBaseCheck.class,
            AttributeOptionComboCheck.class,
            AttributeOptionComboAclCheck.class
        );
        // @formatter:on

    @Bean( "eventValidatorMap" )
    public Map<ImportStrategy, List<Class<? extends org.hisp.dhis.dxf2.events.event.validation.ValidationCheck>>> eventValidatorMap()
    {
        return ImmutableMap.of(
            ImportStrategy.CREATE, CREATE_EVENTS_CHECKS,
            ImportStrategy.CREATE_AND_UPDATE, CREATE_EVENTS_CHECKS,
            ImportStrategy.NEW_AND_UPDATES, CREATE_EVENTS_CHECKS );
    }

    private final static List<Class<? extends PreProcessor>> CREATE_EVENTS_PREPROCESS = Lists
        .newArrayList( ProgramInstancePreProcessor.class, ProgramStagePreProcessor.class );

    @Bean( "eventPreProcessorsMap" )
    public Map<ImportStrategy, List<Class<? extends PreProcessor>>> eventPreProcessorsMap()
    {
        return ImmutableMap.of(
            ImportStrategy.CREATE, CREATE_EVENTS_PREPROCESS,
            ImportStrategy.CREATE_AND_UPDATE, CREATE_EVENTS_PREPROCESS,
            ImportStrategy.NEW_AND_UPDATES, CREATE_EVENTS_PREPROCESS );
    }
}
