package com.sdpm.workitem.service;

import com.sdpm.workitem.dto.DictCreateReqDTO;
import com.sdpm.workitem.dto.DictUpdateReqDTO;
import com.sdpm.workitem.vo.DictRespVO;

import java.util.List;

public interface DictService {

    List<DictRespVO> listByType(String type, Boolean enabledOnly);

    DictRespVO createDict(DictCreateReqDTO dto);

    DictRespVO updateDict(Long id, DictUpdateReqDTO dto);

    boolean deleteDict(Long id);
}