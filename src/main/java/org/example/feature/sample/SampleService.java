package org.example.feature.sample;

import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.feature.sample.dto.SampleCreateRequest;
import org.example.feature.sample.dto.SampleResponse;
import org.example.feature.sample.model.SampleItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SampleService {

    private final Map<Long, SampleItem> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public SampleService() {
        // Sample seed data
        storage.put(1L, SampleItem.builder()
                .id(1L)
                .name("Item Alpha")
                .description("Mô tả dữ liệu mẫu Alpha")
                .createdAt(Instant.now())
                .build());
    }

    public List<SampleResponse> getAll() {
        List<SampleResponse> list = new ArrayList<>();
        storage.values().forEach(item -> list.add(toResponse(item)));
        return list;
    }

    public SampleResponse getById(Long id) {
        SampleItem item = storage.get(id);
        if (item == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy item với ID: " + id);
        }
        return toResponse(item);
    }

    public SampleResponse create(SampleCreateRequest request) {
        long newId = idGenerator.incrementAndGet();
        SampleItem item = SampleItem.builder()
                .id(newId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .createdAt(Instant.now())
                .build();
        storage.put(newId, item);
        return toResponse(item);
    }

    private SampleResponse toResponse(SampleItem item) {
        return SampleResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .createdAt(item.getCreatedAt())
                .build();
    }
}

