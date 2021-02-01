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
package org.hisp.dhis.webapi.controller.tracker;

import static org.hisp.dhis.tracker.AtomicMode.ALL;
import static org.hisp.dhis.tracker.FlushMode.AUTO;
import static org.hisp.dhis.tracker.TrackerIdScheme.UID;
import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.ValidationMode.FULL;
import static org.hisp.dhis.tracker.bundle.TrackerBundleMode.COMMIT;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.ATOMIC_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.DATA_ELEMENT_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.FLUSH_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.IMPORT_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.IMPORT_STRATEGY_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.ORG_UNIT_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.PROGRAM_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.PROGRAM_STAGE_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.SKIP_RULE_ENGINE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.TrackerImportParamsBuilder.TrackerImportParamKey.VALIDATION_MODE_KEY;

import java.util.List;
import java.util.Map;

import lombok.Getter;

import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;

import com.google.common.base.Enums;

/**
 * @author Luciano Fiandesio
 */
public class TrackerImportParamsBuilder
{

    public static TrackerImportParams build( Map<String, List<String>> parameters )
    {
        return builder( parameters )
            .build();
    }

    public static TrackerImportParams.TrackerImportParamsBuilder builder( Map<String, List<String>> parameters )
    {
        return TrackerImportParams.builder()
            .validationMode( getEnumWithDefault( ValidationMode.class, parameters, VALIDATION_MODE_KEY, FULL ) )
            .importMode( getEnumWithDefault( TrackerBundleMode.class, parameters, IMPORT_MODE_KEY, COMMIT ) )
            .identifiers( getTrackerIdentifiers( parameters ) )
            .importStrategy(
                getEnumWithDefault( TrackerImportStrategy.class, parameters, IMPORT_STRATEGY_KEY, CREATE_AND_UPDATE ) )
            .atomicMode( getEnumWithDefault( AtomicMode.class, parameters, ATOMIC_MODE_KEY, ALL ) )
            .flushMode( getEnumWithDefault( FlushMode.class, parameters, FLUSH_MODE_KEY, AUTO ) )
            .skipRuleEngine( getBooleanValueOrDefault( parameters, SKIP_RULE_ENGINE_KEY ) );
    }

    private static <T extends Enum<T>> T getEnumWithDefault( Class<T> enumKlass, Map<String, List<String>> parameters,
        TrackerImportParamKey trackerImportParamKey, T defaultValue )
    {
        if ( parameters == null || parameters.get( trackerImportParamKey.getKey() ) == null
            || parameters.get( trackerImportParamKey.getKey() ).isEmpty() )
        {
            return defaultValue;
        }

        if ( TrackerIdScheme.class.equals( enumKlass )
            && IdScheme.isAttribute( parameters.get( trackerImportParamKey.getKey() ).get( 0 ) ) )
        {
            return Enums.getIfPresent( enumKlass, "ATTRIBUTE" ).orNull();
        }

        String value = String.valueOf( parameters.get( trackerImportParamKey.getKey() ).get( 0 ) );

        return Enums.getIfPresent( enumKlass, value ).or( defaultValue );
    }

    private static TrackerIdentifierParams getTrackerIdentifiers( Map<String, List<String>> parameters )
    {
        TrackerIdScheme idScheme = getEnumWithDefault( TrackerIdScheme.class, parameters, ID_SCHEME_KEY, UID );

        return TrackerIdentifierParams.builder()
            .idScheme( bySchemeAndKey( parameters, ID_SCHEME_KEY, idScheme ) )
            .orgUnitIdScheme( bySchemeAndKey( parameters, ORG_UNIT_ID_SCHEME_KEY, idScheme ) )
            .programIdScheme( bySchemeAndKey( parameters, PROGRAM_ID_SCHEME_KEY, idScheme ) )
            .programStageIdScheme( bySchemeAndKey( parameters, PROGRAM_STAGE_ID_SCHEME_KEY, idScheme ) )
            .dataElementIdScheme( bySchemeAndKey( parameters, DATA_ELEMENT_ID_SCHEME_KEY, idScheme ) )
            .build();
    }

    private static Boolean getBooleanValueOrDefault( Map<String, List<String>> parameters,
        TrackerImportParamKey trackerImportParamKey )
    {
        if ( parameters == null || parameters.get( trackerImportParamKey.getKey() ) == null
            || parameters.get( trackerImportParamKey.getKey() ).isEmpty() )
        {
            return false;
        }

        return BooleanUtils.toBooleanObject( parameters.get( trackerImportParamKey.getKey() ).get( 0 ) );
    }

    private static String getAttributeUidOrNull( Map<String, List<String>> parameters,
        TrackerImportParamKey trackerImportParamKey )
    {
        if ( parameters == null || parameters.get( trackerImportParamKey.getKey() ) == null
            || parameters.get( trackerImportParamKey.getKey() ).isEmpty() )
        {
            return null;
        }

        if ( IdScheme.isAttribute( parameters.get( trackerImportParamKey.getKey() ).get( 0 ) ) )
        {
            String uid = "";

            // Get second half of string, separated by ':'
            String[] splitParam = parameters.get( trackerImportParamKey.getKey() ).get( 0 ).split( ":" );

            if ( splitParam.length > 1 )
            {
                uid = splitParam[1];
            }

            if ( CodeGenerator.isValidUid( uid ) )
            {
                return uid;
            }
        }

        return null;
    }

    private static TrackerIdentifier bySchemeAndKey( Map<String, List<String>> parameters,
        TrackerImportParamKey trackerImportParameterKey,
        TrackerIdScheme defaultIdScheme )
    {

        TrackerIdScheme trackerIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters,
            trackerImportParameterKey, defaultIdScheme );

        return TrackerIdentifier.of( trackerIdScheme, getAttributeUidOrNull( parameters, trackerImportParameterKey ) );
    }

    enum TrackerImportParamKey
    {
        VALIDATION_MODE_KEY( "validationMode" ),
        IMPORT_MODE_KEY( "importMode" ),
        IMPORT_STRATEGY_KEY( "importStrategy" ),
        ATOMIC_MODE_KEY( "atomicMode" ),
        FLUSH_MODE_KEY( "flushMode" ),
        SKIP_RULE_ENGINE_KEY( "skipRuleEngine" ),
        ID_SCHEME_KEY( "idScheme" ),
        ORG_UNIT_ID_SCHEME_KEY( "orgUnitIdScheme" ),
        PROGRAM_ID_SCHEME_KEY( "programIdScheme" ),
        PROGRAM_STAGE_ID_SCHEME_KEY( "programStageIdScheme" ),
        DATA_ELEMENT_ID_SCHEME_KEY( "dataElementIdScheme" );

        @Getter
        private final String key;

        TrackerImportParamKey( String key )
        {
            this.key = key;
        }
    }
}
