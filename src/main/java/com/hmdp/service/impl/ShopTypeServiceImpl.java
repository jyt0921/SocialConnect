package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /*下面是采用的list
    @Override
    public Result queryList() {
        //先从Redis中查，这里的常量值是固定前缀 + 店铺id
        List<String> shopTypes = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        //如果不为空（查询到了），则转为ShopType类型直接返回
        if (!shopTypes.isEmpty()) {
            List<ShopType> tmp = new ArrayList<>();
            for (String types : shopTypes) {
                ShopType shopType = JSONUtil.toBean(types, ShopType.class);
                tmp.add(shopType);
            }
            return Result.ok(tmp);
        }
        //否则去数据库中查
        List<ShopType> tmp = query().orderByAsc("sort").list();
        if (tmp == null){
            return Result.fail("店铺类型不存在！！");
        }
        //查到了转为json字符串，存入redis
        for (ShopType shopType : tmp) {
            String jsonStr = JSONUtil.toJsonStr(shopType);
            shopTypes.add(jsonStr);
        }
        stringRedisTemplate.opsForList().leftPushAll(CACHE_SHOP_TYPE_KEY,shopTypes);
        //最终把查询到的商户分类信息返回给前端
        return Result.ok(tmp);
    }*/
    //下面是String
    @Override
    public Result queryList() {
        //先从Redis中查，这里的常量值是固定前缀 + 店铺id
        String ShopTypeJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);
        //如果不为空（查询到了），则转为ShopType类型直接返回
        List<ShopType> shopTypes = JSONUtil.toList(ShopTypeJson, ShopType.class);
        if (!CollectionUtils.isEmpty(shopTypes)) {
            return Result.ok(shopTypes);
        }
        //否则去数据库中查
        List<ShopType> tmp = query().orderByAsc("sort").list();
        if (tmp == null){
            return Result.fail("店铺类型不存在！！");
        }
        //查到了转为json字符串，存入redis
        ShopTypeJson = JSONUtil.toJsonStr(tmp);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY,ShopTypeJson,LOGIN_USER_TTL, TimeUnit.MINUTES);
        //最终把查询到的商户分类信息返回给前端
        return Result.ok(tmp);
/*        // 3. 未命中，从数据库中查询商铺类型,并根据sort排序
        List<ShopType> shopTypesByMysql = query().orderByAsc("sort").list();
        // 4. 将商铺类型存入到redis中
        stringRedisTemplate.opsForValue().set("shop-type",JSONUtil.toJsonStr(shopTypesByMysql));
        // 5. 返回数据库中商铺类型信息
        return Result.ok(shopTypesByMysql);*/
}}
