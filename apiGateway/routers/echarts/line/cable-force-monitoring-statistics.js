const charCfg = require('../../echarts/chartConfig');
const common = require('./common-line');

let option = {
    title : {
        text: '索力监测数据统计分析',
        x: 'center',
        y:'bottom'
    },
    legend: {
        data:['最大索力', '平均索力', '最小索力'],
        //orient: 'vertical',
        x:'center',
        y:'top',
        //backgroundColor: '#eee',
        //borderColor: 'rgba(178,34,34,0.8)',
        borderWidth: 1
    },
    toolbox: {
        show : false
    },
    calculable : false,
    xAxis : [
        {
            type : 'category',
            boundaryGap : false,
            data : []
        }
    ],
    yAxis : [
       {
            type : 'value',
            axisLabel : {
                formatter: '{value}'
            }
        }
    ],
    series : [
        {
            name:'最大索力',
            type:'line',
            data:[]
        },
      	{
            name:'平均索力',
            type:'line',
            data:[]
        },
      	{
            name:'最小索力',
            type:'line',
            data:[]
        }
    ]
};

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: function (params, token, res, calllback) {
        plotCfg = charCfg.getCfgParams(params.chart_name, 'LINE');
        common.resetPreData(option, plotCfg.maxCnt);
        common.getData(plotCfg, option, params, token, res);
    }
}
module.exports = chart_area;
