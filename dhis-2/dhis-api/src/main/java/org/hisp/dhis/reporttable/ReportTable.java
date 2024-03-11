package org.hisp.dhis.reporttable;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.NumberType;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendDisplayStrategy;
import org.hisp.dhis.legend.LegendDisplayStyle;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.user.User;
import org.springframework.util.Assert;

import java.util.*;
import java.util.Objects;

import static org.hisp.dhis.common.DimensionalObject.*;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "reportTable", namespace = DxfNamespaces.DXF_2_0 )
public class ReportTable
    extends BaseAnalyticalObject implements MetadataObject
{
    public static final String REPORTING_MONTH_COLUMN_NAME = "reporting_month_name";
    public static final String PARAM_ORGANISATIONUNIT_COLUMN_NAME = "param_organisationunit_name";
    public static final String ORGANISATION_UNIT_IS_PARENT_COLUMN_NAME = "organisation_unit_is_parent";

    public static final String SEPARATOR = "_";
    public static final String DASH_PRETTY_SEPARATOR = " - ";
    public static final String SPACE = " ";
    public static final String KEY_ORGUNIT_GROUPSET = "orgunit_groupset_";

    public static final String TOTAL_COLUMN_NAME = "total";
    public static final String TOTAL_COLUMN_PRETTY_NAME = "Total";

    public static final DimensionalItemObject[] IRT = new DimensionalItemObject[0];
    public static final DimensionalItemObject[][] IRT2D = new DimensionalItemObject[0][];

    public static final String EMPTY = "";

    private static final String ILLEGAL_FILENAME_CHARS_REGEX = "[/\\?%*:|\"'<>.]";

    public static final Map<String, String> COLUMN_NAMES = DimensionalObjectUtils.asMap(
        DATA_X_DIM_ID, "data",
        CATEGORYOPTIONCOMBO_DIM_ID, "categoryoptioncombo",
        PERIOD_DIM_ID, "period",
        ORGUNIT_DIM_ID, "organisationunit"
    );

    // -------------------------------------------------------------------------
    // Persisted properties
    // -------------------------------------------------------------------------

    /**
     * Indicates the criteria to apply to data measures.
     */
    private String measureCriteria;
    
    /**
     * Indicates whether the ReportTable contains regression columns.
     */
    private boolean regression;

    /**
     * Indicates whether the ReportTable contains cumulative columns.
     */
    private boolean cumulative;

    /**
     * Dimensions to crosstabulate / use as columns.
     */
    private List<String> columnDimensions = new ArrayList<>();

    /**
     * Dimensions to use as rows.
     */
    private List<String> rowDimensions = new ArrayList<>();

    /**
     * Dimensions to use as filter.
     */
    private List<String> filterDimensions = new ArrayList<>();

    /**
     * The ReportParams of the ReportTable.
     */
    private ReportParams reportParams;

    /**
     * Indicates rendering of row totals for the table.
     */
    private boolean rowTotals;

    /**
     * Indicates rendering of column totals for the table.
     */
    private boolean colTotals;

    /**
     * Indicates rendering of row sub-totals for the table.
     */
    private boolean rowSubTotals;

    /**
     * Indicates rendering of column sub-totals for the table.
     */
    private boolean colSubTotals;

    /**
     * Indicates whether to hide rows with no data values in the table.
     */
    private boolean hideEmptyRows;
    
    /**
     * Indicates whether to hide columns with no data values in the table.
     */
    private boolean hideEmptyColumns;

    /**
     * The display density of the text in the table.
     */
    private DisplayDensity displayDensity;

    /**
     * The font size of the text in the table.
     */
    private FontSize fontSize;

    /**
     * The legend set in the table.
     */
    private LegendSet legendSet;

    /**
     * The legend set display strategy.
     */
    private LegendDisplayStrategy legendDisplayStrategy;

    /**
     * The legend set display type.
     */
    private LegendDisplayStyle legendDisplayStyle;

    /**
     * The number type.
     */
    private NumberType numberType;
    
    /**
     * Indicates showing organisation unit hierarchy names.
     */
    private boolean showHierarchy;

    /**
     * Indicates showing organisation unit hierarchy names.
     */
    private boolean showDimensionLabels;

    /**
     * Indicates rounding values.
     */
    private boolean skipRounding;

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    /**
     * All crosstabulated columns.
     */
    private transient List<List<DimensionalItemObject>> gridColumns = new ArrayList<>();

    /**
     * All rows.
     */
    private transient List<List<DimensionalItemObject>> gridRows = new ArrayList<>();

    /**
     * The name of the reporting month based on the report param.
     */
    private transient String reportingPeriodName;

    /**
     * The title of the report table grid.
     */
    private transient String gridTitle;

    @Override
    protected void clearTransientStateProperties()
    {
        gridColumns = new ArrayList<>();
        gridRows = new ArrayList<>();
        reportingPeriodName = null;
        gridTitle = null;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Constructor for persistence purposes.
     */
    public ReportTable()
    {
    }

    /**
     * Default constructor.
     *
     * @param name the name.
     * @param dataElements the data elements.
     * @param indicators the indicators.
     * @param reportingRates the reporting rates.
     * @param periods the periods. Cannot have the name property set.
     * @param organisationUnits the organisation units.
     * @param doIndicators indicating whether indicators should be crosstabulated.
     * @param doPeriods indicating whether periods should be crosstabulated.
     * @param doUnits indicating whether organisation units should be crosstabulated.
     * @param relatives the relative periods.
     * @param reportParams the report parameters.
     * @param reportingPeriodName the reporting period name.
     */
    public ReportTable( String name, List<DataElement> dataElements, List<Indicator> indicators,
        List<ReportingRate> reportingRates, List<Period> periods,
        List<OrganisationUnit> organisationUnits,
        boolean doIndicators, boolean doPeriods, boolean doUnits, RelativePeriods relatives, ReportParams reportParams,
        String reportingPeriodName )
    {
        this.name = name;
        addAllDataDimensionItems( dataElements );
        addAllDataDimensionItems( indicators );
        addAllDataDimensionItems( reportingRates );
        this.periods = periods;
        this.organisationUnits = organisationUnits;
        this.relatives = relatives;
        this.reportParams = reportParams;
        this.reportingPeriodName = reportingPeriodName;

        if ( doIndicators )
        {
            columnDimensions.add( DATA_X_DIM_ID );
        }
        else
        {
            rowDimensions.add( DATA_X_DIM_ID );
        }

        if ( doPeriods )
        {
            columnDimensions.add( PERIOD_DIM_ID );
        }
        else
        {
            rowDimensions.add( PERIOD_DIM_ID );
        }

        if ( doUnits )
        {
            columnDimensions.add( ORGUNIT_DIM_ID );
        }
        else
        {
            rowDimensions.add( ORGUNIT_DIM_ID );
        }
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    public void init( User user, Date date, OrganisationUnit organisationUnit,
        List<OrganisationUnit> organisationUnitsAtLevel, List<OrganisationUnit> organisationUnitsInGroups, I18nFormat format )
    {
        verify( (periods != null && !periods.isEmpty()) || hasRelativePeriods(), "Must contain periods or relative periods" );

        this.relativePeriodDate = date;
        this.relativeOrganisationUnit = organisationUnit;

        // Handle report parameters

        if ( hasRelativePeriods() )
        {
            this.reportingPeriodName = relatives.getReportingPeriodName( date, format );
        }

        if ( organisationUnit != null && hasReportParams() && reportParams.isParamParentOrganisationUnit() )
        {
            organisationUnit.setCurrentParent( true );
            addTransientOrganisationUnits( organisationUnit.getChildren() );
            addTransientOrganisationUnit( organisationUnit );
        }

        if ( organisationUnit != null && hasReportParams() && reportParams.isParamOrganisationUnit() )
        {
            addTransientOrganisationUnit( organisationUnit );
        }

        // Handle special dimension

        if ( isDimensional() )
        {
            transientCategoryOptionCombos.addAll( Objects.requireNonNull( getFirstCategoryCombo() ).getSortedOptionCombos() );
            verify( nonEmptyLists( transientCategoryOptionCombos ) == 1, "Category option combos size must be larger than 0" );
        }

        // Populate grid

        this.populateGridColumnsAndRows( date, user, organisationUnitsAtLevel, organisationUnitsInGroups, format );
    }

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    public void populateGridColumnsAndRows( Date date, User user,
        List<OrganisationUnit> organisationUnitsAtLevel, List<OrganisationUnit> organisationUnitsInGroups, I18nFormat format )
    {
        List<DimensionalItemObject[]> tableColumns = new ArrayList<>();
        List<DimensionalItemObject[]> tableRows = new ArrayList<>();
        List<DimensionalItemObject> filterItems = new ArrayList<>();

        for ( String dimension : columnDimensions )
        {
            tableColumns.add( getDimensionalObject( dimension, date, user, false, organisationUnitsAtLevel, organisationUnitsInGroups, format ).getItems().toArray( IRT ) );
        }

        for ( String dimension : rowDimensions )
        {
            tableRows.add( getDimensionalObject( dimension, date, user, true, organisationUnitsAtLevel, organisationUnitsInGroups, format ).getItems().toArray( IRT ) );
        }

        for ( String filter : filterDimensions )
        {
            filterItems.addAll( getDimensionalObject( filter, date, user, true, organisationUnitsAtLevel, organisationUnitsInGroups, format ).getItems() );
        }

        gridColumns = new CombinationGenerator<>( tableColumns.toArray( IRT2D ) ).getCombinations();
        gridRows = new CombinationGenerator<>( tableRows.toArray( IRT2D ) ).getCombinations();

        addListIfEmpty( gridColumns );
        addListIfEmpty( gridRows );

        gridTitle = IdentifiableObjectUtils.join( filterItems );
    }

    @Override
    public void populateAnalyticalProperties()
    {
        for ( String column : columnDimensions )
        {
            columns.add( getDimensionalObject( column ) );
        }

        for ( String row : rowDimensions )
        {
            rows.add( getDimensionalObject( row ) );
        }

        for ( String filter : filterDimensions )
        {
            filters.add( getDimensionalObject( filter ) );
        }
    }

    /**
     * Indicates whether this ReportTable is multi-dimensional.
     */
    public boolean isDimensional()
    {
        return !getDataElements().isEmpty() && (
            columnDimensions.contains( CATEGORYOPTIONCOMBO_DIM_ID ) || rowDimensions.contains( CATEGORYOPTIONCOMBO_DIM_ID ));
    }

    /**
     * Generates a pretty column name based on the given display property of the
     * argument objects. Null arguments are ignored in the name.
     */
    public static String getPrettyColumnName( List<DimensionalItemObject> objects, DisplayProperty displayProperty )
    {
        StringBuilder builder = new StringBuilder();

        for ( DimensionalItemObject object : objects )
        {
            builder.append( object != null ? ( object.getDisplayProperty( displayProperty ) + SPACE ) : EMPTY );
        }

        return builder.length() > 0 ? builder.substring( 0, builder.lastIndexOf( SPACE ) ) : TOTAL_COLUMN_PRETTY_NAME;
    }

    /**
     * Generates a column name based on short-names of the argument objects.
     * Null arguments are ignored in the name.
     * <p/>
     * The period column name must be static when on columns so it can be
     * re-used in reports, hence the name property is used which will be formatted
     * only when the period dimension is on rows.
     */
    public static String getColumnName( List<DimensionalItemObject> objects )
    {
        StringBuffer buffer = new StringBuffer();

        for ( DimensionalItemObject object : objects )
        {
            if ( object != null && object instanceof Period )
            {
                buffer.append( object.getName() ).append( SEPARATOR );
            }
            else
            {
                buffer.append( object != null ? ( object.getShortName() + SEPARATOR ) : EMPTY );
            }
        }

        String column = columnEncode( buffer.toString() );

        return column.length() > 0 ? column.substring( 0, column.lastIndexOf( SEPARATOR ) ) : TOTAL_COLUMN_NAME;
    }

    /**
     * Generates a string which is acceptable as a filename.
     */
    public static String columnEncode( String string )
    {
        if ( string != null )
        {
            string = string.replaceAll( "<", "_lt" );
            string = string.replaceAll( ">", "_gt" );
            string = string.replaceAll( ILLEGAL_FILENAME_CHARS_REGEX, EMPTY );
            string = string.length() > 255 ? string.substring( 0, 255 ) : string;
            string = string.toLowerCase();
        }

        return string;
    }

    /**
     * Checks whether the given List of IdentifiableObjects contains an object
     * which is an OrganisationUnit and has the currentParent property set to
     * true.
     *
     * @param objects the List of IdentifiableObjects.
     */
    public static boolean isCurrentParent( List<? extends IdentifiableObject> objects )
    {
        for ( IdentifiableObject object : objects )
        {
            if ( object != null && object instanceof OrganisationUnit && ((OrganisationUnit) object).isCurrentParent() )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether this report table has report params.
     */
    public boolean hasReportParams()
    {
        return reportParams != null;
    }

    /**
     * Returns the name of the parent organisation unit, or an empty string if null.
     */
    public String getParentOrganisationUnitName()
    {
        return relativeOrganisationUnit != null ? relativeOrganisationUnit.getName() : EMPTY;
    }

    /**
     * Adds an empty list of DimensionalItemObjects to the given list if empty.
     */
    public static void addListIfEmpty( List<List<DimensionalItemObject>> list )
    {
        if ( list != null && list.size() == 0 )
        {
            list.add( Arrays.asList( new DimensionalItemObject[0] ) );
        }
    }

    /**
     * Generates a grid for this report table based on the given aggregate value
     * map.
     *
     * @param grid the grid, should be empty and not null.
     * @param valueMap the mapping of identifiers to aggregate values.
     * @param displayProperty the display property to use for meta data.
     * @param reportParamColumns whether to include report parameter columns.
     * @return a grid.
     */
    public Grid getGrid( Grid grid, Map<String, Object> valueMap, DisplayProperty displayProperty, boolean reportParamColumns )
    {
        valueMap = new HashMap<>( valueMap );

        sortKeys( valueMap );

        // ---------------------------------------------------------------------
        // Title
        // ---------------------------------------------------------------------

        if ( name != null )
        {
            grid.setTitle( name );
            grid.setSubtitle( gridTitle );
        }
        else
        {
            grid.setTitle( gridTitle );
        }

        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        Map<String, String> metaData = getMetaData();
        metaData.putAll( DimensionalObject.PRETTY_NAMES );

        for ( String row : rowDimensions )
        {
            String name = StringUtils.defaultIfEmpty( metaData.get( row ), row );
            String col = StringUtils.defaultIfEmpty( COLUMN_NAMES.get( row ), row );

            grid.addHeader( new GridHeader( name + " ID", col + "id", ValueType.TEXT, String.class.getName(), true, true ) );
            grid.addHeader( new GridHeader( name, col + "name", ValueType.TEXT, String.class.getName(), false, true ) );
            grid.addHeader( new GridHeader( name + " code", col + "code", ValueType.TEXT, String.class.getName(), true, true ) );
            grid.addHeader( new GridHeader( name + " description", col + "description", ValueType.TEXT, String.class.getName(), true, true ) );
        }

        if ( reportParamColumns )
        {
            grid.addHeader( new GridHeader( "Reporting month", REPORTING_MONTH_COLUMN_NAME,
                ValueType.TEXT, String.class.getName(), true, true ) );
            grid.addHeader( new GridHeader( "Organisation unit parameter", PARAM_ORGANISATIONUNIT_COLUMN_NAME,
                ValueType.TEXT, String.class.getName(), true, true ) );
            grid.addHeader( new GridHeader( "Organisation unit is parent", ORGANISATION_UNIT_IS_PARENT_COLUMN_NAME,
                ValueType.TEXT, String.class.getName(), true, true ) );
        }

        final int startColumnIndex = grid.getHeaders().size();
        final int numberOfColumns = getGridColumns().size();

        for ( List<DimensionalItemObject> column : gridColumns )
        {
            grid.addHeader( new GridHeader( getColumnName( column ), getPrettyColumnName( column, displayProperty ),
                ValueType.NUMBER, Double.class.getName(), false, false ) );
        }

        // ---------------------------------------------------------------------
        // Values
        // ---------------------------------------------------------------------

        for ( List<DimensionalItemObject> row : gridRows )
        {
            grid.addRow();

            // -----------------------------------------------------------------
            // Row meta data
            // -----------------------------------------------------------------

            for ( DimensionalItemObject object : row )
            {
                grid.addValue( object.getDimensionItem() );
                grid.addValue( object.getDisplayProperty( displayProperty ) );
                grid.addValue( object.getCode() );
                grid.addValue( object.getDisplayDescription() );
            }

            if ( reportParamColumns )
            {
                grid.addValue( reportingPeriodName );
                grid.addValue( getParentOrganisationUnitName() );
                grid.addValue( isCurrentParent( row ) ? "Yes" : "No" );
            }

            // -----------------------------------------------------------------
            // Row data values
            // -----------------------------------------------------------------

            boolean hasValue = false;

            for ( List<DimensionalItemObject> column : gridColumns )
            {
                String key = getIdentifier( column, row );

                Object value = valueMap.get( key );

                grid.addValue( value );

                hasValue = hasValue || value != null;
            }

            if ( hideEmptyRows && !hasValue )
            {
                grid.removeCurrentWriteRow();
            }
            
            // TODO hide empty columns
        }
        
        if ( hideEmptyColumns )
        {
            grid.removeEmptyColumns();
        }

        if ( regression )
        {
            grid.addRegressionToGrid( startColumnIndex, numberOfColumns );
        }

        if ( cumulative )
        {
            grid.addCumulativesToGrid( startColumnIndex, numberOfColumns );
        }

        // ---------------------------------------------------------------------
        // Sort and limit
        // ---------------------------------------------------------------------

        if ( sortOrder != BaseAnalyticalObject.NONE )
        {
            grid.sortGrid( grid.getWidth(), sortOrder );
        }

        if ( topLimit > 0 )
        {
            grid.limitGrid( topLimit );
        }

        // ---------------------------------------------------------------------
        // Show hierarchy option
        // ---------------------------------------------------------------------

        if ( showHierarchy && rowDimensions.contains( ORGUNIT_DIM_ID ) && grid.hasInternalMetaDataKey( AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS.getKey() ) )
        {
            int ouIdColumnIndex = rowDimensions.indexOf( ORGUNIT_DIM_ID ) * 4;

            addHierarchyColumns( grid, ouIdColumnIndex );
        }

        return grid;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Adds grid columns for each organisation unit level.
     */
    @SuppressWarnings( "unchecked" )
    private void addHierarchyColumns( Grid grid, int ouIdColumnIndex )
    {
        Map<Object, List<?>> ancestorMap = (Map<Object, List<?>>) grid.getInternalMetaData().get( AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS.getKey() );

        Assert.notEmpty( ancestorMap, "Ancestor map cannot be null or empty when show hierarchy is enabled" );

        int newColumns = ancestorMap.values().stream().mapToInt( List::size ).max().orElseGet( () -> 0 );

        List<GridHeader> headers = new ArrayList<>();

        for ( int i = 0; i < newColumns; i++ )
        {
            int level = i + 1;

            String name = String.format( "Org unit level %d", level );
            String column = String.format( "orgunitlevel%d", level );

            headers.add( new GridHeader( name, column, ValueType.TEXT, String.class.getName(), false, true ) );
        }

        grid.addHeaders( ouIdColumnIndex, headers );
        grid.addAndPopulateColumnsBefore( ouIdColumnIndex, ancestorMap, newColumns );
    }

    /**
     * Returns the number of empty lists among the argument lists.
     */
    private static int nonEmptyLists( List<?>... lists )
    {
        int nonEmpty = 0;

        for ( List<?> list : lists )
        {
            if ( list != null && list.size() > 0 )
            {
                ++nonEmpty;
            }
        }

        return nonEmpty;
    }

    /**
     * Supportive method.
     */
    private static void verify( boolean expression, String falseMessage )
    {
        if ( !expression )
        {
            throw new IllegalStateException( falseMessage );
        }
    }

    /**
     * Returns the category combo of the first data element.
     */
    private CategoryCombo getFirstCategoryCombo()
    {
        if ( !getDataElements().isEmpty() )
        {
            return getDataElements().get( 0 ).getCategoryCombos().iterator().next();
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Get- and set-methods for persisted properties
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getMeasureCriteria()
    {
        return measureCriteria;
    }

    public void setMeasureCriteria( String measureCriteria )
    {
        this.measureCriteria = measureCriteria;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRegression()
    {
        return regression;
    }

    public void setRegression( boolean regression )
    {
        this.regression = regression;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCumulative()
    {
        return cumulative;
    }

    public void setCumulative( boolean cumulative )
    {
        this.cumulative = cumulative;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "columnDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "columnDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getColumnDimensions()
    {
        return columnDimensions;
    }

    public void setColumnDimensions( List<String> columnDimensions )
    {
        this.columnDimensions = columnDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "rowDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "rowDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getRowDimensions()
    {
        return rowDimensions;
    }

    public void setRowDimensions( List<String> rowDimensions )
    {
        this.rowDimensions = rowDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "filterDimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "filterDimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getFilterDimensions()
    {
        return filterDimensions;
    }

    public void setFilterDimensions( List<String> filterDimensions )
    {
        this.filterDimensions = filterDimensions;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ReportParams getReportParams()
    {
        return reportParams;
    }

    public void setReportParams( ReportParams reportParams )
    {
        this.reportParams = reportParams;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getSortOrder()
    {
        return sortOrder;
    }

    @Override
    public void setSortOrder( int sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getTopLimit()
    {
        return topLimit;
    }

    @Override
    public void setTopLimit( int topLimit )
    {
        this.topLimit = topLimit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRowTotals()
    {
        return rowTotals;
    }

    public void setRowTotals( boolean rowTotals )
    {
        this.rowTotals = rowTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isColTotals()
    {
        return colTotals;
    }

    public void setColTotals( boolean colTotals )
    {
        this.colTotals = colTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRowSubTotals()
    {
        return rowSubTotals;
    }

    public void setRowSubTotals( boolean rowSubTotals )
    {
        this.rowSubTotals = rowSubTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isColSubTotals()
    {
        return colSubTotals;
    }

    public void setColSubTotals( boolean colSubTotals )
    {
        this.colSubTotals = colSubTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHideEmptyRows()
    {
        return hideEmptyRows;
    }


    public void setHideEmptyRows( boolean hideEmptyRows )
    {
        this.hideEmptyRows = hideEmptyRows;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHideEmptyColumns()
    {
        return hideEmptyColumns;
    }

    public void setHideEmptyColumns( boolean hideEmptyColumns )
    {
        this.hideEmptyColumns = hideEmptyColumns;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DisplayDensity getDisplayDensity()
    {
        return displayDensity;
    }

    public void setDisplayDensity( DisplayDensity displayDensity )
    {
        this.displayDensity = displayDensity;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FontSize getFontSize()
    {
        return fontSize;
    }

    public void setFontSize( FontSize fontSize )
    {
        this.fontSize = fontSize;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendSet getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( LegendSet legendSet )
    {
        this.legendSet = legendSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendDisplayStrategy getLegendDisplayStrategy()
    {
        return legendDisplayStrategy;
    }

    public void setLegendDisplayStrategy( LegendDisplayStrategy legendDisplayStrategy )
    {
        this.legendDisplayStrategy = legendDisplayStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendDisplayStyle getLegendDisplayStyle()
    {
        return legendDisplayStyle;
    }

    public void setLegendDisplayStyle( LegendDisplayStyle legendDisplayStyle )
    {
        this.legendDisplayStyle = legendDisplayStyle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public NumberType getNumberType()
    {
        return numberType;
    }

    public void setNumberType( NumberType numberType )
    {
        this.numberType = numberType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isShowHierarchy()
    {
        return showHierarchy;
    }

    public void setShowHierarchy( boolean showHierarchy )
    {
        this.showHierarchy = showHierarchy;
    }


    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isShowDimensionLabels()
    {
        return showDimensionLabels;
    }

    public void setShowDimensionLabels( boolean showDimensionLabels )
    {
        this.showDimensionLabels = showDimensionLabels;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipRounding()
    {
        return skipRounding;
    }

    public void setSkipRounding( boolean skipRounding )
    {
        this.skipRounding = skipRounding;
    }

    // -------------------------------------------------------------------------
    // Get- and set-methods for transient properties
    // -------------------------------------------------------------------------

    @JsonIgnore
    public String getReportingPeriodName()
    {
        return reportingPeriodName;
    }

    @JsonIgnore
    public ReportTable setReportingPeriodName( String reportingPeriodName )
    {
        this.reportingPeriodName = reportingPeriodName;
        return this;
    }

    @JsonIgnore
    public List<List<DimensionalItemObject>> getGridColumns()
    {
        return gridColumns;
    }

    public ReportTable setGridColumns( List<List<DimensionalItemObject>> gridColumns )
    {
        this.gridColumns = gridColumns;
        return this;
    }

    @JsonIgnore
    public List<List<DimensionalItemObject>> getGridRows()
    {
        return gridRows;
    }

    public ReportTable setGridRows( List<List<DimensionalItemObject>> gridRows )
    {
        this.gridRows = gridRows;
        return this;
    }

    @JsonIgnore
    public String getGridTitle()
    {
        return gridTitle;
    }

    public ReportTable setGridTitle( String gridTitle )
    {
        this.gridTitle = gridTitle;
        return this;
    }
}
