package org.hisp.dhis.outlierdetection.parser;

import lombok.AllArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.validation.outlierdetection.ValidationOutlierDetectionRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class OutlierDetectionQueryParser {

    private final IdentifiableObjectManager idObjectManager;
    private final ValidationOutlierDetectionRequest validator;

    /**
     * Creates a {@link OutlierDetectionRequest} from the given query.
     *
     * @param query the {@link OutlierDetectionQuery}.
     * @return a {@link OutlierDetectionRequest}.
     */
    public OutlierDetectionRequest getFromQuery(OutlierDetectionQuery query, boolean isAnalyticsRequest) {
        OutlierDetectionRequest.Builder request = new OutlierDetectionRequest.Builder();

        List<DataSet> dataSets = idObjectManager.getByUid(DataSet.class, query.getDs());

        // Re-fetch data elements to maintain access control

        List<String> de =
                dataSets.stream()
                        .map(DataSet::getDataElements)
                        .flatMap(Collection::stream)
                        .filter(d -> d.getValueType().isNumeric())
                        .map(DataElement::getUid)
                        .collect(Collectors.toList());

        de.addAll(query.getDe());

        List<DataElement> dataElements = idObjectManager.getByUid(DataElement.class, de);
        List<OrganisationUnit> orgUnits =
                idObjectManager.getByUid(OrganisationUnit.class, query.getOu());

        request
                .withDataElements(dataElements)
                .withStartEndDate(query.getStartDate(), query.getEndDate())
                .withOrgUnits(orgUnits)
                .withDataStartDate(query.getDataStartDate())
                .withDataEndDate(query.getDataEndDate());

        if (query.getAlgorithm() != null) {
            request.withAlgorithm(query.getAlgorithm());
        }

        if (query.getThreshold() != null) {
            request.withThreshold(query.getThreshold());
        }

        if (query.getOrderBy() != null) {
            request.withOrderBy(query.getOrderBy());
        }

        if (query.getMaxResults() != null) {
            request.withMaxResults(query.getMaxResults());
        }

        OutlierDetectionRequest outlierDetectionRequest = request.build();
        validator.validate(outlierDetectionRequest, isAnalyticsRequest);

        return outlierDetectionRequest;
    }
}
