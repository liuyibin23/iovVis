const axios = require('axios');
const node_echarts = require('node-echarts');
const util  = require('../../../util/utils');
const charCfg = require('../../echarts/chartConfig');

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
    //let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${plotCfg.keys}&startTs=${params.startTime}&endTs=${params.endTime}&limit=${limit}`;
    let interval = Number.parseFloat(params.interval) * 1000;
    let keyValue = plotCfg.keys;
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
        + `&startTs=${params.startTime}&endTs=${params.endTime}&interval=${interval}&agg=AVG`;
    api = encodeURI(api);

    axios.get(api, {
        headers: { "X-Authorization": token }
    }).then(resp => {
        let keys = plotCfg.keys.split(',');
        for (let i = 0; i < keys.length; i++){
            let data = resp.data[keys[i]];
            if (data){
                let len = 16;
                for (let j = 0; j < len; j++){
                    if (data[j]) {
                        option.series[0].data[i].value[j] = Number.parseFloat(data[j].value);  
                    } else {
                        option.series[0].data[i].value[j] = 0;
                    }
                }                         
            }
        }   
        
        SendPngResponse(option, params, res);
    }).catch(err =>{
        console.log(err);
        SendPngResponse(option, params, res);
    });
}

function resetPreData(option){
    let len = option.series[0].data.length;
    option.series[0].data[0].value = [];
    option.series[0].data[1].value = [];
}

exports.resetPreData = resetPreData;
exports.getData = getData;