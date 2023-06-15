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
package org.hisp.dhis.program;

import static org.hisp.dhis.program.Program.DEFAULT_PREFIX;
import static org.hisp.dhis.program.Program.PREFIX_KEY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.BaseDataDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;

/**
 * @author Chau Thu Tran
 */
@JacksonXmlRootElement( localName = "programIndicator", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramIndicator
    extends BaseDataDimensionalItemObject
    implements MetadataObject
{
    public static final String DB_SEPARATOR_ID = "_";

    public static final String SEPARATOR_ID = "\\.";

    public static final String KEY_DATAELEMENT = "#";

    public static final String KEY_ATTRIBUTE = "A";

    public static final String KEY_PROGRAM_VARIABLE = "V";

    public static final String KEY_CONSTANT = "C";

    public static final String VAR_ENROLLMENT_DATE = "enrollment_date";

    public static final String VAR_INCIDENT_DATE = "incident_date";

    private static final String ANALYTICS_VARIABLE_REGEX = "V\\{analytics_period_(start|end)\\}";

    private static final Pattern ANALYTICS_VARIABLE_PATTERN = Pattern.compile( ANALYTICS_VARIABLE_REGEX );

    public static final String VALID = "valid";

    public static final String EXPRESSION_NOT_VALID = "expression_not_valid";

    private static final Set<AnalyticsPeriodBoundary> DEFAULT_EVENT_TYPE_BOUNDARIES = ImmutableSet
        .<AnalyticsPeriodBoundary> builder()
        .add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD ) )
        .add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD ) )
        .build();

    private Program program;

    private String expression;

    private String filter;

    private String formName;

    private String orgUnitField;

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
     * Returns aggregation type supported by postgres or by analytics code, if
     * not exists returns AVERAGE.
     */
    public AggregationType getAggregationTypeFallback()
    {
        if ( aggregationType == null )
        {
            return AggregationType.AVERAGE;
        }

        switch ( aggregationType )
        {
        case AVERAGE_SUM_ORG_UNIT, LAST_IN_PERIOD, MAX_SUM_ORG_UNIT, MIN_SUM_ORG_UNIT:
            return AggregationType.SUM;
        case LAST_IN_PERIOD_AVERAGE_ORG_UNIT, DEFAULT:
            return AggregationType.AVERAGE;
        case FIRST, LAST, FIRST_AVERAGE_ORG_UNIT, LAST_AVERAGE_ORG_UNIT:
        default:
            return aggregationType;
        }
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

    /**
     * Indicates whether the program indicator has standard reporting period
     * boundaries, and can use the pre-aggregated data in the analytics tables
     * directly, or whether a custom set of boundaries is used.
     *
     * @return true if the program indicator uses custom boundaries that the
     *         database query will need to handle.
     */
    public Boolean hasNonDefaultBoundaries()
    {
        return this.analyticsPeriodBoundaries.size() != 2 || (this.analyticsType == AnalyticsType.EVENT &&
            !this.analyticsPeriodBoundaries.containsAll( DEFAULT_EVENT_TYPE_BOUNDARIES ) ||
            this.analyticsType == AnalyticsType.ENROLLMENT);
    }

    /**
     * Checks if indicator expression or indicator filter expression contains
     * V{analytics_period_end} or V{analytics_period_start}. It will be use in
     * conjunction with hasNonDefaultBoundaries() in order to split sql queries
     * for each period provided.
     *
     * @return true if expression has analytics period variables.
     */
    public boolean hasAnalyticsVariables()
    {
        return ANALYTICS_VARIABLE_PATTERN.matcher( StringUtils.defaultIfBlank( this.expression, "" ) ).find() ||
            ANALYTICS_VARIABLE_PATTERN.matcher( StringUtils.defaultIfBlank( this.filter, "" ) ).find();
    }

    /**
     * Indicates whether the program indicator includes event boundaries, to be
     * applied if the program indicator queries event data.
     */
    public Boolean hasEventBoundary()
    {
        return getEndEventBoundary() != null || getStartEventBoundary() != null;
    }

    /**
     * Returns the boundary for the latest event date to include in the further
     * evaluation.
     *
     * @return The analytics period boundary that defines the event end date.
     *         Null if none is found.
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
     * Returns the boundary for the earliest event date to include in the
     * further evaluation.
     *
     * @return The analytics period boundary that defines the event start date.
     *         Null if none is found.
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
     * Determines wether there exists any analytics period boundaries that has
     * type "Event in program stage".
     *
     * @return true if any boundary exists with type "Event in program stage"
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
     * Returns any analytics period boundaries that has type "Event in program
     * stage", organized as a map where the program stage is the key, and the
     * list of boundaries for that program stage is the value.
     */
    public Map<String, Set<AnalyticsPeriodBoundary>> getEventDateCohortBoundaryByProgramStage()
    {
        Map<String, Set<AnalyticsPeriodBoundary>> map = new HashMap<>();
        for ( AnalyticsPeriodBoundary boundary : analyticsPeriodBoundaries )
        {
            if ( boundary.isEnrollmentHavingEventDateCohortBoundary() )
            {
                Matcher matcher = AnalyticsPeriodBoundary.COHORT_HAVING_PROGRAM_STAGE_PATTERN
                    .matcher( boundary.getBoundaryTarget() );
                Assert.isTrue( matcher.find(), "Can not parse program stage pattern for analyticsPeriodBoundary "
                    + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
                String programStage = matcher.group( AnalyticsPeriodBoundary.PROGRAM_STAGE_REGEX_GROUP );
                Assert.isTrue( programStage != null, "Can not find programStage for analyticsPeriodBoundary "
                    + boundary.getUid() + " - boundaryTarget: " + boundary.getBoundaryTarget() );
                if ( !map.containsKey( programStage ) )
                {
                    map.put( programStage, new HashSet<>() );
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

    public static ProgramIndicator copyOf( ProgramIndicator original, Program program, Map<String, String> copyOptions )
    {
        ProgramIndicator copy = new ProgramIndicator();
        copy.setProgram( program );
        copy.setAutoFields();
        setShallowCopyValues( copy, original, copyOptions );
        return copy;
    }

    private static void setShallowCopyValues( ProgramIndicator copy, ProgramIndicator original,
        Map<String, String> copyOptions )
    {
        String prefix = copyOptions.getOrDefault( PREFIX_KEY, DEFAULT_PREFIX );
        copy.setAccess( original.getAccess() );
        copy.setAttributeValues( original.getAttributeValues() );
        copy.setAnalyticsPeriodBoundaries( ObjectUtils.copyOf( original.getAnalyticsPeriodBoundaries() ) );
        copy.setAnalyticsType( original.getAnalyticsType() );
        copy.setDecimals( original.getDecimals() );
        copy.setDescription( original.getDescription() );
        copy.setDisplayInForm( original.getDisplayInForm() );
        copy.setExpression( original.getExpression() );
        copy.setFilter( original.getFilter() );
        copy.setFormName( original.getFormName() );
        copy.setName( prefix + original.getName() );
        copy.setOrgUnitField( original.getOrgUnitField() );
        copy.setPublicAccess( original.getPublicAccess() );
        copy.setSharing( original.getSharing() );
        copy.setShortName( original.getShortName() );
        copy.setStyle( original.getStyle() );
        copy.setTranslations( original.getTranslations() );
    }
}
