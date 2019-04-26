package org.thingsboard.rule.engine.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbMsgAttributesProtocolNodeConfiguration implements NodeConfiguration<TbMsgAttributesProtocolNodeConfiguration> {

	private  String protocolType;
	@Override
	public TbMsgAttributesProtocolNodeConfiguration defaultConfiguration() {
		TbMsgAttributesProtocolNodeConfiguration configuration = new TbMsgAttributesProtocolNodeConfiguration();
		configuration.setProtocolType("v1.0");
		return configuration;
	}
}
