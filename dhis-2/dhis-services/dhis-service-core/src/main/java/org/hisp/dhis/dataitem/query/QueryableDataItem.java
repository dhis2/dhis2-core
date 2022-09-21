/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dataitem.query;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;

import java.util.Set;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;

/**
 * This Enum holds the allowed data item types to be queried.
 *
 * @author maikel arabori
 */
public enum QueryableDataItem
{
    INDICATOR( Indicator.class ),
    DATA_ELEMENT( DataElement.class ),
    DATA_SET( DataSet.class ),
    PROGRAM_INDICATOR( ProgramIndicator.class ),
    PROGRAM_DATA_ELEMENT( ProgramDataElementDimensionItem.class ),
    PROGRAM_ATTRIBUTE( ProgramTrackedEntityAttributeDimensionItem.class );

    private Class<? extends BaseIdentifiableObject> entity;

    QueryableDataItem( final Class<? extends BaseIdentifiableObject> entity )
    {
        this.entity = entity;
    }

    public Class<? extends BaseIdentifiableObject> getEntity()
    {
        return this.entity;
    }

    public static Set<Class<? extends BaseIdentifiableObject>> getEntities()
    {
        return of( QueryableDataItem.values() ).map( QueryableDataItem::getEntity ).collect( toSet() );
    }
}
