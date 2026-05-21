package io.lumen.data.repository;

import jakarta.persistence.*;

@Entity
@Table(name = "test_user")
public class TestUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int age;

    public TestUser() {}

    public TestUser(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public Long getId()     { return id; }
    public String getName() { return name; }
    public int getAge()     { return age; }
}
