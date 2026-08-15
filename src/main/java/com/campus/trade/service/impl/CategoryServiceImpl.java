package com.campus.trade.service.impl;

import com.campus.trade.entity.CategoryDO;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.service.CategoryService;
import com.campus.trade.vo.CategoryVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryVO> getCategoryTree() {
        List<CategoryDO> all = categoryMapper.selectAll();

        Map<Long, List<CategoryDO>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != 0)
                .collect(Collectors.groupingBy(CategoryDO::getParentId));

        return all.stream()
                .filter(c -> c.getParentId() == 0)
                .map(c -> toVO(c, childrenMap))
                .collect(Collectors.toList());
    }

    private CategoryVO toVO(CategoryDO c, Map<Long, List<CategoryDO>> childrenMap) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setSort(c.getSort());
        vo.setIcon(c.getIcon());

        List<CategoryDO> children = childrenMap.get(c.getId());
        if (children != null) {
            vo.setChildren(children.stream()
                    .map(child -> toVO(child, childrenMap))
                    .collect(Collectors.toList()));
        } else {
            vo.setChildren(new ArrayList<>());
        }
        return vo;
    }
}
