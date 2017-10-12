package org.hisp.dhis.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;

/**
 * @author Henning Håkonsen
 */
public class MetaDataItemDetails {
    private String uid;

    private String code;

    private DimensionItemType dimensionItemType;

    private DimensionType dimensionType;

    private AggregationType aggregationType;

    MetaDataItemDetails( DimensionalItemObject dimensionalItemObject )
    {
        this.code = dimensionalItemObject.getCode();
        this.dimensionItemType = dimensionalItemObject.getDimensionItemType();
        this.aggregationType = dimensionalItemObject.getAggregationType();
        this.uid = dimensionalItemObject.getUid();
    }

    MetaDataItemDetails( DimensionalObject dimensionalObject )
    {
        this.code = dimensionalObject.getCode();
        this.dimensionType = dimensionalObject.getDimensionType();
        this.aggregationType = dimensionalObject.getAggregationType();
        this.uid = dimensionalObject.getUid();
    }

    @JsonProperty
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @JsonProperty
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonProperty
    public DimensionType getDimensionType() {
        return dimensionType;
    }

    public void setDimensionType(DimensionType type) {
        this.dimensionType = type;
    }

    @JsonProperty
    public AggregationType getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(AggregationType itemSpecificType) {
        this.aggregationType = itemSpecificType;
    }

    @JsonProperty
    public DimensionItemType getDimensionItemType() {
        return dimensionItemType;
    }

    public void setDimensionItemType(DimensionItemType dimensionItemType) {
        this.dimensionItemType = dimensionItemType;
    }
}
