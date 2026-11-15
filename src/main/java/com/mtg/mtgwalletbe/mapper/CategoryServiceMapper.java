package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.CategorySelectResponse;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MtgMapperConfig.class)
public interface CategoryServiceMapper {
    String WITHOUT_USER = "toCategoryDtoWithoutUser";

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "parentCategoryId", target = "parentCategory", qualifiedByName = "mapParentCategoryIdToParentCategory_id")
    Category toCategoryEntity(CategoryDto categoryDto);

    @Named("mapParentCategoryIdToParentCategory_id")
    default Category mapParentCategoryIdToParentCategory_id(Long parentCategoryId) {
        if (parentCategoryId == null) {
            return null;
        }
        Category parentCategory = new Category();
        parentCategory.setId(parentCategoryId);
        return parentCategory;
    }

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "parentCategory.name", target = "parentCategoryName")
    CategoryDto toCategoryDto(Category categoryEntity);

    /**
     * A category nested in another DTO (a payee's): its owner is the outer DTO's, so the category's lazy user isn't
     * loaded for it (no N+1 in transaction search). Selected only by name.
     */
    @Named(WITHOUT_USER)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "parentCategory.name", target = "parentCategoryName")
    CategoryDto toCategoryDtoWithoutUser(Category categoryEntity);

    CategoryResponse toCategoryResponse(CategoryDto categoryDto);

    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "parentCategory.name", target = "parentCategoryName")
    CategoryResponse toCategoryResponse(Category category);

    @Mapping(source = "id", target = "value")
    @Mapping(source = "name", target = "label")
    CategorySelectResponse toCategorySelectResponse(CategoryResponse category);
}
