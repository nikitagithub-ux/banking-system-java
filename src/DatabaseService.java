import java.sql.*;
import java.time.LocalDateTime;

// Handles ALL database read/write operations
public class DatabaseService {

    // ─── USERS ───────────────────────────────────────────────

    public static void saveUser(User user) {
        String sql = "INSERT INTO users (username, password, login_locked, login_attempts) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordRaw());
            ps.setBoolean(3, user.isLoginLocked());
            ps.setInt(4, user.getLoginAttempts());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error saving user: " + e.getMessage());
        }
    }

    public static void updateUserLockStatus(User user) {
        String sql = "UPDATE users SET login_locked = ?, login_attempts = ? WHERE username = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, user.isLoginLocked());
            ps.setInt(2, user.getLoginAttempts());
            ps.setString(3, user.getUsername());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error updating user lock status: " + e.getMessage());
        }
    }

    // Load a user from DB — returns null if not found
    public static User loadUser(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = new User(rs.getString("username"), rs.getString("password"));
                // Restore lock state
                for (int i = 0; i < rs.getInt("login_attempts"); i++) {
                    user.incrementLoginAttempts();
                }
                if (rs.getBoolean("login_locked")) user.lockLogin();
                return user;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error loading user: " + e.getMessage());
        }
        return null;
    }

    public static boolean usernameExists(String username) {
        String sql = "SELECT username FROM users WHERE username = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("❌ Error checking username: " + e.getMessage());
        }
        return false;
    }

    // ─── ACCOUNTS ────────────────────────────────────────────

    public static void saveAccount(String username, String bankName, Account acc, String accountType) {
        String sql = "INSERT INTO accounts (account_number, username, bank_name, account_type, balance, pin, is_locked, failed_pin_attempts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, acc.getAccountNumber());
            ps.setString(2, username);
            ps.setString(3, bankName);
            ps.setString(4, accountType);
            ps.setDouble(5, acc.getBalance());
            ps.setString(6, acc.getPinRaw());
            ps.setBoolean(7, acc.isLocked());
            ps.setInt(8, acc.getFailedPinAttempts());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error saving account: " + e.getMessage());
        }
    }

    public static void updateAccountBalance(Account acc) {
        String sql = "UPDATE accounts SET balance = ?, is_locked = ?, failed_pin_attempts = ? WHERE account_number = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, acc.getBalance());
            ps.setBoolean(2, acc.isLocked());
            ps.setInt(3, acc.getFailedPinAttempts());
            ps.setString(4, acc.getAccountNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error updating account: " + e.getMessage());
        }
    }

    // Load all accounts for a user and link them
    public static void loadAccountsForUser(User user, java.util.ArrayList<Bank> banks) {
        String sql = "SELECT * FROM accounts WHERE username = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String accNo    = rs.getString("account_number");
                String bankName = rs.getString("bank_name");

                // ✅ REUSE the existing account object already loaded at startup
                for (Bank b : banks) {
                    if (b.getBankName().equals(bankName)) {
                        Account existing = b.getBankService().findAccount(accNo);
                        if (existing != null) {
                            user.linkAccountSilent(bankName, existing); // link the SAME object
                        }
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error loading accounts: " + e.getMessage());
        }
    }

    // ─── TRANSACTIONS ─────────────────────────────────────────

    public static void saveTransaction(String accountNumber, Transaction t) {
        String sql = "INSERT INTO transactions (account_number, type, amount, related_account, date_time) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, t.getType());
            ps.setDouble(3, t.getAmount());
            ps.setString(4, t.getRelatedAccount());
            ps.setObject(5, t.getDate());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Error saving transaction: " + e.getMessage());
        }
    }

    public static void loadTransactionsForAccount(Account acc) {
        String sql = "SELECT * FROM transactions WHERE account_number = ? ORDER BY date_time ASC";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, acc.getAccountNumber());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        acc.getAccountNumber(),
                        acc.getAccountHolderName(),
                        rs.getString("related_account"),
                        rs.getObject("date_time", LocalDateTime.class)
                );
                acc.addTransaction(t);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error loading transactions: " + e.getMessage());
        }
    }

    // Load ALL accounts into their banks (called once at startup)
    public static void loadAllAccounts(java.util.ArrayList<Bank> banks) {
        String sql = "SELECT * FROM accounts";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String accNo    = rs.getString("account_number");
                String username = rs.getString("username");
                String bankName = rs.getString("bank_name");
                String type     = rs.getString("account_type");
                double balance  = rs.getDouble("balance");
                String pin      = rs.getString("pin");
                boolean locked  = rs.getBoolean("is_locked");
                int failedAttempts = rs.getInt("failed_pin_attempts");

                Account acc;
                if (type.equals("Savings")) {
                    acc = new SavingsAccount(accNo, username, balance, pin);
                } else {
                    acc = new CurrentAccount(accNo, username, balance, pin);
                }

                for (int i = 0; i < failedAttempts; i++) acc.incrementFailedAttempts();
                if (locked) acc.forceLock();

                for (Bank b : banks) {
                    if (b.getBankName().equals(bankName)) {
                        b.registerAccount(acc);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error loading all accounts: " + e.getMessage());
        }
    }
}