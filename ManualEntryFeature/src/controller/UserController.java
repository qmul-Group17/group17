package controller;

import com.google.gson.reflect.TypeToken;
import model.User;
import util.JsonUtil;
import util.HashUtil;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UserController {
    private static final File USER_FILE = new File("PersonalFinanceTracker/users.json");
    private List<User> userList;

    public UserController() {
        userList = JsonUtil.readJson(USER_FILE, new TypeToken<List<User>>(){}.getType());
        if (userList == null) {
        userList = new ArrayList<>();
        saveUsers(); 
        }
    }

   public boolean register(String username, String password) {
    if (getUserByUsername(username) != null) return false;

    String hashed = HashUtil.hash(password);
    User user = new User(username, hashed);
    userList.add(user);
    saveUsers();

    // ✅ Ensure the directory exists before creating file
    File dir = new File("PersonalFinanceTracker");
    if (!dir.exists()) {
        dir.mkdirs(); // create directory if missing
    }

    File transactionFile = new File(dir, "transactions_" + username + ".json");
    if (!transactionFile.exists()) {
        System.out.println("[DEBUG] Creating file: " + transactionFile.getAbsolutePath());
        JsonUtil.writeJson(transactionFile, new ArrayList<>()); // write empty list
    }

    return true;
}


    public User login(String username, String password) {
        User user = getUserByUsername(username);
        if (user != null && user.getPasswordHash().equals(HashUtil.hash(password))) {
            return user;
        }
        return null;
    }

    private User getUserByUsername(String username) {
        return userList.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    private void saveUsers() {
        JsonUtil.writeJson(USER_FILE, userList);
    }
}

