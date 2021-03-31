package pt.tecnico.bicloin.hub.domain;

public class User {

    private String username;
    private String name;
    private String phone;

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
