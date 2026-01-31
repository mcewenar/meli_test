package com.hackerrank.sample.mapper;

import com.hackerrank.sample.dto.ModelRequest;
import com.hackerrank.sample.dto.ModelResponse;
import com.hackerrank.sample.model.Model;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ModelMapper {
    public Model toEntity(ModelRequest request) {
        if (request == null) {
            return null;
        }
        return new Model(request.getId(), request.getName());
    }

    public ModelResponse toResponse(Model model) {
        if (model == null) {
            return null;
        }
        return new ModelResponse(model.getId(), model.getName());
    }

    public List<ModelResponse> toResponseList(List<Model> models) {
        return models.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
