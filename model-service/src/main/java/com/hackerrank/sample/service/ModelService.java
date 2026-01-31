package com.hackerrank.sample.service;

import com.hackerrank.sample.model.Model;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ModelService {
    void deleteAllModels();
    void deleteModelById(Long id);

    Model createModel(Model model);

    Model getModelById(Long id);

    List<Model> getAllModels();

    Page<Model> getModelsPage(Pageable pageable);
}
