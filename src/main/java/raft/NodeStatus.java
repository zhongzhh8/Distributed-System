package raft;

public interface NodeStatus {
    final int FOLLOWER = 0;
    final int CANDIDATE = 1;
    final int LEADER = 2;

    public int status = FOLLOWER;

}
