package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository  extends JpaRepository<Category, Long> {
    List<Category> findByVisibilityToCustomersTrue();
    Optional<Category> findByName(String name);
}
