/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.config;

import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE;
import static org.hisp.dhis.importexport.ImportStrategy.DELETE;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.delete.postprocess.EventDeleteAuditPostProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.postprocess.EventInsertAuditPostProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.preprocess.EventGeometryPreProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.preprocess.ProgramInstancePreProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.preprocess.ProgramStagePreProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.preprocess.UserInfoInsertPreProcessor;
import org.hisp.dhis.dxf2.events.importer.insert.validation.AttributeOptionComboAclCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.DataValueAclCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.EventCreationAclCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.EventDateCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.OrgUnitCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.ProgramInstanceRepeatableStageCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.ProgramOrgUnitCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.ProgramStageCheck;
import org.hisp.dhis.dxf2.events.importer.insert.validation.TrackedEntityInstanceCheck;
import org.hisp.dhis.dxf2.events.importer.shared.postprocess.ProgramNotificationPostProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.EventStatusPreProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.EventStoredByPreProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.FilteringOutUndeclaredDataElementsProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.ImportOptionsPreProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.validation.AttributeOptionComboCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.AttributeOptionComboDateCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.DataValueCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.EventBaseCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.EventGeometryCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.FilteredDataValueCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.ProgramCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.ProgramInstanceCheck;
import org.hisp.dhis.dxf2.events.importer.update.postprocess.EventUpdateAuditPostProcessor;
import org.hisp.dhis.dxf2.events.importer.update.postprocess.PublishEventPostProcessor;
import org.hisp.dhis.dxf2.events.importer.update.preprocess.ProgramInstanceGeometryPreProcessor;
import org.hisp.dhis.dxf2.events.importer.update.preprocess.ProgramStageInstanceUpdatePreProcessor;
import org.hisp.dhis.dxf2.events.importer.update.preprocess.UserInfoUpdatePreProcessor;
import org.hisp.dhis.dxf2.events.importer.update.validation.EventSimpleCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ExpirationDaysCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ProgramStageInstanceAclCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ProgramStageInstanceAuthCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ProgramStageInstanceBasicCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.CreationCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DeletionCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DuplicateIdsCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.MandatoryAttributesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.NotOwnerReferencesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ReferencesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.SchemaCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.SecurityCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.TranslationsCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UniqueAttributesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UniqueMultiPropertiesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UniquenessCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UpdateCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationHooksCheck;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.external.conf.ConfigurationPropertyFactoryBean;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.action.validation.AlwaysValidProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.AssignProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.BaseProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.HideOptionProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.HideProgramStageProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.HideSectionProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.NotificationProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.ShowHideOptionGroupProgramRuleActionValidator;
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

    private final static List<Class<? extends ValidationCheck>> CREATE_UPDATE_CHECKS = newArrayList(
        DuplicateIdsCheck.class,
        ValidationHooksCheck.class,
        SecurityCheck.class,
        SchemaCheck.class,
        UniquenessCheck.class,
        UniqueMultiPropertiesCheck.class,
        MandatoryAttributesCheck.class,
        UniqueAttributesCheck.class,
        ReferencesCheck.class,
        NotOwnerReferencesCheck.class,
        TranslationsCheck.class );

    private final static List<Class<? extends ValidationCheck>> CREATE_CHECKS = newArrayList(
        DuplicateIdsCheck.class,
        ValidationHooksCheck.class,
        SecurityCheck.class,
        CreationCheck.class,
        SchemaCheck.class,
        UniquenessCheck.class,
        UniqueMultiPropertiesCheck.class,
        MandatoryAttributesCheck.class,
        UniqueAttributesCheck.class,
        ReferencesCheck.class,
        NotOwnerReferencesCheck.class,
        TranslationsCheck.class );

    private final static List<Class<? extends ValidationCheck>> UPDATE_CHECKS = newArrayList(
        DuplicateIdsCheck.class,
        ValidationHooksCheck.class,
        SecurityCheck.class,
        UpdateCheck.class,
        SchemaCheck.class,
        UniquenessCheck.class,
        UniqueMultiPropertiesCheck.class,
        MandatoryAttributesCheck.class,
        UniqueAttributesCheck.class,
        ReferencesCheck.class,
        NotOwnerReferencesCheck.class,
        TranslationsCheck.class );

    private final static List<Class<? extends ValidationCheck>> DELETE_CHECKS = newArrayList(
        SecurityCheck.class,
        DeletionCheck.class );

    @Bean( "validatorMap" )
    public Map<ImportStrategy, List<Class<? extends ValidationCheck>>> validatorMap()
    {
        return ImmutableMap.of(
            ImportStrategy.CREATE_AND_UPDATE, CREATE_UPDATE_CHECKS,
            CREATE, CREATE_CHECKS,
            ImportStrategy.UPDATE, UPDATE_CHECKS,
            ImportStrategy.DELETE, DELETE_CHECKS );
    }

    /*
     * TRACKER EVENT IMPORT VALIDATION
     */

    @Bean
    public Map<ImportStrategy, List<Class<? extends Checker>>> eventInsertValidatorMap()
    {
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
            FilteredDataValueCheck.class,
            DataValueAclCheck.class,
            ExpirationDaysCheck.class ) );
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Checker>>> eventUpdateValidatorMap()
    {
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
            DataValueCheck.class,
            FilteredDataValueCheck.class,
            ExpirationDaysCheck.class ) );
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Checker>>> eventDeleteValidatorMap()
    {
        return ImmutableMap.of( DELETE, newArrayList(
            org.hisp.dhis.dxf2.events.importer.delete.validation.ProgramStageInstanceAclCheck.class ) );
    }

    /*
     * TRACKER EVENT PRE/POST PROCESSING
     */

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventInsertPreProcessorMap()
    {
        return ImmutableMap.of( CREATE, newArrayList(
            ImportOptionsPreProcessor.class,
            EventStoredByPreProcessor.class,
            EventStatusPreProcessor.class,
            ProgramInstancePreProcessor.class,
            ProgramStagePreProcessor.class,
            EventGeometryPreProcessor.class,
            FilteringOutUndeclaredDataElementsProcessor.class,
            UserInfoInsertPreProcessor.class ) );
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventInsertPostProcessorMap()
    {
        return ImmutableMap.of( CREATE, newArrayList(
            ProgramNotificationPostProcessor.class,
            PublishEventPostProcessor.class,
            EventInsertAuditPostProcessor.class,
            FilteringOutUndeclaredDataElementsProcessor.class ) );
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventUpdatePreProcessorMap()
    {
        return ImmutableMap.of( UPDATE, newArrayList(
            ImportOptionsPreProcessor.class,
            EventStoredByPreProcessor.class,
            EventStatusPreProcessor.class,
            ProgramStageInstanceUpdatePreProcessor.class,
            ProgramInstanceGeometryPreProcessor.class,
            UserInfoUpdatePreProcessor.class ) );
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventUpdatePostProcessorMap()
    {
        return ImmutableMap.of( UPDATE, newArrayList(
            PublishEventPostProcessor.class,
            ProgramNotificationPostProcessor.class,
            EventUpdateAuditPostProcessor.class ) );
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventDeletePreProcessorMap()
    {
        return ImmutableMap.of( DELETE, newArrayList(
        /*
         * Intentionally left empty since we don't have pre-delete processors at
         * the moment, so at the moment this is a placeholder where to add
         * pre-delete processors when we will need it (if ever). Remove this
         * comment if you add a pre-delete processor.
         */
        ) );
    }

    @Bean
    public Map<ImportStrategy, List<Class<? extends Processor>>> eventDeletePostProcessorMap()
    {
        return ImmutableMap.of( DELETE, newArrayList(
            EventDeleteAuditPostProcessor.class ) );
    }

    @Bean
    public Map<ProgramRuleActionType, Class<? extends ProgramRuleActionValidator>> programRuleActionValidatorMap()
    {
        return new ImmutableMap.Builder<ProgramRuleActionType, Class<? extends ProgramRuleActionValidator>>()
            .put( ProgramRuleActionType.SENDMESSAGE, NotificationProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.SCHEDULEMESSAGE, NotificationProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.SHOWOPTIONGROUP, ShowHideOptionGroupProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.HIDEOPTIONGROUP, ShowHideOptionGroupProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.DISPLAYTEXT, AlwaysValidProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.DISPLAYKEYVALUEPAIR, AlwaysValidProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.ASSIGN, AssignProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.HIDEFIELD, BaseProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.CREATEEVENT, BaseProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.WARNINGONCOMPLETE, AlwaysValidProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.ERRORONCOMPLETE, AlwaysValidProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.SHOWWARNING, AlwaysValidProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.SHOWERROR, AlwaysValidProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.SETMANDATORYFIELD, BaseProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.HIDEOPTION, HideOptionProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.HIDESECTION, HideSectionProgramRuleActionValidator.class )
            .put( ProgramRuleActionType.HIDEPROGRAMSTAGE, HideProgramStageProgramRuleActionValidator.class )
            .build();
    }
}
