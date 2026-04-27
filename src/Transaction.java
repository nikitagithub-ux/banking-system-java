import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {

    private String type;
    private double amount;
    private String accountNumber;
    private String accountHolderName;
    private String relatedAccount;
    private LocalDateTime date;

    // Constructor for new transactions (auto timestamp)
    public Transaction(String type, double amount, String accNo, String name, String relatedAccount) {
        this.type = type;
        this.amount = amount;
        this.accountNumber = accNo;
        this.accountHolderName = name;
        this.relatedAccount = relatedAccount;
        this.date = LocalDateTime.now();
    }

    // Constructor for loading from DB (timestamp from DB)
    public Transaction(String type, double amount, String accNo, String name, String relatedAccount, LocalDateTime date) {
        this.type = type;
        this.amount = amount;
        this.accountNumber = accNo;
        this.accountHolderName = name;
        this.relatedAccount = relatedAccount;
        this.date = date;
    }

    public String getType()            { return type; }
    public double getAmount()          { return amount; }
    public String getRelatedAccount()  { return relatedAccount; }
    public LocalDateTime getDate()     { return date; }

    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String details = "📄 Transaction Details\n" +
                "----------------------------------\n" +
                "Account Holder : " + accountHolderName + "\n" +
                "Account Number : " + accountNumber + "\n" +
                "Type           : " + type + "\n" +
                "Amount         : ₹" + String.format("%.2f", amount) + "\n";
        if (relatedAccount != null) {
            details += "Related Account: " + relatedAccount + "\n";
        }
        details += "Date & Time    : " + date.format(formatter) + "\n" +
                "----------------------------------";
        return details;
    }
}