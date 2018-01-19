package org.hisp.dhis.validation;

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

import org.apache.commons.lang3.Validate;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;

import java.util.List;
import java.util.Set;

/**
 * This class holds the context for a validation run subset. A validation
 * run subset is defined by a unique list of validation rules for a given
 * period type. The subset contains all organisation units that are to be
 * evaluated for that list of rules in that period type.
 *
 * Each validation sub context therefore contains the set of rules to be
 * evaluated, the set of organisation units to evaluate those rules with,
 * and the period type for the evaluation.
 *
 * The validation sub context also stores the sets of data items that
 * need to be fetched in order to evaluate those rules, and the period
 * types in which the data may be found (equal to or greater in lenth
 * than the period type for the rules.)
 *
 * @author Jim Grace
 */
public class ValidationSubContext
{
    private List<ValidationRule> rules;
    private List<OrganisationUnit> orgUnits;
    private PeriodTypeExtended periodTypeX;
    private List<PeriodType> allowedPeriodTypes;
    private Set<DataElementOperand> dataItems;
    private Set<DimensionalItemObject> eventItems;
    private Set<DimensionalItemObject> eventItemsWithoutAttributeOptions;

    // -------------------------------------------------------------------------
    // Getter methods
    // -------------------------------------------------------------------------

    public List<ValidationRule> getRules()
    {
        return rules;
    }

    public List<OrganisationUnit> getOrgUnits()
    {
        return orgUnits;
    }

    public PeriodTypeExtended getPeriodTypeX()
    {
        return periodTypeX;
    }

    public List<PeriodType> getAllowedPeriodTypes()
    {
        return allowedPeriodTypes;
    }

    public Set<DataElementOperand> getDataItems()
    {
        return dataItems;
    }

    public Set<DimensionalItemObject> getEventItems()
    {
        return eventItems;
    }

    public Set<DimensionalItemObject> getEventItemsWithoutAttributeOptions()
    {
        return eventItemsWithoutAttributeOptions;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static ValidationSubContext.Builder newBuilder()
    {
        return new ValidationSubContext.Builder();
    }

    public static class Builder
    {
        private final ValidationSubContext sub;

        public Builder()
        {
            this.sub = new ValidationSubContext();
        }

        /**
         * Builds the actual ValidationRunContext object configured with the builder
         *
         * @return a new ValidationParam based on the builders configuration
         */
        public ValidationSubContext build()
        {
            Validate.notNull( this.sub.rules, "Missing required property 'rules'" );
            Validate.notNull( this.sub.orgUnits, "Missing required property 'orgUnits'" ); // (Initially empty.)
            Validate.notNull( this.sub.periodTypeX, "Missing required property 'periodTypeX'" );
            Validate.notEmpty( this.sub.allowedPeriodTypes, "Missing required property 'allowedPeriodTypes'" );
            Validate.notNull( this.sub.dataItems, "Missing required property 'dataItems'" );
            Validate.notNull( this.sub.eventItems, "Missing required property 'eventItems'" );
            Validate.notNull( this.sub.eventItemsWithoutAttributeOptions, "Missing required property 'eventItemsWithoutAttributeOptions'" );

            return this.sub;
        }

        // -------------------------------------------------------------------------
        // Setter methods
        // -------------------------------------------------------------------------

        public ValidationSubContext.Builder withRules(
            List<ValidationRule> rules )
        {
            this.sub.rules = rules;
            return this;
        }

        public ValidationSubContext.Builder withOrgUnits(
            List<OrganisationUnit> orgUnits )
        {
            this.sub.orgUnits = orgUnits;
            return this;
        }

        public ValidationSubContext.Builder withPeriodTypeX(
            PeriodTypeExtended periodTypeX )
        {
            this.sub.periodTypeX = periodTypeX;
            return this;
        }

        public ValidationSubContext.Builder withAllowedPeriodTypes(
            List<PeriodType> allowedPeriodTypes )
        {
            this.sub.allowedPeriodTypes = allowedPeriodTypes;
            return this;
        }

        public ValidationSubContext.Builder withDataItems(
            Set<DataElementOperand> dataItems )
        {
            this.sub.dataItems = dataItems;
            return this;
        }

        public ValidationSubContext.Builder withEventItems(
            Set<DimensionalItemObject> eventItems )
        {
            this.sub.eventItems = eventItems;
            return this;
        }

        public ValidationSubContext.Builder withEventItemsWithoutAttributeOptions(
            Set<DimensionalItemObject> eventItemsWithoutAttributeOptions )
        {
            this.sub.eventItemsWithoutAttributeOptions = eventItemsWithoutAttributeOptions;
            return this;
        }
    }
}
