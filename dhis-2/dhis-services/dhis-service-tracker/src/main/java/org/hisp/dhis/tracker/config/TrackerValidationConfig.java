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
package org.hisp.dhis.tracker.config;

import static org.hisp.dhis.tracker.validation.validators.All.all;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.validation.Validator;
import org.hisp.dhis.tracker.validation.validators.AssignedUserValidationHook;
import org.hisp.dhis.tracker.validation.validators.EnrollmentAttributeValidationHook;
import org.hisp.dhis.tracker.validation.validators.EnrollmentDateValidationHook;
import org.hisp.dhis.tracker.validation.validators.EnrollmentGeoValidationHook;
import org.hisp.dhis.tracker.validation.validators.EnrollmentInExistingValidationHook;
import org.hisp.dhis.tracker.validation.validators.EnrollmentNoteValidationHook;
import org.hisp.dhis.tracker.validation.validators.EnrollmentRuleValidationHook;
import org.hisp.dhis.tracker.validation.validators.EventCategoryOptValidationHook;
import org.hisp.dhis.tracker.validation.validators.EventDataValuesValidationHook;
import org.hisp.dhis.tracker.validation.validators.EventDateValidationHook;
import org.hisp.dhis.tracker.validation.validators.EventGeoValidationHook;
import org.hisp.dhis.tracker.validation.validators.EventNoteValidationHook;
import org.hisp.dhis.tracker.validation.validators.EventRuleValidationHook;
import org.hisp.dhis.tracker.validation.validators.PreCheckDataRelationsValidationHook;
import org.hisp.dhis.tracker.validation.validators.PreCheckExistenceValidationHook;
import org.hisp.dhis.tracker.validation.validators.PreCheckMandatoryFieldsValidationHook;
import org.hisp.dhis.tracker.validation.validators.PreCheckMetaValidationHook;
import org.hisp.dhis.tracker.validation.validators.PreCheckSecurityOwnershipValidationHook;
import org.hisp.dhis.tracker.validation.validators.PreCheckUidValidationHook;
import org.hisp.dhis.tracker.validation.validators.PreCheckUpdatableFieldsValidationHook;
import org.hisp.dhis.tracker.validation.validators.RelationshipsValidationHook;
import org.hisp.dhis.tracker.validation.validators.RepeatedEventsValidationHook;
import org.hisp.dhis.tracker.validation.validators.TrackedEntityAttributeValidationHook;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;

/**
 * Configuration class for the tracker importer validation hook ordering. The
 * Hooks will be run in the same order they appear in this class.
 *
 * @author Luca Cambi <luca@dhis2.org>
 */
@Configuration( "trackerImportValidationConfig" )
public class TrackerValidationConfig
{
    private final Map<Class<? extends Validator>, Validator> validators;

    public TrackerValidationConfig( Collection<Validator> validators )
    {
        this.validators = byClass( validators );
    }

    private Map<Class<? extends Validator>, Validator> byClass(
        Collection<Validator> items )
    {
        return items.stream()
            .collect( Collectors.toMap(
                Validator::getClass,
                Functions.identity() ) );
    }

    @Bean
    public List<Validator> ruleEngineValidators()
    {
        return getValidatorByClass( ImmutableList.of( EnrollmentRuleValidationHook.class,
            EventRuleValidationHook.class,
            TrackedEntityAttributeValidationHook.class,
            EnrollmentAttributeValidationHook.class,
            EventDataValuesValidationHook.class ) );
    }

    @Bean
    public Validator<TrackerBundle> newValidators()
    {
        return all( TrackerBundle.class,
            new PreCheckUidValidationHook() );
    }

    @Bean
    public List<Validator> validators()
    {
        return getValidatorByClass( ImmutableList.of( PreCheckUidValidationHook.class,
            PreCheckExistenceValidationHook.class,
            PreCheckMandatoryFieldsValidationHook.class,
            PreCheckMetaValidationHook.class,
            PreCheckUpdatableFieldsValidationHook.class,
            PreCheckDataRelationsValidationHook.class,
            PreCheckSecurityOwnershipValidationHook.class,

            TrackedEntityAttributeValidationHook.class,

            EnrollmentNoteValidationHook.class,
            EnrollmentInExistingValidationHook.class,
            EnrollmentGeoValidationHook.class,
            EnrollmentDateValidationHook.class,
            EnrollmentAttributeValidationHook.class,

            EventCategoryOptValidationHook.class,
            EventDateValidationHook.class,
            EventGeoValidationHook.class,
            EventNoteValidationHook.class,
            EventDataValuesValidationHook.class,

            RelationshipsValidationHook.class,

            AssignedUserValidationHook.class,

            /*
             * NB! This hook must be run after all the Event validations,
             * because it needs to consider only all events deemed valid
             */
            RepeatedEventsValidationHook.class ) );
    }

    private List<Validator> getValidatorByClass( List<Class<? extends Validator>> validatorClasses )
    {
        return validatorClasses.stream().map( hookClass -> Optional.ofNullable( validators.get( hookClass ) )
            .orElseThrow(
                () -> new IllegalArgumentException( "Unable to find validation hook by class: " + hookClass ) ) )
            .collect( Collectors.toUnmodifiableList() );
    }

}
