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
package org.hisp.dhis.dimension;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.hisp.dhis.common.DimensionType.CATEGORY;
import static org.hisp.dhis.common.DimensionType.CATEGORY_OPTION_GROUP_SET;
import static org.hisp.dhis.common.DimensionType.DATA_ELEMENT_GROUP_SET;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT_GROUP_SET;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_ESCAPED_SEP;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.splitSafe;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_ORGUNIT_GROUP;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_GRANDCHILDREN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryOptionGroupSetDimension;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.schema.MergeService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.dimension.DimensionService" )
public class DefaultDimensionService
    implements DimensionService
{
    private final IdentifiableObjectManager idObjectManager;

    private final CategoryService categoryService;

    private final PeriodService periodService;

    private final OrganisationUnitService organisationUnitService;

    private final AclService aclService;

    private final CurrentUserService currentUserService;

    private final MergeService mergeService;

    public DefaultDimensionService( IdentifiableObjectManager idObjectManager, CategoryService categoryService,
        PeriodService periodService, OrganisationUnitService organisationUnitService, AclService aclService,
        CurrentUserService currentUserService, MergeService mergeService )
    {
        checkNotNull( idObjectManager );
        checkNotNull( categoryService );
        checkNotNull( periodService );
        checkNotNull( organisationUnitService );
        checkNotNull( aclService );
        checkNotNull( currentUserService );
        checkNotNull( mergeService );

        this.idObjectManager = idObjectManager;
        this.categoryService = categoryService;
        this.periodService = periodService;
        this.organisationUnitService = organisationUnitService;
        this.aclService = aclService;
        this.currentUserService = currentUserService;
        this.mergeService = mergeService;
    }

    // --------------------------------------------------------------------------
    // DimensionService implementation
    // --------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public List<DimensionalItemObject> getCanReadDimensionItems( String uid )
    {
        DimensionalObject dimension = idObjectManager.get( DimensionalObject.DYNAMIC_DIMENSION_CLASSES, uid );

        List<DimensionalItemObject> items = new ArrayList<>();

        if ( dimension != null && dimension.hasItems() )
        {
            User user = currentUserService.getCurrentUser();

            items.addAll( getCanReadObjects( user, dimension.getItems() ) );
        }

        return items;
    }

    @Override
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> List<T> getCanReadObjects( List<T> objects )
    {
        User user = currentUserService.getCurrentUser();

        return getCanReadObjects( user, objects );
    }

    @Override
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> List<T> getCanReadObjects( User user, List<T> objects )
    {
        List<T> list = new ArrayList<>( objects );

        list.removeIf( object -> !aclService.canRead( user, object ) );

        return list;
    }

    @Override
    @Transactional( readOnly = true )
    public DimensionType getDimensionType( String uid )
    {
        Category cat = idObjectManager.get( Category.class, uid );

        if ( cat != null )
        {
            return DimensionType.CATEGORY;
        }

        DataElementGroupSet degs = idObjectManager.get( DataElementGroupSet.class, uid );

        if ( degs != null )
        {
            return DimensionType.DATA_ELEMENT_GROUP_SET;
        }

        OrganisationUnitGroupSet ougs = idObjectManager.get( OrganisationUnitGroupSet.class, uid );

        if ( ougs != null )
        {
            return DimensionType.ORGANISATION_UNIT_GROUP_SET;
        }

        CategoryOptionGroupSet cogs = idObjectManager.get( CategoryOptionGroupSet.class, uid );

        if ( cogs != null )
        {
            return DimensionType.CATEGORY_OPTION_GROUP_SET;
        }

        TrackedEntityAttribute tea = idObjectManager.get( TrackedEntityAttribute.class, uid );

        if ( tea != null )
        {
            return DimensionType.PROGRAM_ATTRIBUTE;
        }

        DataElement pde = idObjectManager.get( DataElement.class, uid );

        if ( pde != null && DataElementDomain.TRACKER.equals( pde.getDomainType() ) )
        {
            return DimensionType.PROGRAM_DATA_ELEMENT;
        }

        ProgramIndicator pin = idObjectManager.get( ProgramIndicator.class, uid );

        if ( pin != null )
        {
            return DimensionType.PROGRAM_INDICATOR;
        }

        final Map<String, DimensionType> dimObjectTypeMap = new HashMap<>();

        dimObjectTypeMap.put( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X );
        dimObjectTypeMap.put( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD );
        dimObjectTypeMap.put( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT );

        return dimObjectTypeMap.get( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DimensionalObject> getAllDimensions()
    {
        Collection<Category> dcs = idObjectManager.getDataDimensions( Category.class );
        Collection<CategoryOptionGroupSet> cogs = idObjectManager.getDataDimensions( CategoryOptionGroupSet.class );
        Collection<DataElementGroupSet> degs = idObjectManager.getDataDimensions( DataElementGroupSet.class );
        Collection<OrganisationUnitGroupSet> ougs = idObjectManager.getDataDimensions( OrganisationUnitGroupSet.class );

        final List<DimensionalObject> dimensions = new ArrayList<>();

        dimensions.addAll( dcs );
        dimensions.addAll( cogs );
        dimensions.addAll( degs );
        dimensions.addAll( ougs );

        User user = currentUserService.getCurrentUser();

        return getCanReadObjects( user, dimensions );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DimensionalObject> getDimensionConstraints()
    {
        Collection<CategoryOptionGroupSet> cogs = idObjectManager.getDataDimensions( CategoryOptionGroupSet.class );
        Collection<Category> cs = categoryService.getAttributeCategories();

        final List<DimensionalObject> dimensions = new ArrayList<>();

        dimensions.addAll( cogs );
        dimensions.addAll( cs );

        return dimensions;
    }

    @Override
    @Transactional( readOnly = true )
    public void mergeAnalyticalObject( BaseAnalyticalObject object )
    {
        if ( object != null )
        {
            object.clear();

            if ( object.getCreatedBy() != null )
            {
                object.setCreatedBy( idObjectManager.get( User.class, object.getCreatedBy().getUid() ) );
            }
            else
            {
                object.setCreatedBy( currentUserService.getCurrentUser() );
            }

            mergeDimensionalObjects( object, object.getColumns() );
            mergeDimensionalObjects( object, object.getRows() );
            mergeDimensionalObjects( object, object.getFilters() );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public void mergeEventAnalyticalObject( EventAnalyticalObject object )
    {
        if ( object != null )
        {
            if ( object.getValue() != null )
            {
                String uid = object.getValue().getUid();

                DataElement dataElement = idObjectManager.get( DataElement.class, uid );

                if ( dataElement != null )
                {
                    object.setDataElementValueDimension( dataElement );
                }

                TrackedEntityAttribute attribute = idObjectManager.get( TrackedEntityAttribute.class, uid );

                if ( attribute != null )
                {
                    object.setAttributeValueDimension( attribute );
                }
            }
        }
    }

    @Override
    @Transactional( readOnly = true )
    public DimensionalObject getDimensionalObjectCopy( String uid, boolean filterCanRead )
    {
        BaseDimensionalObject dimension = idObjectManager.get( DimensionalObject.DYNAMIC_DIMENSION_CLASSES, uid );
        BaseDimensionalObject copy = mergeService.clone( dimension );

        if ( filterCanRead )
        {
            User user = currentUserService.getCurrentUser();
            List<DimensionalItemObject> items = getCanReadObjects( user, dimension.getItems() );
            copy.setItems( items );
        }

        return copy;
    }

    @Override
    @Transactional( readOnly = true )
    public DimensionalItemObject getDataDimensionalItemObject( String dimensionItem )
    {
        return getDataDimensionalItemObject( IdScheme.UID, dimensionItem );
    }

    @Override
    @Transactional( readOnly = true )
    public DimensionalItemObject getDataDimensionalItemObject( IdScheme idScheme, String dimensionItem )
    {
        if ( DimensionalObjectUtils.isCompositeDimensionalObject( dimensionItem ) )
        {
            String id0 = splitSafe( dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 0 );
            String id1 = splitSafe( dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 1 );
            String id2 = splitSafe( dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 2 );

            DataElementOperand operand;
            ReportingRate reportingRate;
            ProgramDataElementDimensionItem programDataElement;
            ProgramTrackedEntityAttributeDimensionItem programAttribute;

            if ( (operand = getDataElementOperand( idScheme, id0, id1, id2 )) != null )
            {
                return operand;
            }
            else if ( (reportingRate = getReportingRate( idScheme, id0, id1 )) != null )
            {
                return reportingRate;
            }
            else if ( (programDataElement = getProgramDataElementDimensionItem( idScheme, id0, id1 )) != null )
            {
                return programDataElement;
            }
            else if ( (programAttribute = getProgramAttributeDimensionItem( idScheme, id0, id1 )) != null )
            {
                return programAttribute;
            }
        }
        else if ( !idScheme.is( IdentifiableProperty.UID ) || CodeGenerator.isValidUid( dimensionItem ) )
        {
            DimensionalItemObject itemObject = idObjectManager.get( DataDimensionItem.DATA_DIMENSION_CLASSES, idScheme,
                dimensionItem );

            if ( itemObject != null )
            {
                return itemObject;
            }
        }

        return null;
    }

    @Override
    @Transactional( readOnly = true )
    public DimensionalItemObject getDataDimensionalItemObject( DimensionalItemId itemId )
    {
        Collection<DimensionalItemObject> items = getDataDimensionalItemObjectMap( Sets.newHashSet( itemId ) ).values();

        return items.isEmpty() ? null : items.iterator().next();
    }

    @Override
    @Transactional( readOnly = true )
    public Map<DimensionalItemId, DimensionalItemObject> getDataDimensionalItemObjectMap(
        Set<DimensionalItemId> itemIds )
    {
        SetMap<Class<? extends IdentifiableObject>, String> atomicIds = getAtomicIds( itemIds );

        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects = getAtomicObjects(
            atomicIds );

        return getItemObjectMap( itemIds, atomicObjects );
    }

    @Override
    @Transactional( readOnly = true )
    public Map<DimensionalItemId, DimensionalItemObject> getNoAclDataDimensionalItemObjectMap(
        Set<DimensionalItemId> itemIds )
    {
        SetMap<Class<? extends IdentifiableObject>, String> atomicIds = getAtomicIds( itemIds );

        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects = getNoAclAtomicObjects(
            atomicIds );

        return getItemObjectMap( itemIds, atomicObjects );
    }

    // --------------------------------------------------------------------------
    // Supportive methods
    // --------------------------------------------------------------------------

    /**
     * Breaks down a set of dimensional item ids into the atomic object ids
     * stored in the database. Returns a map from each class of atomic objects
     * to the set of ids for that object class.
     *
     * @param itemIds a set of dimension item object ids.
     * @return map from atomic object classes to sets of atomic ids.
     */
    private SetMap<Class<? extends IdentifiableObject>, String> getAtomicIds( Set<DimensionalItemId> itemIds )
    {
        SetMap<Class<? extends IdentifiableObject>, String> atomicIds = new SetMap<>();

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

            default:
                log.warn( "Unrecognized DimensionItemType " + id.getDimensionItemType().name() + " in getAtomicIds" );
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
    private MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> getAtomicObjects(
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

    private MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> getNoAclAtomicObjects(
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
    private Map<DimensionalItemId, DimensionalItemObject> getItemObjectMap( Set<DimensionalItemId> itemIds,
        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects )
    {
        Map<DimensionalItemId, DimensionalItemObject> itemObjectMap = new HashMap<>();

        for ( DimensionalItemId id : itemIds )
        {
            if ( !id.hasValidIds() )
            {
                continue;
            }
            BaseDimensionalItemObject dimensionalItemObject = null;

            switch ( id.getDimensionItemType() )
            {
            case DATA_ELEMENT:
                DataElement dataElement = (DataElement) atomicObjects.getValue( DataElement.class, id.getId0() );
                if ( dataElement != null )
                {
                    dimensionalItemObject = cloneIfNeeded( dataElement, id );
                }
                break;

            case INDICATOR:
                Indicator indicator = (Indicator) atomicObjects.getValue( Indicator.class, id.getId0() );
                if ( indicator != null )
                {
                    dimensionalItemObject = cloneIfNeeded( indicator, id );
                }
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
                    dimensionalItemObject = new DataElementOperand( dataElement, categoryOptionCombo,
                        attributeOptionCombo );
                }
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
                if ( program != null && dataElement != null )
                {
                    dimensionalItemObject = new ProgramDataElementDimensionItem( program, dataElement );
                }
                break;

            case PROGRAM_ATTRIBUTE:
                program = (Program) atomicObjects.getValue( Program.class, id.getId0() );
                TrackedEntityAttribute attribute = (TrackedEntityAttribute) atomicObjects
                    .getValue( TrackedEntityAttribute.class, id.getId1() );
                if ( program != null && attribute != null )
                {
                    dimensionalItemObject = new ProgramTrackedEntityAttributeDimensionItem( program, attribute );
                }
                break;

            case PROGRAM_INDICATOR:
                ProgramIndicator programIndicator = (ProgramIndicator) atomicObjects.getValue( ProgramIndicator.class,
                    id.getId0() );
                if ( programIndicator != null )
                {
                    dimensionalItemObject = cloneIfNeeded( programIndicator, id );
                }
                break;

            default:
                log.warn(
                    "Unrecognized DimensionItemType " + id.getDimensionItemType().name() + " in getItemObjectMap" );
                break;
            }

            if ( dimensionalItemObject == null )
            {
                continue;
            }

            dimensionalItemObject.setPeriodOffset( id.getPeriodOffset() );

            itemObjectMap.put( id, dimensionalItemObject );
        }

        return itemObjectMap;
    }

    /**
     * Clones a BaseDimensionalItemObject if the periodOffset is not zero, so
     * there can be a BaseDimensionalItemObject for each different periodOffset.
     *
     * @param item the item to clone if needed.
     * @param id the item id with the periodOffset.
     * @return the item or its clone.
     */
    private BaseDimensionalItemObject cloneIfNeeded( BaseDimensionalItemObject item, DimensionalItemId id )
    {
        if ( id.getPeriodOffset() != 0 )
        {
            try
            {
                return (BaseDimensionalItemObject) BeanUtils.cloneBean( item );
            }
            catch ( Exception e )
            {
                return null;
            }
        }

        return item;
    }

    /**
     * Returns a {@link DataElementOperand}. For identifier wild cards
     * {@link ExpressionService#SYMBOL_WILDCARD}, the relevant property will be
     * null.
     *
     * @param idScheme the identifier scheme.
     * @param dataElementId the data element identifier.
     * @param categoryOptionComboId the category option combo identifier.
     */
    private DataElementOperand getDataElementOperand( IdScheme idScheme, String dataElementId,
        String categoryOptionComboId, String attributeOptionComboId )
    {
        DataElement dataElement = idObjectManager.getObject( DataElement.class, idScheme, dataElementId );
        CategoryOptionCombo categoryOptionCombo = idObjectManager.getObject( CategoryOptionCombo.class, idScheme,
            categoryOptionComboId );
        CategoryOptionCombo attributeOptionCombo = idObjectManager.getObject( CategoryOptionCombo.class, idScheme,
            attributeOptionComboId );

        if ( dataElement == null || (categoryOptionCombo == null && !SYMBOL_WILDCARD.equals( categoryOptionComboId )) )
        {
            return null;
        }

        return new DataElementOperand( dataElement, categoryOptionCombo, attributeOptionCombo );
    }

    /**
     * Returns a {@link ReportingRate}.
     *
     * @param idScheme the identifier scheme.
     * @param dataSetId the data set identifier.
     * @param metric the reporting rate metric.
     */
    private ReportingRate getReportingRate( IdScheme idScheme, String dataSetId, String metric )
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
    private ProgramTrackedEntityAttributeDimensionItem getProgramAttributeDimensionItem( IdScheme idScheme,
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
    private ProgramDataElementDimensionItem getProgramDataElementDimensionItem( IdScheme idScheme, String programId,
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

    /**
     * Sets persistent objects for dimensional associations on the given
     * BaseAnalyticalObject based on the given list of transient
     * DimensionalObjects.
     * <p>
     * Relative periods represented by enums are converted into a
     * RelativePeriods object. User organisation units represented by enums are
     * converted and represented by the user organisation unit persisted
     * properties on the BaseAnalyticalObject.
     *
     * @param object the BaseAnalyticalObject to merge.
     * @param dimensions the list of dimensions.
     */
    private void mergeDimensionalObjects( BaseAnalyticalObject object, List<DimensionalObject> dimensions )
    {
        if ( object == null || dimensions == null )
        {
            return;
        }

        for ( DimensionalObject dimension : dimensions )
        {
            DimensionType type = getDimensionType( dimension.getDimension() );

            String dimensionId = dimension.getDimension();

            List<DimensionalItemObject> items = dimension.getItems();

            if ( items != null )
            {
                List<String> uids = getUids( items );

                if ( DATA_X.equals( type ) )
                {
                    for ( String uid : uids )
                    {
                        DimensionalItemObject dimItemObject = getDataDimensionalItemObject( IdScheme.UID, uid );

                        if ( dimItemObject != null )
                        {
                            DataDimensionItem item = DataDimensionItem.create( dimItemObject );

                            object.getDataDimensionItems().add( item );
                        }
                    }
                }
                else if ( PERIOD.equals( type ) )
                {
                    List<RelativePeriodEnum> enums = new ArrayList<>();
                    List<Period> periods = new UniqueArrayList<>();

                    for ( String isoPeriod : uids )
                    {
                        if ( RelativePeriodEnum.contains( isoPeriod ) )
                        {
                            enums.add( RelativePeriodEnum.valueOf( isoPeriod ) );
                        }
                        else
                        {
                            Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

                            if ( period != null )
                            {
                                periods.add( period );
                            }
                        }
                    }

                    object.setRelatives( new RelativePeriods().setRelativePeriodsFromEnums( enums ) );
                    object.setPeriods( periodService.reloadPeriods( new ArrayList<>( periods ) ) );
                }
                else if ( ORGANISATION_UNIT.equals( type ) )
                {
                    for ( String ou : uids )
                    {
                        if ( KEY_USER_ORGUNIT.equals( ou ) )
                        {
                            object.setUserOrganisationUnit( true );
                        }
                        else if ( KEY_USER_ORGUNIT_CHILDREN.equals( ou ) )
                        {
                            object.setUserOrganisationUnitChildren( true );
                        }
                        else if ( KEY_USER_ORGUNIT_GRANDCHILDREN.equals( ou ) )
                        {
                            object.setUserOrganisationUnitGrandChildren( true );
                        }
                        else if ( ou != null && ou.startsWith( KEY_LEVEL ) )
                        {
                            String level = DimensionalObjectUtils.getValueFromKeywordParam( ou );

                            Integer orgUnitLevel = organisationUnitService
                                .getOrganisationUnitLevelByLevelOrUid( level );

                            if ( orgUnitLevel != null )
                            {
                                object.getOrganisationUnitLevels().add( orgUnitLevel );
                            }
                        }
                        else if ( ou != null && ou.startsWith( KEY_ORGUNIT_GROUP ) )
                        {
                            String uid = DimensionalObjectUtils.getUidFromGroupParam( ou );

                            OrganisationUnitGroup group = idObjectManager.get( OrganisationUnitGroup.class, uid );

                            if ( group != null )
                            {
                                object.getItemOrganisationUnitGroups().add( group );
                            }
                        }
                        else
                        {
                            OrganisationUnit unit = idObjectManager.get( OrganisationUnit.class, ou );

                            if ( unit != null )
                            {
                                object.getOrganisationUnits().add( unit );
                            }
                        }
                    }
                }
                else if ( DATA_ELEMENT_GROUP_SET.equals( type ) )
                {
                    DataElementGroupSetDimension groupSetDimension = new DataElementGroupSetDimension();
                    groupSetDimension.setDimension( idObjectManager.get( DataElementGroupSet.class, dimensionId ) );
                    groupSetDimension.getItems()
                        .addAll( idObjectManager.getByUidOrdered( DataElementGroup.class, uids ) );

                    object.getDataElementGroupSetDimensions().add( groupSetDimension );
                }
                else if ( ORGANISATION_UNIT_GROUP_SET.equals( type ) )
                {
                    OrganisationUnitGroupSetDimension groupSetDimension = new OrganisationUnitGroupSetDimension();
                    groupSetDimension
                        .setDimension( idObjectManager.get( OrganisationUnitGroupSet.class, dimensionId ) );
                    groupSetDimension.getItems()
                        .addAll( idObjectManager.getByUidOrdered( OrganisationUnitGroup.class, uids ) );

                    object.getOrganisationUnitGroupSetDimensions().add( groupSetDimension );
                }
                else if ( CATEGORY.equals( type ) )
                {
                    CategoryDimension categoryDimension = new CategoryDimension();
                    categoryDimension.setDimension( idObjectManager.get( Category.class, dimensionId ) );
                    categoryDimension.getItems()
                        .addAll( idObjectManager.getByUidOrdered( CategoryOption.class, uids ) );

                    object.getCategoryDimensions().add( categoryDimension );
                }
                else if ( CATEGORY_OPTION_GROUP_SET.equals( type ) )
                {
                    CategoryOptionGroupSetDimension groupSetDimension = new CategoryOptionGroupSetDimension();
                    groupSetDimension.setDimension( idObjectManager.get( CategoryOptionGroupSet.class, dimensionId ) );
                    groupSetDimension.getItems()
                        .addAll( idObjectManager.getByUidOrdered( CategoryOptionGroup.class, uids ) );

                    object.getCategoryOptionGroupSetDimensions().add( groupSetDimension );
                }
                else if ( PROGRAM_ATTRIBUTE.equals( type ) )
                {
                    TrackedEntityAttributeDimension attributeDimension = new TrackedEntityAttributeDimension();
                    attributeDimension.setAttribute( idObjectManager.get( TrackedEntityAttribute.class, dimensionId ) );
                    attributeDimension.setLegendSet( dimension.hasLegendSet()
                        ? idObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() )
                        : null );
                    attributeDimension.setFilter( dimension.getFilter() );

                    object.getAttributeDimensions().add( attributeDimension );
                }
                else if ( PROGRAM_DATA_ELEMENT.equals( type ) )
                {
                    TrackedEntityDataElementDimension dataElementDimension = new TrackedEntityDataElementDimension();
                    dataElementDimension.setDataElement( idObjectManager.get( DataElement.class, dimensionId ) );
                    dataElementDimension.setLegendSet( dimension.hasLegendSet()
                        ? idObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() )
                        : null );
                    dataElementDimension.setProgramStage( dimension.hasProgramStage()
                        ? idObjectManager.get( ProgramStage.class, dimension.getProgramStage().getUid() )
                        : null );
                    dataElementDimension.setFilter( dimension.getFilter() );

                    object.getDataElementDimensions().add( dataElementDimension );
                }
                else if ( PROGRAM_INDICATOR.equals( type ) )
                {
                    TrackedEntityProgramIndicatorDimension programIndicatorDimension = new TrackedEntityProgramIndicatorDimension();
                    programIndicatorDimension
                        .setProgramIndicator( idObjectManager.get( ProgramIndicator.class, dimensionId ) );
                    programIndicatorDimension.setLegendSet( dimension.hasLegendSet()
                        ? idObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() )
                        : null );
                    programIndicatorDimension.setFilter( dimension.getFilter() );

                    object.getProgramIndicatorDimensions().add( programIndicatorDimension );
                }
            }
        }
    }
}
