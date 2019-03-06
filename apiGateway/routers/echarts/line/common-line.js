const axios = require('axios');
const node_echarts = require('node-echarts');
const util  = require('../../../util/utils');

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

function processData(option, params, allData, maxCnt, res){
    if (allData[0]){
        for (let i = 0; i < allData[0].length; i++) {                    
            for (let idx = 0; idx < maxCnt; idx++) {
                if (allData[idx] && allData[idx][i]) {
                    val = Number.parseFloat(allData[idx][i].value);
                    option.series[idx].data.push(val);
                }
            }

            option.xAxis[0].data.push(i);
        }
    }

    SendPngResponse(option, params, res);
}

function getData(plotCfg, option, params, token, res){    
    if (plotCfg)
    {
        let diff = params.endTime - params.startTime;
        let interval = plotCfg.interval;
        if (diff / interval > 700) {
            console.log("time too long > 700.  %f", diff / interval);
            processData(option, params, plotCfg.maxCnt, res);
            return;
        }

        let keyValue = plotCfg.keys;
        let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
        + `&startTs=${params.startTime}&endTs=${params.endTime}&interval=${interval}&agg=AVG`;
        api = encodeURI(api);
        //console.log(api);

        axios.get(api, {
            headers: { "X-Authorization": token }
        }).then(response => {
            //console.log('idx:' + idx + ' return:' + retCnt);
            let keys = plotCfg.keys.split(',');
            let allData = [];
            for (let i = 0; i < plotCfg.maxCnt; i++){
                allData[i] = response.data[keys[i]];
            }

            processData(option, params, allData, plotCfg.maxCnt, res);
        }).catch(err => {
            console.log("err" + err);
            processData(option, res, params, plotCfg.maxCnt, res);
        });   
    }
}

function resetPreData(option, maxCnt){
    option.xAxis[0].data = [];
    for (let idx = 0; idx < maxCnt; idx++) {
        option.series[idx].data = [];
    }
}

exports.resetPreData = resetPreData;
exports.getData = getData;