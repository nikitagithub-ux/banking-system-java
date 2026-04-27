import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ApiServer {

    static AuthService authService = new AuthService();
    static ArrayList<Bank> banks = new ArrayList<>();
    // Session store: token -> User
    static Map<String, User> sessions = new HashMap<>();

    public static void main(String[] args) throws Exception {

        if (DatabaseConnection.getConnection() == null) {
            System.out.println("❌ Cannot start without database.");
            return;
        }

        banks.add(new Bank("HDFC Bank"));
        banks.add(new Bank("SBI Bank"));
        banks.add(new Bank("ICICI Bank"));
        DatabaseService.loadAllAccounts(banks);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/register",     new RegisterHandler());
        server.createContext("/api/login",        new LoginHandler());
        server.createContext("/api/accounts",     new AccountsHandler());
        server.createContext("/api/open-account", new OpenAccountHandler());
        server.createContext("/api/verify-pin",   new VerifyPinHandler());
        server.createContext("/api/deposit",      new DepositHandler());
        server.createContext("/api/withdraw",     new WithdrawHandler());
        server.createContext("/api/transfer",     new TransferHandler());
        server.createContext("/api/balance",      new BalanceHandler());
        server.createContext("/api/transactions", new TransactionsHandler());
        server.createContext("/api/portfolio",    new PortfolioHandler());
        server.createContext("/",                 new StaticHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Banking System running at http://localhost:8080");
    }

    // ─── STATIC FILE HANDLER ─────────────────────────────────
    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                File file = new File("index.html");
                if (file.exists()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                    ex.getResponseHeaders().set("Content-Type", "text/html");
                    ex.sendResponseHeaders(200, bytes.length);
                    ex.getResponseBody().write(bytes);
                } else {
                    String msg = "index.html not found. Place it in the same folder as ApiServer.java";
                    sendResponse(ex, 404, "{\"error\":\"" + msg + "\"}");
                }
            }
            ex.getResponseBody().close();
        }
    }

    // ─── REGISTER ────────────────────────────────────────────
    static class RegisterHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equals("POST")) { sendResponse(ex, 405, "{}"); return; }
            Map<String, String> body = parseBody(ex);
            String username = body.getOrDefault("username", "");
            String password = body.getOrDefault("password", "");

            boolean ok = authService.register(username, password);
            if (ok) sendResponse(ex, 200, "{\"success\":true,\"message\":\"Registered successfully!\"}");
            else    sendResponse(ex, 400, "{\"success\":false,\"message\":\"Registration failed. Check requirements.\"}");
        }
    }

    // ─── LOGIN ───────────────────────────────────────────────
    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equals("POST")) { sendResponse(ex, 405, "{}"); return; }
            Map<String, String> body = parseBody(ex);
            String username = body.getOrDefault("username", "");
            String password = body.getOrDefault("password", "");

            User user = authService.login(username, password);
            if (user != null) {
                DatabaseService.loadAccountsForUser(user, banks);
                String token = username + "_" + System.currentTimeMillis();
                sessions.put(token, user);
                sendResponse(ex, 200, "{\"success\":true,\"token\":\"" + token + "\",\"username\":\"" + username + "\"}");
            } else {
                sendResponse(ex, 401, "{\"success\":false,\"message\":\"Invalid credentials.\"}");
            }
        }
    }

    // ─── GET ACCOUNTS ─────────────────────────────────────────
    static class AccountsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            StringBuilder json = new StringBuilder("[");
            ArrayList<User.BankAccountEntry> accs = user.getLinkedAccounts();
            for (int i = 0; i < accs.size(); i++) {
                User.BankAccountEntry e = accs.get(i);
                String type = e.getAccount() instanceof SavingsAccount ? "Savings" : "Current";
                json.append("{")
                        .append("\"index\":").append(i).append(",")
                        .append("\"bank\":\"").append(e.getBankName()).append("\",")
                        .append("\"type\":\"").append(type).append("\",")
                        .append("\"accNo\":\"").append(e.getAccount().getAccountNumber()).append("\",")
                        .append("\"locked\":").append(e.getAccount().isLocked())
                        .append("}");
                if (i < accs.size() - 1) json.append(",");
            }
            json.append("]");
            sendResponse(ex, 200, json.toString());
        }
    }

    // ─── OPEN ACCOUNT ─────────────────────────────────────────
    static class OpenAccountHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equals("POST")) { sendResponse(ex, 405, "{}"); return; }
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            Map<String, String> body = parseBody(ex);
            String bankName = body.getOrDefault("bank", "");
            String type     = body.getOrDefault("type", "");
            String pin      = body.getOrDefault("pin", "");
            double balance;
            try { balance = Double.parseDouble(body.getOrDefault("balance", "0")); }
            catch (NumberFormatException e) { sendResponse(ex, 400, "{\"success\":false,\"message\":\"Invalid amount.\"}"); return; }

            if (balance < 500) { sendResponse(ex, 400, "{\"success\":false,\"message\":\"Minimum deposit is ₹500.\"}"); return; }
            if (!pin.matches("\\d{4}")) { sendResponse(ex, 400, "{\"success\":false,\"message\":\"PIN must be 4 digits.\"}"); return; }

            for (User.BankAccountEntry e : user.getLinkedAccounts()) {
                if (e.getBankName().equals(bankName)) {
                    sendResponse(ex, 400, "{\"success\":false,\"message\":\"You already have an account in " + bankName + ".\"}");
                    return;
                }
            }

            Bank selectedBank = getBankByName(bankName);
            if (selectedBank == null) { sendResponse(ex, 400, "{\"success\":false,\"message\":\"Bank not found.\"}"); return; }

            String accNo = selectedBank.getBankService().generateAccountNumber();
            Account acc = type.equals("Savings")
                    ? new SavingsAccount(accNo, user.getUsername(), balance, pin)
                    : new CurrentAccount(accNo, user.getUsername(), balance, pin);

            selectedBank.registerAccount(acc);
            user.linkAccount(bankName, acc);
            DatabaseService.saveAccount(user.getUsername(), bankName, acc, type);

            Transaction tx = new Transaction("Opening Deposit", balance, accNo, user.getUsername(), null);
            acc.addTransaction(tx);
            DatabaseService.saveTransaction(accNo, tx);

            sendResponse(ex, 200, "{\"success\":true,\"accNo\":\"" + accNo + "\",\"message\":\"Account created!\"}");
        }
    }

    // ─── VERIFY PIN ───────────────────────────────────────────
    static class VerifyPinHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equals("POST")) { sendResponse(ex, 405, "{}"); return; }
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            Map<String, String> body = parseBody(ex);
            int index = Integer.parseInt(body.getOrDefault("index", "0"));
            String pin = body.getOrDefault("pin", "");

            Account acc = user.getLinkedAccounts().get(index).getAccount();
            if (acc.isLocked()) { sendResponse(ex, 403, "{\"success\":false,\"message\":\"Account is locked.\"}"); return; }

            boolean ok = acc.verifyPin(pin);
            if (ok) sendResponse(ex, 200, "{\"success\":true}");
            else    sendResponse(ex, 401, "{\"success\":false,\"message\":\"Incorrect PIN.\"}");
        }
    }

    // ─── DEPOSIT ──────────────────────────────────────────────
    static class DepositHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equals("POST")) { sendResponse(ex, 405, "{}"); return; }
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            Map<String, String> body = parseBody(ex);
            int index = Integer.parseInt(body.getOrDefault("index", "0"));
            double amount = Double.parseDouble(body.getOrDefault("amount", "0"));

            Account acc = user.getLinkedAccounts().get(index).getAccount();
            double before = acc.getBalance();
            acc.deposit(amount, false);

            if (acc.getBalance() > before) {
                sendResponse(ex, 200, "{\"success\":true,\"balance\":" + acc.getBalance() + "}");
            } else {
                sendResponse(ex, 400, "{\"success\":false,\"message\":\"Deposit failed.\"}");
            }
        }
    }

    // ─── WITHDRAW ─────────────────────────────────────────────
    static class WithdrawHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equals("POST")) { sendResponse(ex, 405, "{}"); return; }
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            Map<String, String> body = parseBody(ex);
            int index = Integer.parseInt(body.getOrDefault("index", "0"));
            double amount = Double.parseDouble(body.getOrDefault("amount", "0"));

            Account acc = user.getLinkedAccounts().get(index).getAccount();
            double before = acc.getBalance();
            acc.withdraw(amount, false);

            if (acc.getBalance() < before) {
                sendResponse(ex, 200, "{\"success\":true,\"balance\":" + acc.getBalance() + "}");
            } else {
                sendResponse(ex, 400, "{\"success\":false,\"message\":\"Insufficient balance.\"}");
            }
        }
    }

    // ─── TRANSFER ─────────────────────────────────────────────
    static class TransferHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equals("POST")) { sendResponse(ex, 405, "{}"); return; }
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            Map<String, String> body = parseBody(ex);
            int index      = Integer.parseInt(body.getOrDefault("index", "0"));
            String toAccNo = body.getOrDefault("toAccNo", "");
            double amount  = Double.parseDouble(body.getOrDefault("amount", "0"));

            User.BankAccountEntry entry = user.getLinkedAccounts().get(index);
            Bank bank = getBankByName(entry.getBankName());

            if (bank == null) { sendResponse(ex, 400, "{\"success\":false,\"message\":\"Bank not found.\"}"); return; }

            boolean success = bank.getBankService().transfer(entry.getAccount().getAccountNumber(), toAccNo, amount);

            if (!success) {
                sendResponse(ex, 400, "{\"success\":false,\"message\":\"Transfer failed. Recipient account not found in this bank. Cross-bank transfers are not supported.\"}");
                return;
            }

            sendResponse(ex, 200, "{\"success\":true,\"balance\":" + entry.getAccount().getBalance() + "}");
        }
    }

    // ─── BALANCE ──────────────────────────────────────────────
    static class BalanceHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            String query = ex.getRequestURI().getQuery();
            int index = Integer.parseInt(query.replace("index=", ""));
            Account acc = user.getLinkedAccounts().get(index).getAccount();
            sendResponse(ex, 200, "{\"balance\":" + acc.getBalance() + "}");
        }
    }

    // ─── TRANSACTIONS ─────────────────────────────────────────
    static class TransactionsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            String query = ex.getRequestURI().getQuery();
            int index = Integer.parseInt(query.replace("index=", ""));
            Account acc = user.getLinkedAccounts().get(index).getAccount();

            // Rebuild transaction list from DB fresh
            Account fresh = new SavingsAccount(acc.getAccountNumber(), acc.getAccountHolderName(), acc.getBalance(), "0000");
            DatabaseService.loadTransactionsForAccount(fresh);

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Transaction t : getTransactions(fresh)) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"type\":\"").append(t.getType()).append("\",")
                        .append("\"amount\":").append(t.getAmount()).append(",")
                        .append("\"related\":\"").append(t.getRelatedAccount() != null ? t.getRelatedAccount() : "").append("\",")
                        .append("\"date\":\"").append(t.getDate()).append("\"")
                        .append("}");
                first = false;
            }
            json.append("]");
            sendResponse(ex, 200, json.toString());
        }
    }

    // ─── PORTFOLIO ────────────────────────────────────────────
    static class PortfolioHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            User user = getUser(ex);
            if (user == null) { sendResponse(ex, 401, "{\"error\":\"Not logged in\"}"); return; }

            double total = 0;
            StringBuilder json = new StringBuilder("{\"username\":\"" + user.getUsername() + "\",\"accounts\":[");
            ArrayList<User.BankAccountEntry> accs = user.getLinkedAccounts();
            for (int i = 0; i < accs.size(); i++) {
                User.BankAccountEntry e = accs.get(i);
                String type = e.getAccount() instanceof SavingsAccount ? "Savings" : "Current";
                total += e.getAccount().getBalance();
                json.append("{")
                        .append("\"bank\":\"").append(e.getBankName()).append("\",")
                        .append("\"type\":\"").append(type).append("\",")
                        .append("\"accNo\":\"").append(e.getAccount().getAccountNumber()).append("\",")
                        .append("\"balance\":").append(e.getAccount().getBalance()).append(",")
                        .append("\"locked\":").append(e.getAccount().isLocked())
                        .append("}");
                if (i < accs.size() - 1) json.append(",");
            }
            json.append("],\"total\":").append(total).append("}");
            sendResponse(ex, 200, json.toString());
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────

    static User getUser(HttpExchange ex) {
        String token = ex.getRequestHeaders().getFirst("Authorization");
        if (token == null) return null;
        return sessions.get(token.replace("Bearer ", ""));
    }

    static void sendResponse(HttpExchange ex, int code, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    static Map<String, String> parseBody(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(
                    java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
            );
        }
        // Also try JSON
        if (body.startsWith("{")) {
            body = body.replaceAll("[{}\"]", "");
            for (String pair : body.split(",")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    static Bank getBankByName(String name) {
        for (Bank b : banks) if (b.getBankName().equals(name)) return b;
        return null;
    }

    // Reflection-free transaction access via DB reload
    static java.util.List<Transaction> getTransactions(Account acc) {
        java.util.List<Transaction> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_number = ? ORDER BY date_time ASC";
        try (java.sql.PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, acc.getAccountNumber());
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        acc.getAccountNumber(),
                        acc.getAccountHolderName(),
                        rs.getString("related_account"),
                        rs.getObject("date_time", java.time.LocalDateTime.class)
                ));
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching transactions: " + e.getMessage());
        }
        return list;
    }
}