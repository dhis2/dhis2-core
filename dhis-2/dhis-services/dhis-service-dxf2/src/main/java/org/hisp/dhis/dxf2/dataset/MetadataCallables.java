/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.dataset;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.system.callable.CategoryOptionComboCallable;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.dhis.system.callable.PeriodCallable;

/**
 * @author Lars Helge Overland
 */
class MetadataCallables
{
    private final IdentifiableObjectCallable<DataSet> dataSetCallable;

    private final IdentifiableObjectCallable<OrganisationUnit> orgUnitCallable;

    private final IdentifiableObjectCallable<CategoryOptionCombo> optionComboCallable;

    private final IdentifiableObjectCallable<Period> periodCallable;

    MetadataCallables( ImportConfig config, IdentifiableObjectManager idObjManager, PeriodService periodService,
        CategoryService categoryService )
    {
        dataSetCallable = new IdentifiableObjectCallable<>( idObjManager, DataSet.class, config.getDsScheme(), null );
        orgUnitCallable = new IdentifiableObjectCallable<>( idObjManager, OrganisationUnit.class, config.getOuScheme(),
            null );
        optionComboCallable = new CategoryOptionComboCallable( categoryService, config.getAocScheme(), null );
        periodCallable = new PeriodCallable( periodService, null, null );
    }

    IdentifiableObjectCallable<DataSet> getDataSetCallable()
    {
        return dataSetCallable;
    }

    IdentifiableObjectCallable<OrganisationUnit> getOrgUnitCallable()
    {
        return orgUnitCallable;
    }

    IdentifiableObjectCallable<CategoryOptionCombo> getOptionComboCallable()
    {
        return optionComboCallable;
    }

    IdentifiableObjectCallable<Period> getPeriodCallable()
    {
        return periodCallable;
    }
}
