const common = require('./common-bar');
const charCfg = require('../../echarts/chartConfig');

let option = {
    title: {
        text: '车辆荷载自动监测数据车重',
        x: 'center',
        y: 'bottom'
    },
    legend: {
        data: ['车重 > 44T', '24T < 车重 < 44T', '7.5T < 车重 < 24T', '2.5T < 车重 < 7.5T', '车重 < 2.5T'],
        //orient: 'vertical',
        x: 'center',
        y: '2%',
        borderWidth: 1
    },
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
                formatter: '{value} 辆'
            }
        }
    ],
    series: [
        {
            name: '车重 < 2.5T',
            type: 'bar',
            stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data: []
        },
        {
            name: '2.5T < 车重 < 7.5T',
            type: 'bar',
            stack: '总量',
            data: []
        },
        {
            name: '7.5T < 车重 < 24T',
            type: 'bar',
            stack: '总量',

            //itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data: []
        },
        {
            name: '24T < 车重 < 44T',
            type: 'bar',
            stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data: []
        },
        {
            name: '车重 > 44T',
            type: 'bar',
            stack: '总量',
            barCateGoryGap: 20,
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
