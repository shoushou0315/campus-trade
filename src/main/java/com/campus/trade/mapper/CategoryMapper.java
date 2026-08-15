package com.campus.trade.mapper;

import com.campus.trade.entity.CategoryDO;
import java.util.List;

public interface CategoryMapper {

    List<CategoryDO> selectAll();

    CategoryDO selectById(Long id);
}
