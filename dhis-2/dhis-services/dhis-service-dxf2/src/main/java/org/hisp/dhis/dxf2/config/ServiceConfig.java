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
import static org.hisp.dhis.dxf2.events.importer.EventProcessorPhase.DELETE_POST;
import static org.hisp.dhis.dxf2.events.importer.EventProcessorPhase.DELETE_PRE;
import static org.hisp.dhis.dxf2.events.importer.EventProcessorPhase.INSERT_POST;
import static org.hisp.dhis.dxf2.events.importer.EventProcessorPhase.INSERT_PRE;
import static org.hisp.dhis.dxf2.events.importer.EventProcessorPhase.UPDATE_POST;
import static org.hisp.dhis.dxf2.events.importer.EventProcessorPhase.UPDATE_PRE;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.importexport.ImportStrategy.DELETE;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.EventProcessorExecutor;
import org.hisp.dhis.dxf2.events.importer.EventProcessorPhase;
import org.hisp.dhis.dxf2.events.importer.ImportStrategyUtils;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.delete.postprocess.EventDeleteAuditPostProcessor;
import org.hisp.dhis.dxf2.events.importer.delete.validation.DeleteProgramStageInstanceAclCheck;
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
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.EventStoredByPreProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.FilteringOutUndeclaredDataElementsProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.ImportOptionsPreProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.preprocess.SharedEventStatusPreProcessor;
import org.hisp.dhis.dxf2.events.importer.shared.validation.AttributeOptionComboCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.AttributeOptionComboDateCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.DataValueCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.EventBaseCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.EventGeometryCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.FilteredDataValueCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.ProgramInstanceCheck;
import org.hisp.dhis.dxf2.events.importer.shared.validation.SharedProgramCheck;
import org.hisp.dhis.dxf2.events.importer.update.postprocess.EventUpdateAuditPostProcessor;
import org.hisp.dhis.dxf2.events.importer.update.postprocess.PublishEventPostProcessor;
import org.hisp.dhis.dxf2.events.importer.update.preprocess.ProgramInstanceGeometryPreProcessor;
import org.hisp.dhis.dxf2.events.importer.update.preprocess.ProgramStageInstanceUpdatePreProcessor;
import org.hisp.dhis.dxf2.events.importer.update.preprocess.UserInfoUpdatePreProcessor;
import org.hisp.dhis.dxf2.events.importer.update.validation.EventSimpleCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ExpirationDaysCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ProgramStageInstanceAuthCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.ProgramStageInstanceBasicCheck;
import org.hisp.dhis.dxf2.events.importer.update.validation.UpdateProgramStageInstanceAclCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.CreationCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DeletionCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DuplicateIdsCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.MandatoryAttributesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.NotOwnerReferencesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ReferencesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.SchemaCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.SecurityCheck;
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

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
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

    private final Map<Class<? extends Checker>, Checker> checkersByClass;

    private final Map<Class<? extends ValidationCheck>, ValidationCheck> validationCheckByClass;

    private final Map<Class<? extends Processor>, Processor> processorsByClass;

    private final Map<Class<? extends ProgramRuleActionValidator>, ProgramRuleActionValidator> programRuleActionValidatorsByClass;

    private final Map<EventProcessorPhase, List<Processor>> processorsByPhase;

    public ServiceConfig( Collection<Checker> checkers, Collection<ValidationCheck> validationChecks,
        Collection<Processor> processors, Collection<ProgramRuleActionValidator> programRuleActionValidators )
    {
        checkersByClass = byClass( checkers );
        validationCheckByClass = byClass( validationChecks );
        processorsByClass = byClass( processors );
        programRuleActionValidatorsByClass = byClass( programRuleActionValidators );
        processorsByPhase = getProcessorsByPhase();
    }

    @SuppressWarnings( "unchecked" )
    private <T> Map<Class<? extends T>, T> byClass( Collection<T> items )
    {
        return items.stream()
            .collect( Collectors.toMap(
                e -> (Class<? extends T>) e.getClass(),
                Functions.identity() ) );
    }

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

    @Bean
    public Map<ImportStrategy, List<ValidationCheck>> validatorsByImportStrategy()
    {
        return ImmutableMap.of(
            CREATE_AND_UPDATE, newArrayList(
                getValidationCheckByClass( DuplicateIdsCheck.class ),
                getValidationCheckByClass( ValidationHooksCheck.class ),
                getValidationCheckByClass( SecurityCheck.class ),
                getValidationCheckByClass( SchemaCheck.class ),
                getValidationCheckByClass( UniquenessCheck.class ),
                getValidationCheckByClass( UniqueMultiPropertiesCheck.class ),
                getValidationCheckByClass( MandatoryAttributesCheck.class ),
                getValidationCheckByClass( UniqueAttributesCheck.class ),
                getValidationCheckByClass( ReferencesCheck.class ),
                getValidationCheckByClass( NotOwnerReferencesCheck.class ) ),
            CREATE, newArrayList(
                getValidationCheckByClass( DuplicateIdsCheck.class ),
                getValidationCheckByClass( ValidationHooksCheck.class ),
                getValidationCheckByClass( SecurityCheck.class ),
                getValidationCheckByClass( CreationCheck.class ),
                getValidationCheckByClass( SchemaCheck.class ),
                getValidationCheckByClass( UniquenessCheck.class ),
                getValidationCheckByClass( UniqueMultiPropertiesCheck.class ),
                getValidationCheckByClass( MandatoryAttributesCheck.class ),
                getValidationCheckByClass( UniqueAttributesCheck.class ),
                getValidationCheckByClass( ReferencesCheck.class ),
                getValidationCheckByClass( NotOwnerReferencesCheck.class ) ),
            UPDATE, newArrayList(
                getValidationCheckByClass( DuplicateIdsCheck.class ),
                getValidationCheckByClass( ValidationHooksCheck.class ),
                getValidationCheckByClass( SecurityCheck.class ),
                getValidationCheckByClass( UpdateCheck.class ),
                getValidationCheckByClass( SchemaCheck.class ),
                getValidationCheckByClass( UniquenessCheck.class ),
                getValidationCheckByClass( UniqueMultiPropertiesCheck.class ),
                getValidationCheckByClass( MandatoryAttributesCheck.class ),
                getValidationCheckByClass( UniqueAttributesCheck.class ),
                getValidationCheckByClass( ReferencesCheck.class ),
                getValidationCheckByClass( NotOwnerReferencesCheck.class ) ),
            DELETE, newArrayList(
                getValidationCheckByClass( SecurityCheck.class ),
                getValidationCheckByClass( DeletionCheck.class ) ) );
    }

    /*
     * TRACKER EVENT IMPORT VALIDATION
     */

    @Bean
    public List<Checker> checkersRunOnInsert()
    {
        return ImmutableList.of(
            getCheckerByClass( EventDateCheck.class ),
            getCheckerByClass( OrgUnitCheck.class ),
            getCheckerByClass( SharedProgramCheck.class ),
            getCheckerByClass( ProgramStageCheck.class ),
            getCheckerByClass( TrackedEntityInstanceCheck.class ),
            getCheckerByClass( ProgramInstanceCheck.class ),
            getCheckerByClass( ProgramInstanceRepeatableStageCheck.class ),
            getCheckerByClass( ProgramOrgUnitCheck.class ),
            getCheckerByClass( EventGeometryCheck.class ),
            getCheckerByClass( EventCreationAclCheck.class ),
            getCheckerByClass( EventBaseCheck.class ),
            getCheckerByClass( AttributeOptionComboCheck.class ),
            getCheckerByClass( AttributeOptionComboDateCheck.class ),
            getCheckerByClass( AttributeOptionComboAclCheck.class ),
            getCheckerByClass( DataValueCheck.class ),
            getCheckerByClass( FilteredDataValueCheck.class ),
            getCheckerByClass( DataValueAclCheck.class ),
            getCheckerByClass( ExpirationDaysCheck.class ) );
    }

    @Bean
    public List<Checker> checkersRunOnUpdate()
    {
        return ImmutableList.of(
            getCheckerByClass( EventSimpleCheck.class ),
            getCheckerByClass( EventBaseCheck.class ),
            getCheckerByClass( ProgramStageInstanceBasicCheck.class ),
            getCheckerByClass( UpdateProgramStageInstanceAclCheck.class ),
            getCheckerByClass( SharedProgramCheck.class ),
            getCheckerByClass( ProgramInstanceCheck.class ),
            getCheckerByClass( ProgramStageInstanceAuthCheck.class ),
            getCheckerByClass( AttributeOptionComboCheck.class ),
            getCheckerByClass( AttributeOptionComboDateCheck.class ),
            getCheckerByClass( EventGeometryCheck.class ),
            getCheckerByClass( DataValueCheck.class ),
            getCheckerByClass( FilteredDataValueCheck.class ),
            getCheckerByClass( ExpirationDaysCheck.class ) );
    }

    @Bean
    public List<Checker> checkersRunOnDelete()
    {
        return ImmutableList.of(
            getCheckerByClass(
                DeleteProgramStageInstanceAclCheck.class ) );
    }

    private Checker getCheckerByClass( Class<? extends Checker> checkerClass )
    {
        return getByClass( checkersByClass, checkerClass );
    }

    private ValidationCheck getValidationCheckByClass( Class<? extends ValidationCheck> validationCheckClass )
    {
        return getByClass( validationCheckByClass, validationCheckClass );
    }

    private Processor getProcessorByClass( Class<? extends Processor> processorClass )
    {
        return getByClass( processorsByClass, processorClass );
    }

    private ProgramRuleActionValidator getProgramRuleActionValidatorByClass(
        Class<? extends ProgramRuleActionValidator> programRuleActionValidatorClass )
    {
        return getByClass( programRuleActionValidatorsByClass, programRuleActionValidatorClass );
    }

    private <T> T getByClass( Map<Class<? extends T>, ? extends T> tByClass, Class<? extends T> clazz )
    {
        return Optional.ofNullable( tByClass.get( clazz ) )
            .orElseThrow( () -> new IllegalArgumentException( "Unable to find validator by class: " + clazz ) );
    }

    /*
     * TRACKER EVENT PRE/POST PROCESSING
     */

    @Bean
    Map<EventProcessorPhase, EventProcessorExecutor> executorsByPhase()
    {
        return ImmutableMap.<EventProcessorPhase, Predicate<ImportStrategy>> builder()
            .put( INSERT_PRE, ImportStrategyUtils::isInsert )
            .put( INSERT_POST, ImportStrategyUtils::isInsert )
            .put( UPDATE_PRE, ImportStrategyUtils::isUpdate )
            .put( UPDATE_POST, ImportStrategyUtils::isUpdate )
            .put( DELETE_PRE, ImportStrategyUtils::isDelete )
            .put( DELETE_POST, ImportStrategyUtils::isDelete )
            .build().entrySet().stream()
            .map( entry -> Pair.of( entry.getKey(),
                new EventProcessorExecutor( processorsByPhase.get( entry.getKey() ),
                    entry.getValue() ) ) )
            .collect( Collectors.toMap(
                Pair::getKey,
                Pair::getValue ) );
    }

    private Map<EventProcessorPhase, List<Processor>> getProcessorsByPhase()
    {
        return ImmutableMap.<EventProcessorPhase, List<Processor>> builder()
            .put( INSERT_PRE, newArrayList(
                getProcessorByClass( ImportOptionsPreProcessor.class ),
                getProcessorByClass( EventStoredByPreProcessor.class ),
                getProcessorByClass( SharedEventStatusPreProcessor.class ),
                getProcessorByClass( ProgramInstancePreProcessor.class ),
                getProcessorByClass( ProgramStagePreProcessor.class ),
                getProcessorByClass( EventGeometryPreProcessor.class ),
                getProcessorByClass( FilteringOutUndeclaredDataElementsProcessor.class ),
                getProcessorByClass( UserInfoInsertPreProcessor.class ) ) )
            .put( INSERT_POST, newArrayList(
                getProcessorByClass( ProgramNotificationPostProcessor.class ),
                getProcessorByClass( PublishEventPostProcessor.class ),
                getProcessorByClass( EventInsertAuditPostProcessor.class ),
                getProcessorByClass( FilteringOutUndeclaredDataElementsProcessor.class ) ) )
            .put( UPDATE_PRE, newArrayList(
                getProcessorByClass( ImportOptionsPreProcessor.class ),
                getProcessorByClass( EventStoredByPreProcessor.class ),
                getProcessorByClass( SharedEventStatusPreProcessor.class ),
                getProcessorByClass( ProgramStageInstanceUpdatePreProcessor.class ),
                getProcessorByClass( ProgramInstanceGeometryPreProcessor.class ),
                getProcessorByClass( UserInfoUpdatePreProcessor.class ) ) )
            .put( UPDATE_POST, newArrayList(
                getProcessorByClass( PublishEventPostProcessor.class ),
                getProcessorByClass( ProgramNotificationPostProcessor.class ),
                getProcessorByClass( EventUpdateAuditPostProcessor.class ) ) )
            .put( DELETE_PRE, Collections.emptyList() )
            .put( DELETE_POST, newArrayList(
                getProcessorByClass( EventDeleteAuditPostProcessor.class ) ) )
            .build();
    }

    @Bean
    public Map<ProgramRuleActionType, ProgramRuleActionValidator> programRuleActionValidatorMap()
    {
        return ImmutableMap.<ProgramRuleActionType, ProgramRuleActionValidator> builder()
            .put( ProgramRuleActionType.SENDMESSAGE,
                getProgramRuleActionValidatorByClass( NotificationProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.SCHEDULEMESSAGE,
                getProgramRuleActionValidatorByClass( NotificationProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.SHOWOPTIONGROUP,
                getProgramRuleActionValidatorByClass( ShowHideOptionGroupProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.HIDEOPTIONGROUP,
                getProgramRuleActionValidatorByClass( ShowHideOptionGroupProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.DISPLAYTEXT,
                getProgramRuleActionValidatorByClass( AlwaysValidProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.DISPLAYKEYVALUEPAIR,
                getProgramRuleActionValidatorByClass( AlwaysValidProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.ASSIGN,
                getProgramRuleActionValidatorByClass( BaseProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.HIDEFIELD,
                getProgramRuleActionValidatorByClass( BaseProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.CREATEEVENT,
                getProgramRuleActionValidatorByClass( BaseProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.WARNINGONCOMPLETE,
                getProgramRuleActionValidatorByClass( BaseProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.ERRORONCOMPLETE,
                getProgramRuleActionValidatorByClass( BaseProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.SHOWWARNING,
                getProgramRuleActionValidatorByClass( AlwaysValidProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.SHOWERROR,
                getProgramRuleActionValidatorByClass( AlwaysValidProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.SETMANDATORYFIELD,
                getProgramRuleActionValidatorByClass( BaseProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.HIDEOPTION,
                getProgramRuleActionValidatorByClass( HideOptionProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.HIDESECTION,
                getProgramRuleActionValidatorByClass( HideSectionProgramRuleActionValidator.class ) )
            .put( ProgramRuleActionType.HIDEPROGRAMSTAGE,
                getProgramRuleActionValidatorByClass( HideProgramStageProgramRuleActionValidator.class ) )
            .build();
    }
}
