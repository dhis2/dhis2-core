/* global moment, angular, directive, dhis2, selection */

'use strict';

/* Directives */

var d2Directives = angular.module('d2Directives', [])


.directive('selectedOrgUnit', function ($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            
            $("#orgUnitTree").one("ouwtLoaded", function (event, ids, names) {
                if (dhis2.tc && dhis2.tc.metaDataCached) {
                    $timeout(function () {
                        scope.treeLoaded = true;
                        scope.$apply();
                    });
                    selection.responseReceived();
                }
                else {
                    console.log('Finished loading orgunit tree');
                    $("#orgUnitTree").addClass("disable-clicks"); //Disable ou selection until meta-data has downloaded
                    $timeout(function () {
                        scope.treeLoaded = true;
                        scope.$apply();
                    });
                    downloadMetaData();
                }
            });

            //listen to user selection, and inform angular         
            selection.setListenerFunction(setSelectedOu, true);
            function setSelectedOu(ids, names) {
                var ou = {id: ids[0], name: names[0]};
                $timeout(function () {
                    scope.selectedOrgUnit = ou;
                    scope.$apply();
                });
            }
        }
    };
})

.directive('d2SetFocus', function ($timeout) {

    return {        
        scope: { trigger: '@d2SetFocus' },
        link: function(scope, element) {
            scope.$watch('trigger', function(value) {
                if(value === "true") { 
                    $timeout(function() {
                        element[0].focus(); 
                    });
                }
            });
        }
    };
})

.directive('d2LeftBar', function () {

    return {
        restrict: 'E',
        templateUrl: 'views/left-bar.html',
        link: function (scope, element, attrs) {

            $("#searchIcon").click(function () {
                $("#searchSpan").toggle();
                $("#searchField").focus();
            });

            $("#searchField").autocomplete({
                source: "../dhis-web-commons/ouwt/getOrganisationUnitsByName.action",
                select: function (event, ui) {
                    $("#searchField").val(ui.item.value);
                    selection.findByName();
                }
            });
        }
    };
})

.directive('blurOrChange', function () {

    return function (scope, elem, attrs) {
        elem.calendarsPicker({
            onSelect: function () {
                scope.$apply(attrs.blurOrChange);
                $(this).change();
            }
        }).change(function () {
            scope.$apply(attrs.blurOrChange);
        });
    };
})

.directive('d2Enter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if (event.which === 13) {
                scope.$apply(function () {
                    scope.$eval(attrs.d2Enter);
                });
                event.preventDefault();
            }
        });
    };
})

.directive('d2PopOver', function ($compile, $templateCache) {

    return {
        restrict: 'EA',
        scope: {
            content: '=',
            title: '@details',
            template: "@template"
        },
        link: function (scope, element, attrs) {
            var content = $templateCache.get("popover.html");
            content = $compile(content)(scope);
            scope.content.heading = scope.content.value && scope.content.value.length > 20 ? scope.content.value.substring(0,20).concat('...') : scope.content.value;
            var options = {
                content: content,
                placement: 'auto',
                trigger: 'hover',
                html: true,
                title: scope.title
            };
            element.popover(options);
        }
    };
})

.directive('d2Sortable', function ($timeout) {

    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            element.sortable({
                connectWith: ".connectedSortable",
                placeholder: "ui-state-highlight",
                tolerance: "pointer",
                handle: '.handle',
                change: function (event, ui) {
                    $timeout(function () {
                        scope.widgetsOrder = getSortedItems(ui);
                        scope.$apply();
                    });

                },
                receive: function (event, ui) {
                    $timeout(function () {
                        scope.widgetsOrder = getSortedItems(ui);
                        scope.$apply();
                    });
                }
            });

            var getSortedItems = function (ui) {
                var biggerWidgets = $("#biggerWidget").sortable("toArray");
                var smallerWidgets = $("#smallerWidget").sortable("toArray");
                var movedIsIdentifeid = false;

                //look for the moved item in the bigger block
                for (var i = 0; i < biggerWidgets.length && !movedIsIdentifeid; i++) {
                    if (biggerWidgets[i] === "") {
                        biggerWidgets[i] = ui.item[0].id;
                        movedIsIdentifeid = true;
                    }
                }

                //look for the moved item in the smaller block
                for (var i = 0; i < smallerWidgets.length && !movedIsIdentifeid; i++) {
                    if (smallerWidgets[i] === "") {
                        smallerWidgets[i] = ui.item[0].id;
                        movedIsIdentifeid = true;
                    }
                }
                return {smallerWidgets: smallerWidgets, biggerWidgets: biggerWidgets};
            };
        }
    };
})

.directive('serversidePaginator', function factory() {

    return {
        restrict: 'E',
        controller: function ($scope, Paginator) {
            $scope.paginator = Paginator;
        },
        templateUrl: '../dhis-web-commons/angular-forms/serverside-pagination.html'
    };
})

.directive('d2CustomForm', function ($compile) {
    return{
        restrict: 'E',
        link: function (scope, elm, attrs) {
            scope.$watch('customForm', function () {
                if (angular.isObject(scope.customForm)) {
                    elm.html(scope.customForm.htmlCode);
                    $compile(elm.contents())(scope);
                }
            });
        }
    };
})

/* TODO: this directive access an element #contextMenu somewhere in the document. Looks like it has to be rewritten */
.directive('d2ContextMenu', function () {

    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var contextMenu = $("#contextMenu");

            element.click(function (e) {
                var menuHeight = contextMenu.height();
                var menuWidth = contextMenu.width();
                var winHeight = $(window).height();
                var winWidth = $(window).width();

                var pageX = e.pageX;
                var pageY = e.pageY;

                contextMenu.show();

                if ((menuWidth + pageX) > winWidth) {
                    pageX -= menuWidth;
                }

                if ((menuHeight + pageY) > winHeight) {
                    pageY -= menuHeight;

                    if (pageY < 0) {
                        pageY = e.pageY;
                    }
                }

                contextMenu.css({
                    left: pageX,
                    top: pageY
                });

                return false;
            });

            contextMenu.on("click", "a", function () {
                contextMenu.hide();
            });

            $(document).click(function () {
                contextMenu.hide();
            });
        }
    };
})

.directive('d2Date', function (CalendarService, $parse) {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function (scope, element, attrs, ctrl) {
            var calendarSetting = CalendarService.getSetting();
            var dateFormat = 'yyyy-mm-dd';
            if (calendarSetting.keyDateFormat === 'dd-MM-yyyy') {
                dateFormat = 'dd-mm-yyyy';
            }

            var minDate = $parse(attrs.minDate)(scope),
                    maxDate = $parse(attrs.maxDate)(scope),
                    calendar = $.calendars.instance(calendarSetting.keyCalendar);

            element.calendarsPicker({
                changeMonth: true,
                dateFormat: dateFormat,
                yearRange: '-120:+30',
                minDate: minDate,
                maxDate: maxDate,
                calendar: calendar,
                duration: "fast",
                showAnim: "",
                renderer: $.calendars.picker.themeRollerRenderer,
                onSelect: function () {
                    $(this).change();
                }
            })
                    .change(function () {
                        ctrl.$setViewValue(this.value);
                        this.focus();
                        scope.$apply();
                    });
        }
    };
})

.directive('d2FileInput', function(DHIS2EventService, DHIS2EventFactory, FileService, DialogService){
    
    return {
        restrict: "A",
        scope: {
            d2FileInputList: '=',
            d2FileInput: '=',
            d2FileInputName: '=',
            d2FileInputCurrentName: '=',
            d2FileInputPs: '='
        },
        link: function (scope, element, attrs) {
            
            var de = attrs.inputFieldId;
            
            var updateModel = function () {
                
                var update = scope.d2FileInput.event &&  scope.d2FileInput.event !== 'SINGLE_EVENT' ? true : false;
                
                FileService.upload(element[0].files[0]).then(function(data){
                    
                    if(data && data.status === 'OK' && data.response && data.response.fileResource && data.response.fileResource.id && data.response.fileResource.name){
                                            
                        scope.d2FileInput[de] = data.response.fileResource.id;   
                        scope.d2FileInputCurrentName[de] = data.response.fileResource.name;
                        if( update ){                            
                            if(!scope.d2FileInputName[scope.d2FileInput.event]){
                                scope.d2FileInputName[scope.d2FileInput.event] = [];
                            }                            
                            scope.d2FileInputName[scope.d2FileInput.event][de] = data.response.fileResource.name;
                            
                            var updatedSingleValueEvent = {event: scope.d2FileInput.event, dataValues: [{value: data.response.fileResource.id, dataElement:  de}]};
                            var updatedFullValueEvent = DHIS2EventService.reconstructEvent(scope.d2FileInput, scope.d2FileInputPs.programStageDataElements);
                            DHIS2EventFactory.updateForSingleValue(updatedSingleValueEvent, updatedFullValueEvent).then(function(data){
                                scope.d2FileInputList = DHIS2EventService.refreshList(scope.d2FileInputList, scope.d2FileInput);
                            });
                        }
                    }
                    else{
                        var dialogOptions = {
                            headerText: 'error',
                            bodyText: 'file_upload_failed'
                        };		
                        DialogService.showDialog({}, dialogOptions);
                    }
                    
                });                 
            };             
            element.bind('change', updateModel);            
        }
    };    
})

.directive('d2FileInputDelete', function($parse, $timeout, FileService, DialogService){
    
    return {
        restrict: "A",
        link: function (scope, element, attrs) {
            var valueGetter = $parse(attrs.d2FileInputDelete);
            var nameGetter = $parse(attrs.d2FileInputName);
            var nameSetter = nameGetter.assign;
            
            if(valueGetter(scope)) {
                FileService.get(valueGetter(scope)).then(function(data){
                    if(data && data.name && data.id){
                        $timeout(function(){
                            nameSetter(scope, data.name);
                            scope.$apply();
                        });
                    }
                    else{
                        var dialogOptions = {
                            headerText: 'error',
                            bodyText: 'file_missing'
                        };		
                        DialogService.showDialog({}, dialogOptions);
                    }                    
                });                 
            }
        }
    };    
})

.directive('d2Audit', function () {
        return {
            restrict: 'E',
            template: '<i class="glyphicon glyphicon-user audit-icon" data-ng-click="showAuditHistory()" ng-if="showAuditIcon()"></i>',
            scope:{
                dataElementId: '@dataelementId',
                dataElementName: '@dataelementName',
                currentEvent:'@',
                type:'@',
                selectedTeiId:'@'
            },
            controller:function($scope, $modal) {
                if (!$scope.dataElementId) {
                    return;
                }

                $scope.showAuditIcon = function() {
                    if ($scope.currentEvent && $scope.currentEvent !== 'SINGLE_EVENT') {
                        return true;
                    }
                    if ($scope.type === "attribute" && $scope.selectedTeiId) {
                        return true;
                    }
                    return false;
                }

                $scope.showAuditHistory = function() {

                    $modal.open({
                        templateUrl: "../dhis-web-commons/angular-forms/audit-history.html",
                        controller: "AuditHistoryController",
                        resolve: {
                            dataElementId: function () {
                                return $scope.dataElementId;
                            },
                            dataElementName: function () {
                                return $scope.dataElementName;
                            },
                            dataType: function() {
                                return $scope.type;
                            },
                            currentEvent: function() {
                                if($scope.currentEvent === "SINGLE_EVENT") {
                                    alert("Single Event !!!");
                                }
                                return $scope.currentEvent;
                            },
                            selectedTeiId: function() {
                                return $scope.selectedTeiId;
                            }
                        }
                    })

                }

            }
        };
    });


