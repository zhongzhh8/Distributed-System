package raft;

import java.util.Objects;

public class Peer {
    /** ip:selfPort */
    private String addr;

    public Peer(String addr) {
        this.addr = addr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Peer peer = (Peer) o;
        return Objects.equals(addr, peer.addr);
    }

    @Override
    public int hashCode() {

        return Objects.hash(addr);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "addr='" + addr + '\'' +
                '}';
    }

    public String getAddr() {
        return addr;
    }
}
