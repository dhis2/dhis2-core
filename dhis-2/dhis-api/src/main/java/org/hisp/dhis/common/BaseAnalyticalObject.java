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
package org.hisp.dhis.common;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_OCTOBER;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_COLLAPSED_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.STATIC_DIMS;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_ORGUNIT_GROUP;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_GRANDCHILDREN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.UserOrgUnitType;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroupSetDimension;
import org.hisp.dhis.common.adapter.JacksonPeriodDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodSerializer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.eventvisualization.Attribute;
import org.hisp.dhis.eventvisualization.SimpleDimension;
import org.hisp.dhis.eventvisualization.SimpleDimension.Type;
import org.hisp.dhis.eventvisualization.SimpleDimensionHandler;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.period.ConfigurablePeriod;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.DefaultValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * This class contains associations to dimensional meta-data. Should typically
 * be sub-classed by analytical objects like tables, maps and charts.
 * <p>
 * Implementation note: Objects currently managing this class are
 * AnalyticsService, DefaultDimensionService and the getDimensionalObject and
 * getDimensionalObjectList methods of this class.
 *
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "analyticalObject", namespace = DxfNamespaces.DXF_2_0 )
public abstract class BaseAnalyticalObject
    extends BaseNameableObject
    implements AnalyticalObject
{
    public static final int ASC = -1;

    public static final int DESC = 1;

    public static final int NONE = 0;

    public static final String NOT_A_VALID_DIMENSION = "Not a valid dimension: %s";

    // -------------------------------------------------------------------------
    // Persisted properties
    // -------------------------------------------------------------------------

    protected List<DataDimensionItem> dataDimensionItems = new ArrayList<>();

    protected List<OrganisationUnit> organisationUnits = new ArrayList<>();

    protected List<Period> periods = new ArrayList<>();

    private Date startDate;

    private Date endDate;

    protected RelativePeriods relatives;

    protected List<DataElementGroupSetDimension> dataElementGroupSetDimensions = new ArrayList<>();

    protected List<OrganisationUnitGroupSetDimension> organisationUnitGroupSetDimensions = new ArrayList<>();

    protected List<Integer> organisationUnitLevels = new ArrayList<>();

    protected List<CategoryDimension> categoryDimensions = new ArrayList<>();

    protected List<CategoryOptionGroupSetDimension> categoryOptionGroupSetDimensions = new ArrayList<>();

    protected List<TrackedEntityAttributeDimension> attributeDimensions = new ArrayList<>();

    protected List<TrackedEntityDataElementDimension> dataElementDimensions = new ArrayList<>();

    protected List<TrackedEntityProgramIndicatorDimension> programIndicatorDimensions = new ArrayList<>();

    protected boolean userOrganisationUnit;

    protected boolean userOrganisationUnitChildren;

    protected boolean userOrganisationUnitGrandChildren;

    protected List<OrganisationUnitGroup> itemOrganisationUnitGroups = new ArrayList<>();

    protected DigitGroupSeparator digitGroupSeparator;

    protected int sortOrder;

    protected int topLimit;

    protected AggregationType aggregationType;

    protected boolean completedOnly;

    protected String timeField;

    protected String orgUnitField;

    protected String title;

    protected String subtitle;

    protected boolean hideTitle;

    protected boolean hideSubtitle;

    protected Set<Interpretation> interpretations = new HashSet<>();

    protected Set<String> subscribers = new HashSet<>();

    protected UserOrgUnitType userOrgUnitType;

    // -------------------------------------------------------------------------
    // Analytical properties
    // -------------------------------------------------------------------------

    protected transient List<DimensionalObject> columns = new ArrayList<>();

    protected transient List<DimensionalObject> rows = new ArrayList<>();

    protected transient List<DimensionalObject> filters = new ArrayList<>();

    protected transient Map<String, String> parentGraphMap = new HashMap<>();

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    protected transient List<OrganisationUnit> transientOrganisationUnits = new ArrayList<>();

    protected transient List<CategoryOptionCombo> transientCategoryOptionCombos = new ArrayList<>();

    protected transient Date relativePeriodDate;

    protected transient OrganisationUnit relativeOrganisationUnit;

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public abstract void init( User user, Date date, OrganisationUnit organisationUnit,
        List<OrganisationUnit> organisationUnitsAtLevel, List<OrganisationUnit> organisationUnitsInGroups,
        I18nFormat format );

    @Override
    public abstract void populateAnalyticalProperties();

    @Override
    public boolean hasUserOrgUnit()
    {
        return userOrganisationUnit || userOrganisationUnitChildren || userOrganisationUnitGrandChildren;
    }

    public boolean hasRelativePeriods()
    {
        return relatives != null && !relatives.isEmpty();
    }

    public boolean hasOrganisationUnitLevels()
    {
        return organisationUnitLevels != null && !organisationUnitLevels.isEmpty();
    }

    public boolean hasItemOrganisationUnitGroups()
    {
        return itemOrganisationUnitGroups != null && !itemOrganisationUnitGroups.isEmpty();
    }

    public boolean hasSortOrder()
    {
        return sortOrder != 0;
    }

    public boolean hasTitle()
    {
        return title != null && !title.isEmpty();
    }

    protected void addTransientOrganisationUnits( Collection<OrganisationUnit> organisationUnits )
    {
        if ( organisationUnits != null )
        {
            this.transientOrganisationUnits.addAll( organisationUnits );
        }
    }

    protected void addTransientOrganisationUnit( OrganisationUnit organisationUnit )
    {
        if ( organisationUnit != null )
        {
            this.transientOrganisationUnits.add( organisationUnit );
        }
    }

    /**
     * Returns dimension items for data dimensions.
     */
    public List<DimensionalItemObject> getDataDimensionNameableObjects()
    {
        return dataDimensionItems.stream().map( DataDimensionItem::getDimensionalItemObject )
            .collect( Collectors.toList() );
    }

    /**
     * Adds a data dimension object.
     *
     * @return true if a data dimension was added, false if not.
     */
    @Override
    public boolean addDataDimensionItem( DimensionalItemObject object )
    {
        if ( object != null && DataDimensionItem.DATA_DIMENSION_CLASSES.contains( object.getClass() ) )
        {
            return dataDimensionItems.add( DataDimensionItem.create( object ) );
        }

        return false;
    }

    /**
     * Removes a data dimension object.
     *
     * @return true if a data dimension was removed, false if not.
     */
    @Override
    public boolean removeDataDimensionItem( DimensionalItemObject object )
    {
        if ( object != null && DataDimensionItem.DATA_DIMENSION_CLASSES.contains( object.getClass() ) )
        {
            return dataDimensionItems
                .removeAll( DataDimensionItem.createWithDependencies( object, dataDimensionItems ) );
        }

        return false;
    }

    /**
     * Adds a {@link DataElementGroupSetDimension}.
     *
     * @param dimension the dimension to add.
     */
    @Override
    public void addDataElementGroupSetDimension( DataElementGroupSetDimension dimension )
    {
        dataElementGroupSetDimensions.add( dimension );
    }

    /**
     * Adds an {@link OrganisationUnitGroupSetDimension}.
     *
     * @param dimension the dimension to add.
     */
    @Override
    public void addOrganisationUnitGroupSetDimension( OrganisationUnitGroupSetDimension dimension )
    {
        organisationUnitGroupSetDimensions.add( dimension );
    }

    /**
     * Adds a {@link CategoryDimension}.
     *
     * @param dimension the dimension to add.
     */
    public void addCategoryDimension( CategoryDimension dimension )
    {
        categoryDimensions.add( dimension );
    }

    /**
     * Adds a {@link CategoryOptionGroupSetDimension}.
     *
     * @param dimension the dimension to add.
     */
    @Override
    public void addCategoryOptionGroupSetDimension( CategoryOptionGroupSetDimension dimension )
    {
        categoryOptionGroupSetDimensions.add( dimension );
    }

    /**
     * Adds a {@link TrackedEntityDataElementDimension}.
     *
     * @param dimension the dimension to add.
     */
    @Override
    public void addTrackedEntityDataElementDimension( TrackedEntityDataElementDimension dimension )
    {
        dataElementDimensions.add( dimension );
    }

    @Override
    public void clearTransientState()
    {
        columns = new ArrayList<>();
        rows = new ArrayList<>();
        filters = new ArrayList<>();
        parentGraphMap = new HashMap<>();

        transientOrganisationUnits = new ArrayList<>();
        transientCategoryOptionCombos = new ArrayList<>();
        relativePeriodDate = null;
        relativeOrganisationUnit = null;

        clearTransientStateProperties();
    }

    /**
     * Clears transient properties.
     */
    protected abstract void clearTransientStateProperties();

    /**
     * Adds all given data dimension objects.
     */
    public void addAllDataDimensionItems( Collection<? extends DimensionalItemObject> objects )
    {
        for ( DimensionalItemObject object : objects )
        {
            addDataDimensionItem( object );
        }
    }

    /**
     * Returns all data elements in the data dimensions. The returned list is
     * immutable.
     */
    @JsonIgnore
    public List<DataElement> getDataElements()
    {
        return ImmutableList.copyOf( dataDimensionItems.stream().filter( i -> i.getDataElement() != null )
            .map( DataDimensionItem::getDataElement ).collect( Collectors.toList() ) );
    }

    /**
     * Returns all indicators in the data dimensions. The returned list is
     * immutable.
     */
    @JsonIgnore
    public List<Indicator> getIndicators()
    {
        return ImmutableList.copyOf( dataDimensionItems.stream().filter( i -> i.getIndicator() != null )
            .map( DataDimensionItem::getIndicator ).collect( Collectors.toList() ) );
    }

    /**
     * Assembles a DimensionalObject based on the persisted properties of this
     * AnalyticalObject. Collapses indicators, data elements, data element
     * operands and data sets into the dx dimension.
     * <p>
     * Collapses fixed and relative periods into the pe dimension. Collapses
     * fixed and user organisation units into the ou dimension.
     *
     * @param dimension the dimension identifier.
     * @param date the date used for generating relative periods.
     * @param user the current user.
     * @param dynamicNames whether to use dynamic or static names.
     * @param format the I18nFormat.
     * @return a DimensionalObject.
     */
    protected DimensionalObject getDimensionalObject( String dimension, Date date, User user, boolean dynamicNames,
        List<OrganisationUnit> organisationUnitsAtLevel, List<OrganisationUnit> organisationUnitsInGroups,
        I18nFormat format )
    {
        List<DimensionalItemObject> items = new ArrayList<>();

        DimensionType type = null;

        if ( DATA_X_DIM_ID.equals( dimension ) )
        {
            items.addAll( getDataDimensionNameableObjects() );

            type = DimensionType.DATA_X;
        }
        else if ( PERIOD_DIM_ID.equals( dimension ) )
        {
            setPeriodNames( periods, dynamicNames, format );

            items.addAll( periods );

            if ( hasRelativePeriods() )
            {
                items.addAll( relatives.getRelativePeriods( date, format, dynamicNames, FINANCIAL_YEAR_OCTOBER ) );
            }

            type = DimensionType.PERIOD;
        }
        else if ( ORGUNIT_DIM_ID.equals( dimension ) )
        {
            items.addAll( organisationUnits );
            items.addAll( transientOrganisationUnits );

            if ( userOrganisationUnit && user != null && user.hasOrganisationUnit() )
            {
                items.addAll( user.getOrganisationUnits() );
            }

            if ( userOrganisationUnitChildren && user != null && user.hasOrganisationUnit() )
            {
                user.getOrganisationUnits().forEach( ou -> items.addAll( ou.getSortedChildren() ) );
            }

            if ( userOrganisationUnitGrandChildren && user != null && user.hasOrganisationUnit() )
            {
                user.getOrganisationUnits().forEach( ou -> items.addAll( ou.getSortedGrandChildren() ) );
            }

            if ( organisationUnitLevels != null && !organisationUnitLevels.isEmpty()
                && organisationUnitsAtLevel != null )
            {
                items.addAll( organisationUnitsAtLevel ); // Must be set
                                                          // externally
            }

            if ( itemOrganisationUnitGroups != null && !itemOrganisationUnitGroups.isEmpty()
                && organisationUnitsInGroups != null )
            {
                items.addAll( organisationUnitsInGroups ); // Must be set
                                                           // externally
            }

            type = DimensionType.ORGANISATION_UNIT;
        }
        else if ( CATEGORYOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            items.addAll( transientCategoryOptionCombos );

            type = DimensionType.CATEGORY_OPTION_COMBO;
        }
        else if ( STATIC_DIMS.contains( dimension ) )
        {
            type = DimensionType.STATIC;
        }
        else
        {
            final Optional<DimensionalObject> trackedEntityDimension = getTrackedEntityDimension( dimension );

            if ( trackedEntityDimension.isPresent() )
            {
                return trackedEntityDimension.get();
            }
        }

        IdentifiableObjectUtils.removeDuplicates( items );

        return new BaseDimensionalObject( dimension, type, items );
    }

    /**
     * This method will first try to return a concrete dimension (one that can
     * be persisted and managed). If a concrete dimension is not found, then it
     * will try to find a "String" dimension (one that is not defined anywhere
     * and only exists for very specific use cases. See
     * {@link SimpleDimension}).
     *
     * @param eventAnalyticalObject the object of type EventAnalyticalObject
     * @param dimension the dimension, ie: dx, pe, eventDate
     * @param parent the parent attribute
     * @return the dimensional object related to the given dimension and
     *         attribute.
     */
    protected DimensionalObject getDimensionalObject( final EventAnalyticalObject eventAnalyticalObject,
        final String dimension, final Attribute parent )
    {
        final Optional<DimensionalObject> dimensionalObject = getDimensionalObject( dimension );

        if ( dimensionalObject.isPresent() )
        {
            return dimensionalObject.get();
        }
        else if ( Type.contains( dimension ) )
        {
            return new SimpleDimensionHandler( eventAnalyticalObject ).getDimensionalObject( dimension, parent );
        }
        else
        {
            throw new IllegalArgumentException( format( NOT_A_VALID_DIMENSION, dimension ) );
        }
    }

    /**
     * Populates the given dimensionalObjects list based on the respective
     * dimensions provided.
     *
     * @param dimensions
     * @param dimensionalObjects
     */
    protected void populateDimensions( final List<String> dimensions, final List<DimensionalObject> dimensionalObjects )
    {
        if ( isNotEmpty( dimensions ) )
        {
            for ( final String dimension : dimensions )
            {
                if ( isNotBlank( dimension ) )
                {
                    final Optional<DimensionalObject> dimensionalObject = getDimensionalObject( dimension );
                    if ( dimensionalObject.isPresent() )
                    {
                        dimensionalObjects.add( dimensionalObject.get() );
                    }
                }
            }
        }
    }

    /**
     * Populates the given dimensionalObjects list based on the respective
     * dimensions provided. It takes in consideration simple dimensions along
     * with its associated "attribute".
     *
     * @param dimensions
     * @param dimensionalObjects
     */
    protected void populateDimensions( final List<String> dimensions, final List<DimensionalObject> dimensionalObjects,
        final Attribute attribute, final EventAnalyticalObject eventAnalyticalObject )
    {
        if ( isNotEmpty( dimensions ) )
        {
            for ( final String dimension : dimensions )
            {
                if ( isNotBlank( dimension ) )
                {
                    dimensionalObjects.add( getDimensionalObject( eventAnalyticalObject, dimension, attribute ) );
                }
            }
        }
    }

    /**
     * Assembles a list of DimensionalObjects based on the concrete objects in
     * this BaseAnalyticalObject.
     * <p>
     * Merges fixed and relative periods into the pe dimension, where the
     * RelativePeriods object is represented by enums (e.g. LAST_MONTH). Merges
     * fixed and user organisation units into the ou dimension, where user
     * organisation units properties are represented by enums (e.g.
     * USER_ORG_UNIT).
     * <p>
     * This method is useful when serializing the AnalyticalObject.
     *
     * @param dimension the dimension identifier.
     * @return a list of DimensionalObjects.
     */
    protected Optional<DimensionalObject> getDimensionalObject( String dimension )
    {
        if ( DATA_X_DIM_ID.equals( dimension ) )
        {
            return Optional
                .of( new BaseDimensionalObject( dimension, DimensionType.DATA_X, getDataDimensionNameableObjects() ) );
        }
        else if ( PERIOD_DIM_ID.equals( dimension ) )
        {
            List<Period> periodList = new ArrayList<>( periods );

            if ( hasRelativePeriods() )
            {
                List<RelativePeriodEnum> list = relatives.getRelativePeriodEnums();

                for ( RelativePeriodEnum periodEnum : list )
                {
                    periodList.add( new ConfigurablePeriod( periodEnum.toString() ) );
                }
            }

            return Optional.of( new BaseDimensionalObject( dimension, DimensionType.PERIOD, periodList ) );
        }
        else if ( ORGUNIT_DIM_ID.equals( dimension ) )
        {
            List<DimensionalItemObject> ouList = new ArrayList<>();
            ouList.addAll( organisationUnits );
            ouList.addAll( transientOrganisationUnits );

            if ( userOrganisationUnit )
            {
                ouList.add( new BaseDimensionalItemObject( KEY_USER_ORGUNIT ) );
            }

            if ( userOrganisationUnitChildren )
            {
                ouList.add( new BaseDimensionalItemObject( KEY_USER_ORGUNIT_CHILDREN ) );
            }

            if ( userOrganisationUnitGrandChildren )
            {
                ouList.add( new BaseDimensionalItemObject( KEY_USER_ORGUNIT_GRANDCHILDREN ) );
            }

            if ( organisationUnitLevels != null && !organisationUnitLevels.isEmpty() )
            {
                for ( Integer level : organisationUnitLevels )
                {
                    String id = KEY_LEVEL + level;

                    ouList.add( new BaseDimensionalItemObject( id ) );
                }
            }

            if ( itemOrganisationUnitGroups != null && !itemOrganisationUnitGroups.isEmpty() )
            {
                for ( OrganisationUnitGroup group : itemOrganisationUnitGroups )
                {
                    String id = KEY_ORGUNIT_GROUP + group.getUid();

                    ouList.add( new BaseDimensionalItemObject( id ) );
                }
            }

            return Optional.of( new BaseDimensionalObject( dimension, DimensionType.ORGANISATION_UNIT, ouList ) );
        }
        else if ( CATEGORYOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            return Optional
                .of( new BaseDimensionalObject( dimension, DimensionType.CATEGORY_OPTION_COMBO, new ArrayList<>() ) );
        }
        else if ( DATA_COLLAPSED_DIM_ID.equals( dimension ) )
        {
            return Optional
                .of( new BaseDimensionalObject( dimension, DimensionType.DATA_COLLAPSED, new ArrayList<>() ) );
        }
        else if ( STATIC_DIMS.contains( dimension ) )
        {
            return Optional.of( new BaseDimensionalObject( dimension, DimensionType.STATIC, new ArrayList<>() ) );
        }
        else
        {
            // Embedded dimensions

            Optional<DimensionalObject> object;

            if ( (object = getDimensionFromEmbeddedObjects( dimension, DimensionType.DATA_ELEMENT_GROUP_SET,
                dataElementGroupSetDimensions )).isPresent() )
            {
                return Optional.of( object.get() );
            }

            if ( (object = getDimensionFromEmbeddedObjects( dimension, DimensionType.ORGANISATION_UNIT_GROUP_SET,
                organisationUnitGroupSetDimensions )).isPresent() )
            {
                return Optional.of( object.get() );
            }

            if ( (object = getDimensionFromEmbeddedObjects( dimension, DimensionType.CATEGORY, categoryDimensions ))
                .isPresent() )
            {
                return Optional.of( object.get() );
            }

            if ( (object = getDimensionFromEmbeddedObjects( dimension, DimensionType.CATEGORY_OPTION_GROUP_SET,
                categoryOptionGroupSetDimensions )).isPresent() )
            {
                return Optional.of( object.get() );
            }

            final Optional<DimensionalObject> trackedEntityDimension = getTrackedEntityDimension( dimension );

            if ( trackedEntityDimension.isPresent() )
            {
                return trackedEntityDimension;
            }
        }

        return Optional.empty();
    }

    private Optional<DimensionalObject> getTrackedEntityDimension( final String dimension )
    {
        // Tracked entity attribute

        final Map<String, TrackedEntityAttributeDimension> attributes = Maps.uniqueIndex( attributeDimensions,
            TrackedEntityAttributeDimension::getUid );

        if ( attributes.containsKey( dimension ) )
        {
            final TrackedEntityAttributeDimension tead = attributes.get( dimension );

            if ( tead != null )
            {
                final ValueType valueType = tead.getAttribute() != null ? tead.getAttribute().getValueType()
                    : null;
                final OptionSet optionSet = tead.getAttribute() != null ? tead.getAttribute().getOptionSet()
                    : null;

                return Optional.of( new BaseDimensionalObject( dimension, DimensionType.PROGRAM_ATTRIBUTE, null,
                    tead.getDisplayName(), tead.getLegendSet(), null, tead.getFilter(), valueType, optionSet ) );
            }
        }

        // Tracked entity data element

        final Map<String, TrackedEntityDataElementDimension> dataElements = Maps.uniqueIndex( dataElementDimensions,
            TrackedEntityDataElementDimension::getUid );

        if ( dataElements.containsKey( dimension ) )
        {
            final TrackedEntityDataElementDimension tedd = dataElements.get( dimension );

            if ( tedd != null )
            {
                final ValueType valueType = tedd.getDataElement() != null
                    ? tedd.getDataElement().getValueType()
                    : null;
                final OptionSet optionSet = tedd.getDataElement() != null
                    ? tedd.getDataElement().getOptionSet()
                    : null;

                return Optional.of( new BaseDimensionalObject( dimension, DimensionType.PROGRAM_DATA_ELEMENT, null,
                    tedd.getDisplayName(), tedd.getLegendSet(), tedd.getProgramStage(), tedd.getFilter(), valueType,
                    optionSet ) );
            }
        }

        // Tracked entity program indicator

        final Map<String, TrackedEntityProgramIndicatorDimension> programIndicators = Maps
            .uniqueIndex( programIndicatorDimensions, TrackedEntityProgramIndicatorDimension::getUid );

        if ( programIndicators.containsKey( dimension ) )
        {
            final TrackedEntityProgramIndicatorDimension teid = programIndicators.get( dimension );

            return Optional.of( new BaseDimensionalObject( dimension, DimensionType.PROGRAM_INDICATOR, null,
                teid.getDisplayName(), teid.getLegendSet(), null, teid.getFilter() ) );
        }

        return Optional.empty();
    }

    /**
     * Searches for a {@link DimensionalObject} with the given dimension
     * identifier in the given list of {@link DimensionalEmbeddedObject} items.
     *
     * @param dimension the dimension identifier.
     * @param dimensionType the dimension type.
     * @param embeddedObjects the list of embedded dimension objects.
     * @return a {@link DimensionalObject} optional, or an empty optional if not
     *         found.
     */
    private <T extends DimensionalEmbeddedObject> Optional<DimensionalObject> getDimensionFromEmbeddedObjects(
        String dimension, DimensionType dimensionType, List<T> embeddedObjects )
    {
        final Map<String, T> dimensions = Maps.uniqueIndex( embeddedObjects, d -> d.getDimension().getDimension() );

        if ( dimensions.containsKey( dimension ) )
        {
            final DimensionalEmbeddedObject object = dimensions.get( dimension );

            if ( object != null )
            {
                return Optional.of( new BaseDimensionalObject( dimension, dimensionType,
                    object.getDimension().getDisplayName(), object.getItems() ) );
            }
        }

        return Optional.empty();
    }

    private void setPeriodNames( List<Period> periods, boolean dynamicNames, I18nFormat format )
    {
        for ( Period period : periods )
        {
            RelativePeriods.setName( period, null, dynamicNames, format );
        }
    }

    /**
     * Returns meta-data mapping for this analytical object. Includes a
     * identifier to name mapping for dynamic dimensions.
     */
    public Map<String, String> getMetaData()
    {
        final Map<String, String> meta = new HashMap<>();

        // TODO use getDimension() instead of getUid() ?
        dataElementGroupSetDimensions
            .forEach( dim -> meta.put( dim.getDimension().getUid(), dim.getDimension().getDisplayName() ) );
        organisationUnitGroupSetDimensions
            .forEach( group -> meta.put( group.getDimension().getUid(), group.getDimension().getDisplayName() ) );
        categoryDimensions
            .forEach( dim -> meta.put( dim.getDimension().getUid(), dim.getDimension().getDisplayName() ) );

        return meta;
    }

    /**
     * Clear or set to false all persistent dimensional (not property)
     * properties for this object.
     */
    public void clear()
    {
        dataDimensionItems.clear();
        periods.clear();
        organisationUnits.clear();
        dataElementGroupSetDimensions.clear();
        organisationUnitGroupSetDimensions.clear();
        organisationUnitLevels.clear();
        categoryDimensions.clear();
        categoryOptionGroupSetDimensions.clear();
        attributeDimensions.clear();
        dataElementDimensions.clear();
        programIndicatorDimensions.clear();
        itemOrganisationUnitGroups.clear();
        relatives = null;
        userOrganisationUnit = false;
        userOrganisationUnitChildren = false;
        userOrganisationUnitGrandChildren = false;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataDimensionItems", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataDimensionItem", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataDimensionItem> getDataDimensionItems()
    {
        return dataDimensionItems;
    }

    public void setDataDimensionItems( List<DataDimensionItem> dataDimensionItems )
    {
        this.dataDimensionItems = dataDimensionItems;
    }

    @Override
    @JsonProperty
    @JsonSerialize( contentAs = BaseNameableObject.class )
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public List<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( List<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    @Override
    @JsonProperty
    @JsonSerialize( contentUsing = JacksonPeriodSerializer.class )
    @JsonDeserialize( contentUsing = JacksonPeriodDeserializer.class )
    @JacksonXmlElementWrapper( localName = "periods", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "period", namespace = DxfNamespaces.DXF_2_0 )
    public List<Period> getPeriods()
    {
        return periods;
    }

    public void setPeriods( List<Period> periods )
    {
        this.periods = periods;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }

    @Gist( included = Include.FALSE )
    @JsonProperty( value = "relativePeriods" )
    @JacksonXmlProperty( localName = "relativePeriods", namespace = DxfNamespaces.DXF_2_0 )
    public RelativePeriods getRelatives()
    {
        return relatives;
    }

    public void setRelatives( RelativePeriods relatives )
    {
        this.relatives = relatives;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataElementGroupSetDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElementGroupSetDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElementGroupSetDimension> getDataElementGroupSetDimensions()
    {
        return dataElementGroupSetDimensions;
    }

    public void setDataElementGroupSetDimensions( List<DataElementGroupSetDimension> dataElementGroupSetDimensions )
    {
        this.dataElementGroupSetDimensions = dataElementGroupSetDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "organisationUnitGroupSetDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnitGroupSetDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<OrganisationUnitGroupSetDimension> getOrganisationUnitGroupSetDimensions()
    {
        return organisationUnitGroupSetDimensions;
    }

    public void setOrganisationUnitGroupSetDimensions(
        List<OrganisationUnitGroupSetDimension> organisationUnitGroupSetDimensions )
    {
        this.organisationUnitGroupSetDimensions = organisationUnitGroupSetDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "organisationUnitLevels", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnitLevel", namespace = DxfNamespaces.DXF_2_0 )
    public List<Integer> getOrganisationUnitLevels()
    {
        return organisationUnitLevels;
    }

    public void setOrganisationUnitLevels( List<Integer> organisationUnitLevels )
    {
        this.organisationUnitLevels = organisationUnitLevels;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "categoryDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<CategoryDimension> getCategoryDimensions()
    {
        return categoryDimensions;
    }

    public void setCategoryDimensions( List<CategoryDimension> categoryDimensions )
    {
        this.categoryDimensions = categoryDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "categoryOptionGroupSetDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOptionGroupSetDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<CategoryOptionGroupSetDimension> getCategoryOptionGroupSetDimensions()
    {
        return categoryOptionGroupSetDimensions;
    }

    public void setCategoryOptionGroupSetDimensions(
        List<CategoryOptionGroupSetDimension> categoryOptionGroupSetDimensions )
    {
        this.categoryOptionGroupSetDimensions = categoryOptionGroupSetDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "attributeDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "attributeDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityAttributeDimension> getAttributeDimensions()
    {
        return attributeDimensions;
    }

    public void setAttributeDimensions( List<TrackedEntityAttributeDimension> attributeDimensions )
    {
        this.attributeDimensions = attributeDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataElementDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElementDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityDataElementDimension> getDataElementDimensions()
    {
        return dataElementDimensions;
    }

    public void setDataElementDimensions( List<TrackedEntityDataElementDimension> dataElementDimensions )
    {
        this.dataElementDimensions = dataElementDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programIndicatorDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programIndicatorDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityProgramIndicatorDimension> getProgramIndicatorDimensions()
    {
        return programIndicatorDimensions;
    }

    public void setProgramIndicatorDimensions( List<TrackedEntityProgramIndicatorDimension> programIndicatorDimensions )
    {
        this.programIndicatorDimensions = programIndicatorDimensions;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isUserOrganisationUnit()
    {
        return userOrganisationUnit;
    }

    public void setUserOrganisationUnit( boolean userOrganisationUnit )
    {
        this.userOrganisationUnit = userOrganisationUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isUserOrganisationUnitChildren()
    {
        return userOrganisationUnitChildren;
    }

    public void setUserOrganisationUnitChildren( boolean userOrganisationUnitChildren )
    {
        this.userOrganisationUnitChildren = userOrganisationUnitChildren;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isUserOrganisationUnitGrandChildren()
    {
        return userOrganisationUnitGrandChildren;
    }

    public void setUserOrganisationUnitGrandChildren( boolean userOrganisationUnitGrandChildren )
    {
        this.userOrganisationUnitGrandChildren = userOrganisationUnitGrandChildren;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "itemOrganisationUnitGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "itemOrganisationUnitGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<OrganisationUnitGroup> getItemOrganisationUnitGroups()
    {
        return itemOrganisationUnitGroups;
    }

    public void setItemOrganisationUnitGroups( List<OrganisationUnitGroup> itemOrganisationUnitGroups )
    {
        this.itemOrganisationUnitGroups = itemOrganisationUnitGroups;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DigitGroupSeparator getDigitGroupSeparator()
    {
        return DefaultValue.defaultIfNull( digitGroupSeparator );
    }

    public void setDigitGroupSeparator( DigitGroupSeparator digitGroupSeparator )
    {
        this.digitGroupSeparator = digitGroupSeparator;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = Integer.MIN_VALUE )
    public int getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder( int sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getTopLimit()
    {
        return topLimit;
    }

    public void setTopLimit( int topLimit )
    {
        this.topLimit = topLimit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public void setAggregationType( AggregationType aggregationType )
    {
        this.aggregationType = aggregationType;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCompletedOnly()
    {
        return completedOnly;
    }

    public void setCompletedOnly( boolean completedOnly )
    {
        this.completedOnly = completedOnly;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTimeField()
    {
        return timeField;
    }

    public void setTimeField( String timeField )
    {
        this.timeField = timeField;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOrgUnitField()
    {
        return orgUnitField;
    }

    public void setOrgUnitField( String orgUnitField )
    {
        this.orgUnitField = orgUnitField;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTitle()
    {
        return title;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Translatable( propertyName = "title" )
    public String getDisplayTitle()
    {
        return getTranslation( "title", getTitle() );
    }

    public void setTitle( String title )
    {
        this.title = title;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSubtitle()
    {
        return subtitle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Translatable( propertyName = "subtitle" )
    public String getDisplaySubtitle()
    {
        return getTranslation( "subtitle", getSubtitle() );
    }

    public void setSubtitle( String subtitle )
    {
        this.subtitle = subtitle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHideTitle()
    {
        return hideTitle;
    }

    public void setHideTitle( boolean hideTitle )
    {
        this.hideTitle = hideTitle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHideSubtitle()
    {
        return hideSubtitle;
    }

    public void setHideSubtitle( boolean hideSubtitle )
    {
        this.hideSubtitle = hideSubtitle;
    }

    @Override
    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "interpretations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "interpretation", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Interpretation> getInterpretations()
    {
        return interpretations;
    }

    public void setInterpretations( Set<Interpretation> interpretations )
    {
        this.interpretations = interpretations;
    }

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    @JsonIgnore
    public List<OrganisationUnit> getTransientOrganisationUnits()
    {
        return transientOrganisationUnits;
    }

    @Override
    @JsonIgnore
    public Date getRelativePeriodDate()
    {
        return relativePeriodDate;
    }

    @Override
    @JsonIgnore
    public OrganisationUnit getRelativeOrganisationUnit()
    {
        return relativeOrganisationUnit;
    }

    // -------------------------------------------------------------------------
    // Analytical properties
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JsonDeserialize( contentAs = BaseDimensionalObject.class )
    @JacksonXmlElementWrapper( localName = "columns", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "column", namespace = DxfNamespaces.DXF_2_0 )
    public List<DimensionalObject> getColumns()
    {
        return columns;
    }

    public void setColumns( List<DimensionalObject> columns )
    {
        this.columns = columns;
    }

    @Override
    @JsonProperty
    @JsonDeserialize( contentAs = BaseDimensionalObject.class )
    @JacksonXmlElementWrapper( localName = "rows", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "row", namespace = DxfNamespaces.DXF_2_0 )
    public List<DimensionalObject> getRows()
    {
        return rows;
    }

    public void setRows( List<DimensionalObject> rows )
    {
        this.rows = rows;
    }

    @Override
    @JsonProperty
    @JsonDeserialize( contentAs = BaseDimensionalObject.class )
    @JacksonXmlElementWrapper( localName = "filters", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "filter", namespace = DxfNamespaces.DXF_2_0 )
    public List<DimensionalObject> getFilters()
    {
        return filters;
    }

    public void setFilters( List<DimensionalObject> filters )
    {
        this.filters = filters;
    }

    @Override
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    @JacksonXmlProperty( localName = "parentGraphMap", namespace = DxfNamespaces.DXF_2_0 )
    public Map<String, String> getParentGraphMap()
    {
        return parentGraphMap;
    }

    public void setParentGraphMap( Map<String, String> parentGraphMap )
    {
        this.parentGraphMap = parentGraphMap;
    }

    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "subscribers", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "subscriber", namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getSubscribers()
    {
        return subscribers;
    }

    public void setSubscribers( Set<String> subscribers )
    {
        this.subscribers = subscribers;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSubscribed()
    {
        User user = UserContext.getUser();

        return (user != null && subscribers != null) && subscribers.contains( user.getUid() );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserOrgUnitType getUserOrgUnitType()
    {
        return userOrgUnitType;
    }

    public void setUserOrgUnitType( UserOrgUnitType userOrgUnitType )
    {
        this.userOrgUnitType = userOrgUnitType;
    }

    @Override
    public boolean subscribe( User user )
    {
        if ( this.subscribers == null )
        {
            this.subscribers = new HashSet<>();
        }

        return this.subscribers.add( user.getUid() );
    }

    @Override
    public boolean unsubscribe( User user )
    {
        if ( this.subscribers == null )
        {
            this.subscribers = new HashSet<>();
        }

        return this.subscribers.remove( user.getUid() );
    }
}
