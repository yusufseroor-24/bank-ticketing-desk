package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository  extends JpaRepository<Category, Long> {
    List<Category> findByVisibilityToCustomersTrue();
}
