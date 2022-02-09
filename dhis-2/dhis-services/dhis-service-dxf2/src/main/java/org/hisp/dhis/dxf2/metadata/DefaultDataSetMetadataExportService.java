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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.schema.descriptors.CategoryComboSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategorySchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorSchemaDescriptor;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;

/**
 * @author Lars Helge Overland
 */
@AllArgsConstructor
@Service( "org.hisp.dhis.dxf2.metadata.DataSetMetadataExportService" )
public class DefaultDataSetMetadataExportService
    implements DataSetMetadataExportService
{
    private static final String FIELD_CATEGORY_OPTIONS = "categoryOptions";

    private static final String FIELD_ID = "id";

    private static final String FIELD_PRESET_SIMPLE = ":simple";

    private static final Set<String> FIELDS_DATA_SETS = Set.of( "categoryCombo~pluck" );

    private static final Set<String> FIELDS_DATA_ELEMENTS = Set.of( "categoryCombo~pluck" );

    private static final Set<String> FIELDS_INDICATORS = Set.of( "explodedNumerator", "explodedDenominator" );

    private static final Set<String> FIELDS_DATAELEMENT_CAT_COMBOS = Set
        .of( "categories~pluck,categoryOptionCombos[:id,code,name,displayName]" );

    private static final Set<String> FIELDS_DATA_SET_CAT_COMBOS = Set.of( "categories~pluck" );

    private final FieldFilterService fieldFilterService;

    private final IdentifiableObjectManager idObjectManager;

    private final CategoryService categoryService;

    private final ExpressionService expressionService;

    private final CurrentUserService currentUserService;

    // TODO explode indicator expressions
    // TODO add lock exceptions
    // TODO add validation caching (ETag and If-None-Match).

    @Override
    public ObjectNode getDataSetMetadata()
    {
        User user = currentUserService.getCurrentUser();
        List<DataSet> dataSets = idObjectManager.getDataWriteAll( DataSet.class );
        Set<DataElement> dataElements = flatMapToSet( dataSets, DataSet::getDataElements );
        Set<Indicator> indicators = flatMapToSet( dataSets, DataSet::getIndicators );
        Set<CategoryCombo> dataElementCategoryCombos = flatMapToSet( dataElements, DataElement::getCategoryCombos );
        Set<CategoryCombo> dataSetCategoryCombos = mapToSet( dataSets, DataSet::getCategoryCombo );
        Set<Category> dataElementCategories = flatMapToSet( dataElementCategoryCombos, CategoryCombo::getCategories );
        Set<Category> dataSetCategories = flatMapToSet( dataSetCategoryCombos, CategoryCombo::getCategories );

        expressionService.substituteIndicatorExpressions( indicators );

        ObjectNode rootNode = fieldFilterService.createObjectNode();

        rootNode.putArray( DataSetSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( dataSets, FIELDS_DATA_SETS, DataSet.class ) );
        rootNode.putArray( DataElementSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( dataElements, FIELDS_DATA_ELEMENTS, DataElement.class ) );
        rootNode.putArray( IndicatorSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( indicators, FIELDS_INDICATORS, Indicator.class ) );
        rootNode.putArray( CategoryComboSchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( dataElementCategoryCombos, FIELDS_DATAELEMENT_CAT_COMBOS, CategoryCombo.class ) )
            .addAll( asObjectNodes( dataSetCategoryCombos, FIELDS_DATA_SET_CAT_COMBOS, CategoryCombo.class ) );
        rootNode.putArray( CategorySchemaDescriptor.PLURAL )
            .addAll( asObjectNodes( dataElementCategories, Set.of(), Category.class ) )
            .addAll( getDataSetCategories( dataSetCategories, user ) );

        return rootNode;
    }

    /**
     * Returns a list of object nodes representing the given categories. Each
     * category node has an array of category option for which the current user
     * has data write sharing access for.
     *
     * @param categories the list of {@link Category}.
     * @param user the current {@link User}.
     * @return a list of {@link ObjectNode}.
     */
    private List<ObjectNode> getDataSetCategories( Collection<Category> categories, User user )
    {
        List<ObjectNode> categoryNodes = asObjectNodes( categories, Set.of(), Category.class );

        Map<String, ObjectNode> objectNodeMap = getIdObjectNodeMap( categoryNodes );

        for ( Category category : categories )
        {
            List<CategoryOption> categoryOptions = categoryService.getDataWriteCategoryOptions( category, user );
            ObjectNode categoryNode = objectNodeMap.get( category.getUid() );
            categoryNode.putArray( FIELD_CATEGORY_OPTIONS )
                .addAll( asObjectNodes( categoryOptions, Set.of(), CategoryOption.class ) );
        }

        return categoryNodes;
    }

    /**
     * Returns the given collection of objects as an {@link ObjectNode}.
     *
     * @param <T>
     * @param objects the collection of objects.
     * @param extraFilters the set of filters to apply in addition to
     *        <code>simple</code>.
     * @param type the class type.
     * @return an {@link ObjectNode}.
     */
    private <T extends IdentifiableObject> List<ObjectNode> asObjectNodes(
        Collection<T> objects, Set<String> extraFilters, Class<T> type )
    {
        Set<String> filters = ImmutableSet.<String> builder()
            .add( FIELD_PRESET_SIMPLE ).addAll( extraFilters ).build();

        FieldFilterParams<T> fieldFilterParams = FieldFilterParams.<T> builder()
            .objects( new ArrayList<>( objects ) )
            .filters( filters )
            .skipSharing( true )
            .build();

        return fieldFilterService.toObjectNodes( fieldFilterParams );
    }

    /**
     * Returns a mapping from identifier to {@link ObjectNode} for the given
     * collection of object nodes.
     *
     * @param objectNodes the collection of object nodes.
     * @return a mapping from identifier to {@link ObjectNode}.
     */
    private Map<String, ObjectNode> getIdObjectNodeMap( Collection<ObjectNode> objectNodes )
    {
        return objectNodes.stream().collect(
            Collectors.toMap( n -> n.get( FIELD_ID ).asText(), Function.identity() ) );
    }
}
