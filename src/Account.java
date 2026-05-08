import java.util.ArrayList;

public class Account {

    private String accountNumber;
    private double balance;
    private ArrayList<Transaction> transactions = new ArrayList<>();
    private String accountHolderName;
    private String pin;

    private int failedPinAttempts = 0;
    private static final int MAX_PIN_ATTEMPTS = 3;
    private boolean locked = false;

    public Account(String accountNumber, String accountHolderName, double initialBalance, String pin) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = initialBalance;
        this.pin = pin;
    }

    // PIN verification with lockout — 3 wrong attempts locks the account
    public boolean verifyPin(String inputPin) {
        if (locked) return false;

        if (this.pin.equals(inputPin)) {
            failedPinAttempts = 0;
            DatabaseService.updateAccountBalance(this);
            return true;
        } else {
            failedPinAttempts++;
            if (failedPinAttempts >= MAX_PIN_ATTEMPTS) locked = true;
            DatabaseService.updateAccountBalance(this);
            return false;
        }
    }

    // Called when restoring lock state from DB at startup
    public void incrementFailedAttempts() { failedPinAttempts++; }
    public void forceLock()               { locked = true; }

    // Getters
    public boolean isLocked()            { return locked; }
    public int getFailedPinAttempts()    { return failedPinAttempts; }
    public String getAccountHolderName() { return accountHolderName; }
    public String getAccountNumber()     { return accountNumber; }
    public double getBalance()           { return balance; }
    public String getPinRaw()            { return pin; }

    // Deposit money. isTransfer=true means BankService.transfer() is calling this
    // and will handle its own transaction records — skip auto-logging here.
    public void deposit(double amount, boolean isTransfer) {
        if (amount <= 0) return;
        balance += amount;
        if (!isTransfer) {
            Transaction t = new Transaction("Deposit", amount, accountNumber, accountHolderName, null);
            transactions.add(t);
            DatabaseService.saveTransaction(accountNumber, t);
        }
        DatabaseService.updateAccountBalance(this);
    }

    // Withdraw money. Same isTransfer logic as deposit.
    public void withdraw(double amount, boolean isTransfer) {
        if (amount <= 0 || amount > balance) return;
        balance -= amount;
        if (!isTransfer) {
            Transaction t = new Transaction("Withdraw", amount, accountNumber, accountHolderName, null);
            transactions.add(t);
            DatabaseService.saveTransaction(accountNumber, t);
        }
        DatabaseService.updateAccountBalance(this);
    }

    public void addTransaction(Transaction t) { transactions.add(t); }
}