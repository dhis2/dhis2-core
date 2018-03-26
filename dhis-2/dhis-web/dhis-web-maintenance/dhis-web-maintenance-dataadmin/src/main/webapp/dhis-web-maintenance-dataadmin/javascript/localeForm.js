function setLocaleCode()
{		
	var localeCode = "";
	
	var language = $( '#language option:selected' );
	var country = $( '#country option:selected' );
			
	if( language.val() != "")
	{
		localeCode = language.val();	
	    
		if( country.val() != "" )
		{
			localeCode += "_" + country.val();	
		}
	}

	setFieldValue( 'localeCode', localeCode );
}
