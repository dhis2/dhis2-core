package org.hisp.dhis.outlierdetection;

public enum Order
{
    Z_SCORE( "z_score" ),
    MEAN_ABS_DEV( "mean_abs_dev" );

    private String key;

    Order( String key )
    {
        this.key = key;
    }

    public String getKey()
    {
        return key;
    }
}
