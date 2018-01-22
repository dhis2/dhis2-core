package org.hisp.dhis.common.Coordinate;

import org.hisp.dhis.organisationunit.FeatureType;

/**
 * @author Henning Håkonsen
 */
public interface CoordinateObject
{
    FeatureType getFeatureType();

    boolean hasFeatureType();

    String getCoordinates();

    boolean hasCoordinates();

    boolean hasDescendantsWithCoordinates();
}
