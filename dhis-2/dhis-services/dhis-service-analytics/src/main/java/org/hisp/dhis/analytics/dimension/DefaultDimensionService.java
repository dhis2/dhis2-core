package org.hisp.dhis.analytics.dimension;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.hisp.dhis.common.DimensionType.CATEGORY;
import static org.hisp.dhis.common.DimensionType.CATEGORYOPTION_GROUPSET;
import static org.hisp.dhis.common.DimensionType.DATAELEMENT_GROUPSET;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATIONUNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATIONUNIT_GROUPSET;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionType.PROGRAM_DATAELEMENT;
import static org.hisp.dhis.common.DimensionType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.splitSafe;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_ORGUNIT_GROUP;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_GRANDCHILDREN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryDimension;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DefaultDimensionService
    implements DimensionService
{
    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private DataElementOperandService operandService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CurrentUserService currentUserService;

    //--------------------------------------------------------------------------
    // DimensionService implementation
    //--------------------------------------------------------------------------

    @Override
    public DimensionalObject getDimension( String uid )
    {
        DataElementCategory cat = identifiableObjectManager.get( DataElementCategory.class, uid );

        if ( cat != null )
        {
            cat.setDimensionType( DimensionType.CATEGORY );
            return cat;
        }

        DataElementGroupSet degs = identifiableObjectManager.get( DataElementGroupSet.class, uid );

        if ( degs != null )
        {
            degs.setDimensionType( DimensionType.DATAELEMENT_GROUPSET );
            return degs;
        }

        OrganisationUnitGroupSet ougs = identifiableObjectManager.get( OrganisationUnitGroupSet.class, uid );

        if ( ougs != null )
        {
            ougs.setDimensionType( DimensionType.ORGANISATIONUNIT_GROUPSET );
            return ougs;
        }

        CategoryOptionGroupSet cogs = identifiableObjectManager.get( CategoryOptionGroupSet.class, uid );

        if ( cogs != null )
        {
            cogs.setDimensionType( DimensionType.CATEGORYOPTION_GROUPSET );
            return cogs;
        }
        
        TrackedEntityAttribute tea = identifiableObjectManager.get( TrackedEntityAttribute.class, uid );
        
        if ( tea != null )
        {
            tea.setDimensionType( DimensionType.PROGRAM_ATTRIBUTE );
            return tea;
        }
        
        DataElement pde = identifiableObjectManager.get( DataElement.class, uid );
        
        if ( pde != null && DataElementDomain.TRACKER.equals( pde.getDomainType() ) )
        {
            pde.setDimensionType( DimensionType.PROGRAM_DATAELEMENT );
            return pde;
        }
        
        ProgramIndicator pin = identifiableObjectManager.get( ProgramIndicator.class, uid );
        
        if ( pin != null )
        {
            pin.setDimensionType( DimensionType.PROGRAM_INDICATOR );
            return pin;
        }
        
        return null;
    }
    
    @Override
    public DimensionalObject getDimension( String uid, DimensionType dimensionType )
    {
        if ( uid == null || dimensionType == null )
        {
            return null;
        }
        
        Class<? extends DimensionalObject> clazz = DimensionalObject.DYNAMIC_DIMENSION_TYPE_CLASS_MAP.get( dimensionType );
        
        if ( clazz == null )
        {
            return null;
        }
        
        return identifiableObjectManager.get( clazz, uid );
    }
    
    @Override
    public List<NameableObject> getCanReadDimensionItems( String uid )
    {
        DimensionalObject dimension = getDimension( uid );

        List<NameableObject> items = new ArrayList<>();

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
        DataElementCategory cat = identifiableObjectManager.get( DataElementCategory.class, uid );

        if ( cat != null )
        {
            return DimensionType.CATEGORY;
        }

        DataElementGroupSet degs = identifiableObjectManager.get( DataElementGroupSet.class, uid );

        if ( degs != null )
        {
            return DimensionType.DATAELEMENT_GROUPSET;
        }

        OrganisationUnitGroupSet ougs = identifiableObjectManager.get( OrganisationUnitGroupSet.class, uid );

        if ( ougs != null )
        {
            return DimensionType.ORGANISATIONUNIT_GROUPSET;
        }

        CategoryOptionGroupSet cogs = identifiableObjectManager.get( CategoryOptionGroupSet.class, uid );

        if ( cogs != null )
        {
            return DimensionType.CATEGORYOPTION_GROUPSET;
        }

        TrackedEntityAttribute tea = identifiableObjectManager.get( TrackedEntityAttribute.class, uid );
        
        if ( tea != null )
        {
            return DimensionType.PROGRAM_ATTRIBUTE;
        }
        
        DataElement pde = identifiableObjectManager.get( DataElement.class, uid );
        
        if ( pde != null && DataElementDomain.TRACKER.equals( pde.getDomainType() ) )
        {
            return DimensionType.PROGRAM_DATAELEMENT;
        }
        
        ProgramIndicator pin = identifiableObjectManager.get( ProgramIndicator.class, uid );
        
        if ( pin != null )
        {
            return DimensionType.PROGRAM_INDICATOR;
        }

        final Map<String, DimensionType> dimObjectTypeMap = new HashMap<>();

        dimObjectTypeMap.put( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X );
        dimObjectTypeMap.put( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD );
        dimObjectTypeMap.put( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATIONUNIT );

        return dimObjectTypeMap.get( uid );
    }

    @Override
    public List<DimensionalObject> getAllDimensions()
    {
        Collection<DataElementCategory> dcs = identifiableObjectManager.getDataDimensions( DataElementCategory.class );
        Collection<CategoryOptionGroupSet> cogs = identifiableObjectManager.getDataDimensions( CategoryOptionGroupSet.class );
        Collection<DataElementGroupSet> degs = identifiableObjectManager.getDataDimensions( DataElementGroupSet.class );
        Collection<OrganisationUnitGroupSet> ougs = identifiableObjectManager.getDataDimensions( OrganisationUnitGroupSet.class );

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
        Collection<CategoryOptionGroupSet> cogs = identifiableObjectManager.getDataDimensions( CategoryOptionGroupSet.class );
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
                object.setUser( identifiableObjectManager.get( User.class, object.getUser().getUid() ) );
            }
            else
            {
                object.setUser( currentUserService.getCurrentUser() );
            }
            
            if ( object.getProgram() != null )
            {
                object.setProgram( identifiableObjectManager.get( Program.class, object.getProgram().getUid() ) );
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
                
                DataElement dataElement = identifiableObjectManager.get( DataElement.class, uid );
                
                if ( dataElement != null )
                {
                    object.setDataElementValueDimension( dataElement );
                }
                
                TrackedEntityAttribute attribute = identifiableObjectManager.get( TrackedEntityAttribute.class, uid );
                
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
        DimensionalObject dimension = getDimension( uid );
        
        BaseDimensionalObject copy = new BaseDimensionalObject();
        copy.mergeWith( dimension, MergeStrategy.MERGE_IF_NOT_NULL );
        
        if ( filterCanRead )
        {
            User user = currentUserService.getCurrentUser();
            List<NameableObject> items = getCanReadObjects( user, dimension.getItems() );
            copy.setItems( items );
        }
        
        return copy;
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    /**
     * Sets persistent objects for dimensional associations on the given
     * BaseAnalyticalObject based on the given list of transient DimensionalObjects.
     * <p/>
     * Relative periods represented by enums are converted into a RelativePeriods
     * object. User organisation units represented by enums are converted and
     * represented by the user organisation unit persisted properties on the
     * BaseAnalyticalObject.
     *
     * @param object     the BaseAnalyticalObject to merge.
     * @param dimensions the
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

            List<NameableObject> items = dimension.getItems();

            if ( items != null )
            {
                List<String> uids = getUids( items );

                if ( DATA_X.equals( type ) )
                {
                    for ( String uid : uids )
                    {
                        if ( DimensionalObjectUtils.isValidDimensionalOperand( uid ) )
                        {
                            DataElementOperand operand = operandService.getOrAddDataElementOperand( 
                                splitSafe( uid, DataElementOperand.ESCAPED_SEPARATOR, 0 ), splitSafe( uid, DataElementOperand.ESCAPED_SEPARATOR, 1 ) );
                            
                            if ( operand != null )
                            {
                                object.getDataDimensionItems().add( DataDimensionItem.create( operand ) );
                            }
                        }
                        else
                        {
                            NameableObject dataObject = identifiableObjectManager.get( DataDimensionItem.DATA_DIMENSION_CLASSES, uid );
                            
                            if ( dataObject != null )
                            {
                                object.getDataDimensionItems().add( DataDimensionItem.create( dataObject ) );
                            }
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
                else if ( ORGANISATIONUNIT.equals( type ) )
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

                            OrganisationUnitGroup group = identifiableObjectManager.get( OrganisationUnitGroup.class, uid );

                            if ( group != null )
                            {
                                object.getItemOrganisationUnitGroups().add( group );
                            }
                        }
                        else
                        {
                            OrganisationUnit unit = identifiableObjectManager.get( OrganisationUnit.class, ou );

                            if ( unit != null )
                            {
                                object.getOrganisationUnits().add( unit );
                            }
                        }
                    }
                }
                else if ( CATEGORY.equals( type ) )
                {
                    DataElementCategoryDimension categoryDimension = new DataElementCategoryDimension();
                    categoryDimension.setDimension( identifiableObjectManager.get( DataElementCategory.class, dimensionId ) );
                    categoryDimension.getItems().addAll( identifiableObjectManager.getByUidOrdered( DataElementCategoryOption.class, uids ) );

                    object.getCategoryDimensions().add( categoryDimension );
                }
                else if ( DATAELEMENT_GROUPSET.equals( type ) )
                {
                    object.getDataElementGroups().addAll( identifiableObjectManager.getByUidOrdered( DataElementGroup.class, uids ) );
                }
                else if ( ORGANISATIONUNIT_GROUPSET.equals( type ) )
                {
                    object.getOrganisationUnitGroups().addAll( identifiableObjectManager.getByUidOrdered( OrganisationUnitGroup.class, uids ) );
                }
                else if ( CATEGORYOPTION_GROUPSET.equals( type ) )
                {
                    object.getCategoryOptionGroups().addAll( identifiableObjectManager.getByUidOrdered( CategoryOptionGroup.class, uids ) );
                }
                else if ( PROGRAM_ATTRIBUTE.equals( type ) )
                {
                    TrackedEntityAttributeDimension attributeDimension = new TrackedEntityAttributeDimension();
                    attributeDimension.setAttribute( identifiableObjectManager.get( TrackedEntityAttribute.class, dimensionId ) );
                    attributeDimension.setLegendSet( dimension.hasLegendSet() ? identifiableObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() ) : null );
                    attributeDimension.setFilter( dimension.getFilter() );
                    
                    object.getAttributeDimensions().add( attributeDimension );
                }
                else if ( PROGRAM_DATAELEMENT.equals( type ) )
                {
                    TrackedEntityDataElementDimension dataElementDimension = new TrackedEntityDataElementDimension();
                    dataElementDimension.setDataElement( identifiableObjectManager.get( DataElement.class, dimensionId ) );
                    dataElementDimension.setLegendSet( dimension.hasLegendSet() ? identifiableObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() ) : null );
                    dataElementDimension.setFilter( dimension.getFilter() );
                    
                    object.getDataElementDimensions().add( dataElementDimension );
                }
                else if ( PROGRAM_INDICATOR.equals( type ) )
                {
                    TrackedEntityProgramIndicatorDimension programIndicatorDimension = new TrackedEntityProgramIndicatorDimension();
                    programIndicatorDimension.setProgramIndicator( identifiableObjectManager.get( ProgramIndicator.class, dimensionId ) );
                    programIndicatorDimension.setLegendSet( dimension.hasLegendSet() ? identifiableObjectManager.get( LegendSet.class, dimension.getLegendSet().getUid() ) : null );
                    programIndicatorDimension.setFilter( dimension.getFilter() );
                    
                    object.getProgramIndicatorDimensions().add( programIndicatorDimension );
                }
            }
        }
    }
}
