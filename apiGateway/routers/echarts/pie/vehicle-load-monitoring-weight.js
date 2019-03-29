const common = require('./common-pie');
const charCfg = require('../../echarts/chartConfig');

option = {
    title : {
           text: '重车车重分布',
           x: 'center',
           y:'top'
       },
      series : [
          {
              type:'pie',
              radius : ['60%', '80%'],
              data:[
              ]
          }
      ]
  }

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        plotCfg = charCfg.getCfgParams(params.chart_name, 'Pie');
        common.resetPreData(option);
        common.getData(plotCfg, option, params, token, res);
    }
}
module.exports = chart_area;
