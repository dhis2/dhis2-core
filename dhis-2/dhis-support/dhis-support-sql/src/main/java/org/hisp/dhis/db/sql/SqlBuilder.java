/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.db.sql;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.db.model.DateUnit;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;

/**
 * Provides methods for generation of SQL statements and queries.
 *
 * @author Lars Helge Overland
 */
public interface SqlBuilder {

  // Database

  /**
   * @return the {@link Database}.
   */
  Database getDatabase();

  // Data types

  /**
   * @return the name of the small integer data type.
   */
  String dataTypeSmallInt();

  /**
   * @return the name of the integer data type.
   */
  String dataTypeInteger();

  /**
   * @return the name of the big integer data type.
   */
  String dataTypeBigInt();

  /**
   * @return the name of the decimal data type.
   */
  String dataTypeDecimal();

  /**
   * @return the name of the float data type.
   */
  String dataTypeFloat();

  /**
   * @return the name of the double data type.
   */
  String dataTypeDouble();

  /**
   * @return the name of the boolean data type.
   */
  String dataTypeBoolean();

  /**
   * @param length the character length.
   * @return the name of the character data type.
   */
  String dataTypeCharacter(int length);

  /**
   * @param length the character length.
   * @return the name of the character varying data type.
   */
  String dataTypeVarchar(int length);

  /**
   * @return the name of the text data type.
   */
  String dataTypeText();

  /**
   * @return the name of the date data type.
   */
  String dataTypeDate();

  /**
   * @return the name of the timestamp data type.
   */
  String dataTypeTimestamp();

  /**
   * @return the name of the timestamp with time zone data type.
   */
  String dataTypeTimestampTz();

  /**
   * @return the name of the geometry data type.
   */
  String dataTypeGeometry();

  /**
   * @return the name of the geometry point data type.
   */
  String dataTypeGeometryPoint();

  /**
   * @return the name of the JSON data type.
   */
  String dataTypeJson();

  // Index types

  /**
   * @return the name of the B-Tree index type.
   */
  String indexTypeBtree();

  /**
   * @return the name of the GiST index type.
   */
  String indexTypeGist();

  /**
   * @return the name of the GIN index type.
   */
  String indexTypeGin();

  // Index functions

  /**
   * @return the name of the upper index function.
   */
  String indexFunctionUpper();

  /**
   * @return the name of the lower index function.
   */
  String indexFunctionLower();

  // Capabilities

  /**
   * @return true if the DBMS supports geospatial data types and functions.
   */
  boolean supportsGeospatialData();

  /**
   * @return true if the DBMS supports declarative partitioning.
   */
  boolean supportsDeclarativePartitioning();

  /**
   * @return true if the DBMS supports table analysis.
   */
  boolean supportsAnalyze();

  /**
   * @return true if the DBMS supports table vacuuming.
   */
  boolean supportsVacuum();

  /**
   * @return true if the DBMS supports correlated subqueries.
   */
  boolean supportsCorrelatedSubquery();

  /**
   * @return true if the DMBS supports multiple statements in one operation.
   */
  boolean supportsMultiStatements();

  /**
   * @return true if the DBMS supports updates for tables based on multiple keys, e.g. composite
   *     primary key.
   */
  boolean supportsUpdateForMultiKeyTable();

  /**
   * @return true if the DBMS requires indexes for analytics tables for performance.
   */
  boolean requiresIndexesForAnalytics();

  // Utilities

  /**
   * @param relation the relation to quote, e.g. a table or column name.
   * @return a double-quoted relation.
   */
  String quote(String relation);

  /**
   * @param relation the relation to quote, e.g. a column name.
   * @return an aliased and double-quoted relation.
   */
  String quote(String alias, String relation);

  /**
   * @param relation the relation to quote.
   * @return an "ax" aliased and double quoted relation.
   */
  String quoteAx(String relation);

  /**
   * @param value the value to quote.
   * @return a single quoted value.
   */
  String singleQuote(String value);

  /**
   * @param value the value to escape.
   * @return the escaped value, with single quotes doubled up.
   */
  String escape(String value);

  /**
   * @param items the items to join.
   * @return a string representing the comma delimited and single quoted item values.
   */
  String singleQuotedCommaDelimited(Collection<String> items);

  /**
   * @param name the table name.
   * @return a fully qualified, quoted table reference specifying the catalog, database and table.
   */
  String qualifyTable(String name);

  /**
   * @param timeUnit the time unit as string, e.g. 'hour', 'day', 'year'.
   * @param source the value expression as string of type timestamp or interval.
   * @return a date truncate expression.
   */
  String dateTrunc(String timeUnit, String source);

  /**
   * @param columnA the name of the first date column.
   * @param columnB the name of the second date column.
   * @return an expression which returns the difference in seconds.
   */
  String differenceInSeconds(String columnA, String columnB);

  /**
   * @param value the string value such as a column or expression.
   * @param pattern the regular expression pattern to match against.
   * @return a regular expression string matching clause.
   */
  String regexpMatch(String value, String pattern);

  /**
   * Concatenates multiple columns or expressions safely (null values are treated as empty strings).
   *
   * @param columns the column names or expressions to concatenate.
   * @return the SQL function for concatenation.
   */
  String safeConcat(String... columns);

  /**
   * Concatenates list of columns or expressions.
   *
   * @param columns a list of column names or expressions to concatenate.
   * @return the SQL function for concatenation.
   */
  String concat(List<String> columns);

  /**
   * Trims the given column or expression by removing leading and trailing spaces.
   *
   * @param expression the expression to trim.
   * @return the SQL function for trimming.
   */
  String trim(String expression);

  /**
   * Creates a coalesce function that returns the first non-null expression from the provided
   * expressions. If the first expression is null, it returns the default expression.
   *
   * @param expression the expression to check for null.
   * @param defaultValue the value to return if the first expression is null.
   * @return the coalesce function.
   */
  String coalesce(String expression, String defaultValue);

  /**
   * Extracts a value from a JSON column using a specified property path.
   *
   * @param json the JSON column name or value to extract from.
   * @param property the JSON property to extract.
   * @return a function for JSON value extraction.
   */
  String jsonExtract(String json, String property);

  /**
   * Extracts a nested value from a JSON object.
   *
   * @param json the JSON column name or object to extract from.
   * @param key the object key.
   * @param property the JSON property to extract.
   * @return a function for JSON value extraction.
   */
  String jsonExtract(String json, String key, String property);

  /**
   * Casts the given column or expression to the given data type.
   *
   * @param column The column or expression to be cast, must not be null.
   * @param dataType the {@link DataType} data type for the cast operation, must not be null.
   * @return A String containing the database-specific SQL casting expression.
   */
  String cast(String column, DataType dataType);

  /**
   * Calculates the difference between two dates based on the specified date part.
   *
   * @param startDate the start date expression (can be a date literal or a column reference)
   * @param endDate the end date expression (can be a date literal or a column reference).
   * @param dateUnit the {@link DateUnit} to calculate the difference in.
   * @return the database-specific SQL expression for calculating the date difference.
   */
  String dateDifference(String startDate, String endDate, DateUnit dateUnit);

  /**
   * @param alias the table alias, not quoted, not null.
   * @param column the column name, not quoted, not null.
   * @return a false predicate.
   */
  String isTrue(String alias, String column);

  /**
   * @param alias the table alias, not quoted, not null.
   * @param column the column name, not quoted, not null.
   * @return a false predicate.
   */
  String isFalse(String alias, String column);

  /**
   * Returns a conditional statement.
   *
   * @param condition the condition to evaluate.
   * @param result the result to return if the condition is true.
   * @return a conditional statement.
   */
  String ifThen(String condition, String result);

  /**
   * Generates a SQL fragment that computes the base-10 logarithm of the specified expression.
   *
   * @param expression a numeric expression or column name for which to compute the logarithm.
   * @return a SQL fragment that calculates the base-10 logarithm of the given expression.
   */
  String log10(String expression);

  /**
   * Generates a SQL fragment that compute the standard deviation of the specified expression.
   *
   * @param expression a numeric expression or column name for which to compute the standard
   *     deviation.
   * @return a SQL fragment that calculates the standard deviation of the given expression.
   */
  String stddev(String expression);

  /**
   * Generates a SQL fragment that computes the variance of the specified expression. Variance
   * measures how far a set of numbers are spread out from their average value. A higher variance
   * indicates greater data dispersion.
   *
   * @param expression a numeric expression or column name for which to compute the variance.
   * @return a SQL fragment that calculates the variance of the given expression.
   */
  String variance(String expression);

  /**
   * Returns a conditional statement.
   *
   * @param condition the condition to evaluate.
   * @param thenResult the result to return if the condition is true.
   * @param elseResult the result to return if the condition is false.
   * @return a conditional statement.
   */
  String ifThenElse(String condition, String thenResult, String elseResult);

  /**
   * Returns a conditional statement.
   *
   * @param conditionA the first condition to evaluate.
   * @param thenResultA the result to return if the first condition is true.
   * @param conditionB the second condition to evaluate.
   * @param thenResultB the result to return if the second condition is false.
   * @param elseResult the result to return if all conditions are false.
   * @return a conditional statement.
   */
  String ifThenElse(
      String conditionA,
      String thenResultA,
      String conditionB,
      String thenResultB,
      String elseResult);

  // Statements

  /**
   * @param table the {@link Table}.
   * @return a create table statement.
   */
  String createTable(Table table);

  /**
   * @param table the {@link Table}.
   * @return an analyze table statement.
   */
  String analyzeTable(Table table);

  /**
   * @param name the table name.
   * @return an analyze table statement.
   */
  String analyzeTable(String name);

  /**
   * @param table the {@link Table}.
   * @return a vacuum table statement.
   */
  String vacuumTable(Table table);

  /**
   * @param table the {@link Table}.
   * @param newName the new name for the table.
   * @return a rename table statement.
   */
  String renameTable(Table table, String newName);

  /**
   * @param table the {@link Table}.
   * @return a drop table if exists statement.
   */
  String dropTableIfExists(Table table);

  /**
   * @param name the table name.
   * @return a drop table if exists statement.
   */
  String dropTableIfExists(String name);

  /**
   * @param table the {@link Table}.
   * @return a drop table if exists cascade statement.
   */
  String dropTableIfExistsCascade(Table table);

  /**
   * @param name the table name.
   * @return a drop table if exists cascade statement.
   */
  String dropTableIfExistsCascade(String name);

  /**
   * @param table the {@link Table}.
   * @param newName the new name for the table.
   * @return a combined drop table if exists cascade and rename table statement.
   */
  String swapTable(Table table, String newName);

  /**
   * @param table the {@link Table}.
   * @param parentName the parent table name.
   * @return a table inherit statement.
   */
  String setParentTable(Table table, String parentName);

  /**
   * @param table the {@link Table}.
   * @param parentName the parent table name.
   * @return a table no inherit statement.
   */
  String removeParentTable(Table table, String parentName);

  /**
   * @param table the {@link Table}.
   * @param parentName the name of the current parent table.
   * @param newParentName the name of the new parent table.
   * @return a combined table inherit and table no inherit statement.
   */
  String swapParentTable(Table table, String parentName, String newParentName);

  /**
   * @param table the {@link Table}.
   * @return a statement which will return a single row with a single column with the table name if
   *     the table exists.
   */
  String tableExists(Table table);

  /**
   * @param name the table name.
   * @return a statement which will return a single row with a single column with the table name if
   *     the table exists.
   */
  String tableExists(String name);

  /**
   * @param table the {@link Table}.
   * @return a count rows statement.
   */
  String countRows(Table table);

  /**
   * @param index the {@link Index}.
   * @return a create index statement.
   */
  String createIndex(Index index);

  /**
   * @param intoTable the table to insert rows into.
   * @param fromTable the name of the table to select rows from, preferrably quoted.
   * @return in insert into select from statement.
   */
  String insertIntoSelectFrom(Table intoTable, String fromTable);
}
