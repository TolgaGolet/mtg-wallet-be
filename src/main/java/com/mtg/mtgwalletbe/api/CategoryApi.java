package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.api.response.CategoryCreateResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.CategoryServiceMapper;
import com.mtg.mtgwalletbe.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryApi {
    private final CategoryService categoryService;
    private final CategoryServiceMapper categoryServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<CategoryCreateResponse> create(@RequestBody @Validated CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/category/create").toUriString());
        return ResponseEntity.created(uri).body(categoryServiceMapper.toCategoryCreateResponse(categoryService.create(categoryCreateRequest)));
    }
}
