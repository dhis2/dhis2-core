/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.db.migration.v40;

import static org.hisp.dhis.db.migration.helper.UniqueValueUtils.copyUniqueValue;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Initialises the {@code shortname} column with a unique name based on the
 * {@code name} column for
 * <ul>
 * <li>{@link org.hisp.dhis.program.ProgramTrackedEntityAttributeGroup}</li>
 * <li>{@link org.hisp.dhis.dataset.DataSet}</li>
 * <li>{@link org.hisp.dhis.organisationunit.OrganisationUnitGroup}</li>
 * <li>{@link org.hisp.dhis.category.CategoryOption}</li>
 * <li>{@link org.hisp.dhis.constant.Constant}</li>
 * <li>{@link org.hisp.dhis.dataelement.DataElementGroup}</li>
 * </ul>
 *
 * @author Jan Bernitt
 */
public class V2_40_16__Unique_non_null_short_names extends BaseJavaMigration
{
    @Override
    public void migrate( Context context )
        throws Exception
    {
        copyUniqueValue( context, "program_attribute_group", "programtrackedentityattributegroupid", "name",
            "shortname", 50 );
        copyUniqueValue( context, "dataset", "datasetid", "name", "shortname", 50 );
        copyUniqueValue( context, "orgunitgroup", "orgunitgroupid", "name", "shortname", 50 );
        copyUniqueValue( context, "dataelementcategoryoption", "categoryoptionid", "name", "shortname", 50 );
        copyUniqueValue( context, "constant", "constantid", "name", "shortname", 50 );
        copyUniqueValue( context, "dataelementgroup", "dataelementgroupid", "name", "shortname", 50 );
    }
}
