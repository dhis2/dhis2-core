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
package org.hisp.dhis.dxf2.metadata;

import static org.hisp.dhis.commons.collection.CollectionUtils.flatMapToSet;
import static org.hisp.dhis.commons.collection.CollectionUtils.mapToSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
@AllArgsConstructor
@Service( "org.hisp.dhis.dxf2.metadata.MetadataExportService" )
public class DefaultDataSetMetadataExportService
{
    private final SchemaService schemaService;

    private final QueryService queryService;

    private final FieldFilterService fieldFilterService;

    private final CurrentUserService currentUserService;

    private final ProgramRuleService programRuleService;

    private final ProgramRuleVariableService programRuleVariableService;

    private final SystemService systemService;

    private final AttributeService attributeService;

    private final IdentifiableObjectManager idObjectManager;

    /**
     * Retrieves metadata for data sets for which the current has data write
     * access, and other relevant entities such as data elements, category
     * combinations, categories and category options.
     *
     * @return
     */
    public ObjectNode getDataSetMetadata()
    {
        Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata = new HashMap<>();

        List<DataSet> dataSets = idObjectManager.getDataWriteAll( DataSet.class );

        Set<DataElement> dataElements = flatMapToSet( dataSets, DataSet::getDataElements );
        Set<CategoryCombo> categoryCombos = flatMapToSet( dataElements, DataElement::getCategoryCombos );
        categoryCombos.addAll( mapToSet( dataSets, DataSet::getCategoryCombo ) );
        Set<Category> categories = flatMapToSet( categoryCombos, CategoryCombo::getCategories );
        Set<CategoryOption> categoryOptions = flatMapToSet( categories, Category::getCategoryOptions );

        return null;
    }
}
