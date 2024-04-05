function getTranslation()
{
	clearFields();

    var loc = $( '#loc :selected' ).val();
	
	if ( loc != 'NONE' )
	{
		jQuery.postJSON( 'getTranslations.action', {
			objectUid: getFieldValue( 'uid' ),
			className: getFieldValue( 'className' ),
			loc: loc
		}, 
		function ( json ) 
		{			
			var translations = json.translations;

			for ( var i = 0; i < translations.length; i++ )
			{
				var fieldId = '#' + translations[i].key;
				
				if ( $( fieldId ).length )
				{
					var fieldValue = translations[i].value;
					
					$( fieldId ).val( fieldValue );
				}
			}
		} );
	}
}

function clearFields( prefix )
{
    for ( var i = 0; i < propNames.length; i++ )
    {
        $( '#' + propNames[i] ).val( '' );
    }
}
