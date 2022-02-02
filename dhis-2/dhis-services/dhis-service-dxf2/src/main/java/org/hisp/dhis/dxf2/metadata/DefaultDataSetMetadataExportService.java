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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPreset;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.schema.descriptors.CategoryComboSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategorySchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorSchemaDescriptor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Lars Helge Overland
 */
@AllArgsConstructor
@Service( "org.hisp.dhis.dxf2.metadata.DataSetMetadataExportService" )
public class DefaultDataSetMetadataExportService
    implements DataSetMetadataExportService
{
    private final FieldFilterService fieldFilterService;

    private final IdentifiableObjectManager idObjectManager;

    @Override
    public ObjectNode getDataSetMetadata()
    {
        List<DataSet> dataSets = idObjectManager.getDataWriteAll( DataSet.class );
        Set<DataElement> dataElements = flatMapToSet( dataSets, DataSet::getDataElements );
        Set<Indicator> indicators = flatMapToSet( dataSets, DataSet::getIndicators );
        Set<CategoryCombo> categoryCombos = flatMapToSet( dataElements, DataElement::getCategoryCombos );
        categoryCombos.addAll( mapToSet( dataSets, DataSet::getCategoryCombo ) );
        Set<Category> categories = flatMapToSet( categoryCombos, CategoryCombo::getCategories );
        Set<CategoryOption> categoryOptions = flatMapToSet( categories, Category::getCategoryOptions );

        ObjectNode rootNode = fieldFilterService.createObjectNode();

        rootNode.putArray( DataSetSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( dataSets, DataSet.class ) );
        rootNode.putArray( DataElementSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( dataElements, DataElement.class ) );
        rootNode.putArray( IndicatorSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( indicators, Indicator.class ) );
        rootNode.putArray( CategoryComboSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( categoryCombos, CategoryCombo.class ) );
        rootNode.putArray( CategorySchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( categories, Category.class ) );
        rootNode.putArray( CategoryOptionSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( categoryOptions, CategoryOption.class ) );

        return rootNode;
    }

    /**
     * Returns the given collection of objects as an {@link ObjectNode}.
     *
     * @param <T>
     * @param objects the collection of objects.
     * @return an {@link ObjectNode}.
     */
    private <T extends IdentifiableObject> List<ObjectNode> asObjectNodes(
        Collection<T> objects, Class<T> type )
    {
        FieldFilterParams<T> fieldFilterParams = FieldFilterParams.<T> builder()
            .objects( new ArrayList<>( objects ) )
            .filters( Set.of( ":" + FieldPreset.SIMPLE ) )
            .skipSharing( true )
            .build();

        return fieldFilterService.toObjectNodes( fieldFilterParams );
    }
}
