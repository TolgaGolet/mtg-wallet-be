package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException;

    public List<CategoryDto> findAllByCurrentUser();

    CategoryDto getCategory(Long id) throws MtgWalletGenericException;
}
