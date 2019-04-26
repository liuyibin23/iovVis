package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.historyvideo.HistoryVideo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@RuleNode(
		type = ComponentType.ACTION,
		name = "process protocol",
		configClazz =  TbMsgAttributesProtocolNodeConfiguration.class,
		nodeDescription = "Process protocol press attributes channle",
		nodeDetails = "Process Protocol press attributes channle",
		uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
		configDirective = "tbNodeEmptyConfig",
		icon = "file_upload"
)
public class TbMsgAttributesProtocolNode implements TbNode {

	private TbMsgAttributesProtocolNodeConfiguration config;
	@Override
	public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
		this.config = TbNodeUtils.convert(configuration, TbMsgAttributesProtocolNodeConfiguration.class);
	}

	@Override
	public void onMsg(TbContext ctx, TbMsg msg) {
		if (!msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name())) {
			ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
			return;
		}


		processProtocol(ctx,msg);


	}

	@Override
	public void destroy() {
	}


	//protocolMsg 消息格式
//	{
//		"historyVideo": "{\"startTs \": 123456,\"endTs\": 223456,\"fileId\": \"53h829fde123\",\"fileUrl\": \"http: //127.0.0.1/123321/gjidfo.mkv\",\"status\": \"DONE\"}"
//	}

	private void processProtocol(TbContext ctx,TbMsg msg){
		String protocolMsg = msg.getData();
		Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(protocolMsg));
		ArrayList<AttributeKvEntry> attributesList = new ArrayList<>(attributes);
		ObjectMapper mapper = new ObjectMapper();
		attributesList.forEach(item->{
			if(item.getKey().equals("historyVideo")&&item.getStrValue().isPresent()){
				String value = item.getStrValue().get();
				try {
					HistoryVideo historyVideo = mapper.readValue(value,HistoryVideo.class);
					historyVideo.setTenantId(ctx.getTenantId());
					historyVideo.setDeviceId(new DeviceId(msg.getOriginator().getId()));
					Device device =ctx.getDeviceService().findDeviceById(ctx.getTenantId(),historyVideo.getDeviceId());
					historyVideo.setCustomerId(device.getCustomerId());
					ctx.getHistoryVideoService().createOrUpdate(historyVideo);
				} catch (IOException e) {
					log.error("TbMsgAttributesProtocolNode error:",e);
					ctx.tellFailure(msg, e);
				}
			}
		});
	}
}
