package com.iuh.ChatAppValo.jwt.request;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class SignupRequest implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullname;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Email không được để trống")
    private String email;

    public SignupRequest(@NotBlank(message = "Username không được để trống") String username,
                         @NotBlank(message = "Họ tên không được để trống") String fullname,
                         @NotBlank(message = "Mật khẩu không được để trống") String password,
                         @NotBlank(message = "Email không được để trống") String email) {
        super();
        this.username = username;
        this.fullname = fullname;
        this.password = password;
        this.email = email;
    }

    public SignupRequest() {
        super();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullname() {return fullname;}

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "SignupRequest{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
