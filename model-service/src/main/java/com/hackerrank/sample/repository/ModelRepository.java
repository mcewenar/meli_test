package com.hackerrank.sample.repository;

import com.hackerrank.sample.model.Model;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("modelRepository")
public interface ModelRepository extends JpaRepository<Model, Long> {
    @Transactional
    void deleteById(Long id);
}
