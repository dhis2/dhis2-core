package org.hisp.dhis.indicator;

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

/**
 * Non-persisted class for representing the various components of an aggregated
 * indicator value.
 * 
 * @author Lars Helge Overland
 */
public class IndicatorValue
{    
    private double numeratorValue;
    
    private double denominatorValue;

    private int factor;
    
    private double annualizationFactor;
    
    public IndicatorValue()
    {
    }

    // -------------------------------------------------------------------------
    // Logic methods
    // -------------------------------------------------------------------------

    /**
     * Returns the calculated indicator value.
     */
    public double getValue()
    {
        return getNumeratorFactorValue() / denominatorValue;
    }

    /**
     * Returns the product of the indicator numerator value, factor and 
     * annualization factor.
     */
    public double getNumeratorFactorValue()
    {
        return numeratorValue * factor * annualizationFactor;
    }
    
    /**
     * Returns the product of the factor and the annualization factor.
     */
    public double getFactorAnnualizedValue()
    {
        return factor * annualizationFactor;
    }
    
    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    public double getNumeratorValue()
    {
        return numeratorValue;
    }

    public IndicatorValue setNumeratorValue( double numeratorValue )
    {
        this.numeratorValue = numeratorValue;
        return this;
    }

    public double getDenominatorValue()
    {
        return denominatorValue;
    }

    public IndicatorValue setDenominatorValue( double denominatorValue )
    {
        this.denominatorValue = denominatorValue;
        return this;
    }
    
    public int getFactor()
    {
        return factor;
    }

    public IndicatorValue setFactor( int factor )
    {
        this.factor = factor;
        return this;
    }

    public double getAnnualizationFactor()
    {
        return annualizationFactor;
    }

    public IndicatorValue setAnnualizationFactor( double annualizationFactor )
    {
        this.annualizationFactor = annualizationFactor;
        return this;
    }
}
