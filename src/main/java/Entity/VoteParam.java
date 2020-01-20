package Entity;

/**
 * 请求投票 RPC 参数.
 */
public class VoteParam extends Param {
    //Candidate的Id(ip:selfPort)
    String candidateId;
    //Candidate的最后日志条目的索引值
    long lastLogIndex;
    //Candidate最后日志条目的任期号
    long lastLogTerm;

    public VoteParam() {
    }

    public VoteParam(String candidateId, long lastLogIndex, long lastLogTerm) {
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "VoteParam{" +
                "candidateId='" + candidateId + '\'' +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                ", term=" + term +
                ", serverId='" + serverId + '\'' +
                '}';
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    public void setLastLogTerm(long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }
}
