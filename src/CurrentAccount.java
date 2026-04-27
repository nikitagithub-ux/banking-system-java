public class CurrentAccount extends Account {

    public CurrentAccount(String accountNumber, String name, double balance, String pin) {
        super(accountNumber, name, balance, pin);
    }

    public void overdraft() {
        System.out.println("💳 Overdraft facility available on this account.");
    }
}