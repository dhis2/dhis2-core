package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
public class PeriodTypeExtended {
	
    private PeriodType periodType;

    private Collection<Period> periods;

    private Collection<ValidationRule> rules;

    private Collection<DataElement> dataElements;

    private Collection<PeriodType> allowedPeriodTypes;

    private Map<OrganisationUnit, Collection<DataElement>> sourceDataElements;

    public PeriodTypeExtended( PeriodType periodType )
    {
    	this.periodType = periodType;
	    periods = new HashSet<>();
	    rules = new HashSet<>();
	    dataElements = new HashSet<>();
	    allowedPeriodTypes = new HashSet<>();
	    sourceDataElements = new HashMap<>();
    }

    public String toString()
    {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
            .append( "\n  periodType", periodType )
            .append( "\n  periods", (Arrays.toString( periods.toArray() )) )
            .append( "\n  rules", (Arrays.toString( rules.toArray() )) )
            .append( "\n  dataElements", (Arrays.toString( dataElements.toArray() )) )
            .append( "\n  allowedPeriodTypes", (Arrays.toString( allowedPeriodTypes.toArray() )) )
            .append( "\n  sourceDataElements", "[" + sourceDataElements.size() + "]" ).toString();
    }

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------  

    public PeriodType getPeriodType() {
		return periodType;
	}

	public Collection<Period> getPeriods() {
		return periods;
	}

	public void setPeriods(Collection<Period> periods) {
		this.periods = periods;
	}

	public Collection<ValidationRule> getRules() {
		return rules;
	}

	public void setRules(Collection<ValidationRule> rules) {
		this.rules = rules;
	}

	public Collection<DataElement> getDataElements() {
		return dataElements;
	}

	public void setDataElements(Collection<DataElement> dataElements) {
		this.dataElements = dataElements;
	}

	public Collection<PeriodType> getAllowedPeriodTypes() {
		return allowedPeriodTypes;
	}

	public void setAllowedPeriodTypes(Collection<PeriodType> allowedPeriodTypes) {
		this.allowedPeriodTypes = allowedPeriodTypes;
	}

	public Map<OrganisationUnit, Collection<DataElement>> getSourceDataElements() {
		return sourceDataElements;
	}

	public void setSourceDataElements(
			Map<OrganisationUnit, Collection<DataElement>> sourceDataElements) {
		this.sourceDataElements = sourceDataElements;
	}
}
