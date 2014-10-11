
package com.microrisc.simply.iqrf.dpa.init;

/**
 * Configuration of DPA initializer.
 * 
 * @author Michal Konopa
 */
public class DPA_InitializerConfiguration {
    /** Configuration of enumeration process. */
    private final EnumerationConfiguration enumConfig;
    
    /** Configuration of processing of bonded nodes. */
    private final BondedNodesConfiguration bondedNodesConfig;
    
    /** Configuration of discovery process. */
    private final DiscoveryConfiguration discoConfig;

    
    public static class Builder {
        private EnumerationConfiguration enumConfig;
        private BondedNodesConfiguration bondedNodesConfig;
        private DiscoveryConfiguration discoConfig;
        
        
        public Builder enumerationConfiguration(EnumerationConfiguration enumConfig) {
            this.enumConfig = enumConfig;
            return this;
        }
        
        public Builder bondedNodesConfiguration(BondedNodesConfiguration bondedNodesConfig) {
            this.bondedNodesConfig = bondedNodesConfig;
            return this;
        }
        
        public Builder discoveryConfiguration(DiscoveryConfiguration discoConfig) {
            this.discoConfig = discoConfig;
            return this;
        }
        
        public DPA_InitializerConfiguration build() {
            return new DPA_InitializerConfiguration(this);
        }
    }
    
    /**
     * Creates configuration of DPA initializer.
     */
    private DPA_InitializerConfiguration(Builder builder) {
        this.enumConfig = builder.enumConfig;
        this.bondedNodesConfig = builder.bondedNodesConfig;
        this.discoConfig = builder.discoConfig;
    }
    
    
    /**
     * @return enumeration configuration
     */
    public EnumerationConfiguration getEnumerationConfiguration() {
        return enumConfig;
    }

    /**
     * @return processing of bonded nodes configuration
     */
    public BondedNodesConfiguration getBondedNodesConfiguration() {
        return bondedNodesConfig;
    }
    
    /**
     * @return discovery configuration
     */
    public DiscoveryConfiguration getDiscoveryConfiguration() {
        return discoConfig;
    }
}
