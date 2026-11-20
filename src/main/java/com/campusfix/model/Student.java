package com.campusfix.model;

import java.time.LocalDateTime;
import java.util.Set;

/** A student who reports campus problems. Demonstrates inheritance and method overriding. */
public class Student extends User {

    public Student(int userId, String name, String email, String passwordHash,
              String phoneNumber, LocalDateTime createdAt) {
        super(userId, name, email, passwordHash, phoneNumber, createdAt);
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public Set<Permission> getPermissions() {
        return Permission.submitterSet();
    }
}
