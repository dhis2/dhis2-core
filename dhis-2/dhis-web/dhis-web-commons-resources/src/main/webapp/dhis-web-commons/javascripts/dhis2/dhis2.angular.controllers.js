'use strict';

/* Controllers */
var d2Controllers = angular.module('d2Controllers', [])

//Controller for column show/hide
.controller('ColumnDisplayController', 
    function($scope, 
            $modalInstance,
            hiddenGridColumns,
            gridColumns,
            saveGridColumns){
    
    $scope.gridColumns = gridColumns;
    $scope.hiddenGridColumns = hiddenGridColumns;
    
    $scope.close = function () {
        $modalInstance.close($scope.gridColumns);
    };
    
    $scope.showHideColumns = function(gridColumn){
       
        if(gridColumn.show){                
            $scope.hiddenGridColumns--;            
        }
        else{
            $scope.hiddenGridColumns++;            
        }
        saveGridColumns($scope.gridColumns);
    };    
})

//controller for dealing with google map
.controller('MapController',
        function($scope, 
                $modalInstance,                
                $translate,
                $http,
                $window,
                storage,
                leafletData,
                CurrentSelection,
                DHIS2URL,
                NotificationService,
                location) {
    
    $scope.location = location;
    
    var currentLevel = 0, ouLevels = CurrentSelection.getOuLevels();
    
    $scope.maxZoom = 8;
    
    $scope.center = {lat: 8.88, lng: -11.55, zoom: $scope.maxZoom};
    
    var systemSetting = storage.get('SYSTEM_SETTING');
    
    var setCoordinateLabel = '<i class="fa fa-map-marker fa-2x"></i><span class="small-horizontal-spacing">' + $translate.instant('set_coordinate') + '</span>';
    var zoomInLabel = '<i class="fa fa-search-plus fa-2x"></i><span class="small-horizontal-spacing">' + $translate.instant('zoom_in') + '</span>';
    var zoomOutLabel = '<i class="fa fa-search-minus fa-2x"></i><span class="small-horizontal-spacing">' + $translate.instant('zoom_out') + '</span>';
    var centerMapLabel = '<i class="fa fa-crosshairs fa-2x"></i><span class="small-horizontal-spacing">' + $translate.instant('center_map') + '</span>';
    
    var contextmenuItems = [{
                                text: setCoordinateLabel,
                                callback: setCoordinate,
                                index: 0
                            },
                            {
                                separator: true,
                                index: 1
                            },
                            {
                                text: zoomInLabel,
                                callback: zoomIn,
                                index: 2
                            },
                            {
                                text: zoomOutLabel,
                                callback: zoomOut,
                                index: 3
                            },
                            {
                                separator: true,
                                index: 4
                            },
                            {
                                text: centerMapLabel,
                                callback: centerMap,
                                index: 5
                            }];
                        
    $scope.mapDefaults = {map: {
                            contextmenu: true,
                            contextmenuWidth: 180,
                            contextmenuItems: contextmenuItems
                        }};
    
    var geojsonMarkerOptions = {
			    radius: 15,
			    fillColor: '#ff7800',
			    color: '#000',
			    weight: 1,
			    opacity: 1,
			    fillOpacity: 0.8
			};
                        
    var style = {fillColor: "green",
                    weight: 1,
                    opacity: 0.8,
                    color: 'black',
                    fillOpacity: 0
                };

    $scope.marker = $scope.location && $scope.location.lat && $scope.location.lng ? {m1: {lat: $scope.location.lat, lng: $scope.location.lng, draggable: true}} : {};
    
    function userNotification(headerMessage, errorMesage ){
        var dialogOptions = {
            headerText: headerMessage,
            bodyText: errorMesage
        };
        DialogService.showDialog({}, dialogOptions);
        return;
    };
    
    function highlightFeature(e) {
        var layer = e.target;

        layer.setStyle({
                weight: 5,
                color: '#666',
                dashArray: '',
                fillOpacity: 0.5
        });

        if (!L.Browser.ie && !L.Browser.opera && !L.Browser.edge) {
            layer.bringToFront();
        }
    }
    
    function resetHighlight(e) {
        var layer = e.target;
        layer.setStyle( style );
    }
    
    function pointToLayer( feature, latlng ){
        return L.circleMarker(latlng, geojsonMarkerOptions);
    };
    
    function onEachFeature(feature, layer) {
        
        layer.on("mouseover",function(e){            
            $("#polygon-label").text( feature.properties.name );            
            //highlightFeature(e);
        });
        layer.on("mouseout",function(e){
            $("#polygon-label").text('');
            //resetHighlight(e);
        });
        
        
        if( layer._layers ){
            layer.eachLayer(function (l) {
                l.bindContextMenu({
                    contextmenu: true,
                    contextmenuWidth: 180,                
                    contextmenuItems: [{
                                    text: setCoordinateLabel,
                                    callback: function(e){
                                        setCoordinate(e, feature);
                                    },
                                    index: 0
                                },
                                {
                                    separator: true,
                                    index: 1
                                },
                                {
                                    text: zoomInLabel,
                                    callback: function(e){
                                        zoomIn(e, feature);
                                    },
                                    index: 2
                                },
                                {
                                    text: zoomOutLabel,
                                    callback: function(e){
                                        zoomOut(e, feature);
                                    },
                                    index: 3,
                                    disabled: currentLevel < 1
                                },
                                {
                                    separator: true,
                                    index: 4
                                },
                                {
                                    text: centerMapLabel,
                                    callback: function(e){
                                        centerMap(e, feature);
                                    },
                                    index: 5
                                }]
                });
            });
        }
        else{
            layer.bindContextMenu({
                    contextmenu: true,
                    contextmenuWidth: 180,
                    contextmenuInheritItems: false,
                    contextmenuItems: [{
                                    text: setCoordinateLabel,
                                    callback: function(e){
                                        setCoordinate(e, feature, layer);
                                    },
                                    index: 0
                                },
                                {
                                    separator: true,
                                    index: 1
                                },
                                {
                                    text: zoomInLabel,
                                    callback: function(e){
                                        zoomIn(e, feature);
                                    },
                                    disabled: true,
                                    index: 2
                                },
                                {
                                    text: zoomOutLabel,
                                    callback: function(e){
                                        zoomOut(e, feature);
                                    },
                                    index: 3
                                },
                                {
                                    separator: true,
                                    index: 4
                                },
                                {
                                    text: centerMapLabel,
                                    callback: function(e){
                                        centerMap(e, feature);
                                    },
                                    index: 5
                                }]
                });
        }        
    }
    
            
    function getGeoJsonByOuLevel(initialize, event, mode) {                    
        var url = null, parent = null;
        if (initialize) {
            currentLevel = 0;
            url = DHIS2URL + '/organisationUnits.geojson?level=' + ouLevels[currentLevel].level;
        }
        else {
            if (mode === 'IN') {
                currentLevel++;
                parent = event.id;
            }
            if (mode === 'OUT') {
                currentLevel--;                
                var parents = event.properties.parentGraph.substring(1, event.properties.parentGraph.length - 1).split('/');
                parent = parents[parents.length - 2];
            }
            
            if( ouLevels[currentLevel] && ouLevels[currentLevel].level && parent && !initialize ){
                url = url = DHIS2URL + '/organisationUnits.geojson?level=' + ouLevels[currentLevel].level + '&parent=' + parent;
            }
        }

        if( url ){
            
            $http.get(url).success(function (data) {

                $scope.currentGeojson = {data: data, style: style, onEachFeature: onEachFeature, pointToLayer: pointToLayer};
                
                leafletData.getMap().then(function( map ){

                    //var L = $window.L;                
                    var latlngs = [];             

                    angular.forEach($scope.currentGeojson.data.features, function(feature){                
                        if( feature.geometry.type === "Point"){
                            //latlngs.push( L.latLng( $scope.currentGeojson.data.features[0].geometry.coordinates) );
                            //isPoints = true;
                        }
                        else{
                            for (var i in feature.geometry.coordinates) {                        
                                var coord = feature.geometry.coordinates[i];                    
                                for (var j in coord) {
                                    var points = coord[j];
                                    for (var k in points) {
                                        latlngs.push(L.GeoJSON.coordsToLatLng(points[k]));
                                    }
                                }
                            }                        
                        }
                    });
                    
                    if( $scope.location && $scope.location.lat && $scope.location.lng ){
                        map.setView([$scope.location.lat, $scope.location.lng], $scope.maxZoom);
                    } 
                    else{
                        if( latlngs.length > 0 ){                            
                            map.fitBounds(latlngs, {maxZoom: $scope.maxZoom});
                        }
                    }
                });
            });
        }
    }
    
    leafletData.getMap().then(function( map ){
        
        if( $scope.marker && $scope.marker.m1 && $scope.marker.m1.lat && $scope.marker.m1.lng ){
            map.setView([$scope.marker.m1.lat, $scope.marker.m1.lng], $scope.maxZoom);
        }        
        
        var L = $window.L;        
        $scope.geocoder = L.control.geocoder(systemSetting.keyMapzenSearchApiKey ? systemSetting.keyMapzenSearchApiKey : 'search-Se1CFzK',{
            markers: {
                draggable: true
            }
        }).addTo(map);

        $scope.geocoder.on('select', function (e) {
            
            $scope.marker = {};
            $scope.location = {lat: e.latlng.lat, lng: e.latlng.lng};
            
            
            $scope.geocoder.marker.on('dragend', function(e){                
                var c = e.target.getLatLng();
                $scope.location = {lat: c.lat, lng: c.lng};
            });
            
        });                
    });
                
    getGeoJsonByOuLevel(true);
    
    function zoomMap(event, mode) {
        if(ouLevels && ouLevels.length > 0){
            getGeoJsonByOuLevel(false, event, mode);
        }                    
    }
    
    function setCoordinate(e, feature, layer){
        if( feature && feature.geometry && feature.geometry.type === 'Point'){
            var m = feature.geometry.coordinates;            
            $scope.marker = {m1: {lat: m[1], lng: m[0], draggable: true}};
        }
        else{
            $scope.marker = {m1: {lat: e.latlng.lat, lng: e.latlng.lng, draggable: true}};
        }
        
        $scope.location = {lat: $scope.marker.m1.lat, lng: $scope.marker.m1.lng};
    };
    
    function zoomIn(e, feature){
        $scope.maxZoom += 2; 
        if( feature && feature.id ){            
            zoomMap( feature, 'IN');
        }
        else{            
            $scope.center = angular.copy(e.latlng);            
            $scope.center.zoom = $scope.maxZoom;
        }        
    };
    
    function zoomOut(e, feature){
        $scope.maxZoom -= 2;
        if( feature && feature.id ){             
            zoomMap( feature, 'OUT');
        }
        else{
            $scope.center = angular.copy(e.latlng);            
            $scope.center.zoom = $scope.maxZoom;
        }
    };
    
    function centerMap(e, feature){
        $scope.maxZoom += 2;
        $scope.center.lat = e.latlng.lat;
        $scope.center.lng = e.latlng.lng;
    };
    
    $scope.close = function () {
        $modalInstance.close();
    };
    
    $scope.captureCoordinate = function(){
        if( $scope.location && $scope.location.lng && $scope.location.lat ){
            $modalInstance.close( $scope.location );
    	}
    	else{
    		//notify user
            NotificationService.showNotifcationDialog($translate.instant("error"),
                $translate.instant("nothing_captured"));
            return;
    	}
    };
    
    $scope.$on('leafletDirectiveMarker.dragend', function (e, args) {        
        $scope.marker.m1.lng = args.model.lng;
        $scope.marker.m1.lat = args.model.lat;
        
        $scope.location = {lng: args.model.lng, lat: args.model.lat};
    });
})

//Controller for audit history
.controller('AuditHistoryController', 
    function ($scope, 
            $modalInstance,
            $translate,
            AuditHistoryDataService, 
            DateUtils, 
            eventId, 
            dataType, 
            nameIdMap,
            optionSets,
            CommonUtils) {


    $scope.model = {type: dataType, 
    				name: dataType === 'dataElement' ? $translate.instant('data_element') : $translate.instant('attribute'),
    				searchPlaceholder: dataType === 'dataElement' ? $translate.instant('search_by_data_element') : $translate.instant('search_by_attribute'),
                    auditColumns: ['name', 'auditType', 'value', 'modifiedBy', 'created'], itemList:[], uniqueRows:[]};

    $scope.close = function () {
        $modalInstance.close();
    };
    
    AuditHistoryDataService.getAuditHistoryData(eventId, dataType).then(function (data) {

        $scope.model.itemList = [];
        $scope.model.uniqueRows = [];
        
        var reponseData = data.trackedEntityDataValueAudits ? data.trackedEntityDataValueAudits :
            data.trackedEntityAttributeValueAudits ? data.trackedEntityAttributeValueAudits : null;

        if (reponseData) {
            for (var index = 0; index < reponseData.length; index++) {                
                var dataValue = reponseData[index];                
                var audit = {}, obj = {};
                if (dataType === "attribute") {
                    if (nameIdMap[dataValue.trackedEntityAttribute.id]) {
                        obj = nameIdMap[dataValue.trackedEntityAttribute.id];
                        audit.name = obj.displayName;
                        audit.valueType = obj.valueType;
                    }
                } else if (dataType === "dataElement") {
                    if (nameIdMap[dataValue.dataElement.id] && nameIdMap[dataValue.dataElement.id].dataElement) {
                        obj = nameIdMap[dataValue.dataElement.id].dataElement;
                        audit.name = obj.displayFormName;
                        audit.valueType = obj.valueType;
                    }
                }
                
                dataValue.value = CommonUtils.formatDataValue(null, dataValue.value, obj, optionSets, 'USER');
                audit.auditType = dataValue.auditType;                
                audit.value = dataValue.value;
                audit.modifiedBy = dataValue.modifiedBy;
                audit.created = DateUtils.formatToHrsMinsSecs(dataValue.created);                
                
                $scope.model.itemList.push(audit);
                if( $scope.model.uniqueRows.indexOf(audit.name) === -1){
                	$scope.model.uniqueRows.push(audit.name);
                }
                
                if($scope.model.uniqueRows.length > 0){
                	$scope.model.uniqueRows = $scope.model.uniqueRows.sort();
                }
            }
        }
    });
})

.controller('OrgUnitTreeController', function($scope, $modalInstance, OrgUnitFactory, orgUnitId) {

    $scope.model = {selectedOrgUnitId: orgUnitId ? orgUnitId : null};

    function expandOrgUnit( orgUnit, ou ){
        if( ou.path.indexOf( orgUnit.path ) !== -1 ){
            orgUnit.show = true;
        }

        orgUnit.hasChildren = orgUnit.children && orgUnit.children.length > 0 ? true : false;
        if( orgUnit.hasChildren ){
            for( var i=0; i< orgUnit.children.length; i++){
                if( ou.path.indexOf( orgUnit.children[i].path ) !== -1 ){
                    orgUnit.children[i].show = true;
                    expandOrgUnit( orgUnit.children[i], ou );
                }
            }
        }
        return orgUnit;
    };

    function attachOrgUnit( orgUnits, orgUnit ){
        for( var i=0; i< orgUnits.length; i++){
            if( orgUnits[i].id === orgUnit.id ){
                orgUnits[i] = orgUnit;
                orgUnits[i].show = true;
                orgUnits[i].hasChildren = orgUnits[i].children && orgUnits[i].children.length > 0 ? true : false;
                return;
            }
            if( orgUnits[i].children && orgUnits[i].children.length > 0 ){
                attachOrgUnit(orgUnits[i].children, orgUnit);
            }
        }
        return orgUnits;
    };

    //Get orgunits for the logged in user
    OrgUnitFactory.getViewTreeRoot().then(function(response) {
        $scope.orgUnits = response.organisationUnits;
        var selectedOuFetched = false;
        var levelsFetched = 0;
        angular.forEach($scope.orgUnits, function(ou){
            ou.show = true;
            levelsFetched = ou.level;
            if( orgUnitId && orgUnitId === ou.id ){
                selectedOuFetched = true;
            }
            angular.forEach(ou.children, function(o){
                levelsFetched = o.level;
                o.hasChildren = o.children && o.children.length > 0 ? true : false;
                if( orgUnitId && !selectedOuFetched && orgUnitId === ou.id ){
                    selectedOuFetched = true;
                }
            });
        });

        levelsFetched = levelsFetched > 0 ? levelsFetched - 1 : levelsFetched;

        if( orgUnitId && !selectedOuFetched ){
            var parents = null;
            OrgUnitFactory.get( orgUnitId ).then(function( ou ){
                if( ou && ou.path ){
                    parents = ou.path.substring(1, ou.path.length);
                    parents = parents.split("/");
                    if( parents && parents.length > 0 ){
                        var url = "fields=id,displayName,path,level,";
                        for( var i=levelsFetched; i<ou.level; i++){
                            url = url + "children[id,displayName,level,path,";
                        }

                        url = url.substring(0, url.length-1);
                        for( var i=levelsFetched; i<ou.level; i++){
                            url = url + "]";
                        }

                        OrgUnitFactory.getOrgUnits(parents[levelsFetched], url).then(function(response){
                            if( response && response.organisationUnits && response.organisationUnits[0] ){
                                response.organisationUnits[0].show = true;
                                response.organisationUnits[0].hasChildren = response.organisationUnits[0].children && response.organisationUnits[0].children.length > 0 ? true : false;
                                response.organisationUnits[0] = expandOrgUnit(response.organisationUnits[0], ou );
                                $scope.orgUnits = attachOrgUnit( $scope.orgUnits, response.organisationUnits[0] );
                            }
                        });
                    }
                }
            });
        }
    });


    //expand/collapse of search orgunit tree
    $scope.expandCollapse = function(orgUnit) {
        if( orgUnit.hasChildren ){
            //Get children for the selected orgUnit
            OrgUnitFactory.getChildren(orgUnit.id).then(function(ou) {
                orgUnit.show = !orgUnit.show;
                orgUnit.hasChildren = false;
                orgUnit.children = ou.children;
                angular.forEach(orgUnit.children, function(ou){
                    ou.hasChildren = ou.children && ou.children.length > 0 ? true : false;
                });
            });
        }
        else{
            orgUnit.show = !orgUnit.show;
        }
    };

    $scope.setSelectedOrgUnit = function( orgUnitId ){
        $scope.model.selectedOrgUnitId = orgUnitId;
    };

    $scope.select = function () {
        $modalInstance.close( $scope.model.selectedOrgUnitId );
    };

    $scope.close = function(){
        $modalInstance.close();
    };
});
