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
package org.hisp.dhis.tracker.validation.hooks;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.tracker.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.programrule.IssueType.WARNING;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.GEOMETRY_CANT_BE_NULL;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.locationtech.jts.geom.Geometry;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class ValidationUtils
{
    static void validateGeometry( ValidationErrorReporter reporter, TrackerDto dto, Geometry geometry,
        FeatureType featureType )
    {
        checkNotNull( geometry, GEOMETRY_CANT_BE_NULL );

        if ( featureType == null )
        {
            reporter.addError( dto, TrackerErrorCode.E1074 );
            return;
        }

        FeatureType typeFromName = FeatureType.getTypeFromName( geometry.getGeometryType() );

        if ( FeatureType.NONE == featureType || featureType != typeFromName )
        {
            reporter.addError( dto, TrackerErrorCode.E1012, featureType.name() );
        }
    }

    protected static List<Note> validateNotes( ValidationErrorReporter reporter, TrackerDto dto,
        List<Note> notesToCheck )
    {
        TrackerPreheat preheat = reporter.getBundle().getPreheat();

        final List<Note> notes = new ArrayList<>();
        for ( Note note : notesToCheck )
        {
            if ( isNotEmpty( note.getValue() ) ) // Ignore notes with no text
            {
                // If a note having the same UID already exist in the db, raise
                // warning, ignore the note and continue
                if ( isNotEmpty( note.getNote() ) && preheat.getNote( note.getNote() ).isPresent() )
                {
                    reporter.addWarning( dto, TrackerErrorCode.E1119, note.getNote() );
                }
                else
                {
                    notes.add( note );
                }
            }
        }
        return notes;
    }

    public static List<MetadataIdentifier> validateMandatoryDataValue( ProgramStage programStage,
        Event event, List<MetadataIdentifier> mandatoryDataElements )
    {
        List<MetadataIdentifier> notPresentMandatoryDataElements = Lists.newArrayList();

        if ( !needsToValidateDataValues( event, programStage ) )
        {
            return notPresentMandatoryDataElements;
        }

        Set<MetadataIdentifier> eventDataElements = event.getDataValues().stream()
            .map( DataValue::getDataElement )
            .collect( Collectors.toSet() );

        for ( MetadataIdentifier mandatoryDataElement : mandatoryDataElements )
        {
            if ( !eventDataElements.contains( mandatoryDataElement ) )
            {
                notPresentMandatoryDataElements.add( mandatoryDataElement );
            }
        }

        return notPresentMandatoryDataElements;
    }

    public static boolean needsToValidateDataValues( Event event, ProgramStage programStage )
    {
        if ( event.getStatus().equals( EventStatus.SCHEDULE ) || event.getStatus().equals( EventStatus.SKIPPED ) )
        {
            return false;
        }
        else if ( programStage.getValidationStrategy().equals( ValidationStrategy.ON_COMPLETE )
            && event.getStatus().equals( EventStatus.COMPLETED ) )
        {
            return true;
        }
        else
        {
            return !programStage.getValidationStrategy().equals( ValidationStrategy.ON_COMPLETE );
        }
    }

    public static void addIssuesToReporter( ValidationErrorReporter reporter, TrackerDto dto,
        List<ProgramRuleIssue> programRuleIssues )
    {
        programRuleIssues
            .stream()
            .filter( issue -> issue.getIssueType().equals( ERROR ) )
            .forEach( issue -> {
                List<String> args = Lists.newArrayList( issue.getRuleUid() );
                args.addAll( issue.getArgs() );
                reporter.addError( dto, issue.getIssueCode(), args.toArray() );
            } );

        programRuleIssues
            .stream()
            .filter( issue -> issue.getIssueType().equals( WARNING ) )
            .forEach(
                issue -> {
                    List<String> args = Lists.newArrayList( issue.getRuleUid() );
                    args.addAll( issue.getArgs() );
                    reporter.addWarning( dto, issue.getIssueCode(), args.toArray() );
                } );
    }

    public static boolean trackedEntityInstanceExist( TrackerBundle bundle, String teiUid )
    {
        return bundle.getTrackedEntityInstance( teiUid ) != null
            || bundle.getPreheat().getReference( teiUid ).isPresent();
    }

    public static boolean enrollmentExist( TrackerBundle bundle, String enrollmentUid )
    {
        return bundle.getProgramInstance( enrollmentUid ) != null
            || bundle.getPreheat().getReference( enrollmentUid ).isPresent();
    }

    public static boolean eventExist( TrackerBundle bundle, String eventUid )
    {
        return bundle.getProgramStageInstance( eventUid ) != null
            || bundle.getPreheat().getReference( eventUid ).isPresent();
    }
}
