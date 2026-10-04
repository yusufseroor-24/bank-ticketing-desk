package com.ga.bankdesk.model;


import com.ga.bankdesk.enums.TicketPriority;
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
    private boolean visibilityToCustomers;

    @Column(nullable = false)
    private boolean active = true;

}
