
function addCategoryOption()
{
	var value = $( '#categoryOptionName' ).val();
	
	if ( value.length == 0 ) 
	{
		markInvalid( 'categoryOptionName', i18n_specify_category_option_name );
	}
	else if ( listContainsById( 'categoryOptionNames', value ) ) 
	{
		markInvalid( 'categoryOptionName', i18n_category_option_name_already_exists );
	}
	else 
	{
		jQuery.postJSON( 'validateDataElementCategoryOption.action', { name:value }, function( json ) 
		{
			if ( json.response == 'success' )
			{					
				addOptionById( 'categoryOptionNames', value, value );
				setFieldValue( 'categoryOptionName', '' );
			}
			else
			{
				markInvalid( 'categoryOptionName', i18n_category_option_name_already_exists );
			}
		} );
	}
}

function getSelectedCategoryOption()
{
	var name = $( '#categoryOptions :selected' ).text();
	$( '#categoryOptionName' ).val( name );
}

function updateCategoryOptionName()
{
	var id = $( '#categoryOptions :selected' ).val();
	var name = $( '#categoryOptionName' ).val();
	
	var url = 'updateDataElementCategoryOption.action?id=' + id + '&name=' + name;
	
	$.postUTF8( url, {}, function() 
	{
		$( '#categoryOptions :selected' ).text( name );
	} );
}

function saveCategoryOption( id, name )
{
	var url = 'addDataElementCategoryOption.action';
	
	$.postJSON( url, { categoryId:id, name:name }, function( json )
	{
		addOptionById( 'categoryOptions', json.dataElementCategoryOption.id, name );
	} );
}

function showMoreOrFewerOptions()
{
    $( "#showMoreOptions" ).toggle();
    $( "#moreOptions" ).toggle();
}

function showDataElementCategoryOptionDetails( context ) {
	jQuery.post( 'getDataElementCategoryOption.action', { id: context.id } ,function ( json ) {		
		setInnerHTML( 'nameField', json.dataElementCategoryOption.name );
		setInnerHTML( 'shortNameField', json.dataElementCategoryOption.shortName );
		setInnerHTML( 'codeField', json.dataElementCategoryOption.code );
		setInnerHTML( 'startDateField', json.dataElementCategoryOption.startDate );
		setInnerHTML( 'endDateField', json.dataElementCategoryOption.endDate );
		setInnerHTML( 'idField', json.dataElementCategoryOption.uid );
		
		var categories = "";
		for( var i in json.dataElementCategoryOption.categories )
		{
			categories += json.dataElementCategoryOption.categories[i].name + "; ";
		}
		categories = categories.substr( 0, categories.length - 2 );
		setInnerHTML( 'categoriesField', categories );
		
        showDetails();
	});
}

function removeDataElementCategoryOption( context ) {
	removeItem( context.id, context.name, i18n_confirm_delete, 'removeDataElementCategoryOption.action' );
}

function showUpdateDataElementCategoryOptionForm( context ) {
    location.href = 'showUpdateDataElementCategoryOptionForm.action?id=' + context.id;
}
