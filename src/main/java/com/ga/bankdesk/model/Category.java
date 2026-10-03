package com.ga.bankdesk.model;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int slaHours;

    @Column(nullable = false)
    private boolean visibilityToCustomers;

    @Column(nullable = false)
    private boolean active = true;
}
