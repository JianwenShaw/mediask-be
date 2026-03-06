package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.UserDO;
import me.jianwen.mediask.dal.mapper.UserMapper;
import me.jianwen.mediask.infra.persistence.converter.UserConverter;
import me.jianwen.mediask.user.domain.entity.User;
import me.jianwen.mediask.user.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现（基础设施层）
 *
 * @author jianwen
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @Override
    public Optional<User> findById(Long id) {
        UserDO user = userMapper.selectById(id);
        return Optional.ofNullable(userConverter.toDomain(user));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, username);
        UserDO user = userMapper.selectOne(wrapper);
        return Optional.ofNullable(userConverter.toDomain(user));
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getPhone, phone);
        UserDO user = userMapper.selectOne(wrapper);
        return Optional.ofNullable(userConverter.toDomain(user));
    }

    @Override
    public Optional<User> findByUsernameOrPhone(String account) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, account)
                .or(wrapper2 -> wrapper2.eq(UserDO::getPhone, account));
        UserDO user = userMapper.selectOne(wrapper);
        return Optional.ofNullable(userConverter.toDomain(user));
    }

    @Override
    public boolean existsByUsername(String username) {
        return countByUsername(username) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        return countByPhone(phone) > 0;
    }

    @Override
    public long countByUsername(String username) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, username);
        return userMapper.selectCount(wrapper);
    }

    @Override
    public long countByPhone(String phone) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getPhone, phone);
        return userMapper.selectCount(wrapper);
    }

    @Override
    public void save(User user) {
        UserDO userDO = userConverter.toDataObject(user);
        if (userDO.getId() == null) {
            userMapper.insert(userDO);
            // 回填ID
            if (user.getId() != null) {
                try {
                    java.lang.reflect.Field field = User.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(user, userDO.getId());
                } catch (Exception e) {
                    // 忽略
                }
            }
        } else {
            userMapper.updateById(userDO);
        }
    }

    @Override
    public Long insert(User user) {
        UserDO userDO = userConverter.toDataObject(user);
        userMapper.insert(userDO);
        return userDO.getId();
    }

    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }
}
