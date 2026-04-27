package com.r2r.sync.connector;

import com.r2r.sync.dto.ApiResponseDTO;
import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.lib.common.Constants;
import com.r2r.sync.lib.utils.JsonUtils;

public class MockConnector implements Connector {
    @Override
    public ApiResponseDTO send(SyncEventDTO event) {
        String connectorId = event.getConnectorId();
        String scenario = Constants.SCENARIO_SUCCESS;
        
        if (event.getPayload() != null && event.getPayload().containsKey("scenario")) {
            scenario = event.getPayload().get("scenario").toString();
        }
        
        return JsonUtils.readMockResponse(connectorId, scenario);
    }
}
