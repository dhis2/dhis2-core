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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * Holds information for each period type that is needed during
 * a validation run (either interactive or a scheduled run).
 * 
 * By computing these values once at the start of a validation run, we avoid
 * the overhead of having to compute them during the processing of every
 * organisation unit. For some of these properties this is also important
 * because they should be copied from Hibernate lazy collections before the
 * multithreaded part of the run starts, otherwise the threads may not be
 * able to access these values.
 * 
 * @author Jim Grace
 */
public class PeriodTypeExtended
{	
    private PeriodType periodType;

    private Set<Period> periods = new HashSet<>();

    private Set<ValidationRuleExtended> ruleXs = new HashSet<>();

    private Set<DataElement> dataElements = new HashSet<>();

    private Set<PeriodType> allowedPeriodTypes = new HashSet<>();

    private Map<OrganisationUnit, Set<DataElement>> orgUnitDataElements = new HashMap<>();

    public PeriodTypeExtended( PeriodType periodType )
    {
    	this.periodType = periodType;
    }

    public String toString()
    {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
            .append( "periodType", periodType )
            .append( "periods", periods )
            .append( "rules", ruleXs.toArray() )
            .append( "dataElements", dataElements )
            .append( "allowedPeriodTypes", allowedPeriodTypes )
            .append( "orgUnitDataElements", orgUnitDataElements.size() ).toString();
    }

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------  

    public PeriodType getPeriodType()
    {
        return periodType;
    }

    public Set<Period> getPeriods()
    {
        return periods;
    }

    public void setPeriods( Set<Period> periods )
    {
        this.periods = periods;
    }

    public Set<ValidationRuleExtended> getRuleXs()
    {
        return ruleXs;
    }

    public void setRules( Set<ValidationRuleExtended> ruleXs )
    {
        this.ruleXs = ruleXs;
    }

    public Set<DataElement> getDataElements()
    {
        return dataElements;
    }

    public void setDataElements( Set<DataElement> dataElements )
    {
        this.dataElements = dataElements;
    }

    public Set<PeriodType> getAllowedPeriodTypes()
    {
        return allowedPeriodTypes;
    }

    public void setAllowedPeriodTypes( Set<PeriodType> allowedPeriodTypes )
    {
        this.allowedPeriodTypes = allowedPeriodTypes;
    }

    public Map<OrganisationUnit, Set<DataElement>> getOrgUnitDataElements()
    {
        return orgUnitDataElements;
    }

    public void setOrgUnitDataElements( Map<OrganisationUnit, Set<DataElement>> orgUnitDataElements )
    {
        this.orgUnitDataElements = orgUnitDataElements;
    }
}
