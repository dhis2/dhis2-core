package org.hisp.dhis.outlierdetection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.util.DateUtils;

import com.google.common.base.Preconditions;

import lombok.Getter;

/**
 * Encapsulation of an outlier detection query with available parameters.
 *
 * @author Lars Helge Overland
 */
@Getter
public class OutlierDetectionRequest
{
    private List<DataElement> dataElements = new ArrayList<>();

    private Date startDate;

    private Date endDate;

    private List<OrganisationUnit> orgUnits = new ArrayList<>();

    private OrgUnitSelection orgUnitSelection;

    private OutlierDetectionAlgorithm outlierAlgorithm = OutlierDetectionAlgorithm.Z_SCORE;

    private double threshold;

    private Order orderBy;

    private int maxResults;

    public List<Long> getDataElementIds()
    {
        return dataElements.stream()
            .map( DataElement::getId )
            .collect( Collectors.toList() );
    }

    public String getStartDateString()
    {
        return DateUtils.getMediumDateString( startDate );
    }

    public String getEndDateString()
    {
        return DateUtils.getMediumDateString( endDate );
    }

    private OutlierDetectionRequest()
    {
    }

    public static class Builder
    {
        private OutlierDetectionRequest query;

        /**
         * Initializes the {@link OutlierDetectionRequest} with default values.
         */
        public Builder()
        {
            this.query = new OutlierDetectionRequest();

            this.query.orgUnitSelection = OrgUnitSelection.DESCENDANTS;
            this.query.outlierAlgorithm = OutlierDetectionAlgorithm.Z_SCORE;
            this.query.threshold = 2.5d;
            this.query.orderBy = Order.MEAN_ABS_DEV;
            this.query.maxResults = 1000;
        }

        public Builder withDataElements( List<DataElement> dataElements )
        {
            this.query.dataElements = dataElements;
            return this;
        }

        public Builder withStartEndDate( Date startDate, Date endDate )
        {
            this.query.startDate = startDate;
            this.query.endDate = endDate;
            return this;
        }

        public Builder withOrgUnits( List<OrganisationUnit> orgUnits )
        {
            this.query.orgUnits = orgUnits;
            return this;
        }

        public Builder withThreshold( double threshold )
        {
            this.query.threshold = threshold;
            return this;
        }

        public Builder withOrderBy( Order orderBy )
        {
            this.query.orderBy = orderBy;
            return this;
        }

        public Builder withMaxResults( int maxResults )
        {
            this.query.maxResults = maxResults;
            return this;
        }

        public OutlierDetectionRequest build()
        {
            Preconditions.checkNotNull( this.query.orgUnitSelection );
            Preconditions.checkNotNull( this.query.outlierAlgorithm );
            Preconditions.checkNotNull( this.query.orderBy );
            return this.query;
        }
    }
}

