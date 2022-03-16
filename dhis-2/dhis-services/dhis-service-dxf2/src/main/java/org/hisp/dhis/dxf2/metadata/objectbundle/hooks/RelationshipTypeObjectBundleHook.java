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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackerdataview.TrackerDataView;
import org.springframework.stereotype.Component;

/**
 * @author Stian Sandvold
 */
@Component( "org.hisp.dhis.dxf2.metadata.objectbundle.hooks.RelationshipTypeObjectBundleHook" )
@AllArgsConstructor
public class RelationshipTypeObjectBundleHook
    extends AbstractObjectBundleHook<RelationshipType>
{
    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final DataElementService dataElementService;

    private final ProgramService programService;

    private final ProgramStageService programStageService;

    @Override
    public void validate( RelationshipType object, ObjectBundle bundle, Consumer<ErrorReport> addReports )
    {
        validateRelationshipType( object, addReports );
    }

    @Override
    public void preCreate( RelationshipType object, ObjectBundle bundle )
    {
        handleRelationshipTypeReferences( object );
    }

    @Override
    public void preUpdate( RelationshipType object, RelationshipType persistedObject, ObjectBundle bundle )
    {
        handleRelationshipTypeReferences( object );

    }

    /**
     * Handles the references for RelationshipType, persisting any objects that
     * might end up in a transient state.
     */
    private void handleRelationshipTypeReferences( RelationshipType relationshipType )
    {
        handleRelationshipConstraintReferences( relationshipType.getFromConstraint() );
        handleRelationshipConstraintReferences( relationshipType.getToConstraint() );
    }

    /**
     * Handles the references for RelationshipConstraint, persisting any object
     * that might end up in a transient state.
     */
    private void handleRelationshipConstraintReferences( RelationshipConstraint relationshipConstraint )
    {
        TrackedEntityType trackedEntityType = relationshipConstraint.getTrackedEntityType();
        Program program = relationshipConstraint.getProgram();
        ProgramStage programStage = relationshipConstraint.getProgramStage();
        TrackerDataView trackerDataView = relationshipConstraint.getTrackerDataView();

        if ( trackedEntityType != null )
        {
            trackedEntityType = trackedEntityTypeService.getTrackedEntityType( trackedEntityType.getUid() );
            relationshipConstraint.setTrackedEntityType( trackedEntityType );

            if ( trackerDataView != null && !trackerDataView.isEmpty() )
            {
                List<TrackedEntityAttribute> attributes = trackerDataView.getTrackedEntityAttributes().stream()
                    .map( a -> trackedEntityAttributeService.getTrackedEntityAttribute( a.getUid() ) )
                    .collect( Collectors.toList() );

                trackerDataView.setTrackedEntityAttributes( attributes );
                relationshipConstraint.setTrackerDataView( trackerDataView );
            }
        }

        if ( program != null )
        {
            program = programService.getProgram( program.getUid() );
            relationshipConstraint.setProgram( program );

            if ( trackerDataView != null && !trackerDataView.isEmpty() )
            {
                List<TrackedEntityAttribute> attributes = trackerDataView.getTrackedEntityAttributes().stream()
                    .map( a -> trackedEntityAttributeService.getTrackedEntityAttribute( a.getUid() ) )
                    .collect( Collectors.toList() );

                trackerDataView.setTrackedEntityAttributes( attributes );
                relationshipConstraint.setTrackerDataView( trackerDataView );
            }
        }

        if ( programStage != null )
        {
            programStage = programStageService.getProgramStage( programStage.getUid() );
            relationshipConstraint.setProgramStage( programStage );

            if ( trackerDataView != null && !trackerDataView.isEmpty() )
            {
                List<DataElement> dataElements = trackerDataView.getDataElements().stream()
                    .map( a -> dataElementService.getDataElement( a.getUid() ) ).collect( Collectors.toList() );

                trackerDataView.setDataElements( dataElements );
                relationshipConstraint.setTrackerDataView( trackerDataView );
            }
        }

        sessionFactory.getCurrentSession().save( relationshipConstraint );
    }

    /**
     * Validates the RelationshipType. A type should have constraints for both
     * left and right side.
     */
    private void validateRelationshipType( RelationshipType relationshipType,
        Consumer<ErrorReport> addReports )
    {
        if ( relationshipType.getFromConstraint() == null )
        {
            addReports.accept( new ErrorReport( RelationshipType.class, ErrorCode.E4000, "leftConstraint" ) );
        }
        else
        {
            validateRelationshipConstraint( relationshipType.getFromConstraint(), addReports );
        }

        if ( relationshipType.getToConstraint() == null )
        {
            addReports.accept( new ErrorReport( RelationshipType.class, ErrorCode.E4000, "rightConstraint" ) );
        }
        else
        {
            validateRelationshipConstraint( relationshipType.getToConstraint(), addReports );
        }
    }

    /**
     * Validates RelationshipConstraint. Each constraint requires different
     * properties set or not set depending on the RelationshipEntity set for
     * this constraint.
     */
    private void validateRelationshipConstraint( RelationshipConstraint constraint,
        Consumer<ErrorReport> addReports )
    {
        switch ( constraint.getRelationshipEntity() )
        {
        case TRACKED_ENTITY_INSTANCE:
            validateTrackedEntityInstance( constraint, addReports, constraint.getTrackerDataView() );
            break;
        case PROGRAM_INSTANCE:
            validateProgramInstance( constraint, addReports, constraint.getTrackerDataView() );
            break;
        case PROGRAM_STAGE_INSTANCE:
            validateProgramStageInstance( constraint, addReports, constraint.getTrackerDataView() );
            break;
        }
    }

    private void validateTrackedEntityInstance( RelationshipConstraint constraint, Consumer<ErrorReport> addReports,
        TrackerDataView trackerDataView )
    {
        List<String> trackerDataViewAttributes = new ArrayList<>();

        if ( trackerDataView != null )
        {
            trackerDataViewAttributes = trackerDataView.getTrackedEntityAttributes()
                .stream().filter( Objects::nonNull ).map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() );
        }

        TrackedEntityType trackedEntityType = constraint.getTrackedEntityType();

        // Should be not be null
        if ( trackedEntityType == null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4024, "trackedEntityType",
                "relationshipEntity", TRACKED_ENTITY_INSTANCE ) );
        }
        else
        {
            trackedEntityType = trackedEntityTypeService.getTrackedEntityType( trackedEntityType.getUid() );

            List<TrackedEntityAttribute> trackedEntityAttributes = Optional.ofNullable( trackedEntityType )
                .map( TrackedEntityType::getTrackedEntityAttributes ).orElse( new ArrayList<>() );

            List<String> trackedEntityTypeAttributeIds = trackedEntityAttributes.stream()
                .filter( Objects::nonNull ).map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() );

            if ( !trackerDataViewAttributes.isEmpty()
                && !trackedEntityTypeAttributeIds.containsAll( trackerDataViewAttributes ) )
            {

                List<String> teaNotPartOfTei = trackerDataViewAttributes.stream()
                    .filter( t -> !trackedEntityTypeAttributeIds.contains( t ) )
                    .collect( Collectors.toList() );

                addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4314,
                    "TrackedEntityAttributes", String.join( ",", teaNotPartOfTei ),
                    "TrackedEntityInstance" ) );
            }
        }

        // Should be null
        if ( constraint.getProgramStage() != null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4023, "programStage",
                "relationshipEntity", TRACKED_ENTITY_INSTANCE ) );
        }
    }

    private void validateProgramInstance( RelationshipConstraint constraint, Consumer<ErrorReport> addReports,
        TrackerDataView trackerDataView )
    {
        List<String> trackerDataViewAttributes = new ArrayList<>();

        if ( trackerDataView != null )
        {
            trackerDataViewAttributes = trackerDataView.getTrackedEntityAttributes()
                .stream().filter( Objects::nonNull ).map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() );
        }

        Program program = constraint.getProgram();

        // Should be null
        if ( constraint.getTrackedEntityType() != null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4023, "trackedEntityType",
                "relationshipEntity", PROGRAM_INSTANCE ) );
        }

        // Should be not be null
        if ( constraint.getProgram() == null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4024, "program",
                "relationshipEntity", PROGRAM_INSTANCE ) );
        }
        else
        {
            program = programService.getProgram( program.getUid() );

            List<TrackedEntityAttribute> trackedEntityAttributes = Optional.ofNullable( program )
                .map( Program::getTrackedEntityAttributes )
                .orElse( new ArrayList<>() );

            List<String> trackedEntityAttributeIds = trackedEntityAttributes.stream()
                .filter( Objects::nonNull ).map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() );

            if ( !trackerDataViewAttributes.isEmpty()
                && !trackedEntityAttributeIds.containsAll( trackerDataViewAttributes ) )
            {
                List<String> teaNotPartOfProgram = trackerDataViewAttributes.stream()
                    .filter( t -> !trackedEntityAttributeIds.contains( t ) )
                    .collect( Collectors.toList() );

                addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4314,
                    "TrackedEntityAttributes", String.join( ",", teaNotPartOfProgram ),
                    "Program" ) );
            }
        }

        // Should be null
        if ( constraint.getProgramStage() != null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4023, "programStage",
                "relationshipEntity", PROGRAM_INSTANCE ) );
        }
    }

    private void validateProgramStageInstance( RelationshipConstraint constraint, Consumer<ErrorReport> addReports,
        TrackerDataView trackerDataView )
    {
        List<String> trackerDataViewDataElements = new ArrayList<>();

        if ( trackerDataView != null )
        {
            trackerDataViewDataElements = trackerDataView.getDataElements()
                .stream().filter( Objects::nonNull ).map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() );
        }

        ProgramStage programStage = constraint.getProgramStage();
        Program program = constraint.getProgram();

        // Should be null
        if ( constraint.getTrackedEntityType() != null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4023, "trackedEntityType",
                "relationshipEntity", PROGRAM_STAGE_INSTANCE ) );
        }

        // Should be null
        if ( program != null && programStage != null )
        {
            addReports.accept(
                new ErrorReport( RelationshipConstraint.class, ErrorCode.E4025, "program", "programStage" ) );
        }

        // ProgramStage Should not be null
        if ( program == null && programStage == null )
        {
            addReports
                .accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4026, "program", "programStage",
                    "relationshipEntity", PROGRAM_STAGE_INSTANCE ) );
        }

        if ( program == null && programStage != null )
        {
            programStage = programStageService.getProgramStage( programStage.getUid() );

            Set<DataElement> dataElements = Optional.ofNullable( programStage ).map( ProgramStage::getDataElements )
                .orElse( new HashSet<>() );

            List<String> dataElementIds = dataElements.stream()
                .filter( Objects::nonNull ).map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() );

            if ( !trackerDataViewDataElements.isEmpty() && !dataElementIds.containsAll( trackerDataViewDataElements ) )
            {
                List<String> dataElementsNotPartOfProgramStage = trackerDataViewDataElements.stream()
                    .filter( d -> !dataElementIds.contains( d ) )
                    .collect( Collectors.toList() );

                addReports.accept(
                    new ErrorReport( RelationshipConstraint.class, ErrorCode.E4314, "DataElements",
                        String.join( ",", dataElementsNotPartOfProgramStage ), "ProgramStage" ) );
            }
        }
    }
}
