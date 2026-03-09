package com.fallguys.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /* 이용약관 동의 여부 */
    @Column(nullable = false)
    private Boolean termsAgreed;

    /* 개인정보처리방침 동의 여부 */
    @Column(nullable = false)
    private Boolean privacyAgreed;

    /* 이메일 인증 값 */
    @Column(nullable = false)
    private Boolean emailVerified = false;

    /* 이메일 수신 동의 여부 */
    @Column(nullable = false)
    private Boolean emailEnabled = true;

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    @Builder
    public User(String email, String password, String name, String phone, Role role,
            Boolean termsAgreed, Boolean privacyAgreed) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.termsAgreed = termsAgreed;
        this.privacyAgreed = privacyAgreed;
        this.emailVerified = false;
        this.emailEnabled = true;
    }
}
