// Savings account — extends Account with a 5% annual interest feature
public class SavingsAccount extends Account {

    private static final double INTEREST_RATE = 0.05;

    public SavingsAccount(String accountNumber, String name, double balance, String pin) {
        super(accountNumber, name, balance, pin);
    }

    // Applies 5% interest to the current balance and saves it
    public void addInterest() {
        double interest = getBalance() * INTEREST_RATE;
        deposit(interest, false);
    }
}