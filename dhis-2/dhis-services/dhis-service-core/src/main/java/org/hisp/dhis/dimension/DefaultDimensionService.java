package org.hisp.dhis.dimension;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.dataelement.CategoryDimension;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.CategoryOptionGroupSetDimension;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.schema.MergeService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.EnumUtils.isValidEnum;
import static org.hisp.dhis.common.DimensionType.*;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_ESCAPED_SEP;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.splitSafe;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.hisp.dhis.organisationunit.OrganisationUnit.*;

/**
 * @author Lars Helge Overland
 */
public class DefaultDimensionService
    implements DimensionService
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private MergeService mergeService;

    //--------------------------------------------------------------------------
    // DimensionService implementation
    //--------------------------------------------------------------------------

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
    public <T extends IdentifiableObject> List<T> getCanReadObjects( List<T> objects )
    {
        User user = currentUserService.getCurrentUser();

        return getCanReadObjects( user, objects );
    }

    @Override
    public <T extends IdentifiableObject> List<T> getCanReadObjects( User user, List<T> objects )
    {
        List<T> list = new ArrayList<>( objects );
        Iterator<T> iterator = list.iterator();

        while ( iterator.hasNext() )
        {
            T object = iterator.next();

            if ( !aclService.canRead( user, object ) )
            {
                iterator.remove();
            }
        }

        return list;
    }

    @Override
    public DimensionType getDimensionType( String uid )
    {
        DataElementCategory cat = idObjectManager.get( DataElementCategory.class, uid );

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
    public List<DimensionalObject> getAllDimensions()
    {
        Collection<DataElementCategory> dcs = idObjectManager.getDataDimensions( DataElementCategory.class );
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
    public List<DimensionalObject> getDimensionConstraints()
    {
        Collection<CategoryOptionGroupSet> cogs = idObjectManager.getDataDimensions( CategoryOptionGroupSet.class );
        Collection<DataElementCategory> cs = categoryService.getAttributeCategories();

        final List<DimensionalObject> dimensions = new ArrayList<>();

        dimensions.addAll( cogs );
        dimensions.addAll( cs );

        return dimensions;
    }

    @Override
    public void mergeAnalyticalObject( BaseAnalyticalObject object )
    {
        if ( object != null )
        {
            object.clear();

            if ( object.getUser() != null )
            {
                object.setUser( idObjectManager.get( User.class, object.getUser().getUid() ) );
            }
            else
            {
                object.setUser( currentUserService.getCurrentUser() );
            }

            mergeDimensionalObjects( object, object.getColumns() );
            mergeDimensionalObjects( object, object.getRows() );
            mergeDimensionalObjects( object, object.getFilters() );
        }
    }

    @Override
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
    public DimensionalItemObject getDataDimensionalItemObject( String dimensionItem )
    {
        return getDataDimensionalItemObject( IdScheme.UID, dimensionItem );
    }

    @Override
    public DimensionalItemObject getDataDimensionalItemObject( IdScheme idScheme, String dimensionItem )
    {
        if ( DimensionalObjectUtils.isCompositeDimensionalObject( dimensionItem ) )
        {
            String id0 = splitSafe( dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 0 );
            String id1 = splitSafe( dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 1 );
            String id2 = splitSafe( dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 2 );

            DataElementOperand operand = null;
            ReportingRate reportingRate = null;
            ProgramDataElementDimensionItem programDataElement = null;
            ProgramTrackedEntityAttributeDimensionItem programAttribute = null;

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
            DimensionalItemObject itemObject = idObjectManager.
                get( DataDimensionItem.DATA_DIMENSION_CLASSES, idScheme, dimensionItem );

            if ( itemObject != null )
            {
                return itemObject;
            }
        }

        return null;
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    /**
     * Returns a {@link DataElementOperand}. For identifier wild cards
     * {@link ExpressionService#SYMBOL_WILDCARD}, the relevant property
     * will be null.
     *
     * @param idScheme              the identifier scheme.
     * @param dataElementId         the data element identifier.
     * @param categoryOptionComboId the category option combo identifier.
     */
    private DataElementOperand getDataElementOperand( IdScheme idScheme, String dataElementId, String categoryOptionComboId, String attributeOptionComboId )
    {
        DataElement dataElement = idObjectManager.getObject( DataElement.class, idScheme, dataElementId );
        DataElementCategoryOptionCombo categoryOptionCombo = idObjectManager.getObject( DataElementCategoryOptionCombo.class, idScheme, categoryOptionComboId );
        DataElementCategoryOptionCombo attributeOptionCombo = idObjectManager.getObject( DataElementCategoryOptionCombo.class, idScheme, attributeOptionComboId );

        if ( dataElement == null || (categoryOptionCombo == null && !SYMBOL_WILDCARD.equals( categoryOptionComboId )) )
        {
            return null;
        }

        return new DataElementOperand( dataElement, categoryOptionCombo, attributeOptionCombo );
    }

    /**
     * Returns a {@link ReportingRate}.
     *
     * @param idScheme  the identifier scheme.
     * @param dataSetId the data set identifier.
     * @param metric    the reporting rate metric.
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
     * @param idScheme    the identifier scheme.
     * @param programId   the program identifier.
     * @param attributeId the attribute identifier.
     */
    private ProgramTrackedEntityAttributeDimensionItem getProgramAttributeDimensionItem( IdScheme idScheme, String programId, String attributeId )
    {
        Program program = idObjectManager.getObject( Program.class, idScheme, programId );
        TrackedEntityAttribute attribute = idObjectManager.getObject( TrackedEntityAttribute.class, idScheme, attributeId );

        if ( program == null || attribute == null )
        {
            return null;
        }

        return new ProgramTrackedEntityAttributeDimensionItem( program, attribute );
    }

    /**
     * Returns a {@link ProgramDataElementDimensionItem}.
     *
     * @param idScheme      the identifier scheme.
     * @param programId     the program identifier.
     * @param dataElementId the data element identifier.
     */
    private ProgramDataElementDimensionItem getProgramDataElementDimensionItem( IdScheme idScheme, String programId, String dataElementId )
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
     * BaseAnalyticalObject based on the given list of transient DimensionalObjects.
     * <p>
     * Relative periods represented by enums are converted into a RelativePeriods
     * object. User organisation units represented by enums are converted and
     * represented by the user organisation unit persisted properties on the
     * BaseAnalyticalObject.
     *
     * @param object     the BaseAnalyticalObject to merge.
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
                            int level = DimensionalObjectUtils.getLevelFromLevelParam( ou );

                            if ( level > 0 )
                            {
                                object.getOrganisationUnitLevels().add( level );
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
                    groupSetDimension.getItems().addAll( idObjectManager.getByUidOrdered( DataElementGroup.class, uids ) );

                    object.getDataElementGroupSetDimensions().add( groupSetDimension );
                }
                else if ( ORGANISATION_UNIT_GROUP_SET.equals( type ) )
                {
                    OrganisationUnitGroupSetDimension groupSetDimension = new OrganisationUnitGroupSetDimension();
                    groupSetDimension.setDimension( idObjectManager.get( OrganisationUnitGroupSet.class, dimensionId ) );
                    groupSetDimension.getItems().addAll( idObjectManager.getByUidOrdered( OrganisationUnitGroup.class, uids ) );

                    object.getOrganisationUnitGroupSetDimensions().add( groupSetDimension );
                }
                else if ( CATEGORY.equals( type ) )
                {
                    CategoryDimension categoryDimension = new CategoryDimension();
                    categoryDimension.setDimension( idObjectManager.get( DataElementCategory.class, dimensionId ) );
                    categoryDimension.getItems().addAll( idObjectManager.getByUidOrdered( DataElementCategoryOption.class, uids ) );

                    object.getCategoryDimensions().add( categoryDimension );
                }
                else if ( CATEGORY_OPTION_GROUP_SET.equals( type ) )
                {
                    CategoryOptionGroupSetDimension groupSetDimension = new CategoryOptionGroupSetDimension();
                    groupSetDimension.setDimension( idObjectManager.get( CategoryOptionGroupSet.class, dimensionId ) );
                    groupSetDimension.getItems().addAll( idObjectManager.getByUidOrdered( CategoryOptionGroup.class, uids ) );

                    object.getCategoryOptionGroupSetDimensions().add( groupSetDimension );
                }
                else if ( PROGRAM_ATTRIBUTE.equals( type ) )
                {
                    TrackedEntityAttributeDimension attributeDimension = new TrackedEntityAttributeDimension();
                    attributeDimension.setAttribute( idObjectManager.get( TrackedEntityAttribute.class, dimensionId ) );
                    attributeDimension.setLegendSet( dimension.hasLegendSet() ?
                        idObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() ) : null );
                    attributeDimension.setFilter( dimension.getFilter() );

                    object.getAttributeDimensions().add( attributeDimension );
                }
                else if ( PROGRAM_DATA_ELEMENT.equals( type ) )
                {
                    TrackedEntityDataElementDimension dataElementDimension = new TrackedEntityDataElementDimension();
                    dataElementDimension.setDataElement( idObjectManager.get( DataElement.class, dimensionId ) );
                    dataElementDimension.setLegendSet( dimension.hasLegendSet() ?
                        idObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() ) : null );
                    dataElementDimension.setFilter( dimension.getFilter() );

                    object.getDataElementDimensions().add( dataElementDimension );
                }
                else if ( PROGRAM_INDICATOR.equals( type ) )
                {
                    TrackedEntityProgramIndicatorDimension programIndicatorDimension = new TrackedEntityProgramIndicatorDimension();
                    programIndicatorDimension.setProgramIndicator( idObjectManager.get( ProgramIndicator.class, dimensionId ) );
                    programIndicatorDimension.setLegendSet( dimension.hasLegendSet() ?
                        idObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() ) : null );
                    programIndicatorDimension.setFilter( dimension.getFilter() );

                    object.getProgramIndicatorDimensions().add( programIndicatorDimension );
                }
            }
        }
    }
}
