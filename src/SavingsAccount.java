public class SavingsAccount extends Account {

    public SavingsAccount(String accountNumber, String name, double balance, String pin) {
        super(accountNumber, name, balance, pin);
    }

    public void addInterest() {
        double interest = 0.05; // 5%
        System.out.println("📈 Interest (5%) added to savings account.");
    }
}