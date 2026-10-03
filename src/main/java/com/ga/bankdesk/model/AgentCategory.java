package com.ga.bankdesk.model;

//join table to describe many-to-many relationship between agents and category
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "agent_category")
public class AgentCategory {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
