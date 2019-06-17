/*
 * Copyright © 2016-2018 The BeiDouApp Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as echarts from 'echarts';

export default angular.module('beidouapp.directives.echarts', [])
    .directive('eChart', echartsAngularjs)
    .name;

/*@ngInject*/
function echartsAngularjs($window, $log, $timeout) {
    return {
        restrict: 'AE',
        template: '<div></div>',
        scope: {
            eoption: '=',
            forceRender: '=',
            height: '@',
            width: '@'
            //     theme: '@',
            // onInit: "&",
            //     onChange: "&"
        },
        //restrict: 'A',
        link: link
    };
    /*
    function link($scope, element, attrs) {
        //初始化图表
        $log.log("link start");
        var myChart = echarts.init(element[0]);
        //监控option数据变化
        $scope.$watch(attrs['ecData'], function() {
            var option = $scope.$eval(attrs.ecData);
            $log.log(option);
            if (angular.isObject(option)) {
                //绘制图表
                myChart.setOption(option);
            }
        }, true);
        $scope.getDom = function() {
            return {
                'height': element[0].offsetHeight,
                'width': element[0].offsetWidth
            };
        };
        //监控图表宽高变化，响应式
        // $scope.$watch($scope.getDom, function() {
        //     // resize echarts图表
        //     myChart.resize();
        // }, true);

        angular.element($window).bind('resize', function () {
            myChart.resize();
        });

        $log.log("link function end");
    }
*/


    function link(scope, elem, attrs) {
        // directive is called once for each chart
        var opts = {
            height: attrs.height || 'auto',
            width: attrs.width || 'auto'
        };
        // $log.log('height:%s,width:%s,attrs,window\n', opts.height, opts.width, attrs, $window,elem);
        // $log.log('parent.offsetParent()[0]',elem.offsetParent()[0]);
        
        var myChart = echarts.init(elem[0], 'macarons', opts);
        scope.chart = myChart;
        // listen to option changes
        if (attrs.eoption) {
            scope.$watch(attrs['eoption'], function () {
                var option = scope.$eval(attrs.eoption);
                if (angular.isObject(option)) {
                    // $log.log("AWEN---setOption---");
                    myChart.setOption(option);
                    myChart.resize();
                }
            }, true); // deep watch
        }

        scope.$evalAsync(function(){
            // $log.log('evalAsync, para',data);
            $log.log('evalAsync, elem[0]', elem[0].parentElement.offsetWidth);
            // $log.log('parent.offsetParent()[0]',elem.offsetParent()[0]);
        });
        $timeout(function(){
            $log.log('evalAsync, elem[0]', elem[0].parentElement.offsetWidth);
            scope.chart && scope.chart.resize();
        }, 20);
        // scope.$watch('forceRender', function () {
        //     $log.log("AWEN---forceRender---");
        //     myChart.resize();
        // }, true);

        // scope.$on('$viewContentLoaded', function(event) {
        //     $log.log("AWEN---content loaded---",event);
        //     myChart.resize();
        //   });

        // scope.$on('$locationChangeSuccess', function() {
        //     $log.log('Check, 3, 4!');
        //     myChart.resize();
        //   });
        
        angular.element($window).bind('resize', function () {
            $log.log('resize evalAsync, elem[0]', elem[0].parentElement.offsetWidth);
            // $log.log('elem.offsetParent()[0]',elem.offsetParent()[0]);
            $log.log('parent.offsetParent',elem.parent()[0].offsetWidth);

            scope.chart && scope.chart.resize();
        });

        // angular.element($window).triggerHandler('resize');

        // 销毁

        scope.$on('$destroy', function () {
            if (scope.chart) {
                scope.chart.dispose();
                scope.chart = null;
            }
        });
    }

    // link: function ($scope, element, attrs) {
    //     // 配置项
    //     var DEFAULT_THEME = "default";
    //     var opts = {
    //         height: attrs.height || 400,
    //         width: attrs.width || 'auto'
    //     };
    //     var container = element.children()[0] || element[0];
    //     // 监听.初始化
    //     $scope.$watch(function () {
    //         return $scope.option;
    //     }, function (option) {
    //         setOption(option);
    //     });
    //     init(container, $scope.theme, opts);
    //     // resize事件
    //     angular.element($window).bind('resize', function () {
    //         $scope.chart && $scope.chart.resize();
    //     });

    //     // 销毁
    //     $scope.$on('$destroy', function () {
    //         if ($scope.chart) {
    //             $scope.chart.dispose();
    //             $scope.chart = null;
    //         }
    //     });
    //     /**
    //      * echarts初始化
    //      * @param {Object} dom		实例容器
    //      * @param {Object} theme	主题
    //      * @param {Object} opts		附加参数
    //      */
    //     function init(dom, theme, opts) {
    //         $log.log("init--->$scope.chart", $scope.chart);
    //         $scope.chart = echarts.init(dom, theme || DEFAULT_THEME, opts);
    //         $log.log("init--->$scope.chart", $scope.chart, theme, opts);
    //         $scope.chart.setOption({
    //                 xAxis: {
    //                     type: 'category',
    //                     data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
    //                 },
    //                 yAxis: {
    //                     type: 'value'
    //                 },
    //                 series: [{
    //                     data: [820, 932, 901, 934, 1290, 1330, 1320],
    //                     type: 'line'
    //                 }]
    //         });
    //         // $scope.onInit() && $scope.onInit()($scope.chart);
    //     }
    //     /**
    //      * 设置图表实例的配置项以及数据
    //      * @param {Object} option	图表实例的配置项以及数据
    //      */
    //     function setOption (option) {
    //         if ($scope.chart && option) {
    //             $scope.chart.setOption(option);
    //             $scope.onChange() && $scope.onChange()(option);
    //         }
    //     }
    // }

}
/* eslint-disable angular/angularelement */