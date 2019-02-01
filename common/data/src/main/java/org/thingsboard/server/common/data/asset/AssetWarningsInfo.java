package org.thingsboard.server.common.data.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.AssetId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetWarningsInfo {
	private AssetId assetId;
	private String assetName;
	private String tenantName;
	private String customerName;
	@JsonProperty("additional_info")
	private JsonNode additionalInfo;
}
