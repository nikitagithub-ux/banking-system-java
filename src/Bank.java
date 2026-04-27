public class Bank {

    private String bankName;
    private BankService bankService;

    public Bank(String bankName) {
        this.bankName = bankName;
        this.bankService = new BankService();
    }

    public String getBankName() { return bankName; }
    public BankService getBankService() { return bankService; }

    // Convenience: register an account into this bank
    public void registerAccount(Account acc) {
        bankService.addAccount(acc);
    }

    // Convenience: find account by number
    public Account findAccount(String accNo) {
        return bankService.findAccount(accNo);
    }
}