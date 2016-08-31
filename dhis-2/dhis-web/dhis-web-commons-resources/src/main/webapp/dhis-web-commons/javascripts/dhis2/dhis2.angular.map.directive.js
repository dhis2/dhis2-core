d2Directives.directive('d2GoogleMap', function ($http, $translate, $q, $window, storage, CurrentSelection, DialogService) {
    
    function lazyLoadApi() {
        var googleMapApiUrl = 'https://maps.google.com/maps/api/js?callback=initMap';
        var storedFormat = storage.get('SYSTEM_SETTING');
        if (angular.isObject(storedFormat) && storedFormat.keyGoogleMapsApiKey ) {
            googleMapApiUrl += '&key=' + storedFormat.keyGoogleMapsApiKey;
        }
        else{
            var dialogOptions = {
                headerText: 'warning',
                bodyText: 'missing_google_map_api_key'
            };

            DialogService.showDialog({}, dialogOptions);
        }	
       
        var deferred = $q.defer();
        $window.initMap = function() {
            deferred.resolve();
        };
        
        var script = document.createElement('script');        
        script.src = googleMapApiUrl;
        document.body.appendChild(script);
        return deferred.promise;
    }

    return {
        restrict: 'E',
        replace: true,
        template: '<div></div>',
        scope: {
            location: '='
        },
        link: function (scope, element, attrs) {
            
            function renderMap(){
                
                // https://developers.google.com/maps/documentation/javascript/customoverlays
                /** @constructor */
                function Dhis2TextOverlay(pos, txt, cls, map) {

                    // Initialize properties.
                    this.pos = pos;
                    this.txt_ = txt;
                    this.cls_ = cls;
                    this.map_ = map;

                    // Div to contain text
                    this.div_ = null;

                    // Explicitly call setMap() on this overlay
                    this.setMap(map);
                }

                Dhis2TextOverlay.prototype = new google.maps.OverlayView();


                /**
                 * onAdd is called when the map's panes are ready and the overlay has been
                 * added to the map.
                 */
                Dhis2TextOverlay.prototype.onAdd = function () {

                    // Create the DIV and set some basic attributes.
                    var div = document.createElement('DIV');
                    div.className = this.cls_;
                    div.innerHTML = this.txt_;

                    // Set the overlay's div_ property to this DIV
                    this.div_ = div;
                    var overlayProjection = this.getProjection();
                    var position = overlayProjection.fromLatLngToDivPixel(this.pos);
                    div.style.left = position.x + 'px';
                    div.style.top = position.y + 'px';

                    // Add the element to the "overlayLayer" pane.
                    var panes = this.getPanes();
                    panes.floatPane.appendChild(div);
                };

                Dhis2TextOverlay.prototype.draw = function () {

                    // We use the south-west and north-east
                    // coordinates of the overlay to peg it to the correct position and size.
                    // To do this, we need to retrieve the projection from the overlay.
                    var overlayProjection = this.getProjection();

                    // Retrieve the southwest and northeast coordinates of this overlay
                    // in latlngs and convert them to pixels coordinates.
                    // We'll use these coordinates to resize the DIV.
                    var position = overlayProjection.fromLatLngToDivPixel(this.pos);

                    // Resize the div to fit the indicated dimensions.
                    var div = this.div_;
                    div.style.left = position.x + 'px';
                    div.style.top = position.y + 'px';
                };

                // The onRemove() method will be called automatically from the API if
                // we ever set the overlay's map property to 'null'
                Dhis2TextOverlay.prototype.onRemove = function () {
                    this.div_.parentNode.removeChild(this.div_);
                    this.div_ = null;
                };

                // Set the visibility to 'hidden' or 'visible'.
                Dhis2TextOverlay.prototype.hide = function () {
                    if (this.div_) {
                        this.div_.style.visibility = "hidden";
                    }
                };

                Dhis2TextOverlay.prototype.show = function () {
                    if (this.div_) {
                        this.div_.style.visibility = "visible";
                    }
                };

                Dhis2TextOverlay.prototype.toggle = function () {
                    if (this.div_) {
                        if (this.div_.style.visibility === "hidden") {
                            this.show();
                        }
                        else {
                            this.hide();
                        }
                    }
                };

                // Detach the map from the DOM via toggleDOM().
                // Note that if we later reattach the map, it will be visible again,
                // because the containing <div> is recreated in the overlay's onAdd() method.
                Dhis2TextOverlay.prototype.toggleDOM = function () {
                    if (this.getMap()) {
                        this.setMap(null);
                    }
                    else {
                        this.setMap(this.map_);
                    }
                };
                
                
                /*
                MapContextMenu v1.0

                A context menu for Google Maps API v3
                http://code.martinpearman.co.uk/googlemapsapi/contextmenu/

                Copyright Martin Pearman
                Last updated 21st November 2011

                developer@martinpearman.co.uk

                This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

                This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

                You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
                */

                function MapContextMenu(map, options){
                        options=options || {};

                        this.setMap(map);

                        this.classNames_=options.classNames || {};
                        this.map_=map;
                        this.mapDiv_=map.getDiv();
                        this.menuItems_=options.menuItems || [];
                        this.pixelOffset=options.pixelOffset || new google.maps.Point(10, -5);
                }

                MapContextMenu.prototype=new google.maps.OverlayView();

                MapContextMenu.prototype.draw=function(){
                        if(this.isVisible_){
                                var mapSize=new google.maps.Size(this.mapDiv_.offsetWidth, this.mapDiv_.offsetHeight);
                                var menuSize=new google.maps.Size(this.menu_.offsetWidth, this.menu_.offsetHeight);
                                var mousePosition=this.getProjection().fromLatLngToDivPixel(this.position_);

                                var left=mousePosition.x;
                                var top=mousePosition.y;

                                if(mousePosition.x>mapSize.width-menuSize.width-this.pixelOffset.x){
                                        left=left-menuSize.width-this.pixelOffset.x;
                                } else {
                                        left+=this.pixelOffset.x;
                                }

                                if(mousePosition.y>mapSize.height-menuSize.height-this.pixelOffset.y){
                                        top=top-menuSize.height-this.pixelOffset.y;
                                } else {
                                        top+=this.pixelOffset.y;
                                }

                                this.menu_.style.left=left+'px';
                                this.menu_.style.top=top+'px';
                        }
                };

                MapContextMenu.prototype.getVisible=function(){
                        return this.isVisible_;
                };

                MapContextMenu.prototype.hide=function(){
                        if(this.isVisible_){
                                this.menu_.style.display='none';
                                this.isVisible_=false;
                        }
                };

                MapContextMenu.prototype.onAdd=function(){
                        function createMenuItem(values){        
                                var menuItem=document.createElement('div');
                                menuItem.innerHTML=values.label;
                                if(values.className){
                                        menuItem.className=values.className;
                                }
                                if(values.id){
                                        menuItem.id=values.id;
                                }
                                menuItem.style.cssText='white-space:nowrap; font-size: 12pt;';
                                menuItem.onclick=function(){
                                        google.maps.event.trigger($this, 'menu_item_selected', $this.event_, $this.position_, values.eventName);
                                };
                                return menuItem;
                        }
                        function createMenuSeparator(){
                                var menuSeparator=document.createElement('div');
                                if($this.classNames_.menuSeparator){
                                        menuSeparator.className=$this.classNames_.menuSeparator;
                                }
                                return menuSeparator;
                        }
                        var $this=this;	//	used for closures

                        var menu=document.createElement('div');
                        if(this.classNames_.menu){
                                menu.className=this.classNames_.menu;
                        }
                        menu.style.cssText='display:none; position:absolute';

                        for(var i=0, j=this.menuItems_.length; i<j; i++){
                                if(this.menuItems_[i].label && this.menuItems_[i].eventName){
                                        menu.appendChild(createMenuItem(this.menuItems_[i]));
                                } else {
                                        menu.appendChild(createMenuSeparator());
                                }
                        }

                        delete this.classNames_;
                        delete this.menuItems_;

                        this.isVisible_=false;
                        this.menu_=menu;
                        this.position_=new google.maps.LatLng(0, 0);

                        google.maps.event.addListener(this.map_, 'click', function(mouseEvent){
                                $this.hide();
                        });

                        this.getPanes().floatPane.appendChild(menu);
                };

                MapContextMenu.prototype.onRemove=function(){
                        this.menu_.parentNode.removeChild(this.menu_);
                        delete this.mapDiv_;
                        delete this.menu_;
                        delete this.position_;
                };

                MapContextMenu.prototype.show=function(e){
                        if(!this.isVisible_){
                                this.menu_.style.display='block';
                                this.isVisible_=true;
                        }
                        this.position_=e.latLng;
                    this.event_=e;
                        this.draw();
                };
                /* Context menu ends */    
    
                CurrentSelection.setLocation(scope.location);

                var contextMenuDisplayed = false;
                var ouLevels = CurrentSelection.getOuLevels();

                //remove angular bootstrap ui modal draggable
                $(".modal-content").draggable({disabled: true});                
                var winHeight = $(window).height();
                $("#map-container").height(winHeight / 2)

                //get a default center
                var latCenter = 12.31, lngCenter = 51.48;
                
                var isLocationValid = function(){
                    if (angular.isObject(scope.location)) {
                        if (scope.location.lat && scope.location.lat !== "" && scope.location.lng && scope.location.lng !== "") {
                            return true;
                        }
                    }
                    return false;
                };

                //if there is any marker already - use it as center
                if ( isLocationValid() ) {
                    latCenter = scope.location.lat;
                    lngCenter = scope.location.lng;
                }

                var centerLatLng = new google.maps.LatLng(latCenter, lngCenter);
                
                //default map configurations 
                var mapOptions = {
                    zoom: 4,
                    center: centerLatLng,
                    mapTypeId: google.maps.MapTypeId.ROADMAP
                },
                featureStyle = {
                    strokeWeight: 2,
                    strokeOpacity: 0.4,
                    fillOpacity: 0.2,
                    fillColor: '#99cc99'
                };

                var map = new google.maps.Map(document.getElementById(attrs.id), mapOptions);

                var marker = new google.maps.Marker({
                    map: map
                });

                if( isLocationValid() ) {
                    addMarker({lat: scope.location.lat, lng: scope.location.lng});
                }

                var currentLayer = 0, currentGeojson, currentGeojsonFeatures;

                var contextMenuOptions = {};
                contextMenuOptions.classNames = {menu: 'map_context_menu', menuSeparator: 'map_context_menu_separator'};

                //create an array of MapContextMenuItem objects            
                var menuItems = [];
                menuItems.push({className: 'map_context_menu_item', eventName: 'captureCoordinate', label: '<i class="fa fa-map-marker"></i>   ' + $translate.instant('set_coordinate')});
                menuItems.push({});
                menuItems.push({className: 'map_context_menu_item', eventName: 'zoom_in', id: 'zoomIn', label: '<i class="fa fa-search-plus"></i>   ' + $translate.instant('zoom_in')});
                menuItems.push({className: 'map_context_menu_item', eventName: 'zoom_out', id: 'zoomOut', label: '<i class="fa fa-search-minus"></i>   ' + $translate.instant('zoom_out')});
                menuItems.push({});
                menuItems.push({className: 'map_context_menu_item', eventName: 'centerMap', label: '<i class="fa fa-crosshairs"></i>   ' + $translate.instant('center_map')});
                contextMenuOptions.menuItems = menuItems;
                var mapContextMenu = new MapContextMenu(map, contextMenuOptions);

                function getGeoJsonByOuLevel(initialize, event, mode) {                    
                    var url = '';
                    if (initialize) {
                        currentLayer = 0;
                        url = '../api/organisationUnits.geojson?level=' + ouLevels[currentLayer].level;
                    }
                    else {
                        if (mode === 'IN') {
                            currentLayer++;                            
                            url = '../api/organisationUnits.geojson?level=' + ouLevels[currentLayer].level + '&parent=' + event.feature.getId();
                        }
                        if (mode === 'OUT') {
                            currentLayer--;
                            var parents = event.feature.getProperty('parentGraph').substring(1, event.feature.getProperty('parentGraph').length - 1).split('/');
                            url = '../api/organisationUnits.geojson?level=' + ouLevels[currentLayer].level + '&parent=' + parents[parents.length - 2];
                        }
                    }

                    $http.get(url).then(function (response) {
                        currentGeojson = response.data;
                        currentGeojsonFeatures = map.data.addGeoJson(currentGeojson);

                        if (initialize) {
                            google.maps.event.addListenerOnce(map, 'idle', function () {
                                google.maps.event.trigger(map, 'resize');
                                map.data.setStyle(featureStyle);
                                centerMap();
                            });
                        }
                        else {
                            centerMap();
                        }
                    });
                }

                function addMarker(loc) {
                    var latLng = new google.maps.LatLng(loc.lat, loc.lng);
                    marker.setPosition(latLng);
                }

                function applyMarker(event) {
                    addMarker({
                        lat: event.latLng.lat(),
                        lng: event.latLng.lng()
                    });

                    scope.location = {lat: event.latLng.lat(), lng: event.latLng.lng()};
                    CurrentSelection.setLocation(scope.location);
                }

                function centerMap() {
                    if (currentGeojson && currentGeojson.features) {                        
                        var latLngBounds = getMapCenter(currentGeojson);                        
                        if( isLocationValid() ){
                            if( latLngBounds.contains(marker.getPosition()) ){                                
                                map.fitBounds(latLngBounds);
                                map.panToBounds(latLngBounds);
                                return;
                            }
                            else{
                                map.setCenter(centerLatLng);
                                return; 
                            }
                        }
                        
                        map.fitBounds(latLngBounds);
                        map.panToBounds(latLngBounds);
                        return;
                    }
                    
                    map.setCenter(centerLatLng);
                }

                var overLays = [];
                function getMapCenter(geoJson) {
                    map.data.setStyle(featureStyle);
                    if (!geoJson || !geoJson.features) {
                        return;
                    }

                    var latLngBounds = new google.maps.LatLngBounds();
                    overLays = [];
                    angular.forEach(geoJson.features, function (feature) {
                        var customTxt = '<div>' + feature.properties.name + '</div>';
                        if (feature.geometry.type === 'MultiPolygon') {
                            var polygonPoints = new google.maps.LatLngBounds();
                            angular.forEach(feature.geometry.coordinates[0][0], function (coordinate) {
                                latLngBounds.extend(new google.maps.LatLng(coordinate[1], coordinate[0]));
                                polygonPoints.extend(new google.maps.LatLng(coordinate[1], coordinate[0]));
                            });
                            var txt = new Dhis2TextOverlay(polygonPoints.getCenter(), customTxt, "polygon-name", map);
                            overLays.push(txt);
                        }
                        else if (feature.geometry.type === 'Point') {
                            latLngBounds.extend(new google.maps.LatLng(feature.geometry.coordinates[1], feature.geometry.coordinates[0]));
                            var txt = new Dhis2TextOverlay(new google.maps.LatLng(feature.geometry.coordinates[1], feature.geometry.coordinates[0]), customTxt, "polygon-name", map);
                            overLays.push(txt);
                        }
                    });

                    return latLngBounds;
                }

                function initializeMap() {
                    if(ouLevels && ouLevels.length > 0){
                        getGeoJsonByOuLevel(true, null);
                    }
                    else{
                        google.maps.event.addListenerOnce(map, 'idle', function () {
                            google.maps.event.trigger(map, 'resize');
                            map.data.setStyle(featureStyle);
                            centerMap();
                        });
                    }
                }

                function zoomMap(event, mode) {

                    for (var i = 0; i < currentGeojsonFeatures.length; i++) {
                        map.data.remove(currentGeojsonFeatures[i]);
                    }

                    for (var i = 0; i < overLays.length; i++) {
                        overLays[i].setMap(null);
                    }
                    
                    if(ouLevels && ouLevels.length > 0){
                        getGeoJsonByOuLevel(false, event, mode);
                    }                    
                }

                function enableDisableZoom() {
                    if (currentLayer >= ouLevels.length - 1) {
                        $("#zoomIn").addClass('disabled-context-menu-item');
                        $("#zoomIn").removeClass('mouse-pointer');
                        $('#zoomIn').attr('disabled', "disabled");
                    } else {
                        $("#zoomIn").removeClass('disabled-context-menu-item');
                        $("#zoomIn").addClass('enable-context-menu-item');
                        $('#zoomIn').attr('disabled', "");
                    }
                    if (currentLayer === 0) {
                        $("#zoomOut").addClass('disabled-context-menu-item');
                        $("#zoomOut").removeClass('enable-context-menu-item');
                        $('#zoomOut').attr('disabled', "disabled");
                    }
                    else {
                        $("#zoomOut").removeClass('disabled-context-menu-item');
                        $("#zoomOut").addClass('enable-context-menu-item');
                        $('#zoomIn').attr('disabled', "");
                    }
                }

                function showHideContextMenu(event, allowDisable) {
                    if (contextMenuDisplayed) {
                        mapContextMenu.hide();
                        contextMenuDisplayed = false;
                    }
                    else {
                        mapContextMenu.show(event);
                        if (allowDisable) {
                            enableDisableZoom();
                        }
                    }
                }

                //get lable for current polygon
                map.data.addListener('mouseover', function (e) {
                    $("#polygon-label").text(e.feature.getProperty('name'));
                    map.data.revertStyle();
                    map.data.overrideStyle(e.feature, {fillOpacity: 0.2});
                });

                //remove polygon label
                map.data.addListener('mouseout', function () {
                    $("#polygon-label").text('');
                    map.data.revertStyle();
                });

                //context menu based on polygons assigned to orgunits
                map.data.addListener('rightclick', function (e) {
                    showHideContextMenu(e, true);
                });

                map.data.addListener('click', function (e) {
                    showHideContextMenu(e, true);
                });

                //context menu based on points assigned to orgunits
                google.maps.event.addListener(marker, 'rightclick', function (e) {
                    showHideContextMenu(e, true);
                });

                google.maps.event.addListener(marker, 'click', function (e) {
                    showHideContextMenu(e, true);
                });

                //context menu anywhere in the map - incase no polygons are defined
                google.maps.event.addListener(map, 'rightclick', function (e) {
                    showHideContextMenu(e, false);
                });

                google.maps.event.addListener(map, 'click', function (e) {
                    showHideContextMenu(e, false);
                });

                //listen for the clicks on mapContextMenu
                google.maps.event.addListener(mapContextMenu, 'menu_item_selected', function (e, latLng, eventName) {
                    switch (eventName) {
                        case 'zoom_in':
                            if (e.feature) {
                                zoomMap(e, 'IN');
                            }
                            else {
                                map.setZoom(map.getZoom() + 1);
                            }

                            break;
                        case 'zoom_out':
                            if (e.feature) {
                                zoomMap(e, 'OUT');
                            }
                            else {
                                map.setZoom(map.getZoom() - 1);
                            }

                            break;
                        case 'centerMap':
                            contextMenuDisplayed = true;
                            map.panTo(latLng);
                            break;
                        case 'captureCoordinate':
                            contextMenuDisplayed = true;
                            applyMarker(e);
                            break;
                    }
                });

                initializeMap();
            } 
            if ($window.google && $window.google.maps) {                
                renderMap();
            } else {
                lazyLoadApi().then(function () {                  
                    renderMap();            
                }, function () {
                    console.log('GM loading failed.');
                });
            }
        }
    };
})
