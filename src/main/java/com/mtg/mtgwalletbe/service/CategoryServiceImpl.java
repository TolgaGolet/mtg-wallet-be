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
    public static final int MAX_ALLOWED_CATEGORY_COUNT = 200;

    @Override
    public CategoryDto create(CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException {
        TransactionType transactionType = TransactionType.of(categoryCreateRequest.getTransactionTypeValue());
        CategoryDto parentCategoryDto = null;
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        List<CategoryDto> userCategories = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userCategories.size() >= MAX_ALLOWED_CATEGORY_COUNT) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORIES_LIMIT_EXCEEDED.getMessage());
        }
        if (userCategories.stream().anyMatch(category -> category.getTransactionType().equals(transactionType)
                && category.getName().equals(categoryCreateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NAME_ALREADY_EXISTS.getMessage());
        }
        if (categoryCreateRequest.getParentCategoryId() != null) {
            parentCategoryDto = getCategory(categoryCreateRequest.getParentCategoryId());
            if (parentCategoryDto == null) {
                throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage());
            }
            if (parentCategoryDto.getParentCategoryId() != null) {
                throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_WITH_PARENT_CATEGORY_NOT_ALLOWED_AS_PARENT_CATEGORY.getMessage());
            }
            setIsParentTrueById(categoryCreateRequest.getParentCategoryId());
        }
        CategoryDto categoryDtoToSave = CategoryDto.builder().name(categoryCreateRequest.getName())
                .transactionType(transactionType)
                .userId(walletUserDto.getId())
                .parentCategoryId(parentCategoryDto != null ? parentCategoryDto.getId() : null)
                .isParent(Boolean.FALSE).status(Status.ACTIVE).build();
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
        userService.validateUserIdIfItsTheCurrentUser(category.getUser().getId());
        List<CategoryDto> userCategories = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userCategories.stream().anyMatch(existingCategory -> !Objects.equals(existingCategory.getId(), id)
                && existingCategory.getTransactionType().equals(category.getTransactionType())
                && existingCategory.getName().equals(categoryUpdateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NAME_ALREADY_EXISTS.getMessage());
        }
        category.setName(categoryUpdateRequest.getName());
        Category parentCategory = null;
        if (categoryUpdateRequest.getParentCategoryId() != null && category.getIsParent()) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PARENT_CATEGORY_CANT_HAVE_PARENT_CATEGORY.getMessage());
        }
        if (categoryUpdateRequest.getParentCategoryId() != null) {
            parentCategory = mapper.toCategoryEntity(getCategory(categoryUpdateRequest.getParentCategoryId()));
            if (parentCategory.getParentCategory() != null) {
                throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_WITH_PARENT_CATEGORY_NOT_ALLOWED_AS_PARENT_CATEGORY.getMessage());
            }
            setIsParentTrueById(parentCategory.getId());
        } else if (category.getParentCategory() != null) {
            setIsParentFalseByIdIfNotParent(category.getParentCategory().getId(), id);
        }
        category.setParentCategory(parentCategory);
        return mapper.toCategoryDto(repository.save(category));
    }

    private void setIsParentTrueById(Long id) throws MtgWalletGenericException {
        Category category = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(category.getUser().getId());
        category.setIsParent(Boolean.TRUE);
        repository.save(category);
    }

    private void setIsParentFalseByIdIfNotParent(Long id, Long oldChildId) throws MtgWalletGenericException {
        Category category = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(category.getUser().getId());
        boolean isParent = findAllByCurrentUserByStatus(Status.ACTIVE)
                .stream()
                .anyMatch(category1 -> category1.getParentCategoryId() != null &&
                        !category1.getId().equals(id) &&
                        !category1.getId().equals(oldChildId) &&
                        category1.getParentCategoryId().equals(category.getId()));
        if (isParent) {
            return;
        }
        category.setIsParent(Boolean.FALSE);
        repository.save(category);
    }

    @Override
    public void delete(Long id) throws MtgWalletGenericException {
        Category category = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(category.getUser().getId());
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
            userService.validateUserIdIfItsTheCurrentUser(category.get().getUser().getId());
        }
        return category.map(mapper::toCategoryDto).orElse(null);
    }

    @Override
    public CategoryCreateScreenEnumResponse getCategoryCreateScreenEnums() {
        CategoryCreateScreenEnumResponse response = new CategoryCreateScreenEnumResponse();
        Page<CategoryResponse> parentCategoriesPage = search(CategorySearchRequest.builder().childrenOnly(true).build(), Status.ACTIVE, PageRequest.of(0, MAX_ALLOWED_CATEGORY_COUNT, Sort.by("name").ascending()));
        List<CategorySelectResponse> parentCategories = parentCategoriesPage.getContent().stream().map(mapper::toCategorySelectResponse).toList();
        response.setParentCategoryList(parentCategories);
        return response;
    }
}
