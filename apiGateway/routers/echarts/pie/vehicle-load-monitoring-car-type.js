const common = require('./common-pie');
const charCfg = require('../../echarts/chartConfig');
const util  = require('../../../util/utils');
const axios = require('axios');
const node_echarts = require('node-echarts');

option = {
    title : {
           text: '车型分布',
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


function resetPreData(option){
    option.series[0].data = [];
}

function SendPngResponse(option, params, res){
    if (!option)
    {
        util.responData(404, '访问资源不存在。', res);
        return;  
    }

    let config = {
        width: params.chartWidth   ? params.chartWidth  * 100 : 500, // Image width, type is number.
        height: params.chartHeight ? params.chartHeight * 100 : 400, // Image height, type is number.
        option: option, // Echarts configuration, type is Object.
        //If the path  is not set, return the Buffer of image.
        // path:  '', // Path is filepath of the image which will be created.
        enableAutoDispose: true  //Enable auto-dispose echarts after the image is created.
    }
    let bytes = node_echarts(config);
    if (bytes) {
        let data = 'data:image/png;base64,' + bytes.toString('base64');
        util.responData(200, data, res);
    }
}

function getData(plotCfg, option, params, token, res){
    let limit = Math.ceil((params.endTime - params.startTime) / 1000);
    // plugins/telemetry/DEVICE/$DEVICE_ID/values/timeseries?keys=$KEYS&startTs=$START_TM&endTs=$END_TIME$FILTER
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${plotCfg.keys}&startTs=${params.startTime}&endTs=${params.endTime}&limit=${limit}`;
    api = encodeURI(api);

    axios.get(api, {
        headers: { "X-Authorization": token }
    }).then(resp => {
        let data = resp.data[plotCfg.keys];
        if (data){
            let datMap = new Map();
            for (let i = 0; i < data.length; i++) {
                let type = data[i].value;
                let cnt = datMap.get(type);
                if (!cnt) {
                    datMap.set(type, 1);
                }
                else {
                    datMap.set(type, cnt + 1);
                }
            }  

            let idx = 0;
            for (let i = 0; i < plotCfg.groupInfo.length; i++){
                let type   = plotCfg.groupInfo[i].type;
                let comment = plotCfg.groupInfo[i].comment;
                let dt = datMap.get(type);
                if (dt){
                    option.series[0].data[idx++] = { value:dt, name:comment};
                }
            }
        }     
        
        SendPngResponse(option, params, res);
    }).catch(err =>{
        console.log(err);
        SendPngResponse(option, params, res);
    });
}

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        plotCfg = charCfg.getCfgParams(params.chart_name, 'Pie');
        resetPreData(option);

        getData(plotCfg, option, params, token, res);
    }
}
module.exports = chart_area;
