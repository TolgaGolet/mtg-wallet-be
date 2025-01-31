package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.api.request.CategorySearchRequest;
import com.mtg.mtgwalletbe.api.request.CategoryUpdateRequest;
import com.mtg.mtgwalletbe.api.response.CategoryCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryDto create(CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException;

    Page<CategoryResponse> search(CategorySearchRequest request, Status status, Pageable pageable);

    CategoryDto update(CategoryUpdateRequest categoryUpdateRequest, Long id) throws MtgWalletGenericException;

    void delete(Long id) throws MtgWalletGenericException;

    public List<CategoryDto> findAllByCurrentUserByStatus(Status status);

    CategoryDto getCategory(Long id) throws MtgWalletGenericException;

    CategoryCreateScreenEnumResponse getCategoryCreateScreenEnums();
}
