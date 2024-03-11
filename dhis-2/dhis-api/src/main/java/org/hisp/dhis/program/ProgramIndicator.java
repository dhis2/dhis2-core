package org.hisp.dhis.program;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.*;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Chau Thu Tran
 */
@JacksonXmlRootElement( localName = "programIndicator", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramIndicator
    extends BaseDataDimensionalItemObject implements MetadataObject
{
    public static final String DB_SEPARATOR_ID = "_";

    public static final String SEPARATOR_ID = "\\.";
    public static final String SEP_OBJECT = ":";
    public static final String KEY_DATAELEMENT = "#";
    public static final String KEY_ATTRIBUTE = "A";
    public static final String KEY_PROGRAM_VARIABLE = "V";
    public static final String KEY_CONSTANT = "C";

    public static final String VAR_EVENT_DATE = "event_date";
    public static final String VAR_EXECUTION_DATE = "execution_date";
    public static final String VAR_DUE_DATE = "due_date";
    public static final String VAR_ENROLLMENT_DATE = "enrollment_date";
    public static final String VAR_INCIDENT_DATE = "incident_date";
    public static final String VAR_ENROLLMENT_STATUS = "enrollment_status";
    public static final String VAR_CURRENT_DATE = "current_date";
    public static final String VAR_VALUE_COUNT = "value_count";
    public static final String VAR_ZERO_POS_VALUE_COUNT = "zero_pos_value_count";
    public static final String VAR_EVENT_COUNT = "event_count";
    public static final String VAR_ENROLLMENT_COUNT = "enrollment_count";
    public static final String VAR_TEI_COUNT = "tei_count";
    public static final String VAR_COMPLETED_DATE = "completed_date";
    public static final String VAR_PROGRAM_STAGE_NAME = "program_stage_name";
    public static final String VAR_PROGRAM_STAGE_ID = "program_stage_id";
    public static final String VAR_ANALYTICS_PERIOD_START = "analytics_period_start";
    public static final String VAR_ANALYTICS_PERIOD_END = "analytics_period_end";
    
    public static final String EXPRESSION_PREFIX_REGEXP = KEY_DATAELEMENT + "|" + KEY_ATTRIBUTE + "|" + KEY_PROGRAM_VARIABLE + "|" + KEY_CONSTANT;
    public static final String EXPRESSION_REGEXP = "(" + EXPRESSION_PREFIX_REGEXP + ")\\{([\\w\\_]+)" + SEPARATOR_ID + "?(\\w*)\\}";
    public static final String SQL_FUNC_ARG_REGEXP = " *(([\"\\w/\\*\\+\\-\\_\\:%\\.\\<\\>\\= \\#\\{\\}]+)|('[^']*'))";
    public static final String SQL_FUNC_REGEXP = "(!\\s*)*d2:(?<func>.+?)\\((?<args>" + SQL_FUNC_ARG_REGEXP + "*( *," + SQL_FUNC_ARG_REGEXP + ")* *)\\)";
    public static final String ARGS_SPLIT = ",";
    public static final String ATTRIBUTE_REGEX = KEY_ATTRIBUTE + "\\{(\\w{11})\\}";
    public static final String DATAELEMENT_REGEX = KEY_DATAELEMENT + "\\{(\\w{11})" + SEPARATOR_ID + "(\\w{11})\\}";
    public static final String VARIABLE_REGEX = KEY_PROGRAM_VARIABLE + "\\{([\\w\\_]+)}";
    public static final String PROGRAMSTAGE_DATAELEMENT_GROUP_REGEX = KEY_DATAELEMENT + "\\{(\\w{11}" + SEPARATOR_ID + "\\w{11})\\}";
    public static final String VALUECOUNT_REGEX = "V\\{(" + VAR_VALUE_COUNT + "|" + VAR_ZERO_POS_VALUE_COUNT + ")\\}";
    public static final String EQUALSEMPTY = " *== *'' *";
    public static final String EQUALSZERO = " *== *0 *";
    public static final String EXPRESSION_EQUALSZEROOREMPTY_REGEX = EXPRESSION_REGEXP + "(" + EQUALSEMPTY + "|" + EQUALSZERO + ")?";

    public static final Pattern EXPRESSION_PATTERN = Pattern.compile( EXPRESSION_REGEXP );
    public static final Pattern EXPRESSION_EQUALSZEROOREMPTY_PATTERN = Pattern.compile( EXPRESSION_EQUALSZEROOREMPTY_REGEX );
    public static final Pattern SQL_FUNC_PATTERN = Pattern.compile( SQL_FUNC_REGEXP );
    public static final Pattern DATAELEMENT_PATTERN = Pattern.compile( DATAELEMENT_REGEX );
    public static final Pattern PROGRAMSTAGE_DATAELEMENT_GROUP_PATTERN = Pattern.compile( PROGRAMSTAGE_DATAELEMENT_GROUP_REGEX );
    public static final Pattern ATTRIBUTE_PATTERN = Pattern.compile( ATTRIBUTE_REGEX );
    public static final Pattern VARIABLE_PATTERN = Pattern.compile( VARIABLE_REGEX );
    public static final Pattern VALUECOUNT_PATTERN = Pattern.compile( VALUECOUNT_REGEX );

    private static final String ANALYTICS_VARIABLE_REGEX = "V\\{analytics_period_(start|end)\\}";
    private static final Pattern ANALYTICS_VARIABLE_PATTERN = Pattern.compile( ANALYTICS_VARIABLE_REGEX );

    public static final String VALID = "valid";
    public static final String EXPRESSION_NOT_VALID = "expression_not_valid";
    public static final String INVALID_IDENTIFIERS_IN_EXPRESSION = "invalid_identifiers_in_expression";
    public static final String FILTER_NOT_EVALUATING_TO_TRUE_OR_FALSE = "filter_not_evaluating_to_true_or_false";
    public static final String UNKNOWN_VARIABLE = "unknown_variable";
    
    private static final Map<String, String> VARIABLE_COLUMNNAME_MAP = ImmutableMap.<String, String>builder().
        put( ProgramIndicator.VAR_EVENT_DATE, "executiondate" ).
        put( ProgramIndicator.VAR_EXECUTION_DATE, "executiondate" ).
        put( ProgramIndicator.VAR_DUE_DATE, "duedate" ).
        put( ProgramIndicator.VAR_ENROLLMENT_DATE, "enrollmentdate" ).
        put( ProgramIndicator.VAR_INCIDENT_DATE, "incidentdate" ).
        put( ProgramIndicator.VAR_ENROLLMENT_STATUS, "enrollmentstatus" ).
        put( ProgramIndicator.VAR_EVENT_COUNT, "psi" ).
        put( ProgramIndicator.VAR_ENROLLMENT_COUNT, "pi" ).
        put( ProgramIndicator.VAR_TEI_COUNT, "tei" ).
        put( ProgramIndicator.VAR_COMPLETED_DATE, "completeddate" ).
        put( ProgramIndicator.VAR_PROGRAM_STAGE_ID, "ps" ).
        put( ProgramIndicator.VAR_PROGRAM_STAGE_NAME, "ps" ).build();

    private static final Set<AnalyticsPeriodBoundary> DEFAULT_EVENT_TYPE_BOUNDARIES = ImmutableSet.<AnalyticsPeriodBoundary>builder().
        add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD ) ).
        add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD ) ).build();
    
    private Program program;

    private String expression;

    private String filter;

    private String formName;

    /**
     * Number of decimals to use for indicator value, null implies default.
     */
    private Integer decimals;

    private Boolean displayInForm;

    private Set<ProgramIndicatorGroup> groups = new HashSet<>();

    private AnalyticsType analyticsType = AnalyticsType.EVENT;
    
    private Set<AnalyticsPeriodBoundary> analyticsPeriodBoundaries = new HashSet<>();

    private ObjectStyle style;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramIndicator()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasFilter()
    {
        return filter != null;
    }

    public boolean hasDecimals()
    {
        return decimals != null && decimals >= 0;
    }

    public boolean hasZeroDecimals()
    {
        return decimals != null && decimals == 0;
    }

    /**
     * Returns aggregation type, if not exists returns AVERAGE.
     */
    public AggregationType getAggregationTypeFallback()
    {
        return aggregationType != null ? aggregationType : AggregationType.AVERAGE;
    }

    /**
     * Returns a set of data element and attribute identifiers part of the given
     * input expression.
     *
     * @param input the expression.
     * @return a set of UIDs.
     */
    public static Set<String> getDataElementAndAttributeIdentifiers( String input, AnalyticsType analyticsType )
    {
        if ( AnalyticsType.ENROLLMENT.equals( analyticsType ) )
        {
            Set<String> allElementsAndAttributes = RegexUtils.getMatches( ATTRIBUTE_PATTERN, input, 1 );

            Set<String> programStagesAndDataElements =
                RegexUtils.getMatches( PROGRAMSTAGE_DATAELEMENT_GROUP_PATTERN, input, 1 );
            for ( String programStageAndDataElement : programStagesAndDataElements )
            {
                allElementsAndAttributes.add( programStageAndDataElement.replace( '.', '_' ) );
            }

            return allElementsAndAttributes;
        }
        else
        {
            return Sets.union(
                RegexUtils.getMatches( DATAELEMENT_PATTERN, input, 2 ),
                RegexUtils.getMatches( ATTRIBUTE_PATTERN, input, 1 ) );
        }
    }
    
    /**
     * Returns a set of all analytics columns required for the variables used in the given expression
     *
     * @param expression the program indicator expression.
     * @return a set of column names
     */
    public static Set<String> getVariableColumnNames( String expression )
    {
        Set<String> requiredColumns = new HashSet<String>();
        
        Set<String> variables =
            RegexUtils.getMatches( VARIABLE_PATTERN, expression, 1 );
        
        for ( String variable : variables )
        {
            String columnName = getVariableColumnName( variable );
            if ( null != columnName )
            {
                requiredColumns.add( columnName );
            }
        }
       
        return requiredColumns;
    }
    
    /**
     * Returns the analytics column name associated with the program indicator variable.
     * 
     * @param var the program indicator variable name
     * @return the analytics column name, or null if there is no specific column used for the variable
     */
    public static String getVariableColumnName( String var ) 
    {
        return VARIABLE_COLUMNNAME_MAP.containsKey( var ) ? VARIABLE_COLUMNNAME_MAP.get( var ) : null;
    }

    public void addProgramIndicatorGroup( ProgramIndicatorGroup group )
    {
        groups.add( group );
        group.getMembers().add( this );
    }

    public void removeIndicatorGroup( ProgramIndicatorGroup group )
    {
        groups.remove( group );
        group.getMembers().remove( this );
    }

    public void updateIndicatorGroups( Set<ProgramIndicatorGroup> updates )
    {
        for ( ProgramIndicatorGroup group : new HashSet<>( groups ) )
        {
            if ( !updates.contains( group ) )
            {
                removeIndicatorGroup( group );
            }
        }

        for ( ProgramIndicatorGroup group : updates )
        {
            addProgramIndicatorGroup( group );
        }
    }
    
    /**
     * Indicates whether the program indicator has standard reporting period boundaries, and can use the 
     * pre-aggregated data in the analytics tables directly, or whether a custom set of boundaries is used. 
     * @return true if the program indicator uses custom boundaries that the database query will need to 
     * handle.
     */
    public Boolean hasNonDefaultBoundaries()
    {
        return this.analyticsPeriodBoundaries.size() != 2 || ( this.analyticsType == AnalyticsType.EVENT &&
            !this.analyticsPeriodBoundaries.containsAll( DEFAULT_EVENT_TYPE_BOUNDARIES ) ||
            this.analyticsType == AnalyticsType.ENROLLMENT );
    }

    /**
     * Checks if indicator expression or indicator filter expression contains V{analytics_period_end} or V{analytics_period_start}. It will be use in conjunction with hasNonDefaultBoundaries() in order to
     * split sql queries for each period provided.
     * @return true if expression has analytics period variables.
     */
    public boolean hasAnalyticsVariables()
    {
        return ANALYTICS_VARIABLE_PATTERN.matcher( StringUtils.defaultIfBlank( this.expression, "" ) ).find() ||
               ANALYTICS_VARIABLE_PATTERN.matcher( StringUtils.defaultIfBlank( this.filter, "" ) ).find();
    }

    /**
     * Indicates whether the program indicator includes event boundaries, to be applied if the program indicator queries event data.
     */
    public Boolean hasEventBoundary()
    {
        return getEndEventBoundary() != null || getStartEventBoundary() != null;
    }
    
    /**
     * Returns the boundary for the latest event date to include in the further evaluation.
     * @return The analytics period boundary that defines the event end date. Null if none is found.
     */
    public AnalyticsPeriodBoundary getEndEventBoundary()
    {
        for ( AnalyticsPeriodBoundary boundary : analyticsPeriodBoundaries )
        {
            if ( boundary.isEventDateBoundary() && boundary.getAnalyticsPeriodBoundaryType().isEndBoundary() )
            {
                return boundary;                
            }
        }

        return null;
    }
    
    /**
     * Returns the boundary for the earliest event date to include in the further evaluation.
     * @return The analytics period boundary that defines the event start date. Null if none is found.
     */
    public AnalyticsPeriodBoundary getStartEventBoundary()
    {
        for ( AnalyticsPeriodBoundary boundary : analyticsPeriodBoundaries )
        {
            if ( boundary.isEventDateBoundary() && boundary.getAnalyticsPeriodBoundaryType().isStartBoundary() )
            {
                return boundary;                
            }
        }

        return null;
    }

    /**
     * Determines wether there exists any analytics period boundaries that has type "Event in program stage".
     * @return true if any boundary exists with type  "Event in program stage"
     */
    public boolean hasEventDateCohortBoundary()
    {
        for ( AnalyticsPeriodBoundary boundary : analyticsPeriodBoundaries )
        {
            if ( boundary.isEnrollmentHavingEventDateCohortBoundary() )
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns any analytics period boundaries that has type "Event in program stage", organized as a map
     * where the program stage is the key, and the list of boundaries for that program stage is the value.
     */
    public Map<String, Set<AnalyticsPeriodBoundary>> getEventDateCohortBoundaryByProgramStage()
    {
        Map<String, Set<AnalyticsPeriodBoundary>> map = new HashMap<String, Set<AnalyticsPeriodBoundary>>();
        for ( AnalyticsPeriodBoundary boundary : analyticsPeriodBoundaries )
        {
            if ( boundary.isEnrollmentHavingEventDateCohortBoundary() )
            {
                Matcher matcher = AnalyticsPeriodBoundary.COHORT_HAVING_PROGRAM_STAGE_PATTERN.matcher( boundary.getBoundaryTarget() );
                Assert.isTrue( matcher.find(), "Can not parse program stage pattern for analyticsPeriodBoundary " + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
                String programStage = matcher.group( AnalyticsPeriodBoundary.PROGRAM_STAGE_REGEX_GROUP );
                Assert.isTrue( programStage != null, "Can not find programStage for analyticsPeriodBoundary " + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
                if ( !map.containsKey( programStage ) )
                {
                    map.put( programStage, new HashSet<AnalyticsPeriodBoundary>() );
                }
                map.get( programStage ).add( boundary );
            }
        }
        
        return map;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.PROGRAM_INDICATOR;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getExpression()
    {
        return expression;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFilter()
    {
        return filter; // Note: Also overrides DimensionalObject
    }

    public void setFilter( String filter )
    {
        this.filter = filter;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getDecimals()
    {
        return decimals;
    }

    public void setDecimals( Integer decimals )
    {
        this.decimals = decimals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDisplayInForm()
    {
        return displayInForm;
    }

    public void setDisplayInForm( Boolean displayInForm )
    {
        this.displayInForm = displayInForm;
    }

    @JsonProperty( "programIndicatorGroups" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programIndicatorGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programIndicatorGroups", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramIndicatorGroup> getGroups()
    {
        return groups;
    }

    public void setGroups( Set<ProgramIndicatorGroup> groups )
    {
        this.groups = groups;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AnalyticsType getAnalyticsType()
    {
        return analyticsType;
    }

    public void setAnalyticsType( AnalyticsType analyticsType )
    {
        this.analyticsType = analyticsType;
    }
    
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "analyticsPeriodBoundaries", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "analyticsPeriodBoundary", namespace = DxfNamespaces.DXF_2_0 )
    public Set<AnalyticsPeriodBoundary> getAnalyticsPeriodBoundaries()
    {
        return analyticsPeriodBoundaries;
    }

    public void setAnalyticsPeriodBoundaries( Set<AnalyticsPeriodBoundary> analyticsPeriodBoundaries )
    {
        this.analyticsPeriodBoundaries = analyticsPeriodBoundaries;
    }


    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ObjectStyle getStyle()
    {
        return style;
    }

    public void setStyle( ObjectStyle style )
    {
        this.style = style;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFormName()
    {
        return formName;
    }

    public void setFormName( String formName )
    {
        this.formName = formName;
    }
}
