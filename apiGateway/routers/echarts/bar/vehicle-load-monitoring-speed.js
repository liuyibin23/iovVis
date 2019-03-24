const common = require('./common-bar');
const charCfg = require('../../echarts/chartConfig');

let option = {
    title : {
         text: '自动监测数据车速',
         x: 'center',
         y:'top'
     },
     legend: {
         data:['80以下', '超速'],
         //orient: 'vertical',
         x:'center',
         y:'bottom',
         backgroundColor: '#eee',
         borderColor: 'rgba(178,34,34,0.8)',
         borderWidth: 1
     },
     toolbox: {
         show : false
     },
     xAxis : [
         {
             type : 'category',
             data : []
         }
     ],
     yAxis : [
         {
                type : 'value',
           axisLabel : {
                 formatter: '{value} 辆'
             }
         }
     ],
     series : [
        {
            name:'80以下',
            type:'bar',
            stack: '总量',
            data:[]
        },
         {
             name:'超速',
             type:'bar',
             stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
             data:[]
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