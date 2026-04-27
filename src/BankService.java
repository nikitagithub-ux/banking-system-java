import java.util.ArrayList;
import java.util.Random;

public class BankService {

    private ArrayList<Account> accounts = new ArrayList<>();

    // Change your addAccount method to this:
    public void addAccount(Account acc) {
        for (Account a : accounts) {
            if (a.getAccountNumber().equals(acc.getAccountNumber())) {
                // Just return silently. The account is already in memory.
                return;
            }
        }
        accounts.add(acc);
    }

    public Account findAccount(String accountNumber) {
        for (Account acc : accounts) {
            if (acc.getAccountNumber().equals(accountNumber)) {
                return acc;
            }
        }
        return null;
    }

    public boolean transfer(String fromAccNo, String toAccNo, double amount) {
        if (fromAccNo.equals(toAccNo)) {
            System.out.println("❌ Cannot transfer to the same account.");
            return false;
        }

        Account sender   = findAccount(fromAccNo);
        Account receiver = findAccount(toAccNo);

        if (sender == null) {
            System.out.println("❌ Sender account not found.");
            return false;
        }
        if (receiver == null) {
            System.out.println("❌ Recipient account not found in this bank.");
            return false;
        }
        if (amount <= 0) {
            System.out.println("❌ Transfer amount must be greater than zero.");
            return false;
        }
        if (amount > sender.getBalance()) {
            System.out.println("❌ Insufficient balance. Available: ₹" + String.format("%.2f", sender.getBalance()));
            return false;
        }

        System.out.println("\n🔄 Processing Transfer...");
        System.out.println("   From  : " + sender.getAccountNumber() + " (" + sender.getAccountHolderName() + ")");
        System.out.println("   To    : " + receiver.getAccountNumber() + " (" + receiver.getAccountHolderName() + ")");
        System.out.println("   Amount: ₹" + String.format("%.2f", amount));

        sender.withdraw(amount, true);
        receiver.deposit(amount, true);

        Transaction sentTx = new Transaction("Transfer Sent", amount,
                sender.getAccountNumber(), sender.getAccountHolderName(), receiver.getAccountNumber());
        Transaction recvTx = new Transaction("Transfer Received", amount,
                receiver.getAccountNumber(), receiver.getAccountHolderName(), sender.getAccountNumber());

        sender.addTransaction(sentTx);
        receiver.addTransaction(recvTx);

        DatabaseService.saveTransaction(sender.getAccountNumber(), sentTx);
        DatabaseService.saveTransaction(receiver.getAccountNumber(), recvTx);

        System.out.println("✅ Transfer Completed Successfully!");
        System.out.println("----------------------------------");
        return true;
    }

    // Clear in-memory accounts (used before reloading from DB on login)
    public void clearAccounts() { accounts.clear(); }

    public String generateAccountNumber() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder accNo = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 10; i++) {
            accNo.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return accNo.toString();
    }
}