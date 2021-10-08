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
package org.hisp.dhis.tracker.preheat.supplier;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.UniqueAttributeValue;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Component
public class UniqueAttributesSupplier extends AbstractPreheatSupplier
{
    @NonNull
    private final TrackedEntityAttributeService trackedEntityAttributeService;

    @NonNull
    private final TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Override
    public void preheatAdd( TrackerImportParams params, TrackerPreheat preheat )
    {
        preheat.setUniqueAttributeValues( calculateUniqueAttributeValues( params, preheat ) );
    }

    private List<UniqueAttributeValue> calculateUniqueAttributeValues(
        TrackerImportParams params, TrackerPreheat preheat )
    {
        List<TrackedEntityAttribute> uniqueTrackedEntityAttributes = trackedEntityAttributeService
            .getAllUniqueTrackedEntityAttributes();

        Map<TrackedEntity, List<Attribute>> allAttributes = getAllAttributesByTrackedEntity( params, preheat );

        List<UniqueAttributeValue> uniqueAttributeValuesFromPayload = getNonUniqueValuesInPayload( allAttributes,
            uniqueTrackedEntityAttributes );

        List<Attribute> allUniqueAttributes = allAttributes
            .values()
            .stream()
            .flatMap( Collection::stream )
            .filter( tea -> uniqueTrackedEntityAttributes.stream()
                .anyMatch( uniqueAttr -> uniqueAttr.getUid().equals( tea.getAttribute() ) ) )
            .distinct()
            .collect( Collectors.toList() );

        Map<TrackedEntityAttribute, List<String>> uniqueAttributes = allUniqueAttributes
            .stream()
            .collect( Collectors.toMap( a -> extractAttribute( a.getAttribute(), uniqueTrackedEntityAttributes ),
                a -> extractValues( allUniqueAttributes, a.getAttribute() ) ) );

        // Merge unique attributes from DB and from Payload
        return Stream.concat( uniqueAttributeValuesFromPayload.stream(),
            trackedEntityAttributeValueService.getUniqueAttributeByValues( uniqueAttributes )
                .stream()
                .map( av -> new UniqueAttributeValue( av.getEntityInstance().getUid(), av.getAttribute().getUid(),
                    av.getValue(), av.getEntityInstance().getOrganisationUnit().getUid() ) ) )
            .collect( Collectors.toList() );
    }

    private List<UniqueAttributeValue> getNonUniqueValuesInPayload( Map<TrackedEntity, List<Attribute>> allAttributes,
        List<TrackedEntityAttribute> uniqueTrackedEntityAttributes )
    {

        // TEIs grouped by Attribute.
        // Two attributes with the same value and uid are considered equals
        TreeMap<Attribute, List<TrackedEntity>> teiByAttributeValue = new TreeMap<>( ( o1, o2 ) -> {
            if ( Objects.equals( o1.getValue(), o2.getValue() )
                && Objects.equals( o1.getAttribute(), o2.getAttribute() ) )
            {
                return 0;
            }
            return 1;
        } );

        for ( Map.Entry<TrackedEntity, List<Attribute>> entry : allAttributes.entrySet() )
        {
            for ( Attribute attribute : entry.getValue() )
            {
                if ( !teiByAttributeValue.containsKey( attribute ) )
                {
                    teiByAttributeValue.put( attribute, Lists.newArrayList() );
                }
                teiByAttributeValue.get( attribute ).add( entry.getKey() );
            }
        }

        // In the list of unique attribute values there are all the attributes
        // with the same value that appear in 2 or more TEIs
        return teiByAttributeValue
            .entrySet()
            .stream()
            .filter( e -> e.getValue().size() > 1 )
            .flatMap( av -> buildUniqueAttributeValues( av.getKey(), av.getValue() ).stream() )
            .filter( tea -> uniqueTrackedEntityAttributes.stream()
                .anyMatch( uniqueAttr -> Objects.equals( uniqueAttr.getUid(), tea.getAttributeUid() ) ) )
            .collect( Collectors.toList() );
    }

    private Collection<UniqueAttributeValue> buildUniqueAttributeValues( Attribute attribute,
        List<TrackedEntity> value )
    {
        return value.stream()
            .map( tei -> new UniqueAttributeValue( tei.getUid(), attribute.getAttribute(), attribute.getValue(),
                tei.getOrgUnit() ) )
            .collect( Collectors.toList() );

    }

    private Map<TrackedEntity, List<Attribute>> getAllAttributesByTrackedEntity( TrackerImportParams params,
        TrackerPreheat preheat )
    {
        Map<TrackedEntity, List<Attribute>> teiAttributes = params.getTrackedEntities()
            .stream()
            .collect( Collectors.toMap( Function.identity(), TrackedEntity::getAttributes ) );
        Map<TrackedEntity, List<Attribute>> enrollmentAttributes = params.getEnrollments()
            .stream()
            .collect( Collectors.groupingBy( Enrollment::getTrackedEntity ) )
            .entrySet()
            .stream()
            .map( e -> new AbstractMap.SimpleEntry<>( getEntityForEnrollment( params, preheat, e.getKey() ),
                e.getValue().stream().flatMap( en -> en.getAttributes().stream() ).collect( Collectors.toList() ) ) )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );

        return mergeAttributes( teiAttributes, enrollmentAttributes );

    }

    private Map<TrackedEntity, List<Attribute>> mergeAttributes( Map<TrackedEntity, List<Attribute>> teiAttributes,
        Map<TrackedEntity, List<Attribute>> enrollmentAttributes )
    {
        return Stream.concat( teiAttributes.entrySet().stream(),
            enrollmentAttributes.entrySet().stream() )
            .collect( Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                ( v1, v2 ) -> {
                    List<Attribute> attributes = Lists.newArrayList( v1 );
                    attributes.addAll( v2 );
                    return attributes;
                } ) );
    }

    private TrackedEntity getEntityForEnrollment( TrackerImportParams params, TrackerPreheat preheat, String teiUid )
    {
        TrackedEntityInstance trackedEntity = preheat.getTrackedEntity( TrackerIdScheme.UID, teiUid );

        // Get tei from Preheat
        Optional<TrackedEntity> optionalTei = params.getTrackedEntities()
            .stream()
            .filter( tei -> Objects.equals( tei.getTrackedEntity(), teiUid ) )
            .findAny();
        if ( optionalTei.isPresent() )
        {
            return optionalTei.get();
        }
        else if ( trackedEntity != null ) // Otherwise build it from Payload
        {
            TrackedEntity tei = new TrackedEntity();
            tei.setTrackedEntity( teiUid );
            tei.setOrgUnit( trackedEntity.getOrganisationUnit().getUid() );
            return tei;
        }
        else // TEI is not present. A validation error will be thrown in
             // validation phase
        {
            TrackedEntity tei = new TrackedEntity();
            tei.setTrackedEntity( teiUid );
            return tei;
        }
    }

    private List<String> extractValues( Collection<Attribute> attributes, String attribute )
    {
        return attributes
            .stream()
            .filter( a -> a.getAttribute().equals( attribute ) )
            .map( Attribute::getValue )
            .collect( Collectors.toList() );
    }

    private TrackedEntityAttribute extractAttribute( String attribute,
        List<TrackedEntityAttribute> uniqueTrackedEntityAttributes )
    {
        return uniqueTrackedEntityAttributes
            .stream()
            .filter( a -> a.getUid().equals( attribute ) )
            .findAny()
            .orElse( null );
    }
}
