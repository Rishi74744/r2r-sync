package com.r2r.sync.service.impl;

import com.r2r.sync.dto.RecordDTO;
import com.r2r.sync.entity.RecordEntity;
import com.r2r.sync.repository.RecordRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecordService {
    private final RecordRepository recordRepository;

    public RecordService(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public RecordDTO createRecord(RecordDTO dto) {
        System.out.println("Creating record: " + dto.getId());
        RecordEntity entity = RecordEntity.builder()
                .id(dto.getId())
                .type(dto.getType())
                .data(dto.getData())
                .version(dto.getVersion())
                .updatedAt(System.currentTimeMillis())
                .build();
        recordRepository.save(entity);
        return mapToDTO(entity);
    }

    public Optional<RecordDTO> getRecord(String id) {
        System.out.println("Fetching record: " + id);
        return recordRepository.findById(id).map(this::mapToDTO);
    }

    public List<RecordDTO> getAllRecords() {
        System.out.println("Fetching all records");
        return recordRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Optional<RecordDTO> updateRecord(String id, RecordDTO dto) {
        System.out.println("Updating record: " + id);
        return recordRepository.findById(id).map(entity -> {
            entity.setType(dto.getType());
            entity.setData(dto.getData());
            entity.setVersion(dto.getVersion());
            entity.setUpdatedAt(System.currentTimeMillis());
            recordRepository.save(entity);
            return mapToDTO(entity);
        });
    }

    public boolean deleteRecord(String id) {
        System.out.println("Deleting record: " + id);
        if (recordRepository.findById(id).isPresent()) {
            recordRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private RecordDTO mapToDTO(RecordEntity entity) {
        return RecordDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .data(entity.getData())
                .version(entity.getVersion())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
