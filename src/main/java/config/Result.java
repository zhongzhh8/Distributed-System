package config;

public class Result {
    public static final int FAIL = 0;
    public static final int SUCCESS = 1;

    private int status;

    private String leaderHint;

    public Result() {
    }

    public Result(int status, String leaderHint) {
        this.status = status;
        this.leaderHint = leaderHint;
    }

    public Result(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Result{" +
                "status=" + status +
                ", leaderHint='" + leaderHint + '\'' +
                '}';
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getLeaderHint() {
        return leaderHint;
    }

    public void setLeaderHint(String leaderHint) {
        this.leaderHint = leaderHint;
    }
}
