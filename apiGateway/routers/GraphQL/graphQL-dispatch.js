// 定期
var schema = require('./schema');
var graphql = require('graphql');
const util = require('../../util/utils');

function dispatchGraphQL(req, res){
    let graphQL = req.query.graphQL;
    if (graphQL) {
        // monitor
        // let pos = graphQL.indexOf('monitorData');
        
        // let end_pos =  graphQL.indexOf('},');
        // let monitorQuery = graphQL.substr(pos, end_pos - pos + 1);
        // console.log(monitorQuery);
        // let start_pos = monitorQuery.indexOf('{');
        // let data = monitorQuery.substr(start_pos, monitorQuery.length - start_pos);
        // let query =  'query {monitorData(assetId:"cd59be20-12fc-11e9-bae8-7562662cc4ee")' + data + '}';
        let query = 'query ' + graphQL;
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