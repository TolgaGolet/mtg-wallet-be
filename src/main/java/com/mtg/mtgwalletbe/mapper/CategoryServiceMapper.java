package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.CategoryCreateResponse;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryServiceMapper {
    Category toCategoryEntity(CategoryDto categoryDto);

    CategoryDto toCategoryDto(Category categoryEntity);

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "id", target = "categoryId")
    CategoryCreateResponse toCategoryCreateResponse(CategoryDto categoryDto);

    List<CategoryDto> toCategoryDtoList(List<Category> categoryEntities);
}
