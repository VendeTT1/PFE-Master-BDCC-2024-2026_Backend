package ma.expertsci.subscriptions.entities;

public enum PlanType {
    TRIAL(5, false, 14),
    PREMIUM(25, true, 30),
    ENTERPRISE(50, true, 30);

    private final int includedUsers;
    private final boolean paid;
    private final Integer durationInDays;

    PlanType(int includedUsers, boolean paid, Integer durationInDays) {
        this.includedUsers = includedUsers;
        this.paid = paid;
        this.durationInDays = durationInDays;
    }

    public int getIncludedUsers() {
        return includedUsers;
    }

    public boolean isPaid() {
        return paid;
    }

    public Integer getDurationInDays() {
        return durationInDays;
    }
}
