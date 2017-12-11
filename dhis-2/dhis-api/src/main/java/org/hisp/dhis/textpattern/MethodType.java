package org.hisp.dhis.textpattern;

public interface MethodType
{
    /**
     * REQUIRED: This method requires a value to be supplied by used
     * OPTIONAL: This method will have the value generated on the server, but user can override (Example: specific date to override CURRENT_DATE)
     * NONE: This method cannot be supplied by user (Example: TEXT)
     */
    enum RequiredStatus {
        REQUIRED,
        OPTIONAL,
        NONE
    }

    /**
     * Validates the pattern of the type against the string input
     * @param raw the string to validate
     * @return true if raw matches pattern, false if not
     */
    boolean validatePattern( String raw );

    /**
     * Validates a text against a format. Format will be adjusted based on the MethodType
     * @param format the format to validate for
     * @param text the text to validate
     * @return true if it matches, false if not
     */
    boolean validateText( String format, String text );

    /**
     * Returns the param part of the Method from the raw String
     * @param raw the string to retrieve param from
     * @return the param from the raw String
     */
    String getParam( String raw );

    /**
     * Returns a regex String based on the format
     * @param format the format to transform into regex
     * @return a regex String that matches the format and MethodType
     */
    String getValueRegex( String format );

    /**
     * Returns a String after applying format to the value
     * @param format the format to apply to the value
     * @param value the string to format
     * @return the formatted text
     */
    String getFormattedText( String format, String value );

    /**
     * Returns true if this MethodType is required when injecting values
     * @return true if REQUIRED RequiredStatus, false if not
     */
    boolean isRequired();

    /**
     * Returns true if this MethodType is optional when injecting values
     * @return true if OPTIONAL RequiredStatus, false if not
     */
    boolean isOptional();
}
