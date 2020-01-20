package Entity;

import raft.Peer;

import java.util.concurrent.Callable;

public class ReplicationFailModel {
    static String count = "_count";
    static String success = "_success";

    public String countKey;
    public String successKey;
    public Callable callable;
    public LogEntry logEntry;
    public Peer peer;
    public Long offerTime;
    public int type;

    public ReplicationFailModel(Callable callable, LogEntry logEntry, Peer peer, Long offerTime) {
        this.callable = callable;
        this.logEntry = logEntry;
        this.peer = peer;
        this.offerTime = offerTime;
        countKey = logEntry.getCommand().getKey() + count;
        successKey = logEntry.getCommand().getKey() + success;
    }
}
