package org.hisp.dhis.resourcetable;

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

/**
 * @author Lars Helge Overland
 */
public interface ResourceTableService
{
    String ID = ResourceTableService.class.getName();

    /**
     * Generates a resource table containing the hierarchy graph for each
     * OrganisationUnit.
     */
    void generateOrganisationUnitStructures();
    
    /**
     * Generates a resource table containing data sets and organisation units 
     * with their associated attribute option combinations.
     */
    void generateDataSetOrganisationUnitCategoryTable();
    
    /**
     * Generates a resource table containing id and a derived name for
     * all DataElementCategoryOptionCombos.
     */
    void generateCategoryOptionComboNames();
    
    /**
     * Generates a resource table for all data elements.
     */
    void generateDataElementGroupSetTable();

    /**
     * Generates a resource table for all indicators.
     */
    void generateIndicatorGroupSetTable();
    
    /**
     * Generates a resource table for all organisation units 
     */
    void generateOrganisationUnitGroupSetTable();

    /**
     * Generates a resource table for all category option combos.
     * 
     * Depends on the category option combo names table.
     */
    void generateCategoryTable();
    
    /**
     * Generates a resource table for all data elements.
     */
    void generateDataElementTable();

    /**
     * Generates a resource table for dates and associated periods.
     */
    void generateDatePeriodTable();
    
    /**
     * Generates a resource table for all periods.
     */
    void generatePeriodTable();
    
    /**
     * Generates a resource table for all data elements and relevant category
     * option combinations.
     */
    void generateDataElementCategoryOptionComboTable();
    
    /**
     * Generates a resource table for data approval aggregated to minimum level.
     */
    void generateDataApprovalMinLevelTable();
    
    /**
     * Create all SQL views.
     */
    void createAllSqlViews();
    
    /**
     * Drop all SQL views.
     */
    void dropAllSqlViews();
}
