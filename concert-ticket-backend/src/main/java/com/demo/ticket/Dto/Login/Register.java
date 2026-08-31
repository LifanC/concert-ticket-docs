package com.demo.ticket.Dto.Login;

public class Register {

    private String account;
    private String name;
    private String email;
    private String phone;
    private String password;

    public Register() {
    }

    public Register(String account, String name, String email, String phone, String password) {
        this.account = account;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
