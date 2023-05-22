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
package org.hisp.dhis.dimension;

import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.subexpression.SubexpressionDimensionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This component is only encapsulating specific methods responsible for
 * extracting IdentifiableObjects and Dimensions.
 *
 * The methods were all extracted from a legacy code in order to make them more
 * isolated.
 *
 * @author maikel arabori
 */
@Slf4j
@AllArgsConstructor
@Component
public class DataDimensionExtractor
{
    private final IdentifiableObjectManager idObjectManager;

    /**
     * Breaks down a set of dimensional item ids into the atomic object ids
     * stored in the database. Returns a map from each class of atomic objects
     * to the set of ids for that object class.
     *
     * @param itemIds a set of dimension item object ids.
     * @return map from atomic object classes to sets of atomic ids.
     */
    SetMap<Class<? extends IdentifiableObject>, String> getAtomicIds( Set<DimensionalItemId> itemIds )
    {
        final SetMap<Class<? extends IdentifiableObject>, String> atomicIds = new SetMap<>();

        for ( DimensionalItemId id : itemIds )
        {
            if ( !id.hasValidIds() )
            {
                continue;
            }

            switch ( id.getDimensionItemType() )
            {
                case DATA_ELEMENT:
                    atomicIds.putValue( DataElement.class, id.getId0() );
                    break;

                case DATA_ELEMENT_OPERAND:
                    atomicIds.putValue( DataElement.class, id.getId0() );
                    if ( id.getId1() != null )
                    {
                        atomicIds.putValue( CategoryOptionCombo.class, id.getId1() );
                    }
                    if ( id.getId2() != null )
                    {
                        atomicIds.putValue( CategoryOptionCombo.class, id.getId2() );
                    }
                    break;

                case INDICATOR:
                    atomicIds.putValue( Indicator.class, id.getId0() );
                    break;

                case REPORTING_RATE:
                    atomicIds.putValue( DataSet.class, id.getId0() );
                    break;

                case PROGRAM_DATA_ELEMENT:
                    atomicIds.putValue( Program.class, id.getId0() );
                    atomicIds.putValue( DataElement.class, id.getId1() );
                    break;

                case PROGRAM_ATTRIBUTE:
                    atomicIds.putValue( Program.class, id.getId0() );
                    atomicIds.putValue( TrackedEntityAttribute.class, id.getId1() );
                    break;

                case PROGRAM_INDICATOR:
                    atomicIds.putValue( ProgramIndicator.class, id.getId0() );
                    break;

                case SUBEXPRESSION_DIMENSION_ITEM:
                    atomicIds.putValues( getAtomicIds( id.getSubexItemIds() ) );
                    break;

                default:
                    log.warn(
                        "Unrecognized DimensionItemType " + id.getDimensionItemType().name() + " in getAtomicIds" );
                    break;
            }
        }

        return atomicIds;
    }

    /**
     * Finds the atomic identifiable objects from the database for each object
     * class. This is done for all objects in each class in a single call, for
     * performance (especially for validation rules which may need to look up
     * hundreds if not thousands of objects from a class.
     *
     * @param atomicIds a map from each class of atomic objects to the set of
     *        ids for that identifiable object class.
     * @return a map from each class of atomic objects to a map that associates
     *         each id of that class with an atomic object.
     */
    @Transactional( readOnly = true )
    public MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> getAtomicObjects(
        SetMap<Class<? extends IdentifiableObject>, String> atomicIds )
    {
        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects = new MapMap<>();

        for ( Map.Entry<Class<? extends IdentifiableObject>, Set<String>> entry : atomicIds.entrySet() )
        {
            atomicObjects.putEntries( entry.getKey(),
                idObjectManager.getByUid( entry.getKey(), entry.getValue() ).stream()
                    .collect( Collectors.toMap( IdentifiableObject::getUid, o -> o ) ) );
        }

        return atomicObjects;
    }

    @Transactional( readOnly = true )
    public MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> getNoAclAtomicObjects(
        SetMap<Class<? extends IdentifiableObject>, String> atomicIds )
    {
        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects = new MapMap<>();

        for ( Map.Entry<Class<? extends IdentifiableObject>, Set<String>> entry : atomicIds.entrySet() )
        {
            atomicObjects.putEntries( entry.getKey(),
                idObjectManager.getNoAcl( entry.getKey(), entry.getValue() ).stream()
                    .collect( Collectors.toMap( IdentifiableObject::getUid, o -> o ) ) );
        }

        return atomicObjects;
    }

    /**
     * Gets a map from dimension item ids to their dimension item objects.
     *
     * @param itemIds a set of ids of the dimension item objects to get.
     * @param atomicObjects a map from each class of atomic objects to a map
     *        that associates each id of that class with an atomic object.
     * @return a map from the item ids to the dimension item objects.
     */
    Map<DimensionalItemId, DimensionalItemObject> getItemObjectMap( Set<DimensionalItemId> itemIds,
        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects )
    {
        Map<DimensionalItemId, DimensionalItemObject> itemObjectMap = new HashMap<>();

        for ( DimensionalItemId id : itemIds )
        {
            if ( id.hasValidIds() )
            {
                DimensionalItemObject dimensionalItemObject = getDimensionalItemObject( atomicObjects,
                    id );

                if ( dimensionalItemObject != null )
                {
                    itemObjectMap.put( id, dimensionalItemObject );
                }
            }
        }

        return itemObjectMap;
    }

    /**
     * Returns a {@link ReportingRate}.
     *
     * @param idScheme the identifier scheme.
     * @param dataSetId the data set identifier.
     * @param metric the reporting rate metric.
     */
    @Transactional( readOnly = true )
    public ReportingRate getReportingRate( IdScheme idScheme, String dataSetId, String metric )
    {
        DataSet dataSet = idObjectManager.getObject( DataSet.class, idScheme, dataSetId );
        boolean metricValid = isValidEnum( ReportingRateMetric.class, metric );

        if ( dataSet == null || !metricValid )
        {
            return null;
        }

        return new ReportingRate( dataSet, ReportingRateMetric.valueOf( metric ) );
    }

    /**
     * Returns a {@link ProgramTrackedEntityAttributeDimensionItem}.
     *
     * @param idScheme the identifier scheme.
     * @param programId the program identifier.
     * @param attributeId the attribute identifier.
     */
    @Transactional( readOnly = true )
    public ProgramTrackedEntityAttributeDimensionItem getProgramAttributeDimensionItem( IdScheme idScheme,
        String programId, String attributeId )
    {
        Program program = idObjectManager.getObject( Program.class, idScheme, programId );
        TrackedEntityAttribute attribute = idObjectManager.getObject( TrackedEntityAttribute.class, idScheme,
            attributeId );

        if ( program == null || attribute == null )
        {
            return null;
        }

        return new ProgramTrackedEntityAttributeDimensionItem( program, attribute );
    }

    /**
     * Returns a {@link ProgramDataElementDimensionItem}.
     *
     * @param idScheme the identifier scheme.
     * @param programId the program identifier.
     * @param dataElementId the data element identifier.
     */
    @Transactional( readOnly = true )
    public ProgramDataElementDimensionItem getProgramDataElementDimensionItem( IdScheme idScheme, String programId,
        String dataElementId )
    {
        Program program = idObjectManager.getObject( Program.class, idScheme, programId );
        DataElement dataElement = idObjectManager.getObject( DataElement.class, idScheme, dataElementId );

        if ( program == null || dataElement == null )
        {
            return null;
        }

        return new ProgramDataElementDimensionItem( program, dataElement );
    }

    private DimensionalItemObject getDimensionalItemObject(
        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects, DimensionalItemId id )
    {
        BaseDimensionalItemObject dimensionalItemObject = null;

        switch ( id.getDimensionItemType() )
        {
            case DATA_ELEMENT:
                DataElement dataElement = (DataElement) atomicObjects.getValue( DataElement.class, id.getId0() );
                dimensionalItemObject = withQueryMods( dataElement, id );
                break;

            case INDICATOR:
                Indicator indicator = (Indicator) atomicObjects.getValue( Indicator.class, id.getId0() );
                dimensionalItemObject = withQueryMods( indicator, id );
                break;

            case DATA_ELEMENT_OPERAND:
                dataElement = (DataElement) atomicObjects.getValue( DataElement.class, id.getId0() );
                CategoryOptionCombo categoryOptionCombo = id.getId1() == null ? null
                    : (CategoryOptionCombo) atomicObjects.getValue( CategoryOptionCombo.class, id.getId1() );
                CategoryOptionCombo attributeOptionCombo = id.getId2() == null ? null
                    : (CategoryOptionCombo) atomicObjects.getValue( CategoryOptionCombo.class, id.getId2() );
                if ( dataElement != null &&
                    (id.getId1() != null) == (categoryOptionCombo != null) &&
                    (id.getId2() != null) == (attributeOptionCombo != null) )
                {
                    dimensionalItemObject = new DataElementOperand( (DataElement) withQueryMods( dataElement, id ),
                        categoryOptionCombo, attributeOptionCombo );
                    dimensionalItemObject.setQueryMods( id.getQueryMods() );
                }
                break;

            case SUBEXPRESSION_DIMENSION_ITEM:
                Map<DimensionalItemId, DimensionalItemObject> map = getItemObjectMap( id.getSubexItemIds(),
                    atomicObjects );
                dimensionalItemObject = new SubexpressionDimensionItem( id.getSubexSql(), map.values(),
                    id.getQueryMods() );
                break;

            case REPORTING_RATE:
                DataSet dataSet = (DataSet) atomicObjects.getValue( DataSet.class, id.getId0() );
                if ( dataSet != null )
                {
                    dimensionalItemObject = new ReportingRate( dataSet, ReportingRateMetric.valueOf( id.getId1() ) );
                }
                break;

            case PROGRAM_DATA_ELEMENT:
                Program program = (Program) atomicObjects.getValue( Program.class, id.getId0() );
                dataElement = (DataElement) atomicObjects.getValue( DataElement.class, id.getId1() );
                if ( allNotNull( program, dataElement ) )
                {
                    dimensionalItemObject = new ProgramDataElementDimensionItem( program, dataElement );
                }
                break;

            case PROGRAM_ATTRIBUTE:
                program = (Program) atomicObjects.getValue( Program.class, id.getId0() );
                TrackedEntityAttribute attribute = (TrackedEntityAttribute) atomicObjects
                    .getValue( TrackedEntityAttribute.class, id.getId1() );
                if ( allNotNull( program, attribute ) )
                {
                    dimensionalItemObject = new ProgramTrackedEntityAttributeDimensionItem( program, attribute );
                }
                break;

            case PROGRAM_INDICATOR:
                dimensionalItemObject = (ProgramIndicator) atomicObjects.getValue( ProgramIndicator.class,
                    id.getId0() );
                break;

            default:
                log.warn(
                    "Unrecognized DimensionItemType " + id.getDimensionItemType().name() + " in getItemObjectMap" );
                break;
        }

        return dimensionalItemObject;
    }

    /**
     * Clones a BaseDimensionalItemObject if there are non-default query mods,
     * so the BaseDimensionalItemObject can reflect the query mods.
     *
     * @param item the item to clone if needed.
     * @param id the item id that may have non-default query modifiers.
     * @return the item or its clone.
     */
    private BaseDimensionalItemObject withQueryMods( BaseDimensionalItemObject item, DimensionalItemId id )
    {
        if ( item == null || id.getQueryMods() == null )
        {
            return item;
        }

        try
        {
            BaseDimensionalItemObject clone = (BaseDimensionalItemObject) BeanUtils.cloneBean( item );
            clone.setQueryMods( id.getQueryMods() );
            return clone;
        }
        catch ( Exception e )
        {
            return null;
        }
    }
}
