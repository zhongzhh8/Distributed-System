package config;

import Entity.LogEntry;
import Entity.Request;
import Entity.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.NodeImpl;
import raft.NodeStatus;
import raft.Peer;

public class NodeChangeImpl implements NodeChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeChangeImpl.class);

    private final NodeImpl node;

    public NodeChangeImpl(NodeImpl node) {
        this.node = node;
    }

    @Override
    public synchronized Result addPeer(Peer newPeer) {
        // 已经存在
        if (node.peerSet.getPeersWithOutSelf().contains(newPeer)) {
            return new Result(Result.FAIL);
        }
        node.peerSet.getPeersWithOutSelf().add(newPeer);
        if (node.status == NodeStatus.LEADER) {
            node.getNextIndexs().put(newPeer, 0L);
            node.getMatchIndexs().put(newPeer, 0L);

            for (long i = 0; i < node.getLogModule().getLastIndex(); i++) {
                LogEntry e = node.getLogModule().read(i);
                if (e != null) {
                    node.replication(newPeer, e);
                }
            }
            for (Peer item : node.peerSet.getPeersWithOutSelf()) {
                //同步到其他节点.
                Request request = new Request();
                request.setType(Request.CHANGE_CONFIG_ADD);
                request.setUrl(newPeer.getAddr());
                request.setObject(newPeer);

                Response response = node.RPC_CLIENT.send(request);
                Result result = (Result) response.getResponse();
                if (result != null && result.getStatus() == Result.SUCCESS) {
                    LOGGER.info("replication config success, peer : {}, newServer : {}", newPeer, newPeer);
                } else {
                    LOGGER.warn("replication config fail, peer : {}, newServer : {}", newPeer, newPeer);
                }
            }

        }
        return new Result(Result.SUCCESS);
    }

    @Override
    public synchronized Result removePeer(Peer oldPeer) {
        node.peerSet.getPeersWithOutSelf().remove(oldPeer);
        node.getNextIndexs().remove(oldPeer);
        node.getMatchIndexs().remove(oldPeer);
        return new Result();
    }
}
