package com.hackerrank.sample.service;

import com.hackerrank.sample.exception.BadResourceRequestException;
import com.hackerrank.sample.exception.NoSuchResourceFoundException;
import com.hackerrank.sample.model.Model;
import com.hackerrank.sample.port.ModelRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service("modelService")
public class ModelServiceImpl implements ModelService {

    private final ModelRepositoryPort modelRepository;

    public ModelServiceImpl(ModelRepositoryPort modelRepository) {
        this.modelRepository = modelRepository;
    }

    @Override
    public void deleteAllModels() {
        modelRepository.deleteAllInBatch();
    }

    @Override
    public void deleteModelById(Long id) {
        if (id == null) {
            throw new BadResourceRequestException("id is required.");
        }

        if (!modelRepository.existsById(id)) {
            throw new NoSuchResourceFoundException("No model with given id found.");
        }

        modelRepository.deleteById(id);
    }

    @Override
    public Model createModel(Model model) {
        if (model == null || model.getId() == null) {
            throw new BadResourceRequestException("id is required.");
        }

        Optional<Model> existingModel = modelRepository.findById(model.getId());

        if (existingModel.isPresent()) {
            throw new BadResourceRequestException("Model with same id exists.");
        }

        return modelRepository.save(model);
    }

    @Override
    public Model getModelById(Long id) {
        if (id == null) {
            throw new BadResourceRequestException("id is required.");
        }

        Optional<Model> model = modelRepository.findById(id);

        if (model.isEmpty()) {
            throw new NoSuchResourceFoundException("No model with given id found.");
        }

        return model.get();
    }

    @Override
    public List<Model> getAllModels() {
        return modelRepository.findAll();
    }

    @Override
    public Page<Model> getModelsPage(Pageable pageable) {
        return modelRepository.findAll(pageable);
    }
}
