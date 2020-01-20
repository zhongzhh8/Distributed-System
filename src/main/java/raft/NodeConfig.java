package raft;

import java.util.List;

public class NodeConfig {
    //自身 selfPort
    public int selfPort;
    //所有节点地址.
    public List<String> peerAddrs;

    public int getSelfPort() {
        return selfPort;
    }

    public void setSelfPort(int selfPort) {
        this.selfPort = selfPort;
    }

    public List<String> getPeerAddrs() {
        return peerAddrs;
    }

    public void setPeerAddrs(List<String> peerAddrs) {
        this.peerAddrs = peerAddrs;
    }
}
