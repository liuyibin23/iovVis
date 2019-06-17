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
/* eslint-disable */
import './integrated.scss';

/*@ngInject*/
export default function IntegratedController(
    $scope, $http, $interval, dateFilter, $filter, $mdMedia, $q, $document, menu, $state, $timeout,
    $log, tbRaf, types, utils, timeService,
    datasourceService, alarmService, dashboardService, deviceService
) {
    var vm = this;
    /*
        $scope.lineOption = {
            xAxis: {
                type: "category",
                data: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
            },
            yAxis: {
                type: "value"
            },
            series: [
                {
                    data: [820, 932, 901, 934, 1290, 1330, 1320],
                    type: "line"
                }
            ]
        };
    
        //创建定时器，每3秒刷新一次
        var timer;
        if (!angular.isDefined(timer)) {
            timer = $interval(function () {
                $log.log('refresh data per 3 seconds')
                var array = [];
                for (var i = 0; i < 7; i++) {
                    array.push(randomNum(200, 900));
                }
                $scope.lineOption.series[0].data = array;
            }, 3000);
        } else {
            $interval.cancel(timer);
        }
        $scope.$on("$destory", function () {
            $log.log('kill timer')
            $interval.cancel(timer);
        });
        function randomNum(minNum, maxNum) {
            return Math.random() * (maxNum - minNum + 1);
        }
    
    */
    $http.get('/static/aqi-beijing.json').then(function (response) {
        let data = response.data;
        $scope.eoption = {
            title: {
                text: '指数'
            },
            tooltip: {
                trigger: 'axis'
            },
            xAxis: {
                data: data.map(function (item) {
                    return item[0];
                })
            },
            yAxis: {
                splitLine: {
                    show: false
                }
            },
            toolbox: {
                orient: 'vertical',
                left: 'right',
                feature: {
                    dataZoom: {
                        yAxisIndex: 'none'
                    },
                    restore: {},
                    saveAsImage: {}
                }
            },
            dataZoom: [{
                startValue: '2014-06-01'
            }, {
                type: 'inside'
            }],
            visualMap: {
                top: 0,
                right: 30,
                orient: 'horizontal',
                pieces: [{
                    gt: 0,
                    lte: 100,
                    color: '#096'
                }, {
                    gt: 100,
                    lte: 300,
                    color: '#ffde33'
                }, {
                    gt: 300,
                    color: '#7e0023'
                }],
                outOfRange: {
                    color: '#999'
                }
            },
            series: [{
                name: '指标A#1',
                type: 'line',
                data: data.map(function (item) {
                    return item[1];
                }),
                markLine: {
                    silent: true,
                    data: [{
                        yAxis: 50
                    }, {
                        yAxis: 100
                    }, {
                        yAxis: 150
                    }, {
                        yAxis: 200
                    }, {
                        yAxis: 300
                    }]
                }
            }, {
                name: '指标B#2',
                type: 'line',
                data: data.map(function (item) {
                    return item[1] + 40;
                }),
                itemStyle: {
                    normal: {
                        lineStyle: {
                            width: 3,
                            color: '#FF0000'
                        }
                    }
                }

            }]
        };
    });
}



