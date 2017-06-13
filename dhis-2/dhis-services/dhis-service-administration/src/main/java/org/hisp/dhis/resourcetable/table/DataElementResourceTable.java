package org.hisp.dhis.resourcetable.table;

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

import static org.hisp.dhis.dataapproval.DataApprovalLevelService.APPROVAL_LEVEL_HIGHEST;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTable;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class DataElementResourceTable
    extends ResourceTable<DataElement>
{
    public DataElementResourceTable( List<DataElement> objects, String columnQuote )
    {
        super( objects, columnQuote );
    }

    @Override
    public String getTableName()
    {
        return "_dataelementstructure";
    }
    
    @Override
    public String getCreateTempTableStatement()
    {
        String sql = "CREATE TABLE " + getTempTableName() + " (" + 
            "dataelementid INTEGER NOT NULL PRIMARY KEY, " +
            "dataelementuid CHARACTER(11), " +
            "dataelementname VARCHAR(230), " +
            "datasetid INTEGER, " +
            "datasetuid CHARACTER(11), " +
            "datasetname VARCHAR(230), " +
            "datasetapprovallevel INTEGER, " +
            "workflowid INTEGER, " +
            "periodtypeid INTEGER, " + 
            "periodtypename VARCHAR(230))";
        
        return sql;
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

        for ( DataElement dataElement : objects )
        {
            List<Object> values = new ArrayList<>();

            final DataSet dataSet = dataElement.getApprovalDataSet();
            final PeriodType periodType = dataElement.getPeriodType();

            // -----------------------------------------------------------------
            // Use highest approval level if data set does not require approval,
            // or null if approval is required.
            // -----------------------------------------------------------------

            values.add( dataElement.getId() );
            values.add( dataElement.getUid() );
            values.add( dataElement.getName() );
            values.add( dataSet != null ? dataSet.getId() : null );
            values.add( dataSet != null ? dataSet.getUid() : null );
            values.add( dataSet != null ? dataSet.getName() : null );
            values.add( dataSet != null && dataSet.isApproveData() ? null : APPROVAL_LEVEL_HIGHEST );
            values.add( dataSet != null && dataSet.isApproveData() ? dataSet.getWorkflow().getId() : null );
            values.add( periodType != null ? periodType.getId() : null );
            values.add( periodType != null ? periodType.getName() : null );

            batchArgs.add( values.toArray() );
        }

        return Optional.of( batchArgs );
    }

    @Override
    public List<String> getCreateIndexStatements()
    {
        return Lists.newArrayList(
            "create unique index in_dataelementstructure_dataelementuid_" + getRandomSuffix() + " on " + getTempTableName() + "(dataelementuid);",
            "create index in_dataelementstructure_datasetid_" + getRandomSuffix() + " on " + getTempTableName() + "(datasetid);",
            "create index in_dataelementstructure_datasetuid_" + getRandomSuffix() + " on " + getTempTableName() + "(datasetuid);",
            "create index in_dataelementstructure_periodtypeid_" + getRandomSuffix() + " on " + getTempTableName() + "(periodtypeid);",
            "create index in_dataelementstructure_workflowid_" + getRandomSuffix() + " on " + getTempTableName() + "(workflowid);" );
    }
}
