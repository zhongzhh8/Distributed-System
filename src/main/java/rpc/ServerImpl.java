package rpc;

import Entity.EntryParam;
import Entity.Request;
import Entity.Response;
import Entity.VoteParam;
import client.KVRequest;
import client.KVResponse;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import config.NodeChange;
import raft.NodeImpl;
import raft.Peer;

public class ServerImpl implements Server{
    private NodeImpl node;
    private RpcServer rpcServer;
    private volatile boolean flag;

    public ServerImpl(int port, NodeImpl node) {

        if (flag) {
            return;
        }
        synchronized (this) {
            if (flag) {
                return;
            }
            rpcServer = new RpcServer(port, false, false);
            rpcServer.registerUserProcessor(new UserProcessor<Request>() {
                @Override
                public Object handleRequest(BizContext bizContext, Request request) throws Exception {
                    return handleRequestImpl(request);
                }
            });

            this.node = node;
            flag = true;
        }

    }

    @Override
    public void start() {
        rpcServer.start();
    }

    @Override
    public void stop() {
        rpcServer.stop();
    }

    @Override
    public Response handleRequestImpl(Request request) {
        if (request.getType() == Request.VOTE) {
            return new Response(node.handleRequestVote((VoteParam) request.getObject()));
        } else if (request.getType() == Request.ENTRIES) {
            return new Response(node.handleAppendEntries((EntryParam) request.getObject()));
        } else if (request.getType() == Request.CLIENT_REQ) {
            KVResponse kvResponse = node.handleClientRequest((KVRequest)request.getObject());
            Response response = new Response(kvResponse);
            response.setValue(kvResponse.getValue());
            return response;
        } else if (request.getType() == Request.CHANGE_CONFIG_REMOVE) {
            return new Response(((NodeChange) node).removePeer((Peer) request.getObject()));
        } else if (request.getType() == Request.CHANGE_CONFIG_ADD) {
            return new Response(((NodeChange) node).addPeer((Peer) request.getObject()));
        }
        return null;

    }
}
