package me.jianwen.mediask.domain.repository;

import me.jianwen.mediask.user.domain.entity.User;

import java.util.Optional;

/**
 * 用户仓储接口
 *
 * @author jianwen
 */
public interface UserRepository {

    /**
     * 根据ID查询用户
     */
    Optional<User> findById(Long id);

    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据用户名或手机号查询用户
     */
    Optional<User> findByUsernameOrPhone(String account);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 统计用户名数量
     */
    long countByUsername(String username);

    /**
     * 统计手机号数量
     */
    long countByPhone(String phone);

    /**
     * 保存用户
     */
    void save(User user);

    /**
     * 插入用户
     */
    Long insert(User user);

    /**
     * 根据ID删除用户
     */
    void deleteById(Long id);
}
