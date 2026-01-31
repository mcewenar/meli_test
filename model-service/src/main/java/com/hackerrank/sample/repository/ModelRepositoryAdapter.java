package com.hackerrank.sample.repository;

import com.hackerrank.sample.model.Model;
import com.hackerrank.sample.port.ModelRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class ModelRepositoryAdapter implements ModelRepositoryPort {
    private final ModelRepository modelRepository;

    public ModelRepositoryAdapter(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    @Override
    public void deleteAllInBatch() {
        modelRepository.deleteAllInBatch();
    }

    @Override
    public void deleteById(Long id) {
        modelRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return modelRepository.existsById(id);
    }

    @Override
    public Optional<Model> findById(Long id) {
        return modelRepository.findById(id);
    }

    @Override
    public Model save(Model model) {
        return modelRepository.save(model);
    }

    @Override
    public List<Model> findAll() {
        return modelRepository.findAll();
    }

    @Override
    public Page<Model> findAll(Pageable pageable) {
        return modelRepository.findAll(pageable);
    }
}
