
// -----------------------------------------------------------------------------
// Author:   Torgeir Lorange Ostby
// Version:  $Id: oust.js 5791 2008-10-02 11:04:41Z larshelg $
// -----------------------------------------------------------------------------

var selectionTreeSelection = new SelectionTreeSelection();
var selectionTree = new SelectionTree();
var selectionTreePath = '../dhis-web-commons/oust/';
var selectedOrganisationUnit = new Array();
var selectedOrganisationUnitUid = new Array();

// -----------------------------------------------------------------------------
// Selection
// -----------------------------------------------------------------------------

function SelectionTreeSelection()
{
    var onSelectFunction = undefined;

    var listenerFunction = undefined;

    var multipleSelectionAllowed = true;

    this.setOnSelectFunction = function( onSelectFunction_ ) 
    {
        onSelectFunction = onSelectFunction_;
    }

    this.setListenerFunction = function( listenerFunction_ ) 
    {
        listenerFunction = listenerFunction_;
    }

    this.setMultipleSelectionAllowed = function( allowed ) 
    {
        multipleSelectionAllowed = allowed;
    }

    this.getSelected = function() 
    {
        return selectedOrganisationUnit;
    }

    this.getSelectedUid = function() 
    {
        return selectedOrganisationUnit;
    }

    this.isSelected = function() 
    {
        return selectedOrganisationUnit && selectedOrganisationUnit.length > 0;
    }

    this.select = function( unitId )
    {
        if ( onSelectFunction )
        {
            onSelectFunction();
        }
		
        var unitTag = document.getElementById( getTagId( unitId ) );
        var linkTags = $(unitTag).find( 'a' );

        if ( linkTags[0].className == 'selected' )
        {
            $.post( selectionTreePath + 'removeorgunit.action', { id:unitId }, responseReceived );
				
            linkTags[0].className = '';			
        }
        else
        {			
            if ( multipleSelectionAllowed )
            {
                $.post( selectionTreePath + 'addorgunit.action', { id:unitId }, responseReceived );
				
                linkTags[0].className = 'selected';
            }
            else
            {
                $.post( selectionTreePath + 'setorgunit.action', { id:unitId }, responseReceived );
				
                // Remove all select marks
                var treeTag = document.getElementById( 'selectionTree' );
                var linkTags = $(treeTag).find( 'a' );

                for ( var i = 0, linkTag; ( linkTag = linkTags[i] ); ++i )
                {
                    linkTag.className = '';
                }

                // Set new select mark
                var unitTag = document.getElementById( getTagId( unitId ) );
                linkTags = $(unitTag).find( 'a' );
                linkTags[0].className = 'selected';
            }
        }
    };

    function responseReceived( json )
    {
        selectedOrganisationUnit = [];
        selectedOrganisationUnitUid = [];

        var unitIds = [];
        var unitUids = [];

        for ( i in json.selectedUnits ) 
        {
            unitIds[i] = json.selectedUnits[i].id;
            selectedOrganisationUnit.push(unitIds[i]);

            unitUids[i] = json.selectedUnits[i].uid;
            selectedOrganisationUnitUid.push(unitUids[i]);
        }

        jQuery('body').trigger('oust.selected', selectedOrganisationUnit);

        if ( listenerFunction ) 
        {
            listenerFunction(unitIds);
        }
    }

    function getTagId( unitId )
    {
        return 'oustOrgUnit' + unitId;
    }
}

// -----------------------------------------------------------------------------
// Subtree
// -----------------------------------------------------------------------------

function SelectionTree()
{
    this.clearSelectedOrganisationUnitsAndBuildTree = function()
    {
    	$.ajax( { 
    		url: selectionTreePath + 'clearSelectedOrganisationUnits.action',
			success: function() {
				selectionTree.buildSelectionTree();
			}
    	} );
    }

    this.toggle = function( unitId )
    {
        var parentTag = document.getElementById( getTagId( unitId ));
        var children = $(parentTag).find( 'ul' );

        if ( children.length < 1 || !isVisible( children[0] ))
        {	
			$.post( selectionTreePath + 'expandSubtree.action',{parentId:unitId},processExpand );		
        }
        else
        {
            setVisible( children[0], false );
            setToggle( parentTag, false );
        }
    };

    this.buildSelectionTree = function()
    {
        selectedOrganisationUnit = [];
        selectedOrganisationUnitUid = [];

        var treeTag = document.getElementById('selectionTree');

        setLoadingMessage(treeTag);

        var children = $(treeTag).find('ul');

        if( children.length > 0 ) {
            treeTag.removeChild(children[0]);
        }

        $.ajax({
            url: selectionTreePath + 'getExpandedTree.action',
            cache: false,
            dataType: "xml",
            success: treeReceived
        });
    };

    function processExpand( rootElement )
    {
        var xml = $.parseXML(rootElement);
        var parentElements = $(xml).find( 'parent' );

        for ( var i = 0, parentElement; ( parentElement = parentElements[i] ); ++i )
        {
            var parentId = parentElement.getAttribute( 'parentId' );
            var parentTag = document.getElementById( getTagId( parentId ));
            var children = $(parentTag).find( 'ul' );

            if ( children.length < 1 )
            {
                createChildren( parentTag, parentElement );
            }
            else
            {
                setVisible( children[0], true );
                setToggle( parentTag, true );
            }
        }
    }

    function treeReceived( rootElement )
    {
        var unitElements = $(rootElement).find( 'unit' );

        var treeTag = document.getElementById( 'selectionTree' );
        var rootsTag = document.createElement( 'ul' );

        for ( var i = 0; i < unitElements.length; ++i )
        {
        	if ( unitIsValid( unitElements[i] ) )
        	{
        		var unitTag = createTreeElementTag( unitElements[i] );
            
        		rootsTag.appendChild( unitTag );
        	}
        }

        treeTag.appendChild( rootsTag );

        var childrenElement = $(rootElement).find( 'children' ).first();
        var parentElements = $(childrenElement).find( 'parent' );

        for ( var i = 0, parentElement; ( parentElement = parentElements[i] ); ++i )
        {
            var parentId = parentElement.getAttribute( 'parentId' );
            var parentTag = document.getElementById( getTagId( parentId ) );

            createChildren( parentTag, parentElement );
        }
        
        clearLoadingMessage( treeTag );

        jQuery( 'body' ).trigger( 'oust.selected', selectedOrganisationUnit );
    }

    function createChildren( parentTag, parentElement )
    {
        var children = $(parentElement).find( 'child' );
        var childrenTag = document.createElement( 'ul' );

        for ( var i = 0, child; ( child = children[i] ); ++i )
        {
        	if ( unitIsValid( child ) )
        	{
        		var childTag = createTreeElementTag( child );

        		childrenTag.appendChild( childTag );
        	}
        }

        setVisible( childrenTag, true );
        setToggle( parentTag, true );

        parentTag.appendChild( childrenTag );
    }

    function createTreeElementTag( child )
    {
        var childId = child.getAttribute( 'id' );
        var childUid = child.getAttribute( 'uid' );
        var hasChildren = child.getAttribute( 'hasChildren' ) != '0';

        var toggleTag = document.createElement( 'span' );
        toggleTag.className = 'toggle';

        if ( hasChildren )
        {
            toggleTag.onclick = new Function( 'selectionTree.toggle( \"' + childId + '\" )' );
            toggleTag.appendChild( getToggleExpand() );
        }
        else
        {
            toggleTag.appendChild( getToggleBlank() );
        }

        var linkTag = document.createElement( 'a' );
        linkTag.href = 'javascript:void selectionTreeSelection.select( \"' + childId + '\" )';
        linkTag.appendChild( document.createTextNode( child.firstChild.nodeValue ));

        if ( child.getAttribute( 'selected' ) == 'true' )
        {
            linkTag.className = 'selected';
            
			selectedOrganisationUnit.push( childId );
			selectedOrganisationUnitUid.push( childUid );

			if ( typeof ( window.addSelectedOrganisationUnit__ ) == 'function' )
			{ 
				addSelectedOrganisationUnit__( childId );// This code is bad and must be removed
			}
        }

        var childTag = document.createElement( 'li' );
        childTag.id = getTagId( childId );
        childTag.appendChild( document.createTextNode( ' ' ));
        childTag.appendChild( toggleTag );
        childTag.appendChild( document.createTextNode( ' ' ));
        childTag.appendChild( linkTag );
        
        return childTag;
    }

    function setToggle( unitTag, expanded )
    {
        var spans = $(unitTag).find( 'span' );
        var toggleTag = spans[0];
        var toggleImg = expanded ? getToggleCollapse() : getToggleExpand();

        if ( toggleTag.firstChild )
        {
        	toggleTag.replaceChild( toggleImg, toggleTag.firstChild );
		}
		else
		{
			toggleTag.appendChild( toggleImg );
		}
    }

    function setVisible( tag, visible )
    {
        tag.style.display = visible ? 'block' : 'none';
    }

    function isVisible( tag )
    {
        return tag.style.display != 'none';
    }

    function getTagId( unitId )
    {
        return 'oustOrgUnit' + unitId;
    }
    
    function getToggleExpand()
    {
        var imgTag = getToggleImage();
        imgTag.src = '../images/colapse.png';
        imgTag.alt = '[+]';
        return imgTag;
    }
    
    function getToggleCollapse()
    {
        var imgTag = getToggleImage();
        imgTag.src = '../images/expand.png';
        imgTag.alt = '[-]';
        return imgTag;
    }

	function getToggleBlank()
	{
		var imgTag = getToggleImage();
		imgTag.src = '../images/transparent.gif';
		imgTag.width = '9';
        imgTag.height = '9';
		imgTag.alt = '';
		return imgTag;
	}

    function getToggleImage()
    {
        var imgTag = document.createElement( 'img' );
        imgTag.width = '9';
        imgTag.height = '9';
        return imgTag;
    }

    function setLoadingMessage( treeTag )
    {
        treeTag.style.backgroundImage = 'url(../images/ajax-loader-circle.gif)';
        treeTag.style.backgroundRepeat = 'no-repeat';
    }

    function clearLoadingMessage( treeTag )
    {
        treeTag.style.backgroundImage = 'none';
    }

    function unitIsValid( unit )
    {
    	if ( isDefined( unit ) && unit && unit.firstChild )
    	{
    		return true;
    	}
    	
    	log( 'Illegal organisation unit' );
    	log( unit );
    	
    	return false;
    }    
}
