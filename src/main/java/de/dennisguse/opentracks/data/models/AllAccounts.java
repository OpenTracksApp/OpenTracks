package de.dennisguse.opentracks.data.models;

import java.util.ArrayList;
import java.util.List;

public class AllAccounts {
    private static final List<Account> accounts = new ArrayList<>();

    public synchronized static List<Account> getAllAccounts() {
        return accounts;
    }

    public synchronized static List<Account> registerAccount(Account account) {
        accounts.add(account);
        return accounts;
    }

    public synchronized static List<Account> modifyAccount(String username, Account newInfo) {
        accounts.forEach(account -> {
            if (account.getUsername().equals(username)) {
                account.setEmail(newInfo.getEmail());
                account.setPassword(newInfo.getPassword());
            }
        });
        return accounts;
    }

    public synchronized static List<Account> deleteAccount(Account account) {
        accounts.removeIf(acc -> acc.getUsername().equals(account.getUsername()));
        return accounts;
    }
}
