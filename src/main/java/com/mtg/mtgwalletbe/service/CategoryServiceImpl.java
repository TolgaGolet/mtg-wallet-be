package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.api.request.CategorySearchRequest;
import com.mtg.mtgwalletbe.api.request.CategoryUpdateRequest;
import com.mtg.mtgwalletbe.api.response.CategoryCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.CategorySelectResponse;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.CategoryServiceMapper;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.CategoryRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.specification.CategorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final CategoryServiceMapper mapper;
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;
    public static final int MAX_ALLOWED_CATEGORY_COUNT = 50;

    @Override
    public CategoryDto create(CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException {
        TransactionType transactionType = TransactionType.of(categoryCreateRequest.getTransactionTypeValue());
        CategoryDto parentCategoryDto = null;
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        List<CategoryDto> userCategories = findAllByCurrentUserByStatus(Status.ACTIVE);
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
                .userId(walletUserDto.getId()).parentCategoryId(parentCategoryDto.getId()).parentCategoryName(parentCategoryDto.getName()).status(Status.ACTIVE).build();
        return mapper.toCategoryDto(repository.save(mapper.toCategoryEntity(categoryDtoToSave)));
    }

    @Override
    public Page<CategoryResponse> search(CategorySearchRequest request, Status status, Pageable pageable) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        request.setUserId(walletUserDto.getId());
        Specification<Category> specification = CategorySpecification.search(request, status);
        Page<Category> categories = repository.findAll(specification, pageable);
        return categories.map(mapper::toCategoryResponse);
    }

    @Override
    public CategoryDto update(CategoryUpdateRequest categoryUpdateRequest, Long id) throws MtgWalletGenericException {
        Category category = repository.findByIdAndStatus(id, Status.ACTIVE).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage()));
        userService.validateUsernameIfItsTheCurrentUser(category.getUser().getUsername());
        List<CategoryDto> userCategories = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userCategories.stream().anyMatch(existingCategory -> !Objects.equals(existingCategory.getId(), id) && existingCategory.getName().equals(categoryUpdateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NAME_ALREADY_EXISTS.getMessage());
        }
        category.setName(categoryUpdateRequest.getName());
        category.setTransactionType(TransactionType.of(categoryUpdateRequest.getTransactionTypeValue()));
        Category parentCategory = categoryUpdateRequest.getParentCategoryId() != null ? mapper.toCategoryEntity(getCategory(categoryUpdateRequest.getParentCategoryId())) : null;
        category.setParentCategory(parentCategory);
        return mapper.toCategoryDto(repository.save(category));
    }

    @Override
    public void delete(Long id) throws MtgWalletGenericException {
        Category category = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage()));
        userService.validateUsernameIfItsTheCurrentUser(category.getUser().getUsername());
        category.setName("Deleted Category");
        category.setStatus(Status.DELETED);
        repository.save(category);
    }

    @Override
    public List<CategoryDto> findAllByCurrentUserByStatus(Status status) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        return mapper.toCategoryDtoList(repository.findAllByUserAndStatus(userServiceMapper.toWalletUserEntity(walletUserDto), status));
    }

    @Override
    public CategoryDto getCategory(Long id) throws MtgWalletGenericException {
        Optional<Category> category = repository.findById(id);
        if (category.isPresent()) {
            userService.validateUsernameIfItsTheCurrentUser(category.get().getUser().getUsername());
        }
        return category.map(mapper::toCategoryDto).orElse(null);
    }

    @Override
    public CategoryCreateScreenEnumResponse getCategoryCreateScreenEnums() {
        CategoryCreateScreenEnumResponse response = new CategoryCreateScreenEnumResponse();
        Page<CategoryResponse> parentCategoriesPage = search(new CategorySearchRequest(), Status.ACTIVE, PageRequest.of(0, MAX_ALLOWED_CATEGORY_COUNT, Sort.by("name").ascending()));
        List<CategorySelectResponse> parentCategories = parentCategoriesPage.getContent().stream().map(mapper::toCategorySelectResponse).toList();
        response.setParentCategoryList(parentCategories);
        return response;
    }
}
