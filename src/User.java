import java.util.ArrayList;

public class User {

    private String username;
    private String password;
    private ArrayList<BankAccountEntry> linkedAccounts = new ArrayList<>();

    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private boolean loginLocked = false;

    // Pairs a bank name with an Account object — one entry per bank account the user owns
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

    public String getUsername()                    { return username; }
    public String getPasswordRaw()                 { return password; }
    public boolean checkPassword(String input)     { return this.password.equals(input); }
    public boolean isLoginLocked()                 { return loginLocked; }
    public int getLoginAttempts()                  { return loginAttempts; }
    public void incrementLoginAttempts()           { loginAttempts++; }
    public void resetLoginAttempts()               { loginAttempts = 0; }
    public void lockLogin()                        { loginLocked = true; }
    public ArrayList<BankAccountEntry> getLinkedAccounts() { return linkedAccounts; }

    // Used when creating a new account — links it to this user
    public void linkAccount(String bankName, Account account) {
        linkedAccounts.add(new BankAccountEntry(bankName, account));
    }

    // Used when loading existing accounts from DB at login — silent, no side effects
    public void linkAccountSilent(String bankName, Account account) {
        linkedAccounts.add(new BankAccountEntry(bankName, account));
    }
}