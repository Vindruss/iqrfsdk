
package com.microrisc.simply.connector;

import com.microrisc.simply.ProtocolLayerService;
import com.microrisc.simply.connector.response_waiting.SimpleResponseWaitingConnectorFactory;
import org.apache.commons.configuration.Configuration;

/**
 * Simple connector factory.
 * 
 * @author Michal Konopa
 */
public final class SimpleConnectorFactory 
extends AbstractConnectorFactory<ProtocolLayerService, Configuration, Connector> {
    /**
     * Types of connectors.
     */
    private static enum ConnectorType {
        RESPONSE_WAITING
    }
    
    /**
     * Mapping of configuration strings to connector types.
     */
    private static enum ConnectorConfigMapping {
        RESPONSE_WAITING    ("responseWaiting", ConnectorType.RESPONSE_WAITING);
        
        private String configString;
        private ConnectorType connectorType;
        
        private ConnectorConfigMapping(String configString, ConnectorType connectorType) {
            this.configString = configString;
            this.connectorType = connectorType;
        }

        /**
         * @return the configuration string
         */
        public String getConfigString() {
            return configString;
        }

        /**
         * @return the connector type
         */
        public ConnectorType getConnectorType() {
            return connectorType;
        }
    }
    
     
    /**
     * Returns type of connector.
     * @param configuration source configuration to discover
     * @return
     * @throws Exception 
     */
    private ConnectorType getConnectorType(Configuration configuration) throws Exception {
        String connTypeStr = configuration.getString("connector.type", "");
        if (connTypeStr.equals("")) {
            throw new Exception("Connector type not specified");
        }
        
        for (ConnectorConfigMapping configMapping : ConnectorConfigMapping.values()) {
            if (configMapping.getConfigString().equals(connTypeStr)) {
                return configMapping.getConnectorType();
            }
        }
        throw new Exception("Unrecognized connector type");
    }
    
    
    @Override
    public Connector getConnector(ProtocolLayerService protocolLayerService, 
            Configuration configuration
    ) throws Exception {
        ConnectorType connectorType = getConnectorType(configuration);
        switch (connectorType) {
            case RESPONSE_WAITING:
                return new SimpleResponseWaitingConnectorFactory().getConnector(
                        protocolLayerService, configuration
                );
        }
        throw new Exception("Unsupported connector type: " + connectorType);
    }
    
}
