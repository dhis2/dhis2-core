package org.hisp.dhis.validationrule.action.dataanalysis;

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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.dataanalysis.DataAnalysisService;
import org.hisp.dhis.dataanalysis.FollowupAnalysisService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.util.SessionUtils;

import com.opensymphony.xwork2.Action;

/**
 * @author Halvdan Hoem Grelland
 */
public class GetFollowupAction
    implements Action
{
    private static final String KEY_ANALYSIS_DATA_VALUES = "analysisDataValues";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private FollowupAnalysisService followupAnalysisService;

    public void setFollowupAnalysisService( FollowupAnalysisService followupAnalysisService )
    {
        this.followupAnalysisService = followupAnalysisService;
    }

    private SelectionTreeManager selectionTreeManager;

    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<DeflatedDataValue> dataValues = new ArrayList<>();

    public List<DeflatedDataValue> getDataValues()
    {
        return dataValues;
    }

    private boolean maxExceeded = false;

    public boolean getMaxExceeded()
    {
        return maxExceeded;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute() throws Exception
    {
        OrganisationUnit orgUnit = selectionTreeManager.getReloadedSelectedOrganisationUnit();

        if ( orgUnit != null )
        {
            dataValues = followupAnalysisService.getFollowupDataValues( orgUnit, DataAnalysisService.MAX_OUTLIERS + 1 ); // +1 to detect overflow

            maxExceeded = dataValues.size() > DataAnalysisService.MAX_OUTLIERS;
        }

        SessionUtils.setSessionVar( KEY_ANALYSIS_DATA_VALUES, dataValues );

        return SUCCESS;
    }
}
