package raft;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PeerSet implements Serializable {

    private List<Peer> list = new ArrayList<>();

    private volatile Peer leader;

    private volatile Peer self;

    public PeerSet() {
    }

    public List<Peer> getPeers() {
        return list;
    }

    public List<Peer> getPeersWithOutSelf() {
        List<Peer> list2 = new ArrayList<>(list);
        list2.remove(self);
        return list2;
    }

    public void addPeer(Peer peer) {
        list.add(peer);
    }

    public void removePeer(Peer peer) {
        list.remove(peer);
    }

    public Peer getLeader() {
        return leader;
    }

    public void setLeader(Peer leader) {
        this.leader = leader;
    }

    public Peer getSelf() {
        return self;
    }

    public void setSelf(Peer self) {
        this.self = self;
    }

    @Override
    public String toString() {
        return "PeerSet{" +
                "list=" + list +
                ", leader=" + leader +
                ", self=" + self +
                '}';
    }
}
