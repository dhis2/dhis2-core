/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.event.data.util;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.event.data.sql.transform.builder.SqlSelectInnerJoinStatementStringBuilder;
import org.hisp.dhis.analytics.event.data.sql.transform.provider.SqlNeedTransformationValueProvider;

/**
 * @author Dusan Bernat
 */
@Slf4j
public class SqlStringProcessor
{
    // private static final String PREFIX_ORG_UNIT_LEVEL = "uidlevel";
    //
    // private static final String LIMIT_1 = "limit 1";
    //
    // private static final String NESTED_SELECT_TOKEN_IN_WHERE_1 = "and (select
    // count";
    //
    // private static final String NESTED_SELECT_TOKEN_IN_WHERE_2 = "and
    // ((select count";
    //
    // private static final String EMPTY = "";
    //
    // private static final String LIMIT_TOKEN_1 = ") limit 100001";
    //
    // private static final String NEW_LINE = "\n";
    //
    // private static final String EMPTY_DELIMITER = " ";
    //
    // private static final String AND_TOKEN_1 = "\\) AND";
    //
    // private static final String LESS = "<";
    //
    // private static final String GREATER_EQ = ">=";
    //
    // private static final String CAST_TOKEN_1 = "cast((";
    //
    // private static final String TRIPLE_LEFT_PAR = "(((";
    //
    // private static final String GREATER_THEN_ZERO = "> 0";
    //
    // private static final String COALESCE_TOKEN_1 = "coalesce((";
    //
    // private static final String COALESCE_TOKEN_END = "'') = ";
    //
    // private static final String AND_TOKEN_2 = "and ";
    //
    // private static final String AND_TOKEN_3 = "AND";
    //
    // private static final String ONE_BRACKET = "\"";
    //
    // private static final String POINT = ".";
    //
    // private static final String COMMA = ",";
    //
    // private static final String COUNT_TOKEN_1 = "count(";
    //
    // private static final String BEGIN_PAR_1 = "(";
    //
    // private static final String END_PAR_1 = ")";
    //
    // private static final String PREFIX_PROGRAM_STAGE = "ps";
    //
    // private static final String PREFIX_PROGRAM_INDICATOR = "pi";
    //
    // private static final String INNER_JOIN_TOKEN_1 = "inner join ";
    //
    // private static final String INNER_JOIN_ON_TOKEN_1 = " on ax.pi = ";
    //
    // private static final String AS_TOKEN_1 = " as ";
    //
    // private static final String SELECT_TOKEN_1 = "select ";
    //
    // private static final String ANALYTICS_ENR_TABLE_ALIAS = "ax";
    //
    // private static final String IS_NOT_NULL_TOKEN_1 = " is not null";
    //
    // private static final String WHERE_TOKEN_1 = "where ";
    //
    // private static final String FROM_TOKEN_1 = "from ";

    public static String toInnerJoins( String sql, boolean pretty )
    {
        SqlNeedTransformationValueProvider sqlNeedTransformationValueProvider = new SqlNeedTransformationValueProvider();
        boolean needTransformation = sqlNeedTransformationValueProvider.getProvider().apply( sql );
        if ( needTransformation )
        {
            SqlSelectInnerJoinStatementStringBuilder sqlSelectInnerJoinStatementStringBuilder = new SqlSelectInnerJoinStatementStringBuilder(
                sql );

            return sqlSelectInnerJoinStatementStringBuilder.build( pretty );
        }
        else
        {
            return sql;
        }
        // if ( !hasToBeTransformed( selectSQL ) )
        // {
        // return selectSQL;
        // }
        //
        // Pair<String, String> sqlWithResidue = getSqlWithResidue(selectSQL);
        //
        // selectSQL = sqlWithResidue.getLeft();
        //
        // String residue = sqlWithResidue.getRight();
        //
        // try
        // {
        // StringBuilder sb = new StringBuilder();
        //
        // Statement select = CCJSqlParserUtil.parse( selectSQL );
        //
        // ParsedSqlStatement parsedSqlStatement = toParsedSqlStatement( select
        // );
        //
        // sb.append( parsedSqlStatement.getSelectColumns() ).append( pretty ?
        // NEW_LINE : EMPTY_DELIMITER );
        //
        // sb.append( parsedSqlStatement.getFrom() ).append( pretty ? NEW_LINE :
        // EMPTY_DELIMITER );
        //
        // List<String> innerJoins = parsedSqlStatement.getInnerJoins();
        //
        // for ( String innerJoin : innerJoins )
        // {
        // sb.append( innerJoin ).append( pretty ? NEW_LINE : EMPTY_DELIMITER );
        // }
        //
        // List<String> whereConditions =
        // parsedSqlStatement.getWhereConditions();
        //
        // for ( String where : whereConditions )
        // {
        // sb.append( where ).append( pretty ? NEW_LINE : EMPTY_DELIMITER );
        // }
        //
        // return sb + residue;
        // }
        // catch ( JSQLParserException e )
        // {
        // log.debug( e.getMessage() );
        // }
        // return EMPTY;
    }

    // private static Pair<String, String> getSqlWithResidue(String selectSQL)
    // {
    // String residue = EMPTY;
    //
    // int remainderIndex = selectSQL.indexOf( NESTED_SELECT_TOKEN_IN_WHERE_2 );
    //
    // if ( remainderIndex >= 0 )
    // {
    // residue = selectSQL.substring( remainderIndex );
    //
    // selectSQL = selectSQL.replace( residue, EMPTY );
    // }
    //
    // remainderIndex = selectSQL.indexOf( NESTED_SELECT_TOKEN_IN_WHERE_1 );
    //
    // if ( remainderIndex >= 0 )
    // {
    // residue += selectSQL.substring( remainderIndex ).replace( LIMIT_TOKEN_1,
    // EMPTY );
    //
    // selectSQL = selectSQL.replace( residue, EMPTY );
    // }
    //
    // return new ImmutablePair<>( selectSQL, residue );
    // }
    //
    // private static ParsedSqlStatement toParsedSqlStatement( Statement select
    // ) throws JSQLParserException
    // {
    // ParsedSqlStatement parsedSqlStatement = new ParsedSqlStatement();
    //
    // String where = ((PlainSelect) ((Select)
    // select).getSelectBody()).getWhere().toString();
    // ExpressionVisitor ev = new ExpressionVisitorAdapter();
    //
    // ((PlainSelect) ((Select) select).getSelectBody()).getWhere().accept(ev);
    //
    // List<String> sqlList = new ArrayList<>();
    //
    // List<String> years = new ArrayList<>();
    //
    // List<String> coalesceBasedFilterList = new ArrayList<>();
    //
    // String[] ands = where.split( AND_TOKEN_1 );
    //
    // for ( String and : Arrays.stream( ands ).distinct().collect(
    // Collectors.toList() ) )
    // {
    // String startDate = getDate( and.toLowerCase(), LESS );
    // if ( startDate != null && !startDate.trim().isEmpty() )
    // {
    // years.add( startDate );
    // }
    // String endDate = getDate( and.toLowerCase(), GREATER_EQ );
    // if ( endDate != null && !endDate.trim().isEmpty() )
    // {
    // years.add( endDate );
    // }
    //
    // int iStart = and.toLowerCase().indexOf( CAST_TOKEN_1 );
    // int iEnd = and.toLowerCase().indexOf( LIMIT_1 );
    // if ( iStart >= 0 && iEnd > iStart )
    // {
    // sqlList.add( and.substring( iStart + CAST_TOKEN_1.length(), iEnd ) );
    // }
    //
    // iStart = and.indexOf( TRIPLE_LEFT_PAR );
    // iEnd = and.indexOf( GREATER_THEN_ZERO );
    // if ( iStart >= 0 && iEnd > iStart )
    // {
    // sqlList
    // .add( and.substring( iStart + TRIPLE_LEFT_PAR.length(), iEnd -
    // (GREATER_THEN_ZERO.length() - 1) ) );
    // }
    //
    // iStart = and.toLowerCase().indexOf( COALESCE_TOKEN_1 );
    // iEnd = and.toLowerCase().indexOf( LIMIT_1 );
    // if ( iStart >= 0 && iEnd > iStart )
    // {
    // int valueIndex = and.indexOf( COALESCE_TOKEN_END );
    // String sqlSnippet = and.substring( iStart + COALESCE_TOKEN_1.length(),
    // iEnd );
    // sqlList.add( sqlSnippet );
    //
    // Statement s = CCJSqlParserUtil.parse( sqlSnippet );
    // List<SelectItem> selectCols = ((PlainSelect) ((Select)
    // s).getSelectBody()).getSelectItems();
    // Optional<SelectItem> columnName = selectCols.stream().findFirst();
    // columnName.ifPresent( selectItem -> coalesceBasedFilterList
    // .add( AND_TOKEN_2 + selectItem.toString().replace( ONE_BRACKET, EMPTY ) +
    // POINT + selectItem + and.substring( valueIndex + 3 ).replace( END_PAR_1,
    // EMPTY ) ) );
    // }
    // }
    //
    // List<String> tableNames = new ArrayList<>();
    // List<String> aliasList = new ArrayList<>();
    // List<String> psList = new ArrayList<>();
    // List<String> aliasBasedFilterList = new ArrayList<>();
    //
    // for ( String sql : sqlList.stream().distinct().collect(
    // Collectors.toList() ) )
    // {
    // Statement nestedSelect = CCJSqlParserUtil.parse( sql );
    //
    // List<SelectItem> selectCols = ((PlainSelect) ((Select)
    // nestedSelect).getSelectBody()).getSelectItems();
    // String alias = selectCols.get( 0 ).toString()
    // .replaceAll( ONE_BRACKET, EMPTY ).replace( COUNT_TOKEN_1, EMPTY )
    // .replace( END_PAR_1, EMPTY );
    // aliasList.add( alias );
    //
    // String nestedWhere = ((PlainSelect) ((Select)
    // nestedSelect).getSelectBody()).getWhere().toString();
    //
    // String[] nestedWhereElements = nestedWhere.split( AND_TOKEN_3 );
    //
    // aliasBasedFilterList.addAll( getAliasBasedFilterList( aliasList,
    // Arrays.stream( nestedWhereElements ).collect( Collectors.toList() ) ) );
    //
    // psList.add( Arrays.stream( nestedWhereElements ).filter( el ->
    // el.contains( PREFIX_PROGRAM_STAGE ) )
    // .map( el -> AND_TOKEN_2 + alias + POINT + el.trim() ).collect(
    // Collectors.joining() ) );
    //
    // String innerJoin = INNER_JOIN_TOKEN_1
    // + ((Table) ((PlainSelect) ((Select)
    // nestedSelect).getSelectBody()).getFromItem()).getName() +
    // AS_TOKEN_1 + alias + INNER_JOIN_ON_TOKEN_1 + alias + POINT +
    // PREFIX_PROGRAM_INDICATOR;
    // tableNames.add( innerJoin );
    // }
    //
    // List<SelectItem> mainSelectCols = ((PlainSelect) ((Select)
    // select).getSelectBody()).getSelectItems();
    //
    // parsedSqlStatement.setSelectColumns( SELECT_TOKEN_1 +
    // mainSelectCols.stream()
    // .map( c -> {
    // if ( !c.toString().contains( POINT + PREFIX_PROGRAM_INDICATOR ) )
    // {
    // return c.toString().replace( PREFIX_PROGRAM_INDICATOR,
    // ANALYTICS_ENR_TABLE_ALIAS + POINT + PREFIX_PROGRAM_INDICATOR );
    // }
    // else
    // {
    // return c.toString();
    // }
    // } ).collect( Collectors.joining( COMMA ) ) );
    //
    // List<String> timeConditions = Arrays.stream( DateUnitType.values() ).map(
    // DateUnitType::getName )
    // .collect( Collectors.toList() );
    // parsedSqlStatement.getWhereConditions()
    // .add( WHERE_TOKEN_1 + aliasList.get( 0 ) + POINT + ONE_BRACKET +
    // aliasList.get( 0 ) + ONE_BRACKET +
    // IS_NOT_NULL_TOKEN_1 );
    // parsedSqlStatement.getWhereConditions()
    // .add( AND_TOKEN_2 + aliasList.get( 0 ) + POINT
    // + Arrays.stream( ands ).filter( and -> and.contains(
    // PREFIX_ORG_UNIT_LEVEL ) )
    // .map( and -> and.replace( BEGIN_PAR_1, EMPTY ).replace( END_PAR_1, EMPTY
    // ).trim() )
    // .collect( Collectors.joining() ) );
    // parsedSqlStatement.getWhereConditions().addAll( psList );
    // parsedSqlStatement.getWhereConditions().addAll( getYearlies( aliasList,
    // years ) );
    // for ( String timeCondition : timeConditions )
    // {
    // String condition = getTimeCondition( aliasList.get( 0 ), mainSelectCols,
    // timeCondition );
    // if ( condition != null && !condition.trim().isEmpty() )
    // {
    // parsedSqlStatement.getWhereConditions().add( condition );
    // }
    // }
    // parsedSqlStatement.getWhereConditions().addAll( aliasBasedFilterList );
    // parsedSqlStatement.getWhereConditions().addAll( coalesceBasedFilterList
    // );
    // parsedSqlStatement
    // .setFrom( FROM_TOKEN_1 + (((PlainSelect) ((Select)
    // select).getSelectBody()).getFromItem()).toString() );
    // parsedSqlStatement.getInnerJoins().addAll(
    // tableNames.stream().distinct().collect( Collectors.toList() ) );
    // return parsedSqlStatement;
    // }
    //
    // private static String getTimeCondition( String alias, List<SelectItem>
    // mainSelectCols, String period )
    // {
    // for ( SelectItem item : mainSelectCols )
    // {
    // String col = item.toString();
    //
    // if ( (col.toLowerCase() + ":").contains( " " + period.toLowerCase() + ":"
    // ) )
    // {
    // String el = col.toLowerCase().replace( "as " + period.toLowerCase(), ""
    // ).trim();
    //
    // return AND_TOKEN_2 + alias + "." + period.toLowerCase() + " = " +
    // col.substring( 0, el.length() );
    // }
    // }
    //
    // return null;
    // }
    //
    // private static List<String> getAliasBasedFilterList( List<String>
    // aliasList, List<String> nestedWhereElements )
    // {
    // return aliasList.stream().map( al -> {
    // if ( nestedWhereElements.stream().anyMatch( nwe -> nwe.contains(
    // ONE_BRACKET + al + "\" =" ) ) )
    // {
    // return AND_TOKEN_2 + al + POINT + nestedWhereElements.stream()
    // .filter( nwe -> nwe.contains( ONE_BRACKET + al + "\" =" ) ).map(
    // String::trim )
    // .collect( Collectors.joining() );
    // }
    //
    // return EMPTY;
    // } ).filter( r -> !r.isEmpty() ).collect( Collectors.toList() );
    // }
    //
    // private static List<String> getYearlies( List<String> aliasList,
    // List<String> years )
    // {
    // return aliasList.stream().map( a -> "and " + a + ".yearly in ('" +
    // years.stream().distinct().map( Object::toString ).collect(
    // Collectors.joining( "','" ) )
    // + "')" ).collect( Collectors.toList() );
    // }
    //
    // private static String getDate( String sqlSnippet, String delimiter )
    // {
    // String[] tokens = sqlSnippet.split( delimiter );
    //
    // Optional<String> date = Arrays.stream( tokens ).filter( s -> !s.contains(
    // "select" ) )
    // .filter( s -> s.contains( "as date" ) ).findFirst();
    //
    // if ( date.isPresent() )
    // try
    // {
    // LocalDate ld = LocalDate.parse( date.map( s -> s.replace( "cast(", "" )
    // .replace( "as date", "" ).replace( "'", "" ).trim() ).orElse( null ) );
    //
    // if ( ld.getMonthValue() == 1 && ld.getDayOfYear() == 1 && "<".equals(
    // delimiter ) )
    // {
    // return null;
    // }
    //
    // return Integer.toString( ld.getYear() );
    // }
    // catch ( DateTimeParseException e )
    // {
    // return null;
    // }
    //
    // return null;
    // }
    //
    // private static boolean hasToBeTransformed( String sql )
    // {
    // return sql.toLowerCase().lastIndexOf( "where" ) !=
    // sql.toLowerCase().indexOf( "where" );
    // }
}
