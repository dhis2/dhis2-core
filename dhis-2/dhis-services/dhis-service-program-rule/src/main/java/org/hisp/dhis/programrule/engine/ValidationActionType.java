package org.hisp.dhis.programrule.engine;

import java.util.Arrays;

public enum ValidationActionType {
    ASSIGN,
    SETMANDATORYFIELD,
    SHOWERROR,
    SHOWWARNING,
    ERRORONCOMPLETE,
    WARNINGONCOMPLETE,
    ERROR;

    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch( v -> v.name().equalsIgnoreCase(value) );
    }
}
