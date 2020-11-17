package org.hisp.dhis.tracker.validation.hooks;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1012;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1119;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.GEOMETRY_CANT_BE_NULL;

import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;

import com.vividsolutions.jts.geom.Geometry;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luciano Fiandesio
 */
public class ValidationUtils
{
    static void validateGeometry( ValidationErrorReporter errorReporter, Geometry geometry, FeatureType featureType )
    {
        checkNotNull( geometry, GEOMETRY_CANT_BE_NULL );

        if ( featureType == null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1074 ) );
            return;
        }

        FeatureType typeFromName = FeatureType.getTypeFromName( geometry.getGeometryType() );

        if ( FeatureType.NONE == featureType || featureType != typeFromName )
        {
            errorReporter.addError( newReport( E1012 ).addArgs( featureType.name() ) );
        }
    }

    protected static List<Note> validateNotes( ValidationErrorReporter reporter, List<Note> notesToCheck )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        final List<Note> notes = new ArrayList<>();
        for ( Note note : notesToCheck )
        {
            if ( isNotEmpty( note.getValue() ) ) // Ignore notes with no text
            {
                // If a note having the same UID already exist in the db, raise error
                if ( isNotEmpty( note.getNote() ) && context.getNote( note.getNote() ).isPresent() )
                {
                    reporter.addError( newReport( E1119 ).addArgs( note.getNote() ) );
                }
                else
                {
                    notes.add( note );
                }
            }
        }
        return notes;
    }
}
