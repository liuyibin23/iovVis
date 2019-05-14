const charCfg = require('../../echarts/chartConfig');
const common = require('./common-line');

/*
温度/湿度统计
单个传感器
样式：双轴图，湿度+温度
X轴刻度：天
左Y轴单位：摄氏度
右Y轴刻度：百分比数值
聚合方式：最高/最低/平均
多个传感器多张
*/
let option = {
    title : {
        text: '温度/湿度统计',
        x: 'center',
        y:'bottom'
    },
    tooltip : {
        trigger: 'axis'
    },
    color:["#FF0000", "#9400D3", "#0000FF", "#00BFFF", "#006400", "#90EE90"],
    legend: {
        data:['最高温度', '平均温度', '最低温度', '最高湿度', '平均湿度', '最低湿度'],
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
            axisLabel: {
                //interval:0,//横轴信息全部显示
                rotate:30,//
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
                formatter: '{value} ℃'
            }
        },
      {
         type : 'value',
            min:0,
            max:100,
            axisLabel : {
                formatter: '{value} %'
            }
      }
    ],
    series : [
        {
            name:'最高温度',
            type:'line',
            symbol:'none',
            data:[
                //80,20,40,10,30,40,70,80,3, 4, 50,60,80,90,20,10, 10,60,80,90,
            ]
        },
      	{
            name:'平均温度',
            type:'line',
            symbol:'none',
            data:[
                //10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40
            ]
        },
      	{
            name:'最低温度',
            type:'line',
            symbol:'none',
            data:[
                //1,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8
            ]
        },
       {
            name:'最高湿度',
            type:'line',
            yAxisIndex: 1,
            symbol:'none',
            data:[
                //20,50,20,40,60,80,90,20,10, 10, 20, 5, 30, 1,22,12,25,4,13,23
            ]
        },
       {
            name:'平均湿度',
            type:'line',
            yAxisIndex: 1,
            symbol:'none',
            data:[
                //10,12,11,6,18,19,10,12,11,6,18,19,10,12,11,6,18,19,10,12,11
            ]
        },
       {
            name:'最低湿度',
            type:'line',
            yAxisIndex: 1,
            symbol:'none',
            data:[
                //5,6,8,9,1,2,3,6,7,5,6,8,9,1,2,3,6,7,5,6,8,9,1,2,3,6,7,5,6,8,9,1,2,3
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
