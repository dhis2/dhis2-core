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

dhis2.util.namespace( 'dhis2.commons' );

dhis2.commons.getCurrentPage = function() {
	return $.cookie( "currentPage" );
}

dhis2.commons.getCurrentKey = function() {
	return $.cookie( "currentKey" );
}

dhis2.commons.redirectCurrentPage = function( url ) {
	var currentPage = dhis2.commons.getCurrentPage();
	var currentKey = dhis2.commons.getCurrentKey();
	var separator = url && url.indexOf( "?" ) == -1 ? "?" : "&";

    var redirect = url;

    if ( currentPage && currentKey ) {
        redirect = currentPage ? ( url + separator + "currentPage=" + currentPage + "&key=" + currentKey ) : url;
    } 
    else if ( currentPage ) {
        redirect = currentPage ? ( url + separator + "currentPage=" + currentPage ) : url;
    }

    window.location.href = redirect;
};

// -----------------------------------------------------------------------------
// Global variables
// -----------------------------------------------------------------------------

var headerMessageTimeout = -1;
var _loading_bar_html = "<span id='loaderSpan'><img src='../images/ajax-loader-bar.gif'></span>";
var _loading_circle_html = "<span id='loaderSpan'><img src='../images/ajax-loader-circle.gif'></span>";

/**
 * Go back using the document.referrer.
 * 
 * @param defaultUrl if there is not document.referrer, use this url
 */
function referrerBack( defaultUrl ) 
{
	if ( document.referrer !== undefined && document.referrer != "" ) 
	{
		if ( document.referrer.indexOf( "login.action" ) == -1 ) 
		{
			location.href = document.referrer;
			return;
		}
	}

	location.href = defaultUrl;
}

/**
 * Redirects to the translate GUI.
 *
 * @param context Context vars from context menu
 */
function translateWithContext( context ) 
{
  translate( context.type, context.uid );
}

/**
 * Redirects to the translate GUI.
 *
 * @param className the name of the object class.
 * @param objectId the identifier of the object.
 */
function translate( className, uid )
{
    var url = "../dhis-web-commons/i18n.action?className=" + className + "&uid=" + uid + "&returnUrl=" + htmlEncode( window.location.href );
    
    window.location.href = url; 
}

/**
 * Scrolls the view port to the bottom of the document.
 */
function scrollToBottom()
{
	var scrollTop = parseInt( $( document ).height() - $( window ).height() );
	
	if ( scrollIsRelevant() )
	{
		$( document ).scrollTop( scrollTop );
	}
}

/**
 * Scrolls the view port to the top of the document.
 */
function scrollToTop()
{
	$( document ).scrollTop( 0 );
}

/**
 * Indicates whether there is a need for scrolling.
 */
function scrollIsRelevant()
{
	var scrollTop = parseInt( $( document ).height() - $( window ).height() );	
	var relevant = ( scrollTop > 0 );
	return relevant;
}

/**
 * Joins the names of the given array of objects and returns it as a single string.
 */
function joinNameableObjects( objects )
{
	var string = "";
	var size = objects.length;
	
	for ( var i in objects )
	{
		string += objects[i].name;
		
		if ( i < ( size - 1 ) )
		{
			string += ", ";
		}
	}
	
	return string;
}

/**
 * Gets help content for the given id. Opens the right bar and puts the content
 * inside. Reads data from an underlying docbook file.
 * 
 * @param id the content id, refers to the section id in the docbook file.
 */
function getHelpContent( id )
{
    $.get( 
       '../dhis-web-commons-about/getHelpContent.action',
       { "id": id },
       function( data )
       {
           $( "div#rightBar" ).fadeIn();
           $( "div#rightBarContents" ).html( data );
       } );
}

/**
 * Hides the help content.
 */
function hideHelpContent()
{
	$( "div#rightBar" ).fadeOut();
}

/**
 * Filters values in a html table with tbody id "list".
 * 
 * @param filter the filter.
 */
function filterValues( filter, columnIndex )
{
	if( columnIndex==undefined ) columnIndex = 1;
	
    var list = document.getElementById( 'list' );
    
    var rows = list.getElementsByTagName( 'tr' );
    
    for ( var i = 0; i < rows.length; ++i )
    {
        var cell = rows[i].getElementsByTagName( 'td' )[columnIndex - 1];
        
        var value = cell.firstChild.nodeValue;

        if ( value.toLowerCase().indexOf( filter.toLowerCase() ) != -1 )
        {
            rows[i].style.display = 'table-row';
        }
        else
        {
            rows[i].style.display = 'none';
        }
    }
}

/**
 * Returns the value of the selected option in the list with the given
 * identifier.
 * 
 * @param listId the list identifier.
 */
function getListValue( listId )
{
    var list = document.getElementById( listId );
    var value = list.options[ list.selectedIndex ].value;
    
    return value;
}

/**
 * Hides the document element with the given identifier.
 * 
 * @param id the element identifier.
 */
function hideById( id )
{
    jQuery("#" + id).hide();
}

/**
 * Shows the document element with the given identifier.
 * 
 * @param id the element identifier.
 */
function showById( id )
{
    jQuery("#" + id).show();
}

/**
 * Returns true if the element with the given identifier has text, false if not.
 * 
 * @param inputId the identifier of the input element.
 */
function hasText( inputId )
{
    return trim( getFieldValue( inputId ) ) != "";
}

function trim( string )
{
	return jQuery.trim( string );
}

/**
 * Returns true if the element with the given identifier is checked, false if
 * not or if it does not exist.
 * 
 * @param checkboxId the identifier of the checkbox element.
 */
function isChecked( checkboxId )
{
	return jQuery( "#" + checkboxId ).length && jQuery( "#" + checkboxId ).is( ":checked" );
}

/**
 * Toggles the checked property of checkbox elements.
 */
function toggleChecked( checkBoxId )
{
	var box = jQuery( "#" + checkBoxId );
	
	if ( box.length )
	{
		box.prop( "checked", !box.is( ":checked" ) );
	}
}

/**
 * Checks the checkbox with the given identifier if the checkbox exists.
 */
function check( checkBoxId )
{
    jQuery( "#" + checkBoxId ).prop( "checked", true );
}

/**
 * Unchecks the checkbox with the given identifier if the checkbox exists.
 */
function uncheck( checkBoxId )
{
    jQuery( "#" + checkBoxId ).prop( "checked", false );
}

/**
 * Enables the element with the given identifier if the element exists.
 */
function enable( elementId )
{
	var hasDatePicker = jQuery("#" + elementId ).data("datepicker");
	if( hasDatePicker == undefined)
	{
		jQuery( "#" + elementId ).removeAttr( "disabled" );
	}
	else
	{
		jQuery('#' + elementId ).datepicker( "enable" );
	} 
}

function enableGroup( selectorJQueryString )
{
	jQuery.each( jQuery( selectorJQueryString ), function( i, item ){
		item.disabled = false;
	});
}

/**
 * Disables the element with the given identifier if the element exists.
 */
function disable( elementId )
{
	var hasDatePicker = jQuery("#" + elementId ).data("datepicker");
	if( hasDatePicker == undefined)
	{
		jQuery( "#" + elementId ).attr("disabled", true );
	}
	else
	{
		jQuery('#' + elementId ).datepicker( "disable" );
	}    
}

function disableGroup( selectorJQueryString )
{
	jQuery.each( jQuery( selectorJQueryString ), function( i, item ){		
		item.disabled = true;
	});
}

/**
 * Enables the element with the given identifier if the element exists in parent
 * window of frame.
 */
function enableParent( elementId )
{
    var element = parent.document.getElementById( elementId );
    
    if ( element )
    {
        element.disabled = false;
    }
}

/**
 * Disables the element with the given identifier if the element exists in
 * parent window of frame.
 */
function disableParent( elementId )
{
    var element = parent.document.getElementById( elementId );
    
    if ( element )
    {
        element.disabled = true;
    }
}

/**
 * Returns true if the element with the given identifier has selected elements
 * associated with it, false if not.
 * 
 * @param listId the identifier of the list element.
 */
function hasElements( listId )
{
    return jQuery( "#" + listId ).children().length > 0;
}

/**
 * Returns true if the given value not equals null or "null" string, false if not.
 * 
 * @param value the given value.
 */
function isNotNull( value )
{
    return ( value != null && trim(value) != "null" );
}

/**
 * Returns true if the element with the given identifier exists, false if not.
 * 
 * @param elementId the identifier of the element.
 */
function isNotEmpty( elementId )
{
    return jQuery("#" + elementId).length == 1;
}

/**
 * HTML encodes the given string.
 * 
 * @param str the input string.
 * @return the HTML encoded string.
 */
function htmlEncode( str )
{
    str = str.replace( /\%/g, "%25" ); // This line must come first so the %
										// doesn't get overwritten later
    str = str.replace( /\ /g, "%20" );
    str = str.replace( /\!/g, "%21" );
    str = str.replace( /\"/g, "%22" );
    str = str.replace( /\#/g, "%23" );
    str = str.replace( /\$/g, "%24" );
    str = str.replace( /\&/g, "%26" );
    str = str.replace( /\'/g, "%27" );
    str = str.replace( /\(/g, "%28" );
    str = str.replace( /\)/g, "%29" );
    str = str.replace( /\*/g, "%2a" );
    str = str.replace( /\+/g, "%2b" );
    str = str.replace( /\,/g, "%2c" );
    str = str.replace( /\-/g, "%2d" );
    str = str.replace( /\./g, "%2e" );
    str = str.replace( /\//g, "%2f" );
    str = str.replace( /\:/g, "%3a" );
    str = str.replace( /\;/g, "%3b" );
    str = str.replace( /\</g, "%3c" );
    str = str.replace( /\=/g, "%3d" );
    str = str.replace( /\>/g, "%3e" );
    str = str.replace( /\?/g, "%3f" );
    str = str.replace( /\@/g, "%40" );
    
    return str;
}

/**
 * Gets the value for the element with the given name from the DOM object.
 * 
 * @param parentElement the DOM object.
 * @param childElementName the name of the element.
 */
function getElementValue( parentElement, childElementName )
{
    var textNode = parentElement.getElementsByTagName( childElementName )[0].firstChild;
    
    return textNode ? textNode.nodeValue : null;
}

/**
 * Gets the attribute value from the given DOM element.
 * 
 * @param parentElement the DOM object.
 * @param attributeName the name of the attribute.
 */
function getElementAttribute( parentElement, childElementName, childAttributeName )
{
	var textNode = parentElement.getElementsByTagName( childElementName )[0];
    
    return textNode ? textNode.getAttribute( childAttributeName ) : null; 
}

/**
 * Gets the value from the given DOM element.
 * 
 * @param rootElement the DOM object.
 */
function getRootElementValue( rootElement )
{
    var textNode = rootElement.firstChild;
    
    return textNode ? textNode.nodeValue : null;
}

/**
 * Gets the value of the attribute with the given name from the given DOM
 * element.
 * 
 * @param rootElement the DOM object.
 * @param attributeName the name of the attribute.
 */
function getRootElementAttribute( rootElement, attributeName )
{
   return rootElement.getAttribute( attributeName );
}

/**
 * Sets the text (HTML is not interpreted) on the given element.
 * 
 * @param fieldId the identifier of the element.
 * @param txt the text to set.
 */
function setText( fieldId, txt )
{
    jQuery("#" + fieldId).text( txt );
}

/**
 * Sets a value on the given element.
 * 
 * @param fieldId the identifier of the element.
 * @param value the value to set.
 */
function setInnerHTML( fieldId, value )
{
    jQuery("#" + fieldId).html( value );
}

/**
 * Gets a value from the given element and HTML encodes it.
 * 
 * @param fieldId the identifier of the element.
 * @return the HTML encoded value of the element with the given identifier.
 */
function getInnerHTML( fieldId )
{
    return jQuery("#" + fieldId).html();
}

/**
 * Sets a value on the given element.
 * 
 * @param fieldId the identifier of the element.
 * @param value the value to set.
 */
function setFieldValue( fieldId, value )
{
    jQuery("#" + fieldId).val( value );
}

/**
 * Gets a value from the given element and HTML encodes it.
 * 
 * @param fieldId the identifier of the element.
 * @return the HTML encoded value of the element with the given identifier.
 */
function getFieldValue( fieldId )
{
	if ( getTypeById( fieldId, 'multiple' ) )
	{
		var selectedItem = jQuery("#" + fieldId).children( "option:selected" )[0];
		
		return (selectedItem ? selectedItem.value : selectedItem);
	}

	return jQuery("#" + fieldId).val();
}

/**
 * Gets a value from the given element and HTML encodes it.
 * 
 * @param fieldId the identifier of the element.
 * @return the type of the element with the given identifier.
 */
function getTypeById( fieldId, attribute )
{
	return jQuery("#" + fieldId).attr( attribute );
}

/**
 * get value of input radio with name
 * 
 * @param radioName name of input radio
 */
function getRadioValue( radioName )
{
	result = null;
	jQuery.each( jQuery( "input[type=radio][name=" + radioName + "]") , function( i, item )
	{		
		if ( item.checked )
		{
			result = item.value;
		}
	});
	
	return result;
}

/**
 * set value for input radio with name
 * 
 * @param radioName name of input radio
 */
function setRadioValue( radioName, value )
{
	jQuery.each( jQuery( "input[type=radio][name=" + radioName + "]") , function( i, item )
	{		
		if ( item.value == value )
		{
			item.checked = true;
		}
	});	
}

// -----------------------------------------------------------------------------
// Message, warning, info, headerMessage
// -----------------------------------------------------------------------------

/**
 * Shows the message span and sets the message as text.
 * 
 * @param message the message.
 */
function setMessage( message )
{
	if ( message )
	{
		$( '#message' ).html( message ).show();
	}
}

/**
 * Shows the message span and sets the message as text together with a wait
 * animation.
 * 
 * @param message the message.
 */
function setWaitMessage( message )
{
	setMessage( message + "&nbsp;&nbsp;&nbsp;" + _loading_bar_html );
}

/**
 * Makes the "message" span invisible.
 */
function hideMessage()
{
	$( '#message' ).hide();
}

/**
 * Slides down the header message div and sets the message as text.
 * 
 * @param message the message.
 */
function setHeaderMessage( message )
{
	window.clearTimeout( headerMessageTimeout );
	
    $( '#headerMessage' ).html( message );
    
    if ( isHeaderMessageHidden() )
    {
    	$( '#headerMessage' ).fadeIn( 'fast' );
    }
}

/**
 * Slides down the header message div and sets the message as text together with
 * a wait animation.
 * 
 * @param message the message.
 */
function setHeaderWaitMessage( message )
{
	window.clearTimeout( headerMessageTimeout );
	
	$( 'div#headerMessage' ).html( message + "&nbsp;&nbsp;&nbsp;" + _loading_bar_html );
	
	if ( isHeaderMessageHidden() )
	{
    	$( 'div#headerMessage' ).fadeIn( 'fast' );
	}
}

/**
 * Sets the header message and hides it after 3 seconds.
 */
function setHeaderDelayMessage( message )
{
	setHeaderMessage( message );
	
	window.clearTimeout( headerMessageTimeout );
	
	headerMessageTimeout = window.setTimeout( "hideHeaderMessage();", 7000 );
}

/**
 * Sets the header wait message and hides it after 3 seconds.
 */
function setHeaderWaitDelayMessage( message )
{
	setHeaderWaitMessage( message );
	
	window.clearTimeout( headerMessageTimeout );
	
	headerMessageTimeout = window.setTimeout( "hideHeaderMessage();", 7000 );
}

/**
 * Hides the header message div.
 */
function hideHeaderMessage()
{
    $( '#headerMessage' ).fadeOut( 'fast' );
}   

/**
 * Indicates whether the header message is visible.
 */
function isHeaderMessageVisible()
{
    return $( '#headerMessage' ).is( ":visible" );
}

/**
 * Indicates whether the header message is not visible.
 */
function isHeaderMessageHidden()
{
    return !isHeaderMessageVisible();
}

/**
 * Slides down the info message div and sets the message as text.
 * 
 * @param message the message.
 */
function setInfo( message )
{
    $( '#info' ).html( message );
    $( '#info' ).fadeIn( 'fast' );
}

/**
 * Hides the info div.
 */
function hideInfo()
{
    $( '#info' ).fadeOut( 'fast' );
}

/**
 * Makes the "detailsArea" span visible.
 */
function showDetails()
{
	$( '#detailsData' ).css( 'width', '270px' );
    $( '#detailsArea' ).show();
}

/**
 * Makes the "detailsArea" span invisible.
 */
function hideDetails()
{
	$( '#detailsData' ).css( 'width', '0' );
    $( '#detailsArea' ).hide();
}

/**
 * Makes the "warningArea" span visible.
 */
function showWarning()
{
    $( '#warningArea' ).show();
}

/**
 * Makes the "warningArea" span invisible.
 */
function hideWarning()
{
    $( '#warningArea' ).hide();
}

/**
 * Convenience method for getting a document element.
 * 
 * @param id id of the element to get.
 */
function byId( elementId )
{
    return document.getElementById( elementId );
}

// -----------------------------------------------------------------------------
// Supportive functions
// -----------------------------------------------------------------------------

/**
 * Toggles visibility for an element.
 * 
 * @param id the identifier of the element.
 * @param display boolean indicator.
 */
function toggleByIdAndFlag( id, display )
{
    var node = byId( id );
    
    if ( !node )
    {
        return;
    }
    
    node.style.display = ( display ? 'block' : 'none' );
}

/**
 * Toggles visibility for an element.
 * 
 * @param id the identifier of the element.
 */
function toggleById( id )
{
    var node = byId( id );

    if ( !node )
    {
        return;
    }
    
    var display = node.style.display;
    
    node.style.display = ( display == 'none' || display == '' ? 'block' : 'none' );
}

/**
 * Returns a query string with all element values in the select list and the
 * specified param.
 */
function getParamString( elementId, param )
{
    var result = "";
	var list = jQuery( "#" + elementId ).children();
	
	list.each( function( i, item ){
		result += param + "=" + item.value;
		result += (i < list.length-1) ? "&" : "";
	});
	
	return result;
}

/**
 * @deprecated Use addOptionById
 */
function addOptionToList( list, optionValue, optionText )
{
    var option = document.createElement( "option" );
    option.value = optionValue;
    option.text = optionText;
    list.add( option, null );
}

/**
 * Returns a query string on the form <paramName>=<listValue> based on the
 * options in the list with the given identifier.
 * 
 * @param listId the list identifier.
 * @param paramName the name of the query param.
 * @return a query string.
 */
function getQueryStringFromList( listId, paramName )
{
	var list = document.getElementById( listId );
	
	var params = "";
	
	for ( var i = 0; i < list.options.length; i++ )
	{
		list.options[i].selected = "selected";
		params += paramName + "=" + list.options[i].value + "&";
	}
	
	return params;
}

/**
 * Shows loader div and hides content div.
 */
function showLoader()
{
    $( "div#loaderDiv" ).show();
    $( "div#contentDiv" ).hide();
}

/**
 * Hides loader div and shows content div.
 */
function hideLoader()
{
    $( "div#loaderDiv" ).hide();
    $( "div#contentDiv" ).show();
}

/**
 * Deletes and removes an item from a table. The table row to be removed must
 * have an identifier on the form "tr[itemId]".
 * 
 * @param itemId the item identifier.
 * @param itemName the item name.
 * @param message the confirmation message.
 * @param action the server action url for deleting the item.
 */
function removeItem( itemId, itemName, confirmation, action, success )
{                
    var result = window.confirm( confirmation + "\n\n" + itemName );
    
    if ( result )
    {
    	setHeaderWaitMessage( i18n_deleting );
    	
    	$.postJSON(
    	    action,
    	    {
    	        "id": itemId   
    	    },
    	    function( json )
    	    { 
    	    	if ( json.response == "success" )
    	    	{
					jQuery( "tr#tr" + itemId ).remove();
	                
					jQuery( "table.listTable tbody tr" ).removeClass( "listRow listAlternateRow" );
	                jQuery( "table.listTable tbody tr:odd" ).addClass( "listAlternateRow" );
	                jQuery( "table.listTable tbody tr:even" ).addClass( "listRow" );
					jQuery( "table.listTable tbody" ).trigger("update");
  
					if ( success && typeof( success) == "function" )
					{
						success.call();
					}
  
					setHeaderDelayMessage( i18n_delete_success );
    	    	}
    	    	else if ( json.response == "error" )
    	    	{ 
					setHeaderMessage( json.message );
    	    	}
    	    }
    	);
    }
}

/**
 * Create jQuery datepicker for input text with id * *
 * 
 * @param id the id of input field which you want enter date *
 */
function datePicker( id, hideDel )
{
	$("#" + id).datepicker(
	{
		dateFormat: dateFormat,
		changeMonth: true,
		changeYear: true,
		monthNamesShort: monthNames,
		dayNamesMin: dayNamesMin,
		showOn: 'both',
		createButton: false,
		constrainInput: true,
        yearRange: '-100:+100'
	});
	jQuery( "#" + id ).attr("readonly", true );
	jQuery( ".ui-datepicker-trigger").hide();
	
	addRemoveDateButton( id, hideDel );
	
	s = jQuery("#" + id );		
	if( s.val()=='' ) s.val( getCurrentDate() );		
}

function datePicker( id, today, hideDel )
{
	$("#" + id).datepicker(
	{
		dateFormat: dateFormat,
		changeMonth: true,
		changeYear: true,
		monthNamesShort: monthNames,
		dayNamesMin: dayNamesMin,
		createButton: false,
		constrainInput: true,
        yearRange: '-100:+100'
	});
	jQuery( "#" + id ).attr("readonly", true );
	jQuery( ".ui-datepicker-trigger").hide();
	addRemoveDateButton( id, hideDel );
	
	if( today == undefined ) today = false;
	
	if( today ){
		s = jQuery("#" + id );
		if( s.val()=='' ) s.val( getCurrentDate() );
	}
	
	addRemoveDateButton( id, hideDel );
}

function datePickerjQuery( jQueryString, hideDel )
{
	jQuery( jQueryString ).datepicker(
	{
		dateFormat: dateFormat,
		changeMonth: true,
		changeYear: true,
		monthNamesShort: monthNames,
		dayNamesMin: dayNamesMin,
		buttonImageOnly: true,
		constrainInput: true,
        yearRange: '-100:+100'
	});		
	jQuery( "#" + id ).attr("readonly", true );
	jQuery( ".ui-datepicker-trigger").hide();
	addRemoveDateButton( id, hideDel );
}

/**
 * Create jQuery datepicker for input text with id * *
 * 
 * @param id the id of input field which you want enter date *
 */
function datePickerValid( id, today, hideDel )
{
	jQuery("#" + id).datepicker(
	{
		dateFormat: dateFormat,
		changeMonth: true,
		changeYear: true,
		monthNamesShort: monthNames,
		dayNamesMin: dayNamesMin,
		createButton: false,
		maxDate: '+0d +0w',
		constrainInput: true,
        yearRange: '-100:+100'
	});
	jQuery( "#" + id ).attr("readonly", true );
	jQuery( ".ui-datepicker-trigger").hide();
	addRemoveDateButton( id, hideDel );

	if ( today == undefined )
	{
		today = false;
	}
	
	if( today )
	{
		s = jQuery("#" + id );
		if( s.val()=='' ) s.val( getCurrentDate() );
	}
}

function datePickerFuture( id, today, hideDel )
{
	jQuery("#" + id).datepicker(
	{
		dateFormat: dateFormat,
		changeMonth: true,
		changeYear: true,
		monthNamesShort: monthNames,
		dayNamesMin: dayNamesMin,
		createButton: false,
		minDate: '+0d +0w',
		constrainInput: true,
        yearRange: '-100:+100'
	});
	jQuery( "#" + id ).attr("readonly", true );
	jQuery( ".ui-datepicker-trigger").hide();
	addRemoveDateButton( id, hideDel );
	
	if ( today == undefined )
	{
		today = false;
	}
	
	if( today )
	{
		s = jQuery("#" + id );
		if( s.val()=='' ) s.val( getCurrentDate() );
	}
}

/**
 * Create jQuery datepicker for start date and end date text with id
 * 
 * @param startdate the id of input field which you want enter start date *
 * @param enddate the id of input field which you want enter end date *
 */
function datePickerInRange ( startdate, enddate, setCurrentStartDate, setCurrentEndDate, hideDel )
{
	if( setCurrentStartDate == undefined ) setCurrentStartDate = true;
	if( setCurrentEndDate == undefined ) setCurrentEndDate = true;
	
	s = jQuery("#" + startdate );
	e = jQuery("#" + enddate );
	if( setCurrentStartDate && s.val()=='') s.val( getCurrentDate() );
	if( setCurrentEndDate && e.val()=='' ) e.val( getCurrentDate() );

	var dates = $('#'+startdate+', #' + enddate).datepicker(
	{
		dateFormat: dateFormat,
		defaultDate: "+1w",
		changeMonth: true,
		changeYear: true,
		numberOfMonths: 1,
		monthNamesShort: monthNames,
		dayNamesMin: dayNamesMin,
		showAnim: '',
		createButton: false,
		constrainInput: true,
        yearRange: '-100:+100',
		onSelect: function(selectedDate)
		{
			var option = this.id == startdate ? "minDate" : "maxDate";
			var instance = $(this).data("datepicker");
			var date = $.datepicker.parseDate(instance.settings.dateFormat || $.datepicker._defaults.dateFormat, selectedDate, instance.settings);
			dates.not(this).datepicker("option", option, date);
		}
	});

	jQuery( "#" + startdate ).attr("readonly", true );
	jQuery( "#" + enddate ).attr("readonly", true );
	jQuery( ".ui-datepicker-trigger").hide();
	addRemoveDateButton( startdate, hideDel );
	addRemoveDateButton( enddate, hideDel );
	
    $("#ui-datepicker-div").hide();
}

function datePickerInRangeValid( startdate, enddate, setCurrentStartDate, setCurrentEndDate, hideDel )
{
	if( setCurrentStartDate == undefined ) setCurrentStartDate = true;
	if( setCurrentEndDate == undefined ) setCurrentEndDate = true;
	
	s = jQuery("#" + startdate );
	e = jQuery("#" + enddate );
	if( setCurrentStartDate && s.val()=='') s.val( getCurrentDate() );
	if( setCurrentEndDate && e.val()=='' ) e.val( getCurrentDate() );

	var dates = $('#'+startdate+', #' + enddate).datepicker(
	{
		dateFormat: dateFormat,
		defaultDate: "+1w",
		changeMonth: true,
		changeYear: true,
		numberOfMonths: 1,
		maxDate: '+0d +0w',
		monthNamesShort: monthNames,
		dayNamesMin: dayNamesMin,
		showAnim: '',
		createButton: false,
		constrainInput: true,
        yearRange: '-100:+100',
		onSelect: function(selectedDate)
		{
			var option = this.id == startdate ? "minDate" : "maxDate";
			var instance = $(this).data("datepicker");
			var date = $.datepicker.parseDate(instance.settings.dateFormat || $.datepicker._defaults.dateFormat, selectedDate, instance.settings);
			dates.not(this).datepicker("option", option, date);
		}
	});

	jQuery( "#" + startdate ).attr("readonly", true );
	jQuery( "#" + enddate ).attr("readonly", true );
	jQuery( ".ui-datepicker-trigger").hide();
	addRemoveDateButton( startdate, hideDel );
	addRemoveDateButton( enddate, hideDel );
}

function addRemoveDateButton( id, hideDel )
{
	if( hideDel == undefined ) hideDel = true;
	if( !hideDel ){
		var removeId = 'delete_' + id;
		if( jQuery("#" + removeId).length == 0 )
		{
			jQuery("#" + id).after(function() {
			  return ' <img src="../images/calendar-delete.png" align="justify" id="'+ removeId +'" onclick="jQuery( \'#' + id + '\').val(\'\');jQuery( \'#' + id + '\').change();"> ';
			});
		}
	}
	jQuery( "#" + id).click(function() {
		$("#ui-datepicker-div").show();
	});
	jQuery( "#" + id).change(function() {
		$("#ui-datepicker-div").hide();
	});
}

function getCurrentDate()
{	
	return jQuery.datepicker.formatDate( dateFormat , new Date() ) ;
}

/**
 * Create input table id become sortable table * *
 * 
 * @param tableId the id of table you want to sort * *
 */

function tableSorter( tableId, sortList )
{
	if(sortList==undefined) sortList = [[0,0]];
	
	jQuery("#" + tableId ).tablesorter(); 
	
	if($("#" + tableId ).find("tbody").children().size() > 0 )
	{
		jQuery("#" + tableId ).trigger("sorton",[sortList]);
	}
}

function setSelectionRange( input, selectionStart, selectionEnd ) 
{
	if ( input.setSelectionRange ) 
	{
		input.focus();
	    input.setSelectionRange( selectionStart, selectionEnd );
	}
	else if ( input.createTextRange ) 
	{
	    var range = input.createTextRange();
	    range.collapse( true );
	    range.moveEnd( 'character', selectionEnd );
	    range.moveStart( 'character', selectionStart );
	    range.select();
	}
}

function setCaretToPos ( input, pos ) 
{
	setSelectionRange( input, pos, pos );
}

function insertTextCommon( inputAreaName, inputText )
{
	var inputArea = document.getElementById( inputAreaName );
	
	// IE support
	if ( document.selection ) 
	{
		inputArea.focus();
        sel = document.selection.createRange();
        sel.text = inputText;
        inputArea.focus();
	}
	// MOZILLA/NETSCAPE support
	else if ( inputArea.selectionStart || inputArea.selectionStart == '0' ) 
	{	
		var startPos = inputArea.selectionStart;
		var endPos = inputArea.selectionEnd;
		
		var existingText = inputArea.value;
		var textBefore = existingText.substring( 0, startPos );
		var textAfter = existingText.substring( endPos, existingText.length );

		inputArea.value = textBefore + inputText + textAfter;
	}
	else 
	{
		inputArea.value += inputText;
		inputArea.focus();
	}

	setCaretToPos( inputArea, inputArea.value.length );
}

// -----------------------------------------------------------------------------
// Form validation
// -----------------------------------------------------------------------------

/**
 * Create validator for fileds in form
 * 
 * This should replace validation() at some point, but theres just to much code
 * depending on the old version for now.
 * 
 * See http://bassistance.de/jquery-plugins/jquery-plugin-validation/ for more
 * information about jquery.validate.
 * 
 * @param formId form to validate
 * @param submitHandler the submitHandler to use
 * @param kwargs A dictionary of optional arguments, currently supported are:
 *        beforeValidateHandler, rules
 */
function validation2( formId, submitHandler, kwargs )
{
	var beforeValidateHandler = null;
	
	if ( isDefined( kwargs ) )
	{
		beforeValidateHandler = kwargs["beforeValidateHandler"];
	}

	var rules = kwargs["rules"];
	var validator = jQuery( "#" + formId ).validate( {
		meta: "validate",
        errorElement: "span",
        warningElement: "span",
		beforeValidateHandler: beforeValidateHandler,
		submitHandler: submitHandler,
		rules: rules,
		errorPlacement: function(error, element) {
			element.parent( "td" ).append( "<br>" ).append( error );
		}
	} );

	$( "#" + formId + " input" ).each( function( n )
	{
		try
		{
			$( this ).attr( "maxlength", rules[this.id].rangelength[1] );
		}
		catch( e )
		{}
	});

	var nameField = jQuery('#' + formId + ' :input')[0];
	
	if ( nameField )
	{
		nameField.focus();
	}
	
	return validator;	
}

/**
 * @param form Get validation rules for this form
 * 
 * @return Validation rules for a given form
 */
function getValidationRules( form ) 
{
	if ( form !== undefined ) {
		return validationRules[form];
	}
	
	return validationRules;
}

function validation( formId, submitHandler, beforeValidateHandler )
{
	var nameField = jQuery('#' + formId + ' :input')[0];

	var validator = jQuery("#" + formId ).validate({
		 meta:"validate"
		,errorElement:"span"
		,beforeValidateHandler:beforeValidateHandler
		,submitHandler: submitHandler
	});
	
	if ( nameField )
	{
		nameField.focus();
	}
	
	return validator;
}

/**
 * Add validation rule remote for input field
 * 
 * @param inputId is id for input field
 * @param url is ajax request url
 * @param params is array of param will send to server by ajax request
 */
function checkValueIsExist( inputId, url, params )
{
	jQuery("#" + inputId).rules("add",{
		remote: {
			url:url,
			type:'post',
			data:params
		}
	});
}

function checkPassword( inputId, password ) {
    var parameter = $("#" + inputId ).val();
    var passWord = $("#" +  password).val();
    if (passWord) {
        if (parameter) {
            if ((passWord.indexOf(parameter) !== -1) ||  (parameter.indexOf(passWord) !== -1)) {
                alert("Username/Email cannot be part of password");
                $("#" +  password).val("");
            }
        }
    }

}

function checkValueIsExistWarning( inputId, url, params )
{
	jQuery("#" + inputId).rules("add",{
		remoteWarning: {
			url:url,
			type:'post',
			data:params
		}
	});
}

function remoteValidateById( inputId, url, params )
{
	jQuery("#" + inputId).rules("add",{
		remote: {
			url:url,
			type:'post',
			data:params
		}
	});
}

function remoteValidate( input , url, params )
{
	jQuery( input ).rules("add",{
		remote: {
			url:url,
			type:'post',
			data:params
		}
	});
}

/**
 * Add any validator Rules for input
 * 
 * @param inputId is id for input field
 * @param rules is array of rule
 * @note: input field must have name same with id
 */
function addValidatorRulesById( inputId, rules )
{
	addValidatorRules( jQuery("#" + inputId), rules );
}

function addValidatorRules( input, rules )
{
	jQuery(input).rules( "add", rules );
}

/**
 * Remove validator Rules for input
 * 
 * @param inputId is id for input field
 * @param rules is array of rule you want to remove
 * @note: input field must have name same with id
 */
function removeValidatorRulesById( inputId, rules )
{
	removeValidatorRules( jQuery("#" + inputId), rules );
}

function removeValidatorRules( input, rules )
{
	jQuery(input).rules( "remove", rules );
}

function listValidator( validatorId, selectedListId )
{
	memberValidator = jQuery( "#" + validatorId );
	memberValidator.attr( 'multiple', 'multiple');
	memberValidator.attr( 'name', validatorId );
	memberValidator.children().remove();
	
	jQuery.each( jQuery( "#" + selectedListId ).children(), function(i, item){
		item.selected = 'selected';
		memberValidator.append( '<option value="' + item.value + '" selected="selected">' + item.value + '</option>');
	});
}

// -----------------------------------------------------------------------------
// Message
// -----------------------------------------------------------------------------

/**
 * Show message at the top-right of screen, this message will hide automatic
 * after 3 second if you don't set time
 * 
 * @param message is message
 * @param time is time the message will hide after showed. Default is 3 second,
 *            set time is 0 if you don't want to hide this message
 */
function showErrorMessage( message, time )
{
	jQuery.growlUI( i18n_error, message, 'error', time ); 	
}

/**
 * Show message at the top-right of screen, this message will hide automatic
 * after 3 second if you don't set time
 * 
 * @param message is message
 * @param time is time the message will hide after showed. Default is 3 second,
 *            set time is 0 if you don't want to hide this message
 */
function showSuccessMessage( message, time )
{
	jQuery.growlUI( i18n_success, message, 'success', time ); 	
}

/**
 * Show message at the top-right of screen, this message will hide automatic
 * after 3 second if you don't set time
 * 
 * @param message is message
 * @param time is time the message will hide after showed. Default is 3 second,
 *            set time is 0 if you don't want to hide this message
 */
function showWarningMessage( message, time )
{
	jQuery.growlUI( i18n_warning, message, 'warning', time ); 	
}

function markInvalid( elementId, message )
{	
	var element = jQuery("#" + elementId );
	html = '<span htmlfor="' + element.attr('name') + '" generated="true" style="font-style: italic; color: red;" class="error">' + message + '</span>';
	element.next().remove();	
	jQuery( html ).insertAfter( element );
}

function markValid( elementId )
{
	var element = jQuery("#" + elementId );
	html = '<span htmlfor="' + element.attr('name') + '" generated="true" style="font-style: italic; color: red;" class="error valid"></span>';
	element.next().remove();	
	jQuery( html ).insertAfter( element );
}

function isValid( elementId )
{		
	var next = jQuery("#" + elementId ).next( 'span[class~=valid]' );
	
	return  next.length > 0 ;
}

// -----------------------------------------------------------------------------
// GUI operations
// -----------------------------------------------------------------------------

/**
 * Clock screen by mask *
 */
function lockScreen()
{
	jQuery.blockUI({ message: i18n_waiting , css: { 
		border: 'none', 
		padding: '15px', 
		backgroundColor: '#000', 
		'-webkit-border-radius': '10px', 
		'-moz-border-radius': '10px', 
		opacity: .5, 
		color: '#fff'			
	} }); 
}
/**
 * unClock screen *
 */
function unLockScreen()
{
	jQuery.unblockUI();
}

function showPopupWindow( html, width, height)
{
	var width_ = document.documentElement.clientWidth;
	var height_ = document.documentElement.clientHeight;	
	
	var top = ((height_ / 2) - (height/2)) + 'px';
	var left =  ((width_ / 2) - (width/2)) + 'px';
	
	jQuery.blockUI({ message:  html, css: {cursor:'default', width: width + 'px', height: height + 'px', top: top , left: left} });
}

function showPopupWindowById( id, width, height )
{
	var width_ = document.documentElement.clientWidth;
	var height_ = document.documentElement.clientHeight;	
	
	var top = ((height_ / 2) - (height/2)) + 'px';
	var left =  ((width_ / 2) - (width/2)) + 'px';
	
	container = jQuery("#" + id);
	container.css('width', width + 'px');
	container.css('height', height + 'px');
	container.css('top', top );
	container.css('left', left );
	container.css('z-index', 1000000 );
	container.css('position', 'fixed' );
	container.css('background-color', '#FFFFFF' );
	container.css('overflow', 'auto' );	
	container.css('border', 'medium solid silver');
	container.show(  jQuery.blockUI({message:null}));
	
}

function hidePopupWindow( id )
{
	hideById( id );
	unLockScreen();
}

/**
 * Removes the opacity div from the document. function deleteDivEffect()
 */
function deleteDivEffect()
{
	var divEffect = document.getElementById( 'divEffect' );
	
	if( divEffect!=null )
	{	
		document.body.removeChild(divEffect);
	}
}

/**
 * Used to export PDF file by the given type and the filter params in page
 */
function exportPdfByType( type, params )
{	
	if ( jQuery( "table.listTable tbody tr" ).length == 0 )
	{
		showWarningMessage( i18n_no_item_to_export );
		return;
	}
	
	var form = byId( 'filterKeyForm' );
	form.action = 'exportToPdf.action?' + params;
	form.submit();
	form.action = type + '.action';
}

/**
 * Displays the div with the first argument id, and hides the divs with ids in
 * the second array argument, except the id given in the first argument.
 */
function displayDiv( divId, divIds ) {
	$( "#" + divId ).show();
	for ( i in divIds ) {
		if ( divIds[i] != divId ) {
			$( "#" + divIds[i] ).hide();
		}
	}
}

function relativePeriodsChecked()
{
    if ( isChecked( "reportingMonth" ) ||
         isChecked( "reportingBimonth" ) ||
         isChecked( "reportingQuarter" ) ||
         isChecked( "lastMonth" ) ||
         isChecked( "lastBimonth" ) ||
         isChecked( "lastQuarter" ) ||
         isChecked( "lastSixMonth" ) ||
         isChecked( "monthsThisYear" ) ||
         isChecked( "quartersThisYear" ) ||
         isChecked( "thisYear" ) ||
         isChecked( "monthsLastYear" ) ||
         isChecked( "quartersLastYear" ) ||
         isChecked( "lastYear" ) ||
         isChecked( "last5Years" ) ||
         isChecked( "last12Months" ) ||
         isChecked( "last3Months" ) ||
         isChecked( "last6BiMonths" ) ||
         isChecked( "last4Quarters" ) ||
         isChecked( "last2SixMonths" ) ||
         isChecked( "thisFinancialYear" ) ||
         isChecked( "lastFinancialYear" ) ||
         isChecked( "last5FinancialYears" ) ||
         isChecked( "lastWeek" ) ||
         isChecked( "last4Weeks" ) ||
         isChecked( "last12Weeks" ) )
    {
        return true;
    }
    
    return false;
}

function startsWith( string, substring )
{
	return ( string && string.lastIndexOf( substring, 0 ) === 0 ) ? true : false;
}

function getRandomNumber()
{
	return Math.floor( 100000000 * Math.random() );
}

function getRandomCode()
{
	var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";	
	var len = 11;	
	var string = "";
	
	for ( var i=0; i < len; i++ ) 
	{
		var rnum = Math.floor( Math.random() * chars.length );
		string += chars.substring( rnum, rnum+1 );
	}
	
	return string;
}

function getDC()
{
	return "_dc=" + getRandomNumber();
}

/**
 * Rounds the given number to the given number of decimals.
 */
function roundTo( number, decimals )
{
	if ( number == null || isNaN( number ) || decimals == null || isNaN( decimals ) )
	{
		return number;
	}
	
	var factor = Math.pow( 10, decimals );
		
	return ( Math.round( number * factor ) / factor );
}

function isDefined( variable )
{
	return ( typeof( variable ) !== "undefined" );
}

// -----------------------------------------------------------------------------
// Paging
// -----------------------------------------------------------------------------

isAjax = false;
var contentDiv = 'contentDiv';
function pagingList( currentPage, pageSize )
{
	var baseLink = jQuery( "#baseLink" ).val();	
	var url = baseLink + "currentPage=" + currentPage + "&pageSize=" + pageSize;
	
	var index = url.indexOf( '?' );
	var link = url.substring( 0, index );
	var data = url.substring( index + 1 );

	if ( !isAjax )
	{
		window.location.href = encodeURI( url );
	}
	else
	{		
		jQuery.postUTF8( link, data, function( html ) {
			setInnerHTML( contentDiv, html );
		} );
	}
}

function changePageSize( event )
{
	var key = event.keyCode || event.charCode || event.which;
	
	if ( key == 13 || key == 1 ) // Enter
	{
		// ---------------------------------------------------
		// Validate parameters
		// ---------------------------------------------------
		
		var pageSize = jQuery( "#sizeOfPage" ).val();
		
		if ( pageSize < 1 )
		{
			pageSize = 1;
		}

		var currentPage = jQuery( "#jumpToPage" ).val();
		var numberOfPages = jQuery( "#numberOfPages" ).val();
		
		if ( currentPage > numberOfPages )
		{
			currentPage = numberOfPages;
		}
		
		jQuery.cookie( "pageSize", pageSize, {path: "/"} );
		
		// ---------------------------------------------------
		// Paging
		// ---------------------------------------------------
		
		pagingList( currentPage, pageSize );
	}
}

// -----------------------------------------------------------------------------
// Notifications
// -----------------------------------------------------------------------------

function pingNotifications( category, tableId, completedCallback )
{
	var lastUid = $( '#' + tableId ).prop( 'lastUid' ); // Store on table property
	
	var param = ( undefined !== lastUid ) ? '?lastId=' + lastUid : '';
	
	$.getJSON( '../api/system/tasks/' + category + param, function( notifications )
	{
		var html = '',
		    isComplete = false;

		if ( isDefined( notifications ) && notifications.length ) {
			$.each( notifications, function( i, notification )
			{
				var first = i == 0,
					loaderHtml = '';

                if ( first ) {
					$( '#' + tableId ).prop( 'lastUid', notification.uid );
					loaderHtml = _loading_bar_html;
					$( '#loaderSpan' ).replaceWith ( '' ); // Hide previous loader bar
				}
				
				var time = '';
				
				if ( undefined !== notification.time ) {
					time = notification.time.replace( 'T', ' ' ).substring( 0, 19 );
				}
				
				html += '<tr><td>' + time + '</td><td>' + notification.message + ' &nbsp;';
				
				if ( notification.level == "ERROR" ) {
					html += '<img src="../images/error_small.png">';
					isComplete = true;
				}
				else if ( notification.completed ) {
					html += '<img src="../images/completed.png">';
					isComplete = true;
				}
				else {
					html += loaderHtml;
				}
				
				html += '</td></tr>';
			} );
		
			$( '#' + tableId ).show().prepend( html );
		
			if ( isComplete && completedCallback && completedCallback.call ) {
				completedCallback();				
			}
		}
	} );
}

//-----------------------------------------------------------------------------
// Symbol
//-----------------------------------------------------------------------------

function openSymbolDialog()
{
	$( "#symbolDiv" ).dialog( {
		height: 294,
		width: 250,
		modal: true,
		resizable: false,
		title: "Select symbol"
	} );
}

function selectSymbol( img )
{
	$( "#symbol" ).val( img );
	$( "#symbolDiv" ).dialog( "close" );
	$( "#symbolImg" ).attr( "src", "../images/orgunitgroup/" + img ).show();
}

var months =
{
  january: {
      index: 1,
      noOfDays: 28
  },
  february: {
      index: 2,
      noOfDays: 28
  },
  march: {
      index: 3,
      noOfDays: 28
  },
  april: {
      index: 4,
      noOfDays: 28
  },
  may: {
      index: 5,
      noOfDays: 28
  },
  june: {
      index: 6,
      noOfDays: 28
  },
  july: {
      index: 7,
      noOfDays: 28
  },
  august: {
      index: 8,
      noOfDays: 28
  },
  september: {
      index: 9,
      noOfDays: 28
  },
  october: {
      index: 10,
      noOfDays: 28
  },
  november: {
      index: 11,
      noOfDays: 28
  },
  december: {
      index: 12,
      noOfDays: 28
  }
};

var generate =
{
    month: function (id, parentClass)
    {
        var options = Object.keys(months).map(function (month)
        {
            return "<option value='" + months[month].index + "'>" + i18n_months[month] + "</option>";
        }).join("");
        $(parentClass+" #" + id).html(options);
    },
    days: function (id, monthIndex, parentClass)
    {
        var month = Object.keys(months)[--monthIndex];
        var noOfDays = months[month].noOfDays;
        var options = "";
        for (var count = 1; count <= noOfDays; count++)
        {
            options += "<option value='" + count + "'>" + count + "</option>"
        }
        $(parentClass+" #" + id).html(options);
    },
    time: function (id, parentClass)
    {
        var hours = 24;
        var times = [];
        for (var hour = 0; hour < hours; hour++)
        {
            times.push(getHour(hour) + ":00");
            times.push(getHour(hour) + ":30");
        }

        var timeOptions = times.map(function (time)
        {
            return "<option value='" + time + "'>" + time + "</option>"
        }).join("");
        $(parentClass+" #" + id).html(timeOptions);
    }
};

var getHour = function (hour)
{
    return hour < 10 ? "0" + hour : hour;
};

var getMinutes = function (minutes)
{
    return minutes == 0 ? "00" : minutes;
};
