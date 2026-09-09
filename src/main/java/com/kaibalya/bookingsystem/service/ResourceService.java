package com.kaibalya.bookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kaibalya.bookingsystem.dto.request.ResourceRequest;
import com.kaibalya.bookingsystem.dto.response.ResourceResponse;
import com.kaibalya.bookingsystem.exception.ResourceNotFoundException;
import com.kaibalya.bookingsystem.repository.ResourceRepository;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        com.kaibalya.bookingsystem.entity.Resource resource = com.kaibalya.bookingsystem.entity.Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .active(request.getActive() == null || request.getActive())
                .build();

        return toResponse(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> findAll(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse findById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        com.kaibalya.bookingsystem.entity.Resource resource = getEntityOrThrow(id);
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setLocation(request.getLocation());
        resource.setCapacity(request.getCapacity());
        if (request.getActive() != null) {
            resource.setActive(request.getActive());
        }
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        com.kaibalya.bookingsystem.entity.Resource resource = getEntityOrThrow(id);
        resourceRepository.delete(resource);
    }

    public com.kaibalya.bookingsystem.entity.Resource getEntityOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private ResourceResponse toResponse(com.kaibalya.bookingsystem.entity.Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .location(resource.getLocation())
                .capacity(resource.getCapacity())
                .active(resource.isActive())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
