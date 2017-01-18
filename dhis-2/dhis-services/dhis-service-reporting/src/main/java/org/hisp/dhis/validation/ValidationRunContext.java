package org.hisp.dhis.validation;

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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Holds common values that are used during a validation run (either interactive
 * or scheduled.) These values don't change during the multi-threaded tasks
 * (except that results entries are added in a threadsafe way.)
 * <p>
 * Some of the values are precalculated collections, to save CPU time during the
 * run. All of these values are stored in this single "context" object to allow
 * a single object reference for each of the scheduled tasks. (This also reduces
 * the amount of memory needed to queue all the multi-threaded tasks.)
 * <p>
 * For some of these properties this is also important because they should be
 * copied from Hibernate lazy collections before the multithreaded part of the
 * run starts, otherwise the threads may not be able to access these values.
 *
 * @author Jim Grace
 */
public class ValidationRunContext
{
    private Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap;

    private ValidationRunType runType;

    private Date lastScheduledRun;

    private Map<String, Double> constantMap;
    
    private Map<ValidationRule, Set<DataElement>> validationRuleDataElementsMap;
    
    private Set<DimensionalItemObject> dimensionItems;

    private Map<ValidationRule, ValidationRuleExtended> ruleXMap;

    private Set<OrganisationUnitExtended> sourceXs;

    private DataElementCategoryOptionCombo attributeCombo;

    private int countOfSourcesToValidate;

    private Set<CategoryOptionGroup> cogDimensionConstraints;

    private Set<DataElementCategoryOption> coDimensionConstraints;

    private Queue<ValidationResult> validationResults;

    private ValidationRunContext()
    {
    }

    /**
     * Creates and fills a new context object for a validation run.
     *
     * @param sources organisation units for validation
     * @param periods periods for validation
     * @param rules validation rules for validation
     * @param attributeCombo the attribute combo to check (if restricted)
     * @param lastScheduledRun (for SCHEDULED runs) date/time of previous run
     * @param runType whether this is an INTERACTIVE or SCHEDULED run
     * @param constantMap map of constants
     * @param validationRuleDataElementsMap map of validation rule to data elements
     * @param validationRuleDimensionItemObjectsMap map of validation rule to dimension item objects
     * @param cogDimensionConstraints category option group dimension constraints.
     * @param coDimensionConstraints category option dimension constraints.

     * @return context object for this run
     */
    public static ValidationRunContext getNewContext( Collection<OrganisationUnit> sources,
        Collection<Period> periods, Collection<ValidationRule> rules, DataElementCategoryOptionCombo attributeCombo,
        Date lastScheduledRun, ValidationRunType runType, Map<String, Double> constantMap, 
        Map<ValidationRule, Set<DataElement>> validationRuleDataElementsMap,
        Set<DimensionalItemObject> dimensionItems,
        Set<CategoryOptionGroup> cogDimensionConstraints, 
        Set<DataElementCategoryOption> coDimensionConstraints )
    {
        ValidationRunContext context = new ValidationRunContext();
        context.validationResults = new ConcurrentLinkedQueue<>(); // thread-safe
        context.periodTypeExtendedMap = new HashMap<>();
        context.ruleXMap = new HashMap<>();
        context.sourceXs = new HashSet<>();
        context.attributeCombo = attributeCombo;
        context.lastScheduledRun = lastScheduledRun;
        context.runType = runType;
        context.constantMap = constantMap;
        context.validationRuleDataElementsMap = validationRuleDataElementsMap;
        context.dimensionItems = dimensionItems;
        context.cogDimensionConstraints = cogDimensionConstraints;
        context.coDimensionConstraints = coDimensionConstraints;
        context.initialize( sources, periods, rules );

        return context;
    }

    /**
     * Initializes context values based on sources, periods and rules
     *
     * @param sources organisation units to evaluate for rules
     * @param periods periods for validation
     * @param rules   validation rules for validation
     */
    private void initialize( Collection<OrganisationUnit> sources, Collection<Period> periods,
        Collection<ValidationRule> rules )
    {
        addPeriodsToContext( periods );

        addRulesToContext( rules );

        removeAnyUnneededPeriodTypes();

        addSourcesToContext( sources, true );

        countOfSourcesToValidate = sources.size();
    }

    /**
     * Adds Periods to the context, grouped by period type.
     *
     * @param periods Periods to group and add
     */
    private void addPeriodsToContext( Collection<Period> periods )
    {
        for ( Period period : periods )
        {
            PeriodTypeExtended periodTypeX = getOrCreatePeriodTypeExtended( period.getPeriodType() );
            periodTypeX.getPeriods().add( period );
        }
    }

    /**
     * Adds validation rules to the context.
     *
     * @param rules validation rules to add.
     */
    private void addRulesToContext( Collection<ValidationRule> rules )
    {
        for ( ValidationRule rule : rules )
        {
            Set<DataElement> dataElements = validationRuleDataElementsMap.get( rule );
            
            // Find the period type extended for this rule
            PeriodTypeExtended periodTypeX = getOrCreatePeriodTypeExtended( rule.getPeriodType() );
            periodTypeX.getRules().add( rule );

             // Add data elements of rule to the period extended
            periodTypeX.getDataElements().addAll( dataElements );

            // Add the allowed period types for data elements of rule
            periodTypeX.getAllowedPeriodTypes().addAll(
                getAllowedPeriodTypesForDataElements( dataElements, rule.getPeriodType() ) );

            // Add the ValidationRuleExtended
            Set<PeriodType> allowedPastPeriodTypes = getAllowedPeriodTypesForDataElements( dataElements, rule.getPeriodType() );
            
            ValidationRuleExtended ruleX = new ValidationRuleExtended( rule, allowedPastPeriodTypes );
            ruleXMap.put( rule, ruleX );
        }
    }

    /**
     * Removes any period types that don't have rules assigned to them.
     */
    private void removeAnyUnneededPeriodTypes()
    {
        Set<PeriodTypeExtended> periodTypeXs = new HashSet<>( periodTypeExtendedMap.values() );

        for ( PeriodTypeExtended periodTypeX : periodTypeXs )
        {
            if ( periodTypeX.getRules().isEmpty() )
            {
                periodTypeExtendedMap.remove( periodTypeX.getPeriodType() );
            }
        }
    }

    /**
     * Adds a collection of organisation units to the validation run context.
     *
     * @param sources organisation units to add
     * @param ruleCheckThisSource true if these organisation units should be
     *        evaluated with validation rules, false if not. (This is false when
     *        adding descendants of organisation units for the purpose of getting
     *        aggregated expression values from descendants, but these organisation
     *        units are not in the main list to be evaluated.)
     */
    private void addSourcesToContext( Collection<OrganisationUnit> sources, boolean ruleCheckThisSource )
    {
        for ( OrganisationUnit source : sources )
        {
            OrganisationUnitExtended sourceX = new OrganisationUnitExtended( source, ruleCheckThisSource );
            sourceXs.add( sourceX );

            Map<PeriodType, Set<DataElement>> sourceElementsMap = source.getDataElementsInDataSetsByPeriodType();

            for ( PeriodTypeExtended periodTypeX : periodTypeExtendedMap.values() )
            {
                periodTypeX.getSourceDataElements().put( source, new HashSet<>() );

                for ( PeriodType allowedType : periodTypeX.getAllowedPeriodTypes() )
                {
                    Set<DataElement> sourceDataElements = sourceElementsMap.get( allowedType );

                    if ( sourceDataElements != null )
                    {
                        periodTypeX.getSourceDataElements().get( source ).addAll( sourceDataElements );
                    }
                }
            }
        }
    }

    /**
     * Gets the PeriodTypeExtended from the context object. If not found,
     * creates a new PeriodTypeExtended object, puts it into the context object,
     * and returns it.
     *
     * @param periodType period type to search for
     * @return period type extended from the context object
     */
    private PeriodTypeExtended getOrCreatePeriodTypeExtended( PeriodType periodType )
    {
        PeriodTypeExtended periodTypeX = periodTypeExtendedMap.get( periodType );

        if ( periodTypeX == null )
        {
            periodTypeX = new PeriodTypeExtended( periodType );
            periodTypeExtendedMap.put( periodType, periodTypeX );
        }

        return periodTypeX;
    }

    /**
     * Finds all period types that may contain given data elements, whose period
     * type interval is at least as long as the given period type.
     *
     * @param dataElements data elements to look for
     * @param periodType   the minimum-length period type
     * @return all period types that are allowed for these data elements
     */
    private static Set<PeriodType> getAllowedPeriodTypesForDataElements( Collection<DataElement> dataElements,
        PeriodType periodType )
    {
        Set<PeriodType> allowedPeriodTypes = new HashSet<>();

        if ( dataElements != null )
        {
            for ( DataElement dataElement : dataElements )
            {
                for ( DataSet dataSet : dataElement.getDataSets() )
                {
                    if ( dataSet.getPeriodType().getFrequencyOrder() >= periodType.getFrequencyOrder() )
                    {
                        allowedPeriodTypes.add( dataSet.getPeriodType() );
                    }
                }
            }
        }

        return allowedPeriodTypes;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
            .append( "periodTypeExtendedMap", periodTypeExtendedMap )
            .append( "runType", runType )
            .append( "lastScheduledRun", lastScheduledRun )
            .append( "constantMap", constantMap.size() )
            .append( "ruleXMap", ruleXMap.size() )
            .append( "sourceXs", sourceXs )
            .append( "validationResults", validationResults ).toString();
    }

    // -------------------------------------------------------------------------
    // Get methods
    // -------------------------------------------------------------------------

    public Map<PeriodType, PeriodTypeExtended> getPeriodTypeExtendedMap()
    {
        return periodTypeExtendedMap;
    }

    public ValidationRunType getRunType()
    {
        return runType;
    }

    public void setRunType( ValidationRunType runType )
    {
        this.runType = runType;
    }

    public Date getLastScheduledRun()
    {
        return lastScheduledRun;
    }

    public Map<String, Double> getConstantMap()
    {
        return constantMap;
    }

    public Set<DimensionalItemObject> getDimensionItems()
    {
        return dimensionItems;
    }

    public Map<ValidationRule, ValidationRuleExtended> getRuleXMap()
    {
        return ruleXMap;
    }

    public Collection<OrganisationUnitExtended> getSourceXs()
    {
        return sourceXs;
    }

    public DataElementCategoryOptionCombo getAttributeCombo()
    {
        return attributeCombo;
    }

    public int getCountOfSourcesToValidate()
    {
        return countOfSourcesToValidate;
    }

    public Collection<ValidationResult> getValidationResults()
    {
        return validationResults;
    }

    public Set<CategoryOptionGroup> getCogDimensionConstraints()
    {
        return cogDimensionConstraints;
    }

    public Set<DataElementCategoryOption> getCoDimensionConstraints()
    {
        return coDimensionConstraints;
    }
}
