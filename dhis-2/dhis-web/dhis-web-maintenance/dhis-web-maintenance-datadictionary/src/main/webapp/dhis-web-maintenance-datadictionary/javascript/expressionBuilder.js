
var numerator = false;	
var validator = null;
var dialog = null;

function indicatorNumeratorForm()
{
	numerator = true;
	
	validator.resetForm();
	
	var expression = getFieldValue( 'numerator' );
	var description = getFieldValue( 'numeratorDescription' );
	setFieldValue( 'indicator-expression-container textarea[id=expression]', expression );
	setFieldValue( 'indicator-expression-container input[id=description]', description );
	
	getExpressionText();
	
	dialog.dialog("option", "title", i18n_edit_numerator);
	dialog.dialog("open");		
}

function indicatorDenominatorForm()
{
	numerator = false;
	
	validator.resetForm();
	
	var expression = getFieldValue( 'denominator' );
	var description = getFieldValue( 'denominatorDescription' );
	setFieldValue( 'indicator-expression-container textarea[id=expression]', expression );
	setFieldValue( 'indicator-expression-container input[id=description]', description );
	
	getExpressionText();
	
	dialog.dialog("option", "title", i18n_edit_denominator);
	dialog.dialog( "open");
}

function getConstantsPage()
{
	var target = jQuery( "#indicator-expression-container select[id=constantId]" );
	target.children().remove();
	
	jQuery.get( '../api/constants.json?paging=false&links=false',
		function( json ) {
			jQuery.each( json.constants, function(i, item) {
				target.append( '<option value="C{' + item.id + '}">' + item.name + '</option>' );
			});
		});
}

function getOrgUnitGroupsPage()
{
    var target = jQuery( "#indicator-expression-container select[id=orgUnitGroupId]" );
    target.children().remove();
    
    jQuery.get( '../api/organisationUnitGroups.json?paging=false&links=false',
        function( json ) {
            jQuery.each( json.organisationUnitGroups, function(i, item) {
                target.append( '<option value="OUG{' + item.id + '}">' + item.name + '</option>' );
            });
        });
}

function getOperandsPage()
{	
	var key = getFieldValue( "indicator-expression-container input[id=filter]");

	dataDictionary.loadOperands( "#indicator-expression-container select[id=dataElementId]", {usePaging: true, key: key, includeTotals: true} );
}

function clearSearchText()
{
	jQuery( "#indicator-expression-container input[id=filter]").val("");
	getOperandsPage();
}	

function getExpressionText()
{
	if ( hasText('expression') ){
		jQuery.ajax({
			url: '../api/expressions/description',
			data: {
				expression: getFieldValue('expression')
			},
			success: function( json, status, xhr ) {
				if ( 'OK' == json.status ) {
					jQuery( "#formulaText").html( json.description );
				}
				else {
					jQuery( "#formulaText").html( json.message );
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

function cleanExpression()
{		
	getExpressionText();
}

function closeExpressionBuilder()
{
	dialog.dialog( "close" );
}

function insertExpression()
{
	var expression = getFieldValue( 'indicator-expression-container textarea[id=expression]' );
	var description = getFieldValue( 'indicator-expression-container input[id=description]' );
	
	jQuery.ajax({
		url: '../api/expressions/description',
		data: {
			expression: getFieldValue('expression')
		},
		success: function( json, status, xhr ) {
			if ( json.status == 'ERROR') {
				markInvalid( 'indicator-expression-container textarea[id=expression]', json.message );
			}
			else {
				if ( numerator ){
					setFieldValue( 'numerator', expression );
					setFieldValue( 'numeratorDescription', description );
				}
				else {
					setFieldValue( 'denominator', expression );
					setFieldValue( 'denominatorDescription', description );
				}
				
				closeExpressionBuilder();
			}
		}
	});
}
