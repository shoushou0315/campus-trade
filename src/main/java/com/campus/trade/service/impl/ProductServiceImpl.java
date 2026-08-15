package com.campus.trade.service.impl;

import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.common.result.PageResult;
import com.campus.trade.dto.request.ProductQueryDTO;
import com.campus.trade.dto.request.ProductSaveDTO;
import com.campus.trade.entity.ProductDO;
import com.campus.trade.entity.UserDO;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.service.CacheService;
import com.campus.trade.service.ProductService;
import com.campus.trade.vo.ProductDetailVO;
import com.campus.trade.vo.ProductVO;
import com.campus.trade.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final String DETAIL_KEY = "product:detail:";
    private static final String LIST_KEY = "product:list:";
    private static final String LIST_VERSION_KEY = "product:list:ver";
    private static final String LOCK_PREFIX = "product:lock:";
    private static final long DETAIL_TTL = 1800;       // 30 分钟
    private static final long NULL_TTL = 300;          // 空值标记 5 分钟

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final CacheService cacheService;

    public ProductServiceImpl(ProductMapper productMapper, UserMapper userMapper, CacheService cacheService) {
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.cacheService = cacheService;
    }

    @Override
    public PageResult<ProductVO> search(ProductQueryDTO query) {
        // 版本号方案：商品变更时版本号自增，旧版本搜索缓存整体失效（替代高并发下灾难性的通配删除）
        long ver = cacheService.getVersion(LIST_VERSION_KEY);
        String cacheKey = LIST_KEY + ver + ":" + query.getKeyword() + "_" + query.getCategoryId() + "_"
                + query.getCampus() + "_" + query.getSortBy() + "_" + query.getPageNum() + "_" + query.getPageSize();
        String lockKey = LOCK_PREFIX + "list:" + ver + ":" + query.getKeyword();

        return cacheService.readThrough(cacheKey, lockKey, DETAIL_TTL, NULL_TTL,
                () -> doSearch(query), PageResult.class);
    }

    private PageResult<ProductVO> doSearch(ProductQueryDTO query) {
        List<ProductDO> products = productMapper.search(
                query.getKeyword(), query.getCategoryId(), query.getCampus(),
                query.getMinPrice(), query.getMaxPrice(), query.getCondition(),
                query.getSortBy(), query.getOffset(), query.getPageSize());

        int total = productMapper.countSearch(
                query.getKeyword(), query.getCategoryId(), query.getCampus(),
                query.getMinPrice(), query.getMaxPrice(), query.getCondition());

        List<ProductVO> voList = products.stream().map(p -> {
            ProductVO vo = ProductVO.from(p);
            UserDO seller = userMapper.selectById(p.getSellerId());
            if (seller != null) {
                vo.setSellerName(seller.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(total, query.getPageNum(), query.getPageSize(), voList);
    }

    @Override
    public ProductDetailVO getDetail(Long id) {
        String cacheKey = DETAIL_KEY + id;
        String lockKey = LOCK_PREFIX + id;

        return cacheService.readThrough(cacheKey, lockKey, DETAIL_TTL, NULL_TTL, () -> {
            ProductDO product = productMapper.selectById(id);
            if (product == null) {
                return null;  // 空值标记，防穿透
            }
            if (product.getStatus() == 0) {
                throw new BusinessException("商品已下架");
            }

            productMapper.incrementViewCount(id);
            ProductVO vo = ProductVO.from(product);

            UserDO seller = userMapper.selectById(product.getSellerId());
            UserVO sellerVO = seller != null ? UserVO.from(seller) : null;

            return new ProductDetailVO(vo, sellerVO, null);
        }, ProductDetailVO.class);
    }

    @Override
    public ProductVO create(Long sellerId, ProductSaveDTO dto) {
        ProductDO product = new ProductDO();
        product.setSellerId(sellerId);
        product.setCategoryId(dto.getCategoryId());
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setImages(dto.getImages());
        product.setCampus(dto.getCampus());
        product.setCondition(dto.getCondition());
        product.setStatus(1);

        productMapper.insert(product);
        cacheService.bumpVersion(LIST_VERSION_KEY);  // 版本号自增，旧搜索缓存失效

        logger.info("用户{}发布商品: {}", sellerId, dto.getTitle());
        return ProductVO.from(product);
    }

    @Override
    public ProductVO update(Long id, Long sellerId, ProductSaveDTO dto) {
        ProductDO exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!exist.getSellerId().equals(sellerId)) {
            throw new BusinessException("只能编辑自己的商品");
        }

        ProductDO product = new ProductDO();
        product.setId(id);
        product.setCategoryId(dto.getCategoryId());
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setImages(dto.getImages());
        product.setCampus(dto.getCampus());
        product.setCondition(dto.getCondition());

        productMapper.update(product);
        cacheService.evict(DETAIL_KEY + id);
        cacheService.bumpVersion(LIST_VERSION_KEY);

        return ProductVO.from(productMapper.selectById(id));
    }

    @Override
    public void updateStatus(Long id, Long sellerId, Integer status) {
        ProductDO exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!exist.getSellerId().equals(sellerId)) {
            throw new BusinessException("只能操作自己的商品");
        }

        productMapper.updateStatus(id, status);
        cacheService.evict(DETAIL_KEY + id);
        cacheService.bumpVersion(LIST_VERSION_KEY);
    }

    @Override
    public void delete(Long id, Long sellerId) {
        ProductDO exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!exist.getSellerId().equals(sellerId)) {
            throw new BusinessException("只能删除自己的商品");
        }

        productMapper.updateStatus(id, 0);
        cacheService.evict(DETAIL_KEY + id);
        cacheService.bumpVersion(LIST_VERSION_KEY);
    }
}
