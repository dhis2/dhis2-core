package org.hisp.dhis.reservedvalue;

import java.util.List;

public interface ReservedValueService
{

    List<String> generateAndReserveRandomValues(String uid, String key, String pattern, int length);
    List<String> generateAndReserveSequentialValues(String uid, String key, String pattern, int length);

}
