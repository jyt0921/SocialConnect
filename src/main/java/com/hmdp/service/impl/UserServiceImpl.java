package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;//Spring Data Redis库中的一个类


    @Override
    public Result sedCode(String phone, HttpSession session) {
        //1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

/*        //4. 保存验证码到session
        session.setAttribute("code",code);*/

        //4、保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5. 发送验证码
        log.debug("发送短信验证码成功，验证码:{}",code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1. 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        //2. 校验验证码(Redis)
        String s = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (s == null || !s.equals(code)){
            //3. 不一致，报错
            return Result.fail("验证码错误");
        }
        //4.一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();

        //5. 判断用户是否存在
        if (user == null){
            //6. 不存在，创建新用户
            user = createUserWithPhone(phone);
        }
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);
        //7.保存用户信息到Redis
        //随即生成令牌，并将其保存进Redis中后面会调用
        String token = UUID.randomUUID().toString(true);
        //将userDTO对象转为Map集合
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> beanToMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .ignoreNullValue()
                        .setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));
        //存储
        String tokenkey=LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenkey,beanToMap);
        //设置token有效期
        stringRedisTemplate.expire(tokenkey,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);

    }


    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        return user;
    }
}
