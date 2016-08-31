var left = true;
var dialog = null;

jQuery(document).ready(function() 
{
	dialog = jQuery( "#expression-container" ).dialog({
		modal: true,
		autoOpen: false,
		minWidth: 840,
		minHeight: 560,
		width: 840,
		height: 655,
		title: "Expression"
	});
		
	jQuery( "#periodTypeName, #ruleType" ).change(function(){
		getOperandsPage();
	});
	
	getConstantsPage();
	getOperandsPage();
});

function editLeftExpression()
{		
	left = true;
	var strategy = $( '#leftSideMissingValueStrategy' ).val();
	strategy = ( strategy ) ? strategy : 'SKIP_IF_ANY_VALUE_MISSING';
	
	$( '#expression' ).val( $( '#leftSideExpression' ).val() );
	$( '#description' ).val( $( '#leftSideDescription' ).val() );
	$( '#formulaText' ).text( $( '#leftSideTextualExpression' ).val() );
	$( 'input[name="missingValueStrategy"][value="' + strategy + '"]' ).prop( 'checked', true );
	
	dialog.dialog("open");
}

function editRightExpression()
{
	left = false;
	var strategy = $( '#rightSideMissingValueStrategy' ).val();
	strategy = ( strategy ) ? strategy : 'SKIP_IF_ANY_VALUE_MISSING';
	
	$( '#expression' ).val( $( '#rightSideExpression' ).val() );
	$( '#description' ).val( $( '#rightSideDescription' ).val() );
	$( '#formulaText' ).text( $( '#rightSideTextualExpression' ).val() );
	$( 'input[name="missingValueStrategy"][value="' + strategy + '"]' ).prop( 'checked', true );
	
	dialog.dialog("open");
}

function getConstantsPage()
{
	var target = jQuery( "#expression-container select[id=constantId]" );
	target.children().remove();
	
	jQuery.get( '../api/constants.json?fields=id,name&paging=false&links=false', {}, function( json ) 
	{
		if ( !json.constants || json.constants.length == 0 )
		{
			setInnerHTML( 'constantHeader', "<i style='color:red'>" + i18n_no_constant_to_select + "</i>" );
			return;
		}
		
		jQuery.each( json.constants, function(i, item) {
			target.append( '<option value="C{' + item.id + '}">' + item.name + '</option>' );
		});
	});
}

function getOperandsPage()
{
	var key = getFieldValue( "expression-container input[id=filter]" );
	
	var periodType = getFieldValue( "periodTypeName" );
	var ruleType = getFieldValue( "ruleType" );
	var periodTypeAllowAverage = ( ruleType && ruleType == "surveillance" ) ? true : false;

	dataDictionary.loadOperands( "#expression-container select[id=dataElementId]", 
		{usePaging: true, key: key, periodType: periodType, includeTotals: true, periodTypeAllowAverage: periodTypeAllowAverage } );	
}

function clearSearchText()
{
	jQuery( "#expression-container input[id=filter]").val("");
	getOperandsPage();
}

function getExpressionText()
{
	if ( hasText('expression') ){
		jQuery.ajax({
			url: '../api/expressions/description',
			data: {
				expression: $( '#expression' ).val()
			},
			success: function( json, status, xhr ) {
				if ( 'OK' == json.status ) {
					jQuery( "#formulaText" ).html( json.description );
				}
				else {
					jQuery( "#formulaText" ).html( json.message );
				}
			}
		});
	}
	else {
		jQuery( "#formulaText").html( '' );
	}
}

function insertText( inputAreaName, inputText )
{
	insertTextCommon( inputAreaName, inputText );
	
	getExpressionText();
}

function insertExpression()
{
	var expression = $( '#expression' ).val(),
		description = $( '#description' ).val(),
		formulaText = $( '#formulaText' ).text(),
		missingValueStrategy = $( 'input[name="missingValueStrategy"]:checked' ).val();
	
	jQuery.ajax({
		url: '../api/expressions/description',
		data: {
			expression: expression
		},
		success: function( json, status, xhr ) {
			if ( json.status == 'ERROR' )
			{
				markInvalid( 'expression-container textarea[id=expression]', json.message );
			}
			else 
			{								
				if ( left )
				{
					$( '#leftSideExpression' ).val( expression );
					$( '#leftSideDescription' ).val( description );
					$( '#leftSideTextualExpression' ).val( formulaText );
					$( '#leftSideMissingValueStrategy' ).val( missingValueStrategy );
				}
				else
				{
					$( '#rightSideExpression' ).val( expression );
					$( '#rightSideDescription' ).val( description );
					$( '#rightSideTextualExpression' ).val( formulaText );
					$( '#rightSideMissingValueStrategy' ).val( missingValueStrategy );
				}
				
				dialog.dialog( "close" );
			}
		}
	});
}

function clearRuleExpression()
{
    var description = $( "#leftSideDescription" ).val();
    $( "#leftSideExpression" ).val( "" );
    $( "#leftSideTextualExpression" ).val( "" );
    saveExpression( "left", description, "", "" );

    description = $( "#rightSideDescription" ).val();
    $( "#rightSideExpression" ).val( "" );
    $( "#rightSideTextualExpression" ).val( "" );
    saveExpression( "right", description, "", "" );

	enable( "ruleType" );
	enable( "periodTypeName" );
}

function saveExpression( side, description, expression, textualDescription )
{
    if ( side == "left" )
    {
        $( "#leftSideDescription" ).val( description );
        $( "#leftSideExpression" ).val( expression );
        $( "#leftSideTextualExpression" ).val( textualDescription );
    } 
    else if ( side == "right" )
    {
    	$( "#rightSideDescription" ).val( description );
    	$( "#rightSideExpression" ).val( expression );
    	$( "#rightSideTextualExpression" ).val( textualDescription );
    }

    hideById( 'dynamicContent' );
    unLockScreen();
}
