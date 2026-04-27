import java.util.ArrayList;

public class User {

    private String username;
    private String password;
    private ArrayList<BankAccountEntry> linkedAccounts = new ArrayList<>();

    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private boolean loginLocked = false;

    public static class BankAccountEntry {
        private String bankName;
        private Account account;

        public BankAccountEntry(String bankName, Account account) {
            this.bankName = bankName;
            this.account = account;
        }

        public String getBankName() { return bankName; }
        public Account getAccount() { return account; }
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername()          { return username; }
    public String getPasswordRaw()       { return password; }
    public boolean checkPassword(String input) { return this.password.equals(input); }

    public boolean isLoginLocked()       { return loginLocked; }
    public int getLoginAttempts()        { return loginAttempts; }
    public void incrementLoginAttempts() { loginAttempts++; }
    public void resetLoginAttempts()     { loginAttempts = 0; }
    public void lockLogin()              { loginLocked = true; }

    // Links account AND prints confirmation (used when creating new account)
    public void linkAccount(String bankName, Account account) {
        linkedAccounts.add(new BankAccountEntry(bankName, account));
        System.out.println("✅ Account linked under bank: " + bankName);
    }

    // Links account silently (used when loading from DB — no print)
    public void linkAccountSilent(String bankName, Account account) {
        linkedAccounts.add(new BankAccountEntry(bankName, account));
    }

    public ArrayList<BankAccountEntry> getLinkedAccounts() { return linkedAccounts; }

    public void showAccounts() {
        if (linkedAccounts.isEmpty()) {
            System.out.println("⚠️  No accounts linked to this user.");
            return;
        }
        System.out.println("\n📋 Your Accounts:");
        System.out.println("==========================================");
        for (int i = 0; i < linkedAccounts.size(); i++) {
            BankAccountEntry entry = linkedAccounts.get(i);
            String type = entry.getAccount() instanceof SavingsAccount ? "Savings" : "Current";
            String lockStatus = entry.getAccount().isLocked() ? " 🔒 LOCKED" : "";
            System.out.printf("[%d] 🏦 %-20s | %-10s | Acc: %s%s%n",
                    i + 1,
                    entry.getBankName(),
                    type,
                    entry.getAccount().getAccountNumber(),
                    lockStatus
            );
        }
        System.out.println("==========================================");
    }
}