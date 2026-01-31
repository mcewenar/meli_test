package com.hackerrank.sample.port;

import com.hackerrank.sample.model.Model;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ModelRepositoryPort {
    void deleteAllInBatch();

    void deleteById(Long id);

    boolean existsById(Long id);

    Optional<Model> findById(Long id);

    Model save(Model model);

    List<Model> findAll();

    Page<Model> findAll(Pageable pageable);
}
