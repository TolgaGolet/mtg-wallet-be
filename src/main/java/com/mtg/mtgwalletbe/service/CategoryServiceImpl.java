package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.CategoryServiceMapper;
import com.mtg.mtgwalletbe.repository.CategoryRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final CategoryServiceMapper mapper;
    private final UserService userService;

    @Override
    public CategoryDto create(CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException {
        TransactionType transactionType = TransactionType.of(categoryCreateRequest.getTransactionTypeKey());
        WalletUserDto walletUserDto = null;
        CategoryDto parentCategoryDto = null;
        if (categoryCreateRequest.getUsername() != null) {
            walletUserDto = userService.getUser(categoryCreateRequest.getUsername());
        }
        if (categoryCreateRequest.getParentCategoryId() != null) {
            parentCategoryDto = getCategory(categoryCreateRequest.getParentCategoryId());
        }
        if (categoryCreateRequest.getParentCategoryId() != null && parentCategoryDto == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage());
        }
        CategoryDto categoryDtoToSave = CategoryDto.builder().name(categoryCreateRequest.getName())
                .transactionType(transactionType)
                .user(walletUserDto).parentCategory(parentCategoryDto).build();
        return mapper.toCategoryDto(repository.save(mapper.toCategoryEntity(categoryDtoToSave)));
    }

    @Override
    public CategoryDto getCategory(Long id) {
        Optional<Category> category = repository.findById(id);
        return category.map(mapper::toCategoryDto).orElse(null);
    }
}
