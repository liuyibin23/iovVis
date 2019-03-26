const common = require('./common-bar');
const charCfg = require('../../echarts/chartConfig');

let option = {
    title: {
        text: '车辆荷载自动监测数据车重',
        x: 'center',
        y: 'bottom'
    },
    // legend: {
    //     data: ['百分比'],
    //     //orient: 'vertical',
    //     x: 'center',
    //     y: '2%',
    //     borderWidth: 1
    // },
    toolbox: {
        show: false
    },
    xAxis: [
        {
            type: 'category',
            data: []
        }
    ],
    yAxis: [
        {
            type: 'value',
            axisLabel: {
                formatter: '{value} %'
            }
        }
    ],
    series: [
        {
            name: '百分比',
            type: 'bar',
            barWidth:35,
            //stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data: []
        }
    ]
};

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        plotCfg = charCfg.getCfgParams(params.chart_name, 'Bar');
        common.resetPreData(option, plotCfg.maxCnt);
        common.getData(plotCfg, option, params, token, res);
    }
}
module.exports = chart_area;
