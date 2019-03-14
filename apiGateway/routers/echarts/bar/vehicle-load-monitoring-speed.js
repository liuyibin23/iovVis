const common = require('./common-bar');
const charCfg = require('../../echarts/chartConfig');

let option = {
    title : {
         text: '自动监测数据车速',
         x: 'center',
         y:'bottom'
     },
     legend: {
         data:['车速 < 80Km/h', '80Km/h < 车速 < 120Km/h', '车速 > 120Km/h'],
         //orient: 'vertical',
         x:'center',
         y:'2%',
         backgroundColor: '#eee',
         borderColor: 'rgba(178,34,34,0.8)',
         borderWidth: 2
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
            name:'车速 < 80Km/h',
            type:'bar',
            stack: '总量',
            data:[]
        },
         {
             name:'80Km/h < 车速 < 120Km/h',
             type:'bar',
             stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
             data:[]
         },
         {
             name:'车速 > 120Km/h',
             type:'bar',
               barWidth:25,
             stack: '总量',
          
             //itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
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