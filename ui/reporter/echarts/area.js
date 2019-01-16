const axios = require('axios');

async function getSync(url, tok) {
    try {
        let res = await axios.get(url, { headers: { "X-Authorization": tok } });
        let res_data = res.data;
        return new Promise((resolve, reject) => {
            console.log(res_data);
            if (res.status === 200) {
                resolve(res_data);
            } else {
                reject(res);
            }
        })
    } catch (err) {
        console.log("error:", err);
    }
}

var chart_data = {
	name: 'chart_data',
	version: '1.0.0',

    fillData: function(devid, tsw, token) {
        console.log(devid,tsw, token);
        
        let  option = {
            title: {
                text: '堆叠区域图'
            },
            tooltip : {
                trigger: 'axis'
            },
            legend: {
                data:['搜索引擎']
            },
            toolbox: {
                feature: {
                    saveAsImage: {}
                }
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                containLabel: true
            },
            xAxis : [
                {
                    type : 'category',
                    boundaryGap : false,
                    data : ['周一','周二','周三','周四','周五','周六','周日']
                }
            ],
            yAxis : [
                {
                    type : 'value'
                }
            ],
            series : [
                {
                    name:'搜索引擎',
                    type:'line',
                    stack: '总量',
                    label: {
                        normal: {
                            show: true,
                            position: 'top'
                        }
                    },
                    areaStyle: {normal: {}},
                    data:[820, 932, 901, 934, 1290, 1330, 1320]
                }
            ]
        };
        // let apiUrl = `http://cf.beidouapp.com:8080/api/plugins/telemetry/DEVICE/${devid}/values/timeseries?limit=100&agg=NONE&keys=crackWidth&startTs=1547547560000&endTs=1547547670000`;
        // let res_data = await getSync(apiUrl, token);
        // console.log(res_data);
        return option;
    }
}
module.exports = chart_data;
