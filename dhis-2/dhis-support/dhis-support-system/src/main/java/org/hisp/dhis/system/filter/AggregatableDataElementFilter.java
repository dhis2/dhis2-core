package org.hisp.dhis.system.filter;

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


import com.google.common.collect.Sets;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.filter.Filter;
import org.hisp.dhis.dataelement.DataElement;

import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public class AggregatableDataElementFilter
    implements Filter<DataElement>
{
    public static final AggregatableDataElementFilter INSTANCE = new AggregatableDataElementFilter();

    private static final Set<ValueType> VALUE_TYPES = Sets.newHashSet(
        ValueType.BOOLEAN, ValueType.TRUE_ONLY, ValueType.TEXT, ValueType.LONG_TEXT, ValueType.LETTER,
        ValueType.INTEGER, ValueType.INTEGER_POSITIVE, ValueType.INTEGER_NEGATIVE, ValueType.INTEGER_ZERO_OR_POSITIVE,
        ValueType.NUMBER, ValueType.UNIT_INTERVAL, ValueType.PERCENTAGE, ValueType.COORDINATE
    );

    @Override
    public boolean retain( DataElement object )
    {
        return object != null && VALUE_TYPES.contains( object.getValueType() );
    }
}
