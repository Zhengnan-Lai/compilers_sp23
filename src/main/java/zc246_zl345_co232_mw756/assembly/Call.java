package zc246_zl345_co232_mw756.assembly;

public class Call extends Assembly {
    private final String label;
    public final int n_args;
    public final long n_rets;

    public Call(String label, int n_args, long n_rets) {
        this.label = label;
        this.n_args = n_args;
        this.n_rets = n_rets;
    }

    @Override
    public String toString() {
        return String.format("call %s", label);
    }
}
