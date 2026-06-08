package com.sdpm.workitem.controller;

import com.sdpm.workitem.common.Result;
import com.sdpm.workitem.dto.DictCreateReqDTO;
import com.sdpm.workitem.dto.DictUpdateReqDTO;
import com.sdpm.workitem.service.DictService;
import com.sdpm.workitem.vo.DictRespVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping
    public Result<List<DictRespVO>> listDicts(@RequestParam String type,
                                              @RequestParam(defaultValue = "true") Boolean enabledOnly) {
        return Result.success(dictService.listByType(type, enabledOnly));
    }

    @PostMapping
    public Result<DictRespVO> createDict(@Valid @RequestBody DictCreateReqDTO dto) {
        return Result.success(dictService.createDict(dto));
    }

    @PutMapping("/{id}")
    public Result<DictRespVO> updateDict(@PathVariable Long id,
                                         @Valid @RequestBody DictUpdateReqDTO dto) {
        return Result.success(dictService.updateDict(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteDict(@PathVariable Long id) {
        dictService.deleteDict(id);
        return Result.success(true);
    }
}