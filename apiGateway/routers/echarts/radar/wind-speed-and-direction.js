const common = require('./common-radar');
const charCfg = require('../../echarts/chartConfig');

let option = {
    title : {
        text: '风速风向图',
      	x:'center',
     	 y:'top'
    },
  legend: {
        data:['风向频率', '最大风速'],
        //orient: 'vertical',
        x:'center',
        y:'bottom',
        //backgroundColor: '#eee',
        //borderColor: 'rgba(178,34,34,0.8)',
        borderWidth: 1
    },
    calculable : false,
    polar : [
        {
             indicator : [
                { text : 'NORTH' },
                { text : 'NNW' },
                { text : 'NW' },
                { text : 'WNW' },

                { text : 'WEST' },
                { text : 'WSW' },
                { text : 'SW' },
                { text : 'SSW' },

                { text : 'SOUTH' },
                { text : 'SSE' },
                { text : 'SE' },
                { text : 'ESE' },

                { text : 'EAST' },
                { text : 'ENE' },
                { text : 'NE' },
                { text : 'NNE' }
            ],
            center : ['50%', '50%'],
            radius : 150,
          	startAngle: 0,
            splitNumber: 5,
          	scale: true,
            type: 'circle',
            axisLine: {            // 坐标轴线
                show: true,        // 默认显示，属性show控制显示与否
                lineStyle: {       // 属性lineStyle控制线条样式
                    //color: 'red',
                    width: 1,
                    type: 'solid'
                }
            }
        }
    ],
    series : [
        {
            type: 'radar',
            polarIndex : 0,
            itemStyle: {normal: {areaStyle: {type: 'default'}}},
            data : [
                {
                    name : '风向频率',
                    value : [
                    ],
                },
                {
                    name:'最大风速',
                    value:[
                    ]
                }
            ]
        }
    ]
};
                    

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        plotCfg = charCfg.getCfgParams(params.chart_name, 'Radar');
        common.resetPreData(option);
        common.getData(plotCfg, option, params, token, res);
    }
}
module.exports = chart_area;
