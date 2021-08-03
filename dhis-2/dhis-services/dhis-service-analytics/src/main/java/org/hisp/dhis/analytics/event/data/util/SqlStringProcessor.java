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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

import org.hisp.dhis.analytics.event.data.model.ParsedSqlStatement;

/**
 * @author Dusan Bernat
 */
public class SqlStringProcessor
{
    public static String toInnerJoins( String selectSQL, boolean pretty )
    {
        if ( !hasToBeTransformed( selectSQL ) )
        {
            return selectSQL;
        }
        String remainder = "";
        int remainderIndex = selectSQL.indexOf( "and ((select count" );
        if ( remainderIndex >= 0 )
        {
            remainder = selectSQL.substring( remainderIndex );
            selectSQL = selectSQL.replace( remainder, "" );
        }

        remainderIndex = selectSQL.indexOf( "and (select count" );
        if ( remainderIndex >= 0 )
        {
            remainder += selectSQL.substring( remainderIndex ).replace( ") limit 100001", "" );
            selectSQL = selectSQL.replace( remainder, "" );
        }

        try
        {
            StringBuilder ret = new StringBuilder();
            Statement select = CCJSqlParserUtil.parse( selectSQL );
            ParsedSqlStatement parsedSqlStatement = toParsedSqlStatement( select );
            ret.append( parsedSqlStatement.getSelectColumns() ).append( pretty ? "\n" : " " );
            ret.append( parsedSqlStatement.getFrom() ).append( pretty ? "\n" : " " );
            List<String> innerJoins = parsedSqlStatement.getInnerJoins();
            for ( String innerJoin : innerJoins )
            {
                ret.append( innerJoin ).append( pretty ? "\n" : " " );
            }
            List<String> whereConditions = parsedSqlStatement.getWhereConditions();
            for ( String where : whereConditions )
            {
                ret.append( where ).append( pretty ? "\n" : " " );
            }
            return ret + remainder;
        }
        catch ( JSQLParserException e )
        {
            e.printStackTrace();
        }
        return "";
    }

    private static ParsedSqlStatement toParsedSqlStatement( Statement select )
        throws JSQLParserException
    {
        ParsedSqlStatement parsedSqlStatement = new ParsedSqlStatement();

        String where = ((PlainSelect) ((Select) select).getSelectBody()).getWhere().toString();
        List<String> sqlList = new ArrayList<>();

        String[] ands = where.split( "\\) AND" );

        List<String> years = new ArrayList<>();
        List<String> coalesceBasedFilterList = new ArrayList<>();
        for ( String and : Arrays.stream( ands ).distinct().collect( Collectors.toList() ) )
        {
            String startDate = getDate( and.toLowerCase(), "<" );
            if ( startDate != null && !startDate.trim().isEmpty() )
            {
                years.add( startDate );
            }
            String endDate = getDate( and.toLowerCase(), ">=" );
            if ( endDate != null && !endDate.trim().isEmpty() )
            {
                years.add( endDate );
            }

            int iStart = and.toLowerCase().indexOf( "cast((" );
            int iEnd = and.toLowerCase().indexOf( "limit 1" );
            if ( iStart >= 0 && iEnd > iStart )
            {
                sqlList.add( and.substring( iStart + 6, iEnd ) );
            }

            iStart = and.indexOf( "(((" );
            iEnd = and.indexOf( "> 0" );
            if ( iStart >= 0 && iEnd > iStart )
            {
                sqlList.add( and.substring( iStart + 3, iEnd - 2 ) );
            }

            iStart = and.toLowerCase().indexOf( "coalesce((" );
            iEnd = and.toLowerCase().indexOf( "limit 1" );
            if ( iStart >= 0 && iEnd > iStart )
            {
                int valueIndex = and.indexOf( "'') = " );
                sqlList.add( and.substring( iStart + 10, iEnd ) );

                Statement s = CCJSqlParserUtil.parse( and.substring( iStart + 10, iEnd ) );
                List<SelectItem> selectCols = ((PlainSelect) ((Select) s).getSelectBody()).getSelectItems();
                Optional<SelectItem> columnName = selectCols.stream().findFirst();
                columnName.ifPresent(
                    selectItem -> coalesceBasedFilterList.add( "and " + selectItem.toString().replace( "\"", "" ) +
                        "." + selectItem + and.substring( valueIndex + 3 ).replace( ")", "" ) ) );
            }
        }

        List<String> tableNames = new ArrayList<>();
        List<String> aliasList = new ArrayList<>();
        List<String> psList = new ArrayList<>();
        List<String> aliasBasedFilterList = new ArrayList<>();
        for ( String sql : sqlList.stream().distinct().collect( Collectors.toList() ) )
        {
            Statement nestedSelect = CCJSqlParserUtil.parse( sql );

            List<SelectItem> selectCols = ((PlainSelect) ((Select) nestedSelect).getSelectBody()).getSelectItems();
            String alias = selectCols.get( 0 ).toString()
                .replaceAll( "\"", "" ).replace( "count(", "" )
                .replace( ")", "" );
            aliasList.add( alias );

            String nestedWhere = ((PlainSelect) ((Select) nestedSelect).getSelectBody()).getWhere().toString();

            String[] nestedWhereElements = nestedWhere.split( "AND" );

            aliasBasedFilterList.addAll( getAliasBasedFilterList( aliasList,
                Arrays.stream( nestedWhereElements ).collect( Collectors.toList() ) ) );

            psList.add( Arrays.stream( nestedWhereElements ).filter( el -> el.contains( "ps" ) )
                .map( el -> "and " + alias + "." + el.trim() ).collect( Collectors.joining() ) );

            String innerJoin = "inner join "
                + ((Table) ((PlainSelect) ((Select) nestedSelect).getSelectBody()).getFromItem()).getName() +
                " as " + alias + " on ax.pi = " + alias + ".pi";
            tableNames.add( innerJoin );
        }

        List<SelectItem> mainSelectCols = ((PlainSelect) ((Select) select).getSelectBody()).getSelectItems();
        parsedSqlStatement.setSelectColumns( "select " + mainSelectCols.stream()
            .map( c -> {
                if ( !c.toString().contains( ".pi" ) )
                {
                    return c.toString().replace( "pi", "ax.pi" );
                }
                else
                {
                    return c.toString();
                }
            } ).collect( Collectors.joining( "," ) ) );

        String[] timeConditions = { "daily", "weekly", "biweekly", "monthly", "bimonthly", "quarterly",
            "weeklywednesday", "weeklythursday", "weeklysaturday", "weeklysunday",
            "sixmonthly", "sixmonthlyapril", "sixmonthlynov",
            "financialapril", "financialjuly", "financialoct", "financialnov" };

        for ( String timeCondition : timeConditions )
        {
            String condition = getTimeCondition( aliasList.get( 0 ), mainSelectCols, timeCondition );
            if ( condition != null && !condition.trim().isEmpty() )
            {
                parsedSqlStatement.getWhereConditions().add( condition );
            }
        }

        parsedSqlStatement.getWhereConditions()
            .add( "where " + aliasList.get( 0 ) + "." + "\"" + aliasList.get( 0 ) + "\"" +
                "is not null" );
        parsedSqlStatement.getWhereConditions()
            .add( "and " + aliasList.get( 0 ) + "." + Arrays.stream( ands ).filter( and -> and.contains( "uidlevel" ) )
                .map( and -> and.replace( "(", "" ).replace( ")", "" ) ).collect( Collectors.joining() ) );

        parsedSqlStatement.getWhereConditions().addAll( psList );
        parsedSqlStatement.getWhereConditions().addAll( getYearlies( aliasList, years ) );
        parsedSqlStatement.getWhereConditions().addAll( aliasBasedFilterList );
        parsedSqlStatement.getWhereConditions().addAll( coalesceBasedFilterList );
        parsedSqlStatement
            .setFrom( "from " + (((PlainSelect) ((Select) select).getSelectBody()).getFromItem()).toString() );
        parsedSqlStatement.getInnerJoins().addAll( tableNames.stream().distinct().collect( Collectors.toList() ) );
        return parsedSqlStatement;
    }

    private static String getTimeCondition( String alias, List<SelectItem> mainSelectCols, String period )
    {
        Optional<String> day = mainSelectCols.stream()
            .filter( c -> c.toString().toLowerCase().contains( " " + period ) )
            .map( c -> c.toString().toLowerCase().replace( "as " + period, "" ).trim() ).findFirst();
        return day.map( s -> "and " + alias + "." + period + " = " + s ).orElse( null );
    }

    private static List<String> getAliasBasedFilterList( List<String> aliasList, List<String> nestedWhereElements )
    {
        return aliasList.stream().map( al -> {
            if ( nestedWhereElements.stream().anyMatch( nwe -> nwe.contains( "\"" + al + "\" =" ) ) )
            {
                return "and " + al + "." + nestedWhereElements.stream()
                    .filter( nwe -> nwe.contains( "\"" + al + "\" =" ) ).collect( Collectors.joining() );
            }
            return "";
        } ).filter( r -> !r.isEmpty() ).collect( Collectors.toList() );
    }

    private static List<String> getYearlies( List<String> aliasList, List<String> years )
    {
        return aliasList.stream().map( a -> "and " + a + ".yearly in('" +
            years.stream().distinct().map( Object::toString ).collect( Collectors.joining( "','" ) )
            + "')" ).collect( Collectors.toList() );
    }

    private static String getDate( String sqlSnippet, String delimiter )
    {
        String[] tokens = sqlSnippet.split( delimiter );
        Optional<String> date = Arrays.stream( tokens ).filter( s -> !s.contains( "select" ) )
            .filter( s -> s.contains( "as date" ) ).findFirst();

        if ( date.isPresent() )
            try
            {
                LocalDate ld = LocalDate.parse( date.map( s -> s.replace( "cast(", "" )
                    .replace( "as date", "" ).replace( "'", "" ).trim() ).orElse( null ) );
                if ( ld.getMonthValue() == 1 && ld.getDayOfYear() == 1 && "<".equals( delimiter ) )
                {
                    return null;
                }
                return Integer.toString( ld.getYear() );
            }
            catch ( DateTimeParseException e )
            {
                return null;
            }

        return null;
    }

    private static boolean hasToBeTransformed( String sql )
    {
        return sql.toLowerCase().lastIndexOf( "where" ) != sql.toLowerCase().indexOf( "where" );
    }
}
