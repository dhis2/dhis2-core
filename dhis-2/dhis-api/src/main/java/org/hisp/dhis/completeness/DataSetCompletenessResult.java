package org.hisp.dhis.completeness;

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

import org.hisp.dhis.common.AggregatedValue;

/**
 * @author Lars Helge Overland
 */
public class DataSetCompletenessResult
    extends AggregatedValue
{
    // -------------------------------------------------------------------------
    // Properties 1, inherits periodId, periodName, organisationUnitId, 
    // organisationUnitGroupId, value
    // -------------------------------------------------------------------------

    private int dataSetId;

    // -------------------------------------------------------------------------
    // Properties 2
    // -------------------------------------------------------------------------

    private String name;

    private int sources;

    private int registrations;

    private int registrationsOnTime;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public DataSetCompletenessResult()
    {
    }

    /**
     * @param name the name.
     * @param sources the number of sources.
     * @param registrations the number of registrations.
     * @param registrationsOnTime the number of registrations on time.
     */
    public DataSetCompletenessResult( String name, int sources, int registrations, int registrationsOnTime )
    {
        this.name = name;
        this.sources = sources;
        this.registrations = registrations;
        this.registrationsOnTime = registrationsOnTime;
    }

    /**
     * @param dataSetId the dataset identifier.
     * @param periodId the period identifier.
     * @param periodName the period name.
     * @param organisationUnitId the organisation unit identifier.
     * @param name the name.
     * @param sources the number of sources.
     * @param registrations the number of registrations.
     * @param registrationsOnTime the number of registrations on time.
     */
    public DataSetCompletenessResult( int dataSetId, int periodId, String periodName, int organisationUnitId,
        String name, int sources, int registrations, int registrationsOnTime )
    {
        this.dataSetId = dataSetId;
        this.periodId = periodId;
        this.periodName = periodName;
        this.organisationUnitId = organisationUnitId;
        this.name = name;
        this.sources = sources;
        this.registrations = registrations;
        this.registrationsOnTime = registrationsOnTime;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public double getPercentage()
    {
        if ( sources > 0 )
        {
            double percentage = (double) registrations / (double) sources * 100;

            return getRounded( percentage, 1 );
        }

        return 0.0;
    }

    public double getPercentageOnTime()
    {
        if ( sources > 0 )
        {
            double percentage = (double) registrationsOnTime / (double) sources * 100;

            return getRounded( percentage, 1 );
        }

        return 0.0;
    }

    @Override
    public int getElementId()
    {
        return dataSetId;
    }

    // -------------------------------------------------------------------------
    // HashCode, equals, and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;

        int result = 1;

        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + sources;
        result = prime * result + registrations;
        result = prime * result + registrationsOnTime;

        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( getClass() != object.getClass() )
        {
            return false;
        }

        final DataSetCompletenessResult other = (DataSetCompletenessResult) object;

        return name.equals( other.getName() ) && sources == other.getSources()
            && registrations == other.getRegistrations() && registrationsOnTime == other.getRegistrationsOnTime();
    }

    @Override
    public String toString()
    {
        String toString = "[Name: " + name + ", sources: " + sources+ ", registrations: " + registrations + ", on time: " + registrationsOnTime + "]";

        return toString;
    }

    // -------------------------------------------------------------------------
    // Getters and setters 1
    // -------------------------------------------------------------------------

    public int getDataSetId()
    {
        return dataSetId;
    }

    public void setDataSetId( int dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    // -------------------------------------------------------------------------
    // Getters and setters 2
    // -------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public int getSources()
    {
        return sources;
    }

    public void setSources( int sources )
    {
        this.sources = sources;
    }

    public int getRegistrations()
    {
        return registrations;
    }

    public void setRegistrations( int registrations )
    {
        this.registrations = registrations;
    }

    public int getRegistrationsOnTime()
    {
        return registrationsOnTime;
    }

    public void setRegistrationsOnTime( int registrationsOnTime )
    {
        this.registrationsOnTime = registrationsOnTime;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a number rounded off to the given number of decimals.
     * 
     * @param value the value to round off.
     * @param decimals the number of decimals.
     */
    private double getRounded( double value, int decimals )
    {
        double factor = Math.pow( 10, decimals );

        return Math.round( value * factor ) / factor;
    }
}
