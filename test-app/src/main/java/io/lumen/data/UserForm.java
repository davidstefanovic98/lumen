package io.lumen.data;

public class UserForm {
    private String username;
    private int age;

    public UserForm() {}

    public UserForm(String username, int age) {
        this.username = username;
        this.age = age;
    }

    public String getUsername() {
        return username;
    }

    public int getAge() {
        return age;
    }
}
