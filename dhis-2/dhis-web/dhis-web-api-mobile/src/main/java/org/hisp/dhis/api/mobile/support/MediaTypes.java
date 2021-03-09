package org.hisp.dhis.api.mobile.support;



import java.util.Arrays;
import java.util.List;

import org.springframework.http.MediaType;

public class MediaTypes
{
    public static String ACTIVITYVALUELIST_SERIALIZED = "application/vnd.org.dhis2.activityvaluelist+serialized";
    public static MediaType ACTIVITYVALUELIST_SERIALIZED_TYPE = 
        MediaType.parseMediaType( ACTIVITYVALUELIST_SERIALIZED );

    public static String DATASETVALUE_SERIALIZED = "application/vnd.org.dhis2.datasetvalue+serialized";
    public static MediaType DATASETVALUE_SERIALIZED_TYPE = 
        MediaType.parseMediaType( DATASETVALUE_SERIALIZED );

    public static String MOBILE_SERIALIZED = "application/vnd.org.dhis2.mobile+serialized";
    //public static String MOBILE_SERIALIZED = "application/vnd.org.dhis2.mobile+serialized;charset=UTF-8";
    public static MediaType MOBILE_SERIALIZED_TYPE = 
        MediaType.parseMediaType( MOBILE_SERIALIZED );
    
    public static String MOBILE_SERIALIZED_WITH_CHARSET = "application/vnd.org.dhis2.mobile+serialized;charset=UTF-8";
    public static MediaType MOBILE_SERIALIZED_TYPE_WITH_CHARSET = 
        MediaType.parseMediaType( MOBILE_SERIALIZED_WITH_CHARSET );

    public static List<MediaType> MEDIA_TYPES = Arrays.asList( new MediaType[] { ACTIVITYVALUELIST_SERIALIZED_TYPE,
            DATASETVALUE_SERIALIZED_TYPE, MOBILE_SERIALIZED_TYPE, MOBILE_SERIALIZED_TYPE_WITH_CHARSET } );
}