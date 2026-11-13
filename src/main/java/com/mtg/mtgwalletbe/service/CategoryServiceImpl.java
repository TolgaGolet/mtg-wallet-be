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
import com.mtg.mtgwalletbe.repository.CategoryRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.specification.CategorySpecification;
import com.mtg.mtgwalletbe.util.Names;
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

import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_CATEGORIES;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final CategoryServiceMapper mapper;
    private final UserService userService;

    @Override
    public CategoryDto create(CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException {
        TransactionType transactionType = TransactionType.of(categoryCreateRequest.getTransactionTypeValue());
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        if (repository.countByUserIdAndStatus(walletUserDto.getId(), Status.ACTIVE) >= MAX_CATEGORIES) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORIES_LIMIT_EXCEEDED);
        }
        if (nameTaken(walletUserDto.getId(), transactionType, categoryCreateRequest.getName(), -1L)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NAME_ALREADY_EXISTS);
        }
        Category parentCategory = null;
        if (categoryCreateRequest.getParentCategoryId() != null) {
            parentCategory = validateParent(categoryCreateRequest.getParentCategoryId(), null, transactionType);
            markAsParent(parentCategory);
        }
        CategoryDto categoryDtoToSave = CategoryDto.builder().name(categoryCreateRequest.getName())
                .transactionType(transactionType)
                .userId(walletUserDto.getId())
                .parentCategoryId(parentCategory != null ? parentCategory.getId() : null)
                .isParent(Boolean.FALSE).status(Status.ACTIVE).build();
        return mapper.toCategoryDto(repository.save(mapper.toCategoryEntity(categoryDtoToSave)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> search(CategorySearchRequest request, Status status, Pageable pageable) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        request.setUserId(walletUserDto.getId());
        Specification<Category> specification = CategorySpecification.search(request, status);
        Page<Category> categories = repository.findAll(specification, pageable);
        return categories.map(mapper::toCategoryResponse);
    }

    @Override
    public CategoryDto update(CategoryUpdateRequest categoryUpdateRequest, Long id) throws MtgWalletGenericException {
        Category category = owned(repository.findByIdAndStatus(id, Status.ACTIVE));
        if (nameTaken(category.getUser().getId(), category.getTransactionType(), categoryUpdateRequest.getName(), id)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NAME_ALREADY_EXISTS);
        }
        Long newParentId = categoryUpdateRequest.getParentCategoryId();
        if (newParentId != null && Boolean.TRUE.equals(category.getIsParent())) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PARENT_CATEGORY_CANT_HAVE_PARENT_CATEGORY);
        }
        Category parentCategory = newParentId != null ? validateParent(newParentId, id, category.getTransactionType()) : null;
        Category oldParent = category.getParentCategory();

        category.setName(categoryUpdateRequest.getName());
        category.setParentCategory(parentCategory);
        Category saved = repository.saveAndFlush(category);

        if (parentCategory != null) {
            markAsParent(parentCategory);
        }
        if (oldParent != null && (parentCategory == null || !Objects.equals(oldParent.getId(), parentCategory.getId()))) {
            refreshIsParent(oldParent);
        }
        return mapper.toCategoryDto(saved);
    }

    /**
     * Loads and checks a would-be parent for a category of the given type. childId is null on create.
     */
    private Category validateParent(Long parentId, Long childId, TransactionType childType) throws MtgWalletGenericException {
        if (Objects.equals(parentId, childId)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_CANT_BE_ITS_OWN_PARENT);
        }
        Category parent = owned(repository.findByIdAndStatus(parentId, Status.ACTIVE));
        if (parent.getParentCategory() != null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_WITH_PARENT_CATEGORY_NOT_ALLOWED_AS_PARENT_CATEGORY);
        }
        if (parent.getTransactionType() != childType) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PARENT_CATEGORY_TRANSACTION_TYPE_MISMATCH);
        }
        return parent;
    }

    private void markAsParent(Category parent) {
        if (!Boolean.TRUE.equals(parent.getIsParent())) {
            parent.setIsParent(Boolean.TRUE);
            repository.save(parent);
        }
    }

    /**
     * Re-derives the stored isParent flag from the actual active children.
     */
    private void refreshIsParent(Category category) {
        boolean hasActiveChildren = repository.existsByParentCategoryIdAndStatus(category.getId(), Status.ACTIVE);
        if (!Objects.equals(category.getIsParent(), hasActiveChildren)) {
            category.setIsParent(hasActiveChildren);
            repository.save(category);
        }
    }

    @Override
    public void delete(Long id) throws MtgWalletGenericException {
        Category category = owned(repository.findByIdAndStatus(id, Status.ACTIVE));
        if (repository.existsByParentCategoryIdAndStatus(id, Status.ACTIVE)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_HAS_ACTIVE_SUBCATEGORIES);
        }
        category.setName("Deleted Category");
        category.setStatus(Status.DELETED);
        repository.saveAndFlush(category);
        if (category.getParentCategory() != null) {
            refreshIsParent(category.getParentCategory());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategory(Long id) throws MtgWalletGenericException {
        return mapper.toCategoryDto(owned(repository.findById(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getActiveCategory(Long id) throws MtgWalletGenericException {
        return mapper.toCategoryDto(owned(repository.findByIdAndStatus(id, Status.ACTIVE)));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryCreateScreenEnumResponse getCategoryCreateScreenEnums() {
        CategoryCreateScreenEnumResponse response = new CategoryCreateScreenEnumResponse();
        Page<CategoryResponse> parentCategoriesPage = search(CategorySearchRequest.builder().childrenOnly(true).build(), Status.ACTIVE, PageRequest.of(0, MAX_CATEGORIES, Sort.by("name").ascending()));
        List<CategorySelectResponse> parentCategories = parentCategoriesPage.getContent().stream().map(mapper::toCategorySelectResponse).toList();
        response.setParentCategoryList(parentCategories);
        return response;
    }

    /**
     * Case-insensitive (Turkish rules) name check among the user's ACTIVE categories of the same type; loads names only.
     */
    private boolean nameTaken(Long userId, TransactionType type, String name, Long excludeId) {
        return Names.containsName(repository.findNamesByUserIdAndStatusAndType(userId, Status.ACTIVE, type, excludeId), name);
    }

    /**
     * Unwraps a lookup: CATEGORY_NOT_FOUND when empty, and the category must belong to the current user.
     */
    private Category owned(Optional<Category> lookup) throws MtgWalletGenericException {
        Category category = lookup.orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND));
        userService.validateUserIdIfItsTheCurrentUser(category.getUser().getId());
        return category;
    }
}
