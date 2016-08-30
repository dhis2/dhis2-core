package org.hisp.dhis.resourcetable.table;

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
import java.util.Optional;

import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.resourcetable.ResourceTable;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class CategoryOptionGroupSetResourceTable
    extends ResourceTable<CategoryOptionGroupSet>
{
    private List<DataElementCategoryOptionCombo> categoryOptionCombos;
    
    public CategoryOptionGroupSetResourceTable( List<CategoryOptionGroupSet> objects, 
        String columnQuote, List<DataElementCategoryOptionCombo> categoryOptionCombos )
    {
        super( objects, columnQuote );
        this.categoryOptionCombos = categoryOptionCombos;
    }

    @Override
    public String getTableName()
    {
        return "_categoryoptiongroupsetstructure";
    }
    
    @Override
    public String getCreateTempTableStatement()
    {
        String statement = "create table " + getTempTableName() + " (" +
            "categoryoptioncomboid integer not null, ";
        
        for ( CategoryOptionGroupSet groupSet : objects )
        {
            statement += columnQuote + groupSet.getName() + columnQuote + " varchar(230), ";
            statement += columnQuote + groupSet.getUid() + columnQuote + " character(11), ";
        }
        
        statement += "primary key (categoryoptioncomboid))";
        
        return statement;
    }

    @Override
    public Optional<String> getPopulateTempTableStatement()
    {
        return Optional.empty();
    }

    @Override
    public Optional<List<Object[]>> getPopulateTempTableContent()
    {
        List<Object[]> batchArgs = new ArrayList<>();

        for ( DataElementCategoryOptionCombo categoryOptionCombo : categoryOptionCombos )
        {
            List<Object> values = new ArrayList<>();

            values.add( categoryOptionCombo.getId() );

            for ( CategoryOptionGroupSet groupSet : objects )
            {
                CategoryOptionGroup group = groupSet.getGroup( categoryOptionCombo );

                values.add( group != null ? group.getName() : null );
                values.add( group != null ? group.getUid() : null );
            }

            batchArgs.add( values.toArray() );
        }

        return Optional.of( batchArgs );
    }

    @Override
    public List<String> getCreateIndexStatements()
    {
        return Lists.newArrayList();
    }
}
