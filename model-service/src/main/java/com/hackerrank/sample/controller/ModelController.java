package com.hackerrank.sample.controller;

import com.hackerrank.sample.dto.ModelRequest;
import com.hackerrank.sample.dto.ModelResponse;
import com.hackerrank.sample.mapper.ModelMapper;
import com.hackerrank.sample.service.ModelService;
import com.hackerrank.sample.validation.ValidationGroups;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
public class ModelController {
    private final ModelService modelService;
    private final ModelMapper modelMapper;

    public ModelController(ModelService modelService, ModelMapper modelMapper) {
        this.modelService = modelService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/")
    @ResponseBody
    @Operation(summary = "Health check", description = "Returns a simple health message.")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("Default Java 21 Project Home Page");
    }

    @PostMapping(value = "/model", consumes = "application/json")
    @Operation(summary = "Create a model", description = "Creates a new item model.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation or request error")
    public ResponseEntity<ModelResponse> createNewModel(@RequestBody @Validated(ValidationGroups.Create.class) ModelRequest request) {
        ModelResponse response = modelMapper.toResponse(
                modelService.createModel(modelMapper.toEntity(request))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/erase")
    @Operation(summary = "Delete all models", description = "Deletes all stored models.")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<Map<String, Object>> deleteAllModels() {
        modelService.deleteAllModels();
        return ResponseEntity.ok(Map.of("message", "All models deleted."));
    }

    @DeleteMapping("/model/{id}")
    @Operation(summary = "Delete model by id", description = "Deletes a model by its id.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Model not found")
    public ResponseEntity<Map<String, Object>> deleteModelById(@PathVariable Long id) {
        modelService.deleteModelById(id);
        return ResponseEntity.ok(Map.of("message", "Model deleted.", "id", id));
    }

    @GetMapping("/model")
    @Operation(summary = "List all models", description = "Returns all stored models.")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<List<ModelResponse>> getAllModels() {
        return ResponseEntity.ok(modelMapper.toResponseList(modelService.getAllModels()));
    }

    @GetMapping("/model/{id}")
    @Operation(summary = "Get model by id", description = "Returns a model by its id.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Model not found")
    public ResponseEntity<ModelResponse> getModelById(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.toResponse(modelService.getModelById(id)));
    }

    @GetMapping("/model/page")
    @Operation(summary = "List models with pagination", description = "Returns a paged list of models.")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<Page<ModelResponse>> getModelsPage(@ParameterObject Pageable pageable) {
        Page<ModelResponse> page = modelService.getModelsPage(pageable)
                .map(modelMapper::toResponse);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/debug/thread")
    @Operation(summary = "Debug thread info", description = "Returns information about the current request thread.")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<Map<String, Object>> getThreadInfo() {
        Thread currentThread = Thread.currentThread();
        return ResponseEntity.ok(Map.of(
                "name", currentThread.getName(),
                "id", currentThread.threadId(),
                "state", currentThread.getState().toString(),
                "virtual", currentThread.isVirtual()
        ));
    }
}
