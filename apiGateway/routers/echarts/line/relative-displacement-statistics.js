const charCfg = require('../../echarts/chartConfig');
const common = require('./common-line');

let option = {
    title : {
        text: '相对位移监测数据统计分析',
        x: 'center',
        y:'bottom'
    },
    legend: {
        data:['最大位移', '平均位移', '最小位移'],
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
                formatter: '{value} mm'
            }
        }
    ],
    series : [
        {
            name:'最大位移',
            type:'line',
            data:[]
        },
      	{
            name:'平均位移',
            type:'line',
            data:[]
        },
      	{
            name:'最小位移',
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