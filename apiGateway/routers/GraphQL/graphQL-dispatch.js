// 定期
var monitorSchema = require('./schema/periodic-monitor-report/monitor-item-and-measure-point');
var vehicleSchema = require('./schema/periodic-monitor-report/vehicle-load-data-stats');
var cableForceSchema = require('./schema/periodic-monitor-report/cable-force-analysis-stats');

// 自动
var autoMonitorSchema =  require('./schema/automatic-monitor-report/automatic-monitor-report');
var crackMeterInfoStatsSchema = require('./schema/automatic-monitor-report/crackmeter-info-stats');
var crackMonitorStatsSchema = require('./schema/automatic-monitor-report/crack-monitor-stats');
var deflectometerInfoSchema = require('./schema/automatic-monitor-report/deflectometer-info-stats');
var deflectometerMonitorSchema = require('./schema/automatic-monitor-report/deflectometer-monitor-stats');
var graphql = require('graphql');
const util = require('../../util/utils');

function dispatchGraphQL(req, res){
    let graphQL = req.query.graphQL;
    if (graphQL) {
        let query = 'query ' + graphQL;
        let schema = "";
        if (graphQL.search('monitorData') != -1){
            schema = monitorSchema;
        }
        else if (graphQL.search('cableForceAnalysisStatsData') != -1) {
            schema = cableForceSchema;
        }
        else if (graphQL.search('autoMonitorData') != -1){
            schema = autoMonitorSchema;
        }
        else if (graphQL.search('crackMeterInfoStatsData') != -1){
            schema = crackMeterInfoStatsSchema;
        }
        else if (graphQL.search('crackMonitoInfoStatsData') != -1){
            schema = crackMonitorStatsSchema;
        }
        else if (graphQL.search('deflectometerInfoStatsData') != -1){
            schema = deflectometerInfoSchema;
        }
        else if (graphQL.search('deflectometerMonitorInfoStatsData') != -1){
            schema = deflectometerMonitorSchema;
        }
        else {
            schema = vehicleSchema;
        }
        // execute GraphQL!
        graphql.graphql(schema, query, req)
        .then((result) => {
            util.responData(200, {data:result.data}, res);
        })
        .catch(err => { 
            util.responErrorMsg(err, res);
        });
    }
}

exports.dispatchGraphQL = dispatchGraphQL;