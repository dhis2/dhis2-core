(function () {
  'use strict';

  var module = angular.module('sticky', []);

  // Directive: sticky
  //
  module.directive('sticky', ['$window', function ($window) {
      return {
        restrict: 'A', // this directive can only be used as an attribute.
        scope: {
          disabled: '=disabledSticky'
        },
        link: function linkFn($scope, $elem, $attrs) {
          var mediaQuery, stickyClass, unstickyClass, bodyClass, elem, $body,
            doc, initialCSS, initialStyle, isSticking,
            stickyLine, stickyBottomLine, offset, anchor, confine, prevOffset, matchMedia, usePlaceholder, placeholder;

          $scope.initSticky = function () {
            isSticking = false;

            matchMedia = $window.matchMedia;

            // elements
            $body = angular.element(document.body);
            elem = $elem[0];
            doc = document.documentElement;

            // attributes
            mediaQuery = $attrs.mediaQuery || false;
            stickyClass = $attrs.stickyClass || '';
            unstickyClass = $attrs.unstickyClass || '';
            bodyClass = $attrs.bodyClass || '';

            usePlaceholder = $attrs.useplaceholder !== undefined;

            initialStyle = $elem.attr('style') || '';

            offset = typeof $attrs.offset === 'string' ?
              parseInt($attrs.offset.replace(/px;?/, '')) :
              0;

            anchor = typeof $attrs.anchor === 'string' ?
              $attrs.anchor.toLowerCase().trim()
              : 'top';
            // Define the confine attribute - will confine sticky to it's parent
            confine = typeof $attrs.confine === 'string' ?
              $attrs.confine.toLowerCase().trim() : 'false';

            confine = (confine === 'true');

            // initial style
            initialCSS = {
              zIndex: $elem.css('z-index'),
              top: $elem.css('top'),
              width: $elem.css('width'),
              position: $elem.css('position'),
              marginTop: $elem.css('margin-top'),
              cssLeft: $elem.css('left')
            };

            switch (anchor) {
              case 'top':
              case 'bottom':
                break;
              default:
                anchor = 'top';
                break;
            }

            // Watcher
            //
            prevOffset = $scope.getTopOffset(elem);

            // Listeners
            //
            angular.element($window).on('scroll', $scope.checkIfShouldStick);
            angular.element($window).on('resize', $scope.$apply.bind($scope, $scope.onResize));
            $scope.$on('$destroy', $scope.onDestroy);
          };

          $scope.getScrollTop = function () {
            if (typeof $window.pageYOffset !== 'undefined') {
              //most browsers except IE before #9
              return $window.pageYOffset;
            }
            else {
              var B = document.body; //IE 'quirks'
              var D = document.documentElement; //IE with doctype
              D = (D.clientHeight) ? D : B;
              return D.scrollTop;
            }
          };

          $scope.getTopOffset = function (element) {
            if (element.getBoundingClientRect) {
              // Using getBoundingClientRect is vastly faster, if it's available
              return element.getBoundingClientRect().top + document.documentElement.scrollTop;
            } else {
              var pixels = 0;

              if (element.offsetParent) {
                do {
                  pixels += element.offsetTop;
                  element = element.offsetParent;
                } while (element);
              }

              return pixels;
            }
          };

          $scope.getBottomOffset = function (element) {
            return element.offsetTop + element.clientHeight;
          };

          $scope.shouldStickWithLimit = function (shouldApplyWithLimit) {
            if (shouldApplyWithLimit === 'true') {
              var elementHeight = elem.offsetHeight;
              var windowHeight = $window.innerHeight;
              return (windowHeight - (elementHeight + parseInt(offset)) < 0);
            } else {
              return false;
            }
          };

          $scope.onResize = function () {
            initialCSS.offsetWidth = elem.offsetWidth;
            $scope.unStickElement();
            $scope.checkIfShouldStick();

            if (isSticking) {
              var parent = $window.getComputedStyle(elem.parentElement, null),
                initialOffsetWidth = elem.parentElement.offsetWidth -
                  parent.getPropertyValue('padding-right').replace('px', '') -
                  parent.getPropertyValue('padding-left').replace('px', '');

              $elem.css('width', initialOffsetWidth + 'px');
            }
          };

          $scope.onDestroy = function () {
            angular.element($window).off('scroll', $scope.checkIfShouldStick);
            angular.element($window).off('resize', $scope.onResize);

            if (bodyClass) {
              $body.removeClass(bodyClass);
            }

            if (placeholder) {
              placeholder.remove();
            }
          };

          // Methods
          //

          // Simple helper function to find closest value
          // from a set of numbers in an array
          $scope.getClosest = function (array, num) {
            var minDiff = 1000;
            var ans;
            for (var i in array) {
              var m = Math.abs(num - array[i]);
              if (m < minDiff) {
                minDiff = m;
                ans = array[i];
              }
            }
            return ans;
          };

          $scope.stickElement = function () {
            var rect, absoluteLeft;

            rect = $elem[0].getBoundingClientRect();
            absoluteLeft = rect.left;

            initialCSS.offsetWidth = elem.offsetWidth;

            isSticking = true;

            if (bodyClass) {
              $body.addClass(bodyClass);
            }

            if (unstickyClass) {
              if ($elem.hasClass(unstickyClass)) {
                $elem.removeClass(unstickyClass);
              }
            }

            if (stickyClass) {
              $elem.addClass(stickyClass);
            }

            //create placeholder to avoid jump
            if (usePlaceholder) {
              placeholder = angular.element('<div>');
              var elementsHeight = $elem[0].offsetHeight;
              placeholder.css('height', elementsHeight + 'px');
              $elem.after(placeholder);
            }

            $elem
              .css('z-index', '10')
              .css('width', elem.offsetWidth + 'px')
              .css('position', 'fixed')
              .css(anchor, offset + 'px')
              .css('left', absoluteLeft + 'px')
              .css('margin-top', 0);

            if (anchor === 'bottom') {
              $elem.css('margin-bottom', 0);
            }
          };

          $scope.checkIfShouldStick = function () {
            if ($scope.disabled === true){
              $scope.unStickElement();
              return false;
            }

            var scrollTop, shouldStick, scrollBottom, scrolledDistance;

            if (mediaQuery && !(matchMedia('(' + mediaQuery + ')').matches || matchMedia(mediaQuery).matches)) {
              // Make sure to unstick element if media query no longer matches
              if (isSticking) {
                $scope.unStickElement();
              }
              return;
            }

            if (anchor === 'top') {
              scrolledDistance = $window.pageYOffset || doc.scrollTop;
              scrollTop = scrolledDistance - (doc.clientTop || 0);
              if (confine === true) {
                shouldStick = scrollTop >= stickyLine && scrollTop <= stickyBottomLine;
              } else {
                shouldStick = scrollTop >= stickyLine;
              }

            } else {
              scrollBottom = $window.pageYOffset + $window.innerHeight;
              shouldStick = scrollBottom <= stickyLine;
            }

            // Switch the sticky mode if the element crosses the sticky line
            // $attrs.stickLimit - when it's equal to true it enables the user
            // to turn off the sticky function when the elem height is
            // bigger then the viewport
            if (shouldStick && !$scope.shouldStickWithLimit($attrs.stickLimit) && !isSticking) {
              $scope.stickElement();
            } else if (!shouldStick && isSticking) {
              // could probably do this better
              var from, compare, closest;
              compare = [stickyLine, stickyBottomLine];
              closest = $scope.getClosest(compare, scrollTop);
              // Check to see if we are closer to the top or bottom confines
              // and set from to let the unstick element know the origin
              if (closest === stickyLine) {
                from = 'top';
              } else if (closest === stickyBottomLine) {
                from = 'bottom';
              }

              $scope.unStickElement(from, scrollTop);
            }
          };

          // Passing in scrolltop and directional origin to help
          // with some math later
          $scope.unStickElement = function (fromDirection) {
            $elem.attr('style', initialStyle);
            isSticking = false;

            if (bodyClass) {
              $body.removeClass(bodyClass);
            }

            if (stickyClass) {
              $elem.removeClass(stickyClass);
            }

            if (unstickyClass) {
              $elem.addClass(unstickyClass);
            }

            if (fromDirection === 'top') {
              $elem
                .css('z-index', initialCSS.zIndex)
                .css('width', '')
                .css('top', initialCSS.top)
                .css('position', initialCSS.position)
                .css('left', initialCSS.cssLeft)
                .css('margin-top', initialCSS.marginTop);

            } else if (fromDirection === 'bottom' && confine === true) {
              // make sure we are checking to see if the element is confined to the parent
              $elem
                .css('z-index', initialCSS.zIndex)
                .css('width', '')
                .css('top', '')
                .css('bottom', 0)
                .css('position', 'absolute')
                .css('left', initialCSS.cssLeft)
                .css('margin-top', initialCSS.marginTop)
                .css('margin-bottom', initialCSS.marginBottom);
            }

            if (placeholder) {
              placeholder.remove();
            }
          };

          $scope.$watch(function () {
            // triggered on load and on digest cycle
            if ($scope.disabled === true){
              $scope.unStickElement();
              return;
            }

            if (isSticking) {
              return prevOffset + $scope.getScrollTop();
            }

            prevOffset =
              (anchor === 'top') ?
                $scope.getTopOffset(elem) :
                $scope.getBottomOffset(elem);

            return prevOffset + $scope.getScrollTop();

          }, function (newVal, oldVal) {
            if (( newVal !== oldVal || typeof stickyLine === 'undefined' ) && newVal !== 0 && !isSticking) {
              stickyLine = newVal - offset;

              // IF the sticky is confined, we want to make sure the parent is relatively positioned,
              // otherwise it won't bottom out properly

              if (confine) {
                $elem.parent().css({
                  'position': 'relative'
                });
              }

              // Get Parent height, so we know when to bottom out for confined stickies
              var parent = $elem.parent()[0];
              var parentHeight = parseInt(parent.offsetHeight);

              // and now lets ensure we adhere to the bottom margins
              // TODO: make this an attribute? Maybe like ignore-margin?
              var marginBottom = parseInt($elem.css('margin-bottom').replace(/px;?/, '')) || 0;

              // specify the bottom out line for the sticky to unstick
              stickyBottomLine = parentHeight - (elem.offsetTop + elem.offsetHeight) + offset + marginBottom;

              $scope.checkIfShouldStick();
            }
          });

          // Init the directive
          $scope.initSticky();
        }
      };
    }]
  );

  // Shiv: matchMedia
  //
  window.matchMedia = window.matchMedia || (function () {
      var warning = 'angular-sticky: This browser does not support ' +
        'matchMedia, therefore the minWidth option will not work on ' +
        'this browser. Polyfill matchMedia to fix this issue.';

      if (window.console && console.warn) {
        console.warn(warning);
      }

      return function () {
        return {
          matches: true
        };
      };
    }());
}());