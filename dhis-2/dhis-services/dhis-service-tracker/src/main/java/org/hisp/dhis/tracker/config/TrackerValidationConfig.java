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
package org.hisp.dhis.tracker.config;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.tracker.validation.hooks.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    @Bean( "ruleEngineValidationHooks" )
    public List<Class<? extends TrackerValidationHook>> getRuleEngineValidationHooks()
    {
        return ImmutableList.of(
            EnrollmentRuleValidationHook.class,
            EventRuleValidationHook.class,
            TrackedEntityAttributeValidationHook.class,
            EnrollmentAttributeValidationHook.class,
            EventDataValuesValidationHook.class );
    }

    @Bean( "validationOrder" )
    public List<Class<? extends TrackerValidationHook>> getValidationOrder()
    {
        return ImmutableList.of(
            PreCheckUidValidationHook.class,
            PreCheckExistenceValidationHook.class,
            PreCheckMandatoryFieldsValidationHook.class,
            PreCheckMetaValidationHook.class,
            PreCheckSecurityValidationHook.class,
            PreCheckDataRelationsValidationHook.class,
            PreCheckOwnershipValidationHook.class,

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

            RepeatedEventsValidationHook.class // This validation must be run
        // after
        // all the Event validations
        // because it needs to consider all and only the valid events
        );
    }

    @Bean( "validationOrderMap" )
    public Map<Class<? extends TrackerValidationHook>, Integer> getValidationOrderMap()
    {
        return IntStream
            .range( 0, getValidationOrder().size() )
            .boxed()
            .collect( toMap( getValidationOrder()::get, Function.identity() ) );
    }
}
