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

import static org.hisp.dhis.common.IdentifiableObjectUtils.sortById;
import static org.hisp.dhis.commons.collection.CollectionUtils.addIfNotNull;
import static org.hisp.dhis.commons.collection.CollectionUtils.flatMapToSet;
import static org.hisp.dhis.commons.collection.CollectionUtils.mapToSet;
import static org.hisp.dhis.commons.collection.ListUtils.union;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.descriptors.CategoryComboSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategorySchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OptionSetSchemaDescriptor;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.dxf2.metadata.DataSetMetadataExportService" )
public class DefaultDataSetMetadataExportService
    implements DataSetMetadataExportService
{
    private static final List<Class<? extends IdentifiableObject>> METADATA_TYPES = List.of(
        DataSet.class, DataElement.class, Indicator.class, CategoryCombo.class,
        Category.class, CategoryOption.class, OptionSet.class );

    private static final String PROPERTY_ORGANISATION_UNITS = "organisationUnits";

    private static final String PROPERTY_DATA_SET_ELEMENTS = "dataSetElements";

    private static final String FIELDS_DATA_SETS = ":simple,categoryCombo[id],formType,dataEntryForm[id]," +
        "dataInputPeriods[period,openingDate,closingDate]," +
        "indicators~pluck[id]," +
        "compulsoryDataElementOperands[dataElement[id],categoryOptionCombo[id]]," +
        "sections[:simple,dataElements~pluck[id],indicators~pluck[id]," +
        "greyedFields[dataElement[id],categoryOptionCombo[id]]]";

    private static final String FIELDS_DATA_SET_ELEMENTS = "dataElement[id],categoryCombo[id]";

    private static final String FIELDS_DATA_ELEMENTS = ":identifiable,displayName,displayShortName,displayFormName," +
        "zeroIsSignificant,valueType,aggregationType,categoryCombo[id],optionSet[id],commentOptionSet,description";

    private static final String FIELDS_INDICATORS = ":simple,explodedNumerator,explodedDenominator,indicatorType[factor]";

    private static final String FIELDS_DATA_ELEMENT_CAT_COMBOS = ":simple,isDefault,categories~pluck[id]," +
        "categoryOptionCombos[id,code,name,displayName,categoryOptions~pluck[id]]";

    private static final String FIELDS_DATA_SET_CAT_COMBOS = ":simple,isDefault,categories~pluck[id]";

    private static final String FIELDS_CATEGORIES = ":simple,categoryOptions~pluck[id]";

    private static final String FIELDS_CATEGORY_OPTIONS = ":simple,organisationUnits~pluck[id]";

    private static final String FIELDS_OPTION_SETS = ":simple,options[id,code,displayName]";

    private final FieldFilterService fieldFilterService;

    private final IdentifiableObjectManager idObjectManager;

    private final CategoryService categoryService;

    private final DataSetService dataSetService;

    private final ExpressionService expressionService;

    private final CurrentUserService currentUserService;

    @Override
    public ObjectNode getDataSetMetadata()
    {
        User user = currentUserService.getCurrentUser();
        CategoryCombo defaultCategoryCombo = categoryService.getDefaultCategoryCombo();
        SetValuedMap<String, String> dataSetOrgUnits = dataSetService.getDataSetOrganisationUnitsAssociations();

        List<DataSet> dataSets = idObjectManager
            .getDataWriteAll( DataSet.class );
        List<DataElement> dataElements = sortById(
            flatMapToSet( dataSets, DataSet::getDataElements ) );
        List<Indicator> indicators = sortById(
            flatMapToSet( dataSets, DataSet::getIndicators ) );
        List<CategoryCombo> dataElementCategoryCombos = sortById(
            flatMapToSet( dataElements, DataElement::getCategoryCombos ) );
        List<CategoryCombo> dataSetCategoryCombos = sortById(
            mapToSet( dataSets, DataSet::getCategoryCombo ) );
        List<Category> dataElementCategories = sortById(
            flatMapToSet( dataElementCategoryCombos, CategoryCombo::getCategories ) );
        List<Category> dataSetCategories = sortById(
            flatMapToSet( dataSetCategoryCombos, CategoryCombo::getCategories ) );
        List<Category> categories = union(
            dataElementCategories, dataSetCategories );
        List<CategoryOption> categoryOptions = sortById(
            getCategoryOptions( dataElementCategories, dataSetCategories, user ) );
        List<OptionSet> optionSets = sortById(
            getOptionSets( dataElements ) );

        dataSetCategoryCombos.remove( defaultCategoryCombo );
        expressionService.substituteIndicatorExpressions( indicators );

        ObjectNode rootNode = fieldFilterService.createObjectNode();

        rootNode.putArray( DataSetSchemaDescriptor.PLURAL )
            .addAll( toDataSetObjectNodes( dataSets, dataSetOrgUnits ) );
        rootNode.putArray( DataElementSchemaDescriptor.PLURAL )
            .addAll( toObjectNodes( dataElements, FIELDS_DATA_ELEMENTS, DataElement.class ) );
        rootNode.putArray( IndicatorSchemaDescriptor.PLURAL )
            .addAll( toObjectNodes( indicators, FIELDS_INDICATORS, Indicator.class ) );
        rootNode.putArray( CategoryComboSchemaDescriptor.PLURAL )
            .addAll( toObjectNodes( dataElementCategoryCombos, FIELDS_DATA_ELEMENT_CAT_COMBOS, CategoryCombo.class ) )
            .addAll( toObjectNodes( dataSetCategoryCombos, FIELDS_DATA_SET_CAT_COMBOS, CategoryCombo.class ) );
        rootNode.putArray( CategorySchemaDescriptor.PLURAL )
            .addAll( toObjectNodes( categories, FIELDS_CATEGORIES, Category.class ) );
        rootNode.putArray( CategoryOptionSchemaDescriptor.PLURAL )
            .addAll( toObjectNodes( categoryOptions, FIELDS_CATEGORY_OPTIONS, CategoryOption.class ) );
        rootNode.putArray( OptionSetSchemaDescriptor.PLURAL )
            .addAll( toObjectNodes( optionSets, FIELDS_OPTION_SETS, OptionSet.class ) );

        return rootNode;
    }

    @Override
    public Date getDataSetMetadataLastModified()
    {
        return ObjectUtils.firstNonNull( DateUtils.max( METADATA_TYPES.stream()
            .map( idObjectManager::getLastUpdated )
            .collect( Collectors.toList() ) ), new Date() );
    }

    /**
     * Returns category options for the given data element and data set
     * categories. For the data set categories, only category options which the
     * current user has data write access to are returned.
     *
     * @param dataElementCategories the data element categories.
     * @param dataSetCategories the data set categories.
     * @param user the current user.
     * @return a set of {@link CategoryOption}.
     */
    private Set<CategoryOption> getCategoryOptions(
        Collection<Category> dataElementCategories, Collection<Category> dataSetCategories, User user )
    {
        Set<CategoryOption> options = flatMapToSet( dataElementCategories, Category::getCategoryOptions );
        dataSetCategories.forEach( c -> options.addAll( categoryService.getDataWriteCategoryOptions( c, user ) ) );
        return options;
    }

    /**
     * Returns option sets for the given data elements.
     *
     * @param dataElements the collection of data elements.
     * @return a set of {@link OptionSet}.
     */
    private Set<OptionSet> getOptionSets( Collection<DataElement> dataElements )
    {
        Set<OptionSet> optionSets = new HashSet<>();
        dataElements.forEach( de -> {
            addIfNotNull( optionSets, de.getOptionSet() );
            addIfNotNull( optionSets, de.getCommentOptionSet() );
        } );
        return optionSets;
    }

    /**
     * Returns data sets as a list of {@link ObjectNode}. Includes associations
     * to organisation units.
     *
     * @param dataSets the collection of {@link DataSet}.
     * @param dataSetOrgUnits the associations between data sets and
     *        organisation units.
     * @return a list of {@link ObjectNode}
     */
    private List<ObjectNode> toDataSetObjectNodes( Collection<DataSet> dataSets,
        SetValuedMap<String, String> dataSetOrgUnits )
    {
        List<ObjectNode> objectNodes = new ArrayList<>();

        for ( DataSet dataSet : dataSets )
        {
            ObjectNode objectNode = fieldFilterService.toObjectNode( dataSet, FIELDS_DATA_SETS );
            objectNode.set( PROPERTY_DATA_SET_ELEMENTS, toDataSetElementsArrayNode( dataSet.getDataSetElements() ) );
            objectNode.set( PROPERTY_ORGANISATION_UNITS, toOrgUnitsArrayNode( dataSet, dataSetOrgUnits ) );
            objectNodes.add( objectNode );
        }

        return objectNodes;
    }

    /**
     * Returns data set elements as an {@link ArrayNode}. The data set elements
     * are sorted by identifier to provide a stable order.
     *
     * @param dataSetElements the set of {@link DataSetElement}.
     * @return an {@link ArrayNode}.
     */
    private ArrayNode toDataSetElementsArrayNode( Set<DataSetElement> dataSetElements )
    {
        List<DataSetElement> elements = toDataSetElementList( dataSetElements );
        ArrayNode arrayNode = fieldFilterService.createArrayNode();
        List<ObjectNode> objectNodes = toObjectNodes( elements, FIELDS_DATA_SET_ELEMENTS, DataSetElement.class );
        objectNodes.forEach( arrayNode::add );
        return arrayNode;
    }

    /**
     * Returns a list of {@link DataSetElement} sorted by identifier to provide
     * a stable order.
     *
     * @param dataSetElements the set of {@link DataSetElement}.
     * @return a list of {@link DataSetElement}.
     */
    private List<DataSetElement> toDataSetElementList( Set<DataSetElement> dataSetElements )
    {
        List<DataSetElement> elements = new ArrayList<>( dataSetElements );
        Collections.sort( elements, Comparator.comparingLong( DataSetElement::getId ) );
        return elements;
    }

    /**
     * Returns organisation unit associations for the given data set as an
     * {@link ArrayNode}.
     *
     * @param dataSet the {@link DataSet}.
     * @param dataSetOrgUnits the associations between data sets and
     *        organisation units.
     * @return an {@link ArrayNode}.
     */
    private ArrayNode toOrgUnitsArrayNode( DataSet dataSet, SetValuedMap<String, String> dataSetOrgUnits )
    {
        ArrayNode arrayNode = fieldFilterService.createArrayNode();
        Set<String> orgUnits = dataSetOrgUnits.get( dataSet.getUid() );
        orgUnits.forEach( arrayNode::add );
        return arrayNode;
    }

    /**
     * Returns the given collection of objects as an {@link ObjectNode}.
     *
     * @param <T>
     * @param objects the collection of objects.
     * @param filters the filters to apply.
     * @param type the class type.
     * @return a list of {@link ObjectNode}.
     */
    private <T> List<ObjectNode> toObjectNodes(
        Collection<T> objects, String filters, Class<T> type )
    {
        FieldFilterParams<T> fieldFilterParams = FieldFilterParams.<T> builder()
            .objects( new ArrayList<>( objects ) )
            .filters( filters )
            .skipSharing( true )
            .build();

        return fieldFilterService.toObjectNodes( fieldFilterParams );
    }
}
