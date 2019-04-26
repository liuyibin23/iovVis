package org.thingsboard.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;

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


		processProtocol(msg.getData());


	}

	@Override
	public void destroy() {
	}

	private boolean processProtocol(String protocolMsg){

		return true;
	}
}
