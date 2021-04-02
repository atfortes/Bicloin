package pt.tecnico.bicloin.hub.domain;

public class User {

    private final String username;
    private final String name;
    private final String phone;

    public User(String username, String name, String phone) {
        this.username = username;
        this.name = name;
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }
}
