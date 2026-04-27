import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

public class ProcessPayment {

    static Scanner sc = new Scanner(System.in);
    static AuthService authService = new AuthService();
    static ArrayList<Bank> banks = new ArrayList<>();

    public static void main(String[] args) {

        // Connect to DB first
        if (DatabaseConnection.getConnection() == null) {
            System.out.println("❌ Cannot start without database. Please check your MySQL connection.");
            return;
        }

        banks.add(new Bank("HDFC Bank"));
        banks.add(new Bank("SBI Bank"));
        banks.add(new Bank("ICICI Bank"));

        // 🗄️ Load ALL accounts once at startup
        DatabaseService.loadAllAccounts(banks);

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║       🏦  BANKING SYSTEM         ║");
        System.out.println("╚══════════════════════════════════╝");

        while (true) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choice: ");

            int choice = readInt();
            if (choice == -1) continue;

            switch (choice) {
                case 1 -> registerUser();
                case 2 -> {
                    User user = loginUser();
                    if (user != null) userDashboard(user);
                }
                case 3 -> {
                    DatabaseConnection.closeConnection();
                    System.out.println("👋 Thank you for using our banking system!");
                    return;
                }
                default -> System.out.println("❌ Invalid choice. Please enter 1, 2 or 3.");
            }
        }
    }

    // ─── SAFE INPUT HELPERS ──────────────────────────────────

    static int readInt() {
        try {
            return sc.nextInt();
        } catch (InputMismatchException e) {
            sc.nextLine();
            System.out.println("⚠️  Please enter a number, not text.");
            return -1;
        }
    }

    static double readDouble() {
        try {
            double val = sc.nextDouble();
            if (val < 0) {
                System.out.println("⚠️  Amount cannot be negative.");
                return -1;
            }
            return val;
        } catch (InputMismatchException e) {
            sc.nextLine();
            System.out.println("⚠️  Please enter a valid number (e.g. 500 or 1000.50).");
            return -1;
        }
    }

    // ─── REGISTER ────────────────────────────────────────────
    static void registerUser() {
        System.out.print("Enter username: ");
        String username = sc.next();
        System.out.print("Enter password: ");
        String password = sc.next();
        authService.register(username, password);
    }

    // ─── LOGIN ───────────────────────────────────────────────
    static User loginUser() {
        System.out.print("Username: ");
        String username = sc.next();
        System.out.print("Password: ");
        String password = sc.next();

        User user = authService.login(username, password);

        if (user != null) {
            // Only link this user's accounts to their profile
            DatabaseService.loadAccountsForUser(user, banks);
        }

        return user;
    }

    // ─── USER DASHBOARD ──────────────────────────────────────
    static void userDashboard(User user) {
        while (true) {
            System.out.println("\n👤 Logged in as: " + user.getUsername());
            System.out.println("--- DASHBOARD ---");
            System.out.println("1. Open New Account");
            System.out.println("2. Select Account to Use");
            System.out.println("3. My Portfolio");
            System.out.println("4. Logout");
            System.out.print("Choice: ");

            int choice = readInt();
            if (choice == -1) continue;

            switch (choice) {
                case 1 -> openNewAccount(user);
                case 2 -> selectAccount(user);
                case 3 -> showPortfolio(user);
                case 4 -> {
                    System.out.println("🔒 Logged out successfully.");
                    return;
                }
                default -> System.out.println("❌ Invalid choice.");
            }
        }
    }

    // ─── OPEN NEW ACCOUNT ────────────────────────────────────
    static void openNewAccount(User user) {

        System.out.println("\n🏦 Choose a Bank:");
        for (int i = 0; i < banks.size(); i++) {
            System.out.println("[" + (i + 1) + "] " + banks.get(i).getBankName());
        }
        System.out.print("Choice: ");
        int bankChoice = readInt();
        if (bankChoice == -1) return;
        bankChoice -= 1;

        if (bankChoice < 0 || bankChoice >= banks.size()) {
            System.out.println("❌ Invalid bank selection.");
            return;
        }

        Bank selectedBank = banks.get(bankChoice);

        for (User.BankAccountEntry entry : user.getLinkedAccounts()) {
            if (entry.getBankName().equals(selectedBank.getBankName())) {
                System.out.println("⚠️  You already have an account in " + selectedBank.getBankName() + ".");
                System.out.println("   Only one account per bank is allowed per user.");
                return;
            }
        }

        System.out.print("Account Type (1 = Savings, 2 = Current): ");
        int type = readInt();
        if (type == -1) return;
        if (type != 1 && type != 2) {
            System.out.println("❌ Invalid account type. Choose 1 or 2.");
            return;
        }

        System.out.print("Initial Deposit (₹): ");
        double balance = readDouble();
        if (balance == -1) return;
        if (balance < 500) {
            System.out.println("❌ Minimum initial deposit is ₹500.");
            return;
        }

        System.out.print("Set a 4-digit PIN for this account: ");
        String pin = sc.next();
        if (pin.length() != 4 || !pin.matches("\\d+")) {
            System.out.println("❌ PIN must be exactly 4 digits (numbers only).");
            return;
        }

        String accNo = selectedBank.getBankService().generateAccountNumber();
        String accountType = (type == 1) ? "Savings" : "Current";

        Account acc;
        if (type == 1) {
            acc = new SavingsAccount(accNo, user.getUsername(), balance, pin);
        } else {
            acc = new CurrentAccount(accNo, user.getUsername(), balance, pin);
        }

        selectedBank.registerAccount(acc);
        user.linkAccount(selectedBank.getBankName(), acc);

        // 🗄️ Save to DB
        DatabaseService.saveAccount(user.getUsername(), selectedBank.getBankName(), acc, accountType);

        // Save opening deposit as first transaction
        Transaction openingTx = new Transaction("Opening Deposit", balance, accNo, user.getUsername(), null);
        acc.addTransaction(openingTx);
        DatabaseService.saveTransaction(accNo, openingTx);

        System.out.println("🎉 Account created successfully!");
        System.out.println("   Account Number : " + accNo);
        System.out.println("   Bank           : " + selectedBank.getBankName());
        System.out.println("   Opening Balance: ₹" + String.format("%.2f", balance));
    }

    // ─── SELECT ACCOUNT ──────────────────────────────────────
    static void selectAccount(User user) {

        if (user.getLinkedAccounts().isEmpty()) {
            System.out.println("⚠️  You have no accounts yet. Please open one first.");
            return;
        }

        user.showAccounts();

        System.out.print("Select account [1-" + user.getLinkedAccounts().size() + "]: ");
        int accChoice = readInt();
        if (accChoice == -1) return;
        accChoice -= 1;

        if (accChoice < 0 || accChoice >= user.getLinkedAccounts().size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        User.BankAccountEntry entry = user.getLinkedAccounts().get(accChoice);
        Account acc = entry.getAccount();

        if (acc.isLocked()) {
            System.out.println("🔒 This account is locked. Please contact your bank.");
            return;
        }

        System.out.print("Enter PIN: ");
        String pin = sc.next();

        if (!acc.verifyPin(pin)) {
            return;
        }

        System.out.println("🔓 Access granted!");
        System.out.println("   Account : " + acc.getAccountNumber());
        System.out.println("   Bank    : " + entry.getBankName());

        accountOperations(acc, entry.getBankName());
    }

    // ─── ACCOUNT OPERATIONS ──────────────────────────────────
    static void accountOperations(Account acc, String bankName) {

        Bank currentBank = getBankByName(bankName);

        while (true) {
            System.out.println("\n--- ACCOUNT MENU ---");
            System.out.println("Account: " + acc.getAccountNumber() + " | Bank: " + bankName);
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Transfer (within same bank)");
            System.out.println("4. Check Balance");
            System.out.println("5. Transaction History");
            System.out.println("6. Back");
            System.out.print("Choice: ");

            int choice = readInt();
            if (choice == -1) continue;

            switch (choice) {
                case 1 -> {
                    System.out.print("Deposit amount (₹): ");
                    double amt = readDouble();
                    if (amt != -1) acc.deposit(amt, false);
                }
                case 2 -> {
                    System.out.print("Withdraw amount (₹): ");
                    double amt = readDouble();
                    if (amt != -1) acc.withdraw(amt, false);
                }
                case 3 -> {
                    if (currentBank == null) {
                        System.out.println("❌ Bank not found.");
                        break;
                    }
                    Account recipient = null;
                    while (recipient == null) {
                        System.out.print("Recipient account number (or 0 to cancel): ");
                        String toAcc = sc.next();
                        if (toAcc.equals("0")) break;
                        if (toAcc.equals(acc.getAccountNumber())) {
                            System.out.println("❌ Cannot transfer to your own account. Try again.");
                            continue;
                        }
                        recipient = currentBank.getBankService().findAccount(toAcc);
                        if (recipient == null) {
                            System.out.println("❌ Account not found in " + bankName + ". Try again.");
                        }
                    }
                    if (recipient == null) break;

                    double amt = -1;
                    while (amt == -1) {
                        System.out.print("Amount (₹): ");
                        amt = readDouble();
                        if (amt == -1) continue;
                        if (amt > acc.getBalance()) {
                            System.out.println("❌ Insufficient balance. Available: ₹" + String.format("%.2f", acc.getBalance()));
                            amt = -1;
                        }
                    }
                    currentBank.getBankService().transfer(acc.getAccountNumber(), recipient.getAccountNumber(), amt);
                }
                case 4 -> acc.checkBalance();
                case 5 -> acc.showTransactions();
                case 6 -> { return; }
                default -> System.out.println("❌ Invalid choice.");
            }
        }
    }

    // ─── MY PORTFOLIO ────────────────────────────────────────
    static void showPortfolio(User user) {
        ArrayList<User.BankAccountEntry> accounts = user.getLinkedAccounts();

        if (accounts.isEmpty()) {
            System.out.println("⚠️  No accounts found. Open an account first.");
            return;
        }

        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║           💼  MY PORTFOLIO                       ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  👤 Account Holder : %-28s║%n", user.getUsername());
        System.out.println("╠══════════════════════════════════════════════════╣");

        double totalBalance = 0;

        for (User.BankAccountEntry entry : accounts) {
            Account acc = entry.getAccount();
            String type = acc instanceof SavingsAccount ? "Savings" : "Current";
            String lockStatus = acc.isLocked() ? " 🔒" : "";
            totalBalance += acc.getBalance();

            System.out.printf("║  🏦 %-15s | %-8s | ₹%,-15.2f  ║%n",
                    entry.getBankName().replace(" Bank", ""),
                    type,
                    acc.getBalance()
            );
            System.out.printf("║     Acc No: %-37s║%n", acc.getAccountNumber() + lockStatus);
            System.out.println("║  ------------------------------------------------║");
        }

        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  💰 Total Balance : ₹%,-27.2f║%n", totalBalance);
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    // ─── HELPER ──────────────────────────────────────────────
    static Bank getBankByName(String name) {
        for (Bank b : banks) {
            if (b.getBankName().equals(name)) return b;
        }
        return null;
    }
}