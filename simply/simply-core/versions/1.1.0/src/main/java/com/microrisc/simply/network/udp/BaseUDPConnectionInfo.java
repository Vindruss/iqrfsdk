
package com.microrisc.simply.network.udp;

import com.microrisc.simply.network.AbstractNetworkConnectionInfo;
import java.net.InetAddress;

/**
 * Base class of implementation of UDP connection information.
 * 
 * @author Michal Konopa
 */
public class BaseUDPConnectionInfo 
extends AbstractNetworkConnectionInfo implements UDPConnectionInfo {
    /** Target IP address. */
    protected InetAddress address = null;
    
    /** Target port. */
    protected int port = -1;
    
    
    /**
     * Creates new {@code SimpleUDPConnectionInfo} object set to specified parameters.
     * @param address target address
     * @param port target port
     */
    public BaseUDPConnectionInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }
    
    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }
    
    @Override
    public String toString() {
        return ("{ " +
                "address=" + address + 
                ", port=" + port + 
                " }");
    }
    
    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof UDPConnectionInfo) ) {
            return false;
        }
        
        UDPConnectionInfo udpConnectionInfo = (UDPConnectionInfo) obj;
        return (this.address.equals(udpConnectionInfo.getAddress()) &&
                (this.port == udpConnectionInfo.getPort()));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.address != null ? this.address.hashCode() : 0);
        hash = 19 * hash + this.port;
        return hash;
    }
}
