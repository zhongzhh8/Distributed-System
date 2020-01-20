package config;

import raft.Peer;

public interface NodeChange {
    //添加节点.
    Result addPeer(Peer newPeer);
    //删除节点.
    Result removePeer(Peer oldPeer);
}
