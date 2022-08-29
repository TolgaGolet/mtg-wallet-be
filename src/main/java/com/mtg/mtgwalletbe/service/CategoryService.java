package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;

public interface CategoryService {
    CategoryDto create(CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException;

    CategoryDto getCategory(Long id);
}
