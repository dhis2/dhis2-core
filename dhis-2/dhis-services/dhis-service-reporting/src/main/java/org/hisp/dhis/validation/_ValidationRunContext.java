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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.DimensionItemType.*;

/**
 * This class determines how a validation analysis should be run.
 * The object is only creatable trough the attached Builder class.
 * organisationUnit, validationRules and periods are required properties for defining the set of data to run the
 * analysis over and is always set
 *
 * @author Stian Sandvold
 */
public class _ValidationRunContext
{

    // -------------------------------------------------------------------------
    // Properties required for analysis
    // -------------------------------------------------------------------------

    private Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap;

    private Map<String, Double> constantMap;

    private Set<DimensionalItemObject> dimensionItems;

    private Set<OrganisationUnitExtended> sourceXs;

    private DataElementCategoryOptionCombo attributeCombo;

    private int countOfSourcesToValidate;

    private Set<CategoryOptionGroup> cogDimensionConstraints;

    private Set<DataElementCategoryOption> coDimensionConstraints;

    private Queue<ValidationResult> validationResults;

    // -------------------------------------------------------------------------
    // Properties to configure analysis
    // -------------------------------------------------------------------------

    private int maxResults = 0;

    private _ValidationRunContext()
    {
        validationResults = new ConcurrentLinkedQueue<>();
    }

    // -------------------------------------------------------------------------
    // Methods for analysis
    // -------------------------------------------------------------------------

    /**
     * A ValidationRun has two exit criteria:
     * 1. We have found enough violations based on our criteria
     * TODO: 2. There is no more violations to find
     *
     * @return true if either exit criteria have met
     */
    public boolean isValidationRunComplete()
    {
        return (maxResults > 0 && validationResults.size() >= maxResults);
    }

    // -------------------------------------------------------------------------
    // Methods for results
    // -------------------------------------------------------------------------

    public Queue<ValidationResult> getValidationResults()
    {
        return validationResults;
    }
    // -------------------------------------------------------------------------
    // Getter methods
    // -------------------------------------------------------------------------

    public Map<PeriodType, PeriodTypeExtended> getPeriodTypeExtendedMap()
    {
        return periodTypeExtendedMap;
    }

    public Map<String, Double> getConstantMap()
    {
        return constantMap;
    }

    public Set<DimensionalItemObject> getDimensionItems()
    {
        return dimensionItems;
    }

    public Set<OrganisationUnitExtended> getSourceXs()
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

    public Set<CategoryOptionGroup> getCogDimensionConstraints()
    {
        return cogDimensionConstraints;
    }

    public Set<DataElementCategoryOption> getCoDimensionConstraints()
    {
        return coDimensionConstraints;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static class Builder
    {

        // TODO: Find a better place for this?
        private static final ImmutableSet<DimensionItemType> EVENT_DIM_ITEM_TYPES = ImmutableSet.of(
            PROGRAM_DATA_ELEMENT, PROGRAM_ATTRIBUTE, PROGRAM_INDICATOR );

        @Autowired
        private ConstantService constantService;

        @Autowired
        private DataElementCategoryService categoryService;

        @Autowired
        private ValidationRuleService validationRuleService;

        private Set<OrganisationUnit> organisationUnits;

        private Set<ValidationRule> validationRules;

        private Set<Period> periods;

        private UserCredentials runAsUser;

        private _ValidationRunContext context;

        private Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap = new HashMap<>();

        private Map<ValidationRule, ValidationRuleExtended> ruleXMap = new HashMap<>();

        private Set<OrganisationUnitExtended> sourceXs = new HashSet<>();

        /**
         * The Builder construction takes all the required parameters
         *
         * @param organisationUnits can not be null or empty
         * @param validationRules   can not be null or empty
         * @param periods           can not be null or empty
         */
        public Builder( Set<OrganisationUnit> organisationUnits, Set<ValidationRule> validationRules,
            Set<Period> periods )
        {
            Validate.notEmpty( organisationUnits, "organisationUnits can not be null or empty" );
            Validate.notEmpty( validationRules, "validationRules can not be null or empty" );
            Validate.notEmpty( periods, "periods can not be null or empty" );

            this.context = new _ValidationRunContext();

            this.organisationUnits = organisationUnits;
            this.validationRules = validationRules;
            this.periods = periods;
        }

        /**
         * Builds the actual ValidationRunContext object configured with the builder
         *
         * @return a new ValidationRunContext based on the builders configuration
         */
        public _ValidationRunContext build()
        {
            // -------------------------------------------------------------------------
            // Get Dimensions and constants
            // -------------------------------------------------------------------------
            context.constantMap = constantService.getConstantMap();

            context.dimensionItems = validationRules.stream()
                .map( rule -> validationRuleService.getDimensionalItemObjects( rule, EVENT_DIM_ITEM_TYPES ) )
                .reduce( Sets::union )
                .get();

            // -------------------------------------------------------------------------
            // Set up periodTypeExtendedMap
            // -------------------------------------------------------------------------
            periodTypeExtendedMap = new HashMap<>();

            // Add rules first so when periods are added later, periods where the PeriodType doesn't exists is skipped,
            // this way all existing periodTypeExtended has one or more rules.
            validationRules.forEach( this::addRulesToPeriodTypeExtendedMap );

            // Adding all periods which has a PeriodType that already exists in the map
            periods.forEach( this::addPeriodToPeriodTypeExtendedMap );

            // Adding allowedPeriodTypes. These are used to determine which data is relevant in the database
            periodTypeExtendedMap.values().forEach( this::getAllowedPeriodTypesForPeriodTypeExtended );

            // We run this after getting allowedPeriodTypes since it is needed to check if an organisation unit's
            // data elements should be added.
            organisationUnits.forEach( this::addOrganisationUnitsToPeriodTypExtendedMap );

            // -------------------------------------------------------------------------
            // Configure constraints
            // -------------------------------------------------------------------------

            if ( runAsUser != null )
            {
                context.cogDimensionConstraints = categoryService.getCogDimensionConstraints( runAsUser );
                context.coDimensionConstraints = categoryService.getCoDimensionConstraints( runAsUser );
            }

            // Thread management / Work planning
            context.countOfSourcesToValidate = organisationUnits.size();

            context.sourceXs = sourceXs;
            context.periodTypeExtendedMap = periodTypeExtendedMap;

            return this.context;
        }

        // -------------------------------------------------------------------------
        // Setter methods
        // -------------------------------------------------------------------------

        /**
         * This is an optional constraint to which attributeCombo we should check
         *
         * @param attributeCombo
         */
        public void setAttributeCombo( DataElementCategoryOptionCombo attributeCombo )
        {
            this.context.attributeCombo = attributeCombo;
        }

        /**
         * Sets the max results to look for before concluding analysis.
         *
         * @param maxResults
         */
        public void setMaxResults( int maxResults )
        {
            this.context.maxResults = maxResults;
        }

        /**
         * This property will add constraints to fetching data based on the user set.
         *
         * @param user the user to run analysis as
         */
        public void setRunAsUser( User user )
        {
            this.runAsUser = user.getUserCredentials();
        }

        // -------------------------------------------------------------------------
        // Builder Helper methods
        // -------------------------------------------------------------------------

        private PeriodTypeExtended getOrCreatePeriodTypeExtended( PeriodType periodType )
        {
            if ( !periodTypeExtendedMap.containsKey( periodType ) )
            {
                return periodTypeExtendedMap.put( periodType, new PeriodTypeExtended( periodType ) );
            }
            else
            {
                return periodTypeExtendedMap.get( periodType );
            }
        }

        private void addRulesToPeriodTypeExtendedMap( ValidationRule rule )
        {
            PeriodTypeExtended periodTypeExtended = getOrCreatePeriodTypeExtended( rule.getPeriodType() );

            periodTypeExtended.getRules().add( rule );
            periodTypeExtended.getDataElements().addAll( validationRuleService.getDataElements( rule ) );
        }

        private void addPeriodToPeriodTypeExtendedMap( Period period )
        {
            // We only want to add the period if the periodType already exists
            if ( periodTypeExtendedMap.containsKey( period.getPeriodType() ) )
            {
                getOrCreatePeriodTypeExtended( period.getPeriodType() ).getPeriods().add( period );
            }
        }

        private Set<PeriodType> getAllowedPeriodTypesForPeriodTypeExtended( PeriodTypeExtended periodTypeExtended )
        {
            PeriodType periodType = periodTypeExtended.getPeriodType();

            return periodTypeExtended.getDataElements().stream()
                .map( dataElement -> getAllowedPeriodTypesForDataElement( periodType, dataElement ) )
                .reduce( Sets::union )
                .get();
        }

        private Set<PeriodType> getAllowedPeriodTypesForDataElement( PeriodType periodType, DataElement dataElement )
        {
            return dataElement.getDataSets().stream()

                // We only want to allow periodTypes which has a bigger frequency.
                // DataSet.getPeriodType will return the periodType with the highest frequency if more than one exists.
                .filter( dataSet -> dataSet.getPeriodType().getFrequencyOrder() >= periodType.getFrequencyOrder() )
                .map( DataSet::getPeriodType )
                .collect( Collectors.toSet() );
        }

        private void addOrganisationUnitsToPeriodTypExtendedMap( OrganisationUnit organisationUnit )
        {
            // TODO: Remove this since it is not actually being used (all objects will be "true")
            OrganisationUnitExtended organisationUnitExtended = new OrganisationUnitExtended( organisationUnit,
                true );
            sourceXs.add( organisationUnitExtended );

            Map<PeriodType, Set<DataElement>> sourceElementsMap = organisationUnit
                .getDataElementsInDataSetsByPeriodType();

            // For each periodTypeExtended, we add an organisation unit's data elements if they conform to the
            // allowedPeriodTypes of the periodTypeExtended.
            periodTypeExtendedMap.forEach( ( periodType, periodTypeExtended ) ->
            {
                Set<PeriodType> allowedPeriodTypes = periodTypeExtended.getAllowedPeriodTypes();
                Set<DataElement> dataElements = sourceElementsMap.keySet().stream()
                    .filter( allowedPeriodTypes::contains )
                    .map( sourceElementsMap::get )
                    .reduce( Sets::union )
                    .get();

                periodTypeExtended.getSourceDataElements().put( organisationUnit, dataElements );
            } );
        }
    }
}
