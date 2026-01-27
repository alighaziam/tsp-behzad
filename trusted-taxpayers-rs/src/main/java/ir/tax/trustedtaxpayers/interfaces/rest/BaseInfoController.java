package ir.tax.trustedtaxpayers.interfaces.rest;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.BaseInfoService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.BaseInfoEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.BaseInfoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت اطلاعات پایه
 * 
 * <p>REST controller handling base info retrieval by category.</p>
 */
@RestController
@RequestMapping("/api/base-info")
@Tag(name = "Base Info Management", description = "مدیریت اطلاعات پایه\n\nراهنمای سریع:\n- Base: /api/base-info\n- دریافت بر اساس category: GET /api/base-info/by-category?category=CATEGORY_NAME\n")
public class BaseInfoController extends BaseController<BaseInfoEntity, UUID> {
    
    private final BaseInfoService baseInfoService;

    public BaseInfoController(BaseInfoService baseInfoService, BaseInfoRepository baseInfoRepository) {
        super(baseInfoService, baseInfoRepository);
        this.baseInfoService = baseInfoService;
    }

    /**
     * دریافت اطلاعات پایه بر اساس category
     * 
     * @param category دسته‌بندی مورد نظر
     * @return لیست اطلاعات پایه که active باشند
     */
    @GetMapping("/by-category")
    @Operation(
        summary = "یافتن اطلاعات پایه بر اساس category",
        description = "دریافت لیست اطلاعات پایه که active باشند بر اساس category"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "لیست اطلاعات پایه با موفقیت دریافت شد",
            content = @Content(schema = @Schema(implementation = BaseInfoEntity.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "پارامتر category الزامی است"
        )
    })
    public ResponseEntity<List<BaseInfoEntity>> findByCategory(@RequestParam String category) {
        if (category == null || category.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<BaseInfoEntity> result = baseInfoService.findByCategory(category);
        return ResponseEntity.ok(result);
    }
}


