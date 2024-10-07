/**
 * Tracker supports importing data via SMS.
 *
 * <p>There are two types of supported SMS
 *
 * <ol>
 *   <li>Compression based SMS which use https://github.com/dhis2/sms-compression to compress and
 *       encode the SMS text. Types found in {@link org.hisp.dhis.smscompression.models} are used to
 *       build the SMS. Compression based SMS can be used by Android to sync data in case the device
 *       is offline. Each type is associated with one class processing the SMS.
 *   <li>Command based SMS which are not compressed and do not have dedicated types. These consist
 *       of plain text which look like {@code "register a=hello|c=there"}. The text is prefixed with
 *       a command name followed by key value pairs. The key value pairs are often data or attribute
 *       value pairs. Commands can be created by users. Each command is associated with a {@code
 *       ParserType} which is connected to one class processing the SMS.
 * </ol>
 */
package org.hisp.dhis.tracker.imports.sms;
