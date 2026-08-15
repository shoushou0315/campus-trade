package com.campus.trade.service;

import com.campus.trade.vo.CategoryVO;
import java.util.List;

public interface CategoryService {

    List<CategoryVO> getCategoryTree();
}
