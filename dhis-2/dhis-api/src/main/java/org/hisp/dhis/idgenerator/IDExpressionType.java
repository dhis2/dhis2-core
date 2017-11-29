package org.hisp.dhis.idgenerator;

public enum IDExpressionType
{

    // COMPLEX contains one or more sub-expressions
    // Resolves to the resolution of its sub-expressions
    COMPLEX,

    // TEXT contains a String
    // Resolves to itself
    TEXT,

    // RANDOM contains a pattern of #, X or x, representing a digit, uppercase letter or lower case letter respectively
    // Resolves to a unique random string based on the pattern
    RANDOM,

    // SEQUENTIAL contains a pattern of #, representing a digit
    // Resolves to the next number for the sequential segment (based on existing generated ids)
    SEQUENTIAL
}
