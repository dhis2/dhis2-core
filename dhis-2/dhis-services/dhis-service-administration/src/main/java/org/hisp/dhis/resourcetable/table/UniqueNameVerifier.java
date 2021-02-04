package org.hisp.dhis.resourcetable.table;

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

import org.hisp.dhis.common.BaseDimensionalObject;

import java.util.ArrayList;
import java.util.List;

import static org.hisp.dhis.system.util.SqlUtils.quote;

/**
 * @author Luciano Fiandesio
 */
public class UniqueNameVerifier {

    protected List<String> columnNames = new ArrayList<>();

    /**
     * Returns the short name in quotes for the given {@see BaseDimensionalObject}, ensuring
     * that the short name is unique across the list of BaseDimensionalObject this
     * class operates on
     *
     * @param baseDimensionalObject a {@see BaseDimensionalObject}
     * @return a unique, quoted short name
     */
    protected String ensureUniqueShortName( BaseDimensionalObject baseDimensionalObject )
    {
        String columnName = quote( baseDimensionalObject.getShortName()
                + (columnNames.contains( baseDimensionalObject.getShortName() ) ? columnNames.size() : "") );

        this.columnNames.add( baseDimensionalObject.getShortName() );

        return columnName;
    }

    /**
     * Returns the name in quotes, ensuring
     * that the name is unique across the list of objects this class operates on
     *
     * @param name a String
     * @return a unique, quoted name
     */
    protected String ensureUniqueName( String name )
    {
        String columnName = quote( name + (columnNames.contains( name ) ? columnNames.size() : "") );

        this.columnNames.add( name );

        return columnName;
    }
}
