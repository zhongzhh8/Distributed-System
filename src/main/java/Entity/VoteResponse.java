package Entity;

import java.io.Serializable;

/**
 * 请求投票RPC返回值.
 */
public class VoteResponse implements Serializable {
    //当前任期号，以便于Candidate去更新自己的任期
    long term;
    //Candidate赢得了此张选票时为真
    boolean voteGranted;

    public VoteResponse() {
    }

    public VoteResponse(long term) {
        this.term = term;
    }

    public VoteResponse(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    public VoteResponse(long term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public static VoteResponse fail() {
        return new VoteResponse(false);
    }

    public static VoteResponse ok() {
        return new VoteResponse(true);
    }


    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    public void setVoteGranted(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }
}
