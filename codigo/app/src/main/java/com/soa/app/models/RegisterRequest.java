package com.soa.app.models;

public class RegisterRequest {
    private String env;
    private String name;
    private String lastname;
    private int dni;
    private String email;
    private String password;
    private int commission;
    private int group;

    public RegisterRequest() {
    }

    // getters
    public String getEnv() {
        return env;
    }
    public String getName() {
        return name;
    }
    public String getLastname() {
        return lastname;
    }
    public int getDni() {
        return dni;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public int getCommission() {
        return commission;
    }
    public int getGroup() {
        return group;
    }

    // setters
    public void setEnv(String env) {
        this.env = env;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }
    public void setDni(int dni) {
        this.dni = dni;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setCommission(int commission) {
        this.commission = commission;
    }
    public void setGroup(int group) {
        this.group = group;
    }

}