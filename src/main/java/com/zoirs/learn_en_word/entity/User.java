package com.zoirs.learn_en_word.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    private String firebaseToken;

    private Integer timezoneOffset;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "integer[]")
    private Set<Integer> knownWords;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "integer[]")
    private Set<Integer> learningWords;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "integer[]")
    private Set<Integer> newWords;

    @Column
    private SubscriptionPaymentType paymentType;

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
}
