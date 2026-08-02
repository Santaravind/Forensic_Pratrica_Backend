package com.security.forecsic.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "register")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Register {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String organization;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private Long mobileNo;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String role ;   // e.g. USER, ADMIN
}