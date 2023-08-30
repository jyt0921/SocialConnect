package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
/*   //下面是采用的redis的hash模式将商铺数据加入到redis中
 @Override
    public Result queryById(Long id){
        String key=CACHE_SHOP_KEY+id;
        //1、从Redis查询商铺缓存
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(key);
        if (!map.isEmpty()) {
            Shop shop = BeanUtil.fillBeanWithMap(map, new Shop(), false);
            return Result.ok(shop);
        }
        Shop byId = getById(id);
        if(byId==null){
            return Result.fail("店铺不存在");
        }
        Map<String, Object> beanToMap = BeanUtil.beanToMap(byId,new HashMap<>(),
                CopyOptions.create()
                        .ignoreNullValue()
                        .setFieldValueEditor((fieldName,fieldValue) -> {
                            if (fieldValue == null) {
                                return "";
                            }
                            return fieldValue.toString();
                        }));
        stringRedisTemplate.opsForHash().putAll(key,beanToMap);
        stringRedisTemplate.expire(key,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(byId);
    }*/
    //下面是String方式将商铺数据加入到redis中
    @Override
    public Result queryById(Long id){
        String key=CACHE_SHOP_KEY+id;
        //1、从Redis查询商铺缓存
        String ShopJson = stringRedisTemplate.opsForValue().get(key);
        //2、判断是否存在
        if (StrUtil.isNotBlank(ShopJson)) {
            //3、存在直接返回
            Shop shop = JSONUtil.toBean(ShopJson, Shop.class);
            return Result.ok(shop);
        }
        //4、不存在，根据id查询数据库
        Shop byId = getById(id);
        if(byId==null){
            return Result.fail("店铺不存在");
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(byId),LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(byId);
    }
    //更新数据
    @Override
    @Transactional
    public Result updata(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺不存在");
        }
        //1、更新数据库
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
    //通过锁解决缓存击穿问题，还有一种是通过逻辑过期解决缓存击穿
    /*

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    public Shop queryWithMutex(Long id)  {
        String key = CACHE_SHOP_KEY + id;
        // 1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get("key");
        // 2、判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 存在,直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断命中的值是否是空值
        if (shopJson != null) {
            //返回一个错误信息
            return null;
        }
        // 4.实现缓存重构
        //4.1 获取互斥锁
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2 判断否获取成功
            if(!isLock){
                //4.3 失败，则休眠重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //4.4 成功，根据id查询数据库
            shop = getById(id);
            // 5.不存在，返回错误
            if(shop == null){
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                //返回错误信息
                return null;
            }
            //6.写入redis
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_NULL_TTL,TimeUnit.MINUTES);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
        finally {
            //7.释放互斥锁
            unlock(lockKey);
        }
        return shop;
    }
   */

    //下面是自己封装了工具类用来解决缓存穿透、缓存击穿问题
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById0(Long id) {
        // 解决缓存穿透
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 7.返回
        return Result.ok(shop);
    }
}
