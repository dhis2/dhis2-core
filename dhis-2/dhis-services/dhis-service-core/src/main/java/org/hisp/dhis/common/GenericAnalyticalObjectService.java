package org.hisp.dhis.common;

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

import java.util.List;

import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Lars Helge Overland
 */
public abstract class GenericAnalyticalObjectService<T extends AnalyticalObject>
    implements AnalyticalObjectService<T>
{
    protected abstract AnalyticalObjectStore<T> getAnalyticalObjectStore();

    public void update( T object )
    {
        getAnalyticalObjectStore().update( object );
    }

    @Override
    public List<T> getAnalyticalObjects( Indicator indicator )
    {
        return getAnalyticalObjectStore().getAnalyticalObjects( indicator );
    }

    @Override
    public List<T> getAnalyticalObjects( DataElement dataElement )
    {
        return getAnalyticalObjectStore().getAnalyticalObjects( dataElement );
    }

    @Override
    public List<T> getAnalyticalObjectsByDataDimension( DataElement dataElement )
    {
        return getAnalyticalObjectStore().getAnalyticalObjectsByDataDimension( dataElement );
    }

    @Override
    public List<T> getAnalyticalObjectsByDataDimension( TrackedEntityAttribute attribute )
    {
        return getAnalyticalObjectStore().getAnalyticalObjectsByDataDimension( attribute );
    }
    
    @Override
    public List<T> getAnalyticalObjects( DataSet dataSet )
    {
        return getAnalyticalObjectStore().getAnalyticalObjects( dataSet );
    }

    @Override
    public List<T> getAnalyticalObjects( ProgramIndicator programIndicator )
    {
        return getAnalyticalObjectStore().getAnalyticalObjects( programIndicator );
    }

    @Override
    public List<T> getAnalyticalObjects( Period period )
    {
        return getAnalyticalObjectStore().getAnalyticalObjects( period );
    }

    @Override
    public List<T> getAnalyticalObjects( OrganisationUnit organisationUnit )
    {
        return getAnalyticalObjectStore().getAnalyticalObjects( organisationUnit );
    }

    @Override
    public List<T> getAnalyticalObjects( CategoryOptionGroup categoryOptionGroup )
    {
        return getAnalyticalObjectStore().getAnalyticalObjects( categoryOptionGroup );
    }
    
    @Override
    public List<T> getAnalyticalObjects( LegendSet legendSet )
    {
        return getAnalyticalObjectStore().getAnalyticalObjects( legendSet );
    }

    @Override
    public int countAnalyticalObjects( Indicator indicator )
    {
        return getAnalyticalObjectStore().countAnalyticalObjects( indicator );
    }

    @Override
    public int countAnalyticalObjects( DataElement dataElement )
    {
        return getAnalyticalObjectStore().countAnalyticalObjects( dataElement );
    }

    @Override
    public int countAnalyticalObjects( DataSet dataSet )
    {
        return getAnalyticalObjectStore().countAnalyticalObjects( dataSet );
    }

    @Override
    public int countAnalyticalObjects( ProgramIndicator programIndicator )
    {
        return getAnalyticalObjectStore().countAnalyticalObjects( programIndicator );
    }

    @Override
    public int countAnalyticalObjects( Period period )
    {
        return getAnalyticalObjectStore().countAnalyticalObjects( period );
    }

    @Override
    public int countAnalyticalObjects( OrganisationUnit organisationUnit )
    {
        return getAnalyticalObjectStore().countAnalyticalObjects( organisationUnit );
    }

    @Override
    public int countAnalyticalObjects( CategoryOptionGroup categoryOptionGroup )
    {
        return getAnalyticalObjectStore().countAnalyticalObjects( categoryOptionGroup );
    }
}
