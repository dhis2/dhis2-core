package org.hisp.dhis.dataelementhistory;

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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: DataElementHistory.java 4438 2008-01-26 16:35:24Z abyot $
 */
public class DataElementHistory
{
    private DataElement dataElement;
    
    private DataElementCategoryOptionCombo optionCombo;

    private OrganisationUnit organisationUnit;
    
    private Integer minLimit;

    private Integer maxLimit;

    private int historyLength;
    
    /**
     * Max value used to draw the history graph
     */
    private double maxHistoryValue;
    
    /**
     * The lowest entered value
     */
    private double minValue;
    
    /**
     * The highest entered value
     */
    private double maxValue = Double.NEGATIVE_INFINITY;
    
    private List<DataElementHistoryPoint> historyPoints = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Integer getMaxLimit()
    {
        return maxLimit;
    }

    public void setMaxLimit( Integer maxLimit )
    {
        this.maxLimit = maxLimit;
    }

    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public void setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
    }

    public Integer getMinLimit()
    {
        return minLimit;
    }

    public void setMinLimit( Integer minLimit )
    {
        this.minLimit = minLimit;
    }

    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }
    
    public DataElementCategoryOptionCombo getOptionCombo()
    {
    	return optionCombo;
    }
    
    public void setOptionCombo( DataElementCategoryOptionCombo optionCombo )
    {
    	this.optionCombo = optionCombo;
    }

    public int getHistoryLength()
    {
        return historyLength;
    }

    public void setHistoryLength( int historyLength )
    {
        this.historyLength = historyLength;
    }

    public List<DataElementHistoryPoint> getHistoryPoints()
    {
        return historyPoints;
    }

    public void setHistoryPoints( List<DataElementHistoryPoint> historyPoints )
    {
        this.historyPoints = historyPoints;
    }

    public double getMaxValue()
    {
        return maxValue;
    }

    public void setMaxValue( double maxValue )
    {
        this.maxValue = maxValue;
    }

    public double getMinValue()
    {       
        return minValue;
    }

    public void setMinValue( double minValue )
    {
        this.minValue = minValue;
    }

    public double getMaxHistoryValue()
    {
        return maxHistoryValue;
    }

    public void setMaxHistoryValue( double maxHistoryValue )
    {
        this.maxHistoryValue = maxHistoryValue;
    }
}
