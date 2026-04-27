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

    // PIN verification with lockout
    public boolean verifyPin(String inputPin) {
        if (locked) {
            System.out.println("🔒 Account is locked due to too many failed PIN attempts.");
            System.out.println("   Please contact your bank to unlock.");
            return false;
        }
        if (this.pin.equals(inputPin)) {
            failedPinAttempts = 0;
            DatabaseService.updateAccountBalance(this);
            return true;
        } else {
            failedPinAttempts++;
            int remaining = MAX_PIN_ATTEMPTS - failedPinAttempts;
            if (remaining > 0) {
                System.out.println("❌ Incorrect PIN. " + remaining + " attempt(s) remaining.");
            } else {
                locked = true;
                System.out.println("🔒 Account LOCKED after " + MAX_PIN_ATTEMPTS + " failed attempts.");
            }
            DatabaseService.updateAccountBalance(this);
            return false;
        }
    }

    // Used when restoring from DB
    public void incrementFailedAttempts() { failedPinAttempts++; }
    public void forceLock() { locked = true; }

    public boolean isLocked()              { return locked; }
    public int getFailedPinAttempts()      { return failedPinAttempts; }
    public String getAccountHolderName()   { return accountHolderName; }
    public String getAccountNumber()       { return accountNumber; }
    public double getBalance()             { return balance; }
    public String getPinRaw()              { return pin; }

    public void deposit(double amount, boolean isTransfer) {
        if (amount <= 0) {
            System.out.println("❌ Deposit amount must be greater than zero.");
            return;
        }
        balance += amount;
        if (!isTransfer) {
            Transaction t = new Transaction("Deposit", amount, accountNumber, accountHolderName, null);
            transactions.add(t);
            DatabaseService.saveTransaction(accountNumber, t);
            System.out.println("✅ Deposit Successful! New Balance: ₹" + String.format("%.2f", balance));
        }
        DatabaseService.updateAccountBalance(this);
    }

    public void withdraw(double amount, boolean isTransfer) {
        if (amount <= 0) {
            System.out.println("❌ Withdrawal amount must be greater than zero.");
            return;
        }
        if (amount > balance) {
            System.out.println("❌ Insufficient balance. Available: ₹" + String.format("%.2f", balance));
            return;
        }
        balance -= amount;
        if (!isTransfer) {
            Transaction t = new Transaction("Withdraw", amount, accountNumber, accountHolderName, null);
            transactions.add(t);
            DatabaseService.saveTransaction(accountNumber, t);
            System.out.println("✅ Withdrawal Successful! New Balance: ₹" + String.format("%.2f", balance));
        }
        DatabaseService.updateAccountBalance(this);
    }

    public void checkBalance() {
        System.out.println("💰 Account Balance: ₹" + String.format("%.2f", balance));
        System.out.println("----------------------------------");
    }

    public void showTransactions() {
        if (transactions.isEmpty()) {
            System.out.println("⚠️  No transactions found.");
            return;
        }
        System.out.println("\n📜 Transaction History:");
        System.out.println("==============================");
        for (Transaction t : transactions) {
            System.out.println(t);
        }
    }

    public void addTransaction(Transaction t) { transactions.add(t); }
    public void updateBalanceFromDb(double newBalance) {
        this.balance = newBalance;
    }
}