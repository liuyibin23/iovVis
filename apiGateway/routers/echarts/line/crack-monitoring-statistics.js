const charCfg = require('../../echarts/chartConfig');
const common = require('./common-line');

let option = {
    title : {
        text: '裂缝监测数据统计分析',
        x: 'center',
        y:'bottom'
    },
    legend: {
        data:['最大裂缝', '平均裂缝', '最小裂缝'],
        //orient: 'vertical',
        x:'center',
        y:'3%',
        backgroundColor: '#eee',
        borderColor: 'rgba(178,34,34,0.8)',
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
            data : [
                //1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
            ]
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
            name:'最大裂缝',
            type:'line',
            data:[
                //80,20,40,10,30,40,70,80,3, 4, 50,60,80,90,20,10, 10,60,80,90,40,70,80,3, 4, 50,60,80,90,20,10, 
            ]
        },
      	{
            name:'平均裂缝',
            type:'line',
            data:[
                //10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40,1,20,30,40,10,20,30,40,10,20
            ]
        },
      	{
            name:'最小裂缝',
            type:'line',
            data:[
               // 1,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8,2,3,6,7,8,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8
            ]
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
