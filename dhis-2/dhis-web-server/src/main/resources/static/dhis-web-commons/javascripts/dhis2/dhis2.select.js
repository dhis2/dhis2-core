/*
 * Copyright (c) 2004-2013, University of Oslo
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

dhis2.util.namespace( 'dhis2.select' );

/**
 * Return a hidden select with id $select + '_ghost'. This is usually used for
 * temporary hiding options, since these can't be hidden using 'display: none'
 * or other similar techniques.
 * 
 * @param $select {jQuery} The select to ghost
 * 
 * @returns The ghost for a given select
 */
dhis2.select.getGhost = function( $select )
{
    var select_ghost_id = $select.attr( 'id' ) + '_ghost';
    var $select_ghost = $( '#' + select_ghost_id );

    if ( $select_ghost.size() === 0 )
    {
        $select_ghost = $( '<select id="' + select_ghost_id + '" multiple="multiple"></select>' );
        $select_ghost.hide();
        $select_ghost.appendTo( 'body' );
    }

    return $select_ghost;
};

/**
 * Filter a select on a given key. Options that are not matched, are moved to
 * ghost.
 * 
 * NOTE: Both selects should already be in sorted order.
 * 
 * @param $select {jQuery} The select to search within
 * @param key {String} Key to search for
 * @param caseSensitive {Boolean} Case sensitive search (defaults to false, so
 *            this parameter only needed if you want case sensitive search)
 */
dhis2.select.filterWithKey = function( $select, key, caseSensitive )
{
    $select_ghost = dhis2.select.getGhost( $select );
    caseSensitive = caseSensitive || false;

    if ( key.length === 0 )
    {
        dhis2.select.moveSorted( $select, $select_ghost.children() );
    }
    else
    {
        var $select_options = $select.children();
        var $select_ghost_options = $select_ghost.children();
        var $select_ghost_matched;
        var $select_not_matched;

        if ( caseSensitive )
        {
            $select_ghost_matched = $select_ghost_options.filter( dhis2.util.jqTextFilterCaseSensitive( key, false ) );
            $select_not_matched = $select_options.filter( dhis2.util.jqTextFilterCaseSensitive( key, true ) );
        }
        else
        {
            $select_ghost_matched = $select_ghost_options.filter( dhis2.util.jqTextFilter( key, false ) );
            $select_not_matched = $select_options.filter( dhis2.util.jqTextFilter( key, true ) );
        }

        dhis2.select.moveSorted( $select_ghost, $select_not_matched );
        dhis2.select.moveSorted( $select, $select_ghost_matched );
    }
};

/**
 * Moves an array of child elements into a select, these will be moved in a
 * sorted fashion. Both the select and array is assumed to be sorted to start
 * with.
 * 
 * @param $select {jQuery} A select which acts as the target
 * @param $array {jQuery} An array of child elements to move
 */
dhis2.select.moveSorted = function( $select, $array )
{
    if ( $select.children().size() === 0 )
    {
        $select.append( $array );
    }
    else
    {
        var array = $array.get();
        var array_idx = 0;
        var current = array.shift();
        var $children = $select.children();

        while ( current !== undefined )
        {
            var $current = $( current );

            if ( dhis2.comparator.htmlNoCaseComparator( $children.eq( array_idx ), $current ) > 0 )
            {
                $( current ).insertBefore( $children.eq( array_idx ) );
                current = array.shift();
            }
            else
            {
                array_idx++;
            }

            if ( $children.size() < array_idx )
            {
                break;
            }
        }

        if ( current !== undefined )
        {
            $select.append( current );
        }

        $select.append( array );
    }
};

/**
 * Moves an array of child elements into a select.
 * 
 * @param $select {jQuery} Select which acts as the target
 * @param $array An array of child elements to move
 */
dhis2.select.move = function( $select, $array )
{
    $select.append( $array );
};

/**
 * Mark all options in a select as selected.
 * 
 * @param $select {jQuery} The select
 */
dhis2.select.selectAll = function( $select )
{
    $select.children().attr( 'selected', true );
};

/**
 * Mark all options as not selected.
 * 
 * @param $select {jQuery} The select
 */
dhis2.select.selectNone = function( $select )
{
    $select.children().attr( 'selected', false );
};

/**
 * Sort options in a select. Based on their html() content. This version is case
 * sensitive.
 * 
 * @param $options Array of the options to sort
 * 
 * @return Sorted array of options
 */
dhis2.select.sort = function( $options )
{
    return $.makeArray( $options ).sort( function( a, b )
    {
        return dhis2.comparator.htmlComparator( $( a ), $( b ) );
    } );
};

/**
 * Sort options in a select. Based on their html() content. This version is case
 * insensitive
 * 
 * @param $options Array of the options to sort
 * 
 * @return Sorted array of options
 */
dhis2.select.sortNC = function( $options )
{
    return $( $.makeArray( $options ).sort( function( a, b )
    {
        return dhis2.comparator.htmlNoCaseComparator( $( a ), $( b ) );
    } ) );
};
