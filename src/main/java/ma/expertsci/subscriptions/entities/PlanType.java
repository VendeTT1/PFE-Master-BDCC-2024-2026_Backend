package ma.expertsci.subscriptions.entities;

/**
 * Defines the available subscription tiers.
 *
 * priceXOF: amount charged in XOF (West African CFA franc).
 *           Adjust these values to match your pricing.
 *           Must be a multiple of 5 (CinetPay requirement).
 *           Set to 0 for free/trial plans.
 *
 * If you operate in multiple currencies, replace priceXOF with a
 * Map<String, Integer> pricePerCurrency or a separate PricingConfig.
 */
public enum PlanType {
    //           includedUsers  paid    durationDays  priceXOF
    TRIAL(              5,     false,      14,            0),
    PREMIUM(           25,     true,       30,        15000),   // TODO: set your actual price
    ENTERPRISE(        50,     true,       30,        35000);   // TODO: set your actual price

    private final int includedUsers;
    private final boolean paid;
    private final int durationInDays;
    private final int priceXOF;

    PlanType(int includedUsers, boolean paid, int durationInDays, int priceXOF) {
        this.includedUsers = includedUsers;
        this.paid = paid;
        this.durationInDays = durationInDays;
        this.priceXOF = priceXOF;
    }

    public int getIncludedUsers() { return includedUsers; }
    public boolean isPaid()        { return paid; }
    public int getDurationInDays() { return durationInDays; }
    public int getPriceXOF()       { return priceXOF; }
}