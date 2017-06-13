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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;

/**
 * Holds information for each validation rule that is needed during a validation
 * run (either interactive or a scheduled run).
 * 
 * By computing these values once at the start of a validation run, we avoid the
 * overhead of having to compute them during the processing of every
 * organisation unit. For some of these properties this is also important
 * because they should be copied from Hibernate lazy collections before the
 * multithreaded part of the run starts, otherwise the threads may not be able
 * to access these values.
 * 
 * @author Jim Grace
 */
public class ValidationRuleExtended
{
    private ValidationRule rule;

    private Set<DimensionalItemObject> dimensionalItemObjects = new HashSet<>();

    private Set<DataElementOperand> dataElementOperands = new HashSet<>();

    private Set<DataElement> dataElements = new HashSet<>();

    public ValidationRuleExtended( ValidationRule rule )
    {
        this.rule = rule;
    }

    public String toString()
    {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
            .append( "rule", rule )
            .append( "dimensionalItemObjects", dimensionalItemObjects )
            .append( "dataElementOperands", dataElementOperands )
            .append( "dataElements", dataElements ).toString();
    }

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------

    public ValidationRule getRule()
    {
        return rule;
    }

    public Set<DimensionalItemObject> getDimensionalItemObjects()
    {
        return dimensionalItemObjects;
    }

    public void setDimensionalItemObjects( Set<DimensionalItemObject> dimensionalItemObjects )
    {
        this.dimensionalItemObjects = dimensionalItemObjects;
    }

    public Set<DataElementOperand> getDataElementOperands()
    {
        return dataElementOperands;
    }

    public void setDataElementOperands( Set<DataElementOperand> dataElementOperands )
    {
        this.dataElementOperands = dataElementOperands;
    }

    public Set<DataElement> getDataElements()
    {
        return dataElements;
    }

    public void setDataElements( Set<DataElement> dataElements )
    {
        this.dataElements = dataElements;
    }
}
