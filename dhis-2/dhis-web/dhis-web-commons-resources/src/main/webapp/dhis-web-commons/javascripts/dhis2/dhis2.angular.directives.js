/* global moment, angular, directive, dhis2, selection */

'use strict';

/* Directives */

var d2Directives = angular.module('d2Directives', [])


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

.directive('selectedOrgUnit', function ($timeout, OrgUnitService, SessionStorageService) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            //once ou tree is loaded, start meta-data download
            $(function () {
                dhis2.ou.store.open().done(function () {
                    if(dhis2.tc && dhis2.tc.metaDataCached){
                    	selection.load();
                        var ouId = SessionStorageService.get('ouSelected');
                        OrgUnitService.get(ouId).then(function(ou){
                            if(ou && ou.id && ou.name){                                    
                                $timeout(function () {
                                    scope.selectedOrgUnit = ou;
                                    scope.treeLoaded = true;
                                    scope.$apply();
                                });
                            }                                                       
                        });
                    }
                    else{
                    	 $( '#orgUnitTree' ).one( 'ouwtLoaded', function( event, ids, names ){
                            console.log('Finished loading orgunit tree');
                            //Disable ou selection until meta-data has downloaded
                            $("#orgUnitTree").addClass("disable-clicks");

                            $timeout(function () {
                                scope.treeLoaded = true;
                                scope.$apply();
                            });

                            downloadMetaData();
                        });
                    }
                });
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
            $(element).popover(options);
        },
        scope: {
            content: '=',
            title: '@details',
            template: "@template"
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

.directive('draggableModal', function () {

    return {
        restrict: 'EA',
        link: function (scope, element) {
            element.draggable();
        }
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

.directive('d2ContextMenu', function (ContextMenuSelectedItem) {

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
});
