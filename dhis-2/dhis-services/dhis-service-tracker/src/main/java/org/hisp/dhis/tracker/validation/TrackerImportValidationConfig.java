package org.hisp.dhis.tracker.validation;

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
 *
 */

import com.google.common.collect.ImmutableList;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentAttributeValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentDateValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentGeoValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentInExistingValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentNoteValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EventCategoryOptValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EventCountValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EventDateValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EventGeoValidationHook;
import org.hisp.dhis.tracker.validation.hooks.EventNoteValidationHook;
import org.hisp.dhis.tracker.validation.hooks.PreCheckDataRelationsValidationHook;
import org.hisp.dhis.tracker.validation.hooks.PreCheckExistenceValidationHook;
import org.hisp.dhis.tracker.validation.hooks.PreCheckMetaValidationHook;
import org.hisp.dhis.tracker.validation.hooks.PreCheckOwnershipValidationHook;
import org.hisp.dhis.tracker.validation.hooks.PreCheckSecurityValidationHook;
import org.hisp.dhis.tracker.validation.hooks.PreCheckValidateAndGenerateUidHook;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityAttributeValidationHook;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

/**
 * Configuration class for the tracker importer validation hook ordering.
 * The Hooks will be run in the same order they apear in this class.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class TrackerImportValidationConfig
{
    protected final static List<Class<? extends TrackerValidationHook>> VALIDATION_ORDER = ImmutableList.of(

        PreCheckValidateAndGenerateUidHook.class,
        PreCheckExistenceValidationHook.class,
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
        EventCountValidationHook.class,
        EventDateValidationHook.class,
        EventGeoValidationHook.class,
        EventNoteValidationHook.class
    );

    protected final static Map<Class<? extends TrackerValidationHook>, Integer> VALIDATION_ORDER_MAP = IntStream
        .range( 0, VALIDATION_ORDER.size() )
        .boxed()
        .collect( toMap( VALIDATION_ORDER::get, Function.identity() ) );

    /**
     * Sort the hooks in the order they are represented in the above VALIDATION_ORDER list.
     *
     * @param hooks list to sort
     */
    public static void sortHooks( List<TrackerValidationHook> hooks )
    {
        //TODO: Make some tests to check this is correctly configured
        hooks.sort( Comparator.comparingInt( o -> VALIDATION_ORDER_MAP.get( o.getClass() ) ) );
    }
}
