package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.CategorySelectResponse;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryServiceMapper {

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
    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "parentCategory.name", target = "parentCategoryName")
    CategoryDto toCategoryDto(Category categoryEntity);

    CategoryResponse toCategoryResponse(CategoryDto categoryDto);

    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "parentCategory.name", target = "parentCategoryName")
    CategoryResponse toCategoryResponse(Category category);

    List<CategoryDto> toCategoryDtoList(List<Category> categoryEntities);

    @Mapping(source = "id", target = "value")
    @Mapping(source = "name", target = "label")
    CategorySelectResponse toCategorySelectResponse(CategoryResponse category);
}
