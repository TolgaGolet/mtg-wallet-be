package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.CategoryServiceMapper;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.CategoryRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final CategoryServiceMapper mapper;
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;

    @Override
    public CategoryDto create(CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException {
        TransactionType transactionType = TransactionType.of(categoryCreateRequest.getTransactionTypeKey());
        CategoryDto parentCategoryDto = null;
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUser();
        List<CategoryDto> userCategories = findAllByCurrentUser();
        if (userCategories.stream().anyMatch(category -> category.getName().equals(categoryCreateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NAME_ALREADY_EXISTS.getMessage());
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
    public List<CategoryDto> findAllByCurrentUser() {
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUser();
        return mapper.toCategoryDtoList(repository.findAllByUser(userServiceMapper.toWalletUserEntity(walletUserDto)));
    }

    @Override
    public CategoryDto getCategory(Long id) throws MtgWalletGenericException {
        Optional<Category> category = repository.findById(id);
        if (category.isPresent()) {
            userService.validateUsernameIfItsTheCurrentUser(category.get().getUser().getUsername());
        }
        return category.map(mapper::toCategoryDto).orElse(null);
    }
}
