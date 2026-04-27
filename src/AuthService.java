public class AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    public boolean register(String username, String password) {
        if (username.trim().isEmpty() || username.length() < 3) {
            System.out.println("❌ Username must be at least 3 characters.");
            return false;
        }
        if (username.contains(" ")) {
            System.out.println("❌ Username cannot contain spaces.");
            return false;
        }
        if (password.length() < 4) {
            System.out.println("❌ Password must be at least 4 characters.");
            return false;
        }

        // Check DB for duplicate
        if (DatabaseService.usernameExists(username)) {
            System.out.println("❌ Username already taken. Please choose another.");
            return false;
        }

        User user = new User(username, password);
        DatabaseService.saveUser(user);
        System.out.println("✅ User registered successfully! You can now login.");
        return true;
    }

    public User login(String username, String password) {
        // Load user from DB
        User user = DatabaseService.loadUser(username);

        if (user == null) {
            System.out.println("❌ No account found with username: " + username);
            return null;
        }

        if (user.isLoginLocked()) {
            System.out.println("🔒 Account locked due to too many failed login attempts.");
            System.out.println("   Please contact support to unlock.");
            return null;
        }

        if (user.checkPassword(password)) {
            user.resetLoginAttempts();
            DatabaseService.updateUserLockStatus(user);
            System.out.println("✅ Login successful! Welcome back, " + user.getUsername() + "!");
            return user;
        } else {
            user.incrementLoginAttempts();
            int remaining = MAX_LOGIN_ATTEMPTS - user.getLoginAttempts();
            if (remaining > 0) {
                System.out.println("❌ Incorrect password. " + remaining + " attempt(s) remaining.");
            } else {
                user.lockLogin();
                System.out.println("🔒 Too many failed attempts. Account locked.");
            }
            DatabaseService.updateUserLockStatus(user);
            return null;
        }
    }
}