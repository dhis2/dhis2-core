package org.hisp.dhis.tracker.validation.hooks;

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
