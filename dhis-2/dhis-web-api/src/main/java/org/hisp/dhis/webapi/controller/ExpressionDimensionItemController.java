/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItemService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Dusan Bernat
 */

/**
 *
 */
@Controller
@RequestMapping( value = "/expressionDimensionItems" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@AllArgsConstructor
public class ExpressionDimensionItemController
{
    private final ExpressionDimensionItemService expressionService;

    /**
     *
     * @param descriptionFilter
     * @return
     */
    @ResponseBody
    @GetMapping( produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<List<ExpressionDimensionItem>> getExpressionDimensionItems(
        @RequestParam( required = false ) String descriptionFilter )
    {
        try
        {
            List<ExpressionDimensionItem> expressions = new ArrayList<>();

            if ( descriptionFilter == null )
            {
                expressions.addAll( expressionService.getAllExpressionDimensionItems() );
            }
            else
            {
                expressions.addAll( expressionService.getAllExpressionDimensionItems()
                    .stream().filter( item -> item.getDescription().contains( descriptionFilter ) )
                    .collect( Collectors.toList() ) );
            }
            if ( expressions.isEmpty() )
            {
                return new ResponseEntity<>( HttpStatus.NO_CONTENT );
            }

            return new ResponseEntity<>( expressions, HttpStatus.OK );
        }
        catch ( Exception e )
        {
            return new ResponseEntity<>( null, HttpStatus.INTERNAL_SERVER_ERROR );
        }
    }

    /**
     *
     * @param id
     * @return
     */
    @ResponseBody
    @GetMapping( path = "{id}", produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<ExpressionDimensionItem> getExpressionDimensionItems( @PathVariable Long id )
    {
        try
        {
            ExpressionDimensionItem item = expressionService.getExpressionDimensionItem( id );
            return item != null ? new ResponseEntity<>( item, HttpStatus.OK )
                : new ResponseEntity<>( HttpStatus.NO_CONTENT );

        }
        catch ( Exception e )
        {
            return new ResponseEntity<>( null, HttpStatus.INTERNAL_SERVER_ERROR );
        }
    }

    /**
     *
     * @param expressionDimensionItem
     * @return
     */
    @ResponseBody
    @PostMapping( produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<ExpressionDimensionItem> createExpressionDimensionItem(
        @RequestBody ExpressionDimensionItem expressionDimensionItem )
    {
        try
        {
            expressionService.addExpressionDimensionItem( expressionDimensionItem );

            return new ResponseEntity<>( expressionDimensionItem, HttpStatus.CREATED );
        }
        catch ( Exception e )
        {
            return new ResponseEntity<>( null, HttpStatus.INTERNAL_SERVER_ERROR );
        }
    }

    /**
     *
     * @param expressionDimensionItem
     * @return
     */
    @ResponseBody
    @PutMapping( produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<ExpressionDimensionItem> updateExpressionDimensionItem(
        @RequestBody ExpressionDimensionItem expressionDimensionItem )
    {
        try
        {
            expressionService.updateExpressionDimensionItem( expressionDimensionItem );

            return new ResponseEntity<>( expressionDimensionItem, HttpStatus.OK );
        }
        catch ( Exception e )
        {
            return new ResponseEntity<>( null, HttpStatus.INTERNAL_SERVER_ERROR );
        }
    }

    /**
     *
     * @param id
     * @return
     */
    @ResponseBody
    @DeleteMapping( path = "{id}", produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<ExpressionDimensionItem> deleteExpressionDimensionItem( @PathVariable Long id )
    {
        try
        {
            ExpressionDimensionItem item = expressionService.getExpressionDimensionItem( id );

            if ( item == null )
            {
                return new ResponseEntity<>( null, HttpStatus.NOT_FOUND );
            }

            expressionService.deleteExpressionDimensionItem( item );

            return new ResponseEntity<>( item, HttpStatus.OK );
        }
        catch ( Exception e )
        {
            return new ResponseEntity<>( null, HttpStatus.INTERNAL_SERVER_ERROR );
        }
    }
}
