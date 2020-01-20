package Entity;

import java.io.Serializable;
/**
 * 附加 RPC 日志返回值.
 */
public class EntryResponse implements Serializable {
    //当前的任期号，用于领导人去更新自己
    long term;
    //follower包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真
    boolean success;

    public EntryResponse() {
    }

    public EntryResponse(long term, boolean success) {
        this.term = term;
        this.success = success;
    }

    public static EntryResponse fail() {
        return new EntryResponse(false);
    }

    public static EntryResponse ok() {
        return new EntryResponse(true);
    }


    public EntryResponse(boolean success) {
        this.success = success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public EntryResponse(long term) {
        this.term = term;
    }

    public boolean isSuccess() {
        return success;
    }
}
