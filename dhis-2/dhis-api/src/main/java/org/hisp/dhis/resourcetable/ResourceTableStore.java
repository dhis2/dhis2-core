package org.hisp.dhis.resourcetable;

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

/**
 * @author Lars Helge Overland
 */
public interface ResourceTableStore
{
    String ID = ResourceTableStore.class.getName();

    String TABLE_NAME_CATEGORY_OPTION_COMBO_NAME = "_categoryoptioncomboname";
    String TABLE_NAME_DATA_ELEMENT_STRUCTURE = "_dataelementstructure";
    String TABLE_NAME_PERIOD_STRUCTURE = "_periodstructure";
    String TABLE_NAME_DATE_PERIOD_STRUCTURE = "_dateperiodstructure";
    String TABLE_NAME_DATA_ELEMENT_CATEGORY_OPTION_COMBO = "_dataelementcategoryoptioncombo";
    String TABLE_NAME_DATA_APPROVAL_MIN_LEVEL = "_dataapprovalminlevel";
    
    /**
     * Generates the given resource table.
     * 
     * @param resourceTable the resource table.
     */
    void generateResourceTable( ResourceTable<?> resourceTable );
    
    /**
     * Performs a batch update.
     * 
     * @param columns the number of columns in the table to update.
     * @param tableName the name of the table to update.
     * @param batchArgs the arguments to use for the update statement.
     */
    void batchUpdate( int columns, String tableName, List<Object[]> batchArgs );
}
