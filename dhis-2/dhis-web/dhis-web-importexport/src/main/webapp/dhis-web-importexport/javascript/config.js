
// -------------------------------------------------------------------------
// Dhis14 Configuration
// -------------------------------------------------------------------------

function validateConfigDhis14()
{
    var dataFile = htmlEncode( document.getElementById( "dataFile" ).value );
    
    var request = new Request();
    request.setResponseTypeXML( "message" );
    request.setCallbackSuccess( configDhis14Received );
    request.send( "validateConfigDhis14.action?dataFile=" + dataFile );
}

function configDhis14Received( messageElement )
{
    var message = getRootElementValue( messageElement );
    var type = getRootElementAttribute( messageElement, "type" );
    
    if ( type == "input" )
    {
        setHeaderMessage( message );
    }
    else
    {
        document.getElementById( "configForm" ).submit();
    }
}

// -------------------------------------------------------------------------
// IXF Configuration
// -------------------------------------------------------------------------

function addLevel()
{
    var list = document.getElementById( "levelNames" );
    var levelName = document.getElementById( "levelName" ).value;
    
    if ( levelName != "" )
    {
        var option = new Option( levelName, levelName );
    
        list.add( option, null );
    }
}

function deleteLevel()
{
    var list = document.getElementById( "levelNames" );
    
    for ( var i = list.length - 1; i >= 0; i-- )
    {
        if ( list.options[ i ].selected )
        {
            list.remove( i );
        }
    }
}

function moveLevelUp()
{
    var list = document.getElementById( "levelNames" );
    
    for ( var i = 0; i < list.length; i++ )
    {
        if ( list.options[ i ].selected )
        {
            if ( i > 0 )
            {
                var precedingOption = new Option( list.options[ i - 1 ].text, list.options[ i - 1 ].value );
                var currentOption = new Option( list.options[ i ].text, list.options[ i ].value );
                
                list.options[ i - 1 ] = currentOption;
                list.options[ i - 1 ].selected = true;
                list.options[ i ] = precedingOption;
            }
        }
    }
}

function moveLevelDown()
{
    var list = document.getElementById( "levelNames" );
    
    for ( var i = list.options.length - 1; i >= 0; i-- )
    {
        if ( list.options[ i ].selected )
        {
            if ( i <= list.options.length - 1 )
            {
                var subsequentOption = new Option( list.options[ i + 1 ].text, list.options[ i + 1 ].value );
                var currentOption = new Option( list.options[ i ].text, list.options[ i ].value );
                
                list.options[ i + 1 ] = currentOption;
                list.options[ i + 1 ].selected = true;
                list.options[ i ] = subsequentOption;
            }
        }
    }
}

function submitConfigForm()
{
	selectAll( document.getElementById( "levelNames" ) );
	
	document.getElementById( "configForm" ).submit();
}
