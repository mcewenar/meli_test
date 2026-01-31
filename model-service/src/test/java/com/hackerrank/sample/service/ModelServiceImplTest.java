package com.hackerrank.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackerrank.sample.exception.BadResourceRequestException;
import com.hackerrank.sample.exception.NoSuchResourceFoundException;
import com.hackerrank.sample.model.Model;
import com.hackerrank.sample.port.ModelRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class ModelServiceImplTest {
    @Mock
    private ModelRepositoryPort modelRepository;

    @InjectMocks
    private ModelServiceImpl modelService;

    private Model model;

    @Before
    public void setUp() {
        model = new Model(1L, "Item name");
    }

    @Test
    public void createModelRejectsNullModel() {
        try {
            modelService.createModel(null);
            fail("Expected BadResourceRequestException");
        } catch (BadResourceRequestException ex) {
            assertEquals("id is required.", ex.getMessage());
            verify(modelRepository, never()).save(any(Model.class));
        }
    }

    @Test
    public void createModelRejectsNullId() {
        Model invalid = new Model(null, "Item name");

        try {
            modelService.createModel(invalid);
            fail("Expected BadResourceRequestException");
        } catch (BadResourceRequestException ex) {
            assertEquals("id is required.", ex.getMessage());
            verify(modelRepository, never()).save(any(Model.class));
        }
    }

    @Test
    public void createModelRejectsDuplicate() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(model));

        try {
            modelService.createModel(model);
            fail("Expected BadResourceRequestException");
        } catch (BadResourceRequestException ex) {
            assertEquals("Model with same id exists.", ex.getMessage());
            verify(modelRepository, never()).save(any(Model.class));
        }
    }

    @Test
    public void createModelSaves() {
        when(modelRepository.findById(1L)).thenReturn(Optional.empty());

        modelService.createModel(model);

        verify(modelRepository).save(model);
    }

    @Test
    public void deleteModelByIdRejectsNull() {
        try {
            modelService.deleteModelById(null);
            fail("Expected BadResourceRequestException");
        } catch (BadResourceRequestException ex) {
            assertEquals("id is required.", ex.getMessage());
        }
    }

    @Test
    public void deleteModelByIdRejectsMissing() {
        when(modelRepository.existsById(1L)).thenReturn(false);

        try {
            modelService.deleteModelById(1L);
            fail("Expected NoSuchResourceFoundException");
        } catch (NoSuchResourceFoundException ex) {
            assertEquals("No model with given id found.", ex.getMessage());
            verify(modelRepository, never()).deleteById(1L);
        }
    }

    @Test
    public void deleteModelByIdDeletes() {
        when(modelRepository.existsById(1L)).thenReturn(true);

        modelService.deleteModelById(1L);

        verify(modelRepository).deleteById(1L);
    }

    @Test
    public void getModelByIdRejectsNull() {
        try {
            modelService.getModelById(null);
            fail("Expected BadResourceRequestException");
        } catch (BadResourceRequestException ex) {
            assertEquals("id is required.", ex.getMessage());
        }
    }

    @Test
    public void getModelByIdRejectsMissing() {
        when(modelRepository.findById(1L)).thenReturn(Optional.empty());

        try {
            modelService.getModelById(1L);
            fail("Expected NoSuchResourceFoundException");
        } catch (NoSuchResourceFoundException ex) {
            assertEquals("No model with given id found.", ex.getMessage());
        }
    }

    @Test
    public void getModelByIdReturnsModel() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(model));

        Model found = modelService.getModelById(1L);

        assertSame(model, found);
    }

    @Test
    public void getAllModelsReturnsList() {
        List<Model> models = List.of(model);
        when(modelRepository.findAll()).thenReturn(models);

        List<Model> found = modelService.getAllModels();

        assertEquals(models, found);
    }

    @Test
    public void deleteAllModelsDeletesInBatch() {
        modelService.deleteAllModels();

        verify(modelRepository).deleteAllInBatch();
    }

    @Test
    public void getModelsPageReturnsPage() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Model> page = new PageImpl<>(List.of(model), pageable, 1);
        when(modelRepository.findAll(pageable)).thenReturn(page);

        Page<Model> result = modelService.getModelsPage(pageable);

        assertEquals(page, result);
    }
}
