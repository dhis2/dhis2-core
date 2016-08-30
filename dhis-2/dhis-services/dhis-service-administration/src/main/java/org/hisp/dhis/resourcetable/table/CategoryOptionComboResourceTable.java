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

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.resourcetable.ResourceTable;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class CategoryOptionComboResourceTable
    extends ResourceTable<DataElementCategoryOptionCombo>
{
    public CategoryOptionComboResourceTable( List<DataElementCategoryOptionCombo> objects, String columnQuote )
    {
        super( objects, columnQuote );
    }
    
    @Override
    public String getTableName()
    {
        return "_dataelementcategoryoptioncombo";
    }

    @Override
    public String getCreateTempTableStatement()
    {
        String sql = "CREATE TABLE " + getTempTableName() + " (" +
            "dataelementid INTEGER NOT NULL, " +
            "dataelementuid VARCHAR(11) NOT NULL, " +
            "categoryoptioncomboid INTEGER NOT NULL, " +
            "categoryoptioncombouid VARCHAR(11) NOT NULL)";
        
        return sql;
    }

    @Override
    public Optional<String> getPopulateTempTableStatement()
    {
        String sql = 
            "insert into " + getTempTableName() + 
            " (dataelementid, dataelementuid, categoryoptioncomboid, categoryoptioncombouid) " +
            "select de.dataelementid as dataelementid, de.uid as dataelementuid, " +
            "coc.categoryoptioncomboid as categoryoptioncomboid, coc.uid as categoryoptioncombouid " +
            "from dataelement de " +
            "join categorycombos_optioncombos cc on de.categorycomboid = cc.categorycomboid " +
            "join categoryoptioncombo coc on cc.categoryoptioncomboid = coc.categoryoptioncomboid";
        
        return Optional.of( sql );        
    }

    @Override
    public Optional<List<Object[]>> getPopulateTempTableContent()
    {
        return Optional.empty();
    }

    @Override
    public List<String> getCreateIndexStatements()
    {
        String name = "in_dataelementcategoryoptioncombo_" + getRandomSuffix();
        
        String sql = "create index " + name + " on " + getTempTableName() + "(dataelementuid, categoryoptioncombouid)";
        
        return Lists.newArrayList( sql );
    }
}
