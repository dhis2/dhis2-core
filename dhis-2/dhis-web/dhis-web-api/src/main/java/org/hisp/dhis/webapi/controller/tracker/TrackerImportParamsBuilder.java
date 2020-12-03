package org.hisp.dhis.webapi.controller.tracker;

import java.util.List;
import java.util.Map;

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
    public static String VALIDATION_MODE_KEY = "validationMode";

    public static String IMPORT_MODE_KEY = "importMode";

    public static String IMPORT_STRATEGY_KEY = "importStrategy";

    public static String ATOMIC_MODE_KEY = "atomicMode";

    public static String FLUSH_MODE_KEY = "flushMode";

    public static TrackerImportParams build( Map<String, List<String>> parameters )
    {
        TrackerImportParams params = new TrackerImportParams();

        params.setValidationMode( getEnumWithDefault( ValidationMode.class, parameters, VALIDATION_MODE_KEY,
            ValidationMode.FULL ) );
        params.setImportMode(
            getEnumWithDefault( TrackerBundleMode.class, parameters, IMPORT_MODE_KEY, TrackerBundleMode.COMMIT ) );
        params.setIdentifiers( getTrackerIdentifiers( parameters ) );
        params.setImportStrategy( getEnumWithDefault( TrackerImportStrategy.class, parameters, IMPORT_STRATEGY_KEY,
            TrackerImportStrategy.CREATE_AND_UPDATE ) );
        params.setAtomicMode( getEnumWithDefault( AtomicMode.class, parameters, ATOMIC_MODE_KEY, AtomicMode.ALL ) );
        params.setFlushMode( getEnumWithDefault( FlushMode.class, parameters, FLUSH_MODE_KEY, FlushMode.AUTO ) );

        return params;

    }

    private static <T extends Enum<T>> T getEnumWithDefault( Class<T> enumKlass, Map<String, List<String>> parameters,
        String key, T defaultValue )
    {
        if ( parameters == null || parameters.get( key ) == null || parameters.get( key ).isEmpty() )
        {
            return defaultValue;
        }

        if ( TrackerIdScheme.class.equals( enumKlass ) && IdScheme.isAttribute( parameters.get( key ).get( 0 ) ) )
        {
            return Enums.getIfPresent( enumKlass, "ATTRIBUTE" ).orNull();
        }

        String value = String.valueOf( parameters.get( key ).get( 0 ) );

        return Enums.getIfPresent( enumKlass, value ).or( defaultValue );
    }

    private static TrackerIdentifierParams getTrackerIdentifiers( Map<String, List<String>> parameters )
    {
        TrackerIdScheme idScheme = getEnumWithDefault( TrackerIdScheme.class, parameters, "idScheme",
            TrackerIdScheme.UID );
        TrackerIdScheme orgUnitIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters, "orgUnitIdScheme",
            idScheme );
        TrackerIdScheme programIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters, "programIdScheme",
            idScheme );
        TrackerIdScheme programStageIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters,
            "programStageIdScheme", idScheme );
        TrackerIdScheme dataElementIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters,
            "dataElementIdScheme", idScheme );

        return TrackerIdentifierParams.builder()
            .idScheme( TrackerIdentifier.builder().idScheme( idScheme )
                .value( getAttributeUidOrNull( parameters, "idScheme" ) ).build() )
            .orgUnitIdScheme( TrackerIdentifier.builder().idScheme( orgUnitIdScheme )
                .value( getAttributeUidOrNull( parameters, "orgUnitIdScheme" ) ).build() )
            .programIdScheme( TrackerIdentifier.builder().idScheme( programIdScheme )
                .value( getAttributeUidOrNull( parameters, "programIdScheme" ) ).build() )
            .programStageIdScheme( TrackerIdentifier.builder().idScheme( programStageIdScheme )
                .value( getAttributeUidOrNull( parameters, "programStageIdScheme" ) ).build() )
            .dataElementIdScheme( TrackerIdentifier.builder().idScheme( dataElementIdScheme )
                .value( getAttributeUidOrNull( parameters, "dataElementIdScheme" ) ).build() )
            .build();
    }

    private static String getAttributeUidOrNull( Map<String, List<String>> parameters, String key )
    {
        if ( parameters == null || parameters.get( key ) == null || parameters.get( key ).isEmpty() )
        {
            return null;
        }

        if ( IdScheme.isAttribute( parameters.get( key ).get( 0 ) ) )
        {
            String uid = "";

            // Get second half of string, separated by ':'
            String[] splitParam = parameters.get( key ).get( 0 ).split( ":" );

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
}
