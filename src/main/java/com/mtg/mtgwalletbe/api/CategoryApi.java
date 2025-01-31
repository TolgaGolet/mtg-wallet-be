package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.api.request.CategorySearchRequest;
import com.mtg.mtgwalletbe.api.request.CategoryUpdateRequest;
import com.mtg.mtgwalletbe.api.response.CategoryCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.CategoryServiceMapper;
import com.mtg.mtgwalletbe.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.mtg.mtgwalletbe.security.SecurityParams.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryApi {
    private final CategoryService categoryService;
    private final CategoryServiceMapper categoryServiceMapper;

    @PostMapping("/create")
    public ResponseEntity<CategoryResponse> create(@RequestBody @Validated CategoryCreateRequest categoryCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(categoryServiceMapper.toCategoryResponse(categoryService.create(categoryCreateRequest)));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<CategoryResponse>> search(@RequestBody @Validated CategorySearchRequest request, @RequestParam(name = "pageNo", defaultValue = "0") int pageNo) {
        return ResponseEntity.ok(categoryService.search(request, Status.ACTIVE, PageRequest.of(pageNo, DEFAULT_PAGE_SIZE, Sort.by("name").ascending())));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CategoryResponse> update(@RequestBody @Validated CategoryUpdateRequest categoryUpdateRequest, @PathVariable Long id) throws MtgWalletGenericException {
        return ResponseEntity.ok(categoryServiceMapper.toCategoryResponse(categoryService.update(categoryUpdateRequest, id)));
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws MtgWalletGenericException {
        categoryService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/create/enums")
    public ResponseEntity<CategoryCreateScreenEnumResponse> getCategoryCreateScreenEnums() {
        return ResponseEntity.ok(categoryService.getCategoryCreateScreenEnums());
    }
}
