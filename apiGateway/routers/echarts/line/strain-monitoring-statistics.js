const charCfg = require('../chartConfig');
const common = require('./common-line');

let option = {
    title : {
        text: '应变监测数据统计分析',
        x: 'center',
        y:'bottom'
    },
    legend: {
        data:['最大应变', '平均应变', '最小应变'],
        //orient: 'vertical',
        x:'center',
        y:'top',
        //ackgroundColor: '#eee',
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
            axisLabel: {
                rotate:30,
            },
            data : []
        }
    ],
    yAxis : [
       {
            type : 'value',
         	//min:-72,
        	//max:60,
            axisLabel : {
                formatter: '{value} με'
            }
        }
    ],
    series : [
        {
            name:'最大应变',
            type:'line',
            data:[]
        },
      	{
            name:'平均应变',
            type:'line',
            data:[]
        },
      	{
            name:'最小应变',
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