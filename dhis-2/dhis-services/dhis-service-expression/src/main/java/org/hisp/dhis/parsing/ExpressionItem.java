package org.hisp.dhis.parsing;

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

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.util.Objects;

/**
 * An expression DimensionalItemObject needing a value.
 *
 * @author Jim Grace
 */

public class ExpressionItem
{
    private OrganisationUnit orgUnit;

    private Period period;

    private DimensionalItemObject dimensionalItemObject;

    private AggregationType aggregationType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ExpressionItem()
    {
    }

    public ExpressionItem( OrganisationUnit orgUnit, Period period,
        DimensionalItemObject dimensionalItemObject, AggregationType aggregationType )
    {
        this.orgUnit = orgUnit;
        this.period = period;
        this.dimensionalItemObject = dimensionalItemObject;
        this.aggregationType = aggregationType;
    }

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return Objects.hash( orgUnit, period, dimensionalItemObject, aggregationType );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        final ExpressionItem other = (ExpressionItem) obj;

        return Objects.equals( this.orgUnit, other.orgUnit )
            && Objects.equals( this.period, other.period )
            && Objects.equals( this.dimensionalItemObject, other.dimensionalItemObject )
            && Objects.equals( this.aggregationType, other.aggregationType );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public OrganisationUnit getOrgUnit()
    {
        return orgUnit;
    }

    public void setOrgUnit( OrganisationUnit orgUnit )
    {
        this.orgUnit = orgUnit;
    }

    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    public DimensionalItemObject getDimensionalItemObject()
    {
        return dimensionalItemObject;
    }

    public void setDimensionalItemObject( DimensionalItemObject dimensionalItemObject )
    {
        this.dimensionalItemObject = dimensionalItemObject;
    }

    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public void setAggregationType( AggregationType aggregationType )
    {
        this.aggregationType = aggregationType;
    }
}
