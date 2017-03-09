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

import org.apache.commons.lang3.Validate;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.period.PeriodType;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class keeps track of a validation analysis. It contains information about the initial params of the analysis,
 * The current state of the analysis and the final results of the analysis.
 *
 * @author Stian Sandvold
 */
public class ValidationRunContext
{
    private Queue<ValidationResult> validationResults;

    private Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap;

    private Map<String, Double> constantMap;

    private Set<DimensionalItemObject> dimensionItems;

    private Set<OrganisationUnitExtended> sourceXs;

    private Set<CategoryOptionGroup> cogDimensionConstraints;

    private Set<DataElementCategoryOption> coDimensionConstraints;

    // -------------------------------------------------------------------------
    // Properties to configure analysis
    // -------------------------------------------------------------------------

    private DataElementCategoryOptionCombo attributeCombo;

    private int maxResults = 0;

    private boolean sendNotifications = false;

    public ValidationRunContext()
    {
        validationResults = new ConcurrentLinkedQueue<>();
    }

    // -------------------------------------------------------------------------
    // Getter methods
    // -------------------------------------------------------------------------

    public DataElementCategoryOptionCombo getAttributeCombo()
    {
        return attributeCombo;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public int getCountOfSourcesToValidate()
    {
        return sourceXs.size();
    }

    public Map<PeriodType, PeriodTypeExtended> getPeriodTypeExtendedMap()
    {
        return periodTypeExtendedMap;
    }

    public Set<DimensionalItemObject> getDimensionItems()
    {
        return dimensionItems;
    }

    public Set<OrganisationUnitExtended> getSourceXs()
    {
        return sourceXs;
    }

    public Map<String, Double> getConstantMap()
    {
        return constantMap;
    }

    public Set<CategoryOptionGroup> getCogDimensionConstraints()
    {
        return cogDimensionConstraints;
    }

    public Set<DataElementCategoryOption> getCoDimensionConstraints()
    {
        return coDimensionConstraints;
    }

    public boolean isSendNotifications()
    {
        return sendNotifications;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public Queue<ValidationResult> getValidationResults()
    {
        return validationResults;
    }

    public boolean isAnalysisComplete()
    {
        return validationResults.size() >= maxResults;
    }

    public static class Builder
    {
        private final ValidationRunContext context;

        public Builder()
        {
            this.context = new ValidationRunContext();
        }

        /**
         * Builds the actual ValidationRunContext object configured with the builder
         *
         * @return a new ValidationParam based on the builders configuration
         */
        public ValidationRunContext build()
        {
            Validate
                .notEmpty( this.context.periodTypeExtendedMap, "Missing required property 'periodTypeExtendedMap'" );
            Validate.notEmpty( this.context.constantMap, "Missing required property 'constantMap'" );
            Validate.notNull( this.context.dimensionItems, "Missing required property 'dimensionItems'" );
            Validate.notEmpty( this.context.sourceXs, "Missing required property 'sourceXs'" );

            return this.context;
        }

        // -------------------------------------------------------------------------
        // Setter methods
        // -------------------------------------------------------------------------

        public Builder withPeriodTypeExtendedMap(
            Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap )
        {
            this.context.periodTypeExtendedMap = periodTypeExtendedMap;
            return this;
        }

        public Builder withConstantMap( Map<String, Double> constantMap )
        {
            this.context.constantMap = constantMap;
            return this;
        }

        public Builder withDimensionItems( Set<DimensionalItemObject> dimensionItems )
        {
            this.context.dimensionItems = dimensionItems;
            return this;
        }

        public Builder withSourceXs( Set<OrganisationUnitExtended> sourceXs )
        {
            this.context.sourceXs = sourceXs;
            return this;
        }

        /**
         * This is an optional constraint to which attributeCombo we should check
         *
         * @param attributeCombo
         */
        public Builder withAttributeCombo( DataElementCategoryOptionCombo attributeCombo )
        {
            this.context.attributeCombo = attributeCombo;
            return this;
        }

        /**
         * Sets the max results to look for before concluding analysis.
         *
         * @param maxResults 0 means unlimited
         */
        public Builder withMaxResults( int maxResults )
        {
            this.context.maxResults = maxResults;
            return this;
        }

        public Builder withSendNotifications( boolean sendNotifications )
        {
            this.context.sendNotifications = sendNotifications;
            return this;
        }

        public Builder withCogDimensionConstraints(
            Set<CategoryOptionGroup> cogDimensionConstraints )
        {
            this.context.cogDimensionConstraints = cogDimensionConstraints;
            return this;

        }

        public Builder withCoDimensionConstraints(
            Set<DataElementCategoryOption> coDimensionConstraints )
        {
            this.context.coDimensionConstraints = coDimensionConstraints;
            return this;
        }
    }
}
