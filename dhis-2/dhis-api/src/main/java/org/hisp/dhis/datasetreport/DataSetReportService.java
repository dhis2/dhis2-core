/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.datasetreport;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Abyot Asalefew
 * @author Lars Helge Overland
 */
public interface DataSetReportService
{
    /**
     * Generates HTML code for a custom data set report.
     *
     * @param dataSet the data set.
     * @param period the period.
     * @param orgUnit the organisation unit.
     * @param dimensions mapping between dimension identifiers and dimension
     *        option identifiers.
     * @param selectedUnitOnly indicates whether to use captured or aggregated
     *        data.
     * @return the HTML code for the custom data set report.
     */
    String getCustomDataSetReport( DataSet dataSet, Period period, OrganisationUnit orgUnit, Set<String> dimensions,
        boolean selectedUnitOnly );

    /**
     * Generates a list of Grids based on the data set sections or custom form.
     *
     * @param dataSet the data set.
     * @param period the period.
     * @param orgUnit the organisation unit.
     * @param dimensions mapping between dimension identifiers and dimension
     *        option identifiers.
     * @param selectedUnitOnly indicates whether to use captured or aggregated
     *        data.
     * @return a list of Grids.
     */
    List<Grid> getDataSetReportAsGrid( DataSet dataSet, Period period, OrganisationUnit orgUnit, Set<String> dimensions,
        boolean selectedUnitOnly );
    
    /**
     * Generates a list of Grids representing a data set report. The data elements
     * are grouped and sorted by their section in the data set.
     * 
     * @param dataSet the data set.
     * @param period the period.
     * @param unit the organisation unit.
     * @param dimensions mapping between dimension identifiers and dimension option identifiers.
     * @param selectedUnitOnly indicators whether to use captured or aggregated data. 
     * @param format the i18n format.
     * @param i18n the i18n object.
     * @return a Grid.
     */
    List<Grid> getSectionDataSetReport( DataSet dataSet, Period period, OrganisationUnit unit, Set<String> filters, boolean selectedUnitOnly );

}
