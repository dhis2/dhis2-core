/**
 * This file depends on form.js.
 * 
 * Format for the span/input identifiers for selectors:
 * 
 * {dataelementid}-{optioncomboid}-val // data value 
 * {dataelementid}-dataelement name of data element 
 * {optioncomboid}-optioncombo // name of category option combo 
 * {dataelementid}-cell // table cell for data element name
 * {dataelementid}-{optioncomboid}-min // min value for data value
 * {dataelementid}-{optioncomboid}-max // max value for data value
 */

// -----------------------------------------------------------------------------
// Save
// -----------------------------------------------------------------------------

/**
 * Updates totals for data element total fields.
 * 
 * @param dataElementId the id of the data element to update total fields, if
 *        omitted then all total fields are updated.
 */
dhis2.de.updateDataElementTotals = function( dataElementId )
{
	var currentTotals = [];
	
	$( 'input[name="total"]' ).each( function( index )
	{
		var de = $( this ).attr( 'dataelementid' );
		
		if ( !dataElementId || dataElementId == de )
		{		
			var total = dhis2.de.getDataElementTotalValue( de );
			
			$( this ).attr( 'value', total );
		}
	} );
}

/**
 * Updates all indicator input fields with the calculated value based on the
 * values in the input entry fields in the form.
 */
dhis2.de.updateIndicators = function()
{
    $( 'input[name="indicator"]' ).each( function( index )
    {
        var indicatorId = $( this ).attr( 'indicatorid' );

        var formula = dhis2.de.indicatorFormulas[indicatorId];
        
        if ( isDefined( formula ) )
        {        
	        var expression = dhis2.de.generateExpression( formula );
	
	        if ( expression )
	        {
		        var value = eval( expression );
		        
		        value = isNaN( value ) ? '-' : roundTo( value, 1 );
		
		        $( this ).attr( 'value', value );
	        }
        }
        else
        {
        	log( 'Indicator does not exist in form: ' + indicatorId );
        }
    } );
}

/**
 * Returns the total sum of values in the current form for the given data element
 * identifier.
 */
dhis2.de.getDataElementTotalValue = function( de )
{
	var sum = new Number();
		
	$( '[id^="' + de + '"]' ).each( function( index )
	{
		var val = $( this ).attr( 'value' );
		
		if ( val && dhis2.validation.isNumber( val ) )
		{
			sum += new Number( $( this ).attr( 'value' ) );
		}
	} );
	
	return sum;
}

/**
 * Returns the value in the current form for the given data element and category
 * option combo identifiers. Returns 0 if the field does not exist in the form.
 */
dhis2.de.getFieldValue = function( de, coc )
{
    var fieldId = '#' + de + '-' + coc + '-val';
	
    var value = '0';
    
    if ( $( fieldId ).length )
    {
        value = $( fieldId ).val() ? $( fieldId ).val() : '0';
    }
    
    return value;
}

/**
 * Parses the expression and substitutes the operand identifiers with the value
 * of the corresponding input entry field.
 */
dhis2.de.generateExpression = function( expression )
{
    var matcher = expression.match( dhis2.de.cst.formulaPattern );

    for ( k in matcher )
    {
        var match = matcher[k];

        // Remove brackets from expression to simplify extraction of identifiers

        var operand = match.replace( /[#\{\}]/g, '' );

        var isTotal = !!( operand.indexOf( dhis2.de.cst.separator ) == -1 );
        
        var value = '0';
        
        if ( isTotal )
        {
        	value = dhis2.de.getDataElementTotalValue( operand );
        }
        else
        {
	        var de = operand.substring( 0, operand.indexOf( dhis2.de.cst.separator ) );
	        var coc = operand.substring( operand.indexOf( dhis2.de.cst.separator ) + 1, operand.length );	
	        value = dhis2.de.getFieldValue( de, coc );
        }

        expression = expression.replace( match, value );
        
        // TODO signed numbers
    }

    return expression;
}

function saveVal( dataElementId, optionComboId, fieldId, feedbackId )
{
    var fieldIds = fieldId.split( "-" );

    feedbackId = '#' + ( feedbackId || fieldId );

    if ( fieldIds.length > 3 )
    {
      dhis2.de.currentOrganisationUnitId = fieldIds[0];
    }

    fieldId = '#' + fieldId;

    var dataElementName = getDataElementName( dataElementId );
    var value = $( fieldId ).val();
    var type = getDataElementType( dataElementId );

    $( feedbackId ).css( 'background-color', dhis2.de.cst.colorYellow );

    var periodId = $( '#selectedPeriodId' ).val();

    var warning = undefined;

    var existing = !!( dhis2.de.currentExistingValue && dhis2.de.currentExistingValue != '' );
	
    if ( value )
    {
        if ( type == 'TEXT' || type == 'NUMBER' || type == 'INTEGER' || type == 'INTEGER_POSITIVE' || type == 'INTEGER_NEGATIVE'
          || type == 'INTEGER_ZERO_OR_POSITIVE' || type == 'UNIT_INTERVAL' || type == 'PERCENTAGE' || type == 'COORDINATE')
        {
            if ( value.length > dhis2.de.cst.valueMaxLength )
            {
                return dhis2.de.alertField( fieldId, i18n_value_too_long + '\n\n' + dataElementName );
            }
            if ( type == 'NUMBER' && !dhis2.validation.isNumber( value ) )
            {
                return dhis2.de.alertField( fieldId, i18n_value_must_number + '\n\n' + dataElementName );
            }
            if ( type == 'INTEGER' && !dhis2.validation.isInt( value ) )
            {
                return dhis2.de.alertField( fieldId, i18n_value_must_integer + '\n\n' + dataElementName );
            }
            if ( type == 'INTEGER_POSITIVE' && !dhis2.validation.isPositiveInt( value ) )
            {
                return dhis2.de.alertField( fieldId, i18n_value_must_positive_integer + '\n\n' + dataElementName );
            }
            if ( type == 'INTEGER_NEGATIVE' && !dhis2.validation.isNegativeInt( value ) )
            {
                return dhis2.de.alertField( fieldId, i18n_value_must_negative_integer + '\n\n' + dataElementName );
            }
            if ( type == 'INTEGER_ZERO_OR_POSITIVE' && !dhis2.validation.isZeroOrPositiveInt( value ) )
            {
                return dhis2.de.alertField( fieldId, i18n_value_must_zero_or_positive_integer + '\n\n' + dataElementName );
            }
            if ( type == 'COORDINATE' && !dhis2.validation.isCoordinate( value ) )
            {
                return dhis2.de.alertField( fieldId, i18n_value_must_coordinate + '\n\n' + dataElementName );
            }
            if ( type == 'UNIT_INTERVAL' && !dhis2.validation.isUnitInterval( value ) )
            {
            	return dhis2.de.alertField( fieldId, i18n_value_must_unit_interval + '\n\n' + dataElementName );
            }
            if ( type == 'PERCENTAGE' && !dhis2.validation.isPercentage( value ) )
            {
            	return dhis2.de.alertField( fieldId, i18n_value_must_percentage + '\n\n' + dataElementName );
            }
            if ( !existing && dhis2.validation.isValidZeroNumber( value ) )
            {
                // If value = 0 and zero not significant for data element, skip
            	// If existing value, let through and delete on server

                if ( dhis2.de.significantZeros.indexOf( dataElementId ) == -1 )
                {
                    $( fieldId ).css( 'background-color', dhis2.de.cst.colorGreen );
                    return false;
                }
            }

            var minString = dhis2.de.currentMinMaxValueMap[dataElementId + '-' + optionComboId + '-min'];
            var maxString = dhis2.de.currentMinMaxValueMap[dataElementId + '-' + optionComboId + '-max'];

            if ( minString && maxString ) // TODO if only one exists?
            {
                var valueNo = new Number( value );
                var min = new Number( minString );
                var max = new Number( maxString );

                if ( valueNo < min )
                {
                    warning = i18n_value_of_data_element_less + ': ' + min + '\n\n' + dataElementName;
                }

                if ( valueNo > max )
                {
                    warning = i18n_value_of_data_element_greater + ': ' + max + '\n\n' + dataElementName;
                }
            }
        }
    }
    
    var color = warning ? dhis2.de.cst.colorOrange : dhis2.de.cst.colorGreen;
    
    var valueSaver = new ValueSaver( dataElementId,	periodId, optionComboId, value, feedbackId, color );
    valueSaver.save();

    dhis2.de.updateIndicators(); // Update indicators for custom form
    dhis2.de.updateDataElementTotals( dataElementId ); // Update data element totals for custom forms
    
    if ( warning )
    {
    	window.alert( warning );
    }
}

function saveBoolean( dataElementId, optionComboId, _fieldId )
{
    var fieldId = '#' + _fieldId;
    
    var value = $('input[id=' + _fieldId + ']:checked').val();

    $( fieldId ).css( 'background-color', dhis2.de.cst.colorYellow );

    var periodId = $( '#selectedPeriodId' ).val();

    var valueSaver = new ValueSaver( dataElementId, periodId, optionComboId, value, fieldId, dhis2.de.cst.colorGreen );
    valueSaver.save();
}

function saveTrueOnly( dataElementId, optionComboId, fieldId )
{
    fieldId = '#' + fieldId;

    var value = $( fieldId ).is( ':checked' );
    
    value = ( value == true) ? value : undefined; // Send nothing if un-ticked

    $( fieldId ).css( 'background-color', dhis2.de.cst.colorYellow );

    var periodId = $( '#selectedPeriodId' ).val();

    var valueSaver = new ValueSaver( dataElementId, periodId, optionComboId, value, fieldId, dhis2.de.cst.colorGreen );
    valueSaver.save();
}

function saveFileResource( dataElementId, optionComboId, fieldId, fileResource, onSuccessCallback )
{
    fieldId = '#' + fieldId;

    var periodId = $( '#selectedPeriodId' ).val();

    var valueSaver = new FileResourceValueSaver( dataElementId, periodId, optionComboId, fileResource, fieldId, dhis2.de.cst.colorGreen, onSuccessCallback );
    valueSaver.save();
}

/**
 * Supportive method.
 */
dhis2.de.alertField = function( fieldId, alertMessage )
{
    var $field = $( fieldId );
    $field.css( 'background-color', dhis2.de.cst.colorYellow );

    window.alert( alertMessage );
    
    var val = dhis2.de.currentExistingValue || '';
    $field.val( val );
    
    $field.focus();

    return false;
}

// -----------------------------------------------------------------------------
// Saver objects
// -----------------------------------------------------------------------------

/**
 * @param de data element identifier.
 * @param pe iso period.
 * @param co category option combo.
 * @param value value.
 * @param fieldId identifier of data input field.
 * @param resultColor the color code to set on input field for success.
 */
function ValueSaver( de, pe, co, value, fieldId, resultColor )
{
	var ou = dhis2.de.getCurrentOrganisationUnit();
	
    var dataValue = {
        'de' : de,
        'co' : co,
        'ou' : ou,
        'pe' : pe,
        'value' : value
    };

    var cc = dhis2.de.getCurrentCategoryCombo();
    var cp = dhis2.de.getCurrentCategoryOptionsQueryValue();
    
    if ( cc && cp )
    {
    	dataValue.cc = cc;
    	dataValue.cp = cp;
    }
    
    this.save = function()
    {
    	dhis2.de.storageManager.saveDataValue( dataValue );

        $.ajax( {
            url: '../api/dataValues',
            data: dataValue,
            dataType: 'json',
            type: 'post',
            success: handleSuccess,
            error: handleError
        } );
    };

    var afterHandleSuccess = function() {};

    this.setAfterHandleSuccess = function( callback ) {
        afterHandleSuccess = callback;
    };

    function handleSuccess()
    {
        dhis2.de.storageManager.clearDataValueJSON( dataValue );
        markValue( fieldId, resultColor );
        $( document ).trigger( dhis2.de.event.dataValueSaved, [ dhis2.de.currentDataSetId, dataValue ] );
        afterHandleSuccess();
    }

    function handleError( xhr, textStatus, errorThrown )
    {
    	if ( 409 == xhr.status || 500 == xhr.status ) // Invalid value or locked
    	{
    		markValue( fieldId, dhis2.de.cst.colorRed );
        var errorText = JSON.parse( xhr.responseText );
    		setHeaderDelayMessage( errorText.message );
    	}
    	else // Offline, keep local value
    	{
    		markValue( fieldId, resultColor );
    		setHeaderDelayMessage( i18n_offline_notification );
    	}
    }

    function markValue( fieldId, color )
    {
        $( fieldId ).css( 'background-color', color );
    }
}

function FileResourceValueSaver( de, pe, co, fileResource, fieldId, resultColor, onSuccessCallback )
{
    var valueSaver = new ValueSaver( de, pe, co, fileResource.id, fieldId, resultColor );

    valueSaver.setAfterHandleSuccess( onSuccessCallback );

    return valueSaver;
}
